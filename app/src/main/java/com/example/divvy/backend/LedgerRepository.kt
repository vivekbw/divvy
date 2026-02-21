package com.example.divvy.backend

import com.example.divvy.models.LedgerEntry
import com.example.divvy.models.LedgerEntryType
import javax.inject.Inject

interface LedgerRepository {
    suspend fun listEntries(): List<LedgerEntry>
    suspend fun getGroupNames(): List<Pair<String, String>>
    suspend fun getMemberNames(): List<String>
}

class StubLedgerRepository @Inject constructor() : LedgerRepository {

    private val entries = listOf(
        LedgerEntry(
            id = "l1", type = LedgerEntryType.EXPENSE,
            title = "Whole Foods", amountCents = 20000L,
            groupId = "1", groupName = "Roommates",
            paidByName = "You", paidByCurrentUser = true,
            dateLabel = "Today", splitMethod = "equally"
        ),
        LedgerEntry(
            id = "l2", type = LedgerEntryType.SETTLEMENT,
            title = "Settlement", amountCents = 5000L,
            groupId = "1", groupName = "Roommates",
            paidByName = "Sarah", paidByCurrentUser = false,
            dateLabel = "Today", toName = "You"
        ),
        LedgerEntry(
            id = "l3", type = LedgerEntryType.EXPENSE,
            title = "Sushi Palace", amountCents = 6200L,
            groupId = "3", groupName = "Work Lunch",
            paidByName = "You", paidByCurrentUser = true,
            dateLabel = "Today", splitMethod = "equally"
        ),
        LedgerEntry(
            id = "l4", type = LedgerEntryType.EXPENSE,
            title = "Uber to Airport", amountCents = 4550L,
            groupId = "1", groupName = "Roommates",
            paidByName = "Sarah", paidByCurrentUser = false,
            dateLabel = "Yesterday"
        ),
        LedgerEntry(
            id = "l5", type = LedgerEntryType.EXPENSE,
            title = "Coffee Run", amountCents = 1800L,
            groupId = "3", groupName = "Work Lunch",
            paidByName = "Priya", paidByCurrentUser = false,
            dateLabel = "Yesterday"
        ),
        LedgerEntry(
            id = "l6", type = LedgerEntryType.SETTLEMENT,
            title = "Settlement", amountCents = 3400L,
            groupId = "2", groupName = "Weekend Trip",
            paidByName = "You", paidByCurrentUser = true,
            dateLabel = "Yesterday", toName = "Jordan"
        ),
        LedgerEntry(
            id = "l7", type = LedgerEntryType.EXPENSE,
            title = "Electric Bill", amountCents = 12300L,
            groupId = "1", groupName = "Roommates",
            paidByName = "You", paidByCurrentUser = true,
            dateLabel = "Mon", splitMethod = "equally"
        ),
        LedgerEntry(
            id = "l8", type = LedgerEntryType.EXPENSE,
            title = "Hotel", amountCents = 45000L,
            groupId = "2", groupName = "Weekend Trip",
            paidByName = "Jordan", paidByCurrentUser = false,
            dateLabel = "Fri", splitMethod = "percentage"
        ),
        LedgerEntry(
            id = "l9", type = LedgerEntryType.EXPENSE,
            title = "Dinner at Maple Bistro", amountCents = 8900L,
            groupId = "2", groupName = "Weekend Trip",
            paidByName = "You", paidByCurrentUser = true,
            dateLabel = "Fri", splitMethod = "equally"
        ),
        LedgerEntry(
            id = "l10", type = LedgerEntryType.SETTLEMENT,
            title = "Settlement", amountCents = 7800L,
            groupId = "1", groupName = "Roommates",
            paidByName = "Mike", paidByCurrentUser = false,
            dateLabel = "Last week", toName = "You"
        ),
        LedgerEntry(
            id = "l11", type = LedgerEntryType.EXPENSE,
            title = "Groceries", amountCents = 15400L,
            groupId = "1", groupName = "Roommates",
            paidByName = "Alex", paidByCurrentUser = false,
            dateLabel = "Last week", splitMethod = "by items"
        ),
        LedgerEntry(
            id = "l12", type = LedgerEntryType.EXPENSE,
            title = "Gas Station", amountCents = 6200L,
            groupId = "2", groupName = "Weekend Trip",
            paidByName = "Taylor", paidByCurrentUser = false,
            dateLabel = "Last week", splitMethod = "equally"
        )
    )

    override suspend fun listEntries(): List<LedgerEntry> = entries

    override suspend fun getGroupNames(): List<Pair<String, String>> = entries
        .map { it.groupId to it.groupName }
        .distinct()

    override suspend fun getMemberNames(): List<String> = entries
        .flatMap { listOf(it.paidByName, it.toName) }
        .filter { it.isNotBlank() && it != "You" }
        .distinct()
        .sorted()
}
