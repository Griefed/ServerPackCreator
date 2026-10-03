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
    testImplementation(kotlin("test"))
}

// The one Java version, from `gradle/libs.versions.toml`; see `JavaVersion` for why it is read
// this way rather than through the type-safe `libs` accessor.
val javaVersion: String = de.griefed.common.gradle.JavaVersion.of(project)

// One block for main and test compilation; they only ever held identical settings. The JVM target IS
// repeated here rather than inherited from java-conventions' toolchain — the comment that used to sit
// here said otherwise while the next line contradicted it — but both now read the same catalog entry,
// so there is one value and two uses of it rather than two values.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion)
    }
}

