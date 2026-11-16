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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.data.local.AulaAluno
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.ui.components.CheckInRow
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.EnviarTreinoWhatsAppButton
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.InitialsAvatar
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.StatTile
import com.fittrainer.pro.ui.components.StatusChip
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time

@Composable
fun HomeScreen(
    aulas: List<AulaEntity>,
    clientes: List<ClienteEntity>,
    treinos: List<TreinoEntity>,
    transacoes: List<TransacaoEntity>,
    onPedirAPagar: (AulaEntity, AulaAluno, Boolean) -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenAula: (String) -> Unit,
    onOpenFinanceiro: () -> Unit,
    onVisualizeTreino: (String) -> Unit,
    onRegistrarEnvioTreino: (TreinoEntity) -> Unit = {}
) {
    val hoje = Time.today()
    val aulasHoje = aulas.filter { it.data == hoje && it.status != "cancelada" }.sortedBy { it.horario }
    val proximaAula = aulas
        .filter { it.status != "cancelada" && it.data >= hoje }
        .minWithOrNull(compareBy<AulaEntity> { it.data }.thenBy { it.horario })
    val outrasAulasHoje = aulasHoje.filterNot { it.id == proximaAula?.id }
    val receber = transacoes.filter { it.tipoTransacao == "aula_realizada" }.sumOf { it.valor }
    val recebido = transacoes.filter { it.tipoTransacao == "pagamento" }.sumOf { kotlin.math.abs(it.valor) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                eyebrow = Time.longToday(),
                title = Time.greeting(),
                subtitle = "Seu espaço e os próximos atendimentos em um só lugar."
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Próximo atendimento", style = MaterialTheme.typography.titleMedium, color = FitColors.Ink)
                if (proximaAula == null) {
                    InfoCard {
                        Text("Sua agenda está livre", style = MaterialTheme.typography.titleSmall, color = FitColors.Ink)
                        Text("Quando uma aula for marcada, ela aparece aqui.", style = MaterialTheme.typography.bodyMedium, color = FitColors.Muted, modifier = Modifier.padding(top = 4.dp))
                        Button(onClick = onOpenAgenda, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Text("Abrir agenda", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    AppointmentCard(
                        aula = proximaAula,
                        clientes = clientes,
                        treinos = treinos,
                        onPedirAPagar = onPedirAPagar,
                        onVisualizeTreino = onVisualizeTreino,
                        onRegistrarEnvioTreino = onRegistrarEnvioTreino,
                        onOpen = { onOpenAula(proximaAula.id) }
                    )
                }
            }
        }
        item {
            InfoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Agenda de hoje", style = MaterialTheme.typography.titleMedium, color = FitColors.Ink)
                        Text("${aulasHoje.size} atendimento(s)", style = MaterialTheme.typography.bodySmall, color = FitColors.Muted)
                    }
                    TextButton(onClick = onOpenAgenda) { Text("Ver agenda →") }
                }
                when {
                    aulasHoje.isEmpty() -> Text("Nenhuma aula agendada para hoje.", style = MaterialTheme.typography.bodyMedium, color = FitColors.Muted, modifier = Modifier.padding(top = 12.dp))
                    outrasAulasHoje.isEmpty() -> Text("O próximo atendimento está destacado acima.", style = MaterialTheme.typography.bodySmall, color = FitColors.Muted, modifier = Modifier.padding(top = 12.dp))
                    else -> outrasAulasHoje.forEach { aula ->
                        val alunos = JsonStore.decodeAlunos(aula.alunosJson)
                        val nome = alunos.joinToString { it.nomeExibicao(clientes) }.ifBlank { "Sem alunos" }
                        val treino = alunos.mapNotNull { it.treinoExibicao(treinos) }.firstOrNull()
                        val pago = alunos.any { it.checkInRealizado }
                        MiniAppointmentRow(
                            horario = aula.horario,
                            nome = nome,
                            treino = treino,
                            status = if (pago) "A pagar" else if (aula.status == "agendada") "Confirmado" else aula.status,
                            onOpen = { onOpenAula(aula.id) }
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Resumo", style = MaterialTheme.typography.titleMedium, color = FitColors.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Alunos ativos", clientes.count { it.status == "ativo" }.toString(), Icons.Outlined.People, Modifier.weight(1f))
                    StatTile("Aulas hoje", aulasHoje.size.toString(), Icons.Outlined.CalendarViewMonth, Modifier.weight(1f))
                }
            }
        }
        item {
            InfoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Resumo financeiro", style = MaterialTheme.typography.titleMedium, color = FitColors.Ink)
                        Text("Valores registrados neste espaço", style = MaterialTheme.typography.bodySmall, color = FitColors.Muted)
                    }
                    TextButton(onClick = onOpenFinanceiro) { Text("Ver detalhes →") }
                }
                Text("Saldo do mês", style = MaterialTheme.typography.bodySmall, color = FitColors.Muted, modifier = Modifier.padding(top = 10.dp))
                Text(Time.money(receber - recebido), style = MaterialTheme.typography.headlineSmall, color = FitColors.Ink)
                FinanceRow("Receitas recebidas", Time.money(recebido))
                FinanceRow("A receber", Time.money(receber), highlight = true)
            }
        }
    }
}

@Composable
private fun FinanceRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = FitColors.Muted, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, color = if (highlight) FitColors.Orange else FitColors.Ink)
    }
}

