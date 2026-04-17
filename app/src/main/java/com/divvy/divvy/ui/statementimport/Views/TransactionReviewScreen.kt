package com.divvy.divvy.ui.statementimport.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divvy.divvy.models.TransactionStatus
import com.divvy.divvy.ui.statementimport.ViewModels.FilterTab
import com.divvy.divvy.ui.statementimport.ViewModels.ReviewableTransaction
import com.divvy.divvy.ui.statementimport.ViewModels.TransactionReviewViewModel
import com.divvy.divvy.ui.theme.NegativeRed
import com.divvy.divvy.ui.theme.PositiveGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionReviewScreen(
    viewModel: TransactionReviewViewModel,
    onBack: () -> Unit,
    onAddAsExpense: (amountCents: Long, description: String) -> Unit,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Review Transactions",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "${uiState.addedCount} of ${uiState.totalCount} added",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onDone)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTab.entries.forEach { tab ->
                    FilterChip(
                        selected = uiState.activeFilter == tab,
                        onClick = { viewModel.onFilterSelected(tab) },
                        label = { Text(tab.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredTransactions, key = { it.index }) { reviewable ->
                    TransactionCard(
                        reviewable = reviewable,
                        onClick = { viewModel.onTransactionSelected(reviewable) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    uiState.selectedTransaction?.let { selected ->
        TransactionDetailSheet(
            transaction = selected,
            onDismiss = viewModel::onDismissDetail,
            onSkip = { viewModel.onSkipTransaction(selected.index) },
            onAddAsExpense = {
                viewModel.onMarkAdded(selected.index)
                onAddAsExpense(
                    selected.transaction.amountCents,
                    selected.transaction.description
                )
            }
        )
    }
}

@Composable
private fun TransactionCard(
    reviewable: ReviewableTransaction,
    onClick: () -> Unit
) {
    val tx = reviewable.transaction
    val statusColor = when (reviewable.status) {
        TransactionStatus.Pending -> MaterialTheme.colorScheme.surface
        TransactionStatus.Added -> PositiveGreen.copy(alpha = 0.08f)
        TransactionStatus.Skipped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(statusColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NegativeRed.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = "Expense",
                tint = NegativeRed,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = tx.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            val dollars = tx.amountCents / 100.0
            Text(
                text = "$${String.format("%.2f", dollars)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (reviewable.status != TransactionStatus.Pending) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (reviewable.status == TransactionStatus.Added)
                            Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (reviewable.status == TransactionStatus.Added)
                            PositiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = if (reviewable.status == TransactionStatus.Added) "Added" else "Skipped",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (reviewable.status == TransactionStatus.Added)
                            PositiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
