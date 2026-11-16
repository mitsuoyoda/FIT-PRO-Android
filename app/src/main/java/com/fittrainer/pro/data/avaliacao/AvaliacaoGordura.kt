package com.fittrainer.pro.data.avaliacao

import com.fittrainer.pro.data.local.DobrasCutaneas
import java.time.LocalDate
import java.time.Period
import kotlin.math.log10
import kotlin.math.roundToInt

data class ResultadoGordura(
    val percentual: Double,
    val protocolo: String
)

object AvaliacaoGordura {
    fun idadeNaData(nascimentoIso: String?, referenciaIso: String): Int? {
        val nasc = nascimentoIso?.take(10).orEmpty()
        val ref = referenciaIso.take(10)
        if (nasc.length < 10 || ref.length < 10) return null
        return runCatching {
            Period.between(LocalDate.parse(nasc), LocalDate.parse(ref)).years
        }.getOrNull()?.takeIf { it in 1..120 }
    }

    fun calcular(sexoFeminino: Boolean, idade: Int?, dobras: DobrasCutaneas): ResultadoGordura? {
        if (idade == null) return null
        setePontos(sexoFeminino, idade, dobras)?.let { return it }
        tresPontos(sexoFeminino, idade, dobras)?.let { return it }
        return quatroPontos(sexoFeminino, idade, dobras)
    }

    private fun setePontos(sexoFeminino: Boolean, idade: Int, dobras: DobrasCutaneas): ResultadoGordura? {
        val soma = soma(
            dobras.peitoral, dobras.axilarMedia, dobras.triceps, dobras.subescapular,
            dobras.abdomen, dobras.suprailiaca, dobras.coxa
        ) ?: return null
        val densidade = if (sexoFeminino) {
            1.097 - 0.00046971 * soma + 0.00000056 * soma * soma - 0.00012828 * idade
        } else {
            1.112 - 0.00043499 * soma + 0.00000055 * soma * soma - 0.00028826 * idade
        }
        return ResultadoGordura(siri(densidade), "Jackson/Pollock 7 pontos")
    }

    private fun tresPontos(sexoFeminino: Boolean, idade: Int, dobras: DobrasCutaneas): ResultadoGordura? {
        val densidade = if (sexoFeminino) {
            val s = soma(dobras.triceps, dobras.suprailiaca, dobras.coxa) ?: return null
            1.0994921 - 0.0009929 * s + 0.0000023 * s * s - 0.0001392 * idade
        } else {
            val s = soma(dobras.peitoral, dobras.abdomen, dobras.coxa) ?: return null
            1.10938 - 0.0008267 * s + 0.0000016 * s * s - 0.0002574 * idade
        }
        return ResultadoGordura(siri(densidade), "Jackson/Pollock 3 pontos")
    }

    private fun quatroPontos(sexoFeminino: Boolean, idade: Int, dobras: DobrasCutaneas): ResultadoGordura? {
        val soma = soma(dobras.biceps, dobras.triceps, dobras.subescapular, dobras.suprailiaca) ?: return null
        if (soma <= 0) return null
        val (c, m) = if (sexoFeminino) {
            when {
                idade < 20 -> 1.1549 to 0.0678
                idade < 30 -> 1.1599 to 0.0717
                idade < 40 -> 1.1423 to 0.0632
                idade < 50 -> 1.1333 to 0.0612
                else -> 1.1339 to 0.0645
            }
        } else {
            when {
                idade < 20 -> 1.1620 to 0.0630
                idade < 30 -> 1.1631 to 0.0632
                idade < 40 -> 1.1422 to 0.0544
                idade < 50 -> 1.1620 to 0.0700
                else -> 1.1715 to 0.0779
            }
        }
        return ResultadoGordura(siri(c - m * log10(soma)), "Durnin-Womersley 4 pontos")
    }

    private fun soma(vararg valores: Double?): Double? {
        if (valores.any { it == null || it < 0 }) return null
        return valores.sumOf { it!! }
    }

    private fun siri(densidade: Double): Double {
        if (densidade <= 0) return 0.0
        return (((495.0 / densidade) - 450.0) * 10).roundToInt() / 10.0
    }
}
