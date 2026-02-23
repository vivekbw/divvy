package com.example.divvy.ui.analytics.Views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvy.components.GroupIcon
import com.example.divvy.formatDouble
import com.example.divvy.ui.analytics.ViewModels.AnalyticsViewModel
import com.example.divvy.ui.analytics.ViewModels.GroupSpending
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C4DFF); private val Blue = Color(0xFF448AFF)
private val GreenBg = Color(0xFFE8F5E9); private val GreenText = Color(0xFF2E7D32)
private val RedBg = Color(0xFFFCE4EC); private val RedText = Color(0xFFC62828)
private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = koinViewModel(), onBack: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Analytics", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { innerPadding ->
        Column(Modifier.fillMaxSize().background(Color.White).padding(innerPadding)) {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                item { Spacer(Modifier.height(8.dp)); Text("Spending overview", style = MaterialTheme.typography.bodyMedium, color = Color.Gray); Spacer(Modifier.height(12.dp)) }
                item { TotalSpentCard(uiState.formattedTotalSpent, uiState.expenseCount); Spacer(Modifier.height(12.dp)) }
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatCard("Your share", uiState.formattedYourShare, Modifier.weight(1f)); StatCard("You paid", uiState.formattedYouPaid, Modifier.weight(1f)) }; Spacer(Modifier.height(12.dp)) }
                item { OverpaidCard(uiState.formattedOverpaid, uiState.isOverpaying); Spacer(Modifier.height(24.dp)) }
                item { Text("SPENDING BY GROUP", style = MaterialTheme.typography.labelMedium, color = Color.Gray, letterSpacing = 1.sp); Spacer(Modifier.height(10.dp)) }
                items(uiState.groupSpending) { group -> GroupSpendingRow(group, uiState.maxGroupSpendingCents); Spacer(Modifier.height(10.dp)) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable private fun TotalSpentCard(totalSpent: String, expenseCount: Int) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(Purple, Blue))).padding(24.dp)) {
        Column { Text("Total group spending", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f)); Spacer(Modifier.height(4.dp)); Text(totalSpent, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White); Spacer(Modifier.height(4.dp)); Text("$expenseCount expenses across all groups", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f)) }
    }
}
@Composable private fun StatCard(label: String, amount: String, modifier: Modifier = Modifier) {
    Card(modifier, RoundedCornerShape(14.dp), CardDefaults.cardColors(containerColor = LightGray), CardDefaults.cardElevation(0.dp)) { Column(Modifier.padding(16.dp)) { Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray); Spacer(Modifier.height(4.dp)); Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black) } }
}
@Composable private fun OverpaidCard(formattedAmount: String, isOverpaying: Boolean) {
    val bgColor = if (isOverpaying) GreenBg else RedBg; val textColor = if (isOverpaying) GreenText else RedText
    val label = if (isOverpaying) "You've overpaid by" else "You've underpaid by"
    val subtitle = if (isOverpaying) "You fronted more than your share — others owe you" else "You owe others for expenses they covered"
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CardDefaults.cardColors(containerColor = bgColor), CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(label, style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.8f)); Text(formattedAmount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor) }; Spacer(Modifier.height(4.dp)); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f)) }
    }
}
@Composable private fun GroupSpendingRow(group: GroupSpending, maxCents: Long) {
    val fraction = if (maxCents > 0) group.yourShareCents.toFloat() / maxCents else 0f
    val animatedFraction by animateFloatAsState(fraction, tween(600), label = "bar")
    val dollars = group.yourShareCents / 100.0; val totalDollars = group.totalCents / 100.0
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CardDefaults.cardColors(containerColor = Color.White), CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Purple.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { GroupIcon(icon = group.groupIcon, tint = Purple, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(group.groupName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.Black); Text("Group total: $${formatDouble(totalDollars, 2)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                Text("$${formatDouble(dollars, 2)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Purple)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Purple.copy(alpha = 0.08f))) { Box(Modifier.fillMaxWidth(animatedFraction).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(Purple, Blue)))) }
        }
    }
}
