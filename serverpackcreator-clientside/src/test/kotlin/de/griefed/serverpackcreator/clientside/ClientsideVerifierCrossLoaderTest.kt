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
 * Pins the cross-loader reconciliation: a crash on one loader cannot stand as clientside evidence when
 * **another loader of the same project booted a server with the same list-entry**.
 *
 * Measured live on 2026-08-23 — the report this exists for. `iron-chests` produced two verdicts in one run:
 *
 * ```
 * Forge    | HIGH | ironchest- | Declared server/both but the server crashed …
 *          |      |            | Forge 48.1.0 / Minecraft 1.20.2 → CRASHED (exit 1)
 * NeoForge | LOW  | ironchest- | NeoForge 21.11.45 / Minecraft 1.21.11 → SURVIVED (exit 137)
 * ```
 *
 * The disproof was already in hand and was thrown away. It is decisive because the *published* artefact is a
 * loader-agnostic file-name stem matched with `startsWith`: publishing `ironchest-` off the Forge crash
 * strips the NeoForge build — the one that had just booted a real server — out of every pack built against
 * the list. Hence the entry, not the loader, is what has to collide for the disproof to count.
 */
internal class ClientsideVerifierCrossLoaderTest {

    private fun verdict(
        loader: String,
        entry: String?,
        bootResult: BootResult?,
        verdict: Verdict = Verdict.CLEAR,
        bootedLoader: String? = loader
    ) = LoaderVerdict(
        loader = loader,
        suggestedEntry = entry,
        declaredClientSide = DeclaredSupport.UNKNOWN,
        declaredServerSide = DeclaredSupport.UNKNOWN,
        jarScan = JarScan.SERVER_OR_BOTH,
        bootResult = bootResult,
        bootedLoader = bootedLoader,
        bootCrashExcerpt = null,
        verdict = verdict,
        sampleFile = null,
        note = null
    )

    /** The `iron-chests` pair, verbatim: same entry, one crashed, one booted. The booted one wins. */
    @Test
    fun aLoaderThatBootedTheSameEntryDisprovesTheCrash() {
        val forge = verdict("Forge", "ironchest-", BootResult.CRASHED, Verdict.CONFIRMED)
        val neoForge = verdict("NeoForge", "ironchest-", BootResult.SURVIVED)

        val disproving = ClientsideVerifier.loaderDisprovingTheCrash(forge, listOf(forge, neoForge))

        Assertions.assertEquals("NeoForge", disproving?.loader)
    }

    /**
     * **Only a loader's *own* clean boot may disprove another loader's crash.**
     *
     * Since the other-version crash re-check began spanning loaders, a verdict's [LoaderVerdict.bootResult]
     * can be the result of a boot run under a *different* loader — `reconcileOtherVersionRecheck` returns the
     * surviving attempt's own outcome, and that attempt may be a cross-loader one. Letting such a SURVIVED
     * disprove a third loader's crash breaks the invariant this reconciliation is built on: the entries are
     * compared so that the published stem cannot strip a build proven to boot, but the build that actually
     * booted belongs to a loader whose stem may be different.
     *
     * The shape below is the one `FilenameStemDeriver.deriveStem` documents — a project shipping
     * `embeddium-` for Forge/NeoForge and `sodium-fabric-` for Fabric. NeoForge's SURVIVED came from a Fabric
     * boot of `sodium-fabric-…jar`, which `embeddium-` would never strip, so publishing the Forge crash
     * endangers nothing and the disproof is unfounded. It would also print "NeoForge booted a server", which
     * NeoForge did not do.
     */
    @Test
    fun aSurvivalBorrowedFromAnotherLoaderDisprovesNothing() {
        val forge = verdict("Forge", "embeddium-", BootResult.CRASHED, Verdict.CONFIRMED)
        val neoForge = verdict("NeoForge", "embeddium-", BootResult.SURVIVED, bootedLoader = "Fabric")

        Assertions.assertNull(
            ClientsideVerifier.loaderDisprovingTheCrash(forge, listOf(forge, neoForge)),
            "NeoForge never booted a server — a Fabric build did, under a stem 'embeddium-' cannot strip"
        )
    }

