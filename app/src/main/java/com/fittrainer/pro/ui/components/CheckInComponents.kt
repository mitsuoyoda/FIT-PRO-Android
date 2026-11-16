package com.fittrainer.pro.ui.components

import android.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.util.Time

@Composable
fun CheckInRow(
    nome: String,
    checked: Boolean,
    detalhe: String,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { if (enabled) onToggle(it) },
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nome,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = detalhe,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CheckInConfirmDialog(
    alunoNome: String,
    realizar: Boolean,
    valorAula: Double?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val valorTexto = Time.money(valorAula ?: 0.0)
    val titulo = if (realizar) "Confirmar aula a pagar?" else "Remover aula a pagar?"
    val mensagem = if (realizar) {
        "Confirmar a aula a pagar de $alunoNome? Será lançado $valorTexto na ficha do aluno e no financeiro, com o valor por aula cadastrado."
    } else {
        "Remover o lançamento de $alunoNome? A dívida desta aula será estornada da ficha e do financeiro."
    }
    DisposableEffect(alunoNome, realizar, valorAula) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(titulo)
            .setMessage(mensagem)
            .setPositiveButton("Salvar") { _, _ -> onConfirm() }
            .setNegativeButton("Cancelar") { _, _ -> onDismiss() }
            .setOnCancelListener { onDismiss() }
            .setCancelable(true)
            .show()
        onDispose {
            if (dialog.isShowing) dialog.dismiss()
        }
    }
}
