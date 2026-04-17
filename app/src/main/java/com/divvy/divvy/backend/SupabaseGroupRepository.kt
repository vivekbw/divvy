package com.divvy.divvy.backend

import com.divvy.divvy.components.GroupIcon
import com.divvy.divvy.models.Group
import io.github.jan.supabase.SupabaseClient
import io.sentry.Sentry
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.divvy.divvy.models.CurrencyBalance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class CurrencyBalanceRow(
    val currency: String? = "USD",
    @SerialName("balance_cents") val balanceCents: Long = 0L
)

@Serializable
private data class GroupSummaryRow(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    @SerialName("created_by")   val createdBy: String = "",
    @SerialName("created_at")   val createdAt: String = "",
    @SerialName("member_count") val memberCount: Long = 0L,
    @SerialName("balance_cents") val balanceCents: Long = 0L,
    val balances: List<CurrencyBalanceRow> = emptyList()
)

@Serializable
private data class GroupRow(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Singleton
class SupabaseGroupRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : GroupRepository {

    companion object {
        private const val CACHE_TTL_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _groups = MutableStateFlow<DataResult<List<Group>>>(DataResult.Loading)
    @Volatile private var _lastRefreshMs = 0L

    init {
        scope.launch { refreshGroups() }
    }

    override fun listGroups(): Flow<DataResult<List<Group>>> = _groups

    override fun getGroup(groupId: String): Flow<Group> =
        _groups.map { result ->
            when (result) {
                is DataResult.Success -> result.data.find { it.id == groupId }
                    ?: Group(id = groupId, name = "Group")
                else -> Group(id = groupId, name = "Group")
            }
        }

    override suspend fun createGroup(name: String, icon: GroupIcon): Group {
        _lastRefreshMs = 0L
        val params = buildJsonObject {
            put("p_name", name)
            put("p_icon", icon.name)
        }
        val row = supabaseClient.postgrest
            .rpc("create_group_with_owner", params)
            .decodeSingle<GroupRow>()

        val group = Group(
            id = row.id,
            name = row.name,
            icon = iconFromName(row.icon),
            memberCount = 1,
            balances = emptyList(),
            createdBy = row.createdBy
        )
        _groups.update { result ->
            val current = (result as? DataResult.Success)?.data ?: emptyList()
            DataResult.Success(current + group)
        }
        return group
    }

    override suspend fun updateGroup(groupId: String, name: String, icon: GroupIcon) {
        supabaseClient.from("groups").update({
            set("name", name)
            set("icon", icon.name)
        }) {
            filter { eq("id", groupId) }
        }
        _groups.update { result ->
            val current = (result as? DataResult.Success)?.data ?: return@update result
            DataResult.Success(current.map { g ->
                if (g.id == groupId) g.copy(name = name, icon = icon) else g
            })
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        _lastRefreshMs = 0L
        val params = buildJsonObject { put("p_group_id", groupId) }
        supabaseClient.postgrest.rpc("delete_group_cascade", params)
        _groups.update { result ->
            val current = (result as? DataResult.Success)?.data ?: return@update result
            DataResult.Success(current.filter { g -> g.id != groupId })
        }
    }

    override suspend fun refreshGroups() {
        val now = System.currentTimeMillis()
        if (now - _lastRefreshMs < CACHE_TTL_MS && _groups.value is DataResult.Success) return

        try {
            val rows = try {
                supabaseClient.postgrest
                    .rpc("get_my_groups_summary_v2")
                    .decodeList<GroupSummaryRow>()
            } catch (e: Exception) {
                // Fallback to original function if _v2 is not available
                Sentry.addBreadcrumb("get_my_groups_summary_v2 unavailable, falling back")
                supabaseClient.postgrest
                    .rpc("get_my_groups_summary")
                    .decodeList<GroupSummaryRow>()
            }

            _groups.value = DataResult.Success(rows.map { row ->
                val currencyBalances = if (row.balances.isNotEmpty()) {
                    row.balances.map { CurrencyBalance(it.currency ?: "USD", it.balanceCents) }
                } else {
                    listOf(CurrencyBalance("USD", row.balanceCents))
                }
                Group(
                    id = row.id,
                    name = row.name,
                    icon = iconFromName(row.icon),
                    memberCount = row.memberCount.toInt(),
                    balances = currencyBalances,
                    createdBy = row.createdBy
                )
            })
            _lastRefreshMs = now
        } catch (e: Exception) {
            Sentry.captureException(e)
            _groups.value = DataResult.Error("Failed to load groups", e)
        }
    }

    override fun invalidateCache() {
        _lastRefreshMs = 0L
    }

    private fun iconFromName(name: String): GroupIcon =
        GroupIcon.entries.find { it.name == name } ?: GroupIcon.Group
}
