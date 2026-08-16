
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

// Every plugin the convention plugins apply, put on this build's compile classpath so that a
// versionless `id("...")` in `src/main/kotlin/*.gradle.kts` resolves. Those precompiled script
// plugins CANNOT use `alias(libs.plugins.x)` — Gradle fails them with `Unresolved reference: libs`
// (verified) — so this is the only place the version can come from, and taking it from the catalog's
// [plugins] block keeps the id and the version declared exactly once.
//
// `pluginMarker` builds the artifact Gradle's plugin resolution would fetch: the marker POM
// `<id>:<id>.gradle.plugin:<version>`, which depends on the plugin's real implementation artifact.
fun Provider<PluginDependency>.marker(): Provider<String> =
    map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

dependencies {
    implementation(libs.plugins.kotlinJvm.marker())
    implementation(libs.plugins.kotlinAllOpen.marker())
    implementation(libs.plugins.kotlinJpa.marker())
    implementation(libs.plugins.springBoot.marker())
    implementation(libs.plugins.dokka.marker())
    implementation(libs.plugins.dokkaJavadoc.marker())
    implementation(libs.plugins.licenseReport.marker())
    implementation(libs.plugins.kover.marker())
    implementation(libs.plugins.frontend.marker())
    implementation(libs.plugins.install4j.marker())
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
