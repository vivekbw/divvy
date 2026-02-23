package com.example.divvy.ui.scanreceipt.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Purple = Color(0xFF7C4DFF)
private val Blue = Color(0xFF448AFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ScanReceiptScreen(
    onBack: () -> Unit,
    onScanComplete: (amount: String, description: String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Camera scanning will be available on iOS soon.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Brush.horizontalGradient(listOf(Purple, Blue)))
                    .clickable { onScanComplete("47.83", "Dinner at Maple Bistro") },
                contentAlignment = Alignment.Center
            ) {
                Text("Use Demo Data", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
