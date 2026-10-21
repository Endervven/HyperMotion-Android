plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.endervven.hypermotion"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.endervven.hypermotion"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
