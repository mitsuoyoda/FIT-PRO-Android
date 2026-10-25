package com.fittrainer.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fittrainer.pro.data.backup.BackupManager
import com.fittrainer.pro.ui.screens.AgendaScreen
import com.fittrainer.pro.ui.screens.AulaFormScreen
import com.fittrainer.pro.ui.screens.AvaliacaoFormScreen
import com.fittrainer.pro.ui.screens.AvaliacoesScreen
import com.fittrainer.pro.ui.screens.BackupScreen
import com.fittrainer.pro.ui.screens.ClienteFormScreen
import com.fittrainer.pro.ui.screens.ClienteVisualizacaoScreen
import com.fittrainer.pro.ui.screens.ClientesScreen
import com.fittrainer.pro.ui.screens.ExerciciosScreen
import com.fittrainer.pro.ui.screens.FinanceiroScreen
import com.fittrainer.pro.ui.screens.HomeScreen
import com.fittrainer.pro.ui.screens.ProgressoScreen
import com.fittrainer.pro.ui.screens.RelatoriosScreen
import com.fittrainer.pro.ui.screens.TreinoFormScreen
import com.fittrainer.pro.ui.screens.TreinoVisualizacaoScreen
import com.fittrainer.pro.ui.screens.TreinosScreen
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time
import kotlinx.coroutines.launch

private val formPrefixes = listOf("cliente/", "treino/", "aula/", "avaliacao/", "treino-view/", "cliente-view/")

