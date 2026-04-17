package com.divvy.divvy.backend

import com.divvy.divvy.models.MemberBalance
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.sentry.Sentry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface BalanceRepository {
    fun observeBalances(groupId: String): Flow<List<MemberBalance>>
    suspend fun refreshBalances(groupId: String)
    fun clearCache(groupId: String)
}

@Serializable
private data class NetBalanceRow(
    @SerialName("user_id") val userId: String = "",
    @SerialName("balance_cents") val balanceCents: Long = 0L,
    val currency: String = "USD"
)

@Singleton
class SupabaseBalanceRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : BalanceRepository {

    companion object {
        private const val CACHE_TTL_MS = 30_000L
    }

    private val _balances = MutableStateFlow<Map<String, List<MemberBalance>>>(emptyMap())
    private val _lastRefreshMs = ConcurrentHashMap<String, Long>()

    override fun observeBalances(groupId: String): Flow<List<MemberBalance>> =
        _balances.map { it[groupId] ?: emptyList() }

    override suspend fun refreshBalances(groupId: String) {
        val now = System.currentTimeMillis()
        val lastRefresh = _lastRefreshMs[groupId] ?: 0L
        if (now - lastRefresh < CACHE_TTL_MS && _balances.value.containsKey(groupId)) return

        val params = buildJsonObject { put("p_group_id", groupId) }
        val balanceRows = try {
            supabaseClient.postgrest
                .rpc("net_balances_v2", params)
                .decodeList<NetBalanceRow>()
        } catch (e: Exception) {
            // v2 RPC may not exist yet on all envs — try fallback, but still record the miss
            Sentry.addBreadcrumb("net_balances_v2 failed for group $groupId, falling back")
            try {
                supabaseClient.postgrest
                    .rpc("net_balances", params)
                    .decodeList<NetBalanceRow>()
            } catch (e2: Exception) {
                Sentry.captureException(e2)
                emptyList()
            }
        }
        val balances = balanceRows.map { row ->
            MemberBalance(
                userId = row.userId,
                name = "",
                balanceCents = row.balanceCents,
                currency = row.currency
            )
        }
        _balances.update { it + (groupId to balances) }
        _lastRefreshMs[groupId] = now
    }

    override fun clearCache(groupId: String) {
        _balances.update { it - groupId }
        _lastRefreshMs.remove(groupId)
    }
}
