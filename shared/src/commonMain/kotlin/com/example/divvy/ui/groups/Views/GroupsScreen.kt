package com.example.divvy.ui.groups.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.divvy.components.GroupIcon
import com.example.divvy.models.Group
import com.example.divvy.ui.groups.ViewModels.GroupsViewModel
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C4DFF)
private val groupColors = listOf(Color(0xFFA5D6A7), Color(0xFF90CAF9), Color(0xFFCE93D8), Color(0xFFFFCC80), Color(0xFFEF9A9A))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(viewModel: GroupsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Manage Groups", fontWeight = FontWeight.SemiBold) },
                actions = {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Purple).clickable { }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add group", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(uiState.groups.size) { index ->
                val group = uiState.groups[index]
                val indicatorColor = groupColors[index % groupColors.size]
                ManageGroupCard(group = group, indicatorColor = indicatorColor, onClick = {})
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun ManageGroupCard(group: Group, indicatorColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(indicatorColor), contentAlignment = Alignment.Center) {
                GroupIcon(icon = group.icon, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${group.memberCount} members", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
