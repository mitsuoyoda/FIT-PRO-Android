package com.fittrainer.pro.data.export

import kotlinx.serialization.Serializable

@Serializable
data class TrainerRef(
    val id: String,
    val name: String
)

@Serializable
data class ClientExport(
    val id: String,
    val name: String,
    val birthDate: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val sex: String? = null,
    val currentWeightKg: Double? = null,
    val heightCm: Double? = null,
    val goal: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val lessonPriceCents: Int? = null,
    val defaultLocation: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PhysicalEvaluationExport(
    val id: String,
    val date: String,
    val sex: String? = null,
    val age: Int? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val bodyFatPercent: Double? = null,
    val measurementsCm: Map<String, Double> = emptyMap(),
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class FinancialPaymentExport(
    val dueDate: String,
    val amountCents: Int,
    val status: String,
    val paidAt: String? = null
)

@Serializable
data class FinancialTransactionExport(
    val id: String,
    val type: String,
    val date: String,
    val amountCents: Int,
    val reference: String? = null,
    val lessonId: String? = null,
    val paymentMethod: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class FinancialSummaryExport(
    val currency: String = "BRL",
    val balanceCents: Int? = null,
    val nextDueDate: String? = null,
    val payments: List<FinancialPaymentExport> = emptyList(),
    val transactions: List<FinancialTransactionExport> = emptyList()
)

@Serializable
data class AppointmentExport(
    val id: String,
    val date: String,
    val time: String,
    val type: String,
    val status: String,
    val durationMinutes: Int,
    val location: String? = null,
    val onlineUrl: String? = null,
    val notes: String? = null,
    val workoutId: String? = null,
    val workoutTitle: String? = null,
    val checkedIn: Boolean = false,
    val checkInTime: String? = null,
    val recordedPriceCents: Int? = null
)

@Serializable
data class HistoryExerciseExport(
    val name: String,
    val muscleGroup: String? = null,
    val setName: String? = null,
    val sets: Int,
    val repetitions: String? = null,
    val executionType: String? = null,
    val executionDuration: String? = null,
    val loadKg: Double? = null,
    val completedSets: List<Boolean> = emptyList()
)

@Serializable
data class WorkoutHistoryExport(
    val id: String,
    val workoutId: String? = null,
    val title: String,
    val date: String,
    val notes: String? = null,
    val exercises: List<HistoryExerciseExport> = emptyList()
)

@Serializable
data class ClientProfileExport(
    val schemaVersion: Int = 1,
    val type: String = "client_profile",
    val exportId: String,
    val exportedAt: String,
    val trainer: TrainerRef,
    val clientId: String,
    val client: ClientExport,
    val physicalEvaluations: List<PhysicalEvaluationExport> = emptyList(),
    val financialSummary: FinancialSummaryExport = FinancialSummaryExport(),
    val appointments: List<AppointmentExport> = emptyList(),
    val workoutHistory: List<WorkoutHistoryExport> = emptyList()
)

@Serializable
data class VideoLinkExport(
    val label: String,
    val url: String
)

@Serializable
data class WeeklyExerciseExport(
    val id: String,
    val name: String,
    val sets: Int,
    val repetitions: String? = null,
    val loadKg: Double? = null,
    val restSeconds: Int? = null,
    val muscleGroup: String? = null,
    val executionType: String? = null,
    val executionDuration: String? = null,
    val instructions: String? = null,
    val catalogId: String? = null,
    val videoUrl: String? = null,
    val alternateVideoUrl: String? = null,
    val links: List<VideoLinkExport> = emptyList()
)

@Serializable
data class WeeklySetExport(
    val id: String,
    val name: String,
    val restSeconds: Int = 0,
    val exercises: List<WeeklyExerciseExport> = emptyList()
)

@Serializable
data class WeeklyDayExport(
    val date: String,
    val title: String,
    val notes: String? = null,
    val workoutSets: List<WeeklySetExport> = emptyList(),
    val exercises: List<WeeklyExerciseExport> = emptyList()
)

@Serializable
data class WeeklyTrainingExport(
    val schemaVersion: Int = 1,
    val type: String = "weekly_training",
    val exportId: String,
    val exportedAt: String,
    val trainer: TrainerRef,
    val clientId: String,
    val trainerWorkoutId: String,
    val title: String,
    val description: String? = null,
    val workoutType: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val sentAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val weekStart: String,
    val weekEnd: String,
    val days: List<WeeklyDayExport> = emptyList()
)
