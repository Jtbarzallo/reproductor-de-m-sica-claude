plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val numero = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "ec.jat.reproductor"
    compileSdk = 35

    defaultConfig {
        applicationId = "ec.jat.reproductor"
        minSdk = 26
        targetSdk = 34
        versionCode = numero
        versionName = "1.0.$numero"
    }

    signingConfigs {
        create("jat") {
            storeFile = file("jat.keystore")
            storePassword = "jatdatos"
            keyAlias = "jat"
            keyPassword = "jatdatos"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("jat")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("jat")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
}
