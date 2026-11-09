plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.arielegno.radio"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.arielegno.radio"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.9"
        // Voces propias (sherpa-onnx): solo para los procesadores de teléfonos (así el APK pesa menos)
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    signingConfigs {
        // Firma fija para que cada APK nuevo se instale encima del anterior sin borrar tu música.
        // Es solo para instalarla tú mismo; para la Play Store se usa otra firma.
        getByName("debug") {
            storeFile = file("firma-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
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
    lint {
        // La revisión de estilo no detiene la compilación del APK.
        abortOnError = false
        checkReleaseBuilds = false
    }
    // La Revista nativa: el mismo código de revista-kmp/ (se copia antes de compilar)
    sourceSets["main"].java.srcDirs(layout.buildDirectory.dir("generated/revista"), "src/revista/kotlin")
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // Las librerías del motor de voz van comprimidas: el APK baja de ~60 MB a ~30 MB
        jniLibs.useLegacyPackaging = true
    }
}

// Copia la Revista nativa (revista-kmp/) quitando las líneas «expect»: aquí las da Plataforma.android.kt
val revistaNativa = tasks.register<Sync>("revistaNativa") {
    from("../../revista-kmp/composeApp/src/commonMain/kotlin")
    into(layout.buildDirectory.dir("generated/revista"))
    filter { linea: String -> if (Regex("^(internal )?expect ").containsMatchIn(linea)) "" else linea }
}
tasks.named("preBuild") { dependsOn(revistaNativa) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.guava:guava:33.3.1-android")

    implementation("androidx.media3:media3-exoplayer:1.5.1")

    // Voces propias sin internet: motor sherpa-onnx (se baja en la compilación a libs/) y descompresor de las voces
    implementation(files("libs/sherpa-onnx.aar"))
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("androidx.media3:media3-session:1.5.1")

    testImplementation("junit:junit:4.13.2")
}
