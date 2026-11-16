package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.data.avaliacao.AvaliacaoGordura
import com.fittrainer.pro.data.avaliacao.ResultadoGordura
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.DobrasCutaneas
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacoesScreen(
    avaliacoes: List<AvaliacaoEntity>,
    clientes: List<ClienteEntity>,
    onAdd: () -> Unit,
    onVisualize: (AvaliacaoEntity) -> Unit,
    onEdit: (AvaliacaoEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var filtro by rememberSaveable { mutableStateOf("") }
    var excluirId by remember { mutableStateOf<String?>(null) }
    val lista = remember(avaliacoes, filtro) {
        if (filtro.isBlank()) avaliacoes else avaliacoes.filter { it.clienteId == filtro }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FitColors.Canvas,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = FitColors.Green) {
                Icon(Icons.Outlined.Add, contentDescription = "Nova avaliação")
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
                    eyebrow = "Periódico",
                    title = "Avaliações físicas",
                    subtitle = "Cada lançamento fica na ficha do aluno com a data da medição."
                )
            }
            item {
                SimpleDropdown(
                    "Filtrar aluno",
                    filtro,
                    listOf("" to "Todos os alunos") + clientes.map { it.id to it.nome }
                ) { filtro = it }
            }
            if (lista.isEmpty()) {
                item { EmptyState("Nenhuma avaliação salva neste aparelho.") }
            } else {
                items(lista, key = { it.id }) { item ->
                    AvaliacaoCard(
                        item = item,
                        onVisualize = { onVisualize(item) },
                        onEdit = { onEdit(item) },
                        onDelete = { excluirId = item.id }
                    )
                }
            }
        }
    }
    excluirId?.let { id ->
        ConfirmarExclusaoAvaliacao(
            onDismiss = { excluirId = null },
            onConfirm = {
                onDelete(id)
                excluirId = null
            }
        )
    }
}

@Composable
fun AvaliacaoCard(
    item: AvaliacaoEntity,
    onVisualize: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val resultado = resultadoDa(item)
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.clienteNome.ifBlank { "Aluno" }, fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                Text(
                    buildString {
                        append(Time.formatDate(item.dataAvaliacao))
                        item.peso?.let { append(" · $it kg") }
                        (resultado?.percentual ?: item.percentualGordura)?.let { append(" · $it% gordura") }
                    },
                    fontSize = 12.sp,
                    color = FitColors.Muted
                )
                resultado?.let {
                    Text(it.protocolo, fontSize = 12.sp, color = FitColors.Green)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = onVisualize, modifier = Modifier.weight(1f)) { Text("Visualizar") }
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Editar") }
        }
    }
}

