import org.gradle.kotlin.dsl.implementation
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

// Repositories are declared in settings.gradle.kts → dependencyResolutionManagement
// (module-level repositories {} is ignored when dependencyResolutionManagement is used)

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

//            linkerOpts("-framework", "Network")
//            linkerOpts(
//                "-framework", "Security",
//                "-framework", "Network"
//            )
            linkerOpts(
                "-framework", "UIKit",
                "-framework", "Foundation",
                "-framework", "Security",
                "-framework", "Network",
                "-framework", "Photos"
            )
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.material3)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.play.services.maps)
            implementation(libs.maps.compose)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.barcode.scanning)
            implementation(libs.intentsdk)
            implementation(libs.play.services.auth)
            implementation(libs.play.services.auth.api.phone)
            implementation(libs.play.services.location)
            implementation(libs.app.update.ktx)

            implementation("com.android.installreferrer:installreferrer:2.2")
            // ✅ FIREBASE ONLY - NO PLAY INTEGRITY
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.7.0"))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation("com.google.firebase:firebase-messaging:24.0.0")
            implementation("com.facebook.android:facebook-android-sdk:17.0.2")

            // WebRTC SDK — Stream's pre-built WebRTC for Android (Maven Central)
            implementation("io.getstream:stream-webrtc-android:1.3.3")

            // Socket.IO client — WebRTC signaling channel
            implementation("io.socket:socket.io-client:2.1.0") {
                exclude(group = "com.squareup.okhttp3", module = "okhttp")
            }



            // ❌ REMOVED: Play Integrity API (causes Play Protect warnings)

            // implementation(libs.integrity)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.lifecycle.viewmodel.compose.v296)
            implementation(libs.lifecycle.runtime.compose.v296)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// ✅ LOAD KEYSTORE PROPERTIES (SAFE WAY)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

require(keystorePropertiesFile.exists()) {
    "❌ keystore.properties file not found at project root"
}

keystorePropertiesFile.inputStream().use {
    keystoreProperties.load(it)
}

// 🔍 TEMP DEBUG (remove after successful build)
println("KEYSTORE_FILE = ${keystoreProperties.getProperty("storeFile")}")
println("KEY_ALIAS = ${keystoreProperties.getProperty("keyAlias")}")


android {
    namespace = "com.developer.ev2z"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.developer.ev2z"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 110
        versionName = "5.7.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

//        create("release") {
//            if (keystorePropertiesFile.exists()) {
//                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
//                storePassword = keystoreProperties["storePassword"] as String
//                keyAlias = keystoreProperties["keyAlias"] as String
//                keyPassword = keystoreProperties["keyPassword"] as String
//            } else {
//                // Fallback to debug
//                println("⚠️ keystore.properties not found, using debug keystore")
//                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
//                storePassword = "android"
//                keyAlias = "androiddebugkey"
//                keyPassword = "android"
//            }
//        }


        create("release") {
            val keystorePath = rootProject.layout.projectDirectory
                .file("keystores/evatozkey.jks")
                .asFile

            storeFile = keystorePath
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }



    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "kotlin/**"
            excludes += "**/*.proto"
            excludes += "**/*.txt"
            excludes += "**/*.xml"
            excludes += "okhttp3/**"
            excludes += "META-INF/versions/**"
            excludes += "META-INF/services/**"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // ── ABI splits — debug APK only ships arm64-v8a (modern phones) ──
    // Cuts debug APK from ~60 MB → ~20 MB. Release uses AAB so splits
    // are handled by Google Play automatically.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")          // covers 95%+ of current Android devices
            isUniversalApk = false
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}