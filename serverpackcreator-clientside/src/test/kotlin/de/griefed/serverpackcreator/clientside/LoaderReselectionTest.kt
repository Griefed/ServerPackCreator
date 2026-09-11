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

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.modscanning.LoaderDescriptors
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * Pins that a jar whose platform page ticked the **wrong loader** is verified under the loader its own
 * descriptor names, instead of being refused.
 *
 * **The measured rows, read from the public grinder on 2026-09-10.** Ten `UNVERIFIABLE` verdicts are a
 * platform mis-tick and nothing else: `bellsandwhistles-0.4.5-1.21.1.jar` carries only
 * `META-INF/neoforge.mods.toml` and is ticked **Forge**; `Highlighter-1.19.4-forge-1.1.5.jar` is ticked
 * **Fabric**. The jars are fine, they run on a client and a server, and every launcher installs them under
 * the loader they were built for — so refusing them publishes a verdict about our own reading of a web form.
 *
 * **Why re-selecting the loader is a different fix from re-selecting the version.**
 * `reselectOnMinecraftContradiction` answers *"the jar excludes this Minecraft"* by trying another version,
 * and `Prepared.Failed.declaredMinecraftConstraint` is set **only** for that disagreement — the landmine in
 * this module's `CLAUDE.md` says so, because a jar carrying the wrong loader's descriptor offers no second
 * version to try and would re-stage down its whole version list, learning nothing each time. So the loader
 * disagreement needs its own channel and its own retry, and exactly one of the two may fire.
 *
 * **What stops this becoming an amnesty**, and each is asserted below: the declared loader must actually
 * have a build for the Minecraft being booted, a jar that declares nothing or declares the requested loader
 * is untouched, and the retry goes through `stageBootPack` — so a second contradiction surfaces rather than
 * loops.
 *
 * **The verdict still says which loader ran.** `BootOutcome.bootedLoader` is stamped from the staged pack,
 * so a re-selected boot has `bootedLoader != loader`, and
 * `ClientsideVerifierCrossLoaderTest.aBootUnderAnotherLoaderCannotDisproveThisOnesCrash` already pins that
 * such a verdict cannot disprove another loader's crash. Not restated here — one home per rule.
 *
 * Driven through the **real** `prepareBootPack` over a real jar written to disk, and observed through the
 * loader-version policy, which `stageBootPack` asks once per staging attempt: that records the exact
 * sequence of loaders staged, so "it re-selected" and "it re-selected once" are the same assertion.
 *
 * @author Griefed
 */
internal class LoaderReselectionTest {

