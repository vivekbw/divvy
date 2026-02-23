package com.example.divvy.ui.ledger.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvy.components.GroupIcon
import com.example.divvy.formatDouble
import com.example.divvy.models.Group
import com.example.divvy.models.LedgerEntry
import com.example.divvy.models.LedgerEntryType
import com.example.divvy.ui.ledger.ViewModels.LedgerFilter
import com.example.divvy.ui.ledger.ViewModels.LedgerViewModel
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C4DFF)
private val GreenBg = Color(0xFFE8F5E9); private val GreenText = Color(0xFF2E7D32)
private val RedBg = Color(0xFFFCE4EC); private val RedText = Color(0xFFC62828)
private val SettlementBlue = Color(0xFFE3F2FD); private val SettlementBlueText = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(viewModel: LedgerViewModel = koinViewModel(), onBack: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val groupIconMap = remember(uiState.groupOptions) { uiState.groupOptions.associate { it.id to it.icon } }
    Scaffold(topBar = { TopAppBar(title = { Text(text = "Ledger", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(innerPadding)) {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                item { Spacer(Modifier.height(8.dp)); Text("Transaction history", style = MaterialTheme.typography.bodyMedium, color = Color.Gray); Spacer(Modifier.height(12.dp)) }
                item { NetBalanceCard(uiState.formattedNetBalance, uiState.isNetPositive); Spacer(Modifier.height(20.dp)) }
                item { FilterRow(uiState.filter, viewModel::onFilterSelected); Spacer(Modifier.height(8.dp)) }
                item { GroupFilterRow(uiState.groupOptions, uiState.selectedGroupId, viewModel::onGroupSelected); Spacer(Modifier.height(16.dp)) }
                item { Text("TRANSACTIONS", style = MaterialTheme.typography.labelMedium, color = Color.Gray, letterSpacing = 1.sp); Spacer(Modifier.height(10.dp)) }
                if (uiState.filteredEntries.isEmpty() && !uiState.isLoading) { item { EmptyState() } }
                items(uiState.filteredEntries, key = { it.id }) { entry -> LedgerEntryCard(entry, groupIconMap[entry.groupId]); Spacer(Modifier.height(8.dp)) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable private fun NetBalanceCard(formattedAmount: String, isPositive: Boolean) {
    val bgColor = if (isPositive) GreenBg else RedBg; val textColor = if (isPositive) GreenText else RedText; val label = if (isPositive) "Net: you are owed" else "Net: you owe"
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bgColor).padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.8f))
        Text(formattedAmount, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable private fun FilterRow(selectedFilter: LedgerFilter, onFilterSelected: (LedgerFilter) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LedgerFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter; val label = when (filter) { LedgerFilter.ALL -> "All"; LedgerFilter.EXPENSES -> "Expenses"; LedgerFilter.SETTLEMENTS -> "Settlements" }
            FilterChip(selected = selected, onClick = { onFilterSelected(filter) }, label = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Purple.copy(alpha = 0.12f), selectedLabelColor = Purple), shape = RoundedCornerShape(20.dp))
        }
    }
}

@Composable private fun GroupFilterRow(groups: List<Group>, selectedGroupId: String?, onGroupSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }; val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    Box {
        FilterChip(selected = selectedGroupId != null, onClick = { expanded = true }, label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedGroup != null) { Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(Purple.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { GroupIcon(icon = selectedGroup.icon, tint = Purple, modifier = Modifier.size(12.dp)) }; Spacer(Modifier.width(6.dp)) }
                Text(selectedGroup?.name ?: "All groups", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Purple.copy(alpha = 0.12f), selectedLabelColor = Purple), shape = RoundedCornerShape(20.dp))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All groups") }, onClick = { onGroupSelected(null); expanded = false })
            groups.forEach { group -> DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Purple.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { GroupIcon(icon = group.icon, tint = Purple, modifier = Modifier.size(14.dp)) }; Spacer(Modifier.width(10.dp)); Text(group.name) } }, onClick = { onGroupSelected(group.id); expanded = false }) }
        }
    }
}

@Composable private fun LedgerEntryCard(entry: LedgerEntry, groupIcon: com.example.divvy.components.GroupIcon? = null) {
    val isSettlement = entry.type == LedgerEntryType.SETTLEMENT; val amount = "$${formatDouble(entry.amountCents / 100.0, 2)}"
    val iconBg: Color; val iconTint: Color; val badgeLabel: String; val badgeBg: Color; val badgeTextColor: Color
    if (isSettlement) { iconBg = SettlementBlue; iconTint = SettlementBlueText; badgeLabel = if (entry.paidByCurrentUser) "You sent" else "Received"; badgeBg = if (entry.paidByCurrentUser) RedBg else GreenBg; badgeTextColor = if (entry.paidByCurrentUser) RedText else GreenText }
    else { iconBg = Purple.copy(alpha = 0.1f); iconTint = Purple; badgeLabel = if (entry.paidByCurrentUser) "You paid" else "You owe"; badgeBg = if (entry.paidByCurrentUser) GreenBg else RedBg; badgeTextColor = if (entry.paidByCurrentUser) GreenText else RedText }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { Icon(if (isSettlement) Icons.Filled.SwapHoriz else Icons.Filled.Receipt, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.Black); Text(amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.Black) }
                Spacer(Modifier.height(4.dp))
                val subtitle = if (isSettlement) "${entry.paidByName} → ${entry.toName}" else "Paid by ${entry.paidByName}"
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("${entry.dateLabel} · $subtitle", style = MaterialTheme.typography.bodySmall, color = Color.Gray); Box(Modifier.clip(RoundedCornerShape(20.dp)).background(badgeBg).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(badgeLabel, style = MaterialTheme.typography.labelSmall, color = badgeTextColor, fontWeight = FontWeight.SemiBold) } }
                if (entry.groupName.isNotBlank()) { Spacer(Modifier.height(4.dp)); Row(verticalAlignment = Alignment.CenterVertically) { if (groupIcon != null) { Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Purple.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { GroupIcon(icon = groupIcon, tint = Purple, modifier = Modifier.size(10.dp)) }; Spacer(Modifier.width(4.dp)) }; Text(entry.groupName, style = MaterialTheme.typography.labelSmall, color = Purple, fontWeight = FontWeight.Medium) } }
            }
        }
    }
}

@Composable private fun EmptyState() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.CheckCircle, null, tint = Color.LightGray, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(12.dp)); Text("No transactions found", style = MaterialTheme.typography.bodyMedium, color = Color.Gray); Spacer(Modifier.height(4.dp)); Text("Try adjusting your filters", style = MaterialTheme.typography.bodySmall, color = Color.LightGray) }
    }
}
