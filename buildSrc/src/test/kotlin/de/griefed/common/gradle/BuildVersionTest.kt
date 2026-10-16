package de.griefed.common.gradle

import org.junit.jupiter.api.Assumptions
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins [BuildVersion.sanitize], the predicate that keeps a branch-derived `-Pversion` from being
 * interpreted as a path. Every case below is a real value the build has been or can be handed, not a
 * constructed one: the three Renovate slugs are the branches whose Forgejo runs actually failed.
 */
class BuildVersionTest {

    /**
     * The defect this function exists for: a `/` in the version made Gradle write
     * `build/libs/serverpackcreator-api-renovate/babel-monorepo.jar`, a DIRECTORY plus a file, so
     * `-api` and `-clientside` both reached `BOOT-INF/lib/` as `babel-monorepo.jar` and `bootJar`
     * refused the duplicate entry.
     */
    @Test
    fun `a renovate branch name loses its path separator`() {
        assertEquals("renovate-babel-monorepo", BuildVersion.sanitize("renovate/babel-monorepo"))
        assertEquals("renovate-typescript-eslint-monorepo", BuildVersion.sanitize("renovate/typescript-eslint-monorepo"))
        assertEquals("renovate-pin-dependencies", BuildVersion.sanitize("renovate/pin-dependencies"))
    }

    /**
     * GitHub's `pull_request` event names the ref `<number>/merge`, so `.github/workflows/test.yml`
     * hits the identical defect on every pull request even though no branch is involved.
     */
    @Test
    fun `a github merge ref loses its path separator`() {
        assertEquals("713-merge", BuildVersion.sanitize("713/merge"))
    }

    /**
     * The release path must be untouched. `resolve-version`'s `tag` mode already refuses anything that
     * is neither `X.Y.Z` nor `X.Y.Z-(alpha|beta).N`, so these are the only two shapes a release build
     * ever passes, and a sanitiser that rewrote them would rename every published artifact.
     */
    @Test
    fun `a release version passes through untouched`() {
        assertEquals("9.3.0", BuildVersion.sanitize("9.3.0"))
        assertEquals("9.0.0-alpha.6", BuildVersion.sanitize("9.0.0-alpha.6"))
        assertEquals("dev", BuildVersion.sanitize("dev"))
    }

    /** `develop-a1b2c3d`, the shape `resolve-version`'s `branch` mode produces, is already legal. */
    @Test
    fun `a branch mode version passes through untouched`() {
        assertEquals("develop-a1b2c3d", BuildVersion.sanitize("develop-a1b2c3d"))
    }

    /**
     * A backslash separates paths on the Windows runner that builds the app image, so it has to go the
     * same way as `/` -- the defect is "this string becomes a path", not "this string contains a slash".
     */
    @Test
    fun `a backslash is a path separator too`() {
        assertEquals("feature-windows", BuildVersion.sanitize("feature\\windows"))
    }

    /**
     * A run of illegal characters collapses to ONE separator rather than one each, so `a//b` cannot
     * produce `a--b`. Without this a reader cannot predict the artifact name from the branch name.
     */
    @Test
    fun `a run of illegal characters collapses to one separator`() {
        assertEquals("feature-a-b", BuildVersion.sanitize("feature/ a : b"))
    }

    /**
     * Characters that are legal in a file name everywhere this project builds stay. `+` in particular
     * is semver build metadata, and dropping it would silently merge two different builds.
     */
    @Test
    fun `legal punctuation survives`() {
        assertEquals("1.2.3+build.4_final", BuildVersion.sanitize("1.2.3+build.4_final"))
    }

    /**
     * `.forgejo/actions/resolve-version` normalises the version it hands the workflows, because that
     * string labels release assets and installer media while [BuildVersion] names the jars inside them.
     * Bash and Kotlin cannot share the expression, so this asserts they still spell it the same -- the
     * duplicated-knowledge drift this repository has already paid for four times.
     *
     * Resolved against the buildSrc project directory, which is Gradle's working directory for this task
     * and always sits one level below the repository root.
     */
    @Test
    fun `the resolve-version action normalises with the same expression`() {
        val action = File("../.forgejo/actions/resolve-version/action.yml")
        assertTrue(action.isFile, "expected the action at ${action.absolutePath}")

        val expected = "s/${BuildVersion.ILLEGAL_IN_A_FILE_NAME_PATTERN}/-/g"
        assertTrue(
            action.readText().contains(expected),
            "resolve-version must normalise with `$expected`, or a workflow labels an artifact " +
                "something the build did not name it"
        )
    }

    /**
     * The check above compares two strings, which proves they were typed the same and nothing about what
     * either one does. This one takes the expression **out of the action** and runs it through `sed`,
     * asserting it answers what [BuildVersion.sanitize] answers for every case this class pins.
     *
     * SKIPS, never passes, where `sed` is not on the path -- a Windows developer running `./gradlew`,
     * which reaches this suite through buildSrc's jar. A check that cannot run must not read as one that
     * did. No Windows CI job here invokes Gradle, so this skips on no runner.
     */
    @Test
    fun `the resolve-version action's own expression agrees with sanitize`() {
        val program = sedProgramFromTheAction()
        Assumptions.assumeTrue(sedIsAvailable(), "sed is not on the path, so the action cannot be executed")

        for (version in VERSIONS_THIS_CLASS_PINS) {
            assertEquals(
                BuildVersion.sanitize(version),
                runSed(program, version),
                "the action and the build disagree about '$version'"
            )
        }
    }

    /** Every input asserted above, so the two implementations are compared on exactly the pinned cases. */
    private val VERSIONS_THIS_CLASS_PINS = listOf(
        "renovate/babel-monorepo", "renovate/typescript-eslint-monorepo", "renovate/pin-dependencies",
        "713/merge", "9.3.0", "9.0.0-alpha.6", "dev", "develop-a1b2c3d", "feature\\windows",
        "feature/ a : b", "1.2.3+build.4_final"
    )

    /** The `sed` program the action actually carries, so this executes the shipped expression, not a copy. */
    private fun sedProgramFromTheAction(): String {
        val action = File("../.forgejo/actions/resolve-version/action.yml")
        assertTrue(action.isFile, "expected the action at ${action.absolutePath}")
        val program = Regex("""sed -E '(.*?)'""").find(action.readText())?.groupValues?.get(1)
        assertTrue(program != null, "resolve-version carries no `sed -E '...'` expression to execute")
        return program!!
    }

    /** Whether `sed` can be executed at all, so its absence becomes a skip rather than an error. */
    private fun sedIsAvailable(): Boolean = try {
        ProcessBuilder("sed", "--version").redirectErrorStream(true).start().waitFor()
        true
    } catch (_: Exception) {
        false
    }

    /** [version] through `sed -E <program>`, exactly as the action pipes it. */
    private fun runSed(program: String, version: String): String {
        val process = ProcessBuilder("sed", "-E", program).redirectErrorStream(true).start()
        process.outputStream.use { it.write(version.toByteArray()) }
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output.trimEnd('\n')
    }
}
