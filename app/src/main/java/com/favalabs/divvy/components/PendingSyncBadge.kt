package com.favalabs.divvy.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.favalabs.divvy.offline.OfflineSyncManager

@Composable
fun PendingSyncBanner(syncManager: OfflineSyncManager) {
    val count by syncManager.pendingCount.collectAsState(initial = 0)

    AnimatedVisibility(
        visible = count > 0,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3E0))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Pending sync",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$count change${if (count != 1) "s" else ""} pending sync",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
fun PendingSyncBadge(syncManager: OfflineSyncManager) {
    val count by syncManager.pendingCount.collectAsState(initial = 0)

    if (count > 0) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF3E0))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Pending sync",
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$count pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}
