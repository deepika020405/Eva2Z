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

        // ⭐ REQUIRED for JetBrains Compose components (MAPS)
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//        maven {
//            url = uri("https://phonepe.bintray.com/PhonePeIntentSDK")
//        }
    }
}

include(":composeApp")