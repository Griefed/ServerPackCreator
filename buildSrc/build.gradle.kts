
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    gradlePluginPortal()
    google()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
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
