package com.example.divvy.ui.ledger.Views

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.divvy.models.LedgerEntry
import com.example.divvy.models.LedgerEntryType
import com.example.divvy.ui.ledger.ViewModels.LedgerFilter
import com.example.divvy.ui.ledger.ViewModels.LedgerViewModel

private val Purple = Color(0xFF7C4DFF)
private val GreenBg = Color(0xFFE8F5E9)
private val GreenText = Color(0xFF2E7D32)
private val RedBg = Color(0xFFFCE4EC)
private val RedText = Color(0xFFC62828)
private val LightGray = Color(0xFFF5F5F5)
private val SettlementBlue = Color(0xFFE3F2FD)
private val SettlementBlueText = Color(0xFF1565C0)

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Transaction history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                NetBalanceCard(
                    formattedAmount = uiState.formattedNetBalance,
                    isPositive = uiState.isNetPositive
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                FilterRow(
                    selectedFilter = uiState.filter,
                    onFilterSelected = viewModel::onFilterSelected
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                GroupFilterRow(
                    groups = uiState.groupOptions,
                    selectedGroupId = uiState.selectedGroupId,
                    onGroupSelected = viewModel::onGroupSelected
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "TRANSACTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (uiState.filteredEntries.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyState()
                }
            }

            items(uiState.filteredEntries, key = { it.id }) { entry ->
                LedgerEntryCard(entry = entry)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NetBalanceCard(
    formattedAmount: String,
    isPositive: Boolean
) {
    val bgColor = if (isPositive) GreenBg else RedBg
    val textColor = if (isPositive) GreenText else RedText
    val label = if (isPositive) "Net: you are owed" else "Net: you owe"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.8f)
        )
        Text(
            text = formattedAmount,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun FilterRow(
    selectedFilter: LedgerFilter,
    onFilterSelected: (LedgerFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LedgerFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            val label = when (filter) {
                LedgerFilter.ALL -> "All"
                LedgerFilter.EXPENSES -> "Expenses"
                LedgerFilter.SETTLEMENTS -> "Settlements"
            }
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Purple.copy(alpha = 0.12f),
                    selectedLabelColor = Purple
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun GroupFilterRow(
    groups: List<Pair<String, String>>,
    selectedGroupId: String?,
    onGroupSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = groups.firstOrNull { it.first == selectedGroupId }?.second ?: "All groups"

    Box {
        FilterChip(
            selected = selectedGroupId != null,
            onClick = { expanded = true },
            label = {
                Text(
                    text = selectedLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Purple.copy(alpha = 0.12f),
                selectedLabelColor = Purple
            ),
            shape = RoundedCornerShape(20.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All groups") },
                onClick = {
                    onGroupSelected(null)
                    expanded = false
                }
            )
            groups.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onGroupSelected(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LedgerEntryCard(entry: LedgerEntry) {
    val isSettlement = entry.type == LedgerEntryType.SETTLEMENT
    val dollars = entry.amountCents / 100.0
    val amount = "$${String.format("%.2f", dollars)}"

    val iconBg: Color
    val iconTint: Color
    val badgeLabel: String
    val badgeBg: Color
    val badgeTextColor: Color

    if (isSettlement) {
        iconBg = SettlementBlue
        iconTint = SettlementBlueText
        badgeLabel = if (entry.paidByCurrentUser) "You sent" else "Received"
        badgeBg = if (entry.paidByCurrentUser) RedBg else GreenBg
        badgeTextColor = if (entry.paidByCurrentUser) RedText else GreenText
    } else {
        iconBg = Purple.copy(alpha = 0.1f)
        iconTint = Purple
        badgeLabel = if (entry.paidByCurrentUser) "You paid" else "You owe"
        badgeBg = if (entry.paidByCurrentUser) GreenBg else RedBg
        badgeTextColor = if (entry.paidByCurrentUser) GreenText else RedText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSettlement) Icons.Filled.SwapHoriz else Icons.Filled.Receipt,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val subtitle = if (isSettlement) {
                    "${entry.paidByName} → ${entry.toName}"
                } else {
                    "Paid by ${entry.paidByName}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${entry.dateLabel} · $subtitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTextColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!isSettlement && entry.groupName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.groupName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Purple,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No transactions found",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try adjusting your filters",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }
    }
}
