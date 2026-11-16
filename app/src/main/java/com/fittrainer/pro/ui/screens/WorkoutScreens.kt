package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import com.fittrainer.pro.data.assessoria.WhatsAppShare
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.data.catalog.ExerciseCatalog
import com.fittrainer.pro.data.catalog.ExerciseCatalogItem
import com.fittrainer.pro.data.local.AulaAluno
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.ExercicioBibliotecaEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.local.WorkoutExercise
import com.fittrainer.pro.data.local.WorkoutSet
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.ui.components.CheckInRow
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.EnviarTreinoWhatsAppButton
import com.fittrainer.pro.ui.components.ExerciseInfoDialog
import com.fittrainer.pro.ui.components.ExerciseNameField
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.ui.components.StatusChip
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreinosScreen(
    treinos: List<TreinoEntity>,
    clientes: List<ClienteEntity> = emptyList(),
    onAdd: () -> Unit,
    onOpen: (TreinoEntity) -> Unit,
    onVisualize: (TreinoEntity) -> Unit,
    onDelete: (String) -> Unit,
    onRegistrarEnvio: (TreinoEntity) -> Unit = {},
    onExportarApp: (TreinoEntity) -> Unit = {},
    onEnviarApp: (TreinoEntity) -> Unit = {}
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FitColors.Canvas,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = FitColors.Green) {
                Icon(Icons.Outlined.Add, contentDescription = "Novo treino")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                PageHeader(
                    eyebrow = "Biblioteca",
                    title = "Treinos salvos",
                    subtitle = "Abra o plano criado para cada aluno e visualize os exercícios."
                )
            }
            if (treinos.isEmpty()) {
                item { EmptyState("Crie um treino para um aluno. Ele fica só neste aparelho.") }
            } else {
                items(treinos, key = { it.id }) { treino ->
                    val sets = JsonStore.decodeSets(treino.exerciciosJson)
                    val exercicios = sets.flatMap { it.exercicios }
                    val aluno = WhatsAppShare.clienteDoCadastro(clientes, treino.clienteId, treino.clienteNome)
                    val telefone = aluno?.telefone?.takeIf { it.isNotBlank() }
                    InfoCard {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(treino.titulo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = FitColors.Ink)
                                Text(
                                    when {
                                        aluno != null && telefone != null -> "${aluno.nome} · $telefone"
                                        aluno != null -> "${aluno.nome} · sem telefone no cadastro"
                                        treino.clienteNome.isBlank() -> "Sem aluno"
                                        else -> treino.clienteNome
                                    },
                                    fontSize = 12.sp,
                                    color = FitColors.Muted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusChip(treino.tipo.replaceFirstChar { it.uppercase() })
                                if (treino.dataEnvio.isNotBlank()) {
                                    StatusChip("Enviado")
                                }
                            }
                            IconButton(onClick = { onDelete(treino.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                            }
                        }
                        if (treino.descricao.isNotBlank()) {
                            Text(
                                treino.descricao,
                                fontSize = 12.sp,
                                color = FitColors.Muted,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                            )
                        }
                        Text(
                            buildString {
                                append("${sets.size} SET${if (sets.size == 1) "" else "s"}")
                                append(" · ${exercicios.size} exercício${if (exercicios.size == 1) "" else "s"}")
                                append(" · ${treino.duracaoEstimada} min")
                                if (treino.dataEnvio.isNotBlank()) {
                                    append(" · enviado ${Time.formatDateTime(treino.dataEnvio)}")
                                }
                            },
                            fontSize = 12.sp,
                            color = FitColors.NavIdle
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            OutlinedButton(onClick = { onVisualize(treino) }, modifier = Modifier.weight(1f)) { Text("Visualizar") }
                            OutlinedButton(onClick = { onOpen(treino) }, modifier = Modifier.weight(1f)) { Text("Editar") }
                        }
                        EnviarTreinoWhatsAppButton(
                            treino = treino,
                            clientes = clientes,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            onRegistrarEnvio = onRegistrarEnvio
                        )
                        OutlinedButton(
                            onClick = { onExportarApp(treino) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("Exportar JSON") }
                        OutlinedButton(
                            onClick = { onEnviarApp(treino) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("Enviar ao app do aluno") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreinoFormScreen(
    existing: TreinoEntity?,
    clientes: List<ClienteEntity>,
    biblioteca: List<ExercicioBibliotecaEntity> = emptyList(),
    onBack: () -> Unit,
    onVisualize: ((String) -> Unit)? = null,
    onSave: (TreinoEntity) -> Unit,
    onRegistrarEnvio: (TreinoEntity) -> Unit = {}
) {
    val ativos = clientes.filter { it.status == "ativo" }
    var titulo by rememberSaveable { mutableStateOf(existing?.titulo.orEmpty()) }
    var descricao by rememberSaveable { mutableStateOf(existing?.descricao.orEmpty()) }
    var clienteId by rememberSaveable { mutableStateOf(existing?.clienteId.orEmpty()) }
    var tipo by rememberSaveable { mutableStateOf(existing?.tipo ?: "forca") }
    var duracao by rememberSaveable { mutableStateOf((existing?.duracaoEstimada ?: 60).toString()) }
    var sets by remember(existing?.id) {
        mutableStateOf(
            JsonStore.decodeSets(existing?.exerciciosJson.orEmpty()).ifEmpty {
                listOf(WorkoutSet(id = Time.newId(), exercicios = listOf(WorkoutExercise()), descansoSegundos = 60))
            }
        )
    }
    val temposDigitados = remember { mutableStateMapOf<String, String>() }
    val descansosDigitados = remember { mutableStateMapOf<Int, String>() }
    var detalhe by remember { mutableStateOf<ExerciseCatalogItem?>(null) }
    var confirmarEnvio by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val alunoCadastro = WhatsAppShare.clienteDoCadastro(clientes, clienteId, existing?.clienteNome.orEmpty())

    fun jsonDosSets(lista: List<WorkoutSet> = sets): String = JsonStore.encodeSets(
        lista.map { set ->
            set.copy(
                id = set.id.ifBlank { Time.newId() },
                exercicios = set.exercicios.filter { it.nome.isNotBlank() }.map { it.copy(descansoSegundos = 0) }
            )
        }.filter { it.exercicios.isNotEmpty() }
    )

    fun atualizarSet(setIndex: Int, transform: (WorkoutSet) -> WorkoutSet) {
        sets = sets.toMutableList().also { it[setIndex] = transform(it[setIndex]) }
    }

    fun atualizarEx(setIndex: Int, exIndex: Int, transform: (WorkoutExercise) -> WorkoutExercise) {
        atualizarSet(setIndex) { set ->
            set.copy(exercicios = set.exercicios.toMutableList().also { it[exIndex] = transform(it[exIndex]) })
        }
    }

    fun aplicarCatalogo(setIndex: Int, exIndex: Int, item: ExerciseCatalogItem) {
        atualizarEx(setIndex, exIndex) { atual ->
            atual.copy(
                nome = item.displayName,
                grupoMuscular = item.grupo,
                observacoes = item.secundarios,
                catalogId = item.id,
                videoPrincipal = item.video,
                videoAlternativo = item.videoAlt
            )
        }
        detalhe = item
    }

    fun abrirDetalhe(item: WorkoutExercise) {
        detalhe = ExerciseCatalog.findMerged(context, biblioteca, item.catalogId, item.nome)
            ?: ExerciseCatalogItem(
                nomePt = item.nome,
                grupo = item.grupoMuscular,
                secundarios = item.observacoes,
                video = item.videoPrincipal,
                videoAlt = item.videoAlternativo
            )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Novo treino" else "Editar treino") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(titulo, { titulo = it }, label = { Text("Título *") }, modifier = Modifier.fillMaxWidth())
            SimpleDropdown(
                "Aluno",
                clienteId,
                listOf("" to "Selecione") + ativos.map { it.id to it.nome }
            ) { clienteId = it }
            SimpleDropdown(
                "Tipo",
                tipo,
                listOf("forca" to "Força", "hipertrofia" to "Hipertrofia", "cardio" to "Cardio", "funcional" to "Funcional")
            ) { tipo = it }
            OutlinedTextField(duracao, { duracao = it }, label = { Text("Duração (min)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(descricao, { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
            Text("SETs", style = MaterialTheme.typography.titleMedium)
            Text(
                "Cada SET agrupa um ou mais exercícios em sequência. O descanso fica só no final do SET.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            sets.forEachIndexed { setIndex, set ->
                InfoCard {
                    Text(set.titulo(setIndex), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        set.nome,
                        { valor -> atualizarSet(setIndex) { it.copy(nome = valor) } },
                        label = { Text("Nome do SET (opcional)") },
                        placeholder = { Text(set.titulo(setIndex)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    set.exercicios.forEachIndexed { exIndex, item ->
                        val chaveTempo = "$setIndex-$exIndex"
                        ExerciseNameField(
                            value = item.nome,
                            onValueChange = { valor -> atualizarEx(setIndex, exIndex) { it.copy(nome = valor) } },
                            onPick = { catalogo -> aplicarCatalogo(setIndex, exIndex, catalogo) },
                            onShowInfo = { abrirDetalhe(item) },
                            hasInfo = item.nome.isNotBlank(),
                            biblioteca = biblioteca
                        )
                        if (item.grupoMuscular.isNotBlank()) {
                            Text(item.grupoMuscular, color = MaterialTheme.colorScheme.outline)
                        }
                        val porTempo = item.tipoExecucao == "tempo"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Repetição",
                                fontWeight = if (!porTempo) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!porTempo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                            Switch(
                                checked = porTempo,
                                onCheckedChange = { checked ->
                                    atualizarEx(setIndex, exIndex) {
                                        it.copy(tipoExecucao = if (checked) "tempo" else "repeticao")
                                    }
                                }
                            )
                            Text(
                                "Tempo",
                                fontWeight = if (porTempo) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (porTempo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                item.series.toString(),
                                { valor -> atualizarEx(setIndex, exIndex) { it.copy(series = valor.toIntOrNull() ?: 1) } },
                                label = { Text("Séries") },
                                modifier = Modifier.weight(1f)
                            )
                            if (porTempo) {
                                OutlinedTextField(
                                    temposDigitados[chaveTempo]
                                        ?: item.tempoExecucao?.let { Time.formatMmSs(it) }.orEmpty(),
                                    { valor ->
                                        val sanitizado = Time.sanitizeMmSs(valor)
                                        temposDigitados[chaveTempo] = sanitizado
                                        atualizarEx(setIndex, exIndex) { it.copy(tempoExecucao = Time.parseMmSs(sanitizado)) }
                                    },
                                    label = { Text("Tempo (mm:ss)") },
                                    placeholder = { Text("00:45") },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                OutlinedTextField(
                                    item.repeticoes,
                                    { valor -> atualizarEx(setIndex, exIndex) { it.copy(repeticoes = valor) } },
                                    label = { Text("Reps") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (set.exercicios.size > 1) {
                            TextButtonDelete("Remover exercício") {
                                atualizarSet(setIndex) {
                                    it.copy(exercicios = it.exercicios.filterIndexed { i, _ -> i != exIndex })
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            atualizarSet(setIndex) { it.copy(exercicios = it.exercicios + WorkoutExercise()) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Adicionar exercício neste SET") }
                    OutlinedTextField(
                        descansosDigitados[setIndex] ?: Time.formatMmSs(set.descansoSegundos),
                        { valor ->
                            val sanitizado = Time.sanitizeMmSs(valor)
                            descansosDigitados[setIndex] = sanitizado
                            atualizarSet(setIndex) { it.copy(descansoSegundos = Time.parseMmSs(sanitizado) ?: 0) }
                        },
                        label = { Text("Descanso após o SET (mm:ss)") },
                        placeholder = { Text("01:00") },
                        supportingText = { Text("O descanso acontece só depois de concluir todos os exercícios deste SET.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (sets.size > 1) {
                        TextButtonDelete("Remover SET") {
                            sets = sets.filterIndexed { i, _ -> i != setIndex }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    sets = sets + WorkoutSet(
                        id = Time.newId(),
                        exercicios = listOf(WorkoutExercise()),
                        descansoSegundos = 60
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Adicionar SET") }
            if (existing != null && onVisualize != null) {
                OutlinedButton(
                    onClick = { onVisualize(existing.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Visualizar treino") }
            }
            if (existing != null && sets.any { set -> set.exercicios.any { it.nome.isNotBlank() } }) {
                OutlinedButton(
                    onClick = {
                        val snapshot = existing.copy(
                            titulo = titulo.trim(),
                            clienteId = clienteId,
                            clienteNome = alunoCadastro?.nome.orEmpty(),
                            exerciciosJson = jsonDosSets()
                        )
                        if (WhatsAppShare.enviarTreino(context, snapshot, clientes, alunoCadastro?.id, alunoCadastro?.nome.orEmpty())) {
                            confirmarEnvio = true
                        } else {
                            Toast.makeText(context, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Compartilhar arquivo") }
            }
            Button(
                onClick = {
                    val cliente = clientes.find { it.id == clienteId }
                    val now = Time.nowIso()
                    onSave(
                        TreinoEntity(
                            id = existing?.id ?: Time.newId(),
                            titulo = titulo.trim(),
                            descricao = descricao.trim(),
                            clienteId = clienteId,
                            clienteNome = cliente?.nome.orEmpty(),
                            tipo = tipo,
                            duracaoEstimada = duracao.toIntOrNull() ?: 60,
                            exerciciosJson = jsonDosSets(),
                            dataEnvio = existing?.dataEnvio ?: Time.today(),
                            createdDate = existing?.createdDate ?: now,
                            updatedDate = now
                        )
                    )
                },
                enabled = titulo.isNotBlank() && sets.any { set -> set.exercicios.any { it.nome.isNotBlank() } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar no aparelho") }
        }
    }

    detalhe?.let { catalogo ->
        ExerciseInfoDialog(item = catalogo, onDismiss = { detalhe = null })
    }
    if (confirmarEnvio && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmarEnvio = false },
            title = { Text("O arquivo foi compartilhado?") },
            text = { Text("Só registramos o envio se você confirmar. Escolha o app no seletor do sistema.") },
            confirmButton = {
                TextButton(onClick = {
                    onRegistrarEnvio(existing.copy(dataEnvio = Time.nowIso(), updatedDate = Time.nowIso()))
                    confirmarEnvio = false
                }) { Text("Sim, registrar") }
            },
            dismissButton = { TextButton(onClick = { confirmarEnvio = false }) { Text("Não") } }
        )
    }
}

@Composable
private fun TextButtonDelete(label: String = "Remover exercício", onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(label) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AulaFormScreen(
    existing: AulaEntity?,
    clientes: List<ClienteEntity>,
    treinos: List<TreinoEntity>,
    onBack: () -> Unit,
    onVisualizeTreino: (String) -> Unit = {},
    onPedirAPagar: (String, AulaAluno, Boolean) -> Unit = { _, _, _ -> },
    onRegistrarEnvioTreino: (TreinoEntity) -> Unit = {},
    onSave: (AulaEntity) -> Unit
) {
    var data by rememberSaveable { mutableStateOf(existing?.data ?: Time.today()) }
    var horario by rememberSaveable { mutableStateOf(existing?.horario ?: "09:00") }
    var tipo by rememberSaveable { mutableStateOf(existing?.tipoAula ?: "presencial") }
    var status by rememberSaveable { mutableStateOf(existing?.status ?: "agendada") }
    var duracao by rememberSaveable { mutableStateOf((existing?.duracaoMinutos ?: 60).toString()) }
    var local by rememberSaveable { mutableStateOf(existing?.local.orEmpty()) }
    var link by rememberSaveable { mutableStateOf(existing?.linkOnline.orEmpty()) }
    var observacoes by rememberSaveable { mutableStateOf(existing?.observacoes.orEmpty()) }
    var alunos by remember(existing?.id) {
        mutableStateOf(existing?.let { JsonStore.decodeAlunos(it.alunosJson) } ?: emptyList())
    }
    var alunoSel by rememberSaveable { mutableStateOf("") }
    var treinoSel by rememberSaveable { mutableStateOf("") }
    val aulaSalva = existing != null

    LaunchedEffect(existing?.id, existing?.alunosJson) {
        val gravados = existing?.let { JsonStore.decodeAlunos(it.alunosJson) }.orEmpty()
        if (gravados.isEmpty()) return@LaunchedEffect
        if (alunos.isEmpty()) {
            alunos = gravados
            return@LaunchedEffect
        }
        val mesclados = alunos.map { local ->
            val gravado = gravados.find { it.clienteId == local.clienteId } ?: return@map local
            local.copy(
                clienteNome = local.clienteNome.ifBlank { gravado.clienteNome },
                treinoId = local.treinoId ?: gravado.treinoId,
                treinoTitulo = local.treinoTitulo ?: gravado.treinoTitulo,
                checkInRealizado = gravado.checkInRealizado,
                checkInHorario = gravado.checkInHorario,
                valorAulaRegistrado = gravado.valorAulaRegistrado
            )
        }
        alunos = mesclados + gravados.filter { g -> mesclados.none { it.clienteId == g.clienteId } }
    }

    val ativos = clientes.filter { it.status == "ativo" }
    val treinosDoAlunoSel = treinos.filter { it.clienteId == alunoSel }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nova aula" else "Editar aula") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(data, { data = it }, label = { Text("Data (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(horario, { horario = it }, label = { Text("Horário (HH:MM)") }, modifier = Modifier.fillMaxWidth())
            SimpleDropdown("Tipo", tipo, listOf("presencial" to "Presencial", "online" to "Online")) { tipo = it }
            SimpleDropdown("Status", status, listOf("agendada" to "Agendada", "cancelada" to "Cancelada", "concluida" to "Concluída")) { status = it }
            OutlinedTextField(duracao, { duracao = it }, label = { Text("Duração (min)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(local, { local = it }, label = { Text("Local") }, modifier = Modifier.fillMaxWidth())
            if (tipo == "online") {
                OutlinedTextField(link, { link = it }, label = { Text("Link") }, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(observacoes, { observacoes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())
            Text("Alunos", style = MaterialTheme.typography.titleMedium)
            SimpleDropdown("Adicionar aluno", alunoSel, listOf("" to "Selecione") + ativos.map { it.id to it.nome }) {
                alunoSel = it
                treinoSel = ""
            }
            if (alunoSel.isNotBlank()) {
                SimpleDropdown(
                    "Treino do aluno (opcional)",
                    treinoSel,
                    listOf("" to "Nenhum") + treinosDoAlunoSel.map { it.id to it.titulo }
                ) { treinoSel = it }
                if (treinosDoAlunoSel.isEmpty()) {
                    Text(
                        "Este aluno ainda não tem treino cadastrado.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    val cliente = (ativos + clientes).find { it.id == alunoSel } ?: return@OutlinedButton
                    val treino = treinos.find { it.id == treinoSel }
                    alunos = if (alunos.none { it.clienteId == cliente.id }) {
                        alunos + AulaAluno(
                            clienteId = cliente.id,
                            clienteNome = cliente.nome,
                            treinoId = treino?.id,
                            treinoTitulo = treino?.titulo
                        )
                    } else {
                        alunos.map {
                            if (it.clienteId != cliente.id) it
                            else it.copy(
                                clienteNome = cliente.nome,
                                treinoId = treino?.id ?: it.treinoId,
                                treinoTitulo = treino?.titulo ?: it.treinoTitulo
                            )
                        }
                    }
                    alunoSel = ""
                    treinoSel = ""
                },
                enabled = alunoSel.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Incluir aluno") }
            alunos.forEach { aluno ->
                val treinosAluno = treinos.filter {
                    it.clienteId == aluno.clienteId || it.id == aluno.treinoId
                }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        CheckInRow(
                            nome = aluno.nomeExibicao(clientes).ifBlank { "Aluno" },
                            checked = aluno.checkInRealizado,
                            detalhe = listOfNotNull(
                                aluno.treinoExibicao(treinos),
                                when {
                                    !aulaSalva -> "Salve a aula para marcar a pagar"
                                    aluno.checkInRealizado -> "A pagar ${aluno.checkInHorario.orEmpty()}".trim()
                                    else -> "Toque para lançar a dívida"
                                }
                            ).joinToString(" · "),
                            enabled = aulaSalva,
                            modifier = Modifier.weight(1f),
                            onToggle = { realizar ->
                                existing?.id?.let { onPedirAPagar(it, aluno, realizar) }
                            }
                        )
                        IconButton(onClick = { alunos = alunos.filter { it.clienteId != aluno.clienteId } }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remover")
                        }
                    }
                    SimpleDropdown(
                        "Treino vinculado (opcional)",
                        aluno.treinoId.orEmpty(),
                        buildList {
                            add("" to "Nenhum")
                            addAll(treinosAluno.map { it.id to it.titulo })
                            val atualId = aluno.treinoId
                            if (!atualId.isNullOrBlank() && none { it.first == atualId }) {
                                add(atualId to (aluno.treinoExibicao(treinos) ?: "Treino"))
                            }
                        }
                    ) { escolhido ->
                        val treino = treinos.find { it.id == escolhido }
                        alunos = alunos.map {
                            if (it.clienteId != aluno.clienteId) it
                            else it.copy(
                                treinoId = escolhido.ifBlank { null },
                                treinoTitulo = treino?.titulo
                            )
                        }
                    }
                    if (!aluno.treinoId.isNullOrBlank()) {
                        val treinoVinculado = treinos.find { it.id == aluno.treinoId }
                        OutlinedButton(
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
            Button(
                onClick = {
                    var paraSalvar = alunos
                    if (alunoSel.isNotBlank()) {
                        val cliente = clientes.find { it.id == alunoSel }
                        val treino = treinos.find { it.id == treinoSel }
                        if (cliente != null && paraSalvar.none { it.clienteId == cliente.id }) {
                            paraSalvar = paraSalvar + AulaAluno(
                                clienteId = cliente.id,
                                clienteNome = cliente.nome,
                                treinoId = treino?.id,
                                treinoTitulo = treino?.titulo
                            )
                        }
                    }
                    paraSalvar = paraSalvar.map { aluno ->
                        val cliente = clientes.find { it.id == aluno.clienteId }
                        val treino = treinos.find { it.id == aluno.treinoId }
                        aluno.copy(
                            clienteNome = aluno.clienteNome.ifBlank { cliente?.nome.orEmpty() },
                            treinoTitulo = aluno.treinoTitulo?.takeIf { it.isNotBlank() } ?: treino?.titulo
                        )
                    }
                    val now = Time.nowIso()
                    onSave(
                        AulaEntity(
                            id = existing?.id ?: Time.newId(),
                            data = data.trim(),
                            horario = horario.trim(),
                            tipoAula = tipo,
                            alunosJson = JsonStore.encodeAlunos(paraSalvar),
                            status = status,
                            duracaoMinutos = duracao.toIntOrNull() ?: 60,
                            local = local.trim(),
                            linkOnline = link.trim(),
                            observacoes = observacoes.trim(),
                            createdDate = existing?.createdDate ?: now,
                            updatedDate = now
                        )
                    )
                },
                enabled = data.isNotBlank() && horario.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar no aparelho") }
        }
    }
}
