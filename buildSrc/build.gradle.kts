
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.io.FileInputStream
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
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.3.1")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.5")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.9.23")
    implementation("org.jetbrains.kotlin.plugin.jpa:org.jetbrains.kotlin.plugin.jpa.gradle.plugin:1.9.23")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
    implementation("com.github.jk1:gradle-license-report:2.5")
    implementation("org.siouan.frontend-jdk11:org.siouan.frontend-jdk11.gradle.plugin:6.0.0")
    implementation("gradle.plugin.install4j.install4j.buildtools:gradle_publish:10.0.7")
}

tasks.compileKotlin<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    kotlinOptions {
        allWarningsAsErrors = false
        jvmTarget = props.getProperty("jdkVersion")
        languageVersion = props.getProperty("kotlinMajor")
        apiVersion = props.getProperty("kotlinMajor")
    }
}

tasks.compileTestKotlin<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    kotlinOptions {
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
