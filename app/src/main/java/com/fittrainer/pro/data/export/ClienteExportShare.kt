package com.fittrainer.pro.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ClienteExportShare {
    fun compartilhar(context: Context, arquivos: List<Pair<String, String>>, caption: String): Boolean {
        if (arquivos.isEmpty()) return false
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.filter { it.name.startsWith("cliente-") && it.name.endsWith(".json") }?.forEach { it.delete() }
        val uris = ArrayList<Uri>()
        arquivos.forEach { (nome, conteudo) ->
            val arquivo = File(dir, nome)
            arquivo.writeText(conteudo, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
            uris += uri
        }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                putExtra(Intent.EXTRA_TEXT, caption)
                clipData = ClipData.newRawUri(arquivos.first().first, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/json"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_TEXT, caption)
                clipData = ClipData.newRawUri(arquivos.first().first, uris.first()).also { clip ->
                    uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val chooser = Intent.createChooser(intent, "Enviar ao app do aluno")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(chooser) }.isSuccess
    }
}
