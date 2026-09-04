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
 * What the engine publishes about one mod, in the four states that are actually distinguishable.
 *
 * Replaces the old pairing of `BootResult` (CRASHED/SURVIVED/INCONCLUSIVE) with a separate `Confidence`
 * (HIGH/MEDIUM/LOW/INCONCLUSIVE), which conflated two questions — *what happened* and *how sure are we* —
 * and could not express the one thing the operator most needed to know: whether the grind ran at all.
 *
 * @author Griefed
 */
enum class Verdict(
    /**
     * Whether this verdict's console and artifacts are kept for later reading.
     *
     * Only [CLEAR] discards. [ERROR] and [INCONCLUSIVE] keep them because an admin reads the first and a
     * new rule is extracted from the second; [CONFIRMED] keeps them because it is what publishes a mod to
     * the fallback list, and a verdict that cannot name its own evidence cannot be audited.
     */
    val keepsLogs: Boolean
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
    ERROR(keepsLogs = true),

    /**
     * The boot **ran** and did something unexpected — non-zero exit, crash, timeout, kill — with nothing
     * confirming why. Its log is the raw material the next rule is written from.
     */
    INCONCLUSIVE(keepsLogs = true)
}

/**
 * Whether the host got as far as handing a pack to the container, and if not, why.
 *
 * Separate from [BootObservation] because the two failures are not the same kind of thing: staging is the
 * engine's own work and its failure is an operator problem, while a boot's failure is a statement about the
 * mod — the distinction [Verdict.ERROR] exists to preserve.
 */
sealed interface StagingOutcome {
    /** A pack was generated and handed over; whatever happened next is [BootObservation]'s to report. */
    data object Staged : StagingOutcome

    /** Staging stopped before any container ran; [detail] is the operator-facing reason. */
    data class Prevented(
        /** Why the grind could not be performed, carried into the verdict so the report can show it. */
        val detail: String
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
     * Order matters and encodes the ladder: a prevented grind is [Verdict.ERROR] **before** any rule is
     * consulted, because nothing ran and therefore no console existed for a rule to have matched — a
     * confirmation arriving alongside a prevented grind is a caller bug and must not be laundered into
     * evidence. Only then may a rule confirm. What remains is decided by what the boot did, with a crash
     * that no rule explained landing on [Verdict.INCONCLUSIVE] rather than [Verdict.CONFIRMED]: a crash on
     * its own has never been evidence of sideness.
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
            return Verdict.ERROR
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
    val confirmedByRule: String? = null
)
