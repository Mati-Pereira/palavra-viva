package com.palavraviva.app.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.palavraviva.app.PalavraVivaApp
import com.palavraviva.app.data.prefs.ReminderPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Worker diário: calcula o streak atual (dias consecutivos de leitura,
 * terminando hoje — ontem também conta como streak "em andamento") e
 * mostra uma notificação motivacional.
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Sem notificações habilitadas não há nada a fazer — sucesso silencioso
        // para não gerar retries inúteis até o próximo ciclo de 24h.
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        // Toggle desligado nos Ajustes: respeito duplo, além do cancelamento do
        // trabalho (cobertura para agendamentos que sobreviveram ao cancelamento).
        if (!ReminderPrefs.enabled(applicationContext).first()) {
            return Result.success()
        }

        return try {
            val streakDays = PalavraVivaApp.progressRepository
                .streakDays()
                .first()
                .map { it.day }
                .toSet()

            val message = buildMessage(computeStreak(streakDays, LocalDate.now()))
            NotificationHelper.showDailyReminder(applicationContext, message)
            Result.success()
        } catch (e: CancellationException) {
            // Stop do sistema — deixa o WorkManager lidar com o cancelamento.
            throw e
        } catch (_: Exception) {
            // Banco indisponível/erro transitório — deixa para o próximo ciclo.
            Result.failure()
        }
    }

    /** Streak calculado a partir de [days] ("yyyy-MM-dd"), terminando hoje ou ontem. */
    internal fun computeStreak(days: Set<String>, today: LocalDate): StreakInfo {
        val yesterday = today.minusDays(1)
        // Se hoje ainda não foi marcado, ontem mantém o streak "em andamento".
        val anchor = when {
            today.toString() in days -> today
            yesterday.toString() in days -> yesterday
            else -> return StreakInfo(count = 0, includesToday = false)
        }

        var count = 0
        var cursor = anchor
        while (cursor.toString() in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return StreakInfo(count = count, includesToday = anchor == today)
    }

    /** Texto motivacional conforme o estado do streak. */
    internal fun buildMessage(streak: StreakInfo): String = when {
        // Banco vazio / primeira leitura — convite para começar.
        streak.count == 0 -> "Sua jornada começa hoje. Que tal um versículo para começar? ✨"
        streak.includesToday -> "Continue firme! Streak de ${streak.count} ${diaOuDias(streak.count)} 🔥"
        // Streak vivo até ontem: ainda dá tempo de não perder hoje.
        else -> "Não perca o ritmo! Streak de ${streak.count} ${diaOuDias(streak.count)} aguarda a leitura de hoje 🔥"
    }

    private fun diaOuDias(count: Int): String = if (count == 1) "dia" else "dias"

    /** Resumo do streak para montar a mensagem. */
    internal data class StreakInfo(
        val count: Int,
        /** true quando o próprio dia de hoje já foi marcado como lido. */
        val includesToday: Boolean,
    )
}
