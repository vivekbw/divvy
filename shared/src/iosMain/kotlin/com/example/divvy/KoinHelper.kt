package com.example.divvy

import com.example.divvy.di.sharedModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin() {
    startKoin {
        modules(
            sharedModule,
            module {
                single { DivvyConfig() }
            }
        )
    }
}
