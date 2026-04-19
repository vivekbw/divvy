package com.favalabs.divvy.offline.repository

import com.favalabs.divvy.backend.DataResult
import com.favalabs.divvy.backend.GroupRepository
import com.favalabs.divvy.backend.SupabaseGroupRepository
import com.favalabs.divvy.components.GroupIcon
import com.favalabs.divvy.models.CurrencyBalance
import com.favalabs.divvy.models.Group
import com.favalabs.divvy.offline.NetworkMonitor
import com.favalabs.divvy.offline.db.dao.CachedGroupDao
import com.favalabs.divvy.offline.db.entity.CachedGroupEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineGroupRepository @Inject constructor(
    private val remote: SupabaseGroupRepository,
    private val groupDao: CachedGroupDao,
    private val networkMonitor: NetworkMonitor
) : GroupRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    init {
        scope.launch { refreshGroups() }
    }

    override fun listGroups(): Flow<DataResult<List<Group>>> =
        groupDao.getAll().map { entities ->
            if (entities.isEmpty()) DataResult.Loading
            else DataResult.Success(entities.map { it.toGroup() })
        }

    override fun getGroup(groupId: String): Flow<Group> =
        groupDao.getById(groupId).map { entity ->
            entity?.toGroup() ?: Group(id = groupId, name = "Group")
        }

    override suspend fun createGroup(name: String, icon: GroupIcon): Group {
        val group = remote.createGroup(name, icon)
        groupDao.insertAll(listOf(group.toEntity()))
        return group
    }

    override suspend fun updateGroup(groupId: String, name: String, icon: GroupIcon) {
        remote.updateGroup(groupId, name, icon)
        groupDao.updateNameAndIcon(groupId, name, icon.name)
        invalidateCache()
        refreshGroups()
    }

    override suspend fun deleteGroup(groupId: String) {
        remote.deleteGroup(groupId)
        groupDao.deleteById(groupId)
        refreshGroups()
    }

    override fun invalidateCache() {
        remote.invalidateCache()
    }

    override suspend fun refreshGroups() {
        if (!networkMonitor.isOnline.value) return
        try {
            remote.refreshGroups()
            val dataResult = remote.listGroups().first()
            if (dataResult is DataResult.Success) {
                val entities = dataResult.data.map { it.toEntity() }
                groupDao.deleteAll()
                groupDao.insertAll(entities)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh groups from remote")
        }
    }

    private fun CachedGroupEntity.toGroup(): Group = Group(
        id = id,
        name = name,
        icon = GroupIcon.entries.find { it.name == icon } ?: GroupIcon.Group,
        memberCount = memberCount,
        balances = try {
            json.decodeFromString<List<CurrencyBalance>>(balancesJson)
        } catch (_: Exception) {
            emptyList()
        },
        currency = currency,
        createdBy = createdBy
    )

    private fun Group.toEntity(): CachedGroupEntity = CachedGroupEntity(
        id = id,
        name = name,
        icon = icon.name,
        createdBy = createdBy,
        memberCount = memberCount,
        currency = currency,
        balancesJson = json.encodeToString(balances)
    )
}
