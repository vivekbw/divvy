package com.favalabs.divvy.backend

import com.favalabs.divvy.models.GroupMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface MemberRepository {
    fun getMembers(groupId: String): Flow<List<GroupMember>>
    suspend fun addMember(groupId: String, userId: String)
    suspend fun joinGroup(groupId: String)
    suspend fun leaveGroup(groupId: String)
    suspend fun refreshMembers(groupId: String)
    fun clearCache(groupId: String)
}

@Serializable
private data class GroupMemberRow(
    @SerialName("group_id")  val groupId: String = "",
    @SerialName("user_id")   val userId: String = "",
    @SerialName("joined_at") val joinedAt: String = ""
)

@Serializable
private data class ProfileNameRow(
    val id: String = "",
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name")  val lastName: String? = null
)

@Singleton
class SupabaseMemberRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : MemberRepository {

    companion object {
        private const val CACHE_TTL_MS = 30_000L
    }

    private val _members = MutableStateFlow<Map<String, List<GroupMember>>>(emptyMap())
    private val _lastRefreshMs = ConcurrentHashMap<String, Long>()

    override fun getMembers(groupId: String): Flow<List<GroupMember>> =
        _members.map { it[groupId] ?: emptyList() }

    override suspend fun addMember(groupId: String, userId: String) {
        val params = buildJsonObject {
            put("p_group_id", groupId)
            put("p_user_id", userId)
        }
        supabaseClient.postgrest.rpc("add_group_member", params)
        _lastRefreshMs.remove(groupId)
        refreshMembers(groupId)
    }

    override suspend fun joinGroup(groupId: String) {
        val params = buildJsonObject { put("p_group_id", groupId) }
        supabaseClient.postgrest.rpc("join_group", params)
        _lastRefreshMs.remove(groupId)
        refreshMembers(groupId)
    }

    override suspend fun leaveGroup(groupId: String) {
        supabaseClient.from("group_members").delete {
            filter {
                eq("group_id", groupId)
                eq("user_id", authRepository.getCurrentUserId())
            }
        }
        _members.update { it - groupId }
        _lastRefreshMs.remove(groupId)
    }

    override suspend fun refreshMembers(groupId: String) {
        val now = System.currentTimeMillis()
        val lastRefresh = _lastRefreshMs[groupId] ?: 0L
        if (now - lastRefresh < CACHE_TTL_MS && _members.value.containsKey(groupId)) return

        val members = fetchMembers(groupId)
        _members.update { it + (groupId to members) }
        _lastRefreshMs[groupId] = now
    }

    override fun clearCache(groupId: String) {
        _members.update { it - groupId }
        _lastRefreshMs.remove(groupId)
    }

    private suspend fun fetchMembers(groupId: String): List<GroupMember> {
        val memberRows = supabaseClient.from("group_members")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<GroupMemberRow>()

        if (memberRows.isEmpty()) return emptyList()

        val userIds = memberRows.map { it.userId }
        val profiles = supabaseClient.from("profiles")
            .select { filter { isIn("id", userIds) } }
            .decodeList<ProfileNameRow>()
            .associateBy { it.id }

        return memberRows.map { row ->
            val profile = profiles[row.userId]
            val name = if (profile != null) {
                "${profile.firstName.orEmpty()} ${profile.lastName.orEmpty()}".trim()
            } else {
                row.userId
            }
            GroupMember(userId = row.userId, name = name)
        }
    }
}