@Composable
private fun MiniAppointmentRow(
    horario: String,
    nome: String,
    treino: String?,
    status: String,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(horario, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.width(48.dp), color = FitColors.Ink)
        Column(Modifier.weight(1f)) {
            Text(nome, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FitColors.Ink)
            if (!treino.isNullOrBlank()) Text(treino, fontSize = 12.sp, color = FitColors.Muted)
        }
        StatusChip(status)
        TextButton(onClick = onOpen) { Text("Abrir") }
    }
}

@Composable
private fun AppointmentCard(
    aula: AulaEntity,
    clientes: List<ClienteEntity>,
    treinos: List<TreinoEntity>,
    onPedirAPagar: (AulaEntity, AulaAluno, Boolean) -> Unit,
    onVisualizeTreino: (String) -> Unit,
    onRegistrarEnvioTreino: (TreinoEntity) -> Unit = {},
    onOpen: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val alunos = JsonStore.decodeAlunos(aula.alunosJson)
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                aula.horario,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = FitColors.Ink,
                modifier = Modifier.width(64.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (alunos.isEmpty()) {
                    Text("Sem alunos", color = FitColors.Muted)
                } else {
                    alunos.forEach { aluno ->
                        val nome = aluno.nomeExibicao(clientes).ifBlank { "Aluno" }
                        val treinoNome = aluno.treinoExibicao(treinos)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InitialsAvatar(nome)
                            Column(Modifier.weight(1f)) {
                                Text(nome, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FitColors.Ink)
                                Text(Time.formatDate(aula.data), fontSize = 12.sp, color = FitColors.Muted)
                            }
                        }
                        if (!treinoNome.isNullOrBlank()) {
                            Column {
                                Text("TREINO VINCULADO", fontSize = 12.sp, letterSpacing = 0.6.sp, color = FitColors.Muted)
                                Text(treinoNome, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                            }
                        }
                        CheckInRow(
                            nome = "A pagar",
                            checked = aluno.checkInRealizado,
                            detalhe = if (aluno.checkInRealizado) {
                                "Lançado ${aluno.checkInHorario.orEmpty()}".trim()
                            } else {
                                "Toque para lançar a dívida"
                            },
                            onToggle = { onPedirAPagar(aula, aluno, it) }
                        )
                        if (!aluno.treinoId.isNullOrBlank()) {
                            val treinoVinculado = treinos.find { it.id == aluno.treinoId }
                            Button(
                                onClick = { onVisualizeTreino(aluno.treinoId.orEmpty()) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Visualizar treino") }
                            EnviarTreinoWhatsAppButton(
                                treino = treinoVinculado,
                                clientes = clientes,
                                clienteIdPreferido = aluno.clienteId,
                                clienteNomePreferido = aluno.nomeExibicao(clientes),
                                modifier = Modifier.fillMaxWidth(),
                                onRegistrarEnvio = onRegistrarEnvioTreino
                            )
                        }
                    }
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("Abrir") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    aulas: List<AulaEntity>,
    clientes: List<ClienteEntity> = emptyList(),
    treinos: List<TreinoEntity> = emptyList(),
    onAdd: () -> Unit,
    onOpen: (AulaEntity) -> Unit,
    onVisualizeTreino: (String) -> Unit = {},
    onPedirAPagar: (AulaEntity, AulaAluno, Boolean) -> Unit = { _, _, _ -> },
    onRegistrarEnvioTreino: (TreinoEntity) -> Unit = {},
    onDelete: (String) -> Unit
) {
    val hoje = Time.today()
    val aulasHoje = aulas.count { it.data == hoje && it.status != "cancelada" }
    val vinculados = aulas.count { JsonStore.decodeAlunos(it.alunosJson).any { aluno -> !aluno.treinoId.isNullOrBlank() } }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FitColors.Canvas,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = FitColors.Green) {
                Icon(Icons.Outlined.Add, contentDescription = "Nova aula")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PageHeader(
                    eyebrow = "",
                    title = "Agenda e treinos",
                    subtitle = "Acesse rapidamente o treino vinculado a cada atendimento."
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Atendimentos de hoje", aulasHoje.toString(), Icons.Outlined.CalendarViewMonth)
                    StatTile("Treinos salvos", treinos.size.toString(), Icons.Outlined.FitnessCenter)
                    StatTile("Treinos vinculados", "$vinculados/${aulas.size.coerceAtLeast(1)}", Icons.Outlined.Check)
                }
            }
            item {
                Text("Hoje", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = FitColors.Ink)
                Text("Selecione “Visualizar treino” para abrir o plano do aluno.", fontSize = 12.sp, color = FitColors.Muted)
            }
            if (aulas.isEmpty()) {
                item { EmptyState("Nenhuma aula cadastrada.") }
            } else {
                items(aulas, key = { it.id }) { aula ->
                    AppointmentCard(
                        aula = aula,
                        clientes = clientes,
                        treinos = treinos,
                        onPedirAPagar = onPedirAPagar,
                        onVisualizeTreino = onVisualizeTreino,
                        onRegistrarEnvioTreino = onRegistrarEnvioTreino,
                        onOpen = { onOpen(aula) },
                        onDelete = { onDelete(aula.id) }
                    )
                }
            }
        }
    }
}
