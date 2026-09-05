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

import de.griefed.serverpackcreator.clientside.BootResult
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration

/**
 * Pins the operator configuration by **executing** it.
 *
 * This is what the extraction bought. The wiring these guards cover was previously asserted by grepping
 * `GrinderApplication.main`'s source text for `env("SPC_GRINDER_HOST", "127.0.0.1")` and friends — the only
 * guard available while every read lived inside one 294-line `main` that cannot run without Docker. A
 * string being present in a file is a much weaker claim than a value reaching the field it configures, and
 * a source scan also silently stops covering anything that moves out of the file it scans.
 */
internal class GrinderConfigurationTest {

    private fun config(vararg entries: Pair<String, String>) =
        GrinderConfiguration.from((mapOf(*entries))::get)

    @Test
    fun anEmptyEnvironmentYieldsTheDocumentedDefaults() {
        val config = config()

        Assertions.assertEquals("spc-grinder-runtime:latest", config.image)
        Assertions.assertEquals(8757, config.port)
        Assertions.assertEquals("127.0.0.1", config.host, "the report binds loopback unless told otherwise")
        Assertions.assertEquals(2, config.workers)
        Assertions.assertEquals(25, config.batch)
        Assertions.assertEquals(2.0, config.containerCpus)
        Assertions.assertEquals(3.0, config.containerMemoryGiB)
        Assertions.assertEquals(Duration.ofDays(30), config.reverifyTtl)
        Assertions.assertEquals(Duration.ofDays(7), config.cacheTtl)
        Assertions.assertEquals(Duration.ofSeconds(21_600), config.betweenSweeps)
        Assertions.assertEquals(Duration.ofSeconds(15), config.whileCrawling)
        Assertions.assertEquals(2048L * 1024 * 1024, config.bootLogBudgetBytes)
        Assertions.assertNull(config.ruleFallback, "an undecided rule is left to the ladder by default")
        Assertions.assertNull(config.containerUser, "derived from the work directory's owner when unset")
        Assertions.assertNull(config.curseForgeApiKey)
    }

    /**
     * The report bind address reaching the field that configures it — the join `ReportBindWiringTest` could
     * only assert against source text, because `main` boots Docker and cannot be executed in a test.
     */
    @Test
    fun theBindAddressAndPortReachTheirFields() {
        val config = config("SPC_GRINDER_HOST" to "10.0.0.7", "SPC_GRINDER_PORT" to "9001")

        Assertions.assertEquals("10.0.0.7", config.host)
        Assertions.assertEquals(9001, config.port)
    }

    /** Everything unset defaults to a path *under the home*, so relocating the home relocates all of it. */
    @Test
    fun everyPathDefaultsBeneathTheHome() {
        val config = config("SPC_GRINDER_HOME" to "/srv/grinder")

        Assertions.assertEquals(File("/srv/grinder/work"), config.work)
        Assertions.assertEquals(File("/srv/grinder/cache"), config.cache)
        Assertions.assertEquals(File("/srv/grinder/verdicts.json"), config.store)
        Assertions.assertEquals(File("/srv/grinder/requeue.json"), config.requeue)
        Assertions.assertEquals(File("/srv/grinder/cursors.json"), config.cursors)
        Assertions.assertEquals(File("/srv/grinder/boot-logs"), config.bootLogs)
        Assertions.assertEquals(File("/srv/grinder/boot-rules.json"), config.bootRules)
    }

    /** Each path is still individually overridable, which is what the unit file's commented lines offer. */
    @Test
    fun anyPathCanBeOverriddenIndividually() {
        val config = config("SPC_GRINDER_HOME" to "/srv/grinder", "SPC_GRINDER_STORE" to "/mnt/big/verdicts.json")

        Assertions.assertEquals(File("/mnt/big/verdicts.json"), config.store)
        Assertions.assertEquals(File("/srv/grinder/work"), config.work, "and the rest still follow the home")
    }

    @Test
    fun theRuleFallbackIsOptInAndCaseInsensitive() {
        Assertions.assertEquals(BootResult.INCONCLUSIVE, config("SPC_GRINDER_RULE_FALLBACK" to "inconclusive").ruleFallback)
        Assertions.assertEquals(BootResult.INCONCLUSIVE, config("SPC_GRINDER_RULE_FALLBACK" to "INCONCLUSIVE").ruleFallback)
        Assertions.assertNull(config("SPC_GRINDER_RULE_FALLBACK" to "grinder").ruleFallback)
    }

    /**
     * A malformed number falls back to its documented default rather than throwing.
     *
     * A typo in a unit file must not stop a service that has verdicts to serve, and the daemon logs what it
     * actually used, so the mistake is visible without being fatal.
     */
    @Test
    fun aMalformedNumberFallsBackToItsDefault() {
        val config = config(
            "SPC_GRINDER_PORT" to "eight-thousand",
            "SPC_GRINDER_WORKERS" to "",
            "SPC_GRINDER_CPUS" to "lots",
            "SPC_GRINDER_REVERIFY_TTL_DAYS" to "never"
        )

        Assertions.assertEquals(8757, config.port)
        Assertions.assertEquals(2, config.workers)
        Assertions.assertEquals(2.0, config.containerCpus)
        Assertions.assertEquals(Duration.ofDays(30), config.reverifyTtl)
    }

    /** A blank value is an unset value: `Environment=X=` in a unit file must not mean "the empty string". */
    @Test
    fun aBlankValueReadsAsUnset() {
        Assertions.assertEquals("127.0.0.1", config("SPC_GRINDER_HOST" to "   ").host)
        Assertions.assertNull(config("SPC_GRINDER_CONTAINER_USER" to "").containerUser)
    }

    /**
     * Every knob the configuration reads has to be in [GrinderConfiguration.KNOBS], because that list is
     * what the README and systemd-unit guards iterate. This is the one property still worth asserting
     * against the source: a knob read but not declared would be invisible to the documentation guards, and
     * only the source can say what is actually read.
     */
    @Test
    fun everyVariableReadIsDeclaredAsAKnob() {
        val source = File("src/main/kotlin/de/griefed/serverpackcreator/grinder/GrinderConfiguration.kt")
        Assertions.assertTrue(source.isFile, "configuration source not found at ${source.absolutePath}")

        // The alphabet of reader functions is explicit, and has to be: `Knob("SPC_GRINDER_HOME", ...)`
        // declares knobs in this same file, so a regex matching any call with a quoted name would match the
        // declarations too and this guard would assert nothing. Add a reader here when you add one there --
        // the range-checking readers below were added exactly that way, and this line is what caught them.
        val readers = "text|optional|under|number|intIn|longAtLeast|capAtLeastZero"
        val read = Regex("""(?:$readers)\("([A-Z_]+)"""").findAll(source.readText())
            .map { it.groupValues[1] }.toSet()
        val declared = GrinderConfiguration.KNOBS.map { it.name }.toSet() + "CURSEFORGE_API_KEY"

        Assertions.assertEquals(
            emptySet<String>(), read - declared,
            "read from the environment but not declared as a Knob, so no documentation guard covers it"
        )
        Assertions.assertEquals(
            emptySet<String>(), declared - read - setOf("CURSEFORGE_API_KEY"),
            "declared as a Knob but never actually read"
        )
    }
}
