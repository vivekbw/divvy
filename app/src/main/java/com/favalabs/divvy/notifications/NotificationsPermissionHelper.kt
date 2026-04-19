package com.favalabs.divvy.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Requests the POST_NOTIFICATIONS runtime permission on Android 13+ (TIRAMISU).
 * On older APIs this is a no-op — the permission is granted at install time.
 *
 * Drop this composable once at the top of [com.favalabs.divvy.ui.MainScreen].
 * It fires the system dialog exactly once per install; subsequent calls are no-ops
 * because the launcher detects the permission is already resolved.
 *
 * We do not track the result — [NotificationHelper.areNotificationsEnabled] is checked
 * at [ExpenseNotificationService.start] time, and [NotificationHelper.post] catches the
 * [SecurityException] if the user revokes the permission later.
 */
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — handled gracefully at call sites */ }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}