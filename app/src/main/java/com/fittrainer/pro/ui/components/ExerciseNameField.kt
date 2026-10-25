package com.fittrainer.pro.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.data.catalog.ExerciseCatalog
import com.fittrainer.pro.data.catalog.ExerciseCatalogItem

@Composable
fun ExerciseNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onPick: (ExerciseCatalogItem) -> Unit,
    onShowInfo: () -> Unit,
    hasInfo: Boolean
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var escolhido by remember { mutableStateOf(false) }
    val suggestions = remember(value) { ExerciseCatalog.search(context, value) }
    val mostrarLista = focused && !escolhido && suggestions.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                escolhido = false
                onValueChange(it)
            },
            label = { Text("Nome do exercício") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            trailingIcon = {
                if (hasInfo) {
                    IconButton(onClick = onShowInfo) {
                        Icon(Icons.Outlined.Info, contentDescription = "Ver detalhes e vídeos")
                    }
                }
            }
        )
        if (mostrarLista) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    suggestions.forEach { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    escolhido = true
                                    focused = false
                                    focusManager.clearFocus()
                                    onPick(item)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(item.displayName, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(
                                    item.nomeEn.takeIf { it.isNotBlank() && it != item.displayName },
                                    item.grupo.takeIf { it.isNotBlank() }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseInfoDialog(
    item: ExerciseCatalogItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.displayName) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.nomeEn.isNotBlank() && item.nomeEn != item.displayName) {
                    InfoLine("Nome em inglês", item.nomeEn)
                }
                if (item.grupo.isNotBlank()) InfoLine("Grupo muscular", item.grupo)
                if (item.secundarios.isNotBlank()) InfoLine("Músculos secundários", item.secundarios)
                if (item.calorias.isNotBlank()) {
                    InfoLine("Estimativa calórica (1h)", "${item.calorias} kcal")
                }
                if (item.pesoRefKg.isNotBlank()) InfoLine("Peso de referência", "${item.pesoRefKg} kg")
                if (item.premissa.isNotBlank()) InfoLine("Premissa", item.premissa)
                if (item.videos.isEmpty()) {
                    Text("Nenhum vídeo cadastrado para este exercício.")
                } else {
                    Text("Vídeos", fontWeight = FontWeight.SemiBold)
                    item.videos.forEach { (label, url) ->
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { context.startActivity(intent) }
                                }
                                .padding(vertical = 4.dp)
                        )
                        Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(value)
    }
}