    /** The same verdict with its own loader behind the boot is a disproof, so the guard is about *whose* boot. */
    @Test
    fun theSameSurvivalOnItsOwnLoaderStillDisproves() {
        val forge = verdict("Forge", "embeddium-", BootResult.CRASHED, Verdict.CONFIRMED)
        val neoForge = verdict("NeoForge", "embeddium-", BootResult.SURVIVED, bootedLoader = "NeoForge")

        Assertions.assertEquals(
            "NeoForge",
            ClientsideVerifier.loaderDisprovingTheCrash(forge, listOf(forge, neoForge))?.loader
        )
    }

    /**
     * Different stems mean the published entry would *not* strip the surviving build, so the two verdicts do
     * not contradict each other at all — and a mod really can be client-only on one loader. Without this
     * condition the reconciliation would be a blanket "any survival clears any crash", which is not what the
     * evidence supports.
     */
    @Test
    fun aLoaderBootingUnderADifferentEntryDisprovesNothing() {
        val forge = verdict("Forge", "themod-forge-", BootResult.CRASHED, Verdict.CONFIRMED)
        val fabric = verdict("Fabric", "themod-fabric-", BootResult.SURVIVED)

        Assertions.assertNull(ClientsideVerifier.loaderDisprovingTheCrash(forge, listOf(forge, fabric)))
    }

    /**
     * Same rule as every other guard in this engine: only a clean boot is evidence. A loader that was never
     * booted, or whose boot learned nothing, cannot clear a crash — and neither can one that also crashed,
     * which corroborates it instead.
     */
    @Test
    fun onlyACleanBootDisprovesACrash() {
        val forge = verdict("Forge", "ironchest-", BootResult.CRASHED, Verdict.CONFIRMED)

        listOf(BootResult.CRASHED, BootResult.INCONCLUSIVE, null).forEach { otherResult ->
            val other = verdict("NeoForge", "ironchest-", otherResult)
            Assertions.assertNull(
                ClientsideVerifier.loaderDisprovingTheCrash(forge, listOf(forge, other)),
                "a $otherResult boot must not clear a crash"
            )
        }
    }

    /** Nothing to reconcile unless this verdict is itself a crash — a clean boot is not being overturned. */
    @Test
    fun aVerdictThatDidNotCrashIsLeftAlone() {
        val forge = verdict("Forge", "ironchest-", BootResult.SURVIVED)
        val neoForge = verdict("NeoForge", "ironchest-", BootResult.SURVIVED)

        Assertions.assertNull(ClientsideVerifier.loaderDisprovingTheCrash(forge, listOf(forge, neoForge)))
    }

    /**
     * A verdict with no derivable entry publishes nothing, so nothing can be stripped and there is no
     * contradiction to resolve. Blank is treated as absent — an empty stem would `startsWith`-match *every*
     * file, which is a hazard of its own and must never be what links two verdicts together.
     */
    @Test
    fun aVerdictWithoutAnEntryHasNothingToContradict() {
        val neoForge = verdict("NeoForge", null, BootResult.SURVIVED)
        val blankBooted = verdict("NeoForge", "  ", BootResult.SURVIVED)

        Assertions.assertNull(
            ClientsideVerifier.loaderDisprovingTheCrash(
                verdict("Forge", null, BootResult.CRASHED, Verdict.CONFIRMED), listOf(neoForge)
            )
        )
        Assertions.assertNull(
            ClientsideVerifier.loaderDisprovingTheCrash(
                verdict("Forge", "  ", BootResult.CRASHED, Verdict.CONFIRMED), listOf(blankBooted)
            )
        )
    }

    /** A single-loader project cannot disprove itself, however it booted. */
    @Test
    fun aVerdictIsNeverItsOwnDisproof() {
        val onlyLoader = verdict("Forge", "ironchest-", BootResult.CRASHED, Verdict.CONFIRMED)

        Assertions.assertNull(ClientsideVerifier.loaderDisprovingTheCrash(onlyLoader, listOf(onlyLoader)))
    }

