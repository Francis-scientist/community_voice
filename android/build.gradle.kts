// Top-level build file. The plugins are declared here (apply false) and then
// switched on inside app/build.gradle.kts.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // The Compose compiler ships with Kotlin 2.x, so its version always
    // matches the Kotlin version above.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
