package com.divvy.divvy.ui.groupdetail.Views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ModalBottomSheet
import com.divvy.divvy.components.GroupIcon
import com.divvy.divvy.models.ActivityItem
import com.divvy.divvy.models.SimplifiedPayment
import com.divvy.divvy.models.SupportedCurrency
import com.divvy.divvy.models.formatAmount
import com.divvy.divvy.ui.groupdetail.buildGroupInviteLink
import com.divvy.divvy.ui.groupdetail.ViewModels.GroupDetailViewModel
import com.divvy.divvy.ui.groupdetail.ViewModels.SettleMode
import com.divvy.divvy.ui.groupdetail.ViewModels.UserBalanceGroup
import com.divvy.divvy.ui.theme.AvatarColors
import com.divvy.divvy.ui.theme.DmSansFamily
import com.divvy.divvy.ui.theme.NegativeRed
import com.divvy.divvy.ui.theme.NegativeRedLight
import com.divvy.divvy.ui.theme.PositiveGreen
import com.divvy.divvy.components.GroupIcon as GroupIconComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    onLeaveGroup: () -> Unit,
    onAddExpense: () -> Unit,
    onViewMembers: () -> Unit,
) {
    val viewModel: GroupDetailViewModel = hiltViewModel<GroupDetailViewModel, GroupDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(groupId) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.group.name,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ManageGroupCard(
                        isEditing = uiState.isEditing,
                        editName = uiState.editName,
                        editIcon = uiState.editIcon,
                        isSavingEdit = uiState.isSavingEdit,
                        onStartEdit = viewModel::onStartEdit,
                        onCancelEdit = viewModel::onCancelEdit,
                        onEditNameChange = viewModel::onEditNameChange,
                        onEditIconSelected = viewModel::onEditIconSelected,
                        onSaveEdit = viewModel::onSaveEdit,
                        memberCount = uiState.currentMemberIds.size,
                        onViewMembers = onViewMembers
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    val netCad = uiState.netBalanceCad
                    BalanceSummaryCard(
                        balanceCents = netCad ?: uiState.group.balanceCents,
                        formattedBalance = if (netCad != null)
                            "${formatAmount(netCad, "CAD")}"
                        else
                            uiState.group.formattedBalance
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    SectionLabel("BALANCES")
                    Spacer(modifier = Modifier.height(10.dp))
                    // Toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            label = "Only yours",
                            selected = uiState.onlyMine,
                            onClick = viewModel::onToggleOnlyMine
                        )
                        FilterChip(
                            label = "All in CAD",
                            selected = uiState.convertToCad,
                            onClick = viewModel::onToggleConvertToCad
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // --- Individual Balances Section (Grouped by User) ---
                if (uiState.displayedBalances.isEmpty()) {
                    item {
                        Text(
                            text = "No balances to show.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                } else {
                    items(uiState.displayedBalances.size) { index ->
                        val userGroup = uiState.displayedBalances[index]
                        val showNet = !uiState.onlyMine
                        val expandedCurrency = if (!showNet && uiState.expandedMemberId == userGroup.userId) uiState.expandedCurrency else null

                        MemberBalanceGroupCard(
                            userGroup      = userGroup,
                            avatarColor    = AvatarColors[index % AvatarColors.size],
                            expandedCurrency = expandedCurrency,
                            showNetBalance = showNet,
                            settleMode     = uiState.settleMode,
                            settleAmount   = uiState.settleAmount,
                            isSettling     = uiState.isSettling,
                            onCurrencyClick = { currency -> viewModel.onMemberClick(userGroup.userId, currency) },
                            onModeSelect   = { mode, currency -> viewModel.onSettleModeSelected(mode, currency) },
                            onAmountChange = viewModel::onSettleAmountChange,
                            onConfirm      = { viewModel.onConfirmSettle() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // --- Simplified Payments Section ---
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionLabel("SUGGESTED REPAYMENTS")
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (uiState.displayedPayments.isEmpty()) {
                    item {
                        Text(
                            text = if (uiState.simplifiedPayments.isEmpty()) "All settled up!" else "No transactions match the current filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                } else {
                    items(uiState.displayedPayments.size) { index ->
                        val payment = uiState.displayedPayments[index]
                        val isExpanded = uiState.expandedMemberId == payment.fromUserId && uiState.expandedCurrency == payment.currency
                        SimplifiedPaymentCard(
                            payment        = payment,
                            isExpanded     = isExpanded,
                            settleMode     = uiState.settleMode,
                            settleAmount   = uiState.settleAmount,
                            isSettling     = uiState.isSettling,
                            onCardClick    = { viewModel.onMemberClick(payment.fromUserId, payment.currency) },
                            onModeSelect   = { mode -> viewModel.onSettleModeSelected(mode, payment.currency) },
                            onAmountChange = viewModel::onSettleAmountChange,
                            onConfirm      = { viewModel.onConfirmSettle() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionLabel("ACTIVITY")
                    Spacer(modifier = Modifier.height(10.dp))
                }
                items(uiState.activity) { item ->
                    ActivityCard(item = item)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onAddExpense() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add New Expense",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageGroupCard(
    isEditing: Boolean,
    editName: String,
    editIcon: GroupIcon,
    isSavingEdit: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onEditIconSelected: (GroupIcon) -> Unit,
    onSaveEdit: () -> Unit,
    memberCount: Int,
    onViewMembers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = if (isEditing) onCancelEdit else onStartEdit)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isEditing) "Cancel Edit" else "Edit Group",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        AnimatedVisibility(visible = isEditing) {
            Column(modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp)) {
                TextField(
                    value = editName,
                    onValueChange = onEditNameChange,
                    placeholder = { Text("Group name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    items(GroupIcon.entries) { icon ->
                        val isSelected = icon == editIcon
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(10.dp)
                                    )
                                    else Modifier
                                )
                                .clickable { onEditIconSelected(icon) },
                            contentAlignment = Alignment.Center
                        ) {
                            GroupIconComposable(
                                icon = icon,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (editName.isNotBlank() && !isSavingEdit)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                        .clickable(enabled = editName.isNotBlank() && !isSavingEdit) { onSaveEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSavingEdit) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save Changes", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewMembers)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$memberCount members",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageActionsSheet(
    groupId: String,
    groupName: String,
    isCreator: Boolean,
    onCopyLink: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val inviteUrl = buildGroupInviteLink(groupId, groupName)
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Group actions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Invite link",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Invite link",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { onCopyLink(inviteUrl) }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy link",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onLeaveGroup()
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Leave Group",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isCreator) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NegativeRedLight)
                        .clickable {
                            onDismiss()
                            onDeleteGroup()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = NegativeRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Delete Group",
                        style = MaterialTheme.typography.titleSmall,
                        color = NegativeRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceSummaryCard(balanceCents: Long, formattedBalance: String = "") {
    val isOwed = balanceCents >= 0
    val bgColor = if (isOwed) PositiveGreen else NegativeRed
    val label = if (isOwed) "You are owed" else "You owe"
    val amount = formattedBalance.ifEmpty { formatAmount(balanceCents, "USD") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp
    )
}

@Composable
private fun MemberBalanceGroupCard(
    userGroup: UserBalanceGroup,
    avatarColor: Color,
    expandedCurrency: String?,
    showNetBalance: Boolean,
    settleMode: SettleMode?,
    settleAmount: String,
    isSettling: Boolean,
    onCurrencyClick: (String) -> Unit,
    onModeSelect: (SettleMode, String) -> Unit,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userGroup.name.first().toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = userGroup.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }

        // Balances inside the User's Card
        userGroup.balances.forEachIndexed { index, memberBalance ->
            val isSettled = memberBalance.balanceCents == 0L
            val isPositive = memberBalance.balanceCents > 0

            val arrowLabel = if (showNetBalance) {
                when {
                    isSettled -> "settled up"
                    isPositive -> "gets back"
                    else -> "you owe"
                }
            } else {
                when {
                    isSettled -> "settled up"
                    isPositive -> "you get back"
                    else -> "you owe"
                }
            }

            val amountColor = when {
                isSettled -> MaterialTheme.colorScheme.onSurfaceVariant
                isPositive -> PositiveGreen
                else -> NegativeRed
            }
            val displayAmount = formatAmount(kotlin.math.abs(memberBalance.balanceCents), memberBalance.currency)
            val isExpanded = expandedCurrency == memberBalance.currency

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !showNetBalance) { onCurrencyClick(memberBalance.currency) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = arrowLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = displayAmount,
                        style = MaterialTheme.typography.titleSmall,
                        color = amountColor
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettleChip(
                                label = "Paid Fully",
                                selected = settleMode == SettleMode.Fully,
                                onClick = { onModeSelect(SettleMode.Fully, memberBalance.currency) }
                            )
                            SettleChip(
                                label = "Paid Partially",
                                selected = settleMode == SettleMode.Partially,
                                onClick = { onModeSelect(SettleMode.Partially, memberBalance.currency) }
                            )
                        }

                        AnimatedVisibility(visible = settleMode == SettleMode.Partially) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    BasicTextField(
                                        value = settleAmount,
                                        onValueChange = onAmountChange,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = TextStyle(
                                            fontFamily = DmSansFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        ),
                                        decorationBox = { innerTextField ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = SupportedCurrency.fromCode(memberBalance.currency).symbol,
                                                    fontFamily = DmSansFamily,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                innerTextField()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        val canConfirm = settleMode != null &&
                                (settleMode == SettleMode.Fully || settleAmount.toDoubleOrNull()?.let { it > 0 } == true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (canConfirm && !isSettling) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                                .clickable(enabled = canConfirm && !isSettling) { onConfirm() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSettling) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Confirm",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun SettleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SimplifiedPaymentCard(
    payment: SimplifiedPayment,
    isExpanded: Boolean,
    settleMode: SettleMode?,
    settleAmount: String,
    isSettling: Boolean,
    onCardClick: () -> Unit,
    onModeSelect: (SettleMode) -> Unit,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val currency = SupportedCurrency.fromCode(payment.currency)
    val amount = formatAmount(payment.amountCents, payment.currency)
    val fromLabel = if (payment.fromIsCurrentUser) "You" else payment.fromName
    val toLabel   = if (payment.toIsCurrentUser)   "You" else payment.toName
    val accentColor = when {
        payment.fromIsCurrentUser -> NegativeRed
        payment.toIsCurrentUser   -> PositiveGreen
        else                      -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fromLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (payment.fromIsCurrentUser) "pay" else "pays",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = toLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettleChip(
                        label = "Paid Fully",
                        selected = settleMode == SettleMode.Fully,
                        onClick = { onModeSelect(SettleMode.Fully) }
                    )
                    SettleChip(
                        label = "Paid Partially",
                        selected = settleMode == SettleMode.Partially,
                        onClick = { onModeSelect(SettleMode.Partially) }
                    )
                }
                AnimatedVisibility(visible = settleMode == SettleMode.Partially) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = settleAmount,
                                onValueChange = onAmountChange,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = TextStyle(
                                    fontFamily = DmSansFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                decorationBox = { innerTextField ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currency.symbol,
                                            fontFamily = DmSansFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val canConfirm = settleMode != null &&
                        (settleMode == SettleMode.Fully || settleAmount.toDoubleOrNull()?.let { it > 0 } == true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (canConfirm && !isSettling) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                        .clickable(enabled = canConfirm && !isSettling) { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSettling) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(item: ActivityItem) {
    val amount = formatAmount(item.amountCents, item.currency)
    val accentColor = if (item.paidByCurrentUser) PositiveGreen else NegativeRed
    val label = if (item.paidByCurrentUser) "you get back" else "you owe"
    val borderColor = if (item.isPending) Color(0xFFE65100) else MaterialTheme.colorScheme.outline
    val borderStyle = if (item.isPending) 2.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(borderStyle, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (item.isPending) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Pending sync",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFE65100)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (item.isPending) "${item.dateLabel} · Pending sync"
                       else "${item.dateLabel} · Paid by ${item.paidByLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isPending) Color(0xFFE65100)
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
