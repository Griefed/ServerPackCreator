@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("serverpackcreator.java-conventions")
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation(kotlin("test"))
}

tasks.compileKotlin<KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    compilerOptions {
        allWarningsAsErrors = false
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.compileTestKotlin<KotlinCompile> {
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

