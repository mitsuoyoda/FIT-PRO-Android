package com.fittrainer.pro.data.assessoria

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.TreinoEntity
import java.io.File

object WhatsAppShare {
    fun telefoneE164(raw: String): String? {
        var digits = raw.filter { it.isDigit() }
        if (digits.startsWith("0")) digits = digits.trimStart('0')
        if (digits.length < 10) return null
        return if (digits.startsWith("55")) digits else "55$digits"
    }

    fun clienteDoCadastro(
        clientes: List<ClienteEntity>,
        clienteId: String,
        clienteNome: String = ""
    ): ClienteEntity? {
        clienteId.takeIf { it.isNotBlank() }?.let { id -> clientes.find { it.id == id } }?.let { return it }
        val nome = clienteNome.trim()
        if (nome.isBlank()) return null
        return clientes.singleOrNull { it.nome.trim().equals(nome, ignoreCase = true) }
    }

    fun destino(
        clientes: List<ClienteEntity>,
        clienteIdPlano: String,
        clienteIdsTreino: List<String> = emptyList(),
        clienteNomePlano: String = "",
        clienteNomesTreino: List<String> = emptyList()
    ): ClienteEntity? {
        clienteDoCadastro(clientes, clienteIdPlano, clienteNomePlano)?.let { return it }
        val unicos = clienteIdsTreino.filter { it.isNotBlank() }.distinct()
        if (unicos.size == 1) {
            clienteDoCadastro(clientes, unicos.single())?.let { return it }
        }
        val nomes = clienteNomesTreino.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        return if (nomes.size == 1) clienteDoCadastro(clientes, "", nomes.single()) else null
    }

    fun enviarTreino(
        context: Context,
        treino: TreinoEntity,
        clientes: List<ClienteEntity>,
        clienteIdPreferido: String? = null,
        clienteNomePreferido: String = "",
        ignorarTelefone: Boolean = false
    ): Boolean {
        val aluno = clienteDoCadastro(clientes, clienteIdPreferido.orEmpty(), clienteNomePreferido)
            ?: clienteDoCadastro(clientes, treino.clienteId, treino.clienteNome)
        val html = AssessoriaHtml.gerarTreino(treino, aluno?.nome ?: treino.clienteNome)
        val caption = buildString {
            append("FitTrainer Pro — ${treino.titulo}")
            if ((aluno?.nome ?: treino.clienteNome).isNotBlank()) {
                append("\nAluno: ${aluno?.nome ?: treino.clienteNome}")
            }
            if (!ignorarTelefone && !aluno?.telefone.isNullOrBlank()) {
                append("\nContato: ${aluno?.telefone}")
            }
            append("\nAbra o HTML no navegador. Use o filtro por dia no topo.")
        }
        return compartilharHtml(
            context,
            html,
            AssessoriaHtml.nomeArquivoTreino(treino),
            caption
        )
    }

    fun compartilharHtml(
        context: Context,
        html: String,
        nomeArquivo: String,
        caption: String
    ): Boolean {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.filter {
            it.name.startsWith("assessoria-") || it.name.startsWith("treino-")
        }?.forEach { it.delete() }
        val arquivo = File(dir, nomeArquivo)
        arquivo.writeText(html, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(nomeArquivo, uri)
        }
        val chooser = Intent.createChooser(intent, "Compartilhar arquivo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(chooser) }.isSuccess
    }
}