    // --- what a superseded crash then reports -------------------------------------------------------

    /**
     * The confidence drops to whatever the metadata alone supports — re-derived from `aggregate` with no boot
     * rather than hardcoded here, so there is one ladder and not two. For the reported case that is `LOW`:
     * the jar declares server/both and nothing decisive stands against it any more.
     *
     * The crash itself is **not** erased. `bootResult` stays CRASHED and the excerpt is kept, because the boot
     * really did crash and that is worth diagnosing; what changes is whether it counts as sideness evidence.
     */
    @Test
    fun aSupersededCrashKeepsTheCrashButLosesTheConfidence() {
        val forge = verdict("Forge", "ironchest-", BootResult.CRASHED, Verdict.CONFIRMED)
            .copy(bootCrashExcerpt = "java.lang.NoSuchMethodError")
        val neoForge = verdict("NeoForge", "ironchest-", BootResult.SURVIVED)

        val superseded = ClientsideVerifier.supersededByLoader(
            verdict = forge,
            disproving = neoForge,
            metadataOnly = VerdictAssessment(Verdict.CLEAR, null, null, null),
            bootDetail = "Forge 48.1.0 / Minecraft 1.20.2 → CRASHED (exit 1)"
        )

        Assertions.assertEquals(Verdict.CLEAR, superseded.verdict)
        Assertions.assertEquals(BootResult.CRASHED, superseded.bootResult, "the boot really did crash — do not erase it")
        Assertions.assertEquals("java.lang.NoSuchMethodError", superseded.bootCrashExcerpt)
        Assertions.assertEquals("Forge", superseded.loader)
        Assertions.assertEquals("ironchest-", superseded.suggestedEntry)
    }

    /**
     * The note is rebuilt, not appended to. Its old text ended in "a strong clientside signal", which is now
     * false — leaving it in place and bolting a correction on the end is exactly the stale-prose failure this
     * project keeps paying for. What survives is the boot detail; what replaces it is why the crash no longer
     * counts, naming the loader that disproved it.
     */
    @Test
    fun aSupersededCrashExplainsItselfWithoutTheOldClaim() {
        val forge = verdict("Forge", "ironchest-", BootResult.CRASHED, Verdict.CONFIRMED)
            .copy(note = "Declared server/both but the server crashed — a strong clientside signal. Forge 48.1.0 → CRASHED")

        val note = ClientsideVerifier.supersededByLoader(
            verdict = forge,
            disproving = verdict("NeoForge", "ironchest-", BootResult.SURVIVED),
            metadataOnly = VerdictAssessment(Verdict.CLEAR, null, null, null),
            bootDetail = "Forge 48.1.0 / Minecraft 1.20.2 → CRASHED (exit 1)"
        ).note

        Assertions.assertNotNull(note)
        Assertions.assertFalse(note!!.contains("strong clientside signal"), "the claim is no longer true: $note")
        Assertions.assertTrue(note.contains("NeoForge"), "the note must name the loader that disproved it: $note")
        Assertions.assertTrue(note.contains("ironchest-"), "and the entry the two share: $note")
        Assertions.assertTrue(note.contains("exit 1"), "the crash's own detail must survive: $note")
    }

    /** A metadata note still applies once the crash is set aside, so it is carried into the rebuilt note. */
    @Test
    fun aMetadataCaveatSurvivesTheRewrite() {
        val note = ClientsideVerifier.supersededByLoader(
            verdict = verdict("Forge", "themod-", BootResult.CRASHED, Verdict.CONFIRMED),
            disproving = verdict("NeoForge", "themod-", BootResult.SURVIVED),
            metadataOnly = VerdictAssessment(Verdict.INCONCLUSIVE, null, null, "Platform marks server unsupported but the jar declares server/both."),
            bootDetail = null
        ).note

        Assertions.assertNotNull(note)
        Assertions.assertTrue(note!!.contains("Platform marks server unsupported"), "the caveat still holds: $note")
    }
}
