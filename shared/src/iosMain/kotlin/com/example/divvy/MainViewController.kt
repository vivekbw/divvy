package com.example.divvy

import androidx.compose.ui.window.ComposeUIViewController
import com.example.divvy.ui.MainScreen
import com.example.divvy.ui.theme.DivvyTheme

fun MainViewController() = ComposeUIViewController {
    DivvyTheme {
        MainScreen()
    }
}
