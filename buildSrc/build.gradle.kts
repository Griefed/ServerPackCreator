import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.io.*
import java.util.*

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
}

val props = Properties()
FileInputStream(file("../gradle.properties")).use {
    props.load(it)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${props.getProperty("kotlinVersion")}")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.8.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.7.7")
    implementation("org.panteleyev:jpackage-gradle-plugin:1.5.0")
    implementation("com.github.jk1:gradle-license-report:2.1")
}

tasks.compileKotlin<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    kotlinOptions {
        @Suppress("SpellCheckingInspection")
        val list = freeCompilerArgs.toMutableList()
        list.add("-Xjsr305=strict")
        freeCompilerArgs = list.toList()
        allWarningsAsErrors = false
        jvmTarget = props.getProperty("jdkVersion")
        languageVersion = props.getProperty("kotlinMajor")
        apiVersion = props.getProperty("kotlinMajor")
    }
}

tasks.compileTestKotlin<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    kotlinOptions {
        @Suppress("SpellCheckingInspection")
        val list = freeCompilerArgs.toMutableList()
        list.add("-Xjsr305=strict")
        freeCompilerArgs = list.toList()
        allWarningsAsErrors = false
        jvmTarget = props.getProperty("jdkVersion")
        languageVersion = props.getProperty("kotlinMajor")
        apiVersion = props.getProperty("kotlinMajor")
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(
            JavaLanguageVersion.of(props.getProperty("jdkVersion"))
        )
    }
}