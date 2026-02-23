package com.example.divvy.ui.assignitems.Views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvy.formatDouble
import com.example.divvy.ui.assignitems.ViewModels.AssignItemsViewModel
import com.example.divvy.ui.assignitems.ViewModels.AssignMember
import com.example.divvy.ui.assignitems.ViewModels.ReceiptItem

private val Purple = Color(0xFF7C4DFF)
private val LightGray = Color(0xFFF5F5F5)
private val BorderGray = Color(0xFFE8E8E8)
private val TextGray = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssignItemsScreen(viewModel: AssignItemsViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.done.collect { onDone() } }
    Scaffold(topBar = {
        TopAppBar(title = { Text(text = "Assign Items", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            actions = { Box(modifier = Modifier.padding(end = 12.dp).clip(RoundedCornerShape(20.dp)).background(Purple).clickable(enabled = !uiState.isSaving, onClick = viewModel::onNext).padding(horizontal = 20.dp, vertical = 8.dp)) { Text(text = if (uiState.isSaving) "..." else "Done", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) } })
    }) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                StoreInfoCard(uiState.description, uiState.amountDisplay)
                Spacer(Modifier.height(18.dp))
            }
            item { MemberChipsRow(uiState.members); Spacer(Modifier.height(24.dp)) }
            item { Text(text = "TAP ITEMS TO ASSIGN", style = MaterialTheme.typography.labelMedium, color = TextGray, letterSpacing = 1.sp, fontSize = 12.sp); Spacer(Modifier.height(14.dp)) }
            items(uiState.items, key = { it.id }) { item ->
                val assignedIds = uiState.assignments[item.id].orEmpty()
                val assignedMembers = uiState.members.filter { it.id in assignedIds }
                ItemCard(item, uiState.expandedItemId == item.id, assignedMembers, uiState.members, assignedIds, { viewModel.onItemTap(item.id) }, { memberId -> viewModel.onToggleMemberForItem(item.id, memberId) })
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun StoreInfoCard(description: String, amount: String) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LightGray).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Text(text = description, fontSize = 14.sp, color = TextGray); Spacer(Modifier.height(4.dp))
        val displayAmount = amount.toDoubleOrNull()?.let { "$${formatDouble(it, 2)}" } ?: "$$amount"
        Text(text = displayAmount, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable private fun MemberChipsRow(members: List<AssignMember>) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        members.forEach { member ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(member.color).padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f))); Spacer(Modifier.width(6.dp))
                Text(text = member.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }; Spacer(Modifier.width(8.dp))
        }
        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(LightGray), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit members", tint = TextGray, modifier = Modifier.size(16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ItemCard(item: ReceiptItem, isExpanded: Boolean, assignedMembers: List<AssignMember>, allMembers: List<AssignMember>, assignedMemberIds: Set<String>, onTap: () -> Unit, onToggleMember: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, BorderGray, RoundedCornerShape(14.dp)).background(Color.White).clickable(onClick = onTap).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Color.Black); Spacer(Modifier.height(4.dp))
                if (assignedMembers.isEmpty()) Text(text = "Not assigned", fontSize = 12.sp, color = TextGray)
                else Row(horizontalArrangement = Arrangement.spacedBy((-4).dp), verticalAlignment = Alignment.CenterVertically) { assignedMembers.forEach { InitialAvatar(it.name, it.color, 24) } }
            }
            Text(text = item.formattedPrice, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        }
        AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
                allMembers.forEach { member -> MemberAssignChip(member, member.id in assignedMemberIds, { onToggleMember(member.id) }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable private fun InitialAvatar(name: String, color: Color, size: Int = 24) {
    Box(modifier = Modifier.size(size.dp).clip(CircleShape).background(color).border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
        Text(text = name.first().uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2).sp)
    }
}

@Composable private fun MemberAssignChip(member: AssignMember, isAssigned: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (isAssigned) member.color else Color.White; val borderColor = if (isAssigned) member.color else BorderGray
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).border(1.5.dp, borderColor, RoundedCornerShape(10.dp)).background(bgColor).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text = member.name, fontSize = 14.sp, fontWeight = if (isAssigned) FontWeight.SemiBold else FontWeight.Normal, color = if (isAssigned) Color.White else Color.Black)
    }
}
