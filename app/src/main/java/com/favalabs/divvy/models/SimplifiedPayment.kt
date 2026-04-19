package com.favalabs.divvy.models

/**
 * A single minimized payment produced by the Splitwise-style debt simplification algorithm.
 *
 * [fromName] should pay [amountCents] in [currency] to [toName].
 * [fromIsCurrentUser] / [toIsCurrentUser] drive display styling.
 */
data class SimplifiedPayment(
    val fromUserId: String,
    val fromName: String,
    val toUserId: String,
    val toName: String,
    val amountCents: Long,
    val currency: String,
    val fromIsCurrentUser: Boolean = false,
    val toIsCurrentUser: Boolean = false
)