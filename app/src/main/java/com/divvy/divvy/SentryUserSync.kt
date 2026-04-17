package com.divvy.divvy

import io.sentry.Sentry
import io.sentry.protocol.User

/**
 * Call [attach] after a successful sign-in so every Sentry event is tagged with
 * the user's ID. Call [detach] on sign-out.
 *
 * We only set `id` (an opaque UUID from Supabase) — never email, username, or any
 * financial data. This satisfies NFR 8 and keeps Sentry GDPR-clean.
 */
object SentryUserSync {

    fun attach(userId: String) {
        Sentry.setUser(User().apply { id = userId })
    }

    fun detach() {
        Sentry.setUser(null)
    }
}
