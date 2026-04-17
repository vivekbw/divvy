package com.divvy.divvy.ui.splitexpense.Views

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.divvy.divvy.components.GroupIcon
import com.divvy.divvy.models.Group
import com.divvy.divvy.models.SupportedCurrency
import com.divvy.divvy.models.formatAmount
import com.divvy.divvy.ui.creategroup.CreateGroupSheet
import com.divvy.divvy.ui.splitexpense.ViewModels.SplitExpenseViewModel
import com.divvy.divvy.ui.splitexpense.ViewModels.SplitMember
import com.divvy.divvy.ui.splitexpense.ViewModels.SplitMethod
import com.divvy.divvy.ui.theme.AvatarColors
import com.divvy.divvy.ui.theme.DmSansFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseScreen(
    viewModel: SplitExpenseViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToAssignItems: (groupId: String, amount: String, description: String, paidByUserId: String, currency: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateToSplitByPercentage: (groupId: String, amount: String, description: String, paidByUserId: String, currency: String) -> Unit = { _, _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SplitExpenseViewModel.SplitEvent.Created -> onBack()
                is SplitExpenseViewModel.SplitEvent.GoToAssignItems ->
                    onNavigateToAssignItems(event.groupId, event.amount, event.description, event.paidByUserId, event.currency)
                is SplitExpenseViewModel.SplitEvent.GoToSplitByPercentage ->
                    onNavigateToSplitByPercentage(event.groupId, event.amount, event.description, event.paidByUserId, event.currency)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add expense",
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
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                MembersSection(
                    members = uiState.members,
                    isLoading = uiState.isMembersLoading,
                )

                Spacer(Modifier.height(24.dp))

                AmountSection(
                    amount = uiState.amount,
                    onAmountChange = viewModel::onAmountChange,
                    currencyCode = uiState.currency,
                    onCurrencySelected = viewModel::onCurrencySelected
                )

                Spacer(Modifier.height(16.dp))

                DescriptionField(
                    description = uiState.description,
                    onDescriptionChange = viewModel::onDescriptionChange
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(24.dp))

                PaidBySplitRow(
                    paidByName = uiState.paidByName,
                    splitMethod = uiState.splitMethod,
                    members = uiState.members,
                    paidByUserId = uiState.paidByUserId,
                    onPaidBySelected = viewModel::onPaidBySelected,
                    onSplitMethodSelected = viewModel::onSplitMethodSelected,
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(24.dp))

                GroupSelectionSection(
                    groups = uiState.groups,
                    selectedGroupId = uiState.selectedGroupId,
                    onGroupSelected = viewModel::onGroupSelected,
                    onCreateGroup = viewModel::onShowCreateGroup
                )

                AnimatedVisibility(
                    visible = uiState.splitMethod == SplitMethod.Equally &&
                        uiState.members.isNotEmpty() &&
                        uiState.perPersonAmounts.isNotEmpty()
                ) {
                    Column {
                        Spacer(Modifier.height(24.dp))
                        SplitPreview(
                            members = uiState.members,
                            perPersonAmounts = uiState.perPersonAmounts,
                            currencyCode = uiState.currency,
                            coveredBy = uiState.coveredBy,
                            expandedCoveringMemberId = uiState.expandedCoveringMemberId,
                            onToggleCovering = viewModel::onToggleCoveringForMember,
                            onSetCovering = viewModel::onSetCovering,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            CreateSplitButton(
                enabled = uiState.canCreate,
                isCreating = uiState.isCreating,
                onClick = viewModel::onCreateSplit
            )
        }
    }

    if (uiState.showCreateGroup) {
        CreateGroupSheet(
            step = uiState.createStep,
            name = uiState.createName,
            selectedIcon = uiState.createIcon,
            profiles = uiState.allProfiles,
            profileSearchQuery = uiState.profileSearchQuery,
            selectedMemberIds = uiState.selectedMemberIds,
            isLoadingProfiles = uiState.isLoadingProfiles,
            isLoading = uiState.isCreatingGroup,
            errorMessage = uiState.createErrorMessage,
            onNameChange = viewModel::onCreateNameChange,
            onIconSelected = viewModel::onCreateIconSelected,
            onSearchChange = viewModel::onProfileSearchChange,
            onToggleMember = viewModel::onToggleMemberSelection,
            onBack = viewModel::onCreateBackStep,
            onNext = viewModel::onCreateNextStep,
            onCreate = viewModel::onConfirmCreateGroup,
            onDismiss = viewModel::onDismissCreateGroup
        )
    }
}

@Composable
private fun MembersSection(
    members: List<SplitMember>,
    isLoading: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "With you and:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-6).dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                members.filter { !it.isCurrentUser }.forEachIndexed { index, member ->
                    val color = AvatarColors[index % AvatarColors.size]
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.first().uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (members.size > 1) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${members.size - 1} ${if (members.size - 1 == 1) "person" else "people"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AmountSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    currencyCode: String = "USD",
    onCurrencySelected: (String) -> Unit = {}
) {
    Column {
        Text(
            text = "Amount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = SupportedCurrency.fromCode(currencyCode).symbol,
                fontFamily = DmSansFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(Modifier.width(4.dp))
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                textStyle = TextStyle(
                    fontFamily = DmSansFamily,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (amount.isEmpty()) {
                        Text(
                            text = "0.00",
                            fontFamily = DmSansFamily,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    innerTextField()
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        CurrencySelector(
            selectedCurrency = currencyCode,
            onCurrencySelected = onCurrencySelected
        )
    }
}

@Composable
private fun CurrencySelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        SupportedCurrency.entries.forEach { currency ->
            val isSelected = currency.code == selectedCurrency
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onCurrencySelected(currency.code) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${currency.symbol} ${currency.code}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    BasicTextField(
        value = description,
        onValueChange = onDescriptionChange,
        textStyle = TextStyle(
            fontFamily = DmSansFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (description.isEmpty()) {
                    Text(
                        text = "What's this for?",
                        fontFamily = DmSansFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun PaidBySplitRow(
    paidByName: String,
    splitMethod: SplitMethod,
    members: List<SplitMember>,
    paidByUserId: String,
    onPaidBySelected: (String) -> Unit,
    onSplitMethodSelected: (SplitMethod) -> Unit,
) {
    var showPaidByPicker by remember { mutableStateOf(false) }
    var showSplitPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Paid by",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showPaidByPicker = !showPaidByPicker }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = paidByName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Split",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showSplitPicker = !showSplitPicker }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = splitMethod.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    AnimatedVisibility(visible = showPaidByPicker) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            members.forEach { member ->
                val isSelected = member.id == paidByUserId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable {
                            onPaidBySelected(member.id)
                            showPaidByPicker = false
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(visible = showSplitPicker) {
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SplitMethod.entries.forEach { method ->
                SplitMethodChip(
                    method = method,
                    isSelected = method == splitMethod,
                    onClick = {
                        onSplitMethodSelected(method)
                        showSplitPicker = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SplitMethodChip(
    method: SplitMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = method.icon(),
                contentDescription = null,
                tint = if (isSelected) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = method.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = method.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GroupSelectionSection(
    groups: List<Group>,
    selectedGroupId: String?,
    onGroupSelected: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    Text(
        text = "Group",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEach { group ->
            GroupCard(
                group = group,
                isSelected = group.id == selectedGroupId,
                onClick = { onGroupSelected(group.id) }
            )
        }
        NewGroupCard(onClick = onCreateGroup)
    }
}

@Composable
private fun NewGroupCard(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "New group",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun GroupCard(group: Group, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            GroupIcon(
                icon = group.icon,
                tint = if (isSelected) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = group.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SplitPreview(
    members: List<SplitMember>,
    perPersonAmounts: Map<String, Long>,
    currencyCode: String,
    coveredBy: Map<String, String>,
    expandedCoveringMemberId: String?,
    onToggleCovering: (String) -> Unit,
    onSetCovering: (coveredUserId: String, covererUserId: String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = "Split preview",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        members.forEachIndexed { index, member ->
            val color = AvatarColors[index % AvatarColors.size]
            val coverer = coveredBy[member.id]
            val covererName = members.firstOrNull { it.id == coverer }?.name
            val isExpanded = expandedCoveringMemberId == member.id

            Column(modifier = Modifier.fillMaxWidth()) {
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
                            .background(color),
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
                    val coversCount = coveredBy.values.count { it == member.id }
                    val baseAmount = perPersonAmounts[member.id] ?: 0L
                    val displayAmount = if (coverer != null) {
                        baseAmount
                    } else if (coversCount > 0) {
                        val coveredIds = coveredBy.filter { it.value == member.id }.keys
                        baseAmount + coveredIds.sumOf { perPersonAmounts[it] ?: 0L }
                    } else {
                        baseAmount
                    }
                    Text(
                        text = formatAmount(displayAmount, currencyCode),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (coverer != null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (coverer != null) TextDecoration.LineThrough
                            else TextDecoration.None,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (coverer != null) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove covering",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { onSetCovering(member.id, null) }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CardGiftcard,
                            contentDescription = "Cover this share",
                            tint = if (isExpanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onToggleCovering(member.id) }
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.padding(start = 38.dp, top = 4.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = "Who's covering ${member.name}'s share?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            members.filter { it.id != member.id }.forEach { candidate ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { onSetCovering(member.id, candidate.id) }
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
    }
}

@Composable
private fun CreateSplitButton(
    enabled: Boolean,
    isCreating: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp, top = 8.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled && !isCreating) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )
            .clickable(enabled = enabled && !isCreating, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isCreating) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Add expense",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun SplitMethod.icon(): ImageVector = when (this) {
    SplitMethod.Equally -> Icons.Rounded.AttachMoney
    SplitMethod.ByPercentage -> Icons.Rounded.Percent
    SplitMethod.ByItems -> Icons.Rounded.Checklist
}
