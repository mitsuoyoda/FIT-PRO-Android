package com.fittrainer.pro.data.local

import kotlinx.serialization.SerialName
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
data class WorkoutSet(
    val id: String = "",
    val nome: String = "",
    val exercicios: List<WorkoutExercise> = emptyList(),
    val descansoSegundos: Int = 60
) {
    fun titulo(indice: Int): String = nome.trim().ifBlank { "SET ${indice + 1}" }
}

@Serializable
data class ExecucaoExercicio(
    val nome: String = "",
    val grupoMuscular: String = "",
    val series: Int = 1,
    val repeticoes: String = "",
    val tipoExecucao: String = "repeticao",
    val tempoExecucao: Int? = null,
    val pesoKg: String = "",
    val seriesFeitas: List<Boolean> = emptyList(),
    val setId: String = "",
    val setNome: String = ""
)

@Serializable
data class AulaAluno(
    val clienteId: String,
    val clienteNome: String,
    val treinoId: String? = null,
    val treinoTitulo: String? = null,
    val checkInRealizado: Boolean = false,
    val checkInHorario: String? = null,
    val valorAulaRegistrado: Double? = null
) {
    fun nomeExibicao(clientes: List<ClienteEntity> = emptyList()): String =
        clienteNome.ifBlank { clientes.find { it.id == clienteId }?.nome }.orEmpty()

    fun treinoExibicao(treinos: List<TreinoEntity> = emptyList()): String? =
        treinoTitulo?.takeIf { it.isNotBlank() } ?: treinos.find { it.id == treinoId }?.titulo
}

@Serializable
data class DobrasCutaneas(
    @SerialName("triceps") val triceps: Double? = null,
    @SerialName("suprailiaca") val suprailiaca: Double? = null,
    @SerialName("coxa") val coxa: Double? = null,
    @SerialName("peitoral") val peitoral: Double? = null,
    @SerialName("abdomen") val abdomen: Double? = null,
    @SerialName("biceps") val biceps: Double? = null,
    @SerialName("subescapular") val subescapular: Double? = null,
    @SerialName("axilar_media") val axilarMedia: Double? = null
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

@Serializable
data class PlanoDia(
    val diaSemana: Int,
    val treinoId: String? = null,
    val selecionadoEnvio: Boolean = false
)

@Serializable
data class PlanoSemana(
    val numero: Int,
    val dias: List<PlanoDia> = emptyList()
)
