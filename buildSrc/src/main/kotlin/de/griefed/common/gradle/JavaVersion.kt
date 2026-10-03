package de.griefed.common.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * ServerPackCreator's one Java version, read from `gradle/libs.versions.toml`.
 *
 * Exists because the three convention plugins that need it — the toolchain, the Kotlin `jvmTarget` and
 * Dokka's `jdkVersion` — cannot use the type-safe `libs` accessor. A precompiled script plugin that
 * references `libs` fails `:buildSrc:compilePluginsBlocks` with `Unresolved reference: libs`, so each of
 * them has to go through [VersionCatalogsExtension] instead. Three copies of that lookup is three places
 * for it to drift, which is the whole failure this centralisation exists to remove.
 *
 * The real build scripts — the root one and `buildSrc`'s own — use `libs.versions.java` directly and do
 * not need this.
 */
object JavaVersion {

    /** The catalog entry's name, so a missing one can be reported by the name a reader has to add. */
    private const val CATALOG_ENTRY = "java"

    /**
     * [project]'s Java version as declared in the catalog, e.g. `"21"`. Throws rather than defaulting:
     * a guessed toolchain compiles the whole project to a version nobody chose, and does it silently.
     */
    fun of(project: Project): String = project.extensions.getByType<VersionCatalogsExtension>()
        .named("libs").findVersion(CATALOG_ENTRY).orElseThrow {
            IllegalStateException("gradle/libs.versions.toml declares no `$CATALOG_ENTRY` version")
        }.requiredVersion

    /** The same value as an `Int`, for the APIs that want a language level rather than a string. */
    fun majorOf(project: Project): Int = of(project).toInt()
}
