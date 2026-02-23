package com.example.divvy.ui.creategroup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvy.components.GroupIcon
import com.example.divvy.components.GroupIcon as GroupIconComposable

private val Purple = Color(0xFF7C4DFF); private val Blue = Color(0xFF448AFF); private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupSheet(name: String, selectedIcon: GroupIcon, isLoading: Boolean, onNameChange: (String) -> Unit, onIconSelected: (GroupIcon) -> Unit, onCreate: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Create New Group", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Text("Group Name", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            TextField(value = name, onValueChange = onNameChange, placeholder = { Text("e.g., Summer Vacation", color = Color.Gray) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = LightGray, unfocusedContainerColor = LightGray, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent))
            Spacer(Modifier.height(24.dp))
            Text("Choose an Icon", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(GridCells.Fixed(6), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(GroupIcon.entries) { icon ->
                    val isSelected = icon == selectedIcon
                    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(if (isSelected) Purple.copy(alpha = 0.12f) else LightGray).then(if (isSelected) Modifier.border(2.dp, Purple, RoundedCornerShape(12.dp)) else Modifier).clickable { onIconSelected(icon) }, contentAlignment = Alignment.Center) {
                        GroupIconComposable(icon = icon, tint = if (isSelected) Purple else Color.Gray, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(50.dp)).background(if (name.isNotBlank() && !isLoading) Brush.horizontalGradient(listOf(Purple, Blue)) else Brush.horizontalGradient(listOf(Color.LightGray, Color.LightGray))).clickable(enabled = name.isNotBlank() && !isLoading) { onCreate() }, contentAlignment = Alignment.Center) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Create Group", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
