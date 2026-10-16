
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

    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.test {
    useJUnitPlatform()

    // `BuildVersionTest` reads `.forgejo/actions/resolve-version/action.yml`, which lives outside this
    // project and is therefore not an input Gradle knows about. Without this the task is UP-TO-DATE after
    // a change to the action and the guard never runs on the single edit it exists to catch -- measured:
    // mutating the action's expression left the suite green until this line existed.
    inputs.file(file("../.forgejo/actions/resolve-version/action.yml"))
        .withPropertyName("resolveVersionAction")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// THE ONE EDGE THAT MAKES buildSrc's TESTS RUN. The main build does not run buildSrc's lifecycle
// tasks, so `./gradlew build` reaches `check` in every module except this one; `:buildSrc:check`,
// `gradle.includedBuild("buildSrc")` and `-p buildSrc check` are all unavailable from the root. What
// is always built here is the jar, so the tests hang off that. Without this they are a guard nobody
// runs -- the failure mode this project has already paid for, with 30 tests gated on an environment
// variable no workflow set.
//
// `finalizedBy`, NOT `dependsOn`: `kotlin-dsl` puts this jar on the test compile classpath, so
// `dependsOn(tasks.test)` is a cycle (`compileTestKotlin` -> `jar` -> `test` -> `compileTestKotlin`)
// and fails every invocation outright, `./gradlew help` included. A finalizer runs after the jar it
// needs, and a failing one still fails the build, which is the whole requirement.
tasks.jar {
    finalizedBy(tasks.test)
}

// buildSrc is a SEPARATE build with its own toolchain and target, so these are not inherited from the
// root — but `buildSrc/settings.gradle.kts` points at the same catalog file, so they read the same
// entry. Keep them reading it: a bump that moved the modules and left build logic behind would compile
// the convention plugins against one JDK and the code they configure against another.
val javaVersion = libs.versions.java.get()

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion)
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(
            JavaLanguageVersion.of(javaVersion.toInt())
        )
    }
}
