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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.InitialsAvatar
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.ui.components.StatusChip
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    clientes: List<ClienteEntity>,
    onAdd: () -> Unit,
    onOpen: (ClienteEntity) -> Unit,
    onVisualize: (ClienteEntity) -> Unit,
    onExportarApp: (ClienteEntity) -> Unit = {},
    onIntegrateApp: (ClienteEntity) -> Unit = {},
    onDelete: (String) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FitColors.Canvas,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = FitColors.Green) {
                Icon(Icons.Outlined.Add, contentDescription = "Novo aluno")
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
                    eyebrow = "Alunos",
                    title = "Alunos do espaço",
                    subtitle = "Consulte os alunos cadastrados e abra a ficha de cada um."
                )
            }
            if (clientes.isEmpty()) {
                item { EmptyState("Cadastre o primeiro aluno. Tudo fica salvo neste aparelho.") }
            } else {
                items(clientes, key = { it.id }) { cliente ->
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InitialsAvatar(cliente.nome)
                            Column(Modifier.weight(1f)) {
                                Text(cliente.nome, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = FitColors.Ink)
                                Text(
                                    cliente.email.ifBlank { cliente.telefone.ifBlank { "Sem contato" } },
                                    fontSize = 12.sp,
                                    color = FitColors.Muted
                                )
                                Text(
                                    cliente.objetivo.ifBlank { "Sem objetivo" },
                                    fontSize = 12.sp,
                                    color = FitColors.Muted
                                )
                            }
                            StatusChip(if (cliente.status == "ativo") "Ativo" else "Inativo")
                            IconButton(onClick = { onDelete(cliente.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedButton(onClick = { onVisualize(cliente) }, modifier = Modifier.weight(1f)) { Text("Visualizar") }
                            OutlinedButton(onClick = { onOpen(cliente) }, modifier = Modifier.weight(1f)) { Text("Editar") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(onClick = { onExportarApp(cliente) }, modifier = Modifier.weight(1f)) { Text("Perfil JSON") }
                            Button(onClick = { onIntegrateApp(cliente) }, modifier = Modifier.weight(1f)) { Text("Vincular app") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteFormScreen(
    existing: ClienteEntity?,
    onBack: () -> Unit,
    onVisualize: ((String) -> Unit)? = null,
    onSave: (ClienteEntity) -> Unit
) {
    var nome by rememberSaveable { mutableStateOf(existing?.nome.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(existing?.email.orEmpty()) }
    var telefone by rememberSaveable { mutableStateOf(existing?.telefone.orEmpty()) }
    var nascimento by rememberSaveable { mutableStateOf(existing?.dataNascimento.orEmpty()) }
    var sexo by rememberSaveable { mutableStateOf(existing?.sexo ?: "masculino") }
    var altura by rememberSaveable { mutableStateOf(existing?.altura?.toString().orEmpty()) }
    var peso by rememberSaveable { mutableStateOf(existing?.pesoAtual?.toString().orEmpty()) }
    var objetivo by rememberSaveable { mutableStateOf(existing?.objetivo.orEmpty()) }
    var observacoes by rememberSaveable { mutableStateOf(existing?.observacoes.orEmpty()) }
    var status by rememberSaveable { mutableStateOf(existing?.status ?: "ativo") }
    var valorAula by rememberSaveable { mutableStateOf(existing?.valorAula?.toString().orEmpty()) }
    var local by rememberSaveable { mutableStateOf(existing?.localAulaPadrao.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Novo aluno" else "Editar aluno") },
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
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(telefone, { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nascimento, { nascimento = it }, label = { Text("Nascimento (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            SimpleDropdown("Sexo", sexo, listOf("masculino" to "Masculino", "feminino" to "Feminino"), { sexo = it })
            OutlinedTextField(altura, { altura = it }, label = { Text("Altura (cm)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(peso, { peso = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(objetivo, { objetivo = it }, label = { Text("Objetivo") }, modifier = Modifier.fillMaxWidth())
            SimpleDropdown("Status", status, listOf("ativo" to "Ativo", "inativo" to "Inativo"), { status = it })
            OutlinedTextField(valorAula, { valorAula = it }, label = { Text("Valor da aula") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(local, { local = it }, label = { Text("Local padrão") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(observacoes, { observacoes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())
            if (existing != null && onVisualize != null) {
                OutlinedButton(
                    onClick = { onVisualize(existing.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Visualizar aluno") }
            }
            Button(
                onClick = {
                    val now = Time.nowIso()
                    onSave(
                        ClienteEntity(
                            id = existing?.id ?: Time.newId(),
                            nome = nome.trim(),
                            email = email.trim(),
                            telefone = telefone.trim(),
                            dataNascimento = nascimento.trim(),
                            sexo = sexo,
                            altura = altura.toDoubleOrNull(),
                            pesoAtual = peso.toDoubleOrNull(),
                            objetivo = objetivo.trim(),
                            observacoes = observacoes.trim(),
                            status = status,
                            valorAula = valorAula.replace(",", ".").toDoubleOrNull(),
                            localAulaPadrao = local.trim(),
                            createdDate = existing?.createdDate ?: now,
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
