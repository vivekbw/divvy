package com.example.divvy.backend

import com.example.divvy.components.GroupIcon
import com.example.divvy.models.ActivityItem
import com.example.divvy.models.ExpenseSplit
import com.example.divvy.models.Group
import com.example.divvy.models.GroupExpense
import com.example.divvy.models.GroupMember
import com.example.divvy.models.MemberBalance
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Stable ID for the current user in all stub data. */
const val CURRENT_USER_ID = "u_me"

private const val ME = CURRENT_USER_ID

interface GroupRepository {
    // --- Groups ---
    fun listGroups(): Flow<List<Group>>
    fun getGroup(groupId: String): Flow<Group>
    suspend fun createGroup(name: String, icon: GroupIcon): Group

    // --- Members ---
    fun getMembers(groupId: String): Flow<List<GroupMember>>
    suspend fun addMember(groupId: String, member: GroupMember)

    // --- Expenses ---
    suspend fun addExpense(expense: GroupExpense)
    suspend fun leaveGroup(groupId: String)
    fun getAllExpenses(): Flow<List<GroupExpense>>

    // --- Derived (computed from expenses + members) ---
    fun getMemberBalances(groupId: String): Flow<List<MemberBalance>>
    fun getActivity(groupId: String): Flow<List<ActivityItem>>
}

class StubGroupRepository @Inject constructor() : GroupRepository {

    private data class GroupData(val id: String, val name: String, val icon: GroupIcon)

    // Reactive state — every mutation replaces the value so all derived flows re-emit.
    private val _groupsState   = MutableStateFlow<List<GroupData>>(emptyList())
    private val _membersState  = MutableStateFlow<Map<String, List<GroupMember>>>(emptyMap())
    private val _expensesState = MutableStateFlow<Map<String, List<GroupExpense>>>(emptyMap())

