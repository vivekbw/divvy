package com.example.divvy.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.divvy.ui.navigation.AppNavHost

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    AppNavHost(navController)
}
