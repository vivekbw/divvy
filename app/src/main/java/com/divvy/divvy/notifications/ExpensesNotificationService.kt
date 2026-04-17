package com.divvy.divvy.notifications

import com.divvy.divvy.backend.AuthRepository
import com.divvy.divvy.backend.ProfilesRepository
import com.divvy.divvy.models.formatAmount
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens for new rows on the `expenses` table via Supabase Realtime (WebSocket).
 * No Firebase / FCM required.
 *
 * Rules:
 *  - Ignore expenses paid by the current user — they just created it.
 *  - Regular expenses  → "New expense" notification on [NotificationHelper.CHANNEL_EXPENSES].
 *  - Settlement rows   → "Settlement recorded" notification on [NotificationHelper.CHANNEL_SETTLEMENT].
 *
 * Lifecycle:
 *  - Call [start] from [com.divvy.divvy.MainActivity] after the user is confirmed signed in.
 *  - Call [stop]  from [com.divvy.divvy.ui.profile.ViewModels.ProfileViewModel.signOut].
 */
@Singleton
class ExpenseNotificationService @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val profilesRepository: ProfilesRepository,
    private val notificationHelper: NotificationHelper
) {
    private var serviceScope: CoroutineScope? = null
    private val notifIdCounter = AtomicInteger(2000)

    // ------------------------------------------------------------------
    // Public lifecycle API
    // ------------------------------------------------------------------

    fun start() {
        if (serviceScope != null) return                   // idempotent
        if (!notificationHelper.areNotificationsEnabled()) return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceScope = scope
        scope.launch { subscribe(scope) }
    }

    fun stop() {
        serviceScope?.cancel()
        serviceScope = null
    }

    // ------------------------------------------------------------------
    // Realtime subscription
    // ------------------------------------------------------------------

    private suspend fun subscribe(scope: CoroutineScope) {
        val myUserId = try {
            authRepository.getCurrentUserId()
        } catch (e: Exception) {
            Sentry.captureException(e)
            return
        }

        try {
            val channel = supabaseClient.channel("expense-push-notifications")

            channel
                .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "expenses"
                }
                .onEach { action ->
                    handleInsert(action, myUserId)
                }
                .catch { e ->
                    Sentry.addBreadcrumb("ExpenseNotificationService: realtime flow error — $e")
                }
                .launchIn(scope)

            channel.subscribe()
        } catch (e: Exception) {
            Sentry.captureException(e)
        }
    }

    // ------------------------------------------------------------------
    // Notification dispatch
    // ------------------------------------------------------------------

    private suspend fun handleInsert(action: PostgresAction.Insert, myUserId: String) {
        val record = action.record

        val paidBy      = record["paid_by_user_id"]?.jsonPrimitive?.content ?: return
        val merchant    = record["merchant"]?.jsonPrimitive?.content ?: "an expense"
        val amountCents = record["amount_cents"]?.jsonPrimitive?.longOrNull ?: 0L
        val currency    = record["currency"]?.jsonPrimitive?.content ?: "USD"
        val splitMethod = record["split_method"]?.jsonPrimitive?.content ?: ""

        // Never notify the person who just paid — they created the expense.
        if (paidBy == myUserId) return

        val payerName = resolveDisplayName(paidBy)
        val formatted = formatAmount(amountCents, currency)
        val id = notifIdCounter.getAndIncrement()

        if (splitMethod == "SETTLEMENT") {
            notificationHelper.postSettlementNotification(
                notificationId = id,
                title = "Settlement recorded",
                body  = "$payerName recorded a $formatted settlement."
            )
        } else {
            notificationHelper.postExpenseNotification(
                notificationId = id,
                title = "New expense: $merchant",
                body  = "$payerName added \"$merchant\" for $formatted — check your share."
            )
        }
    }

    private suspend fun resolveDisplayName(userId: String): String {
        return try {
            profilesRepository.getProfile(userId)?.let { p ->
                "${p.firstName.orEmpty()} ${p.lastName.orEmpty()}".trim().ifBlank { null }
            } ?: "Someone"
        } catch (e: Exception) {
            Sentry.addBreadcrumb("ExpenseNotificationService: could not resolve name for $userId")
            "Someone"
        }
    }
}