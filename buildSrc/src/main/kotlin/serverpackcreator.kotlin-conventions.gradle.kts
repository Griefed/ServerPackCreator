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
    // NO bare "org.jetbrains.kotlin:kotlin-bom"/"kotlin-stdlib" here. A precompiled script plugin cannot
    // read the version catalog (see .claude/rules/build-layout.md), so declaring them here meant declaring
    // them WITHOUT a version -- and Gradle published exactly that: a dependencyManagement BOM import with
    // no version and a runtime kotlin-stdlib with no version. Sonatype Central rejects such a POM, which
    // is what failed the 9.0.0-beta.2 release at :closeSonatypeStagingRepository.
    //
    // They were redundant anyway: `kotlin("jvm")` above adds a stdlib at the plugin's own version
    // (kotlin.stdlib.default.dependency is unset, so it defaults to true), and a module wanting it
    // explicitly uses libs.kotlinStdlib, which is versioned.
    testImplementation(kotlin("test"))
}

// One block for main and test compilation; they only ever held identical settings.
// The JVM target follows the toolchain that java-conventions already pins, so it is not repeated here.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

