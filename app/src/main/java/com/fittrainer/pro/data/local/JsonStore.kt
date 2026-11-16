package com.fittrainer.pro.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object JsonStore {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeSets(items: List<WorkoutSet>): String = json.encodeToString(items)

    fun decodeSets(raw: String): List<WorkoutSet> {
        val elemento = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonArray ?: return emptyList()
        if (elemento.isEmpty()) return emptyList()
        val primeiro = elemento.first() as? JsonObject ?: return emptyList()
        return if (primeiro.containsKey("exercicios")) {
            runCatching { json.decodeFromString<List<WorkoutSet>>(raw) }.getOrDefault(emptyList())
                .mapIndexed { index, set ->
                    set.copy(id = set.id.ifBlank { "set-$index" })
                }
        } else {
            runCatching { json.decodeFromString<List<WorkoutExercise>>(raw) }.getOrDefault(emptyList())
                .mapIndexed { index, ex ->
                    WorkoutSet(
                        id = "set-$index",
                        nome = "",
                        exercicios = listOf(ex.copy(descansoSegundos = 0)),
                        descansoSegundos = ex.descansoSegundos.takeIf { it > 0 } ?: 60
                    )
                }
        }
    }

    fun encodeExercicios(items: List<WorkoutExercise>): String = encodeSets(
        items.mapIndexed { index, ex ->
            WorkoutSet(
                id = "set-$index",
                exercicios = listOf(ex.copy(descansoSegundos = 0)),
                descansoSegundos = ex.descansoSegundos.takeIf { it > 0 } ?: 60
            )
        }
    )

    fun decodeExercicios(raw: String): List<WorkoutExercise> =
        decodeSets(raw).flatMap { it.exercicios }

    fun encodeAlunos(items: List<AulaAluno>): String = json.encodeToString(items)

    fun decodeAlunos(raw: String): List<AulaAluno> =
        runCatching { json.decodeFromString<List<AulaAluno>>(raw) }.getOrDefault(emptyList())

    fun encodeExecucoes(items: List<ExecucaoExercicio>): String = json.encodeToString(items)

    fun decodeExecucoes(raw: String): List<ExecucaoExercicio> =
        runCatching { json.decodeFromString<List<ExecucaoExercicio>>(raw) }.getOrDefault(emptyList())

    fun encodeDobras(item: DobrasCutaneas): String = json.encodeToString(item)

    fun decodeDobras(raw: String): DobrasCutaneas =
        runCatching { json.decodeFromString<DobrasCutaneas>(raw) }.getOrDefault(DobrasCutaneas())

    fun encodeSemanas(items: List<PlanoSemana>): String = json.encodeToString(items)

    fun decodeSemanas(raw: String): List<PlanoSemana> =
        runCatching { json.decodeFromString<List<PlanoSemana>>(raw) }.getOrDefault(emptyList())

    fun encodeInteiros(items: List<Int>): String = json.encodeToString(items)

    fun decodeInteiros(raw: String): List<Int> =
        runCatching { json.decodeFromString<List<Int>>(raw) }.getOrDefault(emptyList())
}
