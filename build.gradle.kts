// Top-level build file.
// AGP 9 has built-in Kotlin support and bundles KGP 2.2.10 by default.
// Per the AGP 9 release notes, a newer KGP is selected by adding it to the
// buildscript classpath. The Compose compiler plugin version must match it.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
