package com.example.divvy.ui.splitpercentage.Views

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvy.formatDouble
import com.example.divvy.ui.splitpercentage.ViewModels.PercentageMember
import com.example.divvy.ui.splitpercentage.ViewModels.SplitByPercentageViewModel

private val Purple = Color(0xFF7C4DFF)
private val Blue = Color(0xFF448AFF)
private val LightGray = Color(0xFFF5F5F5)
private val BorderGray = Color(0xFFE8E8E8)
private val TextGray = Color(0xFF999999)
private val GreenCheck = Color(0xFF4CAF50)
private val RedWarn = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitByPercentageScreen(viewModel: SplitByPercentageViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.done.collect { onDone() } }
    Scaffold(topBar = {
        TopAppBar(title = { Text(text = "Split by Percentage", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            actions = { Box(modifier = Modifier.padding(end = 12.dp).clip(RoundedCornerShape(20.dp)).background(if (uiState.isValid) Purple else Color.LightGray).clickable(enabled = uiState.isValid && !uiState.isSaving, onClick = viewModel::onDone).padding(horizontal = 20.dp, vertical = 8.dp)) { Text(text = if (uiState.isSaving) "..." else "Done", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) } })
    }) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp)) {
            item { Spacer(Modifier.height(8.dp)); InfoCard(uiState.description, uiState.amountDisplay); Spacer(Modifier.height(18.dp)) }
            item { MemberChipsRow(uiState.members); Spacer(Modifier.height(24.dp)) }
            item { PercentageTotalBar(uiState.totalPercentage, uiState.isValid, viewModel::onSplitEvenly); Spacer(Modifier.height(18.dp)) }
            item { Text(text = "SET PERCENTAGES", style = MaterialTheme.typography.labelMedium, color = TextGray, letterSpacing = 1.sp, fontSize = 12.sp); Spacer(Modifier.height(14.dp)) }
            items(uiState.members, key = { it.id }) { member ->
                MemberPercentageCard(member, uiState.percentages[member.id] ?: "", uiState.dollarAmountFor(member.id)) { value -> viewModel.onPercentageChange(member.id, value) }
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun InfoCard(description: String, amount: String) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LightGray).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Text(text = description, fontSize = 14.sp, color = TextGray); Spacer(Modifier.height(4.dp))
        val displayAmount = amount.toDoubleOrNull()?.let { "$${formatDouble(it, 2)}" } ?: "$$amount"
        Text(text = displayAmount, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable private fun MemberChipsRow(members: List<PercentageMember>) {
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

@Composable private fun PercentageTotalBar(total: Double, isValid: Boolean, onSplitEvenly: () -> Unit) {
    val progress = (total / 100.0).toFloat().coerceIn(0f, 1f)
    val barColor by animateColorAsState(targetValue = when { isValid -> GreenCheck; total > 100.0 -> RedWarn; else -> Purple }, label = "barColor")
    val totalLabel = formatDouble(total, 1)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom) { Text(text = "$totalLabel%", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = barColor); Text(text = " / 100%", fontSize = 14.sp, color = TextGray, modifier = Modifier.padding(bottom = 1.dp)) }
            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, BorderGray, RoundedCornerShape(16.dp)).clickable(onClick = onSplitEvenly).padding(horizontal = 14.dp, vertical = 6.dp)) { Text(text = "Split evenly", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Purple) }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = barColor, trackColor = LightGray)
    }
}

@Composable private fun MemberPercentageCard(member: PercentageMember, percentage: String, dollarAmount: String, onPercentageChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, BorderGray, RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(member.color), contentAlignment = Alignment.Center) { Text(text = member.name.first().uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) { Text(text = member.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black); Spacer(Modifier.height(2.dp)); Text(text = dollarAmount, fontSize = 13.sp, color = TextGray) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(LightGray).padding(horizontal = 12.dp, vertical = 8.dp)) {
            BasicTextField(value = percentage, onValueChange = onPercentageChange, textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.End), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, cursorBrush = SolidColor(Purple), modifier = Modifier.width(48.dp),
                decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterEnd) { if (percentage.isEmpty()) Text(text = "0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.LightGray, textAlign = TextAlign.End); innerTextField() } })
            Spacer(Modifier.width(2.dp)); Text(text = "%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextGray)
        }
    }
}
