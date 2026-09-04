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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Which stream of text a rule's pattern is matched against.
 *
 * @author Griefed
 */
enum class RuleSource {
    /** The boot's console output, one line at a time — the ladder the classifier walks. */
    CONSOLE,

    /**
     * The single canonical facts line [MetadataFacts] renders, describing what the platform and the jar
     * *declare* about a mod before anything is booted.
     */
    METADATA
}

/**
 * What a mod's own metadata *claims* about the side it belongs on — a self-report, never a verdict.
 *
 * Deliberately a separate vocabulary from [Verdict]. A metadata rule cannot reach CONFIRMED, CLEAR or ERROR,
 * because a declaration is the unreliable half of the evidence and is the entire reason the expensive boot
 * exists. Giving the two streams one codomain is what would let a self-report be published as a finding.
 *
 * @author Griefed
 */
enum class Declaration {
    /** The mod says it is client-only — the platform marks the server unsupported, or the jar says client. */
    CLIENT,

    /** The mod says it runs on a server. The interesting case when the console then disagrees. */
    SERVER,

    /** The platform and the jar disagree, so the mod declared nothing usable. */
    CONTRADICTORY
}

/**
 * Renders what is declared about a mod into one line for [RuleSource.METADATA] rules to match.
 *
 * **One line, deliberately.** A regex matches a line at a time, so facts on separate lines could never
 * express a conjunction — and the most careful judgment the old hardcoded fold made was exactly that: the
 * platform marking the server unsupported *while* the jar declares server/both is a contradiction, and the
 * case where confidence must fall rather than rise. With every fact on one line a pattern naming two fields
 * is an AND, and that caution survives into the rules.
 *
 * The field names are an interface operators write patterns against; `MetadataRuleTest` pins them, because a
 * rename would present as "nothing is clientside any more" rather than as a break.
 *
 * @author Griefed
 */
object MetadataFacts {

    /**
     * The facts line for one candidate: what the platform declares of each side, and what reading the jar's
     * own descriptor concluded. Values are lower-cased enum names, so they read as they are written in the
     * platforms' own vocabulary.
     */
    fun line(serverSide: DeclaredSupport, clientSide: DeclaredSupport, jarScan: JarScan): String =
        "platform_server=${serverSide.name.lowercase()} " +
            "platform_client=${clientSide.name.lowercase()} " +
            "manifest=${manifestValue(jarScan)}"

    /**
     * The jar-scan's outcome as a rule-facing word. [JarScan.SERVER_OR_BOTH] becomes `server_or_both` and
     * [JarScan.DEFERRED] `deferred` — the latter mattering because a distribution-locked file could be
     * neither scanned nor booted, so nothing is known about it and no rule may confirm from it.
     */
    private fun manifestValue(jarScan: JarScan): String = when (jarScan) {
        JarScan.CLIENT -> "client"
        JarScan.SERVER_OR_BOTH -> "server_or_both"
        JarScan.DEFERRED -> "deferred"
        else -> jarScan.name.lowercase()
    }
}

/**
 * One console signature and what matching it means.
 *
 * The redesign's rule, distinct from the older `ConsoleRule` because it speaks [Verdict] rather than
 * `BootResult`; the two coexist only until the consumers are migrated off the old vocabulary.
 *
 * @author Griefed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BootRule(
    /**
     * Stable identifier, reported on every verdict this rule decided. It is how a rule that fires too
     * broadly is found by counting, and how an operator refers to one they want changed.
     */
    val id: String = "",
    /** Regular expression matched case-insensitively against each console line, one line at a time. */
    val pattern: String = "",
    /**
     * What a match means. `null` leaves the decision to the caller and lets the rule merely name itself,
     * which is how a signature still being judged gets labelled without yet deciding anything.
     */
    val verdict: Verdict? = null,
    /** Why this signature means what it does. Carried into the verdict detail an operator reads. */
    val note: String? = null,
    /** `false` parks a rule without deleting it, so a suspected over-match can be tested by removal. */
    val enabled: Boolean = true,
    /**
     * Which stream this rule reads. Defaults to [RuleSource.CONSOLE], so an operator's existing console
     * rule needs no edit and a rule that forgets the field reads the console rather than silently matching
     * nothing.
     */
    val source: RuleSource = RuleSource.CONSOLE,
    /**
     * For a [RuleSource.METADATA] rule, what the mod thereby claims about itself.
     *
     * Separate from [verdict] on purpose, and a metadata rule may only ever set this one: a declaration is a
     * self-report, so it may inform a report and never decide a verdict. `ConsoleOutranksMetadataTest` fails
     * the build if a metadata rule carries a verdict, because that regression would be silent — the file
     * would simply start publishing mods that were never booted.
     */
    val declares: Declaration? = null
) {
    /**
     * The compiled pattern, or `null` when it does not compile.
     *
     * Compiled once here rather than per line: a rule set is consulted for every line of every boot, and an
     * unparseable pattern must be inert rather than throwing mid-classification.
     */
    internal val regex: Regex? = runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.getOrNull()

    /** The first line this rule matches, or `null` — including when it is disabled or does not compile. */
    fun firstMatch(consoleLines: List<String>): String? {
        if (!enabled) {
            return null
        }
        val compiled = regex ?: return null
        return consoleLines.firstOrNull { compiled.containsMatchIn(it) }
    }
}

