import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.installreferrer)
    implementation(libs.play.services.ads)
    implementation(libs.koin.android)
    implementation(libs.firebase.messaging.android)
    implementation(libs.firebase.crashlytics)
    // App Check: Play Integrity attests the Play build; the debug provider never ships in it.
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

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

/**
 * The upload key's credentials: `keystore.properties` at the project root, never committed, or the
 * `TURNIA_UPLOAD_*` environment variables on CI. See `keystore.properties.example`.
 */
val keystoreProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText.orNull?.let { load(it.reader()) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: providers.environmentVariable(env).orNull

android {
    namespace = "com.geoviksoft.turnia"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.geoviksoft.turnia"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // The release pipeline passes its own, so two uploads never share one; see .github/workflows/release.yml.
        versionCode = providers.gradleProperty("turnia.versionCode").orNull?.toInt() ?: 999
        versionName = providers.gradleProperty("turnia.versionName").orNull ?: "999 Debug"

        buildConfigField("String", "WEB_CLIENT_ID", "\"${webClientId()}\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        // Without a keystore the release build is still produced, unsigned: a machine that only
        // builds and tests does not need the upload key.
        val storeFile = signingValue("storeFile", "TURNIA_UPLOAD_STORE_FILE")
        if (storeFile != null) {
            fun required(key: String, env: String) = requireNotNull(signingValue(key, env)) {
                "Release signing: $key is missing from keystore.properties (or $env)"
            }
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                storePassword = required("storePassword", "TURNIA_UPLOAD_STORE_PASSWORD")
                keyAlias = required("keyAlias", "TURNIA_UPLOAD_KEY_ALIAS")
                keyPassword = required("keyPassword", "TURNIA_UPLOAD_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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