    private val apiWrapper = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))
    private val resolver = LoaderVersionResolver(apiWrapper.versionMeta)

    /**
     * A real server release where Forge and NeoForge both publish builds **and** NeoForge reads its own
     * `neoforge.mods.toml` — taken from SPC's own metadata so the test states an era rather than a version.
     * Below 1.20.5 NeoForge also reads `META-INF/mods.toml`, so no jar can contradict Forge there.
     */
    private val neoTomlRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .first {
            LoaderDescriptors.neoForgeUsesNeoToml(it) &&
                resolver.latest("Forge", it) != null &&
                resolver.latest("NeoForge", it) != null
        }

    /**
     * The newest real release where NeoForge still reads Forge's `META-INF/mods.toml` — i.e. below 1.20.5 —
     * and publishes a build. That is where a `mods.toml`-only jar is *both* loaders' descriptor, which is
     * what the version retry exists to reach.
     */
    private val sharedTomlRelease = apiWrapper.versionMeta.minecraft.serverReleases()
        .map { it.minecraftVersion }
        .filter { !LoaderDescriptors.neoForgeUsesNeoToml(it) && resolver.latest("NeoForge", it) != null }
        .maxWithOrNull(BootCandidateSelector.minecraftComparator)

    /**
     * Records the loader of every staging attempt, and can make a loader unavailable.
     *
     * `preferredVersion` is asked once per `stageBootPack` call, so its argument list *is* the re-selection
     * history. `latestVersion` is what the bootability gate reads, so withholding it is how a loader with no
     * build for this Minecraft is expressed. Both answer a build that cannot be installed, which is what
     * keeps staging exercised and no server ever launched.
     */
    private class RecordingPolicy(private val unavailable: Set<String> = emptySet()) : LoaderVersionPolicy {
        val stagedFor = mutableListOf<String>()

        override fun preferredVersion(loader: String, minecraftVersion: String): String? {
            stagedFor.add(loader)
            return "0.0.0-no-such-build"
        }

        override fun latestVersion(loader: String, minecraftVersion: String): String? =
            "0.0.0-no-such-build".takeIf { loader !in unavailable }
    }

    /** The mis-ticked file: the platform says Forge, and only the descriptor knows better. */
    private val tickedForge = ModFile(
        "bellsandwhistles-0.4.5-$neoTomlRelease.jar",
        setOf("Forge"),
        setOf(neoTomlRelease),
        "https://cdn/bellsandwhistles.jar",
        null,
        emptyList()
    )

    private fun projectOf(file: ModFile) = ProjectFiles(
        platform = "Modrinth", slug = "bells-and-whistles",
        projectUrl = "https://modrinth.com/mod/bells-and-whistles",
        clientSide = DeclaredSupport.UNKNOWN, serverSide = DeclaredSupport.UNKNOWN,
        files = listOf(file)
    )

    private fun platformOf(file: ModFile) = object : ModPlatform {
        override val name: String = "Modrinth"
        override fun handles(projectUrl: String): Boolean = true
        override fun resolve(projectUrl: String): ProjectFiles = projectOf(file)
        override fun resolveDependency(nativeRef: String, minecraftVersion: String?): ProjectFiles? = null
    }

    /** A descriptor with no dependency table, so nothing but the loader is under test. */
    private fun descriptorBody(minecraftRange: String? = null) = """
        modLoader="javafml"
        loaderVersion="[1,)"
        license="MIT"
        [[mods]]
        modId="bellsandwhistles"
        version="0.4.5"
    """.trimIndent() + (
        minecraftRange?.let {
            """

            [[dependencies.bellsandwhistles]]
            modId="minecraft"
            mandatory=true
            versionRange="$it"
            """.trimIndent()
        } ?: ""
        )

    /**
     * Writes the real jar the gate will read, carrying [descriptor] and nothing else — plus, when
     * [minecraftRange] is given, a `[[dependencies]]` entry declaring it, which is what makes the jar
     * disagree about the Minecraft version as well as the loader.
     */
    private fun downloaderFor(descriptor: String, minecraftRange: String? = null) = JarDownloader { file, targetDirectory ->
        targetDirectory.mkdirs()
        File(targetDirectory, file.fileName).also { jar ->
            JarOutputStream(jar.outputStream()).use { out ->
                out.putNextEntry(JarEntry(descriptor))
                out.write(descriptorBody(minecraftRange).toByteArray())
                out.closeEntry()
            }
        }
    }

    private fun prepare(
        descriptor: String,
        workDir: File,
        policy: RecordingPolicy = RecordingPolicy(),
        file: ModFile = tickedForge
    ): Pair<BootVerifier.Prepared, RecordingPolicy> {
        val prepared = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platformOf(file),
            httpDownloader = downloaderFor(descriptor),
            loaderVersionPolicy = policy,
            workDirectory = workDir
        ).prepareBootPack(projectOf(file), "Forge")
        return prepared to policy
    }

    /**
     * The precondition every guard rests on: at this Minecraft version a `neoforge.mods.toml`-only jar
     * really does contradict a Forge boot. A fixture the gate accepted would let these pass for the wrong
     * reason — the failure mode this module has already paid for twice.
     */
    @Test
    fun theFixtureJarReallyContradictsAForgeBoot(@TempDir workDir: File) {
        val jar = requireNotNull(
            downloaderFor(LoaderDescriptors.NEOFORGE_TOML).download(tickedForge, workDir)
        )

        Assertions.assertEquals(
            setOf("NeoForge"), JarSelfDeclaration.declaredLoaders(jar, neoTomlRelease),
            "on $neoTomlRelease only NeoForge reads neoforge.mods.toml"
        )
        Assertions.assertNotNull(
            JarSelfDeclaration.contradiction(jar, "Forge", neoTomlRelease, null),
            "and a Forge boot of it is refused, which is what there is to answer"
        )
    }

    /**
     * **The ten live rows.** The page ticked Forge, the jar says NeoForge, and NeoForge has a build for this
     * Minecraft — so the mod is verified under NeoForge rather than published as unverifiable.
     */
    @Test
    fun aNeoForgeJarTickedForgeIsVerifiedUnderNeoForge(@TempDir workDir: File) {
        val (prepared, policy) = prepare(LoaderDescriptors.NEOFORGE_TOML, workDir)

        Assertions.assertEquals(
            listOf("Forge", "NeoForge"), policy.stagedFor,
            "the requested loader is staged first, then re-selected once to the one the jar declares"
        )
        val detail = (prepared as? BootVerifier.Prepared.Failed)?.detail.orEmpty()
        Assertions.assertFalse(
            detail.contains("not a Forge mod"),
            "the descriptor mismatch is answered, not published ($detail)"
        )
    }

    /**
     * The re-staged attempt uses the **requested** loader's scratch directory, exactly as the cross-loader
     * crash re-check does: staging wipes the directory it uses, and the loader whose descriptor we borrowed
     * has its own verdict to build from its own pack and console.
     */
    @Test
    fun theReselectedAttemptStagesIntoTheRequestedLoadersDirectory(@TempDir workDir: File) {
        val (_, policy) = prepare(LoaderDescriptors.NEOFORGE_TOML, workDir)

        // Asserted here too, because without it every claim below holds trivially for a run that never
        // re-staged -- a shape assertion rather than a pin.
        Assertions.assertEquals(
            listOf("Forge", "NeoForge"), policy.stagedFor, "there is no re-staged attempt to place otherwise"
        )
        val requested = File(workDir, AttemptDirectory.nameFor("Modrinth", "bells-and-whistles", "Forge"))
        val borrowed = File(workDir, AttemptDirectory.nameFor("Modrinth", "bells-and-whistles", "NeoForge"))

        Assertions.assertEquals(
            listOf(tickedForge.fileName),
            File(requested, "modpack/mods").listFiles()?.map { it.name }.orEmpty(),
            "the pack this verdict is built from stays where its owner reads it"
        )
        Assertions.assertFalse(
            borrowed.exists(),
            "and NeoForge's own attempt directory is never touched by a Forge row"
        )
    }

    /**
     * **Not an amnesty.** A jar declaring a loader that has no build for this Minecraft is refused exactly
     * as before — `Highlighter-1.19.4-forge-1.1.5.jar` is the shape: its `mods.toml` names Forge and
     * NeoForge, neither of which NeoForge published for 1.19.4.
     */
    @Test
    fun aDeclaredLoaderWithNoBuildStillRefuses(@TempDir workDir: File) {
        val (prepared, policy) = prepare(
            LoaderDescriptors.NEOFORGE_TOML, workDir, RecordingPolicy(unavailable = setOf("NeoForge"))
        )

        Assertions.assertEquals(
            listOf("Forge"), policy.stagedFor,
            "nothing to re-select to means nothing is re-staged"
        )
        Assertions.assertTrue(
            (prepared as? BootVerifier.Prepared.Failed)?.detail.orEmpty().contains("not a Forge mod"),
            "and the original refusal stands, with its original reason"
        )
    }

    /**
     * **Both disagreements are recorded, because only one of them can be answered here.**
     *
     * `refuseForSelfDeclaration` reports the loader mismatch first — correctly, since no other Minecraft
     * version makes a jar into a mod for a loader whose descriptor it does not carry — and it used to *null*
     * the Minecraft channel to enforce "exactly one retry" through the data. It no longer does: each channel
     * says what the jar said, and `prepareBootPack`'s control flow decides which retry to spend.
     *
     * **This is a behaviour-neutral simplification, not a rescued boot.** A real scan cannot hand this
     * function both, because the range is read by the *mismatching* loader's own scanner —
     * `aLoaderMismatchLeavesNoRangeToRetryOn` pins exactly that. What the change buys is that the ordering
     * no longer depends on an invariant proved in another unit: if a scanner ever merged descriptors the way
     * `QuiltPackScanner` merges Fabric's, the old `takeIf` would have silently suppressed a range that had
     * become meaningful.
     *
     * "Exactly one retry" is `prepareBootPack`'s to enforce, and the two guards above are what hold it
     * there: they assert the staging sequence is `Forge, NeoForge` and not one longer.
     */
    @Test
    fun aJarDisagreeingAboutBothRecordsBothChannels(@TempDir workDir: File) {
        val jar = requireNotNull(
            downloaderFor(LoaderDescriptors.NEOFORGE_TOML).download(tickedForge, workDir)
        )

        val refusal = requireNotNull(
            BootVerifier.refuseForSelfDeclaration(jar, "Forge", neoTomlRelease) { "~1.16.5" }
        ) { "the fixture must be refused, or this guard asserts nothing" }

        Assertions.assertEquals(
            setOf("NeoForge"), refusal.declaredLoaders,
            "the loader channel carries what the jar declares"
        )
        Assertions.assertEquals(
            "~1.16.5", refusal.declaredMinecraftConstraint,
            "and the Minecraft channel still carries the range, because the retry order is the caller's job"
        )
    }

    /**
     * **Which declared loader gets picked, asserted directly.** The rule is reached through staging only
     * with a *single* declared loader, so neither half of it was pinned: prefer one the platform also
     * tagged — the author's two statements agreeing is better evidence than either alone — and otherwise
     * take the alphabetically first, purely so the choice is deterministic rather than dependent on set
     * iteration order. A jar declaring two bootable loaders neither of which its page mentions offers
     * nothing to choose on, and picking by file name is the silently-plausible-value trap.
     */
    @Test
    fun theDeclaredLoaderThePlatformAlsoTaggedWins() {
        Assertions.assertEquals(
            "NeoForge",
            BootVerifier.loaderToVerifyUnder(setOf("Forge", "NeoForge"), tagged = setOf("NeoForge")) { true },
            "the page and the descriptor agreeing on NeoForge outranks Forge sorting first"
        )
    }

    /** With nothing tagged in common the answer is stable rather than arbitrary. */
    @Test
    fun anUntaggedChoiceIsAlphabeticalAndStable() {
        Assertions.assertEquals(
            "Forge",
            BootVerifier.loaderToVerifyUnder(setOf("NeoForge", "Forge"), tagged = setOf("Fabric")) { true }
        )
        Assertions.assertEquals(
            "Forge",
            BootVerifier.loaderToVerifyUnder(setOf("Forge", "NeoForge"), tagged = emptySet()) { true },
            "and it does not depend on the order the set happens to iterate in"
        )
    }

    /** Nothing bootable means nothing to re-select to, which is what keeps this from being an amnesty. */
    @Test
    fun aDeclarationNothingCanBootYieldsNoChoice() {
        Assertions.assertNull(
            BootVerifier.loaderToVerifyUnder(setOf("Forge", "NeoForge"), tagged = setOf("Forge")) { false }
        )
        Assertions.assertNull(
            BootVerifier.loaderToVerifyUnder(emptySet(), tagged = setOf("Forge")) { true },
            "and a jar that declares nothing was never refused for its loader in the first place"
        )
    }

    /**
     * **Why the two retries can never collide, which is the fact worth pinning.**
     *
     * The Minecraft range comes from `scannerFor(loader, minecraftVersion)` — the scanner for the loader
     * *being booted*. When the loader is the thing that mismatches, that scanner reads a descriptor the jar
     * does not carry, so `ScannedMod.minecraftConstraint` is `null` and there is no range to retry on. A
     * `mods.toml`-only jar requested as NeoForge on 1.20.5+ is the case: `neoForgeTomlScanner` looks for
     * `META-INF/neoforge.mods.toml`, finds nothing, and reads no range — even though the jar states one.
     *
     * So the two channels are mutually exclusive **by construction**, not by the refusal nulling one of
     * them. That is why `prepareBootPack` can order the retries without checking for the combination, and
     * why the guard above only has to show that the *function* reports both when both are handed to it.
     *
     * Recorded as a guard rather than a comment because it is a claim about another unit's behaviour: if a
     * scanner ever merged descriptors the way `QuiltPackScanner` merges Fabric's, a range would become
     * readable here and this would go red — which is exactly when somebody needs to re-think the ordering.
     */
    @Test
    fun aLoaderMismatchLeavesNoRangeToRetryOn(@TempDir workDir: File) {
        val tickedNeoForge = tickedForge.copy(loaders = setOf("NeoForge"))
        val policy = RecordingPolicy(unavailable = setOf("Forge"))

        val prepared = BootVerifier(
            apiWrapper = apiWrapper,
            platform = platformOf(tickedNeoForge),
            // The jar declares a Minecraft range *and* carries only Forge's descriptor.
            httpDownloader = downloaderFor(LoaderDescriptors.FORGE_TOML, minecraftRange = "[1.0,1.20.5)"),
            loaderVersionPolicy = policy,
            workDirectory = workDir
        ).prepareBootPack(projectOf(tickedNeoForge), "NeoForge")

        Assertions.assertEquals(
            listOf("NeoForge"), policy.stagedFor,
            "no retry is possible: Forge has no build here and the range was never readable"
        )
        val refusal = prepared as? BootVerifier.Prepared.Failed
        Assertions.assertEquals(
            setOf("Forge"), refusal?.declaredLoaders,
            "the loader channel is what a mismatching jar fills"
        )
        Assertions.assertNull(
            refusal?.declaredMinecraftConstraint,
            "and the range is not readable, because the scanner that would read it is the mismatching " +
                "loader's own: ${refusal?.detail}"
        )
    }

    /** A jar that declares the loader it was asked about is staged once and left alone. */
    @Test
    fun aJarThatDeclaresTheRequestedLoaderIsNotReselected(@TempDir workDir: File) {
        val (_, policy) = prepare(LoaderDescriptors.FORGE_TOML, workDir)

        Assertions.assertEquals(
            listOf("Forge"), policy.stagedFor,
            "there is no contradiction to answer, so nothing re-stages"
        )
    }
}
