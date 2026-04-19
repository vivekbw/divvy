package com.favalabs.divvy.ui.auth

import com.favalabs.divvy.models.ProfileRow

/**
 * Dummy user and profile used when auth bypass is enabled (AUTH_BYPASS in local.properties).
 * Provides a consistent fake account so the app can run without Supabase auth.
 */
object DummyAccount {
    const val USER_ID = "254e584d-13be-4a90-882d-19b403849921"

    val profile: ProfileRow = ProfileRow(
        id = USER_ID,
        firstName = "Demo",
        lastName = "User",
        createdAt = null,
        authMethod = "PHONE",
        email = "demo@example.com",
        phone = "+15550000000",
        phoneVerified = true
    )
}