package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.data.catalog.ExerciseCatalog
import com.fittrainer.pro.data.catalog.ExerciseCatalogItem
import com.fittrainer.pro.data.local.ExercicioBibliotecaEntity
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.ExerciseInfoDialog
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.ui.components.StatusChip
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciciosScreen(
    biblioteca: List<ExercicioBibliotecaEntity>,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    val idsBiblioteca = remember(biblioteca) { biblioteca.map { it.id }.toSet() }
    val lista = remember(biblioteca) { ExerciseCatalog.merged(context, biblioteca) }
    var busca by rememberSaveable { mutableStateOf("") }
    var detalhe by remember { mutableStateOf<ExerciseCatalogItem?>(null) }
    var excluirId by remember { mutableStateOf<String?>(null) }
    val filtrada = remember(busca, lista) {
        val needle = busca.trim()
        if (needle.length < 2) lista.take(60)
        else ExerciseCatalog.searchMerged(context, biblioteca, needle, 60)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FitColors.Canvas,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = FitColors.Green) {
                Icon(Icons.Outlined.Add, contentDescription = "Novo exercício")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PageHeader(
                    eyebrow = "Biblioteca",
                    title = "Exercícios",
                    subtitle = "Cadastre os seus e edite os do catálogo. O original do catálogo volta se você excluir a edição."
                )
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    label = { Text("Buscar exercício ou músculo") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
            if (filtrada.isEmpty()) {
                item { EmptyState("Nenhum exercício encontrado.") }
            } else {
                items(filtrada, key = { it.id.ifBlank { it.displayName } }) { item ->
                    val meu = item.id in idsBiblioteca
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.displayName, fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                                Text(
                                    listOfNotNull(
                                        item.grupo.takeIf { it.isNotBlank() },
                                        item.calorias.takeIf { it.isNotBlank() }?.let { "$it kcal/h" }
                                    ).joinToString(" · ").ifBlank { "Sem grupo muscular" },
                                    fontSize = 12.sp,
                                    color = FitColors.Muted
                                )
                            }
                            StatusChip(if (meu) "Meu" else "Catálogo")
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            OutlinedButton(onClick = { detalhe = item }, modifier = Modifier.weight(1f)) {
                                Text("Visualizar")
                            }
                            OutlinedButton(onClick = { onEdit(item.id.ifBlank { "new" }) }, modifier = Modifier.weight(1f)) {
                                Text("Editar")
                            }
                        }
                        if (meu) {
                            OutlinedButton(
                                onClick = { excluirId = item.id },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) { Text("Excluir da biblioteca") }
                        }
                    }
                }
            }
        }
    }
    detalhe?.let { ExerciseInfoDialog(item = it, onDismiss = { detalhe = null }) }
    excluirId?.let { id ->
        AlertDialog(
            onDismissRequest = { excluirId = null },
            title = { Text("Excluir exercício?") },
            text = {
                Text(
                    if (ExerciseCatalog.items(context).any { it.id == id }) {
                        "A edição local será apagada e o exercício volta ao original do catálogo."
                    } else {
                        "Este exercício sai da biblioteca deste aparelho."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(id)
                    excluirId = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { excluirId = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercicioFormScreen(
    existingId: String?,
    biblioteca: List<ExercicioBibliotecaEntity>,
    onBack: () -> Unit,
    onSave: (ExercicioBibliotecaEntity) -> Unit
) {
    val context = LocalContext.current
    val catalogo = remember { ExerciseCatalog.items(context) }
    val grupos = remember(catalogo) {
        (listOf("" to "Sem grupo") + catalogo.map { it.grupo }.filter { it.isNotBlank() }.distinct().sorted().map { it to it })
    }
    val baseCatalogo = catalogo.find { it.id == existingId }
    val baseBiblioteca = biblioteca.find { it.id == existingId }
    val novo = existingId.isNullOrBlank() || existingId == "new"

    var nome by rememberSaveable(existingId) {
        mutableStateOf(baseBiblioteca?.nome ?: baseCatalogo?.displayName.orEmpty())
    }
    var grupo by rememberSaveable(existingId) {
        mutableStateOf(baseBiblioteca?.grupoMuscular ?: baseCatalogo?.grupo.orEmpty())
    }
    var descricao by rememberSaveable(existingId) {
        mutableStateOf(baseBiblioteca?.descricao ?: baseCatalogo?.secundarios.orEmpty())
    }
    var video by rememberSaveable(existingId) {
        mutableStateOf(baseBiblioteca?.video ?: baseCatalogo?.video.orEmpty())
    }
    var videoAlt by rememberSaveable(existingId) {
        mutableStateOf(baseBiblioteca?.videoAlt ?: baseCatalogo?.videoAlt.orEmpty())
    }
    var calorias by rememberSaveable(existingId) {
        mutableStateOf(baseBiblioteca?.calorias ?: baseCatalogo?.calorias.orEmpty())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (novo && baseCatalogo == null) "Novo exercício" else "Editar exercício") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val now = Time.nowIso()
                            onSave(
                                ExercicioBibliotecaEntity(
                                    id = if (novo && baseCatalogo == null) "lib-${Time.newId()}" else existingId!!,
                                    nome = nome.trim(),
                                    grupoMuscular = grupo.trim(),
                                    descricao = descricao.trim(),
                                    video = video.trim(),
                                    videoAlt = videoAlt.trim(),
                                    calorias = calorias.trim(),
                                    createdDate = baseBiblioteca?.createdDate ?: now,
                                    updatedDate = now
                                )
                            )
                        },
                        enabled = nome.isNotBlank()
                    ) { Text("Salvar") }
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
            if (baseCatalogo != null) {
                Text(
                    "Edição salva neste aparelho. O catálogo original não é alterado.",
                    fontSize = 13.sp,
                    color = FitColors.Muted
                )
            }
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome *") }, modifier = Modifier.fillMaxWidth())
            SimpleDropdown("Grupo muscular", grupo, grupos) { grupo = it }
            OutlinedTextField(
                descricao,
                { descricao = it },
                label = { Text("Músculos secundários / observações") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(video, { video = it }, label = { Text("Vídeo (URL)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(videoAlt, { videoAlt = it }, label = { Text("Vídeo alternativo (URL)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(calorias, { calorias = it }, label = { Text("kcal/h (opcional)") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val now = Time.nowIso()
                    onSave(
                        ExercicioBibliotecaEntity(
                            id = if (novo && baseCatalogo == null) "lib-${Time.newId()}" else existingId!!,
                            nome = nome.trim(),
                            grupoMuscular = grupo.trim(),
                            descricao = descricao.trim(),
                            video = video.trim(),
                            videoAlt = videoAlt.trim(),
                            calorias = calorias.trim(),
                            createdDate = baseBiblioteca?.createdDate ?: now,
                            updatedDate = now
                        )
                    )
                },
                enabled = nome.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar no aparelho") }
        }
    }
}
