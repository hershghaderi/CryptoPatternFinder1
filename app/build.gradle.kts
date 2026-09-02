plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.cryptopatternfinder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cryptopatternfinder"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
