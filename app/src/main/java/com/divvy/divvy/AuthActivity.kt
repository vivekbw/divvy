package com.divvy.divvy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.divvy.divvy.backend.PushTokenRepository
import com.divvy.divvy.backend.SupabaseClientProvider
import com.divvy.divvy.ui.auth.Views.AuthNav
import com.divvy.divvy.ui.theme.DivvyTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.gotrue.handleDeeplinks
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    @Inject lateinit var pushTokenRepository: PushTokenRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FeatureFlags.AUTH_BYPASS) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        if (SupabaseClientProvider.isConfigured()) {
            SupabaseClientProvider.client.handleDeeplinks(intent)
        }
        setContent {
            DivvyTheme {
                AuthNav(
                    onAuthenticated = {
                        lifecycleScope.launch { pushTokenRepository.syncToken() }
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (SupabaseClientProvider.isConfigured()) {
            SupabaseClientProvider.client.handleDeeplinks(intent)
        }
    }
}
