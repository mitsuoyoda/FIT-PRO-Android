package com.fittrainer.pro.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.data.avaliacao.AvaliacaoGordura
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.ProgressoEntity
import com.fittrainer.pro.ui.components.EmptyState
import com.fittrainer.pro.ui.components.InfoCard
import com.fittrainer.pro.ui.components.PageHeader
import com.fittrainer.pro.ui.components.SimpleDropdown
import com.fittrainer.pro.ui.theme.FitColors
import com.fittrainer.pro.util.Time

private data class SerieProgresso(
    val id: String,
    val label: String,
    val unidade: String,
    val cor: Color,
    val pontos: List<PontoProgresso>
)

private data class PontoProgresso(
    val data: String,
    val valor: Double
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgressoScreen(
    clientes: List<ClienteEntity>,
    avaliacoes: List<AvaliacaoEntity>,
    progresso: List<ProgressoEntity> = emptyList(),
    onVisualizarAvaliacao: (AvaliacaoEntity) -> Unit = {}
) {
    var clienteId by rememberSaveable { mutableStateOf("") }
    var serieIsolada by rememberSaveable { mutableStateOf<String?>(null) }
    val doAluno = remember(avaliacoes, clienteId) {
        avaliacoes.filter { it.clienteId == clienteId }.sortedBy { it.dataAvaliacao }
    }
    val extras = remember(progresso, clienteId, doAluno) {
        val datasAvaliacao = doAluno.map { it.dataAvaliacao.take(10) }.toSet()
        progresso.filter { it.clienteId == clienteId && it.dataRegistro.take(10) !in datasAvaliacao }
    }
    val series = remember(doAluno, extras) { montarSeries(doAluno, extras) }
    val visiveis = series.filter { serieIsolada == null || it.id == serieIsolada }.ifEmpty { series }
    val datas = visiveis.flatMap { it.pontos.map { p -> p.data } }.distinct().sorted()
    val usarBarras = visiveis.size == 1 && datas.size in 2..3

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = FitColors.Canvas
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
                    eyebrow = "Evolução",
                    title = "Progresso",
                    subtitle = "Acompanhe as variáveis das avaliações físicas do aluno."
                )
            }
            item {
                SimpleDropdown(
                    "Aluno",
                    clienteId,
                    listOf("" to "Selecione um aluno") + clientes.map { it.id to it.nome }
                ) {
                    clienteId = it
                    serieIsolada = null
                }
            }
            if (clienteId.isBlank()) {
                item { EmptyState("Selecione um aluno para ver a evolução.") }
            } else {
                item {
                    val intervalo = if (doAluno.isEmpty()) {
                        "Nenhuma avaliação deste aluno."
                    } else {
                        val inicio = Time.formatDate(doAluno.first().dataAvaliacao)
                        val fim = Time.formatDate(doAluno.last().dataAvaliacao)
                        "${doAluno.size} avaliação${if (doAluno.size == 1) "" else "s"} · $inicio a $fim"
                    }
                    Text(intervalo, color = FitColors.Muted, fontSize = 13.sp)
                }
                when {
                    doAluno.isEmpty() -> item { EmptyState("Nenhuma avaliação deste aluno.") }
                    doAluno.size == 1 && series.isEmpty() -> item {
                        EmptyState("É preciso pelo menos duas avaliações para montar o gráfico.")
                    }
                    series.isEmpty() -> item {
                        EmptyState("É preciso pelo menos duas medições da mesma variável para montar o gráfico.")
                    }
                    else -> {
                        item {
                            InfoCard {
                                Text(
                                    if (usarBarras) "Barras · valores reais" else if (visiveis.size == 1) "Linha · valores reais" else "Linha · cada série na própria escala",
                                    fontSize = 12.sp,
                                    color = FitColors.Muted
                                )
                                Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    GraficoProgresso(
                                        series = visiveis,
                                        datas = datas,
                                        barras = usarBarras,
                                        normalizar = visiveis.size > 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
                                    )
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Legendas", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                                Button(onClick = { serieIsolada = null }) { Text("Todos") }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                series.forEach { serie ->
                                    val ativa = serieIsolada == null || serieIsolada == serie.id
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (ativa) serie.cor.copy(alpha = 0.16f) else FitColors.Paper)
                                            .border(1.dp, if (ativa) serie.cor else FitColors.Line, RoundedCornerShape(20.dp))
                                            .clickable { serieIsolada = serie.id }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (ativa) serie.cor else FitColors.Line)
                                        )
                                        Text(
                                            "${serie.label} (${serie.unidade})",
                                            fontSize = 12.sp,
                                            color = if (ativa) FitColors.Ink else FitColors.Muted,
                                            fontWeight = if (serieIsolada == serie.id) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (doAluno.isNotEmpty()) {
                    item {
                        Text("Avaliações do aluno", fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                    }
                    items(doAluno.sortedByDescending { it.dataAvaliacao }, key = { it.id }) { item ->
                        InfoCard {
                            Text(Time.formatDate(item.dataAvaliacao), fontWeight = FontWeight.SemiBold, color = FitColors.Ink)
                            Text(
                                buildString {
                                    item.peso?.let { append("$it kg") }
                                    gorduraDe(item)?.let {
                                        if (isNotEmpty()) append(" · ")
                                        append("$it% gordura")
                                    }
                                }.ifBlank { "Sem peso ou % gordura" },
                                fontSize = 12.sp,
                                color = FitColors.Muted
                            )
                            OutlinedButton(
                                onClick = { onVisualizarAvaliacao(item) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) { Text("Visualizar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraficoProgresso(
    series: List<SerieProgresso>,
    datas: List<String>,
    barras: Boolean,
    normalizar: Boolean,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = FitColors.Muted, fontSize = 12.sp)
    Canvas(modifier) {
        if (datas.isEmpty() || series.isEmpty()) return@Canvas
        val left = 46.dp.toPx()
        val right = 10.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = 32.dp.toPx()
        val plotW = size.width - left - right
        val plotH = size.height - top - bottom
        val n = datas.size

        fun xAt(index: Int): Float =
            left + if (n <= 1) plotW / 2f else plotW * index / (n - 1)

        val yBounds = if (normalizar) 0.0 to 1.0 else {
            val vals = series.flatMap { it.pontos.map { p -> p.valor } }
            val min = vals.minOrNull() ?: 0.0
            val max = vals.maxOrNull() ?: 1.0
            val pad = ((max - min) * 0.12).coerceAtLeast(0.5)
            (min - pad) to (max + pad)
        }

        fun yAt(valor: Double, serie: SerieProgresso): Float {
            val lo: Double
            val hi: Double
            if (normalizar) {
                val vs = serie.pontos.map { it.valor }
                val min = vs.minOrNull() ?: 0.0
                val max = vs.maxOrNull() ?: 1.0
                val t = if (max == min) 0.5 else (valor - min) / (max - min)
                return top + plotH - (t.toFloat() * plotH)
            } else {
                lo = yBounds.first
                hi = yBounds.second
            }
            val span = (hi - lo).takeIf { it != 0.0 } ?: 1.0
            return top + plotH - (((valor - lo) / span) * plotH).toFloat()
        }

        repeat(4) { i ->
            val y = top + plotH * i / 3f
            drawLine(FitColors.Line, Offset(left, y), Offset(left + plotW, y), strokeWidth = 1.dp.toPx())
            if (!normalizar) {
                val (lo, hi) = yBounds
                val valor = hi - (hi - lo) * i / 3.0
                val label = if (valor >= 10) "%.0f".format(valor) else "%.1f".format(valor)
                val layout = measurer.measure(label, labelStyle)
                drawText(measurer, label, Offset(4.dp.toPx(), y - layout.size.height / 2f), labelStyle)
            }
        }

        if (barras && series.size == 1) {
            val serie = series.first()
            val barW = (plotW / n.coerceAtLeast(1)) * 0.45f
            datas.forEachIndexed { i, data ->
                val ponto = serie.pontos.find { it.data == data } ?: return@forEachIndexed
                val x = xAt(i)
                val y = yAt(ponto.valor, serie)
                val h = (top + plotH) - y
                drawRect(
                    color = serie.cor,
                    topLeft = Offset(x - barW / 2f, y),
                    size = Size(barW, h.coerceAtLeast(2.dp.toPx()))
                )
            }
        } else {
            series.forEach { serie ->
                val path = Path()
                var started = false
                datas.forEachIndexed { i, data ->
                    val ponto = serie.pontos.find { it.data == data } ?: return@forEachIndexed
                    val x = xAt(i)
                    val y = yAt(ponto.valor, serie)
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
                if (started) {
                    drawPath(path, serie.cor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
                datas.forEachIndexed { i, data ->
                    val ponto = serie.pontos.find { it.data == data } ?: return@forEachIndexed
                    drawCircle(serie.cor, radius = 5.dp.toPx(), center = Offset(xAt(i), yAt(ponto.valor, serie)))
                }
            }
        }

        datas.forEachIndexed { i, data ->
            val label = runCatching { Time.formatDate(data).dropLast(5) }.getOrDefault(data.takeLast(5))
            val layout = measurer.measure(label, labelStyle)
            drawText(
                measurer,
                label,
                Offset(xAt(i) - layout.size.width / 2f, size.height - bottom + 8.dp.toPx()),
                labelStyle
            )
        }
    }
}

private fun montarSeries(
    avaliacoes: List<AvaliacaoEntity>,
    extras: List<ProgressoEntity>
): List<SerieProgresso> {
    fun pontos(extrair: (AvaliacaoEntity) -> Double?): List<PontoProgresso> =
        avaliacoes.mapNotNull { av -> extrair(av)?.let { PontoProgresso(av.dataAvaliacao.take(10), it) } }

    fun pontosPeso(): List<PontoProgresso> =
        (pontos { it.peso } + extras.mapNotNull { p ->
            p.peso?.let { PontoProgresso(p.dataRegistro.take(10), it) }
        }).sortedBy { it.data }

    fun pontosGordura(): List<PontoProgresso> =
        (pontos { gorduraDe(it) } + extras.mapNotNull { p ->
            p.percentualGordura?.let { PontoProgresso(p.dataRegistro.take(10), it) }
        }).sortedBy { it.data }

    val defs = listOf(
        Triple("peso", "Peso" to "kg", FitColors.Green) to pontosPeso(),
        Triple("gordura", "% gordura" to "%", FitColors.Orange) to pontosGordura(),
        Triple("altura", "Altura" to "cm", FitColors.Dobra7Ink) to pontos { it.altura },
        Triple("triceps", "Tríceps" to "mm", Color(0xFF216D4D)) to pontos { JsonStore.decodeDobras(it.medidasJson).triceps },
        Triple("suprailiaca", "Supra-ilíaca" to "mm", Color(0xFF3A9B6E)) to pontos { JsonStore.decodeDobras(it.medidasJson).suprailiaca },
        Triple("coxa", "Coxa" to "mm", Color(0xFF17543A)) to pontos { JsonStore.decodeDobras(it.medidasJson).coxa },
        Triple("peitoral", "Peitoral" to "mm", Color(0xFF5BA882)) to pontos { JsonStore.decodeDobras(it.medidasJson).peitoral },
        Triple("abdomen", "Abdômen" to "mm", Color(0xFF2E7A55)) to pontos { JsonStore.decodeDobras(it.medidasJson).abdomen },
        Triple("biceps", "Bíceps" to "mm", FitColors.Dobra4Ink) to pontos { JsonStore.decodeDobras(it.medidasJson).biceps },
        Triple("subescapular", "Subescapular" to "mm", Color(0xFFB08960)) to pontos { JsonStore.decodeDobras(it.medidasJson).subescapular },
        Triple("axilar", "Axilar média" to "mm", FitColors.Dobra7Ink) to pontos { JsonStore.decodeDobras(it.medidasJson).axilarMedia }
    )
    return defs.mapNotNull { (meta, pts) ->
        if (pts.size < 2) return@mapNotNull null
        val (id, nomeUnidade, cor) = meta
        SerieProgresso(id, nomeUnidade.first, nomeUnidade.second, cor, pts)
    }
}

private fun gorduraDe(avaliacao: AvaliacaoEntity): Double? {
    val calc = AvaliacaoGordura.calcular(
        sexoFeminino = avaliacao.sexo == "F",
        idade = avaliacao.idade,
        dobras = JsonStore.decodeDobras(avaliacao.medidasJson)
    )
    return calc?.percentual ?: avaliacao.percentualGordura
}
