package com.favalabs.divvy.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.favalabs.divvy.backend.SupabaseClientProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var syncTrigger: (() -> Unit)? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasOffline = !_isOnline.value
                _isOnline.value = true
                if (wasOffline) {
                    Timber.d("Network restored, triggering sync")
                    // Refresh the auth session token now that we're online
                    refreshAuthSession()
                    syncTrigger?.invoke()
                }
            }

            override fun onLost(network: Network) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
                val stillConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                if (!stillConnected) {
                    _isOnline.value = false
                    Timber.d("Network lost")
                }
            }
        })
    }

    fun registerSyncTrigger(trigger: () -> Unit) {
        syncTrigger = trigger
    }

    private fun refreshAuthSession() {
        if (!SupabaseClientProvider.isConfigured()) return
        scope.launch {
            try {
                SupabaseClientProvider.client.auth.refreshCurrentSession()
                Timber.d("Auth session refreshed after network restore")
            } catch (e: Exception) {
                Timber.w(e, "Failed to refresh auth session")
            }
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
