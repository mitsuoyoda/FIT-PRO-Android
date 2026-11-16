package com.fittrainer.pro.data.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

data class TrainerClientLink(
    val id: String,
    val clientId: String,
    val clientUid: String,
    val clientName: String,
    val trainerName: String,
    val status: String,
)

data class TrainerFeedbackExercise(
    val name: String,
    val plannedSets: Int,
    val completedSets: Int,
    val loadKg: Double?,
    val sets: List<TrainerFeedbackSet>,
)

data class TrainerFeedbackSet(
    val number: Int,
    val startedAtMs: Long?,
    val completedAtMs: Long?,
)

data class TrainerFeedbackSession(
    val id: String,
    val workoutDate: String,
    val startedAtMs: Long?,
    val submittedAtMs: Long?,
    val notes: String?,
    val exercises: List<TrainerFeedbackExercise>,
)

object TrainerFirebaseSync {
    private const val PREFS = "fittrainer_cloud"
    private const val CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    fun isSignedIn(): Boolean = auth.currentUser != null
    fun signedInEmail(): String? = auth.currentUser?.email
    fun signedInName(): String? = auth.currentUser?.displayName

    suspend fun createAccount(name: String, email: String, password: String) {
        require(name.isNotBlank()) { "Informe seu nome." }
        require(password.length >= 6) { "A senha precisa ter pelo menos 6 caracteres." }
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Não foi possível criar a conta do treinador.")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        saveTrainerProfile(user.uid, name.trim(), email.trim())
    }

    suspend fun signIn(name: String, email: String, password: String) {
        val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Não foi possível acessar a conta.")
        val trainerName = user.displayName?.takeIf { it.isNotBlank() } ?: name.trim().ifBlank { "Treinador" }
        saveTrainerProfile(user.uid, trainerName, email.trim())
    }

