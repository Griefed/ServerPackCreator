
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

// buildSrc is a separate build and cannot read the root settings' repositories, so it declares its own.
// Deliberately NOT mavenLocal(): it was first in this list, so any stale artifact in ~/.m2 silently
// shadowed the real one and the build stopped being reproducible between machines.
repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.kotlinAllOpen)
    implementation(libs.springGradlePlugin)
    implementation(libs.springDependencyMan)
    implementation(libs.kotlinJpa)
    implementation(libs.dokka)
    implementation(libs.dokkaJavaDoc)
    implementation(libs.licenseReport)
    implementation(libs.koverGradlePlugin)
    implementation(libs.frontendPlugin)
    implementation(libs.install4j)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(
            JavaLanguageVersion.of(21)
        )
    }
}
