@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("serverpackcreator.java-conventions")
    kotlin("jvm")
    // Coverage reporting for every Kotlin module: koverHtmlReport / koverXmlReport.
    id("org.jetbrains.kotlinx.kover")
}


dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation(kotlin("test"))
}

// One block for main and test compilation; they only ever held identical settings.
// The JVM target follows the toolchain that java-conventions already pins, so it is not repeated here.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