    init {
        val groups   = mutableListOf<GroupData>()
        val members  = mutableMapOf<String, List<GroupMember>>()
        val expenses = mutableMapOf<String, List<GroupExpense>>()

        // --- Roommates (g1) ---
        // Members: me, Sarah, Mike, Alex
        // Whole Foods $200 (me paid, 4-way split)
        // Electric Bill $123 (me paid, 3-way: me+Sarah+Mike)
        // Uber to Airport $45.50 (Sarah paid, 2-way: me+Sarah)
        groups  += GroupData("g1", "Roommates", GroupIcon.Home)
        members["g1"]  = listOf(
            GroupMember("u_sarah", "Sarah"),
            GroupMember("u_mike",  "Mike"),
            GroupMember("u_alex",  "Alex")
        )
        expenses["g1"] = listOf(
            GroupExpense(
                id = "e1", groupId = "g1", title = "Whole Foods",
                amountCents = 20000, paidByUserId = ME,
                splits = listOf(
                    ExpenseSplit(ME,          5000),
                    ExpenseSplit("u_sarah",   5000),
                    ExpenseSplit("u_mike",    5000),
                    ExpenseSplit("u_alex",    5000)
                ),
                createdAt = "2026-02-20"
            ),
            GroupExpense(
                id = "e2", groupId = "g1", title = "Electric Bill",
                amountCents = 12300, paidByUserId = ME,
                splits = listOf(
                    ExpenseSplit(ME,          4100),
                    ExpenseSplit("u_sarah",   4100),
                    ExpenseSplit("u_mike",    4100)
                ),
                createdAt = "2026-02-16"
            ),
            GroupExpense(
                id = "e3", groupId = "g1", title = "Uber to Airport",
                amountCents = 4550, paidByUserId = "u_sarah",
                splits = listOf(
                    ExpenseSplit(ME,          2275),
                    ExpenseSplit("u_sarah",   2275)
                ),
                createdAt = "2026-02-19"
            ),
            GroupExpense(
                id = "s1", groupId = "g1", title = "Settlement",
                amountCents = 5000, paidByUserId = "u_sarah",
                splits = listOf(ExpenseSplit(ME, 5000)),
                createdAt = "2026-02-20"
            ),
            GroupExpense(
                id = "s2", groupId = "g1", title = "Settlement",
                amountCents = 7800, paidByUserId = "u_mike",
                splits = listOf(ExpenseSplit(ME, 7800)),
                createdAt = "2026-02-17"
            )
        )

        // --- Weekend Trip (g2) ---
        // Members: me, Jordan, Taylor, Casey, Riley
        // Hotel $450 (Jordan paid, 5-way split)
        // Dinner $89 (me paid, 5-way split)
        groups  += GroupData("g2", "Weekend Trip", GroupIcon.Flight)
        members["g2"]  = listOf(
            GroupMember("u_jordan", "Jordan"),
            GroupMember("u_taylor", "Taylor"),
            GroupMember("u_casey",  "Casey"),
            GroupMember("u_riley",  "Riley")
        )
        expenses["g2"] = listOf(
            GroupExpense(
                id = "e4", groupId = "g2", title = "Hotel",
                amountCents = 45000, paidByUserId = "u_jordan",
                splits = listOf(
                    ExpenseSplit(ME,           9000),
                    ExpenseSplit("u_jordan",   9000),
                    ExpenseSplit("u_taylor",   9000),
                    ExpenseSplit("u_casey",    9000),
                    ExpenseSplit("u_riley",    9000)
                ),
                createdAt = "2026-02-13"
            ),
            GroupExpense(
                id = "e5", groupId = "g2", title = "Dinner",
                amountCents = 8900, paidByUserId = ME,
                splits = listOf(
                    ExpenseSplit(ME,           1780),
                    ExpenseSplit("u_jordan",   1780),
                    ExpenseSplit("u_taylor",   1780),
                    ExpenseSplit("u_casey",    1780),
                    ExpenseSplit("u_riley",    1780)
                ),
                createdAt = "2026-02-13"
            ),
            GroupExpense(
                id = "s3", groupId = "g2", title = "Settlement",
                amountCents = 3400, paidByUserId = ME,
                splits = listOf(ExpenseSplit("u_jordan", 3400)),
                createdAt = "2026-02-19"
            )
        )

        // --- Work Lunch (g3) ---
        // Members: me, Priya, Devon
        // Sushi Palace $62 (me paid, 3-way split)
        // Coffee Run $18 (Priya paid, 3-way split)
        groups  += GroupData("g3", "Work Lunch", GroupIcon.Restaurant)
        members["g3"]  = listOf(
            GroupMember("u_priya", "Priya"),
            GroupMember("u_devon", "Devon")
        )
        expenses["g3"] = listOf(
            GroupExpense(
                id = "e6", groupId = "g3", title = "Sushi Palace",
                amountCents = 6200, paidByUserId = ME,
                splits = listOf(
                    ExpenseSplit(ME,          2067),
                    ExpenseSplit("u_priya",   2067),
                    ExpenseSplit("u_devon",   2066)
                ),
                createdAt = "2026-02-20"
            ),
            GroupExpense(
                id = "e7", groupId = "g3", title = "Coffee Run",
                amountCents = 1800, paidByUserId = "u_priya",
                splits = listOf(
                    ExpenseSplit(ME,          600),
                    ExpenseSplit("u_priya",   600),
                    ExpenseSplit("u_devon",   600)
                ),
                createdAt = "2026-02-19"
            )
        )

        _groupsState.value   = groups
        _membersState.value  = members
        _expensesState.value = expenses
    }

    // --- Groups ---

    override fun listGroups(): Flow<List<Group>> =
        combine(_groupsState, _membersState, _expensesState) { groups, members, expenses ->
            groups.map { g ->
                Group(
                    id = g.id,
                    name = g.name,
                    icon = g.icon,
                    memberCount = members[g.id]?.size ?: 0,
                    balanceCents = computeGroupBalance(g.id, members, expenses)
                )
            }
        }

    override fun getGroup(groupId: String): Flow<Group> =
        combine(_groupsState, _membersState, _expensesState) { groups, members, expenses ->
            val g = groups.find { it.id == groupId }
                ?: return@combine Group(id = groupId, name = "Group")
            Group(
                id = g.id,
                name = g.name,
                icon = g.icon,
                memberCount = members[groupId]?.size ?: 0,
                balanceCents = computeGroupBalance(groupId, members, expenses)
            )
        }

