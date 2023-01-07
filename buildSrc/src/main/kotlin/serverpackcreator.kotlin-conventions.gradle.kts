@file:Suppress("UnstableApiUsage")

import de.griefed.common.gradle.constant.JDK_VERSION
import de.griefed.common.gradle.constant.KOTLIN_VERSION
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
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

val embeddedMajorAndMinorKotlinVersion = project.getKotlinPluginVersion().substringBeforeLast(".")
if (KOTLIN_VERSION != embeddedMajorAndMinorKotlinVersion) {
    logger.warn("Constant 'KOTLIN_VERSION' ($KOTLIN_VERSION) differs from embedded Kotlin version in Gradle (${project.getKotlinPluginVersion()})!\n" +
            "Constant 'KOTLIN_VERSION' should be ($embeddedMajorAndMinorKotlinVersion).")
}


tasks.compileKotlin<KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    kotlinOptions {
        @Suppress("SpellCheckingInspection")
        val list = freeCompilerArgs.toMutableList()
        list.add("-Xjsr305=strict")
        freeCompilerArgs = list.toList()
        allWarningsAsErrors = false
        jvmTarget = JDK_VERSION
        languageVersion = KOTLIN_VERSION
        apiVersion = KOTLIN_VERSION
    }
}

tasks.compileTestKotlin<KotlinCompile> {
    logger.lifecycle("Configuring $name with version ${project.getKotlinPluginVersion()} in project ${project.name}")
    kotlinOptions {
        @Suppress("SpellCheckingInspection")
        val list = freeCompilerArgs.toMutableList()
        list.add("-Xjsr305=strict")
        freeCompilerArgs = list.toList()
        allWarningsAsErrors = false
        jvmTarget = JDK_VERSION
        languageVersion = KOTLIN_VERSION
        apiVersion = KOTLIN_VERSION
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(
            JavaLanguageVersion.of(JDK_VERSION)
        )
    }
}

