package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.data.local.AulaAluno
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.util.Time
import java.util.Locale

@Composable
fun HomeScreen(
    aulas: List<AulaEntity>,
    clientes: List<ClienteEntity>,
    treinos: List<TreinoEntity>,
    transacoes: List<TransacaoEntity>,
    onCheckIn: (AulaEntity, AulaAluno) -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenAula: (String) -> Unit,
    onOpenFinanceiro: () -> Unit,
    onVisualizeTreino: (String) -> Unit
) {
    val hoje = Time.today()
    val aulasHoje = aulas.filter { it.data == hoje && it.status != "cancelada" }
    val receber = transacoes.filter { it.tipoTransacao == "aula_realizada" }.sumOf { it.valor }
    val recebido = transacoes.filter { it.tipoTransacao == "pagamento" }.sumOf { kotlin.math.abs(it.valor) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(Time.longToday().uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("${Time.greeting()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Confira o resumo do espaço e os próximos atendimentos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            InfoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    HomeStat("Alunos", clientes.count { it.status == "ativo" }.toString())
                    HomeStat("Treinos", treinos.size.toString())
                    HomeStat("Hoje", aulasHoje.size.toString())
                }
            }
        }
        item {
            InfoCard {
                Text("Resumo financeiro", fontWeight = FontWeight.SemiBold)
                Text("Saldo R$ ${"%.2f".format(Locale("pt", "BR"), receber - recebido)}", style = MaterialTheme.typography.titleMedium)
                Text("Recebido R$ ${"%.2f".format(Locale("pt", "BR"), recebido)} · A receber R$ ${"%.2f".format(Locale("pt", "BR"), receber)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenFinanceiro) { Text("Ver financeiro →") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Próximos atendimentos", style = MaterialTheme.typography.titleMedium)
                    Text("Agenda de hoje", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenAgenda) { Text("Abrir agenda →") }
            }
        }
        if (aulasHoje.isEmpty()) {
            item { EmptyState("Nenhuma aula agendada para hoje.") }
        }
        items(aulasHoje, key = { it.id }) { aula ->
            val alunos = JsonStore.decodeAlunos(aula.alunosJson)
            InfoCard {
                Text("${aula.horario} · ${if (aula.tipoAula == "online") "Online" else "Presencial"}", fontWeight = FontWeight.SemiBold)
                Text(alunos.joinToString { it.clienteNome }.ifBlank { "Sem alunos" })
                if (aula.local.isNotBlank()) Text(aula.local, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(8.dp))
                alunos.forEach { aluno ->
                    val treinoNome = treinos.find { it.id == aluno.treinoId }?.titulo
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(aluno.clienteNome)
                                if (!treinoNome.isNullOrBlank()) {
                                    Text(treinoNome, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            if (aluno.checkInRealizado) {
                                Text("Check-in ${aluno.checkInHorario.orEmpty()}", color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Button(onClick = { onCheckIn(aula, aluno) }) { Text("Check-in") }
                            }
                        }
                        if (!aluno.treinoId.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { onVisualizeTreino(aluno.treinoId.orEmpty()) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Visualizar treino") }
                        }
                    }
                }
                TextButton(onClick = { onOpenAula(aula.id) }) { Text("Editar aula") }
            }
        }
    }
}

@Composable
private fun HomeStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    aulas: List<AulaEntity>,
    treinos: List<TreinoEntity> = emptyList(),
    onAdd: () -> Unit,
    onOpen: (AulaEntity) -> Unit,
    onVisualizeTreino: (String) -> Unit = {},
    onDelete: (String) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Outlined.Add, contentDescription = "Nova aula")
            }
        }
    ) { padding ->
        if (aulas.isEmpty()) {
            EmptyState("Nenhuma aula cadastrada.", Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(aulas, key = { it.id }) { aula ->
                    val alunos = JsonStore.decodeAlunos(aula.alunosJson)
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${Time.formatDate(aula.data)} · ${aula.horario}", fontWeight = FontWeight.SemiBold)
                                Text(alunos.joinToString { it.clienteNome }.ifBlank { "Sem alunos" })
                                val treinosNomes = alunos.mapNotNull { aluno ->
                                    treinos.find { it.id == aluno.treinoId }?.titulo
                                }.distinct()
                                Text(
                                    buildString {
                                        append("${aula.status} · ${aula.tipoAula}")
                                        if (treinosNomes.isNotEmpty()) append(" · ${treinosNomes.joinToString()}")
                                    },
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(onClick = { onDelete(aula.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            alunos.mapNotNull { it.treinoId }.distinct().forEach { treinoId ->
                                val nome = treinos.find { it.id == treinoId }?.titulo ?: "Treino"
                                OutlinedButton(
                                    onClick = { onVisualizeTreino(treinoId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Visualizar $nome") }
                            }
                            OutlinedButton(onClick = { onOpen(aula) }, modifier = Modifier.fillMaxWidth()) { Text("Abrir") }
                        }
                    }
                }
            }
        }
    }
}