    private suspend fun saveTrainerProfile(uid: String, name: String, email: String) {
        db.collection("users").document(uid).set(
            mapOf("displayName" to name, "email" to email, "role" to "trainer", "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        ).await()
    }

    suspend fun claimStudentCode(context: Context, clientId: String, code: String, trainerName: String): TrainerClientLink {
        val trainer = auth.currentUser ?: error("Entre na conta do treinador primeiro.")
        require(code.filterNot(Char::isWhitespace).length == 10) { "O código deve ter 10 caracteres." }
        val codeHash = hash(code)
        val codeRef = db.collection("pairingCodes").document(codeHash)
        var claimed: TrainerClientLink? = null
        db.runTransaction { transaction ->
            val codeDoc = transaction.get(codeRef)
            check(codeDoc.getString("status") == "available") { "Código inválido, expirado ou já utilizado." }
            val expiry = codeDoc.getTimestamp("expiresAt") ?: error("Código sem validade.")
            check(expiry.toDate().time > System.currentTimeMillis()) { "Este código expirou. Peça um novo ao aluno." }
            val clientUid = codeDoc.getString("clientUid") ?: error("Código sem aluno associado.")
            val clientName = codeDoc.getString("clientName") ?: "Aluno"
            val linkId = hash("${trainer.uid}|$clientUid|$codeHash")
            val linkRef = db.collection("links").document(linkId)
            check(!transaction.get(linkRef).exists()) { "Este código já foi usado." }
            transaction.set(
                linkRef,
                mapOf(
                    "clientId" to clientId,
                    "clientUid" to clientUid,
                    "clientName" to clientName,
                    "trainerUid" to trainer.uid,
                    "trainerName" to trainerName.trim().ifBlank { trainer.displayName ?: "Treinador" },
                    "codeHash" to codeHash,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            transaction.update(
                codeRef,
                mapOf(
                    "status" to "claimed",
                    "trainerUid" to trainer.uid,
                    "trainerName" to trainerName.trim().ifBlank { trainer.displayName ?: "Treinador" },
                    "linkId" to linkId,
                ),
            )
            claimed = TrainerClientLink(linkId, clientId, clientUid, clientName, trainerName, "pending")
            linkId
        }.await()
        val result = checkNotNull(claimed)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(linkKey(trainer.uid, clientId), result.id).apply()
        return result
    }

    suspend fun loadLink(context: Context, clientId: String): TrainerClientLink? {
        val trainerUid = auth.currentUser?.uid ?: return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedId = prefs.getString(linkKey(trainerUid, clientId), null)
        val document = if (savedId != null) {
            db.collection("links").document(savedId).get().await().takeIf { it.exists() }
        } else null
        val resultDoc = document ?: db.collection("links")
            .whereEqualTo("trainerUid", trainerUid)
            .get().await().documents
            .filter { it.getString("clientId") == clientId }
            .maxByOrNull {
                when (it.getString("status")) { "active" -> 2; "pending" -> 1; else -> 0 }
            }
        if (resultDoc == null || resultDoc.getString("trainerUid") != trainerUid) return null
        val link = TrainerClientLink(
            id = resultDoc.id,
            clientId = clientId,
            clientUid = resultDoc.getString("clientUid") ?: return null,
            clientName = resultDoc.getString("clientName") ?: "Aluno",
            trainerName = resultDoc.getString("trainerName") ?: "Treinador",
            status = resultDoc.getString("status") ?: "revoked",
        )
        prefs.edit().putString(linkKey(trainerUid, clientId), link.id).apply()
        return link
    }

    suspend fun publishPlan(context: Context, link: TrainerClientLink, exportId: String, title: String, payload: String) {
        require(link.status == "active") { "O aluno precisa aceitar o vínculo antes de receber treinos." }
        db.collection("links").document(link.id).collection("plans").document(exportId).set(
            mapOf(
                "exportId" to exportId,
                "title" to title,
                "payload" to payload,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
    }

    suspend fun sendMessage(link: TrainerClientLink, title: String, body: String) {
        require(link.status == "active") { "O aluno precisa aceitar o vínculo para receber mensagens." }
        require(body.isNotBlank()) { "Escreva uma mensagem antes de enviar." }
        db.collection("links").document(link.id).collection("messages").add(
            mapOf(
                "title" to title.trim().ifBlank { "Mensagem do treinador" }.take(80),
                "body" to body.trim().take(2000),
                "createdAt" to FieldValue.serverTimestamp(),
                "readAt" to null,
            ),
        )
    }

    suspend fun feedback(link: TrainerClientLink): List<TrainerFeedbackSession> {
        if (link.status != "active") return emptyList()
        return db.collection("links").document(link.id).collection("sessions")
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(30)
            .get().await().documents.map { document ->
                val exercises = (document.get("exercises") as? List<*>)?.mapNotNull { raw ->
                    val item = raw as? Map<*, *> ?: return@mapNotNull null
                    val sets = item["sets"] as? List<*> ?: emptyList<Any>()
                    val parsedSets = sets.mapNotNull setMap@{ rawSet ->
                        val set = rawSet as? Map<*, *> ?: return@setMap null
                        TrainerFeedbackSet(
                            number = (set["number"] as? Number)?.toInt() ?: return@setMap null,
                            startedAtMs = (set["startedAtMs"] as? Number)?.toLong(),
                            completedAtMs = (set["completedAtMs"] as? Number)?.toLong(),
                        )
                    }
                    TrainerFeedbackExercise(
                        name = item["name"] as? String ?: "Exercício",
                        plannedSets = (item["plannedSets"] as? Number)?.toInt() ?: sets.size,
                        completedSets = sets.count { (it as? Map<*, *>)?.get("completed") == true },
                        loadKg = (item["loadKg"] as? Number)?.toDouble(),
                        sets = parsedSets,
                    )
                }.orEmpty()
                TrainerFeedbackSession(
                    id = document.id,
                    workoutDate = document.getString("workoutDate") ?: "",
                    startedAtMs = document.getLong("startedAtMs"),
                    submittedAtMs = document.getLong("submittedAtMs"),
                    notes = document.getString("notes"),
                    exercises = exercises,
                )
            }
    }

    suspend fun disconnect(link: TrainerClientLink) {
        if (link.status == "active" || link.status == "pending") {
            db.collection("links").document(link.id).update(
                "status", "revoked",
                "updatedAt", FieldValue.serverTimestamp(),
            ).await()
        }
    }

    fun signOut() = auth.signOut()

    private fun linkKey(trainerUid: String, clientId: String) = "link_${trainerUid}_$clientId"

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.trim().uppercase().toByteArray())
        .joinToString("") { "%02x".format(it) }
}
