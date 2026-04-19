package com.favalabs.divvy.backend

import com.favalabs.divvy.models.ParsedTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionHolder @Inject constructor() {
    var transactions: List<ParsedTransaction> = emptyList()
}
