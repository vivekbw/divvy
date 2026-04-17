package com.divvy.divvy.offline.repository

import com.divvy.divvy.backend.MemberRepository
import com.divvy.divvy.backend.SupabaseMemberRepository
import com.divvy.divvy.models.GroupMember
import com.divvy.divvy.offline.NetworkMonitor
import com.divvy.divvy.offline.db.dao.CachedMemberDao
import com.divvy.divvy.offline.db.entity.CachedMemberEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineMemberRepository @Inject constructor(
    private val remote: SupabaseMemberRepository,
    private val memberDao: CachedMemberDao,
    private val networkMonitor: NetworkMonitor
) : MemberRepository {

    override fun getMembers(groupId: String): Flow<List<GroupMember>> =
        memberDao.getByGroupId(groupId).map { entities ->
            entities.map { GroupMember(userId = it.userId, name = it.name) }
        }

    override suspend fun addMember(groupId: String, userId: String) {
        remote.addMember(groupId, userId)
        refreshMembers(groupId)
    }

    override suspend fun joinGroup(groupId: String) {
        remote.joinGroup(groupId)
        refreshMembers(groupId)
    }

    override suspend fun leaveGroup(groupId: String) {
        remote.leaveGroup(groupId)
        memberDao.deleteByGroupId(groupId)
    }

    override suspend fun refreshMembers(groupId: String) {
        if (!networkMonitor.isOnline.value) return
        try {
            remote.refreshMembers(groupId)
            val members = remote.getMembers(groupId).first()
            memberDao.deleteByGroupId(groupId)
            memberDao.insertAll(members.map { CachedMemberEntity(groupId, it.userId, it.name) })
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh members for group $groupId")
        }
    }

    override fun clearCache(groupId: String) {
        remote.clearCache(groupId)
    }
}
