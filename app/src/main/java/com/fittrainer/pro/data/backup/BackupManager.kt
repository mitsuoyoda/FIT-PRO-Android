package com.fittrainer.pro.data.backup

import android.content.Context
import android.net.Uri
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.repository.BackupPayload
import com.fittrainer.pro.data.repository.FitTrainerRepository

class BackupManager(
    private val context: Context,
    private val repository: FitTrainerRepository
) {
    suspend fun exportTo(uri: Uri, password: CharArray = CharArray(0)) {
        val payload = repository.exportBackup()
        val plain = JsonStore.json.encodeToString(BackupPayload.serializer(), payload).toByteArray(Charsets.UTF_8)
        val body = if (password.isNotEmpty()) BackupCrypto.encrypt(plain, password) else plain
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(body)
        } ?: error("Não foi possível gravar o arquivo")
    }

    suspend fun importFrom(uri: Uri, replace: Boolean, password: CharArray = CharArray(0)) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes()
        } ?: error("Não foi possível ler o arquivo")
        if (bytes.size > BackupCrypto.MAX_BYTES) error("Arquivo de backup grande demais")
        val json = if (BackupCrypto.isEncrypted(bytes)) {
            BackupCrypto.decrypt(bytes, password).toString(Charsets.UTF_8)
        } else {
            bytes.toString(Charsets.UTF_8)
        }
        val payload = JsonStore.json.decodeFromString(BackupPayload.serializer(), json)
        repository.importBackup(payload, replace)
    }
}
