package com.example.divvy.ui.groupdetail.Views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvy.formatDouble
import com.example.divvy.models.ActivityItem
import com.example.divvy.models.MemberBalance
import com.example.divvy.ui.groupdetail.ViewModels.GroupDetailViewModel
import com.example.divvy.ui.groupdetail.ViewModels.SettleMode
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val Purple = Color(0xFF7C4DFF)
private val Blue = Color(0xFF448AFF)
private val GreenBg = Color(0xFF2E7D32)
private val RedBg = Color(0xFFC62828)
private val GreenText = Color(0xFF2E7D32)
private val RedText = Color(0xFFC62828)
private val GreenBadgeBg = Color(0xFFE8F5E9)
private val RedBadgeBg = Color(0xFFFCE4EC)

private val avatarColors = listOf(Color(0xFF7C4DFF), Color(0xFF2E7D32), Color(0xFFE65100), Color(0xFF1565C0), Color(0xFF00695C))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    onLeaveGroup: () -> Unit,
    onAddExpense: () -> Unit,
) {
    val viewModel: GroupDetailViewModel = koinViewModel(parameters = { parametersOf(groupId) })
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.leftGroup) { if (uiState.leftGroup) onLeaveGroup() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.group.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = { viewModel.onGearClick() }) { Icon(imageVector = Icons.Default.Settings, contentDescription = "Manage Group") } }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(Color.White).padding(innerPadding)) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                item {
                    AnimatedVisibility(visible = uiState.showManagePanel) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            ManageGroupCard(groupId = groupId, onLeaveGroup = { viewModel.onLeaveGroup() }, onCopyLink = { url -> clipboardManager.setText(AnnotatedString(url)) })
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)); BalanceSummaryCard(balanceCents = uiState.group.balanceCents); Spacer(modifier = Modifier.height(24.dp)) }
                item { SectionLabel("BALANCES"); Spacer(modifier = Modifier.height(10.dp)) }
                items(uiState.memberBalances) { mb ->
                    MemberBalanceCard(mb, avatarColors[uiState.memberBalances.indexOf(mb) % avatarColors.size],
                        uiState.expandedMemberId == mb.userId, uiState.settleMode, uiState.settleAmount, uiState.isSettling,
                        { viewModel.onMemberClick(mb.userId) }, viewModel::onSettleModeSelected, viewModel::onSettleAmountChange, { viewModel.onConfirmSettle(mb.userId) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item { Spacer(modifier = Modifier.height(8.dp)); SectionLabel("ACTIVITY"); Spacer(modifier = Modifier.height(10.dp)) }
                items(uiState.activity) { item -> ActivityCard(item = item); Spacer(modifier = Modifier.height(8.dp)) }
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(50.dp)).background(Brush.horizontalGradient(listOf(Purple, Blue))).clickable { onAddExpense() }, contentAlignment = Alignment.Center) {
                Text(text = "Add New Expense", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ManageGroupCard(groupId: String, onLeaveGroup: () -> Unit, onCopyLink: (String) -> Unit) {
    val inviteUrl = "divvy.app/join/$groupId"
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFEDE7F6)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Purple, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Invite Link", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
                    Text(text = inviteUrl, fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = { onCopyLink(inviteUrl) }) { Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy link", tint = Color.Gray, modifier = Modifier.size(20.dp)) }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)).background(Color(0xFFFFF5F5)).clickable(onClick = onLeaveGroup).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Leave Group", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFFD32F2F))
            }
        }
    }
}

@Composable
private fun BalanceSummaryCard(balanceCents: Long) {
    val isOwed = balanceCents >= 0
    val bgColor = if (isOwed) GreenBg else RedBg
    val label = if (isOwed) "You are owed" else "You owe"
    val amount = "$${formatDouble(kotlin.math.abs(balanceCents) / 100.0, 2)}"
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bgColor).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = amount, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable private fun SectionLabel(text: String) { Text(text = text, style = MaterialTheme.typography.labelMedium, color = Color.Gray, letterSpacing = 1.sp) }

@Composable
private fun MemberBalanceCard(memberBalance: MemberBalance, avatarColor: Color, isExpanded: Boolean, settleMode: SettleMode?, settleAmount: String, isSettling: Boolean, onCardClick: () -> Unit, onModeSelect: (SettleMode) -> Unit, onAmountChange: (String) -> Unit, onConfirm: () -> Unit) {
    val isOwedByThem = memberBalance.balanceCents >= 0
    val arrowLabel = if (isOwedByThem) "↑ owes you" else "↓ you owe"
    val amountColor = if (isOwedByThem) GreenText else RedText
    val amount = "$${formatDouble(kotlin.math.abs(memberBalance.balanceCents) / 100.0, 2)}"
    Card(onClick = onCardClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) { Text(text = memberBalance.name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = memberBalance.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.weight(1f))
            Text(text = "$arrowLabel $amount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = amountColor)
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettleChip("Paid Fully", settleMode == SettleMode.Fully) { onModeSelect(SettleMode.Fully) }
                    SettleChip("Paid Partially", settleMode == SettleMode.Partially) { onModeSelect(SettleMode.Partially) }
                }
                AnimatedVisibility(visible = settleMode == SettleMode.Partially) {
                    Column { Spacer(Modifier.height(10.dp)); Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        BasicTextField(value = settleAmount, onValueChange = onAmountChange, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black),
                            decorationBox = { innerTextField -> Row(verticalAlignment = Alignment.CenterVertically) { Text(text = "$", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black); innerTextField() } }, modifier = Modifier.fillMaxWidth())
                    } }
                }
                Spacer(Modifier.height(12.dp))
                val canConfirm = settleMode != null && (settleMode == SettleMode.Fully || settleAmount.toDoubleOrNull()?.let { it > 0 } == true)
                val buttonBrush = if (canConfirm && !isSettling) Brush.horizontalGradient(listOf(Purple, Blue)) else Brush.horizontalGradient(listOf(Color.LightGray, Color.LightGray))
                Box(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(50.dp)).background(buttonBrush).clickable(enabled = canConfirm && !isSettling) { onConfirm() }, contentAlignment = Alignment.Center) {
                    if (isSettling) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(text = "Confirm", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SettleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(if (selected) Purple else Color.Transparent).border(1.dp, if (selected) Color.Transparent else Color.LightGray, RoundedCornerShape(50.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text = label, color = if (selected) Color.White else Color.Gray, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ActivityCard(item: ActivityItem) {
    val amount = "$${formatDouble(item.amountCents / 100.0, 2)}"
    val badgeLabel = if (item.paidByCurrentUser) "You paid" else "You owe"
    val badgeBg = if (item.paidByCurrentUser) GreenBadgeBg else RedBadgeBg
    val badgeTextColor = if (item.paidByCurrentUser) GreenText else RedText
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${item.dateLabel} · Paid by ${item.paidByLabel}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(badgeBg).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(text = badgeLabel, style = MaterialTheme.typography.labelSmall, color = badgeTextColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
