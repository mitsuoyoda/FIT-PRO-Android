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
    suspend fun exportTo(uri: Uri) {
        val payload = repository.exportBackup()
        val body = JsonStore.json.encodeToString(BackupPayload.serializer(), payload)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(body.toByteArray(Charsets.UTF_8))
        } ?: error("Não foi possível gravar o arquivo")
    }

    suspend fun importFrom(uri: Uri, replace: Boolean) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error("Não foi possível ler o arquivo")
        val payload = JsonStore.json.decodeFromString(BackupPayload.serializer(), text)
        repository.importBackup(payload, replace)
    }
}
