package com.divvy.divvy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import com.divvy.divvy.notifications.NotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.sentry.android.core.SentryAndroid
import io.sentry.SentryLevel
import com.divvy.divvy.security.SanitizedTimberTree
import timber.log.Timber
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun notificationHelper(): NotificationHelper
}

@HiltAndroidApp
class DivvyApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(SanitizedTimberTree(Timber.DebugTree()))
        }
        Timber.plant(SanitizedTimberTree(SentryTree()))

        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.environment = BuildConfig.BUILD_TYPE
            options.release    = "divvy@${BuildConfig.VERSION_NAME}"
            options.tracesSampleRate       = 1.0
            options.profilesSampleRate     = 1.0
            options.isSendDefaultPii = false
            options.maxBreadcrumbs = 50
            options.setDiagnosticLevel(SentryLevel.WARNING)
        }

        // Create notification channels once at startup (safe to call multiple times).
        EntryPointAccessors
            .fromApplication(this, AppEntryPoint::class.java)
            .notificationHelper()
            .createChannels()
    }
}
