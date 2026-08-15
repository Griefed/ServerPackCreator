
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

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

tasks.compileKotlin<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    compilerOptions {
        allWarningsAsErrors = false
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.compileTestKotlin<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    compilerOptions {
        allWarningsAsErrors = false
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
