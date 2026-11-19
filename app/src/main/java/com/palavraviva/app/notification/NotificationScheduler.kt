package com.palavraviva.app.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Agendamento idempotente do lembrete diário (WorkManager).
 * Chamado pelo Application.onCreate — re-chamadas não re-agendam o trabalho.
 */
object NotificationScheduler {

    const val WORK_NAME = "daily_reminder"
    private const val REMINDER_HOUR = 9

    /**
     * Garante que exista um PeriodicWorkRequest de [DailyReminderWorker]
     * rodando a cada 24h, com política KEEP (não substitui um agendamento
     * já existente).
     */
    fun ensureScheduled(context: android.content.Context) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS,
        )
            // Primeiro disparo às 09:00 (manhã) — depois repete a cada 24h.
            .setInitialDelay(delayUntilNextReminder(), TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Cancela o lembrete diário (usado pelo toggle nos Ajustes). */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Horas até o próximo 09:00 local. Se agora é antes das 09:00, dispara
     * hoje; senão, dispara amanhã. Em qualquer caso o período se mantém
     * alinhado às manhãs seguintes.
     */
    private fun delayUntilNextReminder(): Long {
        val now = LocalDateTime.now()
        val todayAtNine = now.with(LocalTime.of(REMINDER_HOUR, 0))
        val next = if (now.isBefore(todayAtNine)) todayAtNine else todayAtNine.plusDays(1)
        val hours = Duration.between(now, next).toHours()
        // Nunca retorna 0/negativo — WorkManager exige um atraso positivo.
        return maxOf(hours, 1L)
    }
}
