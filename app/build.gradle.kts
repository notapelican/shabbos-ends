plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.shabbosends"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.shabbosends"
        minSdk = 36          // Live Updates need Android 16
        targetSdk = 36
        versionCode = 8
        versionName = "1.7"
    }

    // A fixed key committed to the repo, so every build from GitHub Actions
    // installs over the previous one. Keep the repo private.
    signingConfigs {
        create("sideload") {
            storeFile = file("sideload.jks")
            storePassword = "shabbosends"
            keyAlias = "shabbos"
            keyPassword = "shabbosends"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.kosherjava:zmanim:2.5.0")
}
