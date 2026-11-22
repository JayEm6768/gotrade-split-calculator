import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

kotlin { jvmToolchain(17) }

/** Release signing is optional: `keystore.properties` in the project root points at a private properties file. */
fun loadSigningProperties(): Properties? {
    val pointer = rootProject.file("keystore.properties").takeIf { it.exists() } ?: return null
    val target = Properties().apply { pointer.reader().use(::load) }.getProperty("path") ?: return null
    val file = File(target).takeIf { it.exists() } ?: return null
    return Properties().apply { file.reader().use(::load) }
}

val signingProps = loadSigningProperties()

android {
    namespace = "com.splitcalc.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.splitcalc.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (signingProps != null) {
            create("release") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    lint {
        warningsAsErrors = false
        abortOnError = true
        error += setOf(
            "SecureRandom",
            "TrustAllX509TrustManager",
            "ExportedReceiver",
            "ExportedService",
            "ExportedContentProvider",
            "SetJavaScriptEnabled",
            "AllowBackup",
            "HardcodedDebugMode",
            "UnsafeImplicitIntentLaunch",
            "MutableImplicitPendingIntent",
        )
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(kotlin("test"))
}

tasks.withType<Test> { useJUnitPlatform() }
