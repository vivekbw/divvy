package com.favalabs.divvy.backend

import io.github.jan.supabase.SupabaseClient

object SupabaseClientProvider {

    @Volatile
    private var clientInstance: SupabaseClient? = null

    val client: SupabaseClient
        get() = clientInstance
            ?: error("SupabaseClient not initialised — ensure NetworkModule is installed")

    fun setClient(client: SupabaseClient) {
        clientInstance = client
    }

    fun isInitialized(): Boolean = clientInstance != null

    fun isConfigured(): Boolean = clientInstance != null
}
