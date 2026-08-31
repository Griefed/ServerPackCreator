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
/**
 * Which rung of the ladder settled a boot's verdict, and whether that rung's `CRASHED` counts as **decisive
 * evidence of client-only-ness**.
 *
 * Why this is needed at all: `CRASHED` is reachable from [CLIENT_ONLY_CLASS], which no environment failure
 * can fabricate, and from [EXIT_CODE], which means only *"the process exited non-zero and nothing recognised
 * why"*. Both produced an identical `HIGH`, so the published fallback list could not tell a mod reaching for
 * `net/minecraft/client` from one whose mixins failed to apply. Sampled against the deployed grinder on
 * 2026-08-31, four of five published boot logs were the latter — and one of those mods was already being
 * served to every instance polling the list.
 *
 * @author Griefed
 */
enum class BootDecision(
    /**
     * Whether a `CRASHED` from this rung may publish a clientside entry. **Exactly two qualify**, and the set
     * is deliberately tiny: [CLIENT_ONLY_CLASS] because the marker cannot be faked by a broken harness, and
     * [OPERATOR_RULE] because a rule that reached `CRASHED` said so deliberately — an undecided rule resolves
     * to the ladder or to `INCONCLUSIVE`, never to `CRASHED`.
     */
    val decisive: Boolean = false
) {
    /** The server reported ready. */
    READY_LINE,

    /** The boot ran out of its budget, so the mod never got a fair run. */
    TIMED_OUT,

    /** The pack's own setup aborted before the mod was loaded. */
    SETUP_ABORT,

    /** The JVM never got as far as running the server. */
    LAUNCH_FAILURE,

    /** The modloader fell over before it could load anything. */
    LOADER_BOOTSTRAP_FAILURE,

    /** Killed from outside, or killed for memory — host trouble, not the mod's doing. */
    KILLED_OR_OOM,

    /** An operator's console rule decided it. Decisive, because such a rule states `CRASHED` on purpose. */
    OPERATOR_RULE(decisive = true),

    /** The server died reaching for a client-only class. The one signal a broken harness cannot fabricate. */
    CLIENT_ONLY_CLASS(decisive = true),

    /** A dependency the staging failed to supply, so the mod's own code never ran. */
    DEPENDENCY_FAILURE,

    /** The sandbox denied the network, so the mod failed on the harness. */
    SANDBOX_NETWORK,

    /**
     * A mixin failed to apply or inject — the jar and the Minecraft it was booted on disagree about what
     * exists. Says nothing about sideness: the mod never reached its own server code.
     */
    MIXIN_APPLY_FAILURE,

    /** The modloader's dependency solver gave up, so no mod was loaded to judge. */
    LOADER_SOLVER_FAILURE,

    /** The jar and the runtime disagree about the loader or its language — the wrong jar was staged. */
    RUNTIME_MISMATCH,

    /** Nothing was recognised; the exit status alone decided. **Never** evidence of anything about sideness. */
    EXIT_CODE
}