    override suspend fun createGroup(name: String, icon: GroupIcon): Group {
        val id = UUID.randomUUID().toString()
        _groupsState.update   { it + GroupData(id, name, icon) }
        _membersState.update  { it + (id to emptyList()) }
        _expensesState.update { it + (id to emptyList()) }
        return Group(id = id, name = name, icon = icon, memberCount = 0, balanceCents = 0L)
    }

    // --- Members ---

    override fun getMembers(groupId: String): Flow<List<GroupMember>> =
        _membersState.map { it[groupId] ?: emptyList() }

    override suspend fun addMember(groupId: String, member: GroupMember) {
        _membersState.update { map ->
            map + (groupId to ((map[groupId] ?: emptyList()) + member))
        }
    }

    // --- Expenses ---

    override suspend fun addExpense(expense: GroupExpense) {
        _expensesState.update { map ->
            map + (expense.groupId to ((map[expense.groupId] ?: emptyList()) + expense))
        }
    }

    override suspend fun leaveGroup(groupId: String) {
        _groupsState.update { it.filter { g -> g.id != groupId } }
        _membersState.update { it - groupId }
        _expensesState.update { it - groupId }
    }

    override fun getAllExpenses(): Flow<List<GroupExpense>> =
        _expensesState.map { it.values.flatten() }

    // --- Derived ---

    override fun getMemberBalances(groupId: String): Flow<List<MemberBalance>> =
        combine(_membersState, _expensesState) { members, expenses ->
            val memberList  = members[groupId]  ?: return@combine emptyList()
            val expenseList = expenses[groupId] ?: return@combine emptyList()
            memberList.map { member ->
                var balance = 0L
                for (expense in expenseList) {
                    balance += when {
                        expense.paidByUserId == ME ->
                            expense.splits.find { it.userId == member.userId }?.amountCents ?: 0L
                        expense.paidByUserId == member.userId ->
                            -(expense.splits.find { it.userId == ME }?.amountCents ?: 0L)
                        else -> 0L
                    }
                }
                MemberBalance(userId = member.userId, name = member.name, balanceCents = balance)
            }
        }

    override fun getActivity(groupId: String): Flow<List<ActivityItem>> =
        combine(_membersState, _expensesState) { members, expenses ->
            val expenseList = expenses[groupId] ?: return@combine emptyList()
            val memberMap   = (members[groupId] ?: emptyList()).associateBy { it.userId }
            expenseList.map { expense ->
                val paidByCurrentUser = expense.paidByUserId == ME
                val paidByLabel = if (paidByCurrentUser) "You"
                                  else memberMap[expense.paidByUserId]?.name ?: "Unknown"
                ActivityItem(
                    id = expense.id,
                    title = expense.title,
                    amountCents = expense.amountCents,
                    dateLabel = dateLabel(expense.createdAt),
                    paidByLabel = paidByLabel,
                    paidByCurrentUser = paidByCurrentUser,
                    timestamp = expense.createdAt
                )
            }
        }

    // --- Helpers ---

    private fun computeGroupBalance(
        groupId: String,
        members: Map<String, List<GroupMember>>,
        expenses: Map<String, List<GroupExpense>>
    ): Long {
        val memberList  = members[groupId]  ?: return 0L
        val expenseList = expenses[groupId] ?: return 0L
        var balance = 0L
        for (member in memberList) {
            for (expense in expenseList) {
                balance += when {
                    expense.paidByUserId == ME ->
                        expense.splits.find { it.userId == member.userId }?.amountCents ?: 0L
                    expense.paidByUserId == member.userId ->
                        -(expense.splits.find { it.userId == ME }?.amountCents ?: 0L)
                    else -> 0L
                }
            }
        }
        return balance
    }

    private fun dateLabel(isoDate: String): String {
        return try {
            val date  = LocalDate.parse(isoDate)
            val today = LocalDate.now()
            when (date) {
                today              -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            }
        } catch (e: Exception) {
            isoDate
        }
    }
}
