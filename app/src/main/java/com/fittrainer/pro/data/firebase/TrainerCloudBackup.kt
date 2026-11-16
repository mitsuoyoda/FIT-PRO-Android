package com.fittrainer.pro.data.firebase

import android.content.Context
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.ExercicioBibliotecaEntity
import com.fittrainer.pro.data.local.HistoricoTreinoEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.PlanoAssessoriaEntity
import com.fittrainer.pro.data.local.ProgressoEntity
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.repository.BackupPayload
import com.fittrainer.pro.data.repository.FitTrainerRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import java.security.MessageDigest

object TrainerCloudBackup {
    private const val PREFS = "fittrainer_cloud_backup"
    private const val CURSOR_KEY = "updated_at_cursor"
    private const val PAGE_SIZE = 25L
    private const val WRITE_BATCH_SIZE = 20
    private const val SCHEMA_VERSION = 1
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val recordTypes = setOf(
        "clientes", "treinos", "aulas", "avaliacoes", "progresso",
        "historico_treinos", "transacoes_financeiras", "exercicios_biblioteca", "planos_assessoria",
    )

    data class SyncResult(
        val uploaded: Int,
        val restored: Int,
        val waitingForNetwork: Boolean,
    )

    private data class Record(
        val type: String,
        val id: String,
        val payload: String,
        val hash: String = digest(payload),
        val updatedDate: String = JSONObject(payload).optString("updatedDate"),
    ) {
        val key: String get() = "$type:$id"
        val documentId: String get() = digest(key)
    }

    private data class RemoteRecord(val record: Record)

