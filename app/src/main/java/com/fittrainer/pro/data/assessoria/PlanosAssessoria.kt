package com.fittrainer.pro.data.assessoria

import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.PlanoAssessoriaEntity
import com.fittrainer.pro.data.local.PlanoDia
import com.fittrainer.pro.data.local.PlanoSemana
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.util.Time

object PlanosAssessoria {
    val nomesDias = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

    fun semanasVazias(duracao: Int): List<PlanoSemana> =
        (1..duracao.coerceIn(1, 12)).map { numero ->
            PlanoSemana(numero = numero, dias = (1..7).map { PlanoDia(it) })
        }

    fun normalizar(semanas: List<PlanoSemana>, duracao: Int): List<PlanoSemana> {
        val n = duracao.coerceIn(1, 12)
        val porNumero = semanas.associateBy { it.numero }
        return (1..n).map { numero ->
            val atual = porNumero[numero]
            val dias = (1..7).map { dia ->
                atual?.dias?.find { it.diaSemana == dia } ?: PlanoDia(dia)
            }
            PlanoSemana(numero, dias)
        }
    }

    fun temDadosAlem(semanas: List<PlanoSemana>, novaDuracao: Int): Boolean =
        semanas.any { it.numero > novaDuracao && it.dias.any { dia -> !dia.treinoId.isNullOrBlank() } }

    fun diasPreenchidos(semanas: List<PlanoSemana>): Int =
        semanas.sumOf { semana -> semana.dias.count { !it.treinoId.isNullOrBlank() } }

    fun nomeSugerido(clienteNome: String, duracao: Int): String {
        val aluno = clienteNome.trim()
        return if (aluno.isBlank()) "Plano de $duracao semana${if (duracao == 1) "" else "s"}"
        else "Plano de $duracao semana${if (duracao == 1) "" else "s"} — $aluno"
    }

    fun novo(
        id: String = Time.newId(),
        nome: String = nomeSugerido("", 4),
        duracao: Int = 4
    ): PlanoAssessoriaEntity {
        val now = Time.nowIso()
        return PlanoAssessoriaEntity(
            id = id,
            nome = nome,
            duracaoSemanas = duracao.coerceIn(1, 12),
            semanasJson = JsonStore.encodeSemanas(semanasVazias(duracao)),
            status = "modelo",
            createdDate = now,
            updatedDate = now
        )
    }

    fun copiar(origem: PlanoAssessoriaEntity): PlanoAssessoriaEntity {
        val now = Time.nowIso()
        return origem.copy(
            id = Time.newId(),
            nome = "Cópia de ${origem.nome}",
            clienteId = "",
            clienteNome = "",
            copiadoDeId = origem.id,
            status = "modelo",
            ultimoCompartilhamento = "",
            statusCompartilhamento = "",
            semanasCompartilhadasJson = "[]",
            createdDate = now,
            updatedDate = now
        )
    }

    fun semanasComTreino(semanas: List<PlanoSemana>): Set<Int> =
        semanas.filter { bloco -> bloco.dias.any { !it.treinoId.isNullOrBlank() } }.map { it.numero }.toSet()

    fun semanasEnviadas(plano: PlanoAssessoriaEntity): Set<Int> =
        JsonStore.decodeInteiros(plano.semanasCompartilhadasJson).toSet()

    fun registrarCompartilhamento(plano: PlanoAssessoriaEntity, semanasDestaVez: Set<Int>): PlanoAssessoriaEntity {
        val comTreino = semanasComTreino(JsonStore.decodeSemanas(plano.semanasJson))
        val enviadas = semanasEnviadas(plano) + semanasDestaVez
        val status = when {
            enviadas.isEmpty() -> ""
            comTreino.isNotEmpty() && comTreino.all { it in enviadas } -> "completo"
            else -> "parcial"
        }
        val now = Time.nowIso()
        return plano.copy(
            semanasCompartilhadasJson = JsonStore.encodeInteiros(enviadas.sorted()),
            statusCompartilhamento = status,
            ultimoCompartilhamento = now,
            updatedDate = now
        )
    }

    fun rotuloEnvio(plano: PlanoAssessoriaEntity): String? = when (plano.statusCompartilhamento) {
        "completo", "enviado" -> "Completo"
        "parcial" -> "Parcial"
        else -> null
    }

    fun mensagemDia(
        plano: PlanoAssessoriaEntity,
        semana: Int,
        dia: PlanoDia,
        treino: TreinoEntity?
    ): String {
        val diaNome = nomesDias.getOrNull(dia.diaSemana - 1) ?: "Dia ${dia.diaSemana}"
        val aluno = plano.clienteNome.takeIf { it.isNotBlank() }
        val sets = treino?.let { JsonStore.decodeSets(it.exerciciosJson) }.orEmpty()
        return buildString {
            appendLine("FitTrainer Pro — Assessoria")
            aluno?.let { appendLine("Aluno: $it") }
            appendLine("Semana $semana · $diaNome")
            if (treino == null) {
                append("Nenhum treino associado neste dia.")
                return@buildString
            }
            appendLine("Treino: ${treino.titulo}")
            appendLine("Duração: ${treino.duracaoEstimada} min")
            if (treino.descricao.isNotBlank()) appendLine(treino.descricao)
            if (sets.isNotEmpty()) {
                sets.forEachIndexed { i, set ->
                    appendLine(set.titulo(i))
                    set.exercicios.forEachIndexed { j, ex ->
                        val detalhe = if (ex.tipoExecucao == "tempo") {
                            ex.tempoExecucao?.let { Time.formatMmSs(it) } ?: ""
                        } else ex.repeticoes
                        appendLine("  ${j + 1}. ${ex.nome.ifBlank { "Exercício" }} · ${ex.series}x $detalhe")
                    }
                    if (set.descansoSegundos > 0) {
                        appendLine("  Descanso após o SET: ${Time.formatMmSs(set.descansoSegundos)}")
                    }
                }
            }
        }.trim()
    }

    fun mensagemConsolidada(
        plano: PlanoAssessoriaEntity,
        semana: Int,
        dias: List<PlanoDia>,
        treinos: List<TreinoEntity>
    ): String = dias.joinToString("\n\n———\n\n") { dia ->
        mensagemDia(plano, semana, dia, treinos.find { it.id == dia.treinoId })
    }
}
