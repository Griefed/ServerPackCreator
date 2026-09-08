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
 * Pins the **learned** half of the id-to-project bridge: what a downloaded jar says about itself.
 *
 * `KnownModIds` is a hand-written table plus two shape rules plus an optimistic slug guess, and its own doc
 * says to grow it only for ids observed going unresolved — which means a human notices a wasted boot, reads
 * a log, looks up two platforms and writes an entry. Every one of those entries is a fact the grinder
 * already had in its hands: it downloaded the project, and the jar's descriptor states the mod id.
 *
 * **The mapping is evidence, not a guess.** A jar staged under ref `R` that declares id `X` proves this
 * platform serves `X` at `R` — the same standard the rest of this module applies to verdicts. So a learned
 * mapping is an [ModIdMapping.Alias], with the refusal rights that carries, while an unlearned id stays a
 * [ModIdMapping.Guess].
 *
 * It compounds: `yet_another_config_lib_v3` is unmappable by spelling (both platforms publish YACL as
 * `yacl`), but the moment any candidate stages YACL through a platform ref, every later candidate declaring
 * that id resolves it for free.
 *
 * @author Griefed
 */
internal class LearnedModIdsTest {

    /** Whatever a jar's descriptor said about itself, staged under a ref we asked the platform for. */
    @Test
    fun anIdSeenInAStagedJarIsRememberedAgainstItsRef() {
        val learned = LearnedModIds()

        learned.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))

        Assertions.assertEquals("yacl", learned.refFor("yet_another_config_lib_v3", "Modrinth"))
    }

    /** A ref means nothing on the other platform: `1eAoo2KR` is not a CurseForge id. */
    @Test
    fun aRefIsOnlyValidOnThePlatformItCameFrom() {
        val learned = LearnedModIds()

        learned.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))

        Assertions.assertNull(learned.refFor("yet_another_config_lib_v3", "CurseForge"))
    }

    /** Ids are matched the way descriptors write them, case-insensitively. */
    @Test
    fun caseDoesNotDecideWhetherSomethingIsKnown() {
        val learned = LearnedModIds()

        learned.learn("CurseForge", "667299", setOf("Yet_Another_Config_Lib_v3"))

        Assertions.assertEquals("667299", learned.refFor("yet_another_config_lib_v3", "CurseForge"))
    }

    /** One jar answers to several ids — its own, plus everything it `provides`. */
    @Test
    fun everyIdAJarProvidesPointsAtTheSameProject() {
        val learned = LearnedModIds()

        learned.learn("Modrinth", "fabric-api", setOf("fabric-api", "fabric"))

        Assertions.assertEquals("fabric-api", learned.refFor("fabric", "Modrinth"))
        Assertions.assertEquals("fabric-api", learned.refFor("fabric-api", "Modrinth"))
    }

    /** Nothing learned is nothing claimed. */
    @Test
    fun anUnseenIdIsNotInvented() {
        Assertions.assertNull(LearnedModIds().refFor("whatever", "Modrinth"))
    }

    /**
     * **A learned id outranks the slug guess and carries an alias's weight**, because it rests on a
     * descriptor this process read rather than on a string that happened to look like a slug.
     */
    @Test
    fun aLearnedIdIsAnAliasRatherThanAGuess() {
        val learned = LearnedModIds()
        learned.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))

        Assertions.assertEquals(
            ModIdMapping.Alias("yacl"),
            learned.mappingFor("yet_another_config_lib_v3", "Modrinth") { KnownModIds.mappingFor(it, "Modrinth") }
        )
    }

    /** And an id it has never seen falls through to whatever the registry makes of it. */
    @Test
    fun anUnlearnedIdFallsThroughToTheRegistry() {
        val learned = LearnedModIds()

        Assertions.assertEquals(
            ModIdMapping.Alias("fabric-api"),
            learned.mappingFor("fabric", "Modrinth") { KnownModIds.mappingFor(it, "Modrinth") },
            "the table still answers for the ids it knows"
        )
        Assertions.assertEquals(
            ModIdMapping.Guess("mysterylib"),
            learned.mappingFor("mysterylib", "Modrinth") { KnownModIds.mappingFor(it, "Modrinth") },
            "and an unknown id is still only a guess"
        )
    }

    /**
     * **The first learner wins.** Two projects declaring one id is an upstream collision this cannot
     * adjudicate, and overwriting would make the answer depend on grind order — the same reason
     * `BundledJars.unambiguous` drops a contested version rather than picking one.
     */
    @Test
    fun aContestedIdKeepsTheFirstThingThatProvedIt() {
        val learned = LearnedModIds()

        learned.learn("Modrinth", "first-project", setOf("sharedlib"))
        learned.learn("Modrinth", "second-project", setOf("sharedlib"))

        Assertions.assertEquals("first-project", learned.refFor("sharedlib", "Modrinth"))
    }

    // --- carrying it across restarts ------------------------------------------------------------------

    /**
     * What was learned has to be expressible as plain data, or it cannot outlive the process. The shape is
     * deliberately `platform -> id -> ref`: nested, because a ref is meaningless on the other platform, and
     * a flat map keyed by a joined string would let that mistake through.
     */
    @Test
    fun whatWasLearnedSurvivesARoundTrip() {
        val original = LearnedModIds()
        original.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))
        original.learn("CurseForge", "667299", setOf("yet_another_config_lib_v3"))

        val restored = LearnedModIds()
        restored.restore(original.snapshot())

        Assertions.assertEquals("yacl", restored.refFor("yet_another_config_lib_v3", "Modrinth"))
        Assertions.assertEquals("667299", restored.refFor("yet_another_config_lib_v3", "CurseForge"))
    }

    /** An empty snapshot restores to an empty map rather than throwing at a daemon's startup. */
    @Test
    fun restoringNothingIsNotAFailure() {
        val learned = LearnedModIds()

        learned.restore(emptyMap())

        Assertions.assertNull(learned.refFor("anything", "Modrinth"))
    }

    /**
     * **Only a genuinely new pair is worth a write.** The hook is what a file-backed owner persists on, and
     * re-learning something already known happens constantly — every staged dependency re-states its own id
     * on every candidate that uses it.
     */
    @Test
    fun onlySomethingNewAnnouncesItself() {
        var announcements = 0
        val learned = LearnedModIds(onLearned = { announcements++ })

        learned.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))
        Assertions.assertEquals(1, announcements, "the first sighting is news")

        learned.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3"))
        Assertions.assertEquals(1, announcements, "the same pair again is not")

        learned.learn("Modrinth", "yacl", setOf("yet_another_config_lib_v3", "yacl_extra"))
        Assertions.assertEquals(2, announcements, "a new id alongside a known one is")
    }

    /** Restoring is not learning: loading a file at startup must not immediately ask to write it back. */
    @Test
    fun restoringDoesNotAnnounceAnything() {
        var announcements = 0
        val learned = LearnedModIds(onLearned = { announcements++ })

        learned.restore(mapOf("Modrinth" to mapOf("yet_another_config_lib_v3" to "yacl")))

        Assertions.assertEquals(0, announcements)
        Assertions.assertEquals("yacl", learned.refFor("yet_another_config_lib_v3", "Modrinth"))
    }
}
