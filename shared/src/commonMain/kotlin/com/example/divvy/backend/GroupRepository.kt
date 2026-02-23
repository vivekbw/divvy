package com.example.divvy.backend

import com.example.divvy.components.GroupIcon
import com.example.divvy.models.ActivityItem
import com.example.divvy.models.ExpenseSplit
import com.example.divvy.models.Group
import com.example.divvy.models.GroupExpense
import com.example.divvy.models.GroupMember
import com.example.divvy.models.MemberBalance
import com.example.divvy.randomUuidString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

const val CURRENT_USER_ID = "u_me"

private const val ME = CURRENT_USER_ID

interface GroupRepository {
    fun listGroups(): Flow<List<Group>>
    fun getGroup(groupId: String): Flow<Group>
    suspend fun createGroup(name: String, icon: GroupIcon): Group
    fun getMembers(groupId: String): Flow<List<GroupMember>>
    suspend fun addMember(groupId: String, member: GroupMember)
    suspend fun addExpense(expense: GroupExpense)
    suspend fun leaveGroup(groupId: String)
    fun getAllExpenses(): Flow<List<GroupExpense>>
    fun getMemberBalances(groupId: String): Flow<List<MemberBalance>>
    fun getActivity(groupId: String): Flow<List<ActivityItem>>
}

class StubGroupRepository : GroupRepository {

    private data class GroupData(val id: String, val name: String, val icon: GroupIcon)

    private val _groupsState = MutableStateFlow<List<GroupData>>(emptyList())
    private val _membersState = MutableStateFlow<Map<String, List<GroupMember>>>(emptyMap())
    private val _expensesState = MutableStateFlow<Map<String, List<GroupExpense>>>(emptyMap())

    init {
        val groups = mutableListOf<GroupData>()
        val members = mutableMapOf<String, List<GroupMember>>()
        val expenses = mutableMapOf<String, List<GroupExpense>>()

        groups += GroupData("g1", "Roommates", GroupIcon.Home)
        members["g1"] = listOf(
            GroupMember("u_sarah", "Sarah"),
            GroupMember("u_mike", "Mike"),
            GroupMember("u_alex", "Alex")
        )
        expenses["g1"] = listOf(
            GroupExpense("e1", "g1", "Whole Foods", 20000, ME,
                listOf(ExpenseSplit(ME, 5000), ExpenseSplit("u_sarah", 5000), ExpenseSplit("u_mike", 5000), ExpenseSplit("u_alex", 5000)),
                "2026-02-20"),
            GroupExpense("e2", "g1", "Electric Bill", 12300, ME,
                listOf(ExpenseSplit(ME, 4100), ExpenseSplit("u_sarah", 4100), ExpenseSplit("u_mike", 4100)),
                "2026-02-16"),
            GroupExpense("e3", "g1", "Uber to Airport", 4550, "u_sarah",
                listOf(ExpenseSplit(ME, 2275), ExpenseSplit("u_sarah", 2275)),
                "2026-02-19"),
            GroupExpense("s1", "g1", "Settlement", 5000, "u_sarah",
                listOf(ExpenseSplit(ME, 5000)), "2026-02-20"),
            GroupExpense("s2", "g1", "Settlement", 7800, "u_mike",
                listOf(ExpenseSplit(ME, 7800)), "2026-02-17")
        )

        groups += GroupData("g2", "Weekend Trip", GroupIcon.Flight)
        members["g2"] = listOf(
            GroupMember("u_jordan", "Jordan"), GroupMember("u_taylor", "Taylor"),
            GroupMember("u_casey", "Casey"), GroupMember("u_riley", "Riley")
        )
        expenses["g2"] = listOf(
            GroupExpense("e4", "g2", "Hotel", 45000, "u_jordan",
                listOf(ExpenseSplit(ME, 9000), ExpenseSplit("u_jordan", 9000), ExpenseSplit("u_taylor", 9000), ExpenseSplit("u_casey", 9000), ExpenseSplit("u_riley", 9000)),
                "2026-02-13"),
            GroupExpense("e5", "g2", "Dinner", 8900, ME,
                listOf(ExpenseSplit(ME, 1780), ExpenseSplit("u_jordan", 1780), ExpenseSplit("u_taylor", 1780), ExpenseSplit("u_casey", 1780), ExpenseSplit("u_riley", 1780)),
                "2026-02-13"),
            GroupExpense("s3", "g2", "Settlement", 3400, ME,
                listOf(ExpenseSplit("u_jordan", 3400)), "2026-02-19")
        )

        groups += GroupData("g3", "Work Lunch", GroupIcon.Restaurant)
        members["g3"] = listOf(GroupMember("u_priya", "Priya"), GroupMember("u_devon", "Devon"))
        expenses["g3"] = listOf(
            GroupExpense("e6", "g3", "Sushi Palace", 6200, ME,
                listOf(ExpenseSplit(ME, 2067), ExpenseSplit("u_priya", 2067), ExpenseSplit("u_devon", 2066)),
                "2026-02-20"),
            GroupExpense("e7", "g3", "Coffee Run", 1800, "u_priya",
                listOf(ExpenseSplit(ME, 600), ExpenseSplit("u_priya", 600), ExpenseSplit("u_devon", 600)),
                "2026-02-19")
        )

        _groupsState.value = groups
        _membersState.value = members
        _expensesState.value = expenses
    }

