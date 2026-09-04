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
    val enabled: Boolean = true
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
    /** The first rule matching any line, in order, or `null` when none does. */
    fun firstMatch(consoleLines: List<String>): BootRuleMatch? =
        rules.firstNotNullOfOrNull { rule ->
            rule.firstMatch(consoleLines)?.let { BootRuleMatch(rule, it) }
        }

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
