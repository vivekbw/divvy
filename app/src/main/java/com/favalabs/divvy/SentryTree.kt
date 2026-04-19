package com.favalabs.divvy

import android.util.Log
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import timber.log.Timber

/**
 * Timber tree that routes log output to Sentry:
 *  - DEBUG / INFO / WARN  → breadcrumbs (visible on the event detail page in Sentry)
 *  - ERROR + throwable    → captureException (creates a full Sentry issue)
 *  - ERROR without throwable → captureMessage
 *
 * Installed once in DivvyApplication; nothing else in the app needs to change.
 */
class SentryTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = priority.toSentryLevel()

        if (priority >= Log.ERROR && t != null) {
            Sentry.captureException(t)
            return
        }

        if (priority >= Log.ERROR) {
            Sentry.captureMessage(buildMessage(tag, message), level)
            return
        }

        // Lower priorities become breadcrumbs so they appear as context on the next event
        val crumb = Breadcrumb().apply {
            this.level   = level
            this.message = buildMessage(tag, message)
            this.category = tag ?: "app"
        }
        Sentry.addBreadcrumb(crumb)
    }

    private fun buildMessage(tag: String?, message: String): String =
        if (tag != null) "[$tag] $message" else message

    private fun Int.toSentryLevel(): SentryLevel = when (this) {
        Log.VERBOSE, Log.DEBUG -> SentryLevel.DEBUG
        Log.INFO               -> SentryLevel.INFO
        Log.WARN               -> SentryLevel.WARNING
        else                   -> SentryLevel.ERROR
    }
}
