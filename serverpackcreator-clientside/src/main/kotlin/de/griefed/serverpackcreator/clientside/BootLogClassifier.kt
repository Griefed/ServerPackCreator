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

import de.griefed.serverpackcreator.clientside.BootLogClassifier.clientOnlyClassMarker
import de.griefed.serverpackcreator.clientside.BootLogClassifier.setupAbortMarkers


/**
 * Outcome of booting a server with the candidate mod force-included. Note the asymmetry: only
 * [CRASHED] is a strong positive for "clientside" — a graceful clientside mod boots fine
 * ([SURVIVED]), so SURVIVED does not prove server-safety.
 *
 * @author Griefed
 */
/**
 * What a console classified to, plus the operator rule that had a hand in it — `null` when the built-in
 * ladder decided alone. The rule is carried as a *field* rather than only mentioned in prose, because
 * "how many verdicts did rule X decide?" is the only way to find a bad rule, and a sentence cannot answer it.
 *
 * @author Griefed
 */
data class Classification(
    /** The verdict this console produced. */
    val result: BootResult,
    /** The rule that decided or annotated it, or `null` when no rule matched. */
    val firedRule: ConsoleRuleMatch? = null
) {
    companion object {
        /** A verdict the built-in ladder reached with no rule involved. */
        internal fun of(result: BootResult) = Classification(result)
    }
}

enum class BootResult {
    /** The server reached its ready-line — the mod did not prevent startup. */
    SURVIVED,

    /** The server exited non-zero before becoming ready — the mod likely broke it. */
    CRASHED,

    /** Neither ready nor a clear crash within the time-budget (timeout / clean early exit). */
    INCONCLUSIVE
}

/**
 * Classifies a finished server-boot from its console-output and exit-state, with no I/O of its own so
 * it is fully unit-testable. The Minecraft server's `Done (…)! For help` line is the canonical
 * ready-signal; absent that, a non-zero exit means a crash and a timeout/clean-early-exit is
 * inconclusive.
 *
 * @author Griefed
 */
object BootLogClassifier {

    /** The vanilla server's ready-line, e.g. `[12:00:00] [Server thread/INFO]: Done (21.5s)! For help, ...`. */
    private val readyLine = Regex("""Done \([^)]*\)! For help""")

