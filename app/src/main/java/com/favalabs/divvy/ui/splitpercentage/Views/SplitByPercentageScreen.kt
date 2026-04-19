package com.favalabs.divvy.ui.splitpercentage.Views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.favalabs.divvy.models.SupportedCurrency
import com.favalabs.divvy.models.formatAmount
import com.favalabs.divvy.ui.splitpercentage.ViewModels.PercentageMember
import com.favalabs.divvy.ui.splitpercentage.ViewModels.SplitByPercentageViewModel
import com.favalabs.divvy.ui.theme.DmSansFamily
import com.favalabs.divvy.ui.theme.NegativeRed
import com.favalabs.divvy.ui.theme.PositiveGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitByPercentageScreen(
    viewModel: SplitByPercentageViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.done.collect { onDone() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Split by %",
                        style = MaterialTheme.typography.titleLarge,
                    )
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
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (uiState.isValid) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                            .clickable(enabled = uiState.isValid && !uiState.isSaving, onClick = viewModel::onDone)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (uiState.isSaving) "..." else "Done",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                InfoCard(
                    description = uiState.description,
                    amount = uiState.amountDisplay,
                    currencyCode = uiState.currency
                )
                Spacer(Modifier.height(18.dp))
            }

            item {
                MemberChipsRow(members = uiState.members)
                Spacer(Modifier.height(24.dp))
            }

            item {
                PercentageTotalBar(
                    total = uiState.totalPercentage,
                    isValid = uiState.isValid,
                    onSplitEvenly = viewModel::onSplitEvenly
                )
                Spacer(Modifier.height(18.dp))
            }

            item {
                Text(
                    text = "SET PERCENTAGES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(14.dp))
            }

            items(uiState.members, key = { it.id }) { member ->
                MemberPercentageCard(
                    member = member,
                    percentage = uiState.percentages[member.id] ?: "",
                    dollarAmount = uiState.dollarAmountFor(member.id),
                    effectiveDollarAmount = uiState.effectiveDollarAmountFor(member.id),
                    onPercentageChange = { value ->
                        viewModel.onPercentageChange(member.id, value)
                    },
                    covererName = uiState.coveredBy[member.id]?.let { coverId ->
                        uiState.members.firstOrNull { it.id == coverId }?.name
                    },
                    isExpanded = uiState.expandedCoveringMemberId == member.id,
                    allMembers = uiState.members,
                    onToggleCovering = { viewModel.onToggleCoveringForMember(member.id) },
                    onSetCovering = { covererUserId ->
                        viewModel.onSetCovering(member.id, covererUserId)
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun InfoCard(description: String, amount: String, currencyCode: String = "USD") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        val displayAmount = amount.toDoubleOrNull()?.let {
            formatAmount((it * 100).toLong(), currencyCode)
        } ?: "${SupportedCurrency.fromCode(currencyCode).symbol}$amount"
        Text(
            text = displayAmount,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun MemberChipsRow(members: List<PercentageMember>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        members.forEach { member ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(member.color)
                    .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = member.name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun PercentageTotalBar(
    total: Double,
    isValid: Boolean,
    onSplitEvenly: () -> Unit
) {
    val progress = (total / 100.0).toFloat().coerceIn(0f, 1f)
    val barColor by animateColorAsState(
        targetValue = when {
            isValid -> PositiveGreen
            total > 100.0 -> NegativeRed
            else -> MaterialTheme.colorScheme.primary
        },
        label = "barColor"
    )
    val totalLabel = String.format("%.1f", total)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$totalLabel%",
                    style = MaterialTheme.typography.titleMedium,
                    color = barColor
                )
                Text(
                    text = " / 100%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable(onClick = onSplitEvenly)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Split evenly",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun MemberPercentageCard(
    member: PercentageMember,
    percentage: String,
    dollarAmount: String,
    effectiveDollarAmount: String,
    onPercentageChange: (String) -> Unit,
    covererName: String?,
    isExpanded: Boolean,
    allMembers: List<PercentageMember>,
    onToggleCovering: () -> Unit,
    onSetCovering: (covererUserId: String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(member.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.first().uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                if (covererName != null) {
                    Text(
                        text = dollarAmount,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Text(
                        text = "Covered by $covererName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = effectiveDollarAmount,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (covererName != null) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove covering",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onSetCovering(null) }
                )
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Rounded.CardGiftcard,
                    contentDescription = "Cover this share",
                    tint = if (isExpanded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onToggleCovering)
                )
                Spacer(Modifier.width(8.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = percentage,
                    onValueChange = onPercentageChange,
                    textStyle = TextStyle(
                        fontFamily = DmSansFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.End
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.width(48.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (percentage.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontFamily = DmSansFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.End
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "%",
                    fontFamily = DmSansFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 70.dp, end = 16.dp, bottom = 14.dp)
            ) {
                Text(
                    text = "Who's covering ${member.name}'s share?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allMembers.filter { it.id != member.id }.forEach { candidate ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSetCovering(candidate.id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = candidate.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }
    }
}
