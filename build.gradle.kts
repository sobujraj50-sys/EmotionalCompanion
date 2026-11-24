// Top-level build file. Module configuration lives in app/build.gradle.kts.
// AGP 9 has built-in Kotlin, so only the Compose compiler plugin is declared.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
