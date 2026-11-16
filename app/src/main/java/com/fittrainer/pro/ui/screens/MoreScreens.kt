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
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.TransacaoEntity
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Schedule
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.ui.components.StatTile
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time
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
        containerColor = FitColors.Canvas
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PageHeader(
                    eyebrow = "Caixa",
                    title = "Financeiro",
                    subtitle = "Receitas recebidas, aulas a pagar e lançamentos do espaço."
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("A receber", Time.money(receber), Icons.Outlined.Schedule)
                    StatTile("Recebido", Time.money(recebido), Icons.Outlined.AttachMoney)
                    StatTile("Saldo", Time.money(receber - recebido), Icons.Outlined.AttachMoney)
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
                            Text(
                                when (item.tipoTransacao) {
                                    "aula_realizada" -> "A pagar"
                                    "pagamento" -> "Pagamento"
                                    else -> item.tipoTransacao
                                } + if (item.referencia.isNotBlank()) " · ${item.referencia}" else ""
                            )
                            Text(Time.money(item.valor), fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
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
    cloudBackupStatus: String,
    onSyncCloudBackup: () -> Unit,
    onBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var pendingImportReplace by remember { mutableStateOf<Boolean?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var senha by rememberSaveable { mutableStateOf("") }
    val mimeExport = if (senha.isNotBlank()) "application/octet-stream" else "application/json"
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeExport)) { uri ->
        if (uri != null) {
            val senhaChars = senha.toCharArray()
            scope.launch {
                runCatching { backupManager.exportTo(uri, senhaChars) }
                    .onSuccess { onMessage(if (senha.isNotBlank()) "Backup cifrado exportado" else "Backup exportado") }
                    .onFailure { onMessage(it.message ?: "Falha ao exportar") }
                senhaChars.fill('\u0000')
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val replace = pendingImportReplace
        pendingImportReplace = null
        if (uri != null && replace != null) {
            val senhaChars = senha.toCharArray()
            scope.launch {
                runCatching { backupManager.importFrom(uri, replace, senhaChars) }
                    .onSuccess { onMessage(if (replace) "Banco substituído pelo backup" else "Backup mesclado") }
                    .onFailure { onMessage(it.message ?: "Falha ao importar") }
                senhaChars.fill('\u0000')
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FitColors.Canvas
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PageHeader(
                eyebrow = "Espaço",
                title = "Configurações",
                subtitle = "O app guarda os dados localmente e sincroniza uma cópia incremental na conta Firebase do treinador."
            )
            InfoCard {
                Text("Backup na nuvem", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                Text(cloudBackupStatus, color = FitColors.Muted, modifier = Modifier.padding(top = 5.dp))
                OutlinedButton(onClick = onSyncCloudBackup, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Sincronizar e recuperar agora")
                }
                Text(
                    "No primeiro acesso, os registros são mesclados pela data de atualização. Exclusões locais permanecem na cópia de recuperação.",
                    color = FitColors.Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha do backup (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val nome = if (senha.isNotBlank()) {
                        "fittrainer-backup-${Time.today()}.fitpro"
                    } else {
                        "fittrainer-backup-${Time.today()}.json"
                    }
                    exportLauncher.launch(nome)
                },
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
