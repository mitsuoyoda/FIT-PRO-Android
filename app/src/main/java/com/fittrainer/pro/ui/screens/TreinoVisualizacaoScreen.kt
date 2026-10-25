package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.data.local.ExecucaoExercicio
import com.fittrainer.pro.data.local.HistoricoTreinoEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreinoVisualizacaoScreen(
    treino: TreinoEntity?,
    onBack: () -> Unit,
    onSaveRealizado: (HistoricoTreinoEntity) -> Unit
) {
    val exercicios = remember(treino?.id, treino?.exerciciosJson) {
        treino?.let { JsonStore.decodeExercicios(it.exerciciosJson) } ?: emptyList()
    }
    val feitos = remember(treino?.id) { mutableStateMapOf<String, Boolean>() }
    val pesos = remember(treino?.id) { mutableStateMapOf<Int, String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(treino?.titulo ?: "Visualizar treino")
                        if (!treino?.clienteNome.isNullOrBlank()) {
                            Text(treino?.clienteNome.orEmpty(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (treino == null) {
            EmptyState("Treino não encontrado.", Modifier.padding(padding))
            return@Scaffold
        }
        if (exercicios.isEmpty()) {
            EmptyState("Este treino ainda não tem exercícios.", Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Marque cada série e registre o peso utilizado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (treino.clienteId.isBlank()) {
                Text("Vincule este treino a um aluno para salvar como realizado.", color = MaterialTheme.colorScheme.error)
            }
            exercicios.forEachIndexed { exIndex, exercicio ->
                val series = exercicio.series.coerceAtLeast(1)
                InfoCard {
                    Text(exercicio.nome.ifBlank { "Exercício ${exIndex + 1}" }, fontWeight = FontWeight.SemiBold)
                    val meta = listOfNotNull(
                        exercicio.grupoMuscular.takeIf { it.isNotBlank() },
                        "$series série${if (series > 1) "s" else ""}",
                        if (exercicio.tipoExecucao == "tempo") {
                            exercicio.tempoExecucao?.let { Time.formatMmSs(it) }
                        } else {
                            "${exercicio.repeticoes} reps"
                        },
                        if (exercicio.tipoExecucao == "tempo" && exercicio.descansoSegundos > 0) {
                            "descanso ${Time.formatMmSs(exercicio.descansoSegundos)}"
                        } else {
                            null
                        }
                    ).joinToString(" · ")
                    Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = pesos[exIndex].orEmpty(),
                        onValueChange = { pesos[exIndex] = it },
                        label = { Text("Peso (kg)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    repeat(series) { serieIndex ->
                        val key = "$exIndex-$serieIndex"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = feitos[key] == true,
                                onCheckedChange = { feitos[key] = it }
                            )
                            Text("Série ${serieIndex + 1}")
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val snapshot = exercicios.mapIndexed { exIndex, exercicio ->
                        val series = exercicio.series.coerceAtLeast(1)
                        ExecucaoExercicio(
                            nome = exercicio.nome,
                            grupoMuscular = exercicio.grupoMuscular,
                            series = series,
                            repeticoes = exercicio.repeticoes,
                            tipoExecucao = exercicio.tipoExecucao,
                            tempoExecucao = exercicio.tempoExecucao,
                            pesoKg = pesos[exIndex].orEmpty().trim(),
                            seriesFeitas = (0 until series).map { feitos["$exIndex-$it"] == true }
                        )
                    }
                    val now = Time.nowIso()
                    onSaveRealizado(
                        HistoricoTreinoEntity(
                            id = Time.newId(),
                            clienteId = treino.clienteId,
                            clienteNome = treino.clienteNome,
                            treinoId = treino.id,
                            treinoTitulo = treino.titulo,
                            dataExecucao = now,
                            exerciciosJson = JsonStore.encodeExecucoes(snapshot),
                            createdDate = now,
                            updatedDate = now
                        )
                    )
                },
                enabled = treino.clienteId.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar treino realizado") }
        }
    }
}
