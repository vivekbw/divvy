package com.example.divvy.backend

import com.example.divvy.DivvyConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.plugins.HttpTimeout
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
    private var clientInstance: SupabaseClient? = null
    private var config: DivvyConfig = DivvyConfig()

    fun initialize(config: DivvyConfig) {
        this.config = config
    }

    @OptIn(SupabaseInternal::class)
    val client: SupabaseClient
        get() {
            clientInstance?.let { return it }
            val newClient = createSupabaseClient(
                supabaseUrl = config.supabaseUrl,
                supabaseKey = config.supabaseAnonKey
            ) {
                defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
                httpConfig {
                    install(HttpTimeout) {
                        requestTimeoutMillis = 15000
                        connectTimeoutMillis = 10000
                        socketTimeoutMillis = 15000
                    }
                }
                install(Auth) {
                    scheme = "com.example.divvy"
                    host = "auth"
                }
                install(Postgrest)
            }
            clientInstance = newClient
            return newClient
        }

    fun isInitialized(): Boolean = clientInstance != null

    fun isConfigured(): Boolean {
        return config.supabaseUrl.isNotBlank() && config.supabaseAnonKey.isNotBlank()
    }
}
