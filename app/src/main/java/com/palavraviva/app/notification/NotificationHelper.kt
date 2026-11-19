package com.palavraviva.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.palavraviva.app.MainActivity

/**
 * Criação do canal de notificação + helper de construção/envio da notificação
 * diária. Não cria drawables novos — reaproveita um ícone do próprio Android.
 */
object NotificationHelper {

    const val CHANNEL_ID = "daily_reminder"
    private const val CHANNEL_NAME = "Lembretes"
    private const val NOTIFICATION_ID = 1001

    /** Garante que o canal "daily_reminder" exista (idempotente). */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Lembrete diário de leitura e streak"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Mostra a notificação diária. Não faz nada caso o usuário tenha
     * desativado notificações para o app.
     */
    fun showDailyReminder(context: Context, text: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        ensureChannel(context)

        // Abre o app direto na MainActivity ao tocar na notificação.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // Ícone do sistema (não criamos drawables novos).
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Palavra Viva")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o notify — ignora com elegância.
        }
    }
}
