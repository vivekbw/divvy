package com.example.divvy

import android.app.Application
import com.example.divvy.backend.SupabaseClientProvider
import com.example.divvy.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class DivvyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = DivvyConfig(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            authBypass = BuildConfig.AUTH_BYPASS
        )
        SupabaseClientProvider.initialize(config)

        startKoin {
            androidContext(this@DivvyApplication)
            modules(
                sharedModule,
                module {
                    single { config }
                }
            )
        }
    }
}
