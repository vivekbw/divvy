package com.divvy.divvy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.divvy.divvy.notifications.ExpenseNotificationService
import com.divvy.divvy.offline.NetworkMonitor
import com.divvy.divvy.offline.OfflineSyncManager
import com.divvy.divvy.ui.MainScreen
import com.divvy.divvy.ui.theme.DivvyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var expenseNotificationService: ExpenseNotificationService
    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var syncManager: OfflineSyncManager

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DivvyTheme {
                MainScreen(networkMonitor, syncManager)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        expenseNotificationService.start()
    }

    override fun onStop() {
        super.onStop()
        expenseNotificationService.stop()
    }
}
