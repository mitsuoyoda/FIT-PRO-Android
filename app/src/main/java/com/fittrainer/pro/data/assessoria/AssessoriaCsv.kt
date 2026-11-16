package com.fittrainer.pro.data.assessoria

import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.PlanoAssessoriaEntity

data class DiaPlanoTreino(
    val semana: Int,
    val diaSemana: Int,
    val treinoId: String
)

object AssessoriaCsv {
    fun diasComTreino(plano: PlanoAssessoriaEntity): List<DiaPlanoTreino> =
        JsonStore.decodeSemanas(plano.semanasJson).flatMap { semana ->
            semana.dias.mapNotNull { dia ->
                dia.treinoId?.takeIf { it.isNotBlank() }?.let { id ->
                    DiaPlanoTreino(semana.numero, dia.diaSemana, id)
                }
            }
        }
}
