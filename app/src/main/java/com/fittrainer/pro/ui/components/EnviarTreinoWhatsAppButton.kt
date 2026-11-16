package com.fittrainer.pro.ui.components

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fittrainer.pro.data.assessoria.WhatsAppShare
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.util.Time

@Composable
fun EnviarTreinoWhatsAppButton(
    treino: TreinoEntity?,
    clientes: List<ClienteEntity>,
    modifier: Modifier = Modifier,
    clienteIdPreferido: String? = null,
    clienteNomePreferido: String = "",
    preenchido: Boolean = false,
    onRegistrarEnvio: (TreinoEntity) -> Unit = {}
) {
    if (treino == null) return
    val context = LocalContext.current
    val aluno = WhatsAppShare.clienteDoCadastro(
        clientes,
        clienteIdPreferido?.takeIf { it.isNotBlank() } ?: treino.clienteId,
        clienteNomePreferido.ifBlank { treino.clienteNome }
    )
    var confirmar by remember { mutableStateOf(false) }

    fun disparar() {
        val ok = WhatsAppShare.enviarTreino(
            context,
            treino,
            clientes,
            aluno?.id,
            aluno?.nome.orEmpty()
        )
        if (ok) confirmar = true
        else Toast.makeText(context, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_LONG).show()
    }

    if (preenchido) {
        Button(onClick = { disparar() }, modifier = modifier) { Text("Compartilhar arquivo") }
    } else {
        OutlinedButton(onClick = { disparar() }, modifier = modifier) { Text("Compartilhar arquivo") }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("O arquivo foi compartilhado?") },
            text = { Text("Só registramos o envio se você confirmar. Escolha o app no seletor do sistema.") },
            confirmButton = {
                TextButton(onClick = {
                    onRegistrarEnvio(treino.copy(dataEnvio = Time.nowIso(), updatedDate = Time.nowIso()))
                    confirmar = false
                }) { Text("Sim, registrar") }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Não") } }
        )
    }
}