private fun pageTitle(route: String): Pair<String, String> = when {
    route == "home" -> "Visão geral" to "Resumo do espaço e próximos atendimentos"
    route == "agenda" -> "Agenda" to "Treino vinculado a cada atendimento"
    route == "clientes" -> "Alunos" to "Consulte os alunos e os planos de treino"
    route == "treinos" -> "Treinos salvos" to "Treinos criados para os alunos"
    route == "exercicios" -> "Exercícios" to "Catálogo com vídeos de demonstração"
    route == "financeiro" -> "Financeiro" to "Receitas, aulas e pagamentos"
    route == "relatorios" -> "Relatórios" to "Indicadores do espaço neste mês"
    route == "avaliacoes" -> "Avaliações" to "Peso, medidas e histórico"
    route == "progresso" -> "Progresso" to "Evolução dos alunos"
    route == "backup" -> "Configurações" to "Exportar ou importar o banco local"
    else -> "FitTrainer Pro" to "espaço do treinador"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitTrainerApp(viewModel: AppViewModel, backupManager: BackupManager) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route.orEmpty()
    val isForm = formPrefixes.any { current.startsWith(it) }
    val (title, _) = pageTitle(current)

    val clientes by viewModel.clientes.collectAsStateWithLifecycle()
    val treinos by viewModel.treinos.collectAsStateWithLifecycle()
    val aulas by viewModel.aulas.collectAsStateWithLifecycle()
    val avaliacoes by viewModel.avaliacoes.collectAsStateWithLifecycle()
    val progresso by viewModel.progresso.collectAsStateWithLifecycle()
    val transacoes by viewModel.transacoes.collectAsStateWithLifecycle()
    val historico by viewModel.historico.collectAsStateWithLifecycle()

    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isForm,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = FitColors.Sidebar,
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                AppDrawer(currentRoute = current) { route ->
                    scope.launch { drawerState.close() }
                    go(route)
                }
            }
        }
    ) {
        Scaffold(
            containerColor = FitColors.Canvas,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (!isForm) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = FitColors.Canvas,
                            titleContentColor = FitColors.Ink
                        ),
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = FitColors.Ink)
                            }
                        },
                        title = {
                            Column {
                                Text("Painel  /  $title", fontSize = 13.sp, color = FitColors.Muted, fontWeight = FontWeight.Normal)
                                Text(title, fontWeight = FontWeight.Bold, color = FitColors.Ink)
                            }
                        },
                        actions = {
                            Text(
                                Time.longToday(),
                                color = FitColors.NavIdle,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .border(1.dp, FitColors.Line, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(CircleShape)
                                    .background(FitColors.Mint)
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("FT", color = FitColors.GreenDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(
                        aulas = aulas,
                        clientes = clientes,
                        treinos = treinos,
                        transacoes = transacoes,
                        onCheckIn = { aula, aluno ->
                            val valor = clientes.find { it.id == aluno.clienteId }?.valorAula
                            viewModel.checkIn(aula, aluno, valor)
                            message("Check-in registrado")
                        },
                        onOpenAgenda = { go("agenda") },
                        onOpenAula = { nav.navigate("aula/${it}") },
                        onOpenFinanceiro = { go("financeiro") },
                        onVisualizeTreino = { nav.navigate("treino-view/$it") }
                    )
                }
                composable("agenda") {
                    AgendaScreen(
                        aulas = aulas,
                        treinos = treinos,
                        onAdd = { nav.navigate("aula/new") },
                        onOpen = { nav.navigate("aula/${it.id}") },
                        onVisualizeTreino = { nav.navigate("treino-view/$it") },
                        onDelete = viewModel::deleteAula
                    )
                }
                composable("clientes") {
                    ClientesScreen(
                        clientes = clientes,
                        onAdd = { nav.navigate("cliente/new") },
                        onOpen = { nav.navigate("cliente/${it.id}") },
                        onVisualize = { nav.navigate("cliente-view/${it.id}") },
                        onDelete = viewModel::deleteCliente
                    )
                }
                composable("treinos") {
                    TreinosScreen(
                        treinos = treinos,
                        onAdd = { nav.navigate("treino/new") },
                        onOpen = { nav.navigate("treino/${it.id}") },
                        onVisualize = { nav.navigate("treino-view/${it.id}") },
                        onDelete = viewModel::deleteTreino
                    )
                }
                composable("exercicios") {
                    ExerciciosScreen(onBack = { go("home") })
                }
                composable("relatorios") {
                    RelatoriosScreen(
                        aulas = aulas,
                        clientes = clientes,
                        transacoes = transacoes,
                        onBack = { go("home") }
                    )
                }
                composable(
                    "cliente/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    ClienteFormScreen(
                        existing = clientes.find { it.id == id },
                        onBack = { nav.popBackStack() },
                        onVisualize = { nav.navigate("cliente-view/$it") },
                        onSave = {
                            viewModel.saveCliente(it)
                            nav.popBackStack()
                        }
                    )
                }
                composable(
                    "treino/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    TreinoFormScreen(
                        existing = treinos.find { it.id == id },
                        clientes = clientes,
                        onBack = { nav.popBackStack() },
                        onVisualize = { nav.navigate("treino-view/$it") },
                        onSave = {
                            viewModel.saveTreino(it)
                            nav.popBackStack()
                        }
                    )
                }
                composable(
                    "aula/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    AulaFormScreen(
                        existing = aulas.find { it.id == id },
                        clientes = clientes,
                        treinos = treinos,
                        onBack = { nav.popBackStack() },
                        onVisualizeTreino = { nav.navigate("treino-view/$it") },
                        onSave = {
                            viewModel.saveAula(it)
                            nav.popBackStack()
                        }
                    )
                }
                composable(
                    "treino-view/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    TreinoVisualizacaoScreen(
                        treino = treinos.find { it.id == id },
                        onBack = { nav.popBackStack() },
                        onSaveRealizado = {
                            viewModel.saveHistorico(it)
                            message("Treino realizado salvo no cadastro do aluno")
                            nav.popBackStack()
                        }
                    )
                }
                composable(
                    "cliente-view/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    ClienteVisualizacaoScreen(
                        cliente = clientes.find { it.id == id },
                        historico = historico,
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("avaliacoes") {
                    AvaliacoesScreen(
                        avaliacoes = avaliacoes,
                        onBack = { go("home") },
                        onAdd = { nav.navigate("avaliacao/new") },
                        onOpen = { nav.navigate("avaliacao/${it.id}") },
                        onDelete = viewModel::deleteAvaliacao
                    )
                }
                composable(
                    "avaliacao/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    AvaliacaoFormScreen(
                        existing = avaliacoes.find { it.id == id },
                        clientes = clientes,
                        onBack = { nav.popBackStack() },
                        onSave = {
                            viewModel.saveAvaliacao(it)
                            nav.popBackStack()
                        }
                    )
                }
                composable("progresso") {
                    ProgressoScreen(
                        clientes = clientes,
                        progresso = progresso,
                        onBack = { go("home") },
                        onSave = viewModel::saveProgresso,
                        onDelete = viewModel::deleteProgresso
                    )
                }
                composable("financeiro") {
                    FinanceiroScreen(
                        clientes = clientes,
                        transacoes = transacoes,
                        onBack = { go("home") },
                        onSave = viewModel::saveTransacao,
                        onDelete = viewModel::deleteTransacao
                    )
                }
                composable("backup") {
                    BackupScreen(
                        backupManager = backupManager,
                        onBack = { go("home") },
                        onMessage = ::message
                    )
                }
            }
        }
    }
}
