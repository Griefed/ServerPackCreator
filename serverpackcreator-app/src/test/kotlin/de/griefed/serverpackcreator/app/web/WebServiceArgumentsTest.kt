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
     * carries spring.data.mongodb.uri.
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
}
