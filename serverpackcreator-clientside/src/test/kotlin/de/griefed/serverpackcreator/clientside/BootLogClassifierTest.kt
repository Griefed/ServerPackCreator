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
 * Pins how a finished server-boot is read from its console-output and exit-state, including the
 * deliberate asymmetry that only a crash (not a clean boot) is a strong clientside signal.
 */
internal class BootLogClassifierTest {

    @Test
    fun readyLineMeansSurvivedEvenWhenLaterKilled() {
        val lines = listOf(
            "[12:00:01] [Server thread/INFO]: Preparing level \"world\"",
            "[12:00:21] [Server thread/INFO]: Done (21.473s)! For help, type \"help\""
        )
        // exit is non-zero because we force-kill a server that reached ready — SURVIVED still wins.
        Assertions.assertEquals(BootResult.SURVIVED, BootLogClassifier.classify(lines, 137, timedOut = false))
    }

    @Test
    fun nonZeroExitWithoutReadyMeansCrashed() {
        val lines = listOf(
            "[12:00:03] [Server thread/ERROR]: Encountered an unexpected exception",
            "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"
        )
        Assertions.assertEquals(BootResult.CRASHED, BootLogClassifier.classify(lines, 1, timedOut = false))
    }

    @Test
    fun loaderNotAvailableForMinecraftIsInconclusiveNotCrashed() {
        // Verbatim from a grinder e2e: Fabric has no build for a brand-new Minecraft, so start.sh
        // aborts before the mod is ever loaded. Scoring this CRASHED was a false clientside HIGH.
        val lines = listOf(
            "Detected 26.2. - Java 25",
            "Running Fabric checks and setup...",
            "Fabric is not available for Minecraft 26.2, Fabric 0.19.3."
        )
        Assertions.assertEquals(BootResult.INCONCLUSIVE, BootLogClassifier.classify(lines, 1, timedOut = false))
    }

    @Test
    fun loaderLauncherDownloadFailureIsInconclusive() {
        val lines = listOf(
            "Running Quilt checks and setup...",
            "quilt-server-launch.jar not found. Maybe the Quilt servers are having trouble. Please try again in a couple of minutes and check your internet connection."
        )
        Assertions.assertEquals(BootResult.INCONCLUSIVE, BootLogClassifier.classify(lines, 1, timedOut = false))
    }

    @Test
    fun environmentSetupAbortsAreInconclusive() {
        // A spread of the pre-launch crashServer failures — none are the mod's fault.
        listOf(
            "Something went wrong during the server installation. Please try again in a couple of minutes and check your internet connection.",
            "Java installation failed. Couldn't find /opt/java-25/bin/java.",
            "User did not agree to Mojang's EULA. Entered: no."
        ).forEach { marker ->
            Assertions.assertEquals(
                BootResult.INCONCLUSIVE,
                BootLogClassifier.classify(listOf(marker), exitCode = 1, timedOut = false),
                "setup abort must not be a crash: $marker"
            )
        }
    }