    /**
     * Signatures of a **pre-launch setup abort** — `start.sh`'s `crashServer` messages for
     * environment/loader/install failures that stop the server *before the mod is ever loaded*: the
     * loader not supporting the Minecraft version, a loader/launcher-jar download failure, the
     * ServerStarterJar install failing, a Java setup failure, a missing `variables.txt`, an
     * unrecognized modloader, or a declined EULA. None of these are the mod's fault, so they are
     * [BootResult.INCONCLUSIVE], not [BootResult.CRASHED] — scoring them as a crash would be a false
     * clientside HIGH. Kept specific so a genuine mod-load crash (a stacktrace, a mixin error) does
     * **not** match.
     */
    private val setupAbortMarkers = Regex(
        "(is not available for Minecraft" +
            "|servers are having trouble" +
            "|Something went wrong during the server installation" +
            "|Java install-script failed" +
            "|Java installation failed" +
            "|wget or curl is required" +
            "|variables\\.txt not present" +
            "|Incorrect modloader specified" +
            "|did not agree to Mojang's EULA)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Console evidence that the run died for lack of memory rather than because of the mod — the JVM's own
     * out-of-memory reports and the shell's message when the kernel's OOM killer takes the server.
     *
     * A mod *can* be memory-hungry, but running out of memory is not evidence that it needs a client, and the
     * confidence model only claims [BootResult.CRASHED] when it is sure. Measured 2026-07-30: the grinder caps a boot
     * at 3 GiB while the host's Docker VM held 1.93 GiB, so the cap could not be honoured and fat mods were killed by
     * the VM — which, without this, scored as a HIGH-confidence clientside crash.
     */
    private val outOfMemoryMarkers = Regex(
        $$"(java\\.lang\\.OutOfMemoryError" +
            "|insufficient memory for the Java Runtime Environment" +
            "|Cannot allocate memory" +
            $$"|Killed\\s+\"?\\$?JAVA)",
        RegexOption.IGNORE_CASE
    )

    /**
     * The JVM never started: it could not open or identify the jar it was told to run. The server therefore never
     * loaded the mod, so the run says nothing about sideness.
     *
     * Found live on 2026-07-30 immediately after exit-status propagation began working: consoles consisting of
     * `Error: Unable to access jarfile forge.jar` (an incomplete cached Forge install layer) exited non-zero and were
     * promoted to HIGH-confidence clientside — ten of the sweep's first fifteen HIGH verdicts, including the
     * definitely-server-side libraries `balm`, `collective` and `geckolib`. Trusting the exit status is what made this
     * class visible, which is why it needs the same pre-launch treatment as [setupAbortMarkers].
     *
     * `Error: could not open` is the JVM launcher's message for an **`@argfile`** it cannot read, and it belongs to
     * exactly the same incomplete-cached-install case — only the file the boot depends on differs. It became
     * reachable when the grinder started launching Forge from `@libraries/.../unix_args.txt` instead of through the
     * ServerStarterJar.
     *
     * **Anchored to line start, and that anchor is load-bearing.** This guard is checked ahead of
     * [clientOnlyClassMarker], so anything it matches never reaches the decisive marker — an over-broad
     * alternative here does not add noise, it turns a textbook clientside crash into
     * [BootResult.INCONCLUSIVE] and drops a true positive. `could not open` is a generic verb phrase a mod may
     * well log about one of its own resources, unlike the three distinctive sentences beside it. The launcher
     * emits this one as the **entire line**, while every mod line carries a timestamp and level prefix, so `^`
     * means "the launcher said it" and nothing else. Lines are matched one at a time, which is what makes the
     * anchor mean line start.
     */
    private val launchFailureMarkers = Regex(
        "(Unable to access jarfile" +
            "|Could not find or load main class" +
            "|Invalid or corrupt jarfile" +
            "|^Error: could not open)",
        RegexOption.IGNORE_CASE
    )

    /**
     * The **modloader itself** failed to bootstrap: the JVM started, but the server never did, so no mod was ever
     * loaded and the run says nothing about sideness. One rung below [launchFailureMarkers] — there the JVM could
     * not open the jar, here it opened it and the loader fell over on its own module wiring.
     *
     * Found live on 2026-08-23 in `CurseForge-ars-nouveau-Forge.log`, which was scored CRASHED and therefore
     * headed for a clientside HIGH for a mod whose code never ran. The cause is upstream and deterministic, not a
     * flaky boot: the NeoForge ServerStarterJar synthesises a boot layer for the module path named in Forge's
     * `unix_args.txt`, and Forge's `SecureModuleClassLoader` looks a read module's configuration up among its
     * **direct** parents only — so `java.base`, one level further up in the real boot configuration, is not found
     * and it throws. cpw's original, which NeoForge itself runs, falls back to the platform classloader there,
     * which is why the same starter jar launches NeoForge and not Forge.
     *
     * The starter jar's own give-ups are the same class of failure and sit here too: an install layer with no
     * run-script leaves it nothing to read launch arguments out of, and it exits before any loader code runs.
     */
    private val loaderBootstrapFailureMarkers = Regex(
        "(Could not find parent layer for module" +
            "|Failed to find run file at" +
            "|Failed to find startup arguments using run script path)",
        RegexOption.IGNORE_CASE
    )

    /**
     * A mod whose **required dependencies** were not satisfied never got a fair test: it was refused before its own
     * code ran, so its failure says nothing about client-vs-server.
     *
     * Staging force-includes the mod plus its recursively-resolved required deps, but resolution is imperfect —
     * transitive requirements, version ranges and distribution-locked CurseForge files leak through. Measured
     * 2026-07-30 across 112 kept boot logs: **36** failed exactly here, the largest single failure class. Kept
     * deliberately narrow, and always subordinate to [clientOnlyClassMarker] below.
     */
    private val dependencyFailureMarkers = Regex(
        "(Missing or unsupported mandatory dependencies" +
            "|Unmet dependency listing" +
            "|Incompatible mods found" +
            "|requires .{1,80} or above" +
            "|requires any version of)",
        RegexOption.IGNORE_CASE
    )

    /**
     * The decisive clientside signal: the server loaded the mod and then died reaching for a client-only class. This
     * is the one thing the expensive boot exists to catch, so it outranks the dependency excuse above — an
     * informational "Found 2 dependencies" line must never suppress it.
     */
    private val clientOnlyClassMarker = Regex(
        "(NoClassDefFoundError: net/minecraft/client|ClassNotFoundException: net\\.minecraft\\.client)"
    )

    /**
     * Exit codes meaning "terminated from outside" (POSIX `128 + signal`): `SIGKILL` — what Docker reports for an
     * OOM-killed container — and `SIGTERM`. Neither says anything about the mod, so neither may count as a crash.
     * `SIGABRT` (134) is deliberately **not** here: a fatal JVM abort is a real failure of the running server.
     */
    private val killedExitCodes = setOf(137, 143)

    /**
     * Classify a boot from its [consoleLines], the process [exitCode] (`null` if it was killed/never
     * exited) and whether the time-budget was exceeded ([timedOut]).
     *
     * The ready-line wins outright — when present the boot [BootResult.SURVIVED] even though the
     * process is subsequently killed (yielding a non-zero exit). Otherwise a timeout is
     * [BootResult.INCONCLUSIVE]; a pre-launch [setupAbortMarkers] hit is [BootResult.INCONCLUSIVE]
     * (the mod was never tested — the loader/env/install failed first); so is a JVM that never launched
     * ([launchFailureMarkers]) or a loader that never bootstrapped ([loaderBootstrapFailureMarkers]); a clean
     * `0` exit without ever reaching ready is [BootResult.INCONCLUSIVE]; and any other non-zero exit is
     * [BootResult.CRASHED].
     */
    fun classify(consoleLines: List<String>, exitCode: Int?, timedOut: Boolean): BootResult =
        classify(consoleLines, exitCode, timedOut, ConsoleRuleSet.EMPTY).result

    /**
     * As above, consulting an operator's [rules] at rung 7 — above the exit code and above
     * [clientOnlyClassMarker], below the timeout, killed/OOM and environment guards.
     *
     * **Why exactly there.** Everything above rung 7 means *the mod never got a fair run*, so a hand-edited
     * file must not be able to manufacture a `CRASHED` — and therefore a `HIGH` — out of host trouble; a
     * memory-starved VM doing precisely that, systematically, to the biggest mods is on this engine's
     * record. Below the marker instead would leave a rule unable to raise the signature this feature exists
     * for: FML's `for invalid dist DEDICATED_SERVER` on a **zero** exit, which the fallback excuses.
     *
     * A matching rule always decides, and a rule that states no verdict decides INCONCLUSIVE — the one
     * outcome that can never publish. First match in file order wins, because the file's order is the only
     * precedence its author can see.
     */
    fun classify(
        consoleLines: List<String>,
        exitCode: Int?,
        timedOut: Boolean,
        rules: ConsoleRuleSet
    ): Classification {
        if (consoleLines.any { readyLine.containsMatchIn(it) }) {
            return Classification.of(BootResult.SURVIVED)
        }
        if (timedOut) {
            return Classification.of(BootResult.INCONCLUSIVE)
        }
        if (consoleLines.any { setupAbortMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE)
        }
        // The JVM never got as far as running the server, so nothing about the mod was exercised.
        if (consoleLines.any { launchFailureMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE)
        }
        // The loader fell over before it could load anything, so there was no mod in the run to blame.
        if (consoleLines.any { loaderBootstrapFailureMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE)
        }
        // Killed from outside, or killed for memory: the mod never got the chance to fail on its own merits.
        if (exitCode in killedExitCodes || consoleLines.any { outOfMemoryMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE)
        }
        // Rung 7 -- the operator's own rules. Below every guard above, all of which mean the mod never got a
        // fair run, so a hand-edited file can never turn host trouble into a HIGH. Above the marker below,
        // so a rule can raise a signature the exit code excused and excuse one the marker would crash.
        val fired = rules.rules.firstNotNullOfOrNull { rule ->
            rule.firstMatch(consoleLines)?.let { ConsoleRuleMatch(rule, it) }
        }
        if (fired != null) {
            return Classification(fired.rule.verdict, fired)
        }

        // A server that died reaching for a client-only class is decisive on the console alone, and must be, because
        // the exit status cannot be trusted here: measured 2026-07-30, NeoForge's ServerStarterJar reports the crash
        // in full and then exits **0**, so `modelfix` -- textbook `NoClassDefFoundError: net/minecraft/client/
        // Minecraft` -- was scored INCONCLUSIVE and no verdict in a 517-strong store ever reached HIGH. Environment
        // failures cannot fake this marker, which is what makes it safe to trust over the exit code.
        if (consoleLines.any { clientOnlyClassMarker.containsMatchIn(it) }) {
            return Classification.of(BootResult.CRASHED)
        }
        // Dependencies our staging failed to supply mean the mod was never fairly tested.
        if (consoleLines.any { dependencyFailureMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE)
        }
        return Classification.of(
            when (exitCode) {
                null, 0 -> BootResult.INCONCLUSIVE
                else -> BootResult.CRASHED
            }
        )
    }
}

/**
 * Pulls the relevant slice of a crashed server's console-output for a maintainer to read in the
 * issue-comment, without making them open the full log-artifact. A crash raises confidence that the
 * mod is clientside-only but is no proof — the excerpt lets a human judge *why* it crashed.
 *
 * @author Griefed
 */
object BootLogExcerpt {

    /** Lines that mark the onset of a crash; the excerpt starts at the first match. */
    private val crashMarkers = Regex(
        "(Exception|Error|Caused by:|Failed to|crash report|FATAL|A problem occurred|NoClassDefFound|NoSuchMethod|could not be loaded)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extract a crash-excerpt from [consoleLines]: from the first crash-marker to the end (capped at
     * [maxLines] and [maxChars]), falling back to the tail when no marker is found. Returns `null`
     * for empty input.
     */
    fun crashExcerpt(consoleLines: List<String>, maxLines: Int = 60, maxChars: Int = 4_000): String? {
        if (consoleLines.isEmpty()) {
            return null
        }
        val firstMarker = consoleLines.indexOfFirst { crashMarkers.containsMatchIn(it) }
        val from = if (firstMarker >= 0) firstMarker else (consoleLines.size - maxLines).coerceAtLeast(0)
        val slice = consoleLines.subList(from, consoleLines.size).take(maxLines)
        return slice.joinToString("\n").take(maxChars).ifBlank { null }
    }
}
