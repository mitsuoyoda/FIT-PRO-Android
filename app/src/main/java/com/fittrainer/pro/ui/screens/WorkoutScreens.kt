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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.local.WorkoutExercise
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.ExerciseInfoDialog
import com.fittrainer.pro.ui.components.ExerciseNameField
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreinosScreen(
    treinos: List<TreinoEntity>,
    onAdd: () -> Unit,
    onOpen: (TreinoEntity) -> Unit,
    onVisualize: (TreinoEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Outlined.Add, contentDescription = "Novo treino")
            }
        }
    ) { padding ->
        if (treinos.isEmpty()) {
            EmptyState("Crie um treino para um aluno. Ele fica só neste aparelho.", Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(treinos, key = { it.id }) { treino ->
                    val exercicios = JsonStore.decodeExercicios(treino.exerciciosJson)
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(treino.titulo, fontWeight = FontWeight.SemiBold)
                                Text(treino.clienteNome.ifBlank { "Sem aluno" })
                                Text(
                                    "${exercicios.size} exercícios · ${treino.tipo}",
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(onClick = { onDelete(treino.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onVisualize(treino) }) { Text("Visualizar") }
                            OutlinedButton(onClick = { onOpen(treino) }) { Text("Editar") }
                        }
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
    onBack: () -> Unit,
    onVisualize: ((String) -> Unit)? = null,
    onSave: (TreinoEntity) -> Unit
) {
    val ativos = clientes.filter { it.status == "ativo" }
    var titulo by rememberSaveable { mutableStateOf(existing?.titulo.orEmpty()) }
    var descricao by rememberSaveable { mutableStateOf(existing?.descricao.orEmpty()) }
    var clienteId by rememberSaveable { mutableStateOf(existing?.clienteId.orEmpty()) }
    var tipo by rememberSaveable { mutableStateOf(existing?.tipo ?: "forca") }
    var duracao by rememberSaveable { mutableStateOf((existing?.duracaoEstimada ?: 60).toString()) }
    var exercicios by remember { mutableStateOf(existing?.let { JsonStore.decodeExercicios(it.exerciciosJson) } ?: emptyList()) }
    val temposDigitados = remember { mutableStateMapOf<Int, String>() }
    val descansosDigitados = remember { mutableStateMapOf<Int, String>() }
    var detalhe by remember { mutableStateOf<ExerciseCatalogItem?>(null) }
    val context = LocalContext.current

    fun aplicarCatalogo(index: Int, item: ExerciseCatalogItem) {
        val atual = exercicios[index]
        exercicios = exercicios.toMutableList().also { list ->
            list[index] = atual.copy(
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
        detalhe = ExerciseCatalog.items(context).firstOrNull { it.id == item.catalogId && item.catalogId.isNotBlank() }
            ?: ExerciseCatalog.findByName(context, item.nome)
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
            Text("Exercícios", style = MaterialTheme.typography.titleMedium)
            exercicios.forEachIndexed { index, item ->
                InfoCard {
                    ExerciseNameField(
                        value = item.nome,
                        onValueChange = {
                            exercicios = exercicios.toMutableList().also { list -> list[index] = item.copy(nome = it) }
                        },
                        onPick = { catalogo -> aplicarCatalogo(index, catalogo) },
                        onShowInfo = { abrirDetalhe(item) },
                        hasInfo = item.nome.isNotBlank()
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
                                exercicios = exercicios.toMutableList().also { list ->
                                    list[index] = item.copy(tipoExecucao = if (checked) "tempo" else "repeticao")
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
                            { exercicios = exercicios.toMutableList().also { list -> list[index] = item.copy(series = it.toIntOrNull() ?: 1) } },
                            label = { Text("Séries") },
                            modifier = Modifier.weight(1f)
                        )
                        if (porTempo) {
                            OutlinedTextField(
                                temposDigitados[index]
                                    ?: item.tempoExecucao?.let { Time.formatMmSs(it) }.orEmpty(),
                                { valor ->
                                    val sanitizado = Time.sanitizeMmSs(valor)
                                    temposDigitados[index] = sanitizado
                                    exercicios = exercicios.toMutableList().also { list ->
                                        list[index] = item.copy(tempoExecucao = Time.parseMmSs(sanitizado))
                                    }
                                },
                                label = { Text("Tempo (mm:ss)") },
                                placeholder = { Text("00:45") },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            OutlinedTextField(
                                item.repeticoes,
                                { exercicios = exercicios.toMutableList().also { list -> list[index] = item.copy(repeticoes = it) } },
                                label = { Text("Reps") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (porTempo) {
                        OutlinedTextField(
                            descansosDigitados[index]
                                ?: item.descansoSegundos.takeIf { it > 0 }?.let { Time.formatMmSs(it) }.orEmpty(),
                            { valor ->
                                val sanitizado = Time.sanitizeMmSs(valor)
                                descansosDigitados[index] = sanitizado
                                exercicios = exercicios.toMutableList().also { list ->
                                    list[index] = item.copy(descansoSegundos = Time.parseMmSs(sanitizado) ?: 0)
                                }
                            },
                            label = { Text("Descanso (mm:ss, opcional)") },
                            placeholder = { Text("01:00") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButtonDelete { exercicios = exercicios.filterIndexed { i, _ -> i != index } }
                }
            }
            OutlinedButton(
                onClick = { exercicios = exercicios + WorkoutExercise() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Adicionar exercício") }
            if (existing != null && onVisualize != null) {
                OutlinedButton(
                    onClick = { onVisualize(existing.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Visualizar treino") }
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
                            exerciciosJson = JsonStore.encodeExercicios(exercicios),
                            dataEnvio = existing?.dataEnvio ?: Time.today(),
                            createdDate = existing?.createdDate ?: now,
                            updatedDate = now
                        )
                    )
                },
                enabled = titulo.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar no aparelho") }
        }
    }

    detalhe?.let { catalogo ->
        ExerciseInfoDialog(item = catalogo, onDismiss = { detalhe = null })
    }
}

@Composable
private fun TextButtonDelete(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text("Remover exercício") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AulaFormScreen(
    existing: AulaEntity?,
    clientes: List<ClienteEntity>,
    treinos: List<TreinoEntity>,
    onBack: () -> Unit,
    onVisualizeTreino: (String) -> Unit = {},
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
    var alunos by remember { mutableStateOf(existing?.let { JsonStore.decodeAlunos(it.alunosJson) } ?: emptyList()) }
    var alunoSel by rememberSaveable { mutableStateOf("") }
    var treinoSel by rememberSaveable { mutableStateOf("") }

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
                    val cliente = ativos.find { it.id == alunoSel } ?: return@OutlinedButton
                    if (alunos.none { it.clienteId == cliente.id }) {
                        alunos = alunos + AulaAluno(
                            clienteId = cliente.id,
                            clienteNome = cliente.nome,
                            treinoId = treinoSel.ifBlank { null }
                        )
                    }
                    alunoSel = ""
                    treinoSel = ""
                },
                enabled = alunoSel.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Incluir aluno") }
            alunos.forEach { aluno ->
                val treinosAluno = treinos.filter { it.clienteId == aluno.clienteId }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(aluno.clienteNome, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { alunos = alunos.filter { it.clienteId != aluno.clienteId } }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remover")
                        }
                    }
                    SimpleDropdown(
                        "Treino vinculado (opcional)",
                        aluno.treinoId.orEmpty(),
                        listOf("" to "Nenhum") + treinosAluno.map { it.id to it.titulo }
                    ) { escolhido ->
                        alunos = alunos.map {
                            if (it.clienteId == aluno.clienteId) it.copy(treinoId = escolhido.ifBlank { null }) else it
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
            Button(
                onClick = {
                    val now = Time.nowIso()
                    onSave(
                        AulaEntity(
                            id = existing?.id ?: Time.newId(),
                            data = data.trim(),
                            horario = horario.trim(),
                            tipoAula = tipo,
                            alunosJson = JsonStore.encodeAlunos(alunos),
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
