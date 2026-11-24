plugins {
    // AGP 9 has built-in Kotlin support, so the separate kotlin-android plugin is not applied.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Signing key comes from GitHub Actions secrets (see .github/workflows/build.yml).
// Never commit a keystore or its password.
val ecKeystorePath: String? = System.getenv("EC_KEYSTORE_PATH")
val ecKeystorePassword: String? = System.getenv("EC_KEYSTORE_PASSWORD")
val hasEcKeystore = !ecKeystorePath.isNullOrBlank() && !ecKeystorePassword.isNullOrBlank()

android {
    namespace = "com.emotionalcompanion"
    // Compose 1.12 (BOM 2026.08.00) requires compileSdk 37.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.emotionalcompanion"
        minSdk = 26
        targetSdk = 36
        // CI passes -PversionCode=<run number> so every build installs as an update.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = "0.5.0-mE"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasEcKeystore) {
            create("ec") {
                storeFile = file(ecKeystorePath!!)
                storePassword = ecKeystorePassword
                keyAlias = "ec"
                keyPassword = ecKeystorePassword
            }
        }
    }

    buildTypes {
        release {
            // R8 is enabled in Milestone F together with keep rules for OkHttp/serialization.
            isMinifyEnabled = false
            if (hasEcKeystore) {
                signingConfig = signingConfigs.getByName("ec")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
