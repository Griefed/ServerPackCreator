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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * One operator-written rule matching a boot console. A newly-observed clientside signature is then a file
 * edit rather than a release, which is the point: the built-in ladder can only carry signatures somebody
 * shipped code for.
 *
 * @author Griefed
 */
data class ConsoleRule(
    /** Stable name, reported on the verdict that a match decided, so a bad rule can be found and removed. */
    val id: String,
    /** Regular expression matched against each console line, case-insensitively. */
    val pattern: String,
    /**
     * What a match means, or `null` when the rule states none.
     *
     * A rule that states nothing is *undecided*, not safe-by-default: what an undecided rule means is the
     * operator's choice, made once for the whole file via [ConsoleRuleSet.undecidedVerdict]. An **unreadable**
     * verdict is a different thing entirely — the author tried to state one and failed — and always resolves
     * to [BootResult.INCONCLUSIVE] at load time, because a typo must never be honoured as an intention.
     */
    val verdict: BootResult? = null,
    /** Why this signature means what it does, carried into the verdict detail for whoever reads it later. */
    val note: String? = null
) {
    /**
     * [pattern] compiled once, or `null` when it will not compile. Null rather than a throw because a rule
     * that cannot be applied must cost its own match and nothing else — never a boot.
     */
    internal val regex: Regex? = runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.getOrNull()

    /** The first console line this rule matches, or `null`. A rule that would not compile matches nothing. */
    internal fun firstMatch(consoleLines: List<String>): String? =
        regex?.let { compiled -> consoleLines.firstOrNull { compiled.containsMatchIn(it) } }
}

/** A rule that fired, with the console line that set it off — enough to explain a verdict to a human. */
data class ConsoleRuleMatch(
    /** The rule whose pattern matched. */
    val rule: ConsoleRule,
    /** The console line it matched, so the evidence travels with the claim. */
    val line: String
)

/**
 * A loaded set of rules plus whatever could not be loaded. [errors] is not decoration: "keep the last good
 * rules" is what stops a mid-edit save from silently disabling everything an operator wrote, and that same
 * behaviour would hide the breakage completely if nothing reported it.
 *
 * @author Griefed
 */
data class ConsoleRuleSet(
    /** The rules that loaded, in file order — which is also their precedence. */
    val rules: List<ConsoleRule>,
    /** One entry per rule (or whole file) that could not be loaded, naming it and why. */
    val errors: List<String>,
    /** Where these came from, for the status endpoint to show. */
    val source: String,
    /**
     * What a rule that states **no** verdict means, or `null` for "let the built-in ladder decide".
     *
     * `null` is the default because a rule without a verdict is *undecided*, not *unsafe*: the operator
     * wrote a pattern to label a signature, and the ladder is still the better judge of what it means.
     * Setting this to [BootResult.INCONCLUSIVE] opts into the conservative reading — no such rule can then
     * contribute to a `HIGH` — which suits a run where unfinished rules are expected.
     *
     * It deliberately does **not** govern an *unreadable* verdict. A typo is a broken intention rather than
     * an absent one, and always resolves to INCONCLUSIVE regardless of this setting.
     */
    val undecidedVerdict: BootResult? = null
) {
    companion object {
        /** No rules and no errors: exactly the behaviour this engine had before rules existed. */
        val EMPTY = ConsoleRuleSet(emptyList(), emptyList(), "none")
    }
}

/**
 * Reads [file] into a [ConsoleRuleSet], re-reading it when it changes so an operator can edit rules while
 * the daemon is running.
 *
 * **Reload is a `stat`, not a watcher.** [BootLogClassifier.classify] runs once per boot and a boot takes
 * minutes, so checking `lastModified` + `length` on read costs microseconds at a frequency far below one
 * per second — while a `WatchService` costs a thread, a platform-specific backend (on macOS the JDK's
 * default is itself a poller, so it is not even more responsive), and the whole "the editor saved via
 * rename so the watch key died" family of bugs. Its tests would need sleeps, which is how flaky suites
 * start.
 *
 * **Landmine:** the cache key is the **pair** `(lastModified, length)`, because mtime has one-second
 * granularity on some filesystems. Two edits inside one second that leave the byte count identical are
 * missed. Closing that would mean hashing the content on every call, which defeats the point; the tradeoff
 * is stated rather than hidden.
 *
 * @param file The rules document; absent means [ConsoleRuleSet.EMPTY], which is the normal install.
 * @param undecidedVerdict What a rule stating no verdict means. `null` (the default) leaves it to the
 *        built-in ladder; [BootResult.INCONCLUSIVE] opts into the conservative reading.
 * @author Griefed
 */
