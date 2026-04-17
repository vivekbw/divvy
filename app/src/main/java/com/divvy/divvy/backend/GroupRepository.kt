package com.divvy.divvy.backend

import com.divvy.divvy.components.GroupIcon
import com.divvy.divvy.models.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

interface GroupRepository {
    fun listGroups(): Flow<DataResult<List<Group>>>
    fun getGroup(groupId: String): Flow<Group>
    suspend fun createGroup(name: String, icon: GroupIcon): Group
    suspend fun updateGroup(groupId: String, name: String, icon: GroupIcon)
    suspend fun deleteGroup(groupId: String)
    suspend fun refreshGroups()
    fun invalidateCache()
}

class StubGroupRepository @Inject constructor() : GroupRepository {

    private data class GroupData(val id: String, val name: String, val icon: GroupIcon, val createdBy: String)

    private val _groups = MutableStateFlow<List<GroupData>>(
        listOf(
            GroupData("g1", "Roommates", GroupIcon.Home, "u_me"),
            GroupData("g2", "Weekend Trip", GroupIcon.Flight, "u_me"),
            GroupData("g3", "Work Lunch", GroupIcon.Restaurant, "u_me")
        )
    )

    override fun listGroups(): Flow<DataResult<List<Group>>> =
        _groups.map { groups ->
            DataResult.Success(groups.map { g ->
                Group(id = g.id, name = g.name, icon = g.icon, createdBy = g.createdBy)
            })
        }

    override fun getGroup(groupId: String): Flow<Group> =
        _groups.map { groups ->
            val g = groups.find { it.id == groupId }
                ?: return@map Group(id = groupId, name = "Group")
            Group(id = g.id, name = g.name, icon = g.icon, createdBy = g.createdBy)
        }

    override suspend fun createGroup(name: String, icon: GroupIcon): Group {
        val id = UUID.randomUUID().toString()
        _groups.update { it + GroupData(id, name, icon, "u_me") }
        return Group(id = id, name = name, icon = icon, createdBy = "u_me")
    }

    override suspend fun updateGroup(groupId: String, name: String, icon: GroupIcon) {
        _groups.update { list ->
            list.map { if (it.id == groupId) it.copy(name = name, icon = icon) else it }
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        _groups.update { it.filter { g -> g.id != groupId } }
    }

    override suspend fun refreshGroups() { /* no-op for stub */ }
    override fun invalidateCache() { /* no-op for stub */ }
}
