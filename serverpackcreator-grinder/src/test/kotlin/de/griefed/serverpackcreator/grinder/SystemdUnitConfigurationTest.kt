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
package de.griefed.serverpackcreator.grinder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Keeps the shipped systemd unit in step with [GrinderApplication], the same way [ReadmeConfigurationTest]
 * keeps the README's table in step with it.
 *
 * The unit is the operator's starting point: it lists every knob, commented out, with the default it falls
 * back to. A variable added to the service and forgotten here is invisible to whoever deploys it, and a
 * default quoted here that the code no longer uses is worse than no comment at all — it reads as authoritative.
 * Neither is catchable by testing the Kotlin, so the two files are compared directly.
 */
internal class SystemdUnitConfigurationTest {

    private val unit = File("deploy/spc-grinder.service")

    /** Only the literal-default `env(...)` calls: those are the ones whose exact default the unit can quote. */
    private val envWithLiteralDefault = Regex("""env\("(SPC_GRINDER_[A-Z_]+)",\s*"([^"]*)"\)""")

    /** Every name handed to `env(...)`, whatever its default looks like. */
    private val envAnyName = Regex("""env\("(SPC_GRINDER_[A-Z_]+)"""")

    /** `System.getenv("NAME")` — the optional ones, with no default to state. */
    private val envWithoutDefault = Regex("""getenv\("([A-Z_]+)"\)""")

    /** Both the active `Environment=` lines and the commented-out ones — the unit documents by commenting. */
    private val declared = Regex("""^\s*#?\s*Environment=([A-Z_]+)=(.*)$""", RegexOption.MULTILINE)

    private fun source() = grinderEntryPoint.readText()

    @Test
    fun everyVariableTheServiceReadsAppearsInTheUnit() {
        Assertions.assertTrue(unit.isFile, "unit not found at ${unit.absolutePath}")
        val text = unit.readText()

        val names = buildSet {
            addAll(envAnyName.findAll(source()).map { it.groupValues[1] })
            addAll(
                envWithoutDefault.findAll(source()).map { it.groupValues[1] }
                    .filter { it.startsWith("SPC_GRINDER_") || it == "CURSEFORGE_API_KEY" }
            )
        }
        Assertions.assertTrue(names.isNotEmpty(), "no env(...) calls found — did the entry point change shape?")

        for (name in names) {
            Assertions.assertTrue(
                text.contains("Environment=$name="),
                "the systemd unit does not mention $name — an operator deploying from it cannot know it exists"
            )
        }
    }

    @Test
    fun theUnitDoesNotInventVariablesTheServiceIgnores() {
        val known = buildSet {
            addAll(envAnyName.findAll(source()).map { it.groupValues[1] })
            addAll(envWithoutDefault.findAll(source()).map { it.groupValues[1] })
        }
        val phantom = declared.findAll(unit.readText()).map { it.groupValues[1] }.toSet() - known
        Assertions.assertTrue(phantom.isEmpty(), "the systemd unit declares variables nothing reads: $phantom")
    }

    /**
     * Where the unit quotes a default it must be the real one. `SPC_GRINDER_HOME` is exempt and deliberately
     * set rather than commented: its code default follows the home of `User=`, and the unit pins it so that
     * changing the account cannot silently relocate the daemon's state.
     */
    @Test
    fun everyDefaultTheUnitQuotesIsTheRealOne() {
        val defaults = envWithLiteralDefault.findAll(source())
            .associate { it.groupValues[1] to it.groupValues[2] }
        Assertions.assertTrue(defaults.isNotEmpty(), "no literal-default env(...) calls found")

        val quoted = declared.findAll(unit.readText()).associate { it.groupValues[1] to it.groupValues[2].trim() }

        for ((name, real) in defaults) {
            val stated = quoted[name] ?: continue
            Assertions.assertEquals(real, stated, "the systemd unit states the wrong default for $name")
        }
    }

    /**
     * The unit must keep `WorkingDirectory=`. Without it systemd starts the service in `/`, which SPC took
     * for its home — the original `FileNotFoundException: /log4j2.xml` this service first died on.
     */
    @Test
    fun theUnitSetsAWorkingDirectory() {
        Assertions.assertTrue(
            Regex("""^\s*WorkingDirectory=\S+""", RegexOption.MULTILINE).containsMatchIn(unit.readText()),
            "the unit has no active WorkingDirectory= — systemd would start the daemon in /"
        )
    }
}
