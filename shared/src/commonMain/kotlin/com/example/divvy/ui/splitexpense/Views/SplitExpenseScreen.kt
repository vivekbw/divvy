package com.example.divvy.ui.splitexpense.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvy.components.GroupIcon
import com.example.divvy.models.Group
import com.example.divvy.ui.splitexpense.ViewModels.SplitExpenseViewModel
import com.example.divvy.ui.splitexpense.ViewModels.SplitMethod
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C4DFF)
private val Blue = Color(0xFF448AFF)
private val LightGray = Color(0xFFF5F5F5)
private val BorderGray = Color(0xFFE8E8E8)
private val TextGray = Color(0xFF999999)
private val SubtitleGray = Color(0xFF888888)
private val GradientBrush = Brush.horizontalGradient(listOf(Purple, Blue))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseScreen(
    viewModel: SplitExpenseViewModel = koinViewModel(),
    onBack: () -> Unit,
    onNavigateToAssignItems: (groupId: String, amount: String, description: String) -> Unit = { _, _, _ -> },
    onNavigateToSplitByPercentage: (groupId: String, amount: String, description: String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SplitExpenseViewModel.SplitEvent.Created -> onBack()
                is SplitExpenseViewModel.SplitEvent.GoToAssignItems -> onNavigateToAssignItems(event.groupId, event.amount, event.description)
                is SplitExpenseViewModel.SplitEvent.GoToSplitByPercentage -> onNavigateToSplitByPercentage(event.groupId, event.amount, event.description)
            }
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(text = "Split Expense", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } })
    }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(innerPadding)) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(8.dp))
                AmountSection(uiState.amount, viewModel::onAmountChange)
                Spacer(Modifier.height(16.dp))
                DescriptionField(uiState.description, viewModel::onDescriptionChange)
                Spacer(Modifier.height(28.dp))
                GroupSelectionSection(uiState.groups, uiState.selectedGroupId, viewModel::onGroupSelected)
                Spacer(Modifier.height(28.dp))
                SplitMethodSection(uiState.splitMethod, viewModel::onSplitMethodSelected)
                Spacer(Modifier.height(32.dp))
            }
            CreateSplitButton(uiState.amount.isNotBlank() && uiState.selectedGroupId != null, uiState.isCreating, viewModel::onCreateSplit)
        }
    }
}

@Composable private fun AmountSection(amount: String, onAmountChange: (String) -> Unit) {
    Text(text = "Total Amount", style = MaterialTheme.typography.bodySmall, color = TextGray, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Text(text = "$", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
        Spacer(Modifier.width(4.dp))
        BasicTextField(value = amount, onValueChange = onAmountChange, textStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.Black), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, cursorBrush = SolidColor(Purple), modifier = Modifier.weight(1f),
            decorationBox = { innerTextField -> if (amount.isEmpty()) Text(text = "0.00", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.LightGray); innerTextField() })
    }
}

@Composable private fun DescriptionField(description: String, onDescriptionChange: (String) -> Unit) {
    BasicTextField(value = description, onValueChange = onDescriptionChange, textStyle = TextStyle(fontSize = 14.sp, color = Color.DarkGray, textAlign = TextAlign.Center), singleLine = true, cursorBrush = SolidColor(Purple), modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField -> Box(modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray, RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) { if (description.isEmpty()) Text(text = "What's this for?", fontSize = 14.sp, color = Color.LightGray, textAlign = TextAlign.Center); innerTextField() } })
}

@Composable private fun GroupSelectionSection(groups: List<Group>, selectedGroupId: String?, onGroupSelected: (String) -> Unit) {
    Text(text = "Select Group", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 15.sp)
    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { groups.forEach { group -> GroupCard(group, group.id == selectedGroupId) { onGroupSelected(group.id) } } }
}

@Composable private fun GroupCard(group: Group, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Purple else BorderGray; val borderWidth = if (isSelected) 2.dp else 1.dp
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(borderWidth, borderColor, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(if (isSelected) GradientBrush else Brush.horizontalGradient(listOf(LightGray, LightGray))), contentAlignment = Alignment.Center) { GroupIcon(icon = group.icon, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp)); Text(text = group.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black, modifier = Modifier.weight(1f))
        if (isSelected) Icon(imageVector = Icons.Rounded.Check, contentDescription = "Selected", tint = Purple, modifier = Modifier.size(22.dp))
    }
}

@Composable private fun SplitMethodSection(selectedMethod: SplitMethod, onMethodSelected: (SplitMethod) -> Unit) {
    Text(text = "Split Method", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.Black, fontSize = 15.sp)
    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { SplitMethod.entries.forEach { method -> SplitMethodCard(method, method == selectedMethod) { onMethodSelected(method) } } }
}

@Composable private fun SplitMethodCard(method: SplitMethod, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Purple else BorderGray; val borderWidth = if (isSelected) 2.dp else 1.dp
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(borderWidth, borderColor, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(if (isSelected) GradientBrush else Brush.horizontalGradient(listOf(LightGray, LightGray))), contentAlignment = Alignment.Center) {
            Icon(imageVector = method.icon(), contentDescription = null, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) { Text(text = method.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black); Spacer(Modifier.height(2.dp)); Text(text = method.subtitle, fontSize = 12.sp, color = SubtitleGray) }
        if (isSelected) Icon(imageVector = Icons.Rounded.Check, contentDescription = "Selected", tint = Purple, modifier = Modifier.size(22.dp))
    }
}

@Composable private fun CreateSplitButton(enabled: Boolean, isCreating: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp, top = 8.dp).height(54.dp).clip(RoundedCornerShape(50.dp)).background(if (enabled && !isCreating) GradientBrush else Brush.horizontalGradient(listOf(Color.LightGray, Color.LightGray))).clickable(enabled = enabled && !isCreating, onClick = onClick), contentAlignment = Alignment.Center) {
        if (isCreating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        else Text(text = "Create Split", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

private fun SplitMethod.icon(): ImageVector = when (this) {
    SplitMethod.Equally -> Icons.Rounded.AttachMoney
    SplitMethod.ByPercentage -> Icons.Rounded.Percent
    SplitMethod.ByItems -> Icons.Rounded.Checklist
}
