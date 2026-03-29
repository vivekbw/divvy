package com.example.divvy.di

import com.example.divvy.BuildConfig
import com.example.divvy.backend.SupabaseClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.ExternalAuthAction
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            httpEngine = OkHttp.create()
            defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
            install(Auth) {
                scheme = "com.example.divvy"
                host = "auth"
                defaultExternalAuthAction = ExternalAuthAction.CUSTOM_TABS
                alwaysAutoRefresh = false
            }
            install(Postgrest)
            install(Realtime)
        }
        SupabaseClientProvider.setClient(client)
        return client
    }
}
