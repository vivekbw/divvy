package com.divvy.divvy.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.divvy.divvy.MainActivity
import com.divvy.divvy.R
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_EXPENSES   = "divvy_expenses"
        const val CHANNEL_SETTLEMENT = "divvy_settlements"
    }

    /**
     * Creates the notification channels required by Android 8+.
     * Safe to call multiple times — the OS ignores duplicate registrations.
     * Call once in [com.divvy.divvy.DivvyApplication.onCreate].
     */
    fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EXPENSES,
                "New Expenses",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when a group member adds a new expense"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SETTLEMENT,
                "Settlements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when someone records a settlement with you"
            }
        )
    }

    fun postExpenseNotification(notificationId: Int, title: String, body: String) {
        post(notificationId, CHANNEL_EXPENSES, title, body)
    }

    fun postSettlementNotification(notificationId: Int, title: String, body: String) {
        post(notificationId, CHANNEL_SETTLEMENT, title, body)
    }

    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    // -------------------------------------------------------------------------

    private fun post(id: Int, channel: String, title: String, body: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (se: SecurityException) {
            // POST_NOTIFICATIONS was revoked at runtime after we already checked.
            // Record the breadcrumb so it shows up in Sentry context if relevant.
            Sentry.addBreadcrumb("NotificationHelper: POST_NOTIFICATIONS denied — $se")
        }
    }
}