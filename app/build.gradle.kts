plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "local.petlibrocompanion"
    compileSdk = 37

    defaultConfig {
        // Same id as the first (javac-built) PetBro, so this installs over it.
        applicationId = "local.petlibrocompanion"
        minSdk = 28
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        // The key the first PetBro was signed with, so updates install over it and keep the
        // saved sign-in. Kept out of git; without it, builds fall back to the usual debug key.
        val legacy = rootProject.file("petbro-debug.p12")
        if (legacy.exists()) {
            create("petbro") {
                storeFile = legacy
                storeType = "PKCS12"
                storePassword = "android"
                keyAlias = "debug"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        val key = signingConfigs.findByName("petbro") ?: signingConfigs.getByName("debug")
        debug {
            signingConfig = key
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideload-only.
            signingConfig = key
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
}
