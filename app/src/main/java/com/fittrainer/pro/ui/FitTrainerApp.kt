package com.fittrainer.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fittrainer.pro.data.assessoria.PlanosAssessoria
import com.fittrainer.pro.data.assessoria.AssessoriaCsv
import com.fittrainer.pro.data.firebase.TrainerFirebaseSync
import com.fittrainer.pro.data.firebase.TrainerClientLink
import com.fittrainer.pro.data.export.ClienteExport
import com.fittrainer.pro.data.export.ClienteExportShare
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.backup.BackupManager
import com.fittrainer.pro.data.checkin.CheckInLogic
import com.fittrainer.pro.data.checkin.CheckInPedido
import com.fittrainer.pro.data.local.AulaAluno
import com.fittrainer.pro.ui.components.CheckInConfirmDialog
import com.fittrainer.pro.ui.screens.AssessoriaEnvioScreen
import com.fittrainer.pro.ui.screens.AssessoriaFormScreen
import com.fittrainer.pro.ui.screens.AssessoriaScreen
import com.fittrainer.pro.ui.screens.AgendaScreen
import com.fittrainer.pro.ui.screens.AulaFormScreen
import com.fittrainer.pro.ui.screens.AvaliacaoFormScreen
import com.fittrainer.pro.ui.screens.AvaliacaoVisualizacaoScreen
import com.fittrainer.pro.ui.screens.AvaliacoesScreen
import com.fittrainer.pro.ui.screens.BackupScreen
import com.fittrainer.pro.ui.screens.ClienteFormScreen
import com.fittrainer.pro.ui.screens.ClienteVisualizacaoScreen
import com.fittrainer.pro.ui.screens.ClientesScreen
import com.fittrainer.pro.ui.screens.ExercicioFormScreen
import com.fittrainer.pro.ui.screens.ExerciciosScreen
import com.fittrainer.pro.ui.screens.FinanceiroScreen
import com.fittrainer.pro.ui.screens.HomeScreen
import com.fittrainer.pro.ui.screens.ProgressoScreen
import com.fittrainer.pro.ui.screens.RelatoriosScreen
import com.fittrainer.pro.ui.screens.TreinoFormScreen
import com.fittrainer.pro.ui.screens.TreinoVisualizacaoScreen
import com.fittrainer.pro.ui.screens.TreinosScreen
import com.fittrainer.pro.ui.screens.TrainerIntegrationScreen
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time
import kotlinx.coroutines.launch

private val formPrefixes = listOf("cliente/", "treino/", "aula/", "avaliacao/", "avaliacao-view/", "treino-view/", "cliente-view/", "exercicio/", "assessoria/", "assessoria-envio/", "integracao/")

