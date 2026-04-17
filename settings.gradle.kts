rootProject.name = "Eva2z_Connect-Multiplatform"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()

        // ⭐ REQUIRED for JetBrains Compose components (MAPS)
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()

        // JetBrains Compose components (Maps, etc.)
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

        // PhonePe Intent SDK
        maven("https://phonepe.mycloudrepo.io/public/repositories/phonepe-intentsdk-android")

        // JitPack — for libraries not on Maven Central
        maven("https://jitpack.io")
    }
}

include(":composeApp")