plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Read API_BASE_URL from gradle.properties (or from -PAPI_BASE_URL=... on the
// command line) and fall back to the emulator address.
val apiBaseUrl: String = (project.findProperty("API_BASE_URL") as String?)
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "http://10.0.2.2:8080/"

android {
    namespace = "com.example.communityvoice"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.communityvoice"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }

    buildFeatures {
        compose = true
        // Needed so BuildConfig.API_BASE_URL is generated.
        buildConfig = true
    }

    buildTypes {
        debug {
            // Plain http:// is allowed in debug only - see src/debug/AndroidManifest.xml
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // A real release build should point at an https:// URL.
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // The Compose "BOM" picks matching versions of every Compose library,
    // so the individual Compose dependencies below need no version number.
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Moves between the welcome, feed, detail, form, profile and insights
    // screens, and keeps the back stack right across configuration changes.
    implementation("androidx.navigation:navigation-compose:2.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Networking: Retrofit turns the Kotlin interface in CommunityVoiceApi.kt
    // into real HTTP calls; Gson converts JSON <-> data classes.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
