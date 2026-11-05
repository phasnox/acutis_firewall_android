import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// Release signing comes from keystore.properties (local) or the KEYSTORE_* / KEY_*
// environment variables (CI). Without it, release artifacts stay unsigned, which is
// fine for local verification but unacceptable for Play uploads.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(property: String, env: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(env)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "KEYSTORE_FILE")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }
val releaseStorePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "KEY_PASSWORD")
val releaseSigningSecrets = listOfNotNull(releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
val hasReleaseSigning = releaseStoreFile != null && releaseSigningSecrets.size == 3

// A CI job that loses one secret must not quietly publish an unsigned bundle.
if (releaseSigningSecrets.isNotEmpty() && !hasReleaseSigning) {
    throw GradleException(
        "Release signing is partially configured. Missing: " +
            (if (releaseStoreFile == null) "keystore file" else "store password, key alias or key password") +
            ". Set all of storeFile/storePassword/keyAlias/keyPassword in keystore.properties, " +
            "or KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD in the environment."
    )
}

android {
    namespace = "com.acutis.firewall"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.acutis.firewall"
        minSdk = 29
        targetSdk = 36
        versionCode = 10010
        versionName = "1.0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Several JUnit 5 jars reach the androidTest classpath transitively and
            // each ships these, which fails mergeDebugAndroidTestJavaResource. They
            // are not in the release APK, so excluding them is a no-op there.
            excludes += "/META-INF/{LICENSE.md,LICENSE-notice.md,NOTICE.md}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.1")
    ksp("com.google.dagger:hilt-compiler:2.59.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-service:2.6.2")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.8.4")
    testImplementation("com.google.truth:truth:1.1.5")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.59.1")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.59.1")
}
