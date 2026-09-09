import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
       namespace = "com.geoviksoft.turnia.core"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            api(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.functions)
            implementation(libs.firebase.analytics)
            api(libs.koin.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            api(project.dependencies.platform(libs.firebase.bom))
            // The Logger reports through it. GitLive wraps no Crashlytics, so this is the native
            // SDK and Android-only: the iOS Logger writes to NSLog and nothing else.
            implementation(libs.firebase.crashlytics)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
