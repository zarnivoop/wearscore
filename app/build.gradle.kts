plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.squashscore"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.squashscore"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Wear OS
    implementation("androidx.wear:wear:1.3.0")

    // Wear OS Compose
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear.compose:compose-navigation:1.4.0")

    // Compose
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.foundation:foundation:1.7.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Serialization (for JSON persistence)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Wearable Data Layer (phone companion sync)
    implementation("com.google.android.gms:play-services-wearable:19.0.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
}
