import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
    implementation(libs.firebase.messaging.android)
    implementation(libs.firebase.crashlytics)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

/** `client_type` of the project's web OAuth client inside `google-services.json`. */
val WEB_OAUTH_CLIENT_TYPE = 3

/**
 * The web OAuth client id Google Sign-In takes as its `serverId`.
 */
fun webClientId(): String {
    val config = JsonSlurper().parse(file("google-services.json")) as Map<*, *>

    return (config["client"] as? List<*>).orEmpty()
        .mapNotNull { (it as? Map<*, *>)?.get("oauth_client") as? List<*> }
        .flatten()
        .mapNotNull { it as? Map<*, *> }
        .firstOrNull { it["client_type"] == WEB_OAUTH_CLIENT_TYPE }
        ?.get("client_id") as? String
        ?: error("google-services.json has no web OAuth client (client_type $WEB_OAUTH_CLIENT_TYPE)")
}

android {
    namespace = "com.geoviksoft.turnia"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.geoviksoft.turnia"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "WEB_CLIENT_ID", "\"${webClientId()}\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
