package com.fittrainer.pro.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.data.backup.BackupManager
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.ProgressoEntity
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.util.Time
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onExercicios: () -> Unit,
    onFinanceiro: () -> Unit,
    onRelatorios: () -> Unit,
    onAvaliacoes: () -> Unit,
    onProgresso: () -> Unit,
    onBackup: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Menu") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MenuRow("Exercícios", "Catálogo com vídeos de demonstração", onExercicios)
            MenuRow("Financeiro", "Receitas, aulas e pagamentos", onFinanceiro)
            MenuRow("Relatórios", "Indicadores do espaço neste mês", onRelatorios)
            MenuRow("Avaliações físicas", "Peso, medidas e histórico", onAvaliacoes)
            MenuRow("Progresso", "Registros de evolução do aluno", onProgresso)
            MenuRow("Configurações", "Exportar ou importar o banco local", onBackup)
        }
    }
}

@Composable
private fun MenuRow(title: String, subtitle: String, onClick: () -> Unit) {
    InfoCard {
        Column(Modifier.clickable(onClick = onClick).fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacoesScreen(
    avaliacoes: List<AvaliacaoEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (AvaliacaoEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Outlined.Add, contentDescription = "Nova avaliação")
            }
        }
    ) { padding ->
        if (avaliacoes.isEmpty()) {
            EmptyState("Nenhuma avaliação salva neste aparelho.", Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(avaliacoes, key = { it.id }) { item ->
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.clienteNome, fontWeight = FontWeight.SemiBold)
                                Text("${Time.formatDate(item.dataAvaliacao)} · ${item.peso ?: "-"} kg")
                            }
                            IconButton(onClick = { onDelete(item.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                            }
                        }
                        OutlinedButton(onClick = { onOpen(item) }) { Text("Editar") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoFormScreen(
    existing: AvaliacaoEntity?,
    clientes: List<ClienteEntity>,
    onBack: () -> Unit,
    onSave: (AvaliacaoEntity) -> Unit
) {
    var clienteId by rememberSaveable { mutableStateOf(existing?.clienteId.orEmpty()) }
    var data by rememberSaveable { mutableStateOf(existing?.dataAvaliacao ?: Time.today()) }
    var peso by rememberSaveable { mutableStateOf(existing?.peso?.toString().orEmpty()) }
    var altura by rememberSaveable { mutableStateOf(existing?.altura?.toString().orEmpty()) }
    var idade by rememberSaveable { mutableStateOf(existing?.idade?.toString().orEmpty()) }
    var gordura by rememberSaveable { mutableStateOf(existing?.percentualGordura?.toString().orEmpty()) }
    var observacoes by rememberSaveable { mutableStateOf(existing?.observacoes.orEmpty()) }
    val ativos = clientes.filter { it.status == "ativo" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nova avaliação" else "Editar avaliação") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SimpleDropdown("Aluno", clienteId, listOf("" to "Selecione") + ativos.map { it.id to it.nome }) { clienteId = it }
            OutlinedTextField(data, { data = it }, label = { Text("Data (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(peso, { peso = it }, label = { Text("Peso (kg) *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(altura, { altura = it }, label = { Text("Altura (cm)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(idade, { idade = it }, label = { Text("Idade") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(gordura, { gordura = it }, label = { Text("% gordura") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(observacoes, { observacoes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val cliente = clientes.find { it.id == clienteId }
                    val now = Time.nowIso()
                    onSave(
                        AvaliacaoEntity(
                            id = existing?.id ?: Time.newId(),
                            clienteId = clienteId,
                            clienteNome = cliente?.nome.orEmpty(),
                            dataAvaliacao = data,
                            sexo = if (cliente?.sexo == "feminino") "F" else "M",
                            idade = idade.toIntOrNull(),
                            peso = peso.replace(",", ".").toDoubleOrNull(),
                            altura = altura.replace(",", ".").toDoubleOrNull(),
                            percentualGordura = gordura.replace(",", ".").toDoubleOrNull(),
                            observacoes = observacoes.trim(),
                            createdDate = existing?.createdDate ?: now,
                            updatedDate = now
                        )
                    )
                },
                enabled = clienteId.isNotBlank() && peso.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar no aparelho") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressoScreen(
    clientes: List<ClienteEntity>,
    progresso: List<ProgressoEntity>,
    onBack: () -> Unit,
    onSave: (ProgressoEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var clienteId by rememberSaveable { mutableStateOf("") }
    var data by rememberSaveable { mutableStateOf(Time.today()) }
    var peso by rememberSaveable { mutableStateOf("") }
    var gordura by rememberSaveable { mutableStateOf("") }
    var observacoes by rememberSaveable { mutableStateOf("") }
    val ativos = clientes.filter { it.status == "ativo" }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SimpleDropdown("Aluno", clienteId, listOf("" to "Selecione") + ativos.map { it.id to it.nome }) { clienteId = it }
                OutlinedTextField(data, { data = it }, label = { Text("Data (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(peso, { peso = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(gordura, { gordura = it }, label = { Text("% gordura") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(observacoes, { observacoes = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Button(
                    onClick = {
                        onSave(
                            ProgressoEntity(
                                id = Time.newId(),
                                clienteId = clienteId,
                                dataRegistro = data,
                                peso = peso.replace(",", ".").toDoubleOrNull(),
                                percentualGordura = gordura.replace(",", ".").toDoubleOrNull(),
                                observacoes = observacoes.trim(),
                                createdDate = Time.nowIso(),
                                updatedDate = Time.nowIso()
                            )
                        )
                        peso = ""; gordura = ""; observacoes = ""
                    },
                    enabled = clienteId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Registrar progresso") }
            }
            items(progresso, key = { it.id }) { item ->
                val nome = clientes.find { it.id == item.clienteId }?.nome ?: "Aluno"
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(nome, fontWeight = FontWeight.SemiBold)
                            Text("${Time.formatDate(item.dataRegistro)} · ${item.peso ?: "-"} kg")
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceiroScreen(
    clientes: List<ClienteEntity>,
    transacoes: List<TransacaoEntity>,
    onBack: () -> Unit,
    onSave: (TransacaoEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    val receber = transacoes.filter { it.tipoTransacao == "aula_realizada" }.sumOf { it.valor }
    val recebido = transacoes.filter { it.tipoTransacao == "pagamento" }.sumOf { kotlin.math.abs(it.valor) }
    var clienteId by rememberSaveable { mutableStateOf("") }
    var valor by rememberSaveable { mutableStateOf("") }
    var metodo by rememberSaveable { mutableStateOf("dinheiro") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                InfoCard {
                    Text("A receber: R$ ${"%.2f".format(Locale("pt", "BR"), receber)}")
                    Text("Recebido: R$ ${"%.2f".format(Locale("pt", "BR"), recebido)}")
                    Text("Saldo: R$ ${"%.2f".format(Locale("pt", "BR"), receber - recebido)}", fontWeight = FontWeight.Bold)
                }
            }
            item {
                SimpleDropdown("Aluno", clienteId, listOf("" to "Selecione") + clientes.map { it.id to it.nome }) { clienteId = it }
                OutlinedTextField(valor, { valor = it }, label = { Text("Valor do pagamento") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                SimpleDropdown("Método", metodo, listOf("dinheiro" to "Dinheiro", "pix" to "Pix", "cartao" to "Cartão")) { metodo = it }
                Button(
                    onClick = {
                        val cliente = clientes.find { it.id == clienteId }
                        val now = Time.nowIso()
                        onSave(
                            TransacaoEntity(
                                id = Time.newId(),
                                clienteId = clienteId,
                                clienteNome = cliente?.nome.orEmpty(),
                                tipoTransacao = "pagamento",
                                dataTransacao = now,
                                valor = -kotlin.math.abs(valor.replace(",", ".").toDoubleOrNull() ?: 0.0),
                                referencia = "Pagamento de aulas",
                                metodoPagamento = metodo,
                                createdDate = now,
                                updatedDate = now
                            )
                        )
                        valor = ""
                    },
                    enabled = clienteId.isNotBlank() && valor.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Lançar pagamento") }
            }
            items(transacoes, key = { it.id }) { item ->
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.clienteNome.ifBlank { "Transação" }, fontWeight = FontWeight.SemiBold)
                            Text("${item.tipoTransacao} · R$ ${"%.2f".format(Locale("pt", "BR"), item.valor)}")
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    backupManager: BackupManager,
    onBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var pendingImportReplace by remember { mutableStateOf<Boolean?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { backupManager.exportTo(uri) }
                    .onSuccess { onMessage("Backup exportado") }
                    .onFailure { onMessage(it.message ?: "Falha ao exportar") }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val replace = pendingImportReplace
        pendingImportReplace = null
        if (uri != null && replace != null) {
            scope.launch {
                runCatching { backupManager.importFrom(uri, replace) }
                    .onSuccess { onMessage(if (replace) "Banco substituído pelo backup" else "Backup mesclado") }
                    .onFailure { onMessage(it.message ?: "Falha ao importar") }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Todos os dados ficam no SQLite deste aparelho. Exporte com frequência e guarde o arquivo em um local seguro.")
            Button(
                onClick = { exportLauncher.launch("fittrainer-backup-${Time.today()}.json") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Exportar banco") }
            OutlinedButton(
                onClick = {
                    pendingImportReplace = false
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importar e mesclar") }
            OutlinedButton(
                onClick = { confirmReplace = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importar e substituir tudo") }
        }
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Substituir o banco local?") },
            text = { Text("Os dados atuais deste aparelho serão apagados e trocados pelo arquivo importado.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplace = false
                    pendingImportReplace = true
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }) { Text("Substituir") }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancelar") } }
        )
    }
}
