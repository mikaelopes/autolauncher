plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mls.painel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mls.painel"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "1.15"
    }

    // Chave fixa: assim cada APK novo atualiza por cima do anterior sem desinstalar.
    signingConfigs {
        create("painel") {
            storeFile = file("painel.keystore")
            storePassword = "painel123"
            keyAlias = "painel"
            keyPassword = "painel123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("painel")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("painel")
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.maplibre.gl:android-sdk-opengl:13.5.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