    override fun listGroups(): Flow<List<Group>> =
        combine(_groupsState, _membersState, _expensesState) { groups, members, expenses ->
            groups.map { g ->
                Group(id = g.id, name = g.name, icon = g.icon,
                    memberCount = members[g.id]?.size ?: 0,
                    balanceCents = computeGroupBalance(g.id, members, expenses))
            }
        }

    override fun getGroup(groupId: String): Flow<Group> =
        combine(_groupsState, _membersState, _expensesState) { groups, members, expenses ->
            val g = groups.find { it.id == groupId }
                ?: return@combine Group(id = groupId, name = "Group")
            Group(id = g.id, name = g.name, icon = g.icon,
                memberCount = members[groupId]?.size ?: 0,
                balanceCents = computeGroupBalance(groupId, members, expenses))
        }

    override suspend fun createGroup(name: String, icon: GroupIcon): Group {
        val id = randomUuidString()
        _groupsState.update { it + GroupData(id, name, icon) }
        _membersState.update { it + (id to emptyList()) }
        _expensesState.update { it + (id to emptyList()) }
        return Group(id = id, name = name, icon = icon, memberCount = 0, balanceCents = 0L)
    }

    override fun getMembers(groupId: String): Flow<List<GroupMember>> =
        _membersState.map { it[groupId] ?: emptyList() }

    override suspend fun addMember(groupId: String, member: GroupMember) {
        _membersState.update { map -> map + (groupId to ((map[groupId] ?: emptyList()) + member)) }
    }

    override suspend fun addExpense(expense: GroupExpense) {
        _expensesState.update { map -> map + (expense.groupId to ((map[expense.groupId] ?: emptyList()) + expense)) }
    }

    override suspend fun leaveGroup(groupId: String) {
        _groupsState.update { it.filter { g -> g.id != groupId } }
        _membersState.update { it - groupId }
        _expensesState.update { it - groupId }
    }

    override fun getAllExpenses(): Flow<List<GroupExpense>> =
        _expensesState.map { it.values.flatten() }

    override fun getMemberBalances(groupId: String): Flow<List<MemberBalance>> =
        combine(_membersState, _expensesState) { members, expenses ->
            val memberList = members[groupId] ?: return@combine emptyList()
            val expenseList = expenses[groupId] ?: return@combine emptyList()
            memberList.map { member ->
                var balance = 0L
                for (expense in expenseList) {
                    balance += when {
                        expense.paidByUserId == ME -> expense.splits.find { it.userId == member.userId }?.amountCents ?: 0L
                        expense.paidByUserId == member.userId -> -(expense.splits.find { it.userId == ME }?.amountCents ?: 0L)
                        else -> 0L
                    }
                }
                MemberBalance(userId = member.userId, name = member.name, balanceCents = balance)
            }
        }

    override fun getActivity(groupId: String): Flow<List<ActivityItem>> =
        combine(_membersState, _expensesState) { members, expenses ->
            val expenseList = expenses[groupId] ?: return@combine emptyList()
            val memberMap = (members[groupId] ?: emptyList()).associateBy { it.userId }
            expenseList.map { expense ->
                val paidByCurrentUser = expense.paidByUserId == ME
                val paidByLabel = if (paidByCurrentUser) "You" else memberMap[expense.paidByUserId]?.name ?: "Unknown"
                ActivityItem(id = expense.id, title = expense.title, amountCents = expense.amountCents,
                    dateLabel = dateLabel(expense.createdAt), paidByLabel = paidByLabel,
                    paidByCurrentUser = paidByCurrentUser, timestamp = expense.createdAt)
            }
        }

    private fun computeGroupBalance(groupId: String, members: Map<String, List<GroupMember>>, expenses: Map<String, List<GroupExpense>>): Long {
        val memberList = members[groupId] ?: return 0L
        val expenseList = expenses[groupId] ?: return 0L
        var balance = 0L
        for (member in memberList) {
            for (expense in expenseList) {
                balance += when {
                    expense.paidByUserId == ME -> expense.splits.find { it.userId == member.userId }?.amountCents ?: 0L
                    expense.paidByUserId == member.userId -> -(expense.splits.find { it.userId == ME }?.amountCents ?: 0L)
                    else -> 0L
                }
            }
        }
        return balance
    }
}

fun dateLabel(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = LocalDate(today.year, today.month, today.dayOfMonth).let {
            val epochDays = it.toEpochDays() - 1
            LocalDate.fromEpochDays(epochDays)
        }
        when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        }
    } catch (_: Exception) {
        isoDate
    }
}
