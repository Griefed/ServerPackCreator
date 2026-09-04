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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins stage 3: **the sideness a mod's metadata declares is decided by rules too**, not by a `when` in
 * `aggregateFor` that no operator can reach.
 *
 * The platform's `server_side` and the jar's own descriptor are the two signals that decide a mod without
 * ever booting it, and they were the last hardcoded clientside determination left. They now match through
 * the same engine as the console rules, so the same file governs everything and any of it can be edited or
 * switched off.
 *
 * **Why one fact line rather than a stream per source.** Today's fold does not read the two signals
 * independently — its most careful branch reads them *together*, to notice that the platform marks the
 * server unsupported while the jar declares server/both. That is a contradiction, and it is the case where
 * confidence should drop rather than rise. A regex matches one line at a time, so facts spread across
 * separate lines could never express it; rendering all of them into a single canonical line makes
 * conjunction ordinary — a pattern naming two fields is an AND. This is the one place stage 3 departs from
 * the console rules' shape, and the reason is that dropping it would silently discard the caution the old
 * fold had.
 */
internal class MetadataRuleTest {

    /** The rules that decide from metadata, in file order. */
    private fun metadataRules() = DefaultBootRules.bundled().rules.filter { it.source == RuleSource.METADATA }

    private fun factsFor(serverSide: DeclaredSupport, jarScan: JarScan) =
        MetadataFacts.line(serverSide = serverSide, clientSide = DeclaredSupport.UNKNOWN, jarScan = jarScan)

    /**
     * The fact line is an interface an operator writes patterns against, so its field names are pinned. A
     * rename that silently stopped matching every metadata rule would otherwise look like "no mod is
     * clientside any more".
     */
    @Test
    fun theFactLineNamesItsFieldsStably() {
        val facts = MetadataFacts.line(
            serverSide = DeclaredSupport.UNSUPPORTED,
            clientSide = DeclaredSupport.REQUIRED,
            jarScan = JarScan.CLIENT
        )

        Assertions.assertTrue(facts.contains("platform_server=unsupported"), facts)
        Assertions.assertTrue(facts.contains("platform_client=required"), facts)
        Assertions.assertTrue(facts.contains("manifest=client"), facts)
    }

    /**
     * Modrinth declaring the server unsupported is a *declaration*, not a verdict. The mod is still booted
     * and the console still decides — see `ConsoleOutranksMetadataTest` for why a self-report may never
     * stand in for the evidence it is unreliable about.
     */
    @Test
    fun aPlatformDeclaringTheServerUnsupportedDeclaresClient() {
        val facts = factsFor(DeclaredSupport.UNSUPPORTED, JarScan.CLIENT)
        val fired = metadataRules().firstNotNullOfOrNull { rule ->
            rule.firstMatch(listOf(facts))?.let { rule }
        }

        Assertions.assertEquals(Declaration.CLIENT, fired?.declares, "matched: ${fired?.id}")
        Assertions.assertNull(fired?.verdict, "a declaration decides nothing on its own")
    }

    /** A jar whose own descriptor says client-only declares client; it does not thereby confirm. */
    @Test
    fun aJarDeclaringClientOnlyDeclaresClient() {
        val facts = factsFor(DeclaredSupport.UNKNOWN, JarScan.CLIENT)
        val fired = metadataRules().firstNotNullOfOrNull { rule ->
            rule.firstMatch(listOf(facts))?.let { rule }
        }

        Assertions.assertEquals(Declaration.CLIENT, fired?.declares, "matched: ${fired?.id}")
        Assertions.assertNull(fired?.verdict, "a declaration decides nothing on its own")
    }

    /**
     * **The contradiction, and the reason the fact line is one line.** The platform marks the server
     * unsupported while the jar declares server/both. The old fold noticed this and said so in the report
     * rather than treating it as clean evidence; losing that on the way into rules would make the engine
     * *more* confident than the code it replaced, which is the wrong direction for a redesign whose whole
     * premise is that the old verdicts were unreliable.
     */
    @Test
    fun aPlatformJarContradictionDoesNotConfirm() {
        val facts = factsFor(DeclaredSupport.UNSUPPORTED, JarScan.SERVER_OR_BOTH)
        val fired = metadataRules().firstNotNullOfOrNull { rule ->
            rule.firstMatch(listOf(facts))?.let { rule }
        }

        Assertions.assertNotNull(fired, "the contradiction must be recognised, not fall through silently")
        Assertions.assertEquals(
            Declaration.CONTRADICTORY, fired?.declares,
            "platform and jar disagree, so the mod declared nothing usable — matched: ${fired?.id}"
        )
    }

    /** A mod nothing declares anything about must not be confirmed by metadata; only a boot can speak. */
    @Test
    fun silentMetadataConfirmsNothing() {
        val facts = factsFor(DeclaredSupport.UNKNOWN, JarScan.SERVER_OR_BOTH)
        val confirming = metadataRules().filter { rule ->
            rule.firstMatch(listOf(facts)) != null && rule.verdict != null
        }

        Assertions.assertTrue(confirming.isEmpty(), "decided with no console: ${confirming.map { it.id }}")
    }

    /**
     * A distribution-locked file could be neither scanned nor booted, so nothing about it is known. It must
     * not confirm — this is the CurseForge `allowModDistribution=false` case, and it is common.
     */
    @Test
    fun aDeferredScanConfirmsNothing() {
        val facts = factsFor(DeclaredSupport.UNKNOWN, JarScan.DEFERRED)
        val confirming = metadataRules().filter { rule ->
            rule.firstMatch(listOf(facts)) != null && rule.verdict != null
        }

        Assertions.assertTrue(confirming.isEmpty(), "decided a mod nothing could read: ${confirming.map { it.id }}")
    }

    /** Console rules must never be evaluated against facts, nor metadata rules against a console. */
    @Test
    fun theTwoStreamsDoNotLeakIntoEachOther() {
        val consoleRules = DefaultBootRules.bundled().rules.filter { it.source == RuleSource.CONSOLE }

        Assertions.assertTrue(consoleRules.isNotEmpty(), "the console ladder is still there")
        Assertions.assertTrue(metadataRules().isNotEmpty(), "metadata rules exist")
        Assertions.assertTrue(
            consoleRules.none { it.id in metadataRules().map { metadata -> metadata.id } },
            "a rule belongs to exactly one stream"
        )
    }
}
