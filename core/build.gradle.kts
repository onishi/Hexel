import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin module: hex geometry, gesture classification and placement.
// No Android dependencies so it can be unit-tested on the JVM (`./gradlew :core:test`).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}
