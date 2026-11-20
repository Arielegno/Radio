plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hilo.lector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hilo.lector"
        minSdk = 26
        targetSdk = 35
        versionCode = 28
        versionName = "2.5.1"
    }

    // Firma fija: así cada versión nueva se instala encima de la anterior sin borrar nada.
    signingConfigs {
        create("hilo") {
            storeFile = file("hilo.keystore")
            storePassword = "NSxpV_m8ZBZE2F5Fa4TF2YAQ"
            keyAlias = "hilo"
            keyPassword = "NSxpV_m8ZBZE2F5Fa4TF2YAQ"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("hilo")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("hilo")
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
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        textReport = true
        textOutput = file("build/reports/lint.txt")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Texto y anotaciones dentro del PDF
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // Reconocer texto en páginas escaneadas y fotos (funciona sin internet)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // Leer EPUB
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("com.github.junrar:junrar:7.5.5")

    testImplementation("junit:junit:4.13.2")
}
