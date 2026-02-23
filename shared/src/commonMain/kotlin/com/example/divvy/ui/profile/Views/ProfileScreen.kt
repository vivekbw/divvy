package com.example.divvy.ui.profile.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvy.components.OutlineButton
import com.example.divvy.ui.auth.Views.AuthBackground
import com.example.divvy.ui.auth.Views.PurplePrimary
import com.example.divvy.ui.auth.Views.PurpleSecondary
import com.example.divvy.ui.profile.ViewModels.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = koinViewModel(), onBack: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    val displayEmail = profile?.email ?: uiState.email
    val displayPhone = profile?.phone ?: uiState.phone
    val phoneStatus = uiState.phoneVerified
    Scaffold(topBar = { TopAppBar(title = { Text("Profile", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }, containerColor = AuthBackground) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp), verticalArrangement = Arrangement.Top) {
            Text("Manage profile, linked accounts, and settings.")
            Spacer(Modifier.height(20.dp))
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(84.dp).clip(CircleShape).background(Brush.linearGradient(listOf(PurpleSecondary, PurplePrimary))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(listOfNotNull(profile?.firstName, profile?.lastName).joinToString(" ").ifBlank { "Unknown user" }, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(displayEmail ?: "No email", color = Color(0xFF6B7280), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("User ID", fontWeight = FontWeight.Medium); Text(profile?.id?.take(8)?.plus("...") ?: "—", color = Color(0xFF6B7280)) }
                    Spacer(Modifier.height(10.dp)); HorizontalDivider(); Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Auth method", fontWeight = FontWeight.Medium); Text(profile?.authMethod ?: "—", color = Color(0xFF6B7280)) }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Email", fontWeight = FontWeight.Medium); Text(displayEmail ?: "No email", color = Color(0xFF6B7280)) }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Phone", fontWeight = FontWeight.Medium); Text(when { displayPhone.isNullOrBlank() -> "No phone"; phoneStatus == true -> displayPhone; phoneStatus == false -> "$displayPhone (unverified)"; else -> displayPhone ?: "No phone" }, color = Color(0xFF6B7280)) }
                }
            }
            Spacer(Modifier.weight(1f))
            OutlineButton(label = if (uiState.isLoading) "Signing out..." else "Log out", modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), enabled = !uiState.isLoading) { viewModel.signOut { /* Platform handles auth navigation */ } }
            if (uiState.errorMessage != null) { Spacer(Modifier.height(10.dp)); Text(uiState.errorMessage ?: "", fontSize = 12.sp, textAlign = TextAlign.Center) }
        }
    }
}
