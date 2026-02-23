package com.example.divvy.ui.scanreceipt.Views

import androidx.compose.runtime.Composable

@Composable
expect fun ScanReceiptScreen(
    onBack: () -> Unit,
    onScanComplete: (amount: String, description: String) -> Unit
)
