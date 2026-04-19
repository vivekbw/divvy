package com.favalabs.divvy.ui.creategroup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.favalabs.divvy.components.GroupIcon
import com.favalabs.divvy.models.ProfileRow
import com.favalabs.divvy.ui.groups.Views.ProfilePickerList
import com.favalabs.divvy.ui.groups.ViewModels.CreateGroupStep
import com.favalabs.divvy.components.GroupIcon as GroupIconComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupSheet(
    step: CreateGroupStep,
    name: String,
    selectedIcon: GroupIcon,
    profiles: List<ProfileRow>,
    profileSearchQuery: String,
    selectedMemberIds: Set<String>,
    isLoadingProfiles: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onIconSelected: (GroupIcon) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val query = profileSearchQuery.trim().lowercase()
    val filteredProfiles = profiles.filter { profile ->
        if (query.isBlank()) return@filter true
        val fullName = "${profile.firstName.orEmpty()} ${profile.lastName.orEmpty()}".trim().lowercase()
        val phone = profile.phone.orEmpty().lowercase()
        fullName.contains(query) || phone.contains(query)
    }
    val selectedProfiles = profiles.filter { selectedMemberIds.contains(it.id) }

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
                text = "Create Group",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            StepIndicator(step = step)
            Spacer(modifier = Modifier.height(20.dp))

            when (step) {
                CreateGroupStep.Basics -> {
                    Text(
                        text = "Group Name",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = name,
                        onValueChange = onNameChange,
                        placeholder = {
                            Text("e.g., Summer Vacation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
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
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Choose an Icon",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(GroupIcon.entries) { icon ->
                            val isSelected = icon == selectedIcon
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(12.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onIconSelected(icon) },
                                contentAlignment = Alignment.Center
                            ) {
                                GroupIconComposable(
                                    icon = icon,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                CreateGroupStep.Members -> {
                    TextField(
                        value = profileSearchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text("Search by name or phone", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
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
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (selectedMemberIds.isEmpty()) "No members selected"
                        else "${selectedMemberIds.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoadingProfiles) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (filteredProfiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No users found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        ProfilePickerList(
                            profiles = filteredProfiles,
                            selectedIds = selectedMemberIds,
                            lockedIds = emptySet(),
                            isLoadingAction = isLoading,
                            onProfileClick = { onToggleMember(it.id) },
                            modifier = Modifier.height(320.dp)
                        )
                    }
                }

                CreateGroupStep.Review -> {
                    ReviewRow(label = "Group name", value = name.trim())
                    ReviewRow(label = "Icon", value = selectedIcon.name)
                    ReviewRow(label = "Members to invite", value = selectedMemberIds.size.toString())
                    if (selectedProfiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selected members",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyColumn(modifier = Modifier.height(220.dp)) {
                            items(selectedProfiles, key = { it.id }) { profile ->
                                val profileName = "${profile.firstName.orEmpty()} ${profile.lastName.orEmpty()}".trim().ifBlank { "Unknown" }
                                Text(
                                    text = profileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step != CreateGroupStep.Basics) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = !isLoading) { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Back",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
                val canProceed = when (step) {
                    CreateGroupStep.Basics -> name.isNotBlank()
                    CreateGroupStep.Members -> true
                    CreateGroupStep.Review -> name.isNotBlank()
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canProceed && !isLoading) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                        .clickable(enabled = canProceed && !isLoading) {
                            if (step == CreateGroupStep.Review) onCreate() else onNext()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (step == CreateGroupStep.Review) "Create Group" else "Continue",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: CreateGroupStep) {
    val steps = listOf("Basics", "Members", "Review")
    val selectedIndex = when (step) {
        CreateGroupStep.Basics -> 0
        CreateGroupStep.Members -> 1
        CreateGroupStep.Review -> 2
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (index <= selectedIndex) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index <= selectedIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
fun Preview_CreateGroupSheet() {
    CreateGroupSheet(
        step = CreateGroupStep.Basics,
        name = "Trip",
        selectedIcon = GroupIcon.Home,
        profiles = emptyList(),
        profileSearchQuery = "",
        selectedMemberIds = emptySet(),
        isLoadingProfiles = false,
        isLoading = false,
        errorMessage = null,
        onNameChange = {},
        onIconSelected = {},
        onSearchChange = {},
        onToggleMember = {},
        onBack = {},
        onNext = {},
        onCreate = {},
        onDismiss = {},
    )
}
