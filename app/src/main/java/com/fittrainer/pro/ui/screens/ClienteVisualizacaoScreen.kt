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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.HistoricoTreinoEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.InitialsAvatar
import com.fittrainer.pro.ui.components.StatusChip
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteVisualizacaoScreen(
    cliente: ClienteEntity?,
    historico: List<HistoricoTreinoEntity>,
    transacoes: List<TransacaoEntity> = emptyList(),
    avaliacoes: List<AvaliacaoEntity> = emptyList(),
    onBack: () -> Unit,
    onVisualizarAvaliacao: (AvaliacaoEntity) -> Unit = {},
    onEditarAvaliacao: (AvaliacaoEntity) -> Unit = {},
    onExcluirAvaliacao: (String) -> Unit = {},
    onExportarApp: () -> Unit = {}
) {
    var excluirAvaliacaoId by remember { mutableStateOf<String?>(null) }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InitialsAvatar(cliente.nome)
                    Column(Modifier.weight(1f)) {
                        Text(cliente.nome, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = FitColors.Ink)
                        Text(cliente.objetivo.ifBlank { "Sem objetivo" }, fontSize = 12.sp, color = FitColors.Muted)
                    }
                    StatusChip(if (cliente.status == "ativo") "Ativo" else "Inativo")
                }
            }
            Button(onClick = onExportarApp, modifier = Modifier.fillMaxWidth()) {
                Text("Exportar dados para o app do aluno")
            }
            InfoCard {
                Text("Dados do aluno", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
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
            val lancamentos = transacoes.filter { it.clienteId == cliente.id }
            val aPagar = lancamentos.filter { it.tipoTransacao == "aula_realizada" }.sumOf { it.valor }
            val recebido = lancamentos.filter { it.tipoTransacao == "pagamento" }.sumOf { kotlin.math.abs(it.valor) }
            Text("Financeiro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            InfoCard {
                Dado("Valor por aula", cliente.valorAula?.let { Time.money(it) }.orEmpty())
                Dado("A pagar", Time.money(aPagar))
                Dado("Recebido", Time.money(recebido))
                Dado("Saldo", Time.money(aPagar - recebido))
            }
            lancamentos.forEach { item ->
                InfoCard {
                    Text(
                        if (item.tipoTransacao == "aula_realizada") "Aula a pagar" else "Pagamento",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(item.referencia.ifBlank { Time.formatDate(item.dataTransacao) })
                    Text(Time.money(kotlin.math.abs(item.valor)))
                }
            }
            Text("Avaliações físicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (avaliacoes.isEmpty()) {
                Text("Nenhuma avaliação neste aluno.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                avaliacoes.sortedByDescending { it.dataAvaliacao }.forEach { item ->
                    AvaliacaoCard(
                        item = item,
                        onVisualize = { onVisualizarAvaliacao(item) },
                        onEdit = { onEditarAvaliacao(item) },
                        onDelete = { excluirAvaliacaoId = item.id }
                    )
                }
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
                                    if (ex.setNome.isNotBlank()) append("${ex.setNome} · ")
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
    excluirAvaliacaoId?.let { id ->
        AlertDialog(
            onDismissRequest = { excluirAvaliacaoId = null },
            title = { Text("Excluir avaliação?") },
            text = { Text("Este lançamento sai da ficha do aluno e não pode ser desfeito neste aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    onExcluirAvaliacao(id)
                    excluirAvaliacaoId = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { excluirAvaliacaoId = null }) { Text("Cancelar") }
            }
        )
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
