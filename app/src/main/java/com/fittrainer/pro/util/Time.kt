package com.fittrainer.pro.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

object Time {
    private val localePt = Locale("pt", "BR")
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", localePt)
    private val longDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", localePt)
    private val isoDateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun nowIso(): String = LocalDateTime.now().format(isoDateTime)

    fun today(): String = LocalDate.now().format(dateFmt)

    fun newId(): String = UUID.randomUUID().toString()

    fun formatDate(isoDate: String): String = runCatching {
        LocalDate.parse(isoDate.take(10)).format(displayDate)
    }.getOrDefault(isoDate)

    fun formatMmSs(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = safe / 60
        val seconds = safe % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun sanitizeMmSs(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(4)
        return when {
            digits.length <= 2 -> digits
            else -> digits.dropLast(2) + ":" + digits.takeLast(2)
        }
    }

    fun parseMmSs(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return if (':' in trimmed) {
            val parts = trimmed.split(":")
            val minutes = parts.getOrNull(0)?.ifBlank { "0" }?.toIntOrNull() ?: return null
            val seconds = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2).toIntOrNull() ?: 0
            minutes * 60 + seconds.coerceAtMost(59)
        } else {
            when (digits.length) {
                1, 2 -> digits.toInt()
                else -> digits.dropLast(2).toInt() * 60 + digits.takeLast(2).toInt().coerceAtMost(59)
            }
        }
    }

    fun longToday(): String = LocalDate.now().format(longDate).replaceFirstChar { it.uppercase() }

    fun greeting(): String {
        val hora = LocalDateTime.now().hour
        return when {
            hora < 12 -> "Bom dia"
            hora < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
    }
}
