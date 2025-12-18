import org.gradle.kotlin.dsl.implementation
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.1.0"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://phonepe.mycloudrepo.io/public/repositories/phonepe-intentsdk-android") // Add this

}

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
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Android-only lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Android-only navigation
            implementation(libs.androidx.navigation.compose)

            // Android-only Material3 (remove from commonMain)
            implementation(libs.androidx.material3)
            implementation(libs.androidx.material.icons.extended)

            // Ktor
            implementation(libs.ktor.client.okhttp)

            implementation(libs.play.services.maps)
            implementation(libs.maps.compose)

            // CameraX for Android
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)

            // ML Kit Barcode Scanning for Android
            implementation(libs.barcode.scanning)
//            implementation("com.phonepe.intent.sdk:android-sdk:2.1.0")
            // In your androidMain dependencies
            implementation("phonepe.intentsdk.android.release:IntentSDK:2.4.1")



        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)                  // Jetbrains MPP
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)       // Jetbrains MPP

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Ktor common
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.tab.navigator)

            // ViewModel for KMP (common)
            implementation(libs.lifecycle.viewmodel.compose.v296)
            implementation(libs.lifecycle.runtime.compose.v296)

            implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc01")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.0-rc01")

//            implementation(libs.components.map)
//            implementation(libs.maplibre)
//            implementation(libs.accompanist.pager)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

    }
}

android {
    namespace = "com.developer.ev2z"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.developer.ev2z"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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