class ConsoleRuleFile(
    private val file: File,
    private val undecidedVerdict: BootResult? = null
) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    // Comments are enabled because a rule file is written by a human who will want to say *why* a signature
    // means what it does next to the signature itself.
    private val mapper = jacksonObjectMapper()
        .configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true)

    private var cachedKey: Pair<Long, Long>? = null
    private var cached: ConsoleRuleSet = ConsoleRuleSet.EMPTY

    /**
     * The current rules, re-parsing only when the file has changed. An unreadable file keeps the last good
     * set and reports why, so one bad save cannot silently disable every rule an operator wrote.
     */
    @Synchronized
    fun current(): ConsoleRuleSet {
        if (!file.isFile) {
            cachedKey = null
            cached = ConsoleRuleSet.EMPTY
            return cached
        }
        val key = file.lastModified() to file.length()
        if (key == cachedKey) {
            return cached
        }

        val elements = runCatching { mapper.readValue<List<JsonNode>>(file) }.getOrElse { failure ->
            log.error("Could not read the console-rule file ${file.absolutePath}; keeping the rules already loaded: ${failure.message}")
            cachedKey = key
            cached = cached.copy(errors = cached.errors + "could not read ${file.name}: ${failure.message}")
            return cached
        }

        val rules = mutableListOf<ConsoleRule>()
        val errors = mutableListOf<String>()
        elements.forEachIndexed { index, element -> readRule(element, index, rules, errors) }

        if (errors.isEmpty()) {
            log.info("Loaded ${rules.size} console rule(s) from ${file.absolutePath}.")
        } else {
            log.error("Loaded ${rules.size} console rule(s) from ${file.absolutePath}; ${errors.size} could not be used: $errors")
        }
        cachedKey = key
        cached = ConsoleRuleSet(rules, errors, file.absolutePath, undecidedVerdict)
        return cached
    }

    /**
     * Validate one entry into [rules], or record why it cannot be used in [errors].
     *
     * The rejections and the fallbacks are each deliberate, and they go in *opposite* directions. A rule
     * with no id or no pattern is **dropped** — it could never be traced back to, or could never match. A
     * verdict that is missing or unreadable **falls back to INCONCLUSIVE** rather than dropping the rule,
     * because dropping would hand the console back to a ladder that may well reach `CRASHED` on its own,
     * and `CRASHED` is the one outcome that publishes. Every fallback is still recorded in [errors]:
     * failing safe must not mean failing silently.
     */
    private fun readRule(element: JsonNode, index: Int, rules: MutableList<ConsoleRule>, errors: MutableList<String>) {
        val id = element.path("id").asText(null)?.takeIf { it.isNotBlank() }
        val pattern = element.path("pattern").asText(null)?.takeIf { it.isNotBlank() }
        val label = id ?: "rule #${index + 1}"

        if (id == null) {
            errors += "$label: no 'id', so a verdict it decided could never be traced back to it"
            return
        }
        if (pattern == null) {
            errors += "$label: no 'pattern' to match"
            return
        }
        if (!element.path("enabled").asBoolean(true)) {
            return
        }

        // An ABSENT verdict stays null: the rule is undecided, and what that means is the operator's
        // choice (ConsoleRuleSet.undecidedVerdict). An UNREADABLE one is different -- the author tried to
        // state a verdict and failed -- so it resolves to INCONCLUSIVE here regardless of that setting,
        // because a typo must never be honoured as an intention, and INCONCLUSIVE is the only outcome that
        // cannot publish. The typo is still recorded, so failing safe never means failing silently.
        val declaredVerdict = element.path("verdict").asText(null)?.takeIf { it.isNotBlank() }
        val verdict = declaredVerdict?.let { name ->
            BootResult.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: run {
                    errors += "$label: '$name' is not a boot result " +
                        "(${BootResult.entries.joinToString("/")}); treated as ${BootResult.INCONCLUSIVE}"
                    BootResult.INCONCLUSIVE
                }
        }

        val rule = ConsoleRule(id, pattern, verdict, element.path("note").asText(null)?.takeIf { it.isNotBlank() })
        if (rule.regex == null) {
            errors += "$label: '$pattern' is not a usable regular expression"
            return
        }
        rules += rule
    }
}
