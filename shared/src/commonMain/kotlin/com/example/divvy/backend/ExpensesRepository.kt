package com.example.divvy.backend

import com.example.divvy.models.Expense
import com.example.divvy.randomUuidString
import kotlinx.datetime.Clock

interface ExpensesRepository {
    suspend fun listExpenses(): List<Expense>
    suspend fun createExpense(groupId: String, description: String, amountCents: Long, splitMethod: String): Expense
}

class StubExpensesRepository : ExpensesRepository {
    private val expenses = mutableListOf<Expense>()

    override suspend fun listExpenses(): List<Expense> = expenses.toList()

    override suspend fun createExpense(groupId: String, description: String, amountCents: Long, splitMethod: String): Expense {
        val expense = Expense(
            id = randomUuidString(),
            groupId = groupId,
            merchant = description,
            amountCents = amountCents,
            splitMethod = splitMethod,
            currency = "USD",
            createdAt = Clock.System.now().epochSeconds.toString()
        )
        expenses.add(expense)
        return expense
    }
}
