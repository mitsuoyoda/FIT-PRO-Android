package com.fittrainer.pro.data.local

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutExercise(
    val nome: String = "",
    val grupoMuscular: String = "",
    val series: Int = 3,
    val repeticoes: String = "10",
    val descansoSegundos: Int = 0,
    val tipoExecucao: String = "repeticao",
    val tempoExecucao: Int? = null,
    val observacoes: String = "",
    val catalogId: String = "",
    val videoPrincipal: String = "",
    val videoAlternativo: String = ""
)

@Serializable
data class ExecucaoExercicio(
    val nome: String = "",
    val grupoMuscular: String = "",
    val series: Int = 1,
    val repeticoes: String = "",
    val tipoExecucao: String = "repeticao",
    val tempoExecucao: Int? = null,
    val pesoKg: String = "",
    val seriesFeitas: List<Boolean> = emptyList()
)

@Serializable
data class AulaAluno(
    val clienteId: String,
    val clienteNome: String,
    val treinoId: String? = null,
    val checkInRealizado: Boolean = false,
    val checkInHorario: String? = null,
    val valorAulaRegistrado: Double? = null
)

@Serializable
data class MedidasCorporais(
    val pescoco: Double? = null,
    val ombros: Double? = null,
    val peito: Double? = null,
    val cintura: Double? = null,
    val quadril: Double? = null,
    val bracoDireito: Double? = null,
    val bracoEsquerdo: Double? = null,
    val coxaDireita: Double? = null,
    val coxaEsquerda: Double? = null
)
