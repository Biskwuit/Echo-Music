plugins {
    id("com.android.library")
}

android {
    namespace = "com.music.smb"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("com.hierynomus:smbj:0.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    coreLibraryDesugaring(libs.desugaring)
    testImplementation(libs.junit)
}