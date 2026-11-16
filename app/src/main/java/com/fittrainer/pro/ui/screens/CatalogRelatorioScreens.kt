package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.StatTile
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time
import java.time.LocalDate
import java.util.Locale

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
        containerColor = FitColors.Canvas
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PageHeader(
                eyebrow = "Indicadores",
                title = "$tituloMes de ${hoje.year}",
                subtitle = "Resumo das sessões, alunos e receita do mês."
            )
            StatTile("Sessões com check-in", "$realizadas/${aulasMes.size}", Icons.Outlined.Check)
            StatTile("Alunos ativos", ativos.toString(), Icons.Outlined.People)
            StatTile("Receita no mês", Time.money(recebido), Icons.Outlined.AttachMoney)
        }
    }
}