@Composable
private fun ConfirmarExclusaoAvaliacao(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir avaliação?") },
        text = { Text("Este lançamento sai da ficha do aluno e não pode ser desfeito neste aparelho.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoFormScreen(
    existing: AvaliacaoEntity?,
    clientes: List<ClienteEntity>,
    onBack: () -> Unit,
    onSave: (AvaliacaoEntity) -> Unit
) {
    val dobrasIniciais = remember(existing?.id) { JsonStore.decodeDobras(existing?.medidasJson.orEmpty()) }
    var clienteId by rememberSaveable(existing?.id) { mutableStateOf(existing?.clienteId.orEmpty()) }
    var data by rememberSaveable(existing?.id) { mutableStateOf(existing?.dataAvaliacao ?: Time.today()) }
    var peso by rememberSaveable(existing?.id) { mutableStateOf(existing?.peso?.toString().orEmpty()) }
    var altura by rememberSaveable(existing?.id) { mutableStateOf(existing?.altura?.toString().orEmpty()) }
    var idade by rememberSaveable(existing?.id) { mutableStateOf(existing?.idade?.toString().orEmpty()) }
    var observacoes by rememberSaveable(existing?.id) { mutableStateOf(existing?.observacoes.orEmpty()) }
    var triceps by rememberSaveable(existing?.id) { mutableStateOf(fmt(dobrasIniciais.triceps)) }
    var suprailiaca by rememberSaveable(existing?.id) { mutableStateOf(fmt(dobrasIniciais.suprailiaca)) }
    var coxa by rememberSaveable(existing?.id) { mutableStateOf(fmt(dobrasIniciais.coxa)) }
    var peitoral by rememberSaveable(existing?.id) { mutableStateOf(fmt(dobrasIniciais.peitoral)) }
    var abdomen by rememberSaveable(existing?.id) { mutableStateOf(fmt(dobrasIniciais.abdomen)) }
    var biceps by rememberSaveable(existing?.id) { mutableStateOf(fmt(dobrasIniciais.biceps)) }
    var subescapular by rememberSaveable(existing?.id) { mutableStateOf(fmt(dobrasIniciais.subescapular)) }
    var axilar by rememberSaveable(existing?.id) { mutableStateOf(fmt(dobrasIniciais.axilarMedia)) }

    val cliente = clientes.find { it.id == clienteId }
    val sexoFeminino = cliente?.sexo == "feminino" || existing?.sexo == "F"
    LaunchedEffect(clienteId, data) {
        if (existing == null && cliente != null) {
            if (altura.isBlank()) altura = cliente.altura?.toString().orEmpty()
            if (peso.isBlank()) peso = cliente.pesoAtual?.toString().orEmpty()
            if (idade.isBlank()) {
                idade = AvaliacaoGordura.idadeNaData(cliente.dataNascimento, data)?.toString().orEmpty()
            }
        }
    }

    val dobras = DobrasCutaneas(
        triceps = parseMm(triceps),
        suprailiaca = parseMm(suprailiaca),
        coxa = parseMm(coxa),
        peitoral = parseMm(peitoral),
        abdomen = parseMm(abdomen),
        biceps = parseMm(biceps),
        subescapular = parseMm(subescapular),
        axilarMedia = parseMm(axilar)
    )
    val idadeNum = idade.toIntOrNull()
        ?: AvaliacaoGordura.idadeNaData(cliente?.dataNascimento, data)
    val calculado = AvaliacaoGordura.calcular(sexoFeminino, idadeNum, dobras)
    val podeSalvar = clienteId.isNotBlank() && data.isNotBlank()
    fun salvar() {
        if (!podeSalvar) return
        val now = Time.nowIso()
        onSave(
            AvaliacaoEntity(
                id = existing?.id ?: Time.newId(),
                clienteId = clienteId,
                clienteNome = cliente?.nome.orEmpty(),
                dataAvaliacao = data.trim(),
                sexo = if (sexoFeminino) "F" else "M",
                idade = idadeNum,
                peso = peso.replace(",", ".").toDoubleOrNull(),
                altura = altura.replace(",", ".").toDoubleOrNull(),
                percentualGordura = calculado?.percentual ?: existing?.percentualGordura,
                medidasJson = JsonStore.encodeDobras(dobras),
                observacoes = observacoes.trim(),
                createdDate = existing?.createdDate ?: now,
                updatedDate = now
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nova avaliação" else "Editar avaliação") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = { salvar() }, enabled = podeSalvar) { Text("Salvar") }
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
            SimpleDropdown(
                "Aluno *",
                clienteId,
                listOf("" to "Selecione") + clientes.map { it.id to it.nome }
            ) { clienteId = it }
            OutlinedTextField(data, { data = it }, label = { Text("Data (AAAA-MM-DD) *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(peso, { peso = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(altura, { altura = it }, label = { Text("Altura (cm)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(idade, { idade = it }, label = { Text("Idade") }, modifier = Modifier.fillMaxWidth())
            PercentualGorduraCard(calculado)
            Text("Dobras cutâneas (mm)", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
            Text(
                "Verde = 3 pontos · Âmbar = 4 pontos · Azul = 7 pontos",
                fontSize = 12.sp,
                color = FitColors.Muted
            )
            DobraField("Tríceps", triceps, { triceps = it }, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraField("Supra-ilíaca", suprailiaca, { suprailiaca = it }, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraField("Coxa", coxa, { coxa = it }, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraField("Peitoral", peitoral, { peitoral = it }, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraField("Abdômen", abdomen, { abdomen = it }, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraField("Bíceps", biceps, { biceps = it }, FitColors.Dobra4Bg, FitColors.Dobra4Ink)
            DobraField("Subescapular", subescapular, { subescapular = it }, FitColors.Dobra4Bg, FitColors.Dobra4Ink)
            DobraField("Axilar média", axilar, { axilar = it }, FitColors.Dobra7Bg, FitColors.Dobra7Ink)
            OutlinedTextField(observacoes, { observacoes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { salvar() },
                enabled = podeSalvar,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar no aparelho") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoVisualizacaoScreen(
    avaliacao: AvaliacaoEntity?,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avaliação física") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (avaliacao == null) {
            EmptyState("Avaliação não encontrada.", Modifier.padding(padding))
            return@Scaffold
        }
        val dobras = JsonStore.decodeDobras(avaliacao.medidasJson)
        val resultado = resultadoDa(avaliacao)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard {
                Text(avaliacao.clienteNome.ifBlank { "Aluno" }, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = FitColors.Ink)
                Text(Time.formatDate(avaliacao.dataAvaliacao), color = FitColors.Muted, fontSize = 13.sp)
            }
            PercentualGorduraCard(resultado, fallback = avaliacao.percentualGordura)
            InfoCard {
                Text("Dados", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                DadoAvaliacao("Sexo", if (avaliacao.sexo == "F") "Feminino" else "Masculino")
                DadoAvaliacao("Idade", avaliacao.idade?.toString().orEmpty())
                DadoAvaliacao("Peso", avaliacao.peso?.let { "$it kg" }.orEmpty())
                DadoAvaliacao("Altura", avaliacao.altura?.let { "$it cm" }.orEmpty())
                DadoAvaliacao("Observações", avaliacao.observacoes)
            }
            Text("Dobras cutâneas (mm)", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
            Text("Verde = 3 pontos · Âmbar = 4 pontos · Azul = 7 pontos", fontSize = 12.sp, color = FitColors.Muted)
            DobraValor("Tríceps", dobras.triceps, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraValor("Supra-ilíaca", dobras.suprailiaca, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraValor("Coxa", dobras.coxa, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraValor("Peitoral", dobras.peitoral, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraValor("Abdômen", dobras.abdomen, FitColors.Dobra3Bg, FitColors.Dobra3Ink)
            DobraValor("Bíceps", dobras.biceps, FitColors.Dobra4Bg, FitColors.Dobra4Ink)
            DobraValor("Subescapular", dobras.subescapular, FitColors.Dobra4Bg, FitColors.Dobra4Ink)
            DobraValor("Axilar média", dobras.axilarMedia, FitColors.Dobra7Bg, FitColors.Dobra7Ink)
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar") }
        }
    }
}

@Composable
private fun PercentualGorduraCard(resultado: ResultadoGordura?, fallback: Double? = null) {
    InfoCard {
        Text("% de gordura", fontSize = 12.sp, color = FitColors.Muted)
        when {
            resultado != null -> {
                Text("${resultado.percentual}%", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                Text(resultado.protocolo, fontSize = 12.sp, color = FitColors.Green)
            }
            fallback != null -> {
                Text("$fallback%", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                Text("Informado manualmente", fontSize = 12.sp, color = FitColors.Muted)
            }
            else -> Text(
                "Preencha as dobras destacadas e a idade para calcular o % de gordura.",
                color = FitColors.Muted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DobraField(label: String, value: String, onChange: (String) -> Unit, bg: Color, ink: Color) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ink,
            unfocusedBorderColor = ink.copy(alpha = 0.45f),
            focusedLabelColor = ink,
            unfocusedLabelColor = ink,
            focusedContainerColor = bg,
            unfocusedContainerColor = bg
        )
    )
}

@Composable
private fun DobraValor(label: String, valor: Double?, bg: Color, ink: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(valor?.toString()?.plus(" mm") ?: "—", color = ink, fontSize = 13.sp)
    }
}

@Composable
private fun DadoAvaliacao(label: String, value: String) {
    if (value.isBlank()) return
    Column(Modifier.padding(top = 6.dp)) {
        Text(label, fontSize = 12.sp, color = FitColors.Muted)
        Text(value, color = FitColors.Ink)
    }
}

private fun resultadoDa(item: AvaliacaoEntity): ResultadoGordura? =
    AvaliacaoGordura.calcular(
        sexoFeminino = item.sexo == "F",
        idade = item.idade,
        dobras = JsonStore.decodeDobras(item.medidasJson)
    )

private fun parseMm(raw: String): Double? =
    raw.replace(",", ".").toDoubleOrNull()?.takeIf { it >= 0 }

private fun fmt(value: Double?): String = value?.toString().orEmpty()
