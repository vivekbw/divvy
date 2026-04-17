package com.divvy.divvy.ui.receiptreview.ViewModels

import androidx.lifecycle.ViewModel
import com.divvy.divvy.backend.ScannedReceiptStore
import com.divvy.divvy.models.ParsedReceipt
import com.divvy.divvy.models.ParsedReceiptItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

data class ReviewUiState(
    val merchant: String = "",
    val items: List<EditableReceiptItem> = emptyList(),
    val taxCents: Long = 0L,
    val tipCents: Long = 0L,
    val discountCents: Long = 0L,
    val editingItemId: String? = null,
    val geminiTotalCents: Long = 0L,
    val userHasEdited: Boolean = false,
) {
    val itemsTotalCents: Long get() = items.sumOf { it.priceCents }
    val totalCents: Long
        get() = if (!userHasEdited && geminiTotalCents > 0) geminiTotalCents
                else maxOf(0L, itemsTotalCents + taxCents + tipCents - discountCents)

    val formattedItemsTotal: String
        get() = "$${String.format("%.2f", itemsTotalCents / 100.0)}"
    val formattedTax: String
        get() = "$${String.format("%.2f", taxCents / 100.0)}"
    val formattedTip: String
        get() = "$${String.format("%.2f", tipCents / 100.0)}"
    val formattedDiscount: String
        get() = "-$${String.format("%.2f", discountCents / 100.0)}"
    val formattedTotal: String
        get() = "$${String.format("%.2f", totalCents / 100.0)}"
    val totalDollars: String
        get() = String.format("%.2f", totalCents / 100.0)
}

data class EditableReceiptItem(
    val id: String,
    val name: String,
    val priceCents: Long,
    val priceText: String = String.format("%.2f", priceCents / 100.0)
) {
    val formattedPrice: String
        get() = "$${String.format("%.2f", priceCents / 100.0)}"
}

@HiltViewModel
class ReceiptReviewViewModel @Inject constructor(
    private val scannedReceiptStore: ScannedReceiptStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    sealed interface ReviewEvent {
        data class Continue(val amount: String, val description: String) : ReviewEvent
    }

    private val _events = Channel<ReviewEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadReceipt()
    }

    private fun loadReceipt() {
        val receipt = scannedReceiptStore.peek() ?: return
        _uiState.update {
            it.copy(
                merchant = receipt.merchant,
                items = receipt.items.map { item ->
                    EditableReceiptItem(
                        id = item.id,
                        name = item.name,
                        priceCents = item.priceCents
                    )
                },
                taxCents = receipt.taxCents,
                tipCents = receipt.tipCents,
                discountCents = receipt.discountCents,
                geminiTotalCents = receipt.totalCents
            )
        }
    }

    fun onItemTap(itemId: String) {
        _uiState.update {
            it.copy(editingItemId = if (it.editingItemId == itemId) null else itemId)
        }
    }

    fun onItemNameChange(itemId: String, name: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) item.copy(name = name) else item
                }
            )
        }
    }

    fun onItemPriceChange(itemId: String, priceText: String) {
        val filtered = priceText.filter { c -> c.isDigit() || c == '.' }
        val dotCount = filtered.count { it == '.' }
        if (dotCount > 1) return
        val dotIndex = filtered.indexOf('.')
        if (dotIndex != -1 && filtered.length - dotIndex - 1 > 2) return

        _uiState.update { state ->
            state.copy(
                userHasEdited = true,
                items = state.items.map { item ->
                    if (item.id == itemId) {
                        val cents = (filtered.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                        item.copy(priceCents = cents, priceText = filtered)
                    } else item
                }
            )
        }
    }

    fun onRemoveItem(itemId: String) {
        _uiState.update { state ->
            state.copy(
                userHasEdited = true,
                items = state.items.filter { it.id != itemId },
                editingItemId = if (state.editingItemId == itemId) null else state.editingItemId
            )
        }
    }

    fun onAddItem() {
        val newId = UUID.randomUUID().toString()
        _uiState.update { state ->
            state.copy(
                userHasEdited = true,
                items = state.items + EditableReceiptItem(
                    id = newId,
                    name = "",
                    priceCents = 0L,
                    priceText = ""
                ),
                editingItemId = newId
            )
        }
    }

    fun onMerchantChange(merchant: String) {
        _uiState.update { it.copy(merchant = merchant) }
    }

    fun onTaxChange(taxText: String) {
        val filtered = taxText.filter { c -> c.isDigit() || c == '.' }
        val cents = (filtered.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
        _uiState.update { it.copy(userHasEdited = true, taxCents = cents) }
    }

    fun onTipChange(tipText: String) {
        val filtered = tipText.filter { c -> c.isDigit() || c == '.' }
        val cents = (filtered.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
        _uiState.update { it.copy(userHasEdited = true, tipCents = cents) }
    }

    fun onDiscountChange(discountText: String) {
        val filtered = discountText.filter { c -> c.isDigit() || c == '.' }
        val cents = (filtered.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
        _uiState.update { it.copy(userHasEdited = true, discountCents = cents) }
    }

    fun onContinue() {
        val state = _uiState.value
        val updatedReceipt = ParsedReceipt(
            merchant = state.merchant,
            items = state.items.filter { it.name.isNotBlank() || it.priceCents > 0 }.map {
                ParsedReceiptItem(id = it.id, name = it.name, priceCents = it.priceCents)
            },
            taxCents = state.taxCents,
            tipCents = state.tipCents,
            discountCents = state.discountCents,
            totalCents = state.totalCents
        )
        scannedReceiptStore.store(updatedReceipt)

        val amount = state.totalDollars
        val description = state.merchant.ifBlank { "Receipt" }
        _events.trySend(ReviewEvent.Continue(amount, description))
    }
}
