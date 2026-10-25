package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.data.catalog.ExerciseCatalog
import com.fittrainer.pro.data.catalog.ExerciseCatalogItem
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.ui.components.ExerciseInfoDialog
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.util.Time
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciciosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val catalogo = remember { ExerciseCatalog.items(context) }
    var busca by rememberSaveable { mutableStateOf("") }
    var detalhe by remember { mutableStateOf<ExerciseCatalogItem?>(null) }
    val lista = remember(busca, catalogo) {
        if (busca.trim().length < 2) catalogo.take(40)
        else ExerciseCatalog.search(context, busca, 40)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("${catalogo.size} movimentos no catálogo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    label = { Text("Buscar exercício ou músculo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(lista, key = { it.id.ifBlank { it.displayName } }) { item ->
                InfoCard {
                    Column(Modifier.clickable { detalhe = item }.fillMaxWidth()) {
                        Text(item.displayName, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(
                                item.grupo.takeIf { it.isNotBlank() },
                                item.calorias.takeIf { it.isNotBlank() }?.let { "$it kcal/h" }
                            ).joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    detalhe?.let { ExerciseInfoDialog(item = it, onDismiss = { detalhe = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(
    aulas: List<AulaEntity>,
    clientes: List<ClienteEntity>,
    transacoes: List<TransacaoEntity>,
    onBack: () -> Unit
) {
    val mes = Time.today().take(7)
    val aulasMes = aulas.filter { it.data.startsWith(mes) && it.status != "cancelada" }
    val realizadas = aulasMes.count { aula ->
        JsonStore.decodeAlunos(aula.alunosJson).any { it.checkInRealizado }
    }
    val ativos = clientes.count { it.status == "ativo" }
    val recebido = transacoes.filter {
        it.tipoTransacao == "pagamento" && it.dataTransacao.take(7) == mes
    }.sumOf { kotlin.math.abs(it.valor) }
    val hoje = LocalDate.now()
    val tituloMes = hoje.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("$tituloMes de ${hoje.year}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            InfoCard {
                Text("Sessões com check-in", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$realizadas de ${aulasMes.size} agendadas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            InfoCard {
                Text("Alunos ativos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$ativos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            InfoCard {
                Text("Receita recebida no mês", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("R$ ${"%.2f".format(Locale("pt", "BR"), recebido)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
