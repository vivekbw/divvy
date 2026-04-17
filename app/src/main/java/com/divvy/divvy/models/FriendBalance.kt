package com.divvy.divvy.models

data class GroupBalance(
    val groupId: String,
    val groupName: String,
    val groupIcon: String?,
    val balanceCents: Long,
    val currency: String
)

data class FriendGroupBalances(
    val groupId: String,
    val groupName: String,
    val groupIcon: String?,
    val balances: List<GroupBalance>
)

data class FriendBalance(
    val userId: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val groupBalances: List<GroupBalance>
) {
    val displayName: String
        get() = "${firstName.orEmpty()} ${lastName.orEmpty()}".trim().ifEmpty { "?" }

    val selectionKey: String
        get() = "divvy_$userId"

    /** Net balances aggregated by currency across all groups */
    val netBalancesByCurrency: Map<String, Long>
        get() = groupBalances.groupBy { it.currency }
            .mapValues { (_, balances) -> balances.sumOf { it.balanceCents } }

    /** True if every currency sums to zero */
    val isSettledUp: Boolean
        get() = netBalancesByCurrency.values.all { it == 0L }

    /** Non-zero group balances for subtitle display */
    val nonZeroGroupBalances: List<GroupBalance>
        get() = groupBalances.filter { it.balanceCents != 0L }

    val initials: String
        get() = (firstName?.take(1).orEmpty() + lastName?.take(1).orEmpty()).ifBlank { "?" }
}
