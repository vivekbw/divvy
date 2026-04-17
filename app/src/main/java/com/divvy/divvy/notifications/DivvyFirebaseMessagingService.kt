package com.divvy.divvy.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.divvy.divvy.backend.PushTokenRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DivvyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var pushTokenRepository: PushTokenRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val data  = message.data
        val title = data["title"] ?: message.notification?.title ?: "New expense"
        val body  = data["body"]  ?: message.notification?.body  ?: ""
        val type  = data["type"]  ?: "expense"
        val id    = System.currentTimeMillis().toInt()

        if (type == "settlement") {
            notificationHelper.postSettlementNotification(id, title, body)
        } else {
            notificationHelper.postExpenseNotification(id, title, body)
        }
    }

    // Called when FCM rotates the token — re-sync it to Supabase.
    // This is best-effort and intentionally non-fatal.
    override fun onNewToken(token: String) {
        scope.launch {
            pushTokenRepository.syncToken(token)
        }
    }
}
