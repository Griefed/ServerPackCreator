/* Copyright (C) 2026 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.app.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for [WebService.springArguments], the composition of the argument array handed to Spring Boot.
 * Pure argument handling, so no Spring context is booted — which is the point of extracting it.
 *
 * The container starts ServerPackCreator as `-web --home /app/serverpackcreator`, so anything this
 * function drops is silently lost configuration; that is what these assertions guard.
 */
internal class WebServiceArgumentsTest {

    /** Stand-in for the real `--spring.config.location=…` argument, whose content is irrelevant here. */
    private val configLocation = "--spring.config.location=optional:file:./overrides.properties"

    /**
     * The docker entrypoint's exact invocation: every argument must survive, because the last of them
     * is the value of `--home` and losing it changes which directory ServerPackCreator runs out of.
     */
    @Test
    fun theContainersArgumentsAllSurvive() {
        val arguments = WebService.springArguments(
            arrayOf("-web", "--home", "/app/serverpackcreator"), configLocation
        )

        Assertions.assertTrue(
            arguments.contains("/app/serverpackcreator"),
            "The --home value was dropped from the arguments handed to Spring: ${arguments.contentToString()}"
        )
        Assertions.assertTrue(arguments.contains("-web"), "The -web argument was dropped")
        Assertions.assertTrue(arguments.contains("--home"), "The --home argument was dropped")
    }

    /**
     * The config-location argument must always be present, whatever else was passed — without it Spring
     * reads none of ServerPackCreator's property-files, including the docker overrides.properties that
     * carries spring.mongodb.uri.
     */
    @Test
    fun theConfigLocationArgumentIsAlwaysAppended() {
        Assertions.assertTrue(
            WebService.springArguments(arrayOf("-web"), configLocation).contains(configLocation)
        )
        Assertions.assertTrue(
            WebService.springArguments(arrayOf(), configLocation).contains(configLocation)
        )
    }

    /**
     * With no arguments of its own, the array is exactly the config-location argument.
     */
    @Test
    fun anEmptyArgumentArrayYieldsOnlyTheConfigLocation() {
        Assertions.assertArrayEquals(
            arrayOf(configLocation), WebService.springArguments(arrayOf(), configLocation)
        )
    }

    /**
     * A single argument must not be cannibalised either — the one-element case is where overwriting
     * "the last argument" and appending look identical in length but differ in content.
     */
    @Test
    fun aSingleArgumentIsKeptAlongsideTheConfigLocation() {
        val arguments = WebService.springArguments(arrayOf("-web"), configLocation)

        Assertions.assertEquals(2, arguments.size, "Expected the argument and the config-location")
        Assertions.assertEquals("-web", arguments[0])
        Assertions.assertEquals(configLocation, arguments[1])
    }

    /**
     * The `--spring.config.location` chain, pinned in full and **in order**.
     *
     * Order is the whole point: later locations win, so the two `overrides.properties` entries must
     * stay last. That is the file the docker image's `init-spc-config` script composes
     * `SPC_DATABASE_*` into, so it is where `spring.mongodb.uri` comes from in a container. If it
     * stopped being last, a value from an earlier file would beat it; if it dropped out entirely, the
     * URI would never be read at all — and per this module's landmine, a missing URI is not a degraded
     * connection but a silent fall-back to Spring Boot's own `mongodb://localhost/test`.
     */
    @Test
    fun theConfigLocationChainListsAllEightLocationsInOrder(@TempDir tempDir: File) {
        val propertiesFile = File(tempDir, "home/serverpackcreator.properties")
        val overridesFile = File(tempDir, "home/overrides.properties")
        val userHome = File(tempDir, "user")

        val argument = WebService.configLocationArgument(propertiesFile, overridesFile, userHome)

        Assertions.assertTrue(
            argument.startsWith("--spring.config.location="),
            "The argument must be the Spring config-location flag: $argument"
        )
        Assertions.assertEquals(
            listOf(
                "classpath:/application.properties",
                "classpath:/serverpackcreator.properties",
                "optional:file:${propertiesFile.absolutePath}",
                "optional:file:${File(userHome, "serverpackcreator.properties").absolutePath}",
                "optional:file:./serverpackcreator.properties",
                "optional:file:${overridesFile.absolutePath}",
                "optional:file:${File(userHome, "overrides.properties").absolutePath}",
                "optional:file:./overrides.properties"
            ),
            argument.removePrefix("--spring.config.location=").split(","),
            "The config-location chain changed. Order matters — later locations win."
        )
    }

    /**
     * The classpath defaults must never be `optional:`. They ship inside the jar, so a missing one is
     * a broken build rather than a deployment choice, and Spring should say so loudly.
     */
    @Test
    fun theClasspathDefaultsAreNotOptional(@TempDir tempDir: File) {
        val locations = WebService.configLocationArgument(
            File(tempDir, "a.properties"), File(tempDir, "b.properties"), tempDir
        ).removePrefix("--spring.config.location=").split(",")

        Assertions.assertTrue(
            locations.filter { it.startsWith("classpath:") }.none { it.startsWith("optional:") },
            "A classpath default became optional: $locations"
        )
        Assertions.assertEquals(
            2, locations.count { it.startsWith("classpath:") },
            "Expected exactly the two shipped classpath property-files"
        )
    }

    /**
     * The overrides file wins over ServerPackCreator's own properties file, which is the entire reason
     * the docker deployment works: the container writes its database settings into overrides.
     */
    @Test
    fun theOverridesFileIsReadAfterThePropertiesFile(@TempDir tempDir: File) {
        val propertiesFile = File(tempDir, "serverpackcreator.properties")
        val overridesFile = File(tempDir, "overrides.properties")

        val locations = WebService.configLocationArgument(propertiesFile, overridesFile, tempDir)
            .removePrefix("--spring.config.location=").split(",")

        Assertions.assertTrue(
            locations.indexOf("optional:file:${overridesFile.absolutePath}") >
                    locations.indexOf("optional:file:${propertiesFile.absolutePath}"),
            "overrides.properties must be read after serverpackcreator.properties: $locations"
        )
        Assertions.assertEquals(
            "optional:file:./overrides.properties", locations.last(),
            "The working-directory overrides file must have the last word"
        )
    }
}
