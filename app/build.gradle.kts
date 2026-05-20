plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

// Set to true to enable Developer Mode (safe testing without affecting real data)
val isDeveloperMode = false

android {
    namespace = "com.example.attempt3"
    compileSdk = 36

    defaultConfig {
        applicationId = if (isDeveloperMode) "com.developer.habitly" else "com.habitly.habitly"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "2.5.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Dynamically set the app name based on the developer mode
        resValue("string", "app_name", if (isDeveloperMode) "Habitly [D]" else "Habitly")
        buildConfigField("boolean", "IS_DEVELOPER_MODE", isDeveloperMode.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.colorpicker.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose BOM — this controls all compose versions automatically
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Core Compose libraries
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.animation) // includes sharedElement

    // Extra icons, etc.
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation & other Jetpack libs
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Work Manager
    implementation(libs.androidx.work.runtime.ktx)

    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.material)
    ksp(libs.androidx.room.compiler)

    // GSON
    implementation(libs.gson)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json) // Add this line

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