data class Classification(
    /** The verdict this console produced. */
    val result: BootResult,
    /** The rule that decided or annotated it, or `null` when no rule matched. */
    val firedRule: ConsoleRuleMatch? = null,
    /**
     * Which rung settled [result]. Carried so the publication gate can refuse a `CRASHED` that is not
     * evidence, and so an audit can report the distribution rather than guessing at it.
     */
    val decidedBy: BootDecision = BootDecision.EXIT_CODE
) {
    companion object {
        /** A verdict the built-in ladder reached with no rule involved. */
        internal fun of(result: BootResult, decidedBy: BootDecision) = Classification(result, null, decidedBy)
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
            "|^Error: could not open" +
            // No vanilla server jar means the loader's launcher aborts before Loader itself starts, so no
            // mod is ever loaded. Both spellings: the shipped template's own message, and Quilt's.
            "|The Minecraft server \\.JAR is missing" +
            "|Missing game jar at)",
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
            "|requires any version of" +
            // Quilt Loader's solver phrasing, e.g. `requires version [0.19.3, INF) of fabricloader`. The largest
            // single class in the 2026-08-29 census -- 63 of 200 published crash logs -- and previously read as a
            // plain non-zero exit, so the *candidate* wore a verdict earned by the pack around it.
            "|requires version .{1,80} of " +
            // A mixin refusing because the class it targets is absent: the target belongs to a mod that was not
            // staged, so nothing of the candidate was exercised. 6 of 200.
            "|ClassMetadataNotFoundException" +
            // The Mixin tweaker is part of the pack *we* assemble; without it no mod loads at all. 6 of 200, all
            // legacy LaunchWrapper-era Forge.
            "|ClassNotFoundException: org\\.spongepowered\\.asm\\.launch\\.MixinTweaker)",
        RegexOption.IGNORE_CASE
    )

    /**
     * The sandbox refusing a mod the network. Boots run `--network none` — that isolation is the entire
     * guarantee — so a mod whose loader reaches for the internet at startup is certain to die here and nowhere
     * else, which makes the crash a property of the harness rather than of the mod.
     *
     * Measured 2026-08-29 across 200 published crash logs: **15 (8%)**. The clearest is OneConfig, which fetches
     * its own stage1 from `api.polyfrost.org`, falls back to a Swing error dialog when it cannot — the
     * `Fontconfig error: No writable cache directories` tail those logs all share, in a headless container — and
     * then calls `System.exit`.
     *
     * Subordinate to [clientOnlyClassMarker], like every other excuse: a clientside mod may phone home *and* die
     * on a client class, and the marker must still win.
     */
    private val sandboxNetworkMarkers = Regex(
        "(java\\.net\\.UnknownHostException" +
            "|java\\.net\\.ConnectException" +
            "|java\\.net\\.NoRouteToHostException" +
            "|java\\.net\\.SocketTimeoutException)"
    )

    /**
     * The decisive clientside signal: the server loaded the mod and then died reaching for a client-only class. This
     * is the one thing the expensive boot exists to catch, so it outranks the dependency excuse above — an
     * informational "Found 2 dependencies" line must never suppress it.
     */
    /**
     * A mixin that could not be applied or injected. **Not sideness evidence**: the jar and the Minecraft it
     * was booted on disagree about what exists, so the mod's own server code never ran.
     *
     * Two of five real logs sampled 2026-08-31 died exactly this way and reached the bare exit-code rung —
     * `create_ltab` on Minecraft 1.20.6 (`@Inject … could not find any targets matching
     * 'Lnet/minecraft/class_4317;method_20807'`) and `debugify` on 1.19.1 (`@Shadow field f_25782_ was not
     * located in the target class`). The existing mixin coverage in [dependencyFailureMarkers] is only
     * `ClassMetadataNotFoundException` and the legacy `MixinTweaker`, neither of which is the apply/inject
     * shape. **Stays below [clientOnlyClassMarker]** — a mod reaching a client-only class *through* a mixin
     * is a genuine signal, and outranking it here would discard true positives.
     */
    private val mixinApplyFailureMarkers = Regex(
        "(InvalidInjectionException" +
            "|InvalidMixinException" +
            "|MixinApplyError" +
            "|MixinTransformerError" +
            "|FAILED during APPLY" +
            "|Critical injection failure" +
            "|Mixin transformation of .{1,120} failed)",
        RegexOption.IGNORE_CASE
    )

    /**
     * The modloader's dependency solver giving up, so nothing was loaded to judge.
     *
     * Quilt's phrasing shares **nothing** with Fabric's: no `requires`, no `Incompatible mods found`, so
     * [dependencyFailureMarkers] does not reach it. Its `requires version .{1,80} of ` alternative was
     * written for a *different* Quilt shape. Observed 2026-08-31 on `create_ltab` / Quilt 0.31.0-beta.1:
     * `Unhandled solver error involving the following rules:` with
     * `quilt_resource_loader versions [*] (0 valid options, 0 invalid options)`.
     */
    private val loaderSolverFailureMarkers = Regex(
        "(Unhandled solver error" +
            "|\\(0 valid options, 0 invalid options\\)" +
            "|Quilt Loader: Failed to load)",
        RegexOption.IGNORE_CASE
    )

    /**
     * The staged jar and the runtime disagree about the loader itself — the wrong jar was staged, so the run
     * says nothing about the mod.
     *
     * Observed 2026-08-31: `DamageVignette-2.0.2-**forge**+mc1.20.jar` staged for a **NeoForge** boot, dying
     * on `Missing language javafml version [46,)` (Forge's language provider, not NeoForge's) and a
     * `java.lang.module.ResolutionException` from the jar's bundled MixinExtras colliding with NeoForge's.
     * One platform file claiming two loaders is what put it there; see `BootCandidateSelector`.
     *
     * **log4j-core belongs here for the opposite reason, and it is the most valuable member.** Its absence is
     * not the mod's doing at all — the server is supposed to *have* a logging framework — so a console
     * reaching for `org.apache.logging.log4j` means the runtime we assembled is broken. Measured 2026-08-31:
     * every one of the 90 boots against the cached `NeoForge 21.11.45 / Minecraft 1.21.11` install died this
     * way, `corgilib` (a library) and `chisels-bits` (a building mod that runs on servers) included, and the
     * ones that reached a non-zero exit were published as clientside. **One poisoned cache entry produced
     * false positives across an entire tuple**, which is exactly the failure a bare exit-code verdict cannot
     * distinguish from a mod crashing on its own merits.
     */
    private val runtimeMismatchMarkers = Regex(
        "(Missing language .{1,40} version" +
            "|java\\.lang\\.module\\.ResolutionException" +
            "|NoClassDefFoundError: org/apache/logging/log4j" +
            "|ClassNotFoundException: org\\.apache\\.logging\\.log4j)",
        RegexOption.IGNORE_CASE
    )

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
     * A rule stating a verdict decides. A rule stating none is *undecided*, and
     * [ConsoleRuleSet.undecidedVerdict] says what that means — by default the ladder decides and the rule
     * merely names itself on the result, so an operator can see their pattern matched without it changing
     * anything. First match in file order wins: the file's order is the only precedence its author can see.
     */
    fun classify(
        consoleLines: List<String>,
        exitCode: Int?,
        timedOut: Boolean,
        rules: ConsoleRuleSet
    ): Classification {
        if (consoleLines.any { readyLine.containsMatchIn(it) }) {
            return Classification.of(BootResult.SURVIVED, BootDecision.READY_LINE)
        }
        if (timedOut) {
            return Classification.of(BootResult.INCONCLUSIVE, BootDecision.TIMED_OUT)
        }
        if (consoleLines.any { setupAbortMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE, BootDecision.SETUP_ABORT)
        }
        // The JVM never got as far as running the server, so nothing about the mod was exercised.
        if (consoleLines.any { launchFailureMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE, BootDecision.LAUNCH_FAILURE)
        }
        // The loader fell over before it could load anything, so there was no mod in the run to blame.
        if (consoleLines.any { loaderBootstrapFailureMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE, BootDecision.LOADER_BOOTSTRAP_FAILURE)
        }
        // Killed from outside, or killed for memory: the mod never got the chance to fail on its own merits.
        if (exitCode in killedExitCodes || consoleLines.any { outOfMemoryMarkers.containsMatchIn(it) }) {
            return Classification.of(BootResult.INCONCLUSIVE, BootDecision.KILLED_OR_OOM)
        }
        // Rung 7 -- the operator's own rules. Below every guard above, all of which mean the mod never got a
        // fair run, so a hand-edited file can never turn host trouble into a HIGH. Above the marker below,
        // so a rule can raise a signature the exit code excused and excuse one the marker would crash.
        val fired = rules.rules.firstNotNullOfOrNull { rule ->
            rule.firstMatch(consoleLines)?.let { ConsoleRuleMatch(rule, it) }
        }
        // A rule that states a verdict decides. One that does not is *undecided*: the rule set says whether
        // that means INCONCLUSIVE or "let the ladder decide", and in the latter case the match still rides
        // along on whatever the ladder settles, so the operator can see that their pattern matched.
        val decided = fired?.rule?.verdict ?: rules.undecidedVerdict?.takeIf { fired != null }
        if (fired != null && decided != null) {
            return Classification(decided, fired, BootDecision.OPERATOR_RULE)
        }
        val annotating = fired

        // A server that died reaching for a client-only class is decisive on the console alone, and must be, because
        // the exit status cannot be trusted here: measured 2026-07-30, NeoForge's ServerStarterJar reports the crash
        // in full and then exits **0**, so `modelfix` -- textbook `NoClassDefFoundError: net/minecraft/client/
        // Minecraft` -- was scored INCONCLUSIVE and no verdict in a 517-strong store ever reached HIGH. Environment
        // failures cannot fake this marker, which is what makes it safe to trust over the exit code.
        if (consoleLines.any { clientOnlyClassMarker.containsMatchIn(it) }) {
            return Classification(BootResult.CRASHED, annotating, BootDecision.CLIENT_ONLY_CLASS)
        }
        // Dependencies our staging failed to supply mean the mod was never fairly tested.
        if (consoleLines.any { dependencyFailureMarkers.containsMatchIn(it) }) {
            return Classification(BootResult.INCONCLUSIVE, annotating, BootDecision.DEPENDENCY_FAILURE)
        }
        // The sandbox denied the network, so the mod failed on the harness rather than on its own merits.
        if (consoleLines.any { sandboxNetworkMarkers.containsMatchIn(it) }) {
            return Classification(BootResult.INCONCLUSIVE, annotating, BootDecision.SANDBOX_NETWORK)
        }
        // The jar and the runtime disagree -- a mixin that cannot apply, a solver that gave up, a language
        // provider from the wrong loader. Each means the pack we assembled was wrong, not that the mod is
        // clientside, and each reached the bare exit code before these rungs existed.
        if (consoleLines.any { mixinApplyFailureMarkers.containsMatchIn(it) }) {
            return Classification(BootResult.INCONCLUSIVE, annotating, BootDecision.MIXIN_APPLY_FAILURE)
        }
        if (consoleLines.any { loaderSolverFailureMarkers.containsMatchIn(it) }) {
            return Classification(BootResult.INCONCLUSIVE, annotating, BootDecision.LOADER_SOLVER_FAILURE)
        }
        if (consoleLines.any { runtimeMismatchMarkers.containsMatchIn(it) }) {
            return Classification(BootResult.INCONCLUSIVE, annotating, BootDecision.RUNTIME_MISMATCH)
        }
        return Classification(
            when (exitCode) {
                null, 0 -> BootResult.INCONCLUSIVE
                else -> BootResult.CRASHED
            },
            annotating,
            BootDecision.EXIT_CODE
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
