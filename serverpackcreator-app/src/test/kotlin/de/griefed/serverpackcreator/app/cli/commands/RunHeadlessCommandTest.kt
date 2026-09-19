package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.serverpack.ServerPackGeneration
import de.griefed.serverpackcreator.api.serverpack.ServerPackHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Optional

/**
 * Pins that a headless generation reports whether it worked.
 *
 * Nothing did, before: every failure -- a config file that is not there, a check that did not pass, a
 * generation that errored -- was printed and then discarded, so `ServerPackCreator.run` had nothing to
 * turn into an exit code and a script wrapping the CLI could not tell a built server pack from a
 * broken one. `-config x.conf && deploy` deployed either way.
 */
internal class RunHeadlessCommandTest {

    /** A wrapper whose check and generation answer as told, so an outcome can be forced. */
    private fun apiAnswering(
        checkPasses: Boolean,
        generationSucceeds: Boolean,
        configsDirectory: File = File("does-not-matter")
    ): ApiWrapper {
        val check = mockk<ConfigCheck>()
        every { check.allChecksPassed } returns checkPasses
        every { check.encounteredErrors } returns mutableListOf("a config problem")

        val generation = mockk<ServerPackGeneration>()
        every { generation.success } returns generationSucceeds
        every { generation.errors } returns mutableListOf("a generation problem")
        every { generation.serverPack } returns File("server-pack")

        val configurationHandler = mockk<ConfigurationHandler>()
        // The real signature carries two further defaulted parameters, so the stub has to name them all:
        // Kotlin compiles defaults into the call site, and a two-argument matcher never matches.
        every {
            configurationHandler.checkConfiguration(any<File>(), any<PackConfig>(), any<ConfigCheck>(), any<Boolean>())
        } returns check

        val serverPackHandler = mockk<ServerPackHandler>()
        every { serverPackHandler.run(any()) } returns generation

        val apiProperties = mockk<ApiProperties>(relaxed = true)
        every { apiProperties.configsDirectory } returns configsDirectory

        val apiWrapper = mockk<ApiWrapper>()
        every { apiWrapper.configurationHandler } returns configurationHandler
        every { apiWrapper.serverPackHandler } returns serverPackHandler
        every { apiWrapper.apiProperties } returns apiProperties
        return apiWrapper
    }

    /** A config file that exists, so the run gets past the "is it there" gate. */
    private fun existingConfig(tempDir: File, name: String = "serverpackcreator.conf"): File {
        val config = File(tempDir, name)
        config.writeText("modpackDir = \"x\"")
        return config
    }

    /**
     * The case the README warns about: a mistyped path must not read as success.
     */
    @Test
    fun aConfigFileThatIsNotThereIsAFailure(@TempDir tempDir: File) {
        val command = RunHeadlessCommand(apiAnswering(checkPasses = true, generationSucceeds = true))

        val generated = command.runHeadless(File(tempDir, "missing.conf"))

        Assertions.assertFalse(generated, "a config file that does not exist cannot have generated anything")
    }

    /**
     * A configuration the checks reject produces no server pack, so it is not a success.
     */
    @Test
    fun aFailedConfigCheckIsAFailure(@TempDir tempDir: File) {
        val command = RunHeadlessCommand(apiAnswering(checkPasses = false, generationSucceeds = true))

        val generated = command.runHeadless(existingConfig(tempDir))

        Assertions.assertFalse(generated, "the checks did not pass, so nothing was generated")
    }

    /**
     * A generation that errored is not a success either, however good the configuration was.
     */
    @Test
    fun aFailedGenerationIsAFailure(@TempDir tempDir: File) {
        val command = RunHeadlessCommand(apiAnswering(checkPasses = true, generationSucceeds = false))

        val generated = command.runHeadless(existingConfig(tempDir))

        Assertions.assertFalse(generated, "the generation reported errors")
    }

    /**
     * And the case that must stay true, or every script breaks the other way.
     */
    @Test
    fun aGeneratedServerPackIsASuccess(@TempDir tempDir: File) {
        val command = RunHeadlessCommand(apiAnswering(checkPasses = true, generationSucceeds = true))

        val generated = command.runHeadless(existingConfig(tempDir))

        Assertions.assertTrue(generated, "the server pack was generated")
    }

    /**
     * One bad configuration among several must not be hidden by the ones that worked.
     */
    @Test
    fun withAllInConfigDirIsAFailureWhenAnyConfigFails(@TempDir tempDir: File) {
        val configs = File(tempDir, "configs")
        configs.mkdirs()
        existingConfig(configs, "good.conf")
        val api = apiAnswering(checkPasses = false, generationSucceeds = true, configsDirectory = configs)

        Assertions.assertFalse(
            RunHeadlessCommand(api).withAllInConfigDir(),
            "a config that failed must not be reported as a clean run"
        )
    }

    /**
     * A configs-directory that cannot be listed is a failure, not an empty success. It used to throw a
     * NullPointerException, because `listFiles()` returns null for a directory that is not there and the
     * loop iterated it unguarded.
     */
    @Test
    fun withAllInConfigDirIsAFailureWhenTheDirectoryCannotBeListed(@TempDir tempDir: File) {
        val absent = File(tempDir, "not-a-directory")
        val api = apiAnswering(checkPasses = true, generationSucceeds = true, configsDirectory = absent)

        Assertions.assertFalse(
            RunHeadlessCommand(api).withAllInConfigDir(),
            "a directory that cannot be listed must be reported, not thrown or ignored"
        )
    }

    /**
     * Every configuration generating is the one case that reports success.
     */
    @Test
    fun withAllInConfigDirIsASuccessWhenEveryConfigGenerates(@TempDir tempDir: File) {
        val configs = File(tempDir, "configs")
        configs.mkdirs()
        existingConfig(configs, "one.conf")
        existingConfig(configs, "two.conf")
        val api = apiAnswering(checkPasses = true, generationSucceeds = true, configsDirectory = configs)

        Assertions.assertTrue(RunHeadlessCommand(api).withAllInConfigDir(), "both configs generated")
    }

    /**
     * The destination override must still reach the configuration it is meant to redirect.
     */
    @Test
    fun theDestinationIsStillHandedToTheGeneration(@TempDir tempDir: File) {
        val destination = File(tempDir, "out")
        val command = RunHeadlessCommand(apiAnswering(checkPasses = true, generationSucceeds = true))

        val generated = command.runHeadless(existingConfig(tempDir), Optional.of(destination))

        Assertions.assertTrue(generated)
    }
}
