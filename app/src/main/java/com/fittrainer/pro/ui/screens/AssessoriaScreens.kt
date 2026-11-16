package com.fittrainer.pro.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.data.export.ClienteExport
import com.fittrainer.pro.data.export.ClienteExportShare
import com.fittrainer.pro.data.assessoria.AssessoriaCsv
import com.fittrainer.pro.data.assessoria.AssessoriaHtml
import com.fittrainer.pro.data.assessoria.PlanosAssessoria
import com.fittrainer.pro.data.assessoria.WhatsAppShare
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.PlanoAssessoriaEntity
import com.fittrainer.pro.data.local.PlanoDia
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.ui.components.StatTile
import com.fittrainer.pro.ui.components.StatusChip
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessoriaScreen(
    planos: List<PlanoAssessoriaEntity>,
    clientes: List<ClienteEntity> = emptyList(),
    onAdd: () -> Unit,
    onOpen: (PlanoAssessoriaEntity) -> Unit,
    onCopy: (PlanoAssessoriaEntity) -> Unit,
    onEnviar: (PlanoAssessoriaEntity) -> Unit,
    onEnviarApp: (PlanoAssessoriaEntity) -> Unit = {},
    onDelete: (String) -> Unit
) {
    var busca by rememberSaveable { mutableStateOf("") }
    var excluir by remember { mutableStateOf<PlanoAssessoriaEntity?>(null) }
    val filtrados = remember(planos, busca) {
        val needle = busca.trim().lowercase()
        if (needle.isBlank()) planos
        else planos.filter {
            it.nome.lowercase().contains(needle) || it.clienteNome.lowercase().contains(needle)
        }
    }
    val atribuido = planos.count { it.clienteId.isNotBlank() }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FitColors.Canvas,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = FitColors.Green) {
                Icon(Icons.Outlined.Add, contentDescription = "Novo plano")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PageHeader(
                    eyebrow = "Programação",
                    title = "Assessoria",
                    subtitle = "Monte planos de 1 a 12 semanas e compartilhe o arquivo do treino."
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Planos", planos.size.toString(), Icons.Outlined.FitnessCenter, Modifier.weight(1f))
                    StatTile("Com aluno", atribuido.toString(), Icons.Outlined.Share, Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    label = { Text("Buscar por aluno ou nome do plano") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (filtrados.isEmpty()) {
                item { EmptyState("Nenhum plano ainda. Crie um modelo ou atribua a um aluno.") }
            } else {
                items(filtrados, key = { it.id }) { plano ->
                    val semanas = JsonStore.decodeSemanas(plano.semanasJson)
                    val preenchidos = PlanosAssessoria.diasPreenchidos(semanas)
                    val total = plano.duracaoSemanas.coerceIn(1, 12) * 7
                    val aluno = WhatsAppShare.clienteDoCadastro(clientes, plano.clienteId, plano.clienteNome)
                    val telefone = aluno?.telefone?.takeIf { it.isNotBlank() }
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(plano.nome, fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                                Text(
                                    when {
                                        aluno != null && telefone != null -> "${aluno.nome} · $telefone"
                                        aluno != null -> "${aluno.nome} · sem telefone no cadastro"
                                        plano.clienteNome.isBlank() -> "Modelo sem aluno"
                                        else -> plano.clienteNome
                                    },
                                    fontSize = 12.sp,
                                    color = FitColors.Muted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusChip(if (plano.clienteId.isBlank()) "Modelo" else "Atribuído")
                                PlanosAssessoria.rotuloEnvio(plano)?.let { rotulo ->
                                    StatusChip(rotulo)
                                }
                            }
                        }
                        if (plano.ultimoCompartilhamento.isNotBlank()) {
                            Text(
                                "Enviado ${Time.formatDateTime(plano.ultimoCompartilhamento)}" +
                                    PlanosAssessoria.semanasEnviadas(plano).takeIf { it.isNotEmpty() }
                                        ?.sorted()
                                        ?.joinToString(prefix = " · sem. ", separator = ", ")
                                        .orEmpty(),
                                fontSize = 12.sp,
                                color = FitColors.Muted,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Text(
                            "$preenchidos/$total dias com treino · ${plano.duracaoSemanas} semana${if (plano.duracaoSemanas == 1) "" else "s"}",
                            fontSize = 12.sp,
                            color = FitColors.Muted,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Box(
                            Modifier.fillMaxWidth().padding(top = 8.dp).height(5.dp)
                                .clip(RoundedCornerShape(8.dp)).background(FitColors.Line)
                        ) {
                            Box(
                                Modifier.fillMaxWidth(if (total == 0) 0f else preenchidos.toFloat() / total)
                                    .height(5.dp).background(FitColors.Green)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedButton(onClick = { onOpen(plano) }, modifier = Modifier.weight(1f)) { Text("Abrir") }
                            if (preenchidos > 0) {
                                OutlinedButton(onClick = { onEnviar(plano) }, modifier = Modifier.weight(1f)) { Text("Enviar arquivo") }
                            }
                            IconButton(onClick = { onCopy(plano) }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar")
                            }
                            IconButton(onClick = { excluir = plano }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                            }
                        }
                        if (preenchidos > 0) {
                            OutlinedButton(
                                onClick = { onEnviarApp(plano) },
                                enabled = plano.clienteId.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) { Text("Enviar plano ao app do aluno") }
                            if (plano.clienteId.isBlank()) {
                                Text(
                                    "Associe um aluno cadastrado para enviar pelo Firebase.",
                                    color = FitColors.Muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    excluir?.let { plano ->
        AlertDialog(
            onDismissRequest = { excluir = null },
            title = { Text("Excluir plano?") },
            text = { Text("“${plano.nome}” sai da assessoria. Os treinos da biblioteca permanecem.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(plano.id)
                    excluir = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { excluir = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessoriaFormScreen(
    planoId: String,
    existing: PlanoAssessoriaEntity?,
    clientes: List<ClienteEntity>,
    treinos: List<TreinoEntity>,
    onBack: () -> Unit,
    onSave: (PlanoAssessoriaEntity) -> Unit,
    onCriarTreino: (PlanoAssessoriaEntity, semana: Int, dia: Int) -> Unit,
    onEnviar: () -> Unit = {}
) {
    var nome by rememberSaveable(existing?.id) { mutableStateOf(existing?.nome ?: PlanosAssessoria.nomeSugerido("", 4)) }
    var clienteId by rememberSaveable(existing?.id) { mutableStateOf(existing?.clienteId.orEmpty()) }
    var duracao by rememberSaveable(existing?.id) { mutableStateOf((existing?.duracaoSemanas ?: 4).toString()) }
    var semanas by remember(existing?.id) {
        mutableStateOf(PlanosAssessoria.normalizar(JsonStore.decodeSemanas(existing?.semanasJson.orEmpty()), existing?.duracaoSemanas ?: 4))
    }
    var semanaAtual by rememberSaveable(existing?.id) { mutableIntStateOf(1) }
    var salvo by remember { mutableStateOf(true) }
    var pendenteDuracao by remember { mutableStateOf<Int?>(null) }
    var escolherDia by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(existing?.id, existing?.updatedDate) {
        if (existing != null) {
            nome = existing.nome
            clienteId = existing.clienteId
            duracao = existing.duracaoSemanas.toString()
            semanas = PlanosAssessoria.normalizar(JsonStore.decodeSemanas(existing.semanasJson), existing.duracaoSemanas)
            semanaAtual = semanaAtual.coerceIn(1, existing.duracaoSemanas.coerceIn(1, 12))
            salvo = true
        }
    }

    val cliente = clientes.find { it.id == clienteId }
    val n = duracao.toIntOrNull()?.coerceIn(1, 12) ?: 4
    val semana = semanas.find { it.numero == semanaAtual } ?: PlanosAssessoria.semanasVazias(n).first()

    fun marcarAlterado() { salvo = false }

    fun montarEntidade(statusEnvio: String? = null, dataEnvio: String? = null): PlanoAssessoriaEntity {
        val now = Time.nowIso()
        val semanasNorm = PlanosAssessoria.normalizar(semanas, n)
        semanas = semanasNorm
        return PlanoAssessoriaEntity(
            id = planoId,
            nome = nome.trim().ifBlank { PlanosAssessoria.nomeSugerido(cliente?.nome.orEmpty(), n) },
            clienteId = clienteId,
            clienteNome = cliente?.nome.orEmpty(),
            duracaoSemanas = n,
            semanasJson = JsonStore.encodeSemanas(semanasNorm),
            copiadoDeId = existing?.copiadoDeId,
            status = if (clienteId.isBlank()) "modelo" else "atribuido",
            ultimoCompartilhamento = dataEnvio ?: existing?.ultimoCompartilhamento.orEmpty(),
            statusCompartilhamento = statusEnvio ?: existing?.statusCompartilhamento.orEmpty(),
            semanasCompartilhadasJson = existing?.semanasCompartilhadasJson ?: "[]",
            createdDate = existing?.createdDate ?: now,
            updatedDate = now
        )
    }

    fun persistir(statusEnvio: String? = null, dataEnvio: String? = null) {
        onSave(montarEntidade(statusEnvio, dataEnvio))
        salvo = true
    }

    fun aplicarDuracao(novo: Int) {
        semanas = PlanosAssessoria.normalizar(semanas, novo)
        duracao = novo.toString()
        semanaAtual = semanaAtual.coerceIn(1, novo)
        marcarAlterado()
    }

    fun atualizarDia(diaSemana: Int, transform: (PlanoDia) -> PlanoDia) {
        semanas = semanas.map { bloco ->
            if (bloco.numero != semanaAtual) bloco
            else bloco.copy(dias = bloco.dias.map { if (it.diaSemana == diaSemana) transform(it) else it })
        }
        marcarAlterado()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Novo plano" else "Editar plano") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (existing != null && PlanosAssessoria.diasPreenchidos(semanas) > 0) {
                        TextButton(onClick = {
                            persistir()
                            onEnviar()
                        }) { Text("Enviar") }
                    }
                    TextButton(onClick = { persistir() }) { Text("Salvar") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (salvo) "Salvo neste aparelho" else "Alterações não salvas", fontSize = 12.sp, color = FitColors.Muted)
            OutlinedTextField(nome, { nome = it; marcarAlterado() }, label = { Text("Nome do plano") }, modifier = Modifier.fillMaxWidth())
            SimpleDropdown(
                "Aluno (opcional)",
                clienteId,
                listOf("" to "Modelo sem aluno") + clientes.map { it.id to it.nome }
            ) {
                clienteId = it
                if (nome.isBlank() || nome.startsWith("Plano de")) {
                    nome = PlanosAssessoria.nomeSugerido(clientes.find { c -> c.id == it }?.nome.orEmpty(), n)
                }
                marcarAlterado()
            }
            SimpleDropdown(
                "Duração",
                n.toString(),
                (1..12).map { it.toString() to "$it semana${if (it == 1) "" else "s"}" }
            ) { valor ->
                val novo = valor.toIntOrNull() ?: n
                if (PlanosAssessoria.temDadosAlem(semanas, novo)) pendenteDuracao = novo
                else aplicarDuracao(novo)
            }
            Text("Semanas", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                semanas.chunked(4).forEach { linha ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        linha.forEach { bloco ->
                            val preenchidos = bloco.dias.count { !it.treinoId.isNullOrBlank() }
                            OutlinedButton(
                                onClick = { semanaAtual = bloco.numero },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Semana ${bloco.numero}", fontWeight = if (semanaAtual == bloco.numero) FontWeight.SemiBold else FontWeight.Normal)
                                    Text("$preenchidos/7", fontSize = 12.sp)
                                }
                            }
                        }
                        repeat(4 - linha.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
            InfoCard {
                Text("Semana $semanaAtual", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                Text("Associe um treino a cada dia. O compartilhamento do arquivo fica no botão Enviar.", fontSize = 12.sp, color = FitColors.Muted)
                semana.dias.forEach { dia ->
                    val treino = treinos.find { it.id == dia.treinoId }
                    val estado = if (treino != null) "Preenchido" else "Vazio"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp)).border(1.dp, FitColors.Line, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(PlanosAssessoria.nomesDias[dia.diaSemana - 1], fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(treino?.titulo ?: estado, fontSize = 12.sp, color = FitColors.Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            TextButton(onClick = { escolherDia = dia.diaSemana }) { Text("Selecionar") }
                            TextButton(onClick = {
                                onCriarTreino(montarEntidade(), semanaAtual, dia.diaSemana)
                            }) { Text("Criar") }
                            if (treino != null) {
                                TextButton(onClick = { atualizarDia(dia.diaSemana) { it.copy(treinoId = null, selecionadoEnvio = false) } }) {
                                    Text("Remover")
                                }
                            }
                        }
                    }
                }
            }
            if (existing != null && PlanosAssessoria.diasPreenchidos(semanas) > 0) {
                Button(
                    onClick = {
                        persistir()
                        onEnviar()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Enviar treinos") }
            }
            Button(onClick = { persistir() }, modifier = Modifier.fillMaxWidth()) { Text("Salvar no aparelho") }
        }
    }

    escolherDia?.let { diaSemana ->
        AlertDialog(
            onDismissRequest = { escolherDia = null },
            title = { Text("Selecionar treino") },
            text = {
                Column(Modifier.height(280.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (treinos.isEmpty()) Text("Nenhum treino salvo. Use Criar treino.", color = FitColors.Muted)
                    treinos.forEach { treino ->
                        OutlinedButton(onClick = {
                            atualizarDia(diaSemana) { it.copy(treinoId = treino.id) }
                            escolherDia = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(treino.titulo)
                                Text("${treino.duracaoEstimada} min · ${treino.clienteNome.ifBlank { "Sem aluno" }}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { escolherDia = null }) { Text("Fechar") } }
        )
    }
    pendenteDuracao?.let { novo ->
        AlertDialog(
            onDismissRequest = { pendenteDuracao = null },
            title = { Text("Reduzir duração?") },
            text = { Text("Semanas acima de $novo com treinos associados serão removidas deste plano. Os treinos da biblioteca permanecem.") },
            confirmButton = { TextButton(onClick = { aplicarDuracao(novo); pendenteDuracao = null }) { Text("Reduzir") } },
            dismissButton = { TextButton(onClick = { pendenteDuracao = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessoriaEnvioScreen(
    plano: PlanoAssessoriaEntity?,
    clientes: List<ClienteEntity>,
    treinos: List<TreinoEntity>,
    onBack: () -> Unit,
    onRegistrarEnvio: (PlanoAssessoriaEntity) -> Unit
) {
    val context = LocalContext.current
    if (plano == null) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Enviar treinos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }) { padding ->
            EmptyState("Plano não encontrado.", Modifier.padding(padding))
        }
        return
    }
    val disponiveis = remember(plano.semanasJson) { AssessoriaCsv.diasComTreino(plano) }
    var marcados by remember(plano.id, disponiveis) {
        mutableStateOf(disponiveis.map { "${it.semana}-${it.diaSemana}" }.toSet())
    }
    var confirmar by remember { mutableStateOf(false) }
    val selecionados = disponiveis.filter { "${it.semana}-${it.diaSemana}" in marcados }
    val treinosSel = selecionados.mapNotNull { slot -> treinos.find { it.id == slot.treinoId } }
    val destino = WhatsAppShare.destino(
        clientes,
        plano.clienteId,
        treinosSel.map { it.clienteId },
        plano.clienteNome,
        treinosSel.map { it.clienteNome }
    )
    val telefoneCadastro = destino?.telefone.orEmpty()
    val nomeAluno = destino?.nome ?: plano.clienteNome

    fun disparar() {
        val html = AssessoriaHtml.gerar(plano, selecionados, treinos, nomeAluno)
        val caption = buildString {
            append("FitTrainer Pro — ${plano.nome}")
            if (nomeAluno.isNotBlank()) append("\nAluno: $nomeAluno")
            if (telefoneCadastro.isNotBlank()) append("\nContato: $telefoneCadastro")
            append("\n${selecionados.size} dia(s) de treino no arquivo HTML.")
            append("\nAbra no navegador e filtre pelo dia e pela semana.")
        }
        val ok = WhatsAppShare.compartilharHtml(
            context,
            html,
            AssessoriaHtml.nomeArquivo(plano),
            caption
        )
        if (ok) confirmar = true
        else Toast.makeText(context, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_LONG).show()
    }

    fun dispararJson() {
        val alunoId = destino?.id.orEmpty()
        if (alunoId.isBlank()) {
            Toast.makeText(context, "Associe um aluno do cadastro para exportar ao app.", Toast.LENGTH_LONG).show()
            return
        }
        val semanas = selecionados.map { it.semana }.distinct().sorted()
        val base = Time.today()
        val minSemana = semanas.minOrNull() ?: 1
        val arquivos = semanas.mapNotNull { numero ->
            val inicio = Time.mondayOf(base).plusWeeks((numero - minSemana).toLong()).toString()
            val json = ClienteExport.planoSemanal(plano, alunoId, numero, selecionados, treinos, inicio)
                ?: return@mapNotNull null
            ClienteExport.nomePlano(destino?.nome ?: plano.clienteNome, numero) to json
        }
        if (arquivos.isEmpty()) {
            Toast.makeText(context, "Nenhum dia selecionado para exportar.", Toast.LENGTH_LONG).show()
            return
        }
        val ok = ClienteExportShare.compartilhar(
            context,
            arquivos,
            "FitTrainer Pro — plano semanal de ${destino?.nome ?: plano.clienteNome} para o app do aluno."
        )
        if (ok) confirmar = true
        else Toast.makeText(context, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_LONG).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enviar treinos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PageHeader(
                    eyebrow = "Compartilhar",
                    title = plano.nome,
                    subtitle = "Selecione os dias e escolha o app para enviar o arquivo. Envio de só algumas semanas fica Parcial na capa."
                )
            }
            if (disponiveis.isEmpty()) {
                item { EmptyState("Nenhum dia deste plano tem treino associado.") }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { marcados = disponiveis.map { "${it.semana}-${it.diaSemana}" }.toSet() }) {
                            Text("Marcar todos")
                        }
                        OutlinedButton(onClick = { marcados = emptySet() }) { Text("Limpar") }
                    }
                }
                items(disponiveis, key = { "${it.semana}-${it.diaSemana}-${it.treinoId}" }) { slot ->
                    val treino = treinos.find { it.id == slot.treinoId }
                    val chave = "${slot.semana}-${slot.diaSemana}"
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = chave in marcados,
                                onCheckedChange = { checked ->
                                    marcados = if (checked) marcados + chave else marcados - chave
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Semana ${slot.semana} · ${PlanosAssessoria.nomesDias.getOrElse(slot.diaSemana - 1) { "Dia ${slot.diaSemana}" }}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = FitColors.Ink
                                )
                                Text(
                                    "${treino?.titulo ?: "Treino"} · ${treino?.duracaoEstimada ?: "-"} min",
                                    fontSize = 12.sp,
                                    color = FitColors.Muted
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        "${selecionados.size} treino(s) · arquivo HTML com filtro por dia",
                        fontSize = 12.sp,
                        color = FitColors.Muted
                    )
                    Button(
                        onClick = { disparar() },
                        enabled = selecionados.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Compartilhar arquivo") }
                    OutlinedButton(
                        onClick = { dispararJson() },
                        enabled = selecionados.isNotEmpty() && destino != null,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("Enviar JSON ao app do aluno") }
                }
            }
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("O arquivo foi compartilhado?") },
            text = { Text("Só registramos o envio se você confirmar. O app não confirma a entrega no outro aplicativo.") },
            confirmButton = {
                TextButton(onClick = {
                    onRegistrarEnvio(
                        PlanosAssessoria.registrarCompartilhamento(
                            plano,
                            selecionados.map { it.semana }.toSet()
                        )
                    )
                    confirmar = false
                    onBack()
                }) { Text("Sim, registrar") }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Não") } }
        )
    }
}
