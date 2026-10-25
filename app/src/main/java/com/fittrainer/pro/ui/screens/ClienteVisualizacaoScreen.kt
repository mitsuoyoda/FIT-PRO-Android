package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.HistoricoTreinoEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteVisualizacaoScreen(
    cliente: ClienteEntity?,
    historico: List<HistoricoTreinoEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cliente?.nome ?: "Aluno") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (cliente == null) {
            EmptyState("Aluno não encontrado.", Modifier.padding(padding))
            return@Scaffold
        }
        val realizados = historico.filter { it.clienteId == cliente.id }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard {
                Text("Dados do aluno", fontWeight = FontWeight.SemiBold)
                Dado("E-mail", cliente.email)
                Dado("Telefone", cliente.telefone)
                Dado("Nascimento", cliente.dataNascimento)
                Dado("Sexo", cliente.sexo)
                Dado("Altura", cliente.altura?.let { "$it cm" }.orEmpty())
                Dado("Peso atual", cliente.pesoAtual?.let { "$it kg" }.orEmpty())
                Dado("Objetivo", cliente.objetivo)
                Dado("Status", cliente.status)
                Dado("Valor da aula", cliente.valorAula?.let { "R$ $it" }.orEmpty())
                Dado("Local padrão", cliente.localAulaPadrao)
                Dado("Observações", cliente.observacoes)
            }
            Text("Treinos realizados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (realizados.isEmpty()) {
                Text("Nenhum treino realizado salvo para este aluno.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                realizados.forEach { item ->
                    val execucoes = JsonStore.decodeExecucoes(item.exerciciosJson)
                    InfoCard {
                        Text(item.treinoTitulo.ifBlank { "Treino" }, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Realizado em ${Time.formatDate(item.dataExecucao)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        execucoes.forEach { ex ->
                            val feitas = ex.seriesFeitas.count { it }
                            Text(
                                buildString {
                                    append(ex.nome.ifBlank { "Exercício" })
                                    append(" · $feitas/${ex.series} séries")
                                    if (ex.pesoKg.isNotBlank()) append(" · ${ex.pesoKg} kg")
                                    if (ex.tipoExecucao == "tempo") {
                                        ex.tempoExecucao?.let { append(" · ${Time.formatMmSs(it)}") }
                                    } else if (ex.repeticoes.isNotBlank()) {
                                        append(" · ${ex.repeticoes} reps")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dado(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
