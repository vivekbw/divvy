package com.favalabs.divvy.ui.friends

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.favalabs.divvy.components.GroupIcon
import com.favalabs.divvy.models.FriendBalance
import com.favalabs.divvy.models.Group
import com.favalabs.divvy.models.formatAmount
import com.favalabs.divvy.ui.theme.Amber
import com.favalabs.divvy.ui.theme.AvatarColors
import com.favalabs.divvy.ui.theme.Charcoal
import com.favalabs.divvy.ui.theme.NegativeRed
import com.favalabs.divvy.ui.theme.PositiveGreen
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = hiltViewModel(),
    onFriendClick: (String) -> Unit = {},
    onCreatedGroupNavigate: (String) -> Unit = {},
    onAddExpenseNavigate: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Refresh data each time the screen is resumed
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle navigation after group creation
    LaunchedEffect(uiState.createdGroupId) {
        uiState.createdGroupId?.let { groupId ->
            onCreatedGroupNavigate(groupId)
            viewModel.onCreatedGroupNavigationHandled()
        }
    }

    // Handle navigation for 1-on-1 expense
    LaunchedEffect(uiState.navigateToSplitWithGroupId) {
        uiState.navigateToSplitWithGroupId?.let { groupId ->
            onAddExpenseNavigate(groupId)
            viewModel.onNavigateToSplitHandled()
        }
    }

    // Contacts permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        val writeGranted = permissions[Manifest.permission.WRITE_CONTACTS] == true
        if (readGranted) viewModel.onContactsPermissionGranted()
        if (writeGranted) viewModel.onWriteContactsPermissionGranted()
    }

    LaunchedEffect(Unit) {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasReadPermission) viewModel.onContactsPermissionGranted()
        if (hasWritePermission) viewModel.onWriteContactsPermissionGranted()
        if (!hasReadPermission || !hasWritePermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                )
            )
        }
    }

    val friendsWithBalances = viewModel.filteredFriendsWithBalances()
    val settledUpFriends = viewModel.filteredSettledUpFriends()
    val filteredDeviceContacts = viewModel.filteredDeviceContacts()
    var settledUpExpanded by remember { mutableStateOf(false) }
    var notOnDivvyExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { viewModel.onShowAddContactSheet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add contact",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchChange(it) },
                    placeholder = { Text("Search friends...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                // Friends with outstanding balances
                if (friendsWithBalances.isNotEmpty()) {
                    items(friendsWithBalances, key = { it.selectionKey }) { friend ->
                        val index = uiState.friendBalances.indexOfFirst { it.userId == friend.userId }
                        FriendBalanceCard(
                            friend = friend,
                            cadBalance = uiState.friendCadBalances[friend.userId],
                            avatarColor = AvatarColors[abs(index) % AvatarColors.size],
                            isSelected = friend.selectionKey in uiState.selectedKeys,
                            onClick = {
                                if (uiState.selectedKeys.isNotEmpty()) {
                                    viewModel.onToggleSelection(friend.selectionKey)
                                } else {
                                    onFriendClick(friend.userId)
                                }
                            },
                            onLongClick = { viewModel.onToggleSelection(friend.selectionKey) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                // Settled-up friends (collapsible)
                if (settledUpFriends.isNotEmpty()) {
                    item {
                        CollapsibleSectionHeader(
                            title = "Settled Up",
                            count = settledUpFriends.size,
                            expanded = settledUpExpanded,
                            onToggle = { settledUpExpanded = !settledUpExpanded }
                        )
                    }
                    if (settledUpExpanded) {
                        items(settledUpFriends, key = { it.selectionKey }) { friend ->
                            val index = uiState.friendBalances.indexOfFirst { it.userId == friend.userId }
                            FriendBalanceCard(
                                friend = friend,
                                cadBalance = uiState.friendCadBalances[friend.userId],
                                avatarColor = AvatarColors[abs(index) % AvatarColors.size],
                                isSelected = friend.selectionKey in uiState.selectedKeys,
                                onClick = {
                                    if (uiState.selectedKeys.isNotEmpty()) {
                                        viewModel.onToggleSelection(friend.selectionKey)
                                    } else {
                                        onFriendClick(friend.userId)
                                    }
                                },
                                onLongClick = { viewModel.onToggleSelection(friend.selectionKey) }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                // Device contacts section (collapsible)
                if (filteredDeviceContacts.isNotEmpty()) {
                    item {
                        CollapsibleSectionHeader(
                            title = "Not on Divvy",
                            count = filteredDeviceContacts.size,
                            expanded = notOnDivvyExpanded,
                            onToggle = { notOnDivvyExpanded = !notOnDivvyExpanded }
                        )
                    }
                    if (notOnDivvyExpanded) {
                        items(filteredDeviceContacts, key = { it.selectionKey }) { contact ->
                            DeviceContactCard(
                                name = contact.name,
                                subtitle = contact.contactValue,
                                initials = contact.name.take(1).uppercase(),
                                onInvite = { viewModel.onInviteContact(context, contact) }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                // Empty state
                if (friendsWithBalances.isEmpty() && settledUpFriends.isEmpty() && filteredDeviceContacts.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) "No results found"
                                else "No friends yet. Join a group to see friends here!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }

            // Floating action bar when items selected
            if (uiState.selectedKeys.isNotEmpty()) {
                SelectionActionBar(
                    count = uiState.selectedKeys.size,
                    onAddToGroup = { viewModel.onShowAddToGroupSheet() },
                    onCreateGroup = { viewModel.onShowCreateGroupSheet() },
                    onClear = { viewModel.onClearSelection() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
        }
    }

    // Bottom sheets
    if (uiState.showAddContactSheet) {
        AddContactSheet(
            name = uiState.addContactName,
            phone = uiState.addContactPhone,
            email = uiState.addContactEmail,
            matchedProfile = uiState.addContactMatchedProfile,
            isAdding = uiState.isAddingContact,
            onNameChange = viewModel::onAddContactNameChange,
            onPhoneChange = viewModel::onAddContactPhoneChange,
            onEmailChange = viewModel::onAddContactEmailChange,
            onSave = viewModel::onAddContactSubmit,
            onDismiss = viewModel::onDismissAddContactSheet
        )
    }

    if (uiState.showActionSheet) {
        FriendsAddToGroupSheet(
            availableGroups = uiState.availableGroups,
            onGroupSelected = viewModel::onAddSelectedFriendsToGroup,
            onDismiss = viewModel::onDismissActionSheet
        )
    }

    if (uiState.showCreateGroupSheet) {
        FriendsCreateGroupSheet(
            name = uiState.createGroupName,
            selectedIcon = uiState.createGroupIcon,
            isCreating = uiState.isCreatingGroup,
            error = uiState.createGroupError,
            onNameChange = viewModel::onCreateGroupNameChange,
            onIconSelected = viewModel::onCreateGroupIconSelected,
            onSubmit = viewModel::submitCreateGroup,
            onDismiss = viewModel::onDismissCreateGroupSheet
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    onAddToGroup: () -> Unit,
    onCreateGroup: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Clear selection",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .clickable { onClear() }
        )
        Text(
            text = "$count selected",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onAddToGroup,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Add to Group", fontSize = 12.sp, maxLines = 1)
        }
        Button(
            onClick = onCreateGroup,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("New Group", fontSize = 12.sp, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendBalanceCard(
    friend: FriendBalance,
    cadBalance: Long?,
    avatarColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val surfaceColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface

    // Use CAD balance for badge and display
    val effectiveCadBalance = cadBalance ?: 0L
    val badgeIcon = when {
        friend.isSettledUp -> Icons.Rounded.CheckCircle
        effectiveCadBalance > 0 -> Icons.Rounded.ArrowDownward
        effectiveCadBalance < 0 -> Icons.Rounded.ArrowUpward
        else -> Icons.Rounded.CheckCircle
    }
    val badgeColor = when {
        friend.isSettledUp -> MaterialTheme.colorScheme.onSurfaceVariant
        effectiveCadBalance > 0 -> PositiveGreen
        effectiveCadBalance < 0 -> NegativeRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .padding(14.dp)
    ) {
        // Top row: Avatar + Name + Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                // Checkmark circle replacing avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // Avatar with status badge
                Box(modifier = Modifier.size(48.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = friend.initials.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                            .border(1.5.dp, surfaceColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Name
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            // Right side: CAD-converted overall amount
            if (friend.isSettledUp || effectiveCadBalance == 0L) {
                Text(
                    text = "settled up",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    val isPositive = effectiveCadBalance > 0
                    Text(
                        text = formatAmount(effectiveCadBalance, Group.BASE_CURRENCY),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) PositiveGreen else NegativeRed
                    )
                    Text(
                        text = if (isPositive) "you get back" else "you owe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Per-group breakdown (below the top row, aligned with name)
        if (friend.nonZeroGroupBalances.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Column(modifier = Modifier.padding(start = 6.dp)) {
                friend.nonZeroGroupBalances.forEach { gb ->
                    val isPositive = gb.balanceCents > 0
                    val groupIcon = GroupIcon.entries.find { it.name == gb.groupIcon } ?: GroupIcon.Group
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = groupIcon.imageVector,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Amber
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = gb.groupName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " · ${if (isPositive) "you get back" else "you owe"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatAmount(gb.balanceCents, gb.currency),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPositive) PositiveGreen else NegativeRed,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceContactCard(
    name: String,
    subtitle: String,
    initials: String,
    onInvite: () -> Unit
) {
    val avatarColor = MaterialTheme.colorScheme.primaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar (no badge)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.width(12.dp))

        // Name + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Invite icon button
        IconButton(
            onClick = onInvite,
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Amber,
                contentColor = Charcoal
            )
        ) {
            Icon(
                Icons.Rounded.PersonAdd,
                contentDescription = "Invite",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
