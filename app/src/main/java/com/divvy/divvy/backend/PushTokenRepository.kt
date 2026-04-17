package com.divvy.divvy.backend

import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushTokenRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    /**
     * Best-effort push token sync.
     *
     * This method is intentionally non-throwing so token-sync failures never crash app startup.
     * Returns true when token upsert succeeded, false otherwise.
     */
    suspend fun syncToken(tokenOverride: String? = null): Boolean {
        val token = tokenOverride ?: runCatching {
            FirebaseMessaging.getInstance().token.await()
        }.getOrElse { e ->
            Timber.w("Push token fetch failed: ${e::class.simpleName}")
            return false
        }

        val userId = authRepository.getCurrentUserIdOrNull() ?: return false

        return upsertToken(userId, token, allowSessionRefreshRetry = true)
    }

    private suspend fun upsertToken(
        userId: String,
        token: String,
        allowSessionRefreshRetry: Boolean
    ): Boolean {
        return runCatching {
            supabaseClient.postgrest["push_tokens"].upsert(
                mapOf("user_id" to userId, "token" to token)
            )
            true
        }.getOrElse { e ->
            if (allowSessionRefreshRetry && isJwtExpiredError(e) && refreshSessionSafely()) {
                return upsertToken(userId, token, allowSessionRefreshRetry = false)
            }
            Timber.w("Push token sync failed: ${e::class.simpleName}")
            false
        }
    }

    private suspend fun refreshSessionSafely(): Boolean {
        return runCatching {
            supabaseClient.auth.refreshCurrentSession()
            true
        }.getOrElse { e ->
            Timber.w("Push token session refresh failed: ${e::class.simpleName}")
            false
        }
    }

    private fun isJwtExpiredError(error: Throwable): Boolean {
        val msg = error.message.orEmpty()
        return msg.contains("jwt expired", ignoreCase = true)
    }
}
