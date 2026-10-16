package de.griefed.common.gradle

import org.gradle.api.Project

/**
 * ServerPackCreator's build version, made safe to put in a file name.
 *
 * Every archive this build produces is named `<module>-<version>.jar`, so `version` is a *file name
 * component* whether or not whoever passed `-Pversion` thought of it that way. Gradle does not enforce
 * that: a version containing `/` is resolved as a path, and the archive lands in a subdirectory whose
 * leaf name no longer carries the module. Two modules then collide under one name — which is how
 * `bootJar` came to refuse `BOOT-INF/lib/babel-monorepo.jar` as a duplicate, `-api` and `-clientside`
 * having both arrived there from `build/libs/<module>-renovate/babel-monorepo.jar`.
 *
 * This is the one place that decides it, for the same reason [JavaVersion] is: the alternative is the
 * sanitising living in each workflow that passes a ref-derived version, where the next one to be
 * written will not have it.
 */
object BuildVersion {

    /**
     * Everything NOT legal in an archive file name on every platform this project builds on, in runs.
     * Matching runs rather than single characters is what makes `a/ b` collapse to `a-b` instead of
     * `a--b`, so the artifact name stays predictable from the branch name. `+` and `_` are kept
     * deliberately: `+` is semver build metadata, and dropping it would merge two distinct builds.
     *
     * **This is a shared contract, not a private detail.** `.forgejo/actions/resolve-version` normalises
     * its own output with the same expression, so the version a workflow labels its artifacts with is the
     * version the build names them. The two cannot share code across bash and Kotlin, so
     * `BuildVersionTest` asserts the action still spells it identically.
     */
    const val ILLEGAL_IN_A_FILE_NAME_PATTERN = "[^A-Za-z0-9._+-]+"

    private val ILLEGAL_IN_A_FILE_NAME = Regex(ILLEGAL_IN_A_FILE_NAME_PATTERN)

    /** The separator illegal runs collapse to — the one this project already uses between name parts. */
    private const val SEPARATOR = "-"

    /**
     * [version] with every run of file-name-illegal characters replaced by a single `-`. A release
     * version passes through untouched: `resolve-version`'s `tag` mode already refuses anything that is
     * neither `X.Y.Z` nor `X.Y.Z-(alpha|beta).N`, so the published artifact names are unaffected.
     */
    fun sanitize(version: String): String = version.replace(ILLEGAL_IN_A_FILE_NAME, SEPARATOR)

    /**
     * [project]'s version, sanitised by [sanitize], announcing itself when it had to change anything.
     * The announcement is the point: a silently rewritten version is a build whose artifacts are not
     * named what the caller asked for, and the one line saying so is what makes that auditable from a
     * CI log instead of from the file listing.
     */
    fun of(project: Project): String {
        val requested = project.version.toString()
        val sanitized = sanitize(requested)
        if (sanitized != requested) {
            project.logger.lifecycle(
                "Version '$requested' is not a legal file name; ${project.name} builds as '$sanitized'."
            )
        }
        return sanitized
    }
}