private fun pageTitle(route: String): Pair<String, String> = when {
    route == "home" -> "Visão geral" to "Resumo do espaço e próximos atendimentos"
    route == "agenda" -> "Agenda" to "Treino vinculado a cada atendimento"
    route == "clientes" -> "Alunos" to "Consulte os alunos e os planos de treino"
    route.startsWith("integracao/") -> "Vínculo com o app" to "Treinos, mensagens e retornos do aluno"
    route == "treinos" -> "Treinos salvos" to "Treinos criados para os alunos"
    route == "assessoria" -> "Assessoria" to "Planos semanais e compartilhamento de arquivo"
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
    val exerciciosBiblioteca by viewModel.exerciciosBiblioteca.collectAsStateWithLifecycle()
    val planosAssessoria by viewModel.planosAssessoria.collectAsStateWithLifecycle()
    val cloudBackupStatus by viewModel.cloudBackupStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.syncCloudBackup(context) }
    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.syncCloudBackup(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    fun exportarPerfil(cliente: ClienteEntity) {
        val json = ClienteExport.perfil(
            cliente = cliente,
            avaliacoes = avaliacoes,
            transacoes = transacoes,
            aulas = aulas,
            historico = historico
        )
        val ok = ClienteExportShare.compartilhar(
            context,
            listOf(ClienteExport.nomePerfil(cliente) to json),
            "FitTrainer Pro — dados de ${cliente.nome} para o app do aluno."
        )
        if (ok) message("JSON do aluno pronto para o app do cliente")
        else Toast.makeText(context, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_LONG).show()
    }

    fun exportarTreino(treino: TreinoEntity) {
        val alunoId = treino.clienteId
        if (alunoId.isBlank()) {
            Toast.makeText(context, "Associe um aluno a este treino para exportar ao app.", Toast.LENGTH_LONG).show()
            return
        }
        val json = ClienteExport.treinoAvulso(treino, alunoId)
        if (json == null) {
            Toast.makeText(context, "Não foi possível montar o JSON do treino.", Toast.LENGTH_LONG).show()
            return
        }
        val ok = ClienteExportShare.compartilhar(
            context,
            listOf(ClienteExport.nomeTreino(treino) to json),
            "FitTrainer Pro — treino ${treino.titulo} para o app do aluno."
        )
        if (ok) message("JSON do treino pronto para o app do cliente")
        else Toast.makeText(context, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_LONG).show()
    }

    suspend fun linkAtivoParaEnvio(clienteId: String): TrainerClientLink? {
        val link = TrainerFirebaseSync.loadLink(context, clienteId)
        if (link?.status != "active") {
            message(
                if (link?.status == "pending") "Aguardando o aluno aceitar o vínculo."
                else "Vincule este aluno ao app e aguarde o aceite para enviar."
            )
            nav.navigate("integracao/$clienteId")
            return null
        }
        return link
    }

    fun enviarTreinoAoApp(treino: TreinoEntity) {
        val clienteId = treino.clienteId
        if (clienteId.isBlank()) {
            message("Associe um aluno cadastrado ao treino antes de enviar.")
            nav.navigate("treino/${treino.id}")
            return
        }
        scope.launch {
            try {
                val link = linkAtivoParaEnvio(clienteId) ?: return@launch
                val payload = ClienteExport.treinoAvulso(treino, clienteId)
                    ?: error("Não foi possível preparar este treino.")
                val exportId = "training-$clienteId-${Time.today()}-${treino.id}"
                TrainerFirebaseSync.publishPlan(context, link, exportId, treino.titulo, payload)
                message("Treino enviado para sincronização com o app de ${link.clientName}.")
            } catch (error: Exception) {
                message(error.message ?: "Não foi possível enviar o treino. Verifique sua conexão.")
            }
        }
    }

    fun enviarPlanoAoApp(plano: com.fittrainer.pro.data.local.PlanoAssessoriaEntity) {
        val clienteId = plano.clienteId
        if (clienteId.isBlank()) {
            message("Associe um aluno cadastrado ao plano antes de enviar.")
            nav.navigate("assessoria/${plano.id}")
            return
        }
        scope.launch {
            try {
                val link = linkAtivoParaEnvio(clienteId) ?: return@launch
                val semanas = AssessoriaCsv.diasComTreino(plano).groupBy { it.semana }.toSortedMap()
                check(semanas.isNotEmpty()) { "Este plano ainda não tem treinos associados." }
                val primeiraSemana = semanas.keys.first()
                val inicioBase = Time.mondayOf(Time.today())
                val publicacoes = semanas.map { (semana, dias) ->
                    val inicio = inicioBase.plusWeeks((semana - primeiraSemana).toLong()).toString()
                    val payload = ClienteExport.planoSemanal(plano, clienteId, semana, dias, treinos, inicio)
                        ?: error("Não foi possível preparar a semana $semana.")
                    Triple(
                        "training-$clienteId-$inicio-${plano.id}-s$semana",
                        "${plano.nome} · Semana $semana",
                        payload,
                    )
                }
                publicacoes.forEach { (exportId, titulo, payload) ->
                    TrainerFirebaseSync.publishPlan(context, link, exportId, titulo, payload)
                }
                message("${publicacoes.size} semana(s) enviada(s) para o app de ${link.clientName}.")
            } catch (error: Exception) {
                message(error.message ?: "Não foi possível enviar o plano. Verifique sua conexão.")
            }
        }
    }

    var pedidoAPagar by remember { mutableStateOf<CheckInPedido?>(null) }
    fun pedirAPagar(aulaId: String, aluno: AulaAluno, realizar: Boolean) {
        val valor = clientes.find { it.id == aluno.clienteId }?.valorAula
        pedidoAPagar = CheckInLogic.abrirPedido(pedidoAPagar, aulaId, aluno, realizar, valor)
    }

    Box(Modifier.fillMaxSize()) {
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
                                Text(title, fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
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
                                Text("FT", color = FitColors.GreenDark, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                        onPedirAPagar = { aula, aluno, realizar ->
                            pedirAPagar(aula.id, aluno, realizar)
                        },
                        onOpenAgenda = { go("agenda") },
                        onOpenAula = { nav.navigate("aula/${it}") },
                        onOpenFinanceiro = { go("financeiro") },
                        onVisualizeTreino = { nav.navigate("treino-view/$it") },
                        onRegistrarEnvioTreino = { viewModel.saveTreino(it) }
                    )
                }
                composable("agenda") {
                    AgendaScreen(
                        aulas = aulas,
                        clientes = clientes,
                        treinos = treinos,
                        onAdd = { nav.navigate("aula/new") },
                        onOpen = { nav.navigate("aula/${it.id}") },
                        onVisualizeTreino = { nav.navigate("treino-view/$it") },
                        onPedirAPagar = { aula, aluno, realizar ->
                            pedirAPagar(aula.id, aluno, realizar)
                        },
                        onRegistrarEnvioTreino = { viewModel.saveTreino(it) },
                        onDelete = viewModel::deleteAula
                    )
                }
                composable("clientes") {
                    ClientesScreen(
                        clientes = clientes,
                        onAdd = { nav.navigate("cliente/new") },
                        onOpen = { nav.navigate("cliente/${it.id}") },
                        onVisualize = { nav.navigate("cliente-view/${it.id}") },
                        onExportarApp = { exportarPerfil(it) },
                        onIntegrateApp = { nav.navigate("integracao/${it.id}") },
                        onDelete = viewModel::deleteCliente
                    )
                }
                composable("treinos") {
                    TreinosScreen(
                        treinos = treinos,
                        clientes = clientes,
                        onAdd = { nav.navigate("treino/new") },
                        onOpen = { nav.navigate("treino/${it.id}") },
                        onVisualize = { nav.navigate("treino-view/${it.id}") },
                        onDelete = viewModel::deleteTreino,
                        onRegistrarEnvio = { viewModel.saveTreino(it) },
                        onExportarApp = { exportarTreino(it) },
                        onEnviarApp = { enviarTreinoAoApp(it) }
                    )
                }
                composable("assessoria") {
                    AssessoriaScreen(
                        planos = planosAssessoria,
                        clientes = clientes,
                        onAdd = { nav.navigate("assessoria/${Time.newId()}") },
                        onOpen = { nav.navigate("assessoria/${it.id}") },
                        onCopy = { original ->
                            val copia = PlanosAssessoria.copiar(original)
                            viewModel.savePlanoAssessoria(copia)
                            message("Cópia criada sem aluno")
                            nav.navigate("assessoria/${copia.id}")
                        },
                        onEnviar = { nav.navigate("assessoria-envio/${it.id}") },
                        onEnviarApp = { enviarPlanoAoApp(it) },
                        onDelete = viewModel::deletePlanoAssessoria
                    )
                }
                composable(
                    "assessoria/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    AssessoriaFormScreen(
                        planoId = id,
                        existing = planosAssessoria.find { it.id == id },
                        clientes = clientes,
                        treinos = treinos,
                        onBack = { nav.popBackStack() },
                        onSave = { plano ->
                            viewModel.savePlanoAssessoria(plano)
                            message("Plano salvo neste aparelho")
                        },
                        onCriarTreino = { plano, semana, dia ->
                            viewModel.prepararTreinoNaAssessoria(plano, semana, dia) {
                                nav.navigate("treino/new")
                            }
                        },
                        onEnviar = { nav.navigate("assessoria-envio/$id") }
                    )
                }
                composable(
                    "assessoria-envio/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    AssessoriaEnvioScreen(
                        plano = planosAssessoria.find { it.id == id },
                        clientes = clientes,
                        treinos = treinos,
                        onBack = { nav.popBackStack() },
                        onRegistrarEnvio = {
                            viewModel.savePlanoAssessoria(it)
                            message("Compartilhamento registrado")
                        }
                    )
                }
                composable("exercicios") {
                    ExerciciosScreen(
                        biblioteca = exerciciosBiblioteca,
                        onAdd = { nav.navigate("exercicio/new") },
                        onEdit = { nav.navigate("exercicio/$it") },
                        onDelete = viewModel::deleteExercicioBiblioteca
                    )
                }
                composable(
                    "exercicio/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    ExercicioFormScreen(
                        existingId = id.takeUnless { it == "new" },
                        biblioteca = exerciciosBiblioteca,
                        onBack = { nav.popBackStack() },
                        onSave = {
                            viewModel.saveExercicioBiblioteca(it)
                            nav.popBackStack()
                        }
                    )
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
                    "integracao/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val cliente = clientes.find { it.id == id }
                    if (cliente != null) {
                        TrainerIntegrationScreen(
                            cliente = cliente,
                            treinos = treinos.filter { it.clienteId == id },
                            planosAssessoria = planosAssessoria,
                            onAccountReady = { viewModel.syncCloudBackup(context) },
                            onBack = { nav.popBackStack() },
                        )
                    }
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
                        biblioteca = exerciciosBiblioteca,
                        onBack = { nav.popBackStack() },
                        onVisualize = { nav.navigate("treino-view/$it") },
                        onSave = { treino ->
                            viewModel.saveTreino(treino) { nav.popBackStack() }
                        },
                        onRegistrarEnvio = { viewModel.saveTreino(it) }
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
                        onPedirAPagar = { aulaId, aluno, realizar ->
                            pedirAPagar(aulaId, aluno, realizar)
                        },
                        onRegistrarEnvioTreino = { viewModel.saveTreino(it) },
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
                        clientes = clientes,
                        onBack = { nav.popBackStack() },
                        onRegistrarEnvio = { viewModel.saveTreino(it) },
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
                        transacoes = transacoes,
                        avaliacoes = avaliacoes.filter { it.clienteId == id },
                        onBack = { nav.popBackStack() },
                        onVisualizarAvaliacao = { nav.navigate("avaliacao-view/${it.id}") },
                        onEditarAvaliacao = { nav.navigate("avaliacao/${it.id}") },
                        onExcluirAvaliacao = viewModel::deleteAvaliacao,
                        onExportarApp = { clientes.find { it.id == id }?.let(::exportarPerfil) }
                    )
                }
                composable("avaliacoes") {
                    AvaliacoesScreen(
                        avaliacoes = avaliacoes,
                        clientes = clientes,
                        onAdd = { nav.navigate("avaliacao/new") },
                        onVisualize = { nav.navigate("avaliacao-view/${it.id}") },
                        onEdit = { nav.navigate("avaliacao/${it.id}") },
                        onDelete = viewModel::deleteAvaliacao
                    )
                }
                composable(
                    "avaliacao-view/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    AvaliacaoVisualizacaoScreen(
                        avaliacao = avaliacoes.find { it.id == id },
                        onBack = { nav.popBackStack() },
                        onEdit = { nav.navigate("avaliacao/$id") }
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
                        avaliacoes = avaliacoes,
                        progresso = progresso,
                        onVisualizarAvaliacao = { nav.navigate("avaliacao-view/${it.id}") }
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
                        cloudBackupStatus = cloudBackupStatus,
                        onSyncCloudBackup = { viewModel.syncCloudBackup(context) },
                        onBack = { go("home") },
                        onMessage = ::message
                    )
                }
            }
        }
    }
    pedidoAPagar?.let { pedido ->
        CheckInConfirmDialog(
            alunoNome = pedido.aluno.clienteNome,
            realizar = pedido.realizar,
            valorAula = pedido.valor,
            onConfirm = {
                viewModel.setCheckIn(pedido.aulaId, pedido.aluno, pedido.realizar, pedido.valor)
                message(if (pedido.realizar) "Aula lançada a pagar" else "Lançamento removido")
                pedidoAPagar = null
            },
            onDismiss = { pedidoAPagar = null }
        )
    }
    }
}
