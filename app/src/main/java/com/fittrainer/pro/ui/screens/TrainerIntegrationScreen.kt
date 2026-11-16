package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.fittrainer.pro.data.export.ClienteExport
import com.fittrainer.pro.data.assessoria.AssessoriaCsv
import com.fittrainer.pro.data.firebase.TrainerClientLink
import com.fittrainer.pro.data.firebase.TrainerFeedbackSession
import com.fittrainer.pro.data.firebase.TrainerFirebaseSync
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.local.PlanoAssessoriaEntity
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TrainerIntegrationScreen(
    cliente: ClienteEntity,
    treinos: List<TreinoEntity>,
    planosAssessoria: List<PlanoAssessoriaEntity>,
    onAccountReady: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(TrainerFirebaseSync.signedInName().orEmpty()) }
    var email by rememberSaveable { mutableStateOf(cliente.email) }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var selectedWorkout by rememberSaveable { mutableStateOf("") }
    var selectedPlan by rememberSaveable { mutableStateOf("") }
    var signedIn by remember { mutableStateOf(TrainerFirebaseSync.isSignedIn()) }
    var link by remember { mutableStateOf<TrainerClientLink?>(null) }
    var sessions by remember { mutableStateOf<List<TrainerFeedbackSession>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        link = TrainerFirebaseSync.loadLink(context, cliente.id)
        sessions = link?.let { TrainerFirebaseSync.feedback(it) }.orEmpty()
    }

    LaunchedEffect(cliente.id) {
        signedIn = TrainerFirebaseSync.isSignedIn()
        if (signedIn) runCatching { reload() }
            .onFailure { notice = "Não foi possível sincronizar agora: ${it.message ?: "verifique a conexão"}" }
    }

    fun runCloud(action: suspend () -> Unit): () -> Unit = {
        scope.launch {
            busy = true
            notice = null
            try {
                action()
            } catch (error: Exception) {
                notice = error.message ?: "Não foi possível concluir. Verifique a conexão e tente novamente."
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageHeader(
            eyebrow = "Integração com o app do aluno",
            title = cliente.nome,
            subtitle = "Vincule por código, envie treinos e acompanhe as execuções.",
        )
        if (!signedIn) {
            InfoCard {
                Text("Conta do treinador", fontWeight = FontWeight.SemiBold, color = FitColors.Ink, fontSize = 16.sp)
                OutlinedTextField(name, { name = it }, label = { Text("Nome do treinador") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
                OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(password, { password = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation())
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy, onClick = runCloud {
                        TrainerFirebaseSync.createAccount(name, email, password)
                        signedIn = true
                        onAccountReady()
                        reload()
                        notice = "Conta criada. Digite o código gerado no app do aluno."
                    }, modifier = Modifier.weight(1f)) { Text("Criar conta") }
                    OutlinedButton(enabled = !busy, onClick = runCloud {
                        TrainerFirebaseSync.signIn(name, email, password)
                        signedIn = true
                        onAccountReady()
                        reload()
                        notice = "Acesso realizado."
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Entrar") }
                }
                Text("As contas usam e-mail e senha do Firebase. Mínimo de 6 caracteres.", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            InfoCard {
                Text(TrainerFirebaseSync.signedInEmail().orEmpty(), color = FitColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                when (link?.status) {
                    "active" -> {
                        Text("App vinculado", color = FitColors.Green, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text("O aluno ${link?.clientName} aceitou o vínculo.", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !busy, onClick = runCloud { reload(); notice = "Dados atualizados." }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Atualizar retornos") }
                            OutlinedButton(enabled = !busy, onClick = runCloud { TrainerFirebaseSync.disconnect(link!!); link = null; sessions = emptyList(); notice = "Vínculo encerrado." }, shape = RoundedCornerShape(12.dp)) { Text("Desvincular") }
                        }
                    }
                    "pending" -> {
                        Text("Aguardando o aluno aceitar", color = FitColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text("O pedido foi enviado para ${link?.clientName}. Peça para abrir a tela Vínculo no app do aluno e aceitar.", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        OutlinedButton(enabled = !busy, onClick = runCloud { reload(); notice = if (link?.status == "active") "Vínculo confirmado." else "Ainda aguardando confirmação." }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp)) { Text("Verificar aceite") }
                    }
                    else -> {
                        Text("Vincular com código do aluno", color = FitColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text("Peça ao aluno o código de uso único exibido no app dele. O código expira em 15 minutos.", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        OutlinedTextField(code, { code = it.filterNot(Char::isWhitespace).uppercase().take(10) }, label = { Text("Código de 10 caracteres") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
                        Button(enabled = !busy && code.length == 10, onClick = runCloud {
                            link = TrainerFirebaseSync.claimStudentCode(
                                context,
                                cliente.id,
                                code,
                                name.ifBlank { TrainerFirebaseSync.signedInName().orEmpty() },
                            )
                            code = ""
                            notice = "Pedido enviado. O aluno precisa aceitar no app dele."
                        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Enviar pedido de vínculo") }
                    }
                }
            }

            if (link?.status == "active") {
                InfoCard {
                    Text("Enviar treino", color = FitColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    val clientWorkouts = treinos.filter { it.clienteId == cliente.id }
                    if (clientWorkouts.isEmpty()) {
                        Text("Ainda não há treinos cadastrados para este aluno.", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    } else {
                        val selectedWorkoutId = selectedWorkout.takeIf { id -> clientWorkouts.any { it.id == id } }
                            ?: clientWorkouts.first().id
                        SimpleDropdown(
                            "Treino do aluno",
                            selectedWorkoutId,
                            clientWorkouts.map { it.id to it.titulo },
                            { selectedWorkout = it },
                        )
                        Button(enabled = !busy, onClick = runCloud {
                            val treino = clientWorkouts.first { it.id == selectedWorkoutId }
                            val payload = ClienteExport.treinoAvulso(treino, cliente.id)
                                ?: error("Não foi possível montar este treino.")
                            TrainerFirebaseSync.publishPlan(context, link!!, "training-${cliente.id}-${LocalDate.now()}-${treino.id}", treino.titulo, payload)
                            notice = "Treino salvo no Firebase; será entregue na próxima sincronização do app do aluno."
                        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Enviar para o app") }
                    }
                }

                InfoCard {
                    Text("Enviar plano semanal", color = FitColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    val clientPlans = planosAssessoria.filter { it.clienteId == cliente.id }
                    if (clientPlans.isEmpty()) {
                        Text("Nenhum plano de assessoria foi associado a este aluno.", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    } else {
                        val selectedPlanId = selectedPlan.takeIf { id -> clientPlans.any { it.id == id } } ?: clientPlans.first().id
                        SimpleDropdown("Plano semanal", selectedPlanId, clientPlans.map { it.id to it.nome }, { selectedPlan = it })
                        Button(enabled = !busy, onClick = runCloud {
                            val plano = clientPlans.first { it.id == selectedPlanId }
                            val slotsByWeek = AssessoriaCsv.diasComTreino(plano).groupBy { it.semana }.toSortedMap()
                            check(slotsByWeek.isNotEmpty()) { "Este plano ainda não tem treinos associados." }
                            val primeiraSemana = slotsByWeek.keys.first()
                            slotsByWeek.forEach { (week, slots) ->
                                val weekStart = Time.mondayOf(Time.today()).plusWeeks((week - primeiraSemana).toLong()).toString()
                                val payload = ClienteExport.planoSemanal(plano, cliente.id, week, slots, treinos, weekStart)
                                    ?: error("Não foi possível montar a semana $week.")
                                val exportId = "training-${cliente.id}-$weekStart-${plano.id}-s$week"
                                TrainerFirebaseSync.publishPlan(context, link!!, exportId, "${plano.nome} · Semana $week", payload)
                            }
                            notice = "${slotsByWeek.size} semana(s) salvas para sincronização com o app do aluno."
                        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Enviar plano para o app") }
                    }
                }

                InfoCard {
                    Text("Mensagem para o aluno", color = FitColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    OutlinedTextField(title, { title = it.take(80) }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
                    OutlinedTextField(body, { body = it.take(2000) }, label = { Text("Mensagem") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3, maxLines = 6)
                    Button(enabled = !busy && body.isNotBlank(), onClick = runCloud {
                        TrainerFirebaseSync.sendMessage(link!!, title, body)
                        title = ""
                        body = ""
                        notice = "Mensagem salva para sincronização na caixa de entrada do aluno."
                    }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Enviar mensagem") }
                    Text("As mensagens aparecem no app quando o aluno abrir a tela Vínculo ou atualizar. Notificação em segundo plano requer servidor fora do plano gratuito atual.", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
                }

                InfoCard {
                    Text("Retornos de treino", color = FitColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    if (sessions.isEmpty()) Text("Os treinos concluídos pelo aluno aparecerão aqui.", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    sessions.forEach { session ->
                        Card(Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = FitColors.Canvas), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(formatDate(session.workoutDate), color = FitColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                session.startedAtMs?.let { started ->
                                    val completed = session.submittedAtMs
                                    Text(
                                        "Início ${formatTime(started)}${completed?.let { " · retorno ${formatTime(it)}" }.orEmpty()}",
                                        color = FitColors.Muted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 3.dp),
                                    )
                                }
                                session.exercises.forEach { exercise ->
                                    Text(
                                        "${exercise.name} · ${exercise.completedSets}/${exercise.plannedSets} séries${exercise.loadKg?.let { " · ${formatKg(it)} kg" } ?: ""}",
                                        color = FitColors.Muted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 5.dp),
                                    )
                                    exercise.sets.forEach { set ->
                                        val status = set.completedAtMs?.let { "Concluída às ${formatTime(it)}" }
                                            ?: set.startedAtMs?.let { "Iniciada às ${formatTime(it)}" }
                                            ?: "Não iniciada"
                                        Text("  Série ${set.number} · $status", color = FitColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
                                    }
                                }
                                session.notes?.takeIf { it.isNotBlank() }?.let {
                                    Text("Comentário: $it", color = FitColors.Ink, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(enabled = !busy, onClick = { TrainerFirebaseSync.signOut(); signedIn = false; link = null; sessions = emptyList() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Sair da conta") }
        }
        notice?.let { Text(it, color = FitColors.GreenDark, fontSize = 12.sp) }
        if (busy) Text("Conectando…", color = FitColors.Muted, fontSize = 12.sp)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Voltar aos alunos") }
        Spacer(Modifier.height(12.dp))
    }
}

private fun formatDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(value)

private fun formatTime(value: Long): String = runCatching {
    java.time.Instant.ofEpochMilli(value).atZone(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("--:--")

private fun formatKg(value: Double): String = "%.1f".format(java.util.Locale("pt", "BR"), value).trimEnd('0').trimEnd(',')
