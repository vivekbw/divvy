package com.favalabs.divvy.offline

import kotlinx.serialization.Serializable

object PendingOperationPayloads {

    @Serializable
    data class CreateExpenseWithSplitsPayload(
        val tempLocalId: String,
        val groupId: String,
        val description: String,
        val amountCents: Long,
        val currency: String,
        val splitMethod: String,
        val paidByUserId: String,
        val splits: List<SplitPayload>
    )

    @Serializable
    data class SplitPayload(
        val userId: String,
        val amountCents: Long,
        val isCoveredBy: String? = null
    )

    @Serializable
    data class DeleteExpensePayload(
        val expenseId: String
    )
}
