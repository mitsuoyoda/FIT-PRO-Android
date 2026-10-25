package com.fittrainer.pro.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonStore {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeExercicios(items: List<WorkoutExercise>): String = json.encodeToString(items)

    fun decodeExercicios(raw: String): List<WorkoutExercise> =
        runCatching { json.decodeFromString<List<WorkoutExercise>>(raw) }.getOrDefault(emptyList())

    fun encodeAlunos(items: List<AulaAluno>): String = json.encodeToString(items)

    fun decodeAlunos(raw: String): List<AulaAluno> =
        runCatching { json.decodeFromString<List<AulaAluno>>(raw) }.getOrDefault(emptyList())

    fun encodeExecucoes(items: List<ExecucaoExercicio>): String = json.encodeToString(items)

    fun decodeExecucoes(raw: String): List<ExecucaoExercicio> =
        runCatching { json.decodeFromString<List<ExecucaoExercicio>>(raw) }.getOrDefault(emptyList())
}
