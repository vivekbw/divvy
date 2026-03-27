package com.example.divvy.backend

import com.example.divvy.components.GroupIcon
import com.example.divvy.models.Group
import io.github.jan.supabase.SupabaseClient
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class GroupSummaryRow(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    @SerialName("created_by")   val createdBy: String = "",
    @SerialName("created_at")   val createdAt: String = "",
    @SerialName("member_count") val memberCount: Long = 0L,
    @SerialName("balance_cents") val balanceCents: Long = 0L
)

@Serializable
private data class GroupRow(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
private data class GroupNetBalanceRow(
    @SerialName("user_id") val userId: String = "",
    @SerialName("balance_cents") val balanceCents: Long = 0L
)

@Singleton
class SupabaseGroupRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : GroupRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _groups = MutableStateFlow<DataResult<List<Group>>>(DataResult.Loading)

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
            balanceCents = 0L,
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
        val params = buildJsonObject { put("p_group_id", groupId) }
        supabaseClient.postgrest.rpc("delete_group_cascade", params)
        _groups.update { result ->
            val current = (result as? DataResult.Success)?.data ?: return@update result
            DataResult.Success(current.filter { g -> g.id != groupId })
        }
    }

    override suspend fun refreshGroups() {
        try {
            val rows = supabaseClient.postgrest
                .rpc("get_my_groups_summary")
                .decodeList<GroupSummaryRow>()

            val groups = rows.map { row ->
                val freshBalanceCents = try {
                    balanceFromNetBalances(row.id)
                } catch (_: Exception) {
                    row.balanceCents
                }
                Group(
                    id = row.id,
                    name = row.name,
                    icon = iconFromName(row.icon),
                    memberCount = row.memberCount.toInt(),
                    balanceCents = freshBalanceCents,
                    createdBy = row.createdBy
                )
            }
            _groups.value = DataResult.Success(groups)
        } catch (e: Exception) {
            _groups.value = DataResult.Error("Failed to load groups", e)
        }
    }

    private suspend fun balanceFromNetBalances(groupId: String): Long {
        val params = buildJsonObject { put("p_group_id", groupId) }
        val rows = supabaseClient.postgrest
            .rpc("net_balances", params)
            .decodeList<GroupNetBalanceRow>()
        return rows.sumOf { it.balanceCents }
    }

    private fun iconFromName(name: String): GroupIcon =
        GroupIcon.entries.find { it.name == name } ?: GroupIcon.Group
}
