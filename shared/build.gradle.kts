import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:${libs.versions.composeMultiplatform.get()}")
            implementation("org.jetbrains.compose.foundation:foundation:${libs.versions.composeMultiplatform.get()}")
            implementation("org.jetbrains.compose.material:material:${libs.versions.composeMultiplatform.get()}")
            implementation("org.jetbrains.compose.material3:material3:1.10.0-alpha05")
            implementation("org.jetbrains.compose.ui:ui:${libs.versions.composeMultiplatform.get()}")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            implementation("io.insert-koin:koin-core:${libs.versions.koin.get()}")
            implementation("io.insert-koin:koin-compose:${libs.versions.koin.get()}")
            implementation("io.insert-koin:koin-compose-viewmodel:${libs.versions.koin.get()}")

            implementation("io.github.jan-tennert.supabase:postgrest-kt:${libs.versions.supabaseBom.get()}")
            implementation("io.github.jan-tennert.supabase:gotrue-kt:${libs.versions.supabaseBom.get()}")

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.coil.compose)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.example.divvy.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
