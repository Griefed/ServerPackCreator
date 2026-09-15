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

/**
 * What the engine publishes about one mod, in the states that are actually distinguishable.
 *
 * Replaces the old pairing of `BootResult` (CRASHED/SURVIVED/INCONCLUSIVE) with a separate `Confidence`
 * (HIGH/MEDIUM/LOW/INCONCLUSIVE), which conflated two questions — *what happened* and *how sure are we* —
 * and could not express the one thing the operator most needed to know: whether the grind ran at all.
 *
 * **[LOCKED] and [UNVERIFIABLE] split out of [ERROR] on 2026-09-09**, because a grind that could not be
 * performed and a grind *we* broke are not the same report. See those two for the measurement.
 *
 * @author Griefed
 */
enum class Verdict(
    /**
     * Whether this verdict's console and artifacts are kept for later reading.
     *
     * The three verdicts a *boot* produces keep them: [ERROR] because an admin reads it, [INCONCLUSIVE]
     * because the next rule is extracted from it, and [CONFIRMED] because it is what publishes a mod to the
     * fallback list and a verdict that cannot name its own evidence cannot be audited. [CLEAR] discards
     * because a clean boot has nothing to investigate; [LOCKED] and [UNVERIFIABLE] discard because no
     * container ran and there is nothing to keep.
     */
    val keepsLogs: Boolean,
    /**
     * Whether a container actually ran to produce this verdict.
     *
     * `false` for the three prevented verdicts — [ERROR], [LOCKED] and [UNVERIFIABLE] — and that is the
     * question several readers ask without caring which of them it was: a report saying "this loader's own
     * grind did not run" needs exactly this, and asking it as a list of verdict names is how the list comes
     * to be missing one.
     */
    val grindRan: Boolean = true
) {
    /**
     * A rule matched: the mod is exclusion-worthy. The only route to this verdict is a rule, so every
     * confirmation names the rule that produced it and any of them can be revoked by editing the file.
     */
    CONFIRMED(keepsLogs = true),

    /**
     * The server booted and nothing matched — the mod is proven server-safe. Kept distinct from
     * [INCONCLUSIVE] because "we proved it is fine" and "we learned nothing" are different claims, and the
     * most expensive signal this engine produces is wasted if they share a bucket.
     */
    CLEAR(keepsLogs = false),

    /**
     * The grind could not be performed: no runtime image, a refusal before the container started, a failed
     * download, a pack that would not generate. An operator's problem, never evidence about the mod.
     *
     * This is the verdict whose absence cost the project the missing-runtime-image outage, where a
     * host-wide defect was published as a per-candidate INCONCLUSIVE and overwrote decisive verdicts.
     */
    ERROR(keepsLogs = true, grindRan = false),

    /**
     * The boot **ran** and did something unexpected — non-zero exit, crash, timeout, kill — with nothing
     * confirming why. Its log is the raw material the next rule is written from.
     */
    INCONCLUSIVE(keepsLogs = true),

    /**
     * A CurseForge distribution opt-out (`allowModDistribution=false`) stands between us and a jar — the
     * mod's own file, or one of its required dependencies. There is no download URL to fetch, so no jar-scan
     * and no boot are possible, and no amount of retrying changes that.
     *
     * **Not [ERROR], because nothing here is ours or an operator's.** 17 of the public grinder's 53 `ERROR`
     * rows were this on 2026-09-09 — five projects' own files (`corail-tombstone`, `entityculling`,
     * `not-enough-animations`, `skin-layers-3d`, `structory`) plus `better-combat-by-daedelus`, whose
     * `player-animation-library` dependency is locked — sitting in the bucket an operator reads to find out
     * what to fix, and nothing in it was fixable.
     *
     * **Not [UNVERIFIABLE] either**, though both are permanent: this one is a *named* fact with a project,
     * a file and an author's decision behind it, and an author can reverse it. That makes it worth finding
     * by filtering, and worth telling apart from an absence.
     *
     * Keeps no logs because no container ran; the detail is the whole story.
     */
    LOCKED(keepsLogs = false, grindRan = false),

    /**
     * The grind was never possible, for a reason outside this engine and outside the mod: a required
     * dependency nothing upstream published for the loader and Minecraft being booted, a loader with no
     * build for that Minecraft, or a jar carrying only another loader's descriptor because its author ticked
     * the wrong box on a web form.
     *
     * **The distinction from [ERROR] is who can act.** `ERROR` means somebody can go and fix this; that is
     * the whole reason it exists, and the reason it must not also mean "the ecosystem does not contain the
     * pack we would need to build". ~18 of the public grinder's 53 `ERROR` rows were this on 2026-09-09,
     * QSL being the clearest case: its last Modrinth release is Minecraft 1.21 and the project is
     * discontinued, so every Quilt mod declaring a `quilt_*` module on 1.21.1 or later is unverifiable
     * *forever*, and re-grinding it will never say anything else.
     *
     * **The distinction from [INCONCLUSIVE] is whether anything ran.** INCONCLUSIVE is a boot that happened
     * and taught us nothing, and its console is the raw material of the next rule. This is no boot at all,
     * so it keeps no logs and offers a rule-writer nothing.
     */
    UNVERIFIABLE(keepsLogs = false, grindRan = false)
}

/**
 * Why staging stopped, in the three kinds that are *blamed on different people* — which is the only
 * distinction the published verdict needs from it.
 *
 * A refusal reason is otherwise a sentence, and a sentence cannot be ranked, filtered or counted. This can:
 * `UnmetReason` and every other refusal site map onto one of these, and [VerdictPolicy.decide] maps these
 * onto the verdict, so the chain from *what happened* to *whose problem it is* is one lookup wide.
 *
 * @author Griefed
 */
