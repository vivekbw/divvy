package com.divvy.divvy.backend

import com.divvy.divvy.models.ParsedTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionHolder @Inject constructor() {
    var transactions: List<ParsedTransaction> = emptyList()
}