    @Test
    fun timeoutWithoutReadyIsInconclusive() {
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf("still installing..."), exitCode = null, timedOut = true)
        )
    }

    @Test
    fun cleanZeroExitWithoutReadyIsInconclusive() {
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf("stopping..."), exitCode = 0, timedOut = false)
        )
    }

    @Test
    fun crashExcerptStartsAtFirstErrorMarker() {
        val lines = listOf(
            "[12:00:01] [Server thread/INFO]: Loading mods",
            "[12:00:02] [Server thread/INFO]: Loading examplemod",
            "[12:00:03] [Server thread/ERROR]: Encountered an unexpected exception",
            "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft",
            "\tat examplemod.ClientThing.<init>(ClientThing.java:10)"
        )
        val excerpt = BootLogExcerpt.crashExcerpt(lines)!!
        Assertions.assertTrue(excerpt.startsWith("[12:00:03] [Server thread/ERROR]"))
        Assertions.assertTrue(excerpt.contains("NoClassDefFoundError"))
        Assertions.assertFalse(excerpt.contains("Loading mods"))
    }

    @Test
    fun crashExcerptFallsBackToTailWhenNoMarker() {
        val lines = (1..100).map { "line $it" }
        val excerpt = BootLogExcerpt.crashExcerpt(lines, maxLines = 10)!!
        Assertions.assertTrue(excerpt.contains("line 91"))
        Assertions.assertFalse(excerpt.contains("line 90"))
    }

    @Test
    fun crashExcerptIsNullForEmptyLog() {
        Assertions.assertNull(BootLogExcerpt.crashExcerpt(emptyList()))
    }

    /**
     * A server killed from outside — the OOM killer, or any `SIGKILL`/`SIGTERM` — says nothing about the mod, so it
     * must never be scored as a crash.
     *
     * This is not hypothetical: the grinder caps each boot at 3 GiB while the host's Docker VM was measured at
     * **1.93 GiB** (2026-07-30), so the cap cannot actually be honoured and a fat modpack mod gets OOM-killed by the
     * VM instead. Docker reports that as exit **137**, there is no ready-line, and the template's `Killed "$JAVA"`
     * line deliberately does not match the setup-abort markers — which previously left exactly one outcome:
     * `CRASHED`, i.e. a **HIGH-confidence "this mod is clientside"** produced purely by host memory pressure. In a
     * catalog-wide sweep that is a systematic false-positive source, and the suspected-clientside list is the entire
     * deliverable.
     */
    @Test
    fun aKilledServerIsInconclusiveNotCrashed() {
        val console = listOf("[Server thread/INFO]: Preparing spawn area: 24%", "start.sh: line 144: Killed \"\$JAVA\"")

        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(console, exitCode = 137, timedOut = false),
            "exit 137 is SIGKILL — the mod was never given the chance to fail on its own"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf("no ready line here"), exitCode = 143, timedOut = false),
            "exit 143 is SIGTERM — an externally stopped server is not evidence either"
        )
    }

    /**
     * Memory exhaustion reported by the JVM or the kernel is environmental, not a sideness signal. A mod *can* be
     * memory-hungry, but running out of memory is still not evidence that it needs a client — and the confidence
     * model only ever claims CRASHED when it is sure.
     */
    @Test
    fun memoryExhaustionMarkersAreInconclusive() {
        val cases = listOf(
            "java.lang.OutOfMemoryError: Java heap space",
            "There is insufficient memory for the Java Runtime Environment to continue.",
            "os::commit_memory failed; error='Cannot allocate memory' (errno=12)"
        )
        for (line in cases) {
            Assertions.assertEquals(
                BootResult.INCONCLUSIVE,
                BootLogClassifier.classify(listOf("booting", line), exitCode = 1, timedOut = false),
                "must not be scored a clientside crash: $line"
            )
        }
    }

    /**
     * The guard must stay narrow: a genuine mod-load failure still has to read as CRASHED, or the boot signal — the
     * one decisive piece of evidence in the whole confidence model — would be neutered.
     */
    @Test
    fun aGenuineModCrashIsStillCrashed() {
        val console = listOf(
            "[Server thread/ERROR]: Failed to create mod instance.",
            "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"
        )

        Assertions.assertEquals(BootResult.CRASHED, BootLogClassifier.classify(console, exitCode = 1, timedOut = false))
    }

    /**
     * A mod whose **required dependencies** were not satisfied never got a fair test, so its failure says nothing
     * about sideness and must not be scored a clientside crash.
     *
     * Staging force-includes the mod plus its recursively-resolved required deps, but resolution is imperfect —
     * transitive requirements, version ranges and distribution-locked CurseForge files all leak through. Measured
     * 2026-07-30 across 112 kept boot logs: **36** failed on exactly this (e.g. `biomes-o-plenty` →
     * `Mod biomesoplenty requires terrablender 26.2.0.0.1 or above`), making it the single largest failure class.
     * It was previously masked, because the start script swallowed the server's exit status and everything read as
     * INCONCLUSIVE anyway; once the templates propagate the real status these become non-zero exits, and without this
     * guard all 36 would have been promoted to HIGH-confidence "clientside" — a far bigger false-positive source
     * than the one the exit-status fix was meant to expose.
     */
    @Test
    fun unsatisfiedModDependenciesAreInconclusiveNotCrashed() {
        val cases = listOf(
            "[main/ERROR] [ne.ne.fm.lo.ModSorter/LOADING]: Missing or unsupported mandatory dependencies:",
            "[main/ERROR] [ne.ne.fm.lo.FMLLoader/]: Mod biomesoplenty requires terrablender 26.2.0.0.1 or above",
            "Incompatible mods found! net.fabricmc.loader.impl.FormattedException",
            "Unmet dependency listing:"
        )
        for (line in cases) {
            Assertions.assertEquals(
                BootResult.INCONCLUSIVE,
                BootLogClassifier.classify(listOf("booting", line), exitCode = 1, timedOut = false),
                "a dependency our staging failed to supply is not evidence about the mod: $line"
            )
        }
    }

    /**
     * The dependency guard must not swallow the decisive signal: a mod that loads and *then* dies reaching for a
     * client-only class is the one case the whole expensive boot exists to catch.
     */
    @Test
    fun aClientOnlyClassCrashIsStillCrashedEvenWhenDependencyWordsAppear() {
        val console = listOf(
            "[main/INFO]: Found 2 dependencies adding them to mods collection",
            "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"
        )

        Assertions.assertEquals(
            BootResult.CRASHED,
            BootLogClassifier.classify(console, exitCode = 1, timedOut = false),
            "the word 'dependencies' in an informational line must not excuse a real client-class crash"
        )
    }

    /**
     * **The signal the whole engine exists for, and it must not depend on the exit status.**
     *
     * Measured 2026-07-30: NeoForge's ServerStarterJar prints a mod-loading crash in full and then exits **0**. With
     * classification keyed on the exit code, `modelfix` — whose console holds a textbook
     * `NoClassDefFoundError: net/minecraft/client/Minecraft` — was scored INCONCLUSIVE, and **no verdict in a
     * 517-verdict store ever reached HIGH**: the expensive boot was running, crashing correctly, and being thrown
     * away. A client-only-class failure is decisive on the console alone, and safe to trust there because no
     * environment problem (memory, network, missing loader build) can fabricate it.
     */
    @Test
    fun aClientOnlyClassCrashIsDecisiveEvenOnAZeroExit() {
        val console = listOf(
            "[main/INFO]: Loading mods",
            "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft",
            "Exiting..."
        )

        Assertions.assertEquals(
            BootResult.CRASHED,
            BootLogClassifier.classify(console, exitCode = 0, timedOut = false),
            "the loader swallowing the failure into a 0 exit must not erase the crash"
        )
        Assertions.assertEquals(
            BootResult.CRASHED,
            BootLogClassifier.classify(console, exitCode = null, timedOut = false),
            "nor must an unknown exit status"
        )
    }

    /**
     * The console override stays subordinate to the two guards that mean "the mod never got a fair run": a boot the
     * host killed, or one that timed out, is still INCONCLUSIVE even if the log mentions a client class — otherwise
     * host trouble could manufacture a HIGH verdict, the failure mode the confidence model most needs to avoid.
     */
    @Test
    fun theConsoleOverrideDoesNotBeatTheKilledOrTimedOutGuards() {
        val console = listOf("java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft")

        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(console, exitCode = 137, timedOut = false),
            "a killed boot proves nothing, whatever its log says"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(console, exitCode = null, timedOut = true),
            "nor does one that ran out of time"
        )
    }

    /**
     * A JVM that could not even start is never evidence about a mod.
     *
     * Found live on 2026-07-30, minutes after exit-status propagation started working: boots whose console was just
     * `Error: Unable to access jarfile forge.jar` (an incomplete cached Forge install layer — the server never
     * launched) exited non-zero and were promoted to **HIGH-confidence clientside**. Ten of the sweep's first fifteen
     * HIGH verdicts were this, including `balm`, `collective` and `geckolib` — library mods that certainly do run on
     * a server. Making the exit status trustworthy is exactly what made this class visible, so it needs the same
     * pre-launch treatment as the other setup aborts.
     */
    @Test
    fun aJvmThatNeverLaunchedIsInconclusive() {
        val cases = listOf(
            "Error: Unable to access jarfile forge.jar",
            "Error: Could not find or load main class do_not_manually_edit",
            "Error: Invalid or corrupt jarfile server.jar",
            // The JVM's own message for an `@argfile` it cannot read, verbatim from Temurin 17. Reachable since
            // Forge boots from `@libraries/.../unix_args.txt`: an install layer cached without that file fails
            // exactly here, which is the same incomplete-cache case the jarfile messages above cover.
            "Error: could not open `libraries/net/minecraftforge/forge/1.20.2-48.1.0/unix_args.txt'"
        )
        for (line in cases) {
            Assertions.assertEquals(
                BootResult.INCONCLUSIVE,
                BootLogClassifier.classify(listOf("openjdk version \"25.0.3\"", line, "Exiting..."), exitCode = 1, timedOut = false),
                "the server never started, so this says nothing about the mod: $line"
            )
        }
    }

    /**
     * The modloader's own bootstrap failed, so the JVM started but the server never did — no mod was loaded, and
     * the run says nothing about sideness.
     *
     * Verbatim from a live grinder verdict (`CurseForge-ars-nouveau-Forge.log`, 2026-08-23), which was scored
     * CRASHED and therefore a clientside HIGH for a mod whose code never ran. The cause is upstream and
     * deterministic: the NeoForge ServerStarterJar synthesises a boot layer for the module path in
     * `unix_args.txt`, and Forge's `SecureModuleClassLoader` matches a read module's configuration against its
     * *direct* parents only, so `java.base` — one level further up, in the real boot configuration — is not
     * found. cpw's original (what NeoForge itself runs) falls back to the platform classloader instead of
     * throwing, which is why the same jar launches NeoForge and not Forge.
     */
    @Test
    fun aModloaderThatNeverBootstrappedIsInconclusive() {
        val lines = listOf(
            "Detected 1.20.2 - Java 17",
            "Running Forge checks and setup...",
            "server.jar present.",
            "Starting server...",
            "Exception in thread \"main\" java.lang.IllegalStateException: Could not find parent layer for module `java.base` read by `net.minecraftforge.eventbus`",
            "\tat cpw.mods.securejarhandler/net.minecraftforge.securemodules.SecureModuleClassLoader.<init>(SecureModuleClassLoader.java:137)",
            "\tat net.minecraftforge.bootstrap@1.2.0/net.minecraftforge.bootstrap.BootstrapLauncher.main(BootstrapLauncher.java:117)",
            "Exiting..."
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(lines, exitCode = 1, timedOut = false),
            "the loader never bootstrapped, so no mod was ever loaded"
        )
    }

    /**
     * The ServerStarterJar's own give-ups, which land before any loader code runs at all: an install layer with
     * no run-script to read arguments out of. Same class as the bootstrap failure above — the server was never
     * launched — and observed on the offline boots whose cached install was incomplete.
     */
    @Test
    fun aStarterJarThatCannotFindItsRunScriptIsInconclusive() {
        listOf(
            "Failed to find run file at run.sh, attempting to run installer",
            "Failed to find startup arguments using run script path run.sh"
        ).forEach { line ->
            Assertions.assertEquals(
                BootResult.INCONCLUSIVE,
                BootLogClassifier.classify(listOf("Starting server...", line), exitCode = 1, timedOut = false),
                "nothing was launched, so this says nothing about the mod: $line"
            )
        }
    }

    /**
     * Pins the guard order **as a whole**, which no other test in this file does.
     *
     * `classify` is eight ordered guards, and its correctness rests entirely on that order. They accreted one at a
     * time, each in reaction to a live false positive, so every constraint is individually covered while the decision
     * table as a unit never was — reordering two guards could leave every other test in this file green. Each case
     * below puts a **higher-priority** signal in the same console as a **lower-priority** one and asserts the higher
     * wins, which is the only way a swap shows up as a failure.
     *
     * The ladder, highest first: ready-line → timeout → setup-abort → launch-failure → loader-bootstrap-failure
     * → killed/OOM → client-only-class → dependency-failure → exit code.
     */
    @Test
    fun theGuardOrderIsPinnedAsAWhole() {
        val ready = "[Server thread/INFO]: Done (4.2s)! For help, type \"help\""
        val setupAbort = "Fabric is not available for Minecraft 26.2, Fabric 0.19.3."
        val launchFailure = "Error: Unable to access jarfile forge.jar"
        val loaderBootstrapFailure =
            "Exception in thread \"main\" java.lang.IllegalStateException: Could not find parent layer for module `java.base` read by `net.minecraftforge.eventbus`"
        val outOfMemory = "java.lang.OutOfMemoryError: Java heap space"
        val clientClass = "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"
        val dependency = "[main/ERROR] [ne.ne.fm.lo.ModSorter/]: Missing or unsupported mandatory dependencies:"

        // The ready-line outranks every other signal, including a decisive-looking crash and a killed exit.
        Assertions.assertEquals(
            BootResult.SURVIVED,
            BootLogClassifier.classify(listOf(ready, clientClass, dependency), exitCode = 137, timedOut = true),
            "a server that reported ready survived, whatever followed"
        )

        // Everything that means "the mod never got a fair run" outranks the crash signal below it.
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf(clientClass), exitCode = 1, timedOut = true),
            "timeout outranks the client-class crash"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf(setupAbort, clientClass), exitCode = 1, timedOut = false),
            "setup-abort outranks the client-class crash"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf(launchFailure, clientClass), exitCode = 1, timedOut = false),
            "a JVM that never launched outranks the client-class crash"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf(loaderBootstrapFailure, clientClass), exitCode = 1, timedOut = false),
            "a loader that never bootstrapped outranks the client-class crash"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf(outOfMemory, clientClass), exitCode = 1, timedOut = false),
            "memory exhaustion outranks the client-class crash"
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf(clientClass), exitCode = 137, timedOut = false),
            "a killed exit outranks the client-class crash"
        )

        // The crash signal in turn outranks everything below it, including a loader that reports success.
        Assertions.assertEquals(
            BootResult.CRASHED,
            BootLogClassifier.classify(listOf(dependency, clientClass), exitCode = 1, timedOut = false),
            "the client-class crash outranks a dependency complaint"
        )
        Assertions.assertEquals(
            BootResult.CRASHED,
            BootLogClassifier.classify(listOf(clientClass), exitCode = 0, timedOut = false),
            "the client-class crash outranks a zero exit"
        )

        // And the dependency guard outranks the bare exit code.
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf(dependency), exitCode = 1, timedOut = false),
            "a dependency complaint outranks a non-zero exit"
        )

        // The floor: nothing recognisable, decided by the exit status alone.
        Assertions.assertEquals(
            BootResult.CRASHED,
            BootLogClassifier.classify(listOf("something unrecognised"), exitCode = 1, timedOut = false)
        )
        Assertions.assertEquals(
            BootResult.INCONCLUSIVE,
            BootLogClassifier.classify(listOf("something unrecognised"), exitCode = 0, timedOut = false)
        )
    }

    /** A boot that reached the ready-line and was then killed stays SURVIVED — the ready-line still wins outright. */
    @Test
    fun aReadyServerKilledAfterwardsStillSurvived() {
        val console = listOf("[Server thread/INFO]: Done (7.2s)! For help, type \"help\"", "Killed")

        Assertions.assertEquals(BootResult.SURVIVED, BootLogClassifier.classify(console, exitCode = 137, timedOut = false))
    }
}