/** A rule that fired, and the console line that made it fire. */
data class BootRuleMatch(
    /** The rule whose pattern matched. */
    val rule: BootRule,
    /** The console line it matched, quoted into the verdict so a reader can see the evidence. */
    val line: String
)

/**
 * An ordered set of [BootRule]s. **Order is precedence** — the first rule to match decides — which is how
 * the file reproduces the ladder the classifier used to hold in code.
 *
 * @author Griefed
 */
data class BootRuleSet(
    /** The rules, in precedence order. */
    val rules: List<BootRule>,
    /** Rules that had to be dropped, phrased for an operator; surfaced rather than silently swallowed. */
    val errors: List<String>,
    /** Where these came from, so a report can say whether defaults or an operator's file decided. */
    val source: String
) {
    /**
     * The first rule of [source] matching any of [lines], in order, or `null` when none does.
     *
     * Scoped by source so the two streams cannot decide each other's questions: a metadata rule must never
     * be tried against a console, nor a console rule against the facts line. Defaults to
     * [RuleSource.CONSOLE], which is the stream every caller before stage 3 meant.
     */
    fun firstMatch(lines: List<String>, source: RuleSource = RuleSource.CONSOLE): BootRuleMatch? =
        rules.asSequence()
            .filter { it.source == source }
            .firstNotNullOfOrNull { rule -> rule.firstMatch(lines)?.let { BootRuleMatch(rule, it) } }

    companion object {
        /** Nothing matches; used where a caller deliberately classifies without rules. */
        val EMPTY = BootRuleSet(emptyList(), emptyList(), "none")

        /**
         * Parse [json] into a rule set, dropping anything unusable rather than failing the boot.
         *
         * A rule without an id or pattern, one whose pattern does not compile, and a duplicate id are all
         * *reported* in [errors] and left out. Dropping beats throwing because this runs per boot on a live
         * service: one bad edit must cost the operator that rule and a message, not every verdict.
         */
        fun parse(json: String, source: String): BootRuleSet {
            val parsed = runCatching { jacksonObjectMapper().readValue<List<BootRule>>(json) }
            val failure = parsed.exceptionOrNull()
            if (failure != null) {
                return BootRuleSet(emptyList(), listOf("Could not read $source: ${failure.message}"), source)
            }
            val errors = mutableListOf<String>()
            val kept = mutableListOf<BootRule>()
            val seenIds = mutableSetOf<String>()
            parsed.getOrDefault(emptyList()).forEachIndexed { index, rule ->
                when {
                    rule.id.isBlank() -> errors.add("Rule at position $index has no id and was dropped.")
                    rule.pattern.isBlank() -> errors.add("Rule '${rule.id}' has no pattern and was dropped.")
                    rule.regex == null -> errors.add("Rule '${rule.id}' has an invalid pattern and was dropped.")
                    !seenIds.add(rule.id) -> errors.add("Rule '${rule.id}' is a duplicate id and was dropped.")
                    else -> kept.add(rule)
                }
            }
            return BootRuleSet(kept, errors, source)
        }
    }
}

/**
 * The rule set shipped in the jar — the whole built-in ladder, in precedence order, as ordinary editable
 * rules.
 *
 * **This file is the ladder.** Its order is the ladder's order: the "never got a fair run" guards first, the
 * decisive client-only evidence next, the excuses last. Re-ordering it re-orders the engine's judgment, and
 * the two ways that goes wrong are both pinned in `DefaultBootRulesTest` — an excuse above the evidence
 * discards true positives, and the evidence above the fair-run guards publishes host trouble as a mod's fault.
 *
 * @author Griefed
 */
object DefaultBootRules {

    /** Resource path of the shipped defaults, packaged from `src/main/resources`. */
    private const val RESOURCE = "/boot-rules.default.json"

    /**
     * The bundled ladder, parsed once.
     *
     * A missing or unreadable resource yields an empty set carrying the reason, never an exception: the
     * classifier still has its structural readings (ready-line, timeout, exit code), so a broken jar degrades
     * to "no console rules" rather than to no verdicts at all.
     */
    private val bundled: BootRuleSet by lazy {
        val json = runCatching {
            DefaultBootRules::class.java.getResourceAsStream(RESOURCE)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (json == null) {
            BootRuleSet(emptyList(), listOf("Bundled rules $RESOURCE could not be read."), RESOURCE)
        } else {
            BootRuleSet.parse(json, RESOURCE)
        }
    }

    /** The shipped ladder. */
    fun bundled(): BootRuleSet = bundled
}