enum class PreventionCause {
    /** Ours: a failed download, a pack that would not generate, a broken loader cache, our own cap. */
    HOST,

    /** CurseForge's: `allowModDistribution=false`, so there is no URL and never will be. */
    DISTRIBUTION_LOCKED,

    /** Nobody's: upstream published nothing this loader and Minecraft can use, so no pack exists to boot. */
    UPSTREAM_UNAVAILABLE
}

/**
 * Whether the host got as far as handing a pack to the container, and if not, why.
 *
 * Separate from [BootResult] because the two failures are not the same kind of thing: staging is the
 * engine's own work and its failure is nobody's statement about the mod, while a boot's failure is exactly
 * that — the distinction [Verdict.ERROR], [Verdict.LOCKED] and [Verdict.UNVERIFIABLE] exist to preserve.
 */
sealed interface StagingOutcome {
    /** A pack was generated and handed over; whatever happened next is [BootResult]'s to report. */
    data object Staged : StagingOutcome

    /** Staging stopped before any container ran; [detail] is the operator-facing reason. */
    data class Prevented(
        /** Why the grind could not be performed, carried into the verdict so the report can show it. */
        val detail: String,
        /**
         * Whose problem [detail] describes, which is what decides the verdict.
         *
         * Defaults to [PreventionCause.HOST] because that is what every prevented grind meant before the
         * causes were told apart, so a refusal site that says nothing keeps its old, loudest reading rather
         * than quietly filing itself as nobody's fault.
         */
        val cause: PreventionCause = PreventionCause.HOST
    ) : StagingOutcome
}

/**
 * Decides the published [Verdict] from what staging did, what the boot was observed to do, and whether a
 * rule confirmed exclusion-worthiness. Pure, so the decision is testable without a container.
 *
 * @author Griefed
 */
object VerdictPolicy {

    /**
     * The verdict for one candidate.
     *
     * Order matters and encodes the ladder: a prevented grind is decided **before** any rule is consulted,
     * because nothing ran and therefore no console existed for a rule to have matched — a confirmation
     * arriving alongside a prevented grind is a caller bug and must not be laundered into evidence. Which
     * of the three prevented verdicts it becomes is [StagingOutcome.Prevented.cause]'s to say. Only then may
     * a rule confirm. What remains is decided by what the boot did, with a crash that no rule explained
     * landing on [Verdict.INCONCLUSIVE] rather than [Verdict.CONFIRMED]: a crash on its own has never been
     * evidence of sideness.
     *
     * **[declared] is accepted and deliberately never consulted.** It is here so the decision is honest
     * about what it was given rather than about what it used: the console outranks the metadata absolutely,
     * so a mod claiming server-side that reaches a client-only class is CONFIRMED, and a mod claiming
     * client-only that boots clean is CLEAR. That asymmetry is the point of booting at all — an honestly
     * declared client mod is already excludable from its metadata and costs nothing to find, while the ones
     * worth a container are those coded unclean, claiming the server and calling the client.
     *
     * @param staging         Whether a pack reached the container, and why not when it did not.
     * @param boot            What the classifier made of the boot, or `null` when none was observed despite
     *                        staging succeeding.
     * @param confirmedByRule Id of the console rule that proved exclusion-worthiness, or `null` if none matched.
     * @param declared        What the mod claims about itself; carried for the report, never for the verdict.
     */
    fun decide(
        staging: StagingOutcome,
        boot: BootResult?,
        confirmedByRule: String?,
        declared: Declaration? = null
    ): Verdict {
        if (staging is StagingOutcome.Prevented) {
            return when (staging.cause) {
                PreventionCause.HOST -> Verdict.ERROR
                PreventionCause.DISTRIBUTION_LOCKED -> Verdict.LOCKED
                PreventionCause.UPSTREAM_UNAVAILABLE -> Verdict.UNVERIFIABLE
            }
        }
        // Staged, but nothing was ever observed: the container failed to start. Late-surfacing, still an
        // operator problem, and still not a statement about the mod.
        if (boot == null) {
            return Verdict.ERROR
        }
        if (confirmedByRule != null) {
            return Verdict.CONFIRMED
        }
        return when (boot) {
            // The only outcome that proves anything good: the server reached its ready-line.
            BootResult.SURVIVED -> Verdict.CLEAR
            // A crash no rule explained, and a boot that ended without a recognised reason, say the same
            // thing — the grind happened and taught us nothing. Neither is an ERROR: the container ran.
            BootResult.CRASHED, BootResult.INCONCLUSIVE -> Verdict.INCONCLUSIVE
        }
    }
}

/**
 * One loader's folded evidence: what is published, what the mod claimed, and which rule confirmed it.
 *
 * The three travel together because a verdict is only auditable alongside the other two — [confirmedByRule]
 * is what an operator edits to revoke a confirmation, and [declared] is what makes a confirmation
 * *interesting*, since a mod claiming the server while calling client classes is the finding this engine
 * exists to produce.
 *
 * @author Griefed
 */
data class VerdictAssessment(
    /** What is published about this loader. */
    val verdict: Verdict,
    /** What the mod claims about itself, or `null` when it claimed nothing recognisable. */
    val declared: Declaration?,
    /** Id of the rule that confirmed, or `null` when nothing did. */
    val confirmedByRule: String? = null,
    /**
     * The sentence shown beside the verdict, or `null` when the verdict speaks for itself. Carries the two
     * things the verdict alone cannot: that a *contradicted* server claim is what makes a confirmation
     * interesting, and that a distribution-locked file was never readable at all.
     */
    val note: String? = null
)
