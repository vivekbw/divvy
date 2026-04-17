package com.divvy.divvy.ui.assignitems.Views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.divvy.divvy.models.SupportedCurrency
import com.divvy.divvy.models.formatAmount
import com.divvy.divvy.ui.assignitems.ViewModels.AssignOwedAmount
import com.divvy.divvy.ui.assignitems.ViewModels.AssignItemsViewModel
import com.divvy.divvy.ui.assignitems.ViewModels.AssignMember
import com.divvy.divvy.ui.assignitems.ViewModels.ReceiptItem
import com.divvy.divvy.ui.theme.DmSansFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignItemsScreen(
    viewModel: AssignItemsViewModel,
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
                        text = "Assign Items",
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
                                if (uiState.canSubmit) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                            .clickable(enabled = uiState.canSubmit, onClick = viewModel::onNext)
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
                StoreInfoCard(
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
                AssignmentCoverageBar(
                    assignedAmount = uiState.formattedAssignedItemTotal,
                    subtotal = uiState.formattedSubtotal,
                    progress = uiState.assignmentProgress,
                    isComplete = uiState.isCoverageComplete,
                )
                Spacer(Modifier.height(18.dp))
            }

            item {
                Text(
                    text = if (uiState.isManualMode) "ADD & ASSIGN ITEMS" else "TAP ITEMS TO ASSIGN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(14.dp))
            }

            items(uiState.items, key = { it.id }) { item ->
                val isExpanded = uiState.expandedItemId == item.id
                val assignedIds = uiState.assignments[item.id].orEmpty()
                val assignedMembers = uiState.members.filter { it.id in assignedIds }

                val isEditing = uiState.editingItemId == item.id
                EditableItemCard(
                    item = item,
                    currencyCode = uiState.currency,
                    isExpanded = isExpanded,
                    isEditing = isEditing,
                    assignedMembers = assignedMembers,
                    allMembers = uiState.members,
                    assignedMemberIds = assignedIds,
                    onTap = { viewModel.onItemTap(item.id) },
                    onToggleMember = { memberId ->
                        viewModel.onToggleMemberForItem(item.id, memberId)
                    },
                    onNameChange = { viewModel.onItemNameChange(item.id, it) },
                    onPriceChange = { viewModel.onItemPriceChange(item.id, it) },
                    onRemove = { viewModel.onRemoveItem(item.id) },
                    onEditToggle = { viewModel.onEditItem(item.id) }
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                AddItemButton(onClick = viewModel::onAddItem)
                Spacer(Modifier.height(16.dp))
            }

            if (uiState.isManualMode) {
                item {
                    ManualExtrasSection(
                        taxEnabled = uiState.taxEnabled,
                        taxIsPercent = uiState.taxIsPercent,
                        taxText = uiState.taxText,
                        tipEnabled = uiState.tipEnabled,
                        tipIsPercent = uiState.tipIsPercent,
                        tipText = uiState.tipText,
                        discountEnabled = uiState.discountEnabled,
                        discountText = uiState.discountText,
                        onToggleTax = viewModel::onToggleTax,
                        onTaxModeChange = viewModel::onTaxModeChange,
                        onTaxTextChange = viewModel::onTaxTextChange,
                        onToggleTip = viewModel::onToggleTip,
                        onTipModeChange = viewModel::onTipModeChange,
                        onTipTextChange = viewModel::onTipTextChange,
                        onToggleDiscount = viewModel::onToggleDiscount,
                        onDiscountTextChange = viewModel::onDiscountTextChange,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                CalculationSummaryCard(
                    subtotal = uiState.formattedSubtotal,
                    tax = uiState.formattedTax,
                    tip = uiState.formattedTip,
                    discount = uiState.formattedDiscount,
                    total = uiState.formattedCalculatedTotal,
                    enteredTotal = uiState.formattedEnteredTotal,
                    isTotalMatch = uiState.isTotalMatch,
                    totalDifference = uiState.formattedTotalDifference,
                )
                Spacer(Modifier.height(24.dp))
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "COVERING",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Gift someone's share so they don't owe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
            }

            items(uiState.members, key = { "cover_${it.id}" }) { member ->
                CoveringMemberRow(
                    member = member,
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

            item {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(18.dp))
                OwedSummaryCard(
                    members = uiState.members,
                    owedAmounts = uiState.owedAmounts,
                    currencyCode = uiState.currency,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StoreInfoCard(description: String, amount: String, currencyCode: String = "USD") {
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
private fun MemberChipsRow(members: List<AssignMember>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        members.forEach { member ->
            MemberChip(member = member)
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun MemberChip(member: AssignMember) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableItemCard(
    item: ReceiptItem,
    currencyCode: String = "USD",
    isExpanded: Boolean,
    isEditing: Boolean,
    assignedMembers: List<AssignMember>,
    allMembers: List<AssignMember>,
    assignedMemberIds: Set<String>,
    onTap: () -> Unit,
    onToggleMember: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemove: () -> Unit,
    onEditToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name.ifBlank { "Unnamed item" },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.name.isBlank()) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                if (assignedMembers.isEmpty()) {
                    Text(
                        text = "Not assigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-4).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        assignedMembers.forEach { member ->
                            InitialAvatar(
                                name = member.name,
                                color = member.color,
                                size = 24
                            )
                        }
                    }
                }
            }

            Text(
                text = item.formattedPrice(currencyCode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626).copy(alpha = 0.1f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                if (!isEditing) {
                    Text(
                        text = "Edit receipt item details and assign who shared it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                EditField(
                    label = "Item name",
                    value = item.name,
                    onValueChange = onNameChange
                )
                Spacer(Modifier.height(8.dp))
                EditField(
                    label = "Price",
                    value = item.priceText,
                    onValueChange = onPriceChange,
                    prefix = "$",
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2
                ) {
                    allMembers.forEach { member ->
                        val isAssigned = member.id in assignedMemberIds
                        MemberAssignChip(
                            member = member,
                            isAssigned = isAssigned,
                            onClick = { onToggleMember(member.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (prefix.isNotEmpty()) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = DmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = if (prefix.isNotEmpty()) "0.00" else label,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun AddItemButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Add item",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ManualExtrasSection(
    taxEnabled: Boolean,
    taxIsPercent: Boolean,
    taxText: String,
    tipEnabled: Boolean,
    tipIsPercent: Boolean,
    tipText: String,
    discountEnabled: Boolean,
    discountText: String,
    onToggleTax: (Boolean) -> Unit,
    onTaxModeChange: (Boolean) -> Unit,
    onTaxTextChange: (String) -> Unit,
    onToggleTip: (Boolean) -> Unit,
    onTipModeChange: (Boolean) -> Unit,
    onTipTextChange: (String) -> Unit,
    onToggleDiscount: (Boolean) -> Unit,
    onDiscountTextChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ExtrasToggleRow(
            label = "Add Tax",
            enabled = taxEnabled,
            isPercent = taxIsPercent,
            text = taxText,
            showPercentToggle = true,
            prefix = if (taxIsPercent) "" else "$",
            suffix = if (taxIsPercent) "%" else "",
            onToggle = onToggleTax,
            onModeChange = onTaxModeChange,
            onTextChange = onTaxTextChange,
        )

        Spacer(Modifier.height(4.dp))

        ExtrasToggleRow(
            label = "Add Tip",
            enabled = tipEnabled,
            isPercent = tipIsPercent,
            text = tipText,
            showPercentToggle = true,
            prefix = if (tipIsPercent) "" else "$",
            suffix = if (tipIsPercent) "%" else "",
            onToggle = onToggleTip,
            onModeChange = onTipModeChange,
            onTextChange = onTipTextChange,
        )

        Spacer(Modifier.height(4.dp))

        ExtrasToggleRow(
            label = "Add Discount",
            enabled = discountEnabled,
            isPercent = false,
            text = discountText,
            showPercentToggle = false,
            prefix = "-$",
            suffix = "",
            onToggle = onToggleDiscount,
            onModeChange = {},
            onTextChange = onDiscountTextChange,
        )
    }
}

@Composable
private fun AssignmentCoverageBar(
    assignedAmount: String,
    subtotal: String,
    progress: Float,
    isComplete: Boolean,
) {
    val barColor by animateColorAsState(
        targetValue = if (isComplete) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondary,
        label = "assignmentCoverageColor"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Assigned so far",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$assignedAmount / $subtotal",
                style = MaterialTheme.typography.titleSmall,
                color = barColor,
                fontWeight = FontWeight.SemiBold,
            )
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

        if (!isComplete) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Assign every item before creating the expense.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalculationSummaryCard(
    subtotal: String,
    tax: String,
    tip: String,
    discount: String,
    total: String,
    enteredTotal: String,
    isTotalMatch: Boolean,
    totalDifference: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        SummaryRow(
            label = "Subtotal",
            value = subtotal,
            valueColor = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        SummaryRow(
            label = "Tax",
            value = tax,
            valueColor = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        SummaryRow(
            label = "Tip",
            value = tip,
            valueColor = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        SummaryRow(
            label = "Discount",
            value = discount,
            valueColor = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        Spacer(Modifier.height(12.dp))
        SummaryRow(
            label = "Calculated total",
            value = total,
            emphasized = true,
            valueColor = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        SummaryRow(
            label = "Entered total",
            value = enteredTotal,
            valueColor = if (isTotalMatch) MaterialTheme.colorScheme.onBackground else Color(0xFFDC2626),
        )

        if (!isTotalMatch) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Update the items or extras until the calculated total matches the entered total. Difference: $totalDifference",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFDC2626),
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            color = valueColor,
        )
    }
}

@Composable
private fun ExtrasToggleRow(
    label: String,
    enabled: Boolean,
    isPercent: Boolean,
    text: String,
    showPercentToggle: Boolean,
    prefix: String,
    suffix: String,
    onToggle: (Boolean) -> Unit,
    onModeChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
            ) {
                if (showPercentToggle) {
                    SegmentedToggle(
                        isPercent = isPercent,
                        onModeChange = onModeChange,
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (prefix.isNotEmpty()) {
                        Text(
                            text = prefix,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            fontFamily = DmSansFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.End
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (text.isEmpty()) {
                                    Text(
                                        text = "0.00",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (suffix.isNotEmpty()) {
                        Text(
                            text = suffix,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedToggle(
    isPercent: Boolean,
    onModeChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(
                    if (isPercent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
                .clickable { onModeChange(true) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPercent) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(
                    if (!isPercent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
                .clickable { onModeChange(false) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (!isPercent) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InitialAvatar(
    name: String,
    color: Color,
    size: Int = 24
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.first().uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2).sp
        )
    }
}

@Composable
private fun MemberAssignChip(
    member: AssignMember,
    isAssigned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isAssigned) member.color else MaterialTheme.colorScheme.surface
    val borderColor = if (isAssigned) member.color else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isAssigned) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isAssigned) Color.White else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun CoveringMemberRow(
    member: AssignMember,
    covererName: String?,
    isExpanded: Boolean,
    allMembers: List<AssignMember>,
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(member.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.first().uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (covererName != null) {
                    Text(
                        text = "Covered by $covererName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
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
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 60.dp, end = 16.dp, bottom = 12.dp)
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

@Composable
private fun OwedSummaryCard(
    members: List<AssignMember>,
    owedAmounts: List<AssignOwedAmount>,
    currencyCode: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = "Who owes what",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        owedAmounts.forEach { owed ->
            val member = members.firstOrNull { it.id == owed.memberId } ?: return@forEach
            val covererName = members.firstOrNull { it.id == owed.coveredByUserId }?.name

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(member.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.first().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (covererName != null) {
                        Text(
                            text = "Covered by $covererName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (covererName != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatAmount(owed.baseAmountCents, currencyCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                        Text(
                            text = formatAmount(0L, currencyCode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    Text(
                        text = formatAmount(owed.effectiveAmountCents, currencyCode),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
