package com.example.divvy.backend

import com.example.divvy.models.MemberBalance
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    @SerialName("balance_cents") val balanceCents: Long = 0L
)

@Singleton
class SupabaseBalanceRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : BalanceRepository {

    private val _balances = MutableStateFlow<Map<String, List<MemberBalance>>>(emptyMap())

    override fun observeBalances(groupId: String): Flow<List<MemberBalance>> =
        _balances.map { it[groupId] ?: emptyList() }

    override suspend fun refreshBalances(groupId: String) {
        val params = buildJsonObject { put("p_group_id", groupId) }
        val balanceRows = try {
            supabaseClient.postgrest
                .rpc("net_balances_v3", params)
                .decodeList<NetBalanceRow>()
        } catch (_: Exception) {
            emptyList()
        }
        val balances = balanceRows.map { row ->
            MemberBalance(
                userId = row.userId,
                name = "",
                balanceCents = row.balanceCents
            )
        }
        _balances.update { it + (groupId to balances) }
    }

    override fun clearCache(groupId: String) {
        _balances.update { it - groupId }
    }
}