    suspend fun sync(context: Context, repository: FitTrainerRepository): SyncResult {
        val uid = auth.currentUser?.uid ?: return SyncResult(0, 0, false)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedCursorSeconds = prefs.getLong(cursorSecondsKey(uid), 0L)
        val savedCursorNanos = prefs.getInt(cursorNanosKey(uid), 0)
        val savedCursorDocumentId = prefs.getString(cursorDocumentKey(uid), null)
        val localRecords = repository.exportBackup().toRecords()
        val remoteRecords = mutableMapOf<String, RemoteRecord>()
        var lastRemoteTimestamp: Timestamp? = null
        var lastRemoteDocumentId: String? = null
        var remoteReadSucceeded = true

        try {
            var query: Query = db.collection("users").document(uid).collection("backups")
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                .limit(PAGE_SIZE)
            if (savedCursorDocumentId != null) {
                query = query.startAfter(Timestamp(savedCursorSeconds, savedCursorNanos), savedCursorDocumentId)
            }
            var page = query.get(Source.SERVER).await()
            while (page.documents.isNotEmpty()) {
                page.documents.forEach { document ->
                    val type = document.getString("entityType")?.takeIf { it in recordTypes } ?: return@forEach
                    val id = document.getString("recordId")?.takeIf { it.isNotBlank() } ?: return@forEach
                    val payload = document.getString("payload") ?: return@forEach
                    document.getTimestamp("updatedAt") ?: return@forEach
                    val payloadHash = document.getString("payloadHash") ?: digest(payload)
                    require(payload.length <= 262_144) { "O backup remoto contém um registro acima do limite permitido." }
                    val record = Record(type, id, payload, payloadHash, JSONObject(payload).optString("updatedDate"))
                    remoteRecords[record.key] = RemoteRecord(record)
                }
                page.documents.lastOrNull()?.let { lastDocument ->
                    lastRemoteTimestamp = lastDocument.getTimestamp("updatedAt")
                    lastRemoteDocumentId = lastDocument.id
                }
                if (page.size().toLong() < PAGE_SIZE) break
                page = query.startAfter(page.documents.last()).get(Source.SERVER).await()
            }
        } catch (_: Exception) {
            remoteReadSucceeded = false
        }

        // On a new device, wait for the first successful cloud read before sending local rows.
        // This prevents an empty or older local database from replacing an existing cloud copy.
        val hasPriorCloudSync = savedCursorDocumentId != null
        val allowUploads = remoteReadSucceeded || hasPriorCloudSync
        val localByKey = localRecords.associateBy { it.key }
        val merged = mutableMapOf<String, Record>()
        val uploadRecords = mutableListOf<Record>()
        val hashesToRemember = mutableMapOf<String, String>()
        var restored = 0

        (localByKey.keys + remoteRecords.keys).forEach { key ->
            val local = localByKey[key]
            val remote = remoteRecords[key]?.record
            val chosen = when {
                local == null -> remote
                remote == null -> local
                local.updatedDate >= remote.updatedDate -> local
                else -> remote
            } ?: return@forEach
            merged[key] = chosen
            val localHash = local?.hash
            val remoteHash = remote?.hash
            val previousHash = prefs.getString(hashKey(uid, chosen), null)
            when {
                chosen === remote || (local != null && chosen.payload == remote?.payload) -> {
                    hashesToRemember[hashKey(uid, chosen)] = chosen.hash
                    if (local == null || local.payload != chosen.payload) restored++
                }
                local != null && allowUploads && localHash != remoteHash &&
                    (remote != null || previousHash != localHash || previousHash == null) -> uploadRecords += local
                local != null && localHash == remoteHash -> hashesToRemember[hashKey(uid, local)] = local.hash
                local != null && remote == null && previousHash == local.hash -> Unit
                local != null && allowUploads && previousHash != local.hash -> uploadRecords += local
            }
        }

        if (restored > 0) repository.importBackup(merged.toPayload(), replace = false)
        hashesToRemember.forEach { (key, value) -> prefs.edit().putString(key, value).apply() }

        var waitingForNetwork = !remoteReadSucceeded
        var uploaded = 0
        if (allowUploads) {
            uploadRecords.distinctBy { it.key }.chunked(WRITE_BATCH_SIZE).forEach { chunk ->
                if (chunk.isEmpty()) return@forEach
                val batch = db.batch()
                chunk.forEach { record ->
                    val ref = db.collection("users").document(uid).collection("backups").document(record.documentId)
                    batch.set(
                        ref,
                        mapOf(
                            "entityType" to record.type,
                            "recordId" to record.id,
                            "payload" to record.payload,
                            "payloadHash" to record.hash,
                            "schemaVersion" to SCHEMA_VERSION,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                }
                val task = batch.commit()
                if (remoteReadSucceeded) {
                    task.await()
                    chunk.forEach { prefs.edit().putString(hashKey(uid, it), it.hash).apply() }
                } else {
                    waitingForNetwork = true
                    val acceptedHashes = chunk.associate { hashKey(uid, it) to it.hash }
                    task.addOnSuccessListener {
                        val edit = prefs.edit()
                        acceptedHashes.forEach { (key, value) -> edit.putString(key, value) }
                        edit.apply()
                    }
                }
                uploaded += chunk.size
            }
        }

        if (remoteReadSucceeded && lastRemoteTimestamp != null && lastRemoteDocumentId != null) {
            prefs.edit()
                .putLong(cursorSecondsKey(uid), lastRemoteTimestamp!!.seconds)
                .putInt(cursorNanosKey(uid), lastRemoteTimestamp!!.nanoseconds)
                .putString(cursorDocumentKey(uid), lastRemoteDocumentId)
                .apply()
        }
        return SyncResult(uploaded, restored, waitingForNetwork)
    }

    private fun BackupPayload.toRecords(): List<Record> = buildList {
        clientes.forEach { add(Record("clientes", it.id, JsonStore.json.encodeToString(it))) }
        treinos.forEach { add(Record("treinos", it.id, JsonStore.json.encodeToString(it))) }
        aulas.forEach { add(Record("aulas", it.id, JsonStore.json.encodeToString(it))) }
        avaliacoes.forEach { add(Record("avaliacoes", it.id, JsonStore.json.encodeToString(it))) }
        progresso.forEach { add(Record("progresso", it.id, JsonStore.json.encodeToString(it))) }
        historicoTreinos.forEach { add(Record("historico_treinos", it.id, JsonStore.json.encodeToString(it))) }
        transacoes.forEach { add(Record("transacoes_financeiras", it.id, JsonStore.json.encodeToString(it))) }
        exerciciosBiblioteca.forEach { add(Record("exercicios_biblioteca", it.id, JsonStore.json.encodeToString(it))) }
        planosAssessoria.forEach { add(Record("planos_assessoria", it.id, JsonStore.json.encodeToString(it))) }
    }

    private fun Map<String, Record>.toPayload() = BackupPayload(
        clientes = values.filter { it.type == "clientes" }.map { JsonStore.json.decodeFromString<ClienteEntity>(it.payload) },
        treinos = values.filter { it.type == "treinos" }.map { JsonStore.json.decodeFromString<TreinoEntity>(it.payload) },
        aulas = values.filter { it.type == "aulas" }.map { JsonStore.json.decodeFromString<AulaEntity>(it.payload) },
        avaliacoes = values.filter { it.type == "avaliacoes" }.map { JsonStore.json.decodeFromString<AvaliacaoEntity>(it.payload) },
        progresso = values.filter { it.type == "progresso" }.map { JsonStore.json.decodeFromString<ProgressoEntity>(it.payload) },
        historicoTreinos = values.filter { it.type == "historico_treinos" }.map { JsonStore.json.decodeFromString<HistoricoTreinoEntity>(it.payload) },
        transacoes = values.filter { it.type == "transacoes_financeiras" }.map { JsonStore.json.decodeFromString<TransacaoEntity>(it.payload) },
        exerciciosBiblioteca = values.filter { it.type == "exercicios_biblioteca" }.map { JsonStore.json.decodeFromString<ExercicioBibliotecaEntity>(it.payload) },
        planosAssessoria = values.filter { it.type == "planos_assessoria" }.map { JsonStore.json.decodeFromString<PlanoAssessoriaEntity>(it.payload) },
    )

    private fun cursorSecondsKey(uid: String) = "${CURSOR_KEY}_seconds_$uid"
    private fun cursorNanosKey(uid: String) = "${CURSOR_KEY}_nanos_$uid"
    private fun cursorDocumentKey(uid: String) = "${CURSOR_KEY}_document_$uid"
    private fun hashKey(uid: String, record: Record) = "hash_${uid}_${record.documentId}"

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
