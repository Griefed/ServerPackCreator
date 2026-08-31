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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.clientside.BootDecision
import de.griefed.serverpackcreator.clientside.BootLogClassifier
import de.griefed.serverpackcreator.clientside.ConsoleRuleFile
import de.griefed.serverpackcreator.clientside.ConsoleRuleSet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Grades a **live** grinder's published verdicts against their own evidence.
 *
 * **Why this exists as a committed test rather than as a one-off census.** Sample-and-fix has run twice — a
 * 200-log census on 2026-08-29, then a merge gate reporting `HIGH 8 → 4` — and both times the same class of
 * defect shipped again, because nothing re-checked the *published list* against the consoles behind it. The
 * censuses were ad-hoc and were never committed, so the loop could not close. This does close it: it asks
 * one question of the whole store at once, and the answer is either an assertion or a distribution a human
 * can read.
 *
 * The question is: **is every published `HIGH` backed by a decision `BootDecision.decisive` marks?** A
 * `CRASHED` from the bare exit-code rung means only *"the process exited non-zero and nothing recognised
 * why"*, and on 2026-08-31 four of five sampled logs were exactly that — with one of the mods already in the
 * served fallback list.
 *
 * Gated, because it needs network and politely hits someone's live daemon:
 * ```
 * GRINDER_AUDIT_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*GrinderAuditIT"
 * ```
 * `SPC_GRINDER_AUDIT_URL` points it elsewhere (default `https://grinder.serverpackcreator.de`),
 * `SPC_GRINDER_AUDIT_SAMPLE` caps how many consoles are fetched (default 200), and
 * `SPC_GRINDER_BOOT_RULES` is honoured so the audit classifies with the same rules the daemon would.
 *
 * **Gotcha with that last one:** a Gradle test JVM's working directory is the *module* directory, so a
 * relative path is relative to `serverpackcreator-grinder/`. `deploy/boot-rules.example.json` is right;
 * prefixing it with the module name silently finds nothing, and the audit then reports
 * `0 rule(s) from none` rather than failing — which is easy to miss in the header line.
 *
 * @author Griefed
 */
internal class GrinderAuditIT {

    private val baseUrl = System.getenv("SPC_GRINDER_AUDIT_URL")?.trimEnd('/') ?: "https://grinder.serverpackcreator.de"
    private val sampleSize = System.getenv("SPC_GRINDER_AUDIT_SAMPLE")?.toIntOrNull() ?: 200

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /** One kept artifact and the tuple it belongs to, parsed from a stored name. */
    private data class Artifact(val name: String, val owner: String)

    @Test
    fun everyPublishedHighIsBackedByDecisiveEvidence() {
        Assumptions.assumeTrue(System.getenv("GRINDER_AUDIT_IT") == "1", "set GRINDER_AUDIT_IT=1 to audit a live grinder")

        val rules = System.getenv("SPC_GRINDER_BOOT_RULES")
            ?.let { ConsoleRuleFile(File(it)).current() }
            ?: ConsoleRuleSet.EMPTY
        println("[audit] $baseUrl, sample $sampleSize, ${rules.rules.size} rule(s) from ${rules.source}")

        val highs = highConfidenceTuples()
        Assumptions.assumeFalse(highs.isEmpty(), "the store published no HIGH verdicts, so there is nothing to grade")
        println("[audit] ${highs.size} HIGH verdict(s) in the store")

        // GROUPED BY TUPLE, not by artifact. A candidate is booted several times -- the first attempt, the
        // newest-build re-check, each other-version re-check -- and every non-survived attempt keeps its own
        // console, so a tuple commonly has two or three. Grading artifacts would count one verdict repeatedly
        // and, worse, count a re-check attempt against a verdict some *other* attempt decided. A verdict is
        // defensible if ANY of its kept consoles carries decisive evidence, which is the charitable reading
        // and the only one that matches what a verdict means.
        val perTuple = consoleArtifacts().filter { it.owner in highs }.groupBy { it.owner }
        val tuples = perTuple.keys.sorted().take(sampleSize)
        Assumptions.assumeFalse(tuples.isEmpty(), "no kept console belongs to a published HIGH")
        println("[audit] grading ${tuples.size} published HIGH verdict(s) over ${perTuple.filterKeys { it in tuples }.values.sumOf { it.size }} console(s)")

        val best = LinkedHashMap<String, BootDecision>()
        for (tuple in tuples) {
            for (artifact in perTuple.getValue(tuple)) {
                val console = fetch("/boot-log?name=${enc(artifact.name)}") ?: continue
                // The stored exit status is not published, and every rung a HIGH can legitimately come from is
                // decided on the console alone. A non-zero exit is assumed so the exit-code rung stays
                // reachable, and therefore counted -- assuming zero would quietly reclassify the very
                // population being audited.
                val decided = BootLogClassifier.classify(console.lines(), exitCode = 1, timedOut = false, rules).decidedBy
                val held = best[tuple]
                if (held == null || (!held.decisive && decided.decisive)) {
                    best[tuple] = decided
                }
            }
        }

        val byDecision = best.entries.groupBy({ it.value }, { it.key })
        println("[audit] decision distribution, one entry per published HIGH verdict:")
        byDecision.entries
            .sortedByDescending { it.value.size }
            .forEach { (decision, owners) ->
                println("[audit]   ${owners.size.toString().padStart(4)}  $decision${if (decision.decisive) "" else "  <-- not evidence"}")
            }

        val undefensible = byDecision.filterKeys { !it.decisive }.values.flatten().sorted()
        Assertions.assertTrue(
            undefensible.isEmpty(),
            "${undefensible.size} of ${best.size} published HIGH verdict(s) rest on no decisive evidence. " +
                "Each is a mod being stripped from every server pack built against this list, for a crash that " +
                "says nothing about sideness. First few: ${undefensible.take(10)}"
        )
    }

    /** The `(platform, slug, loader)` tuples the store publishes as `HIGH`, read from the CSV export. */
    private fun highConfidenceTuples(): Set<String> {
        val csv = fetch("/export.csv") ?: return emptySet()
        val rows = csv.lines().filter { it.isNotBlank() }
        val header = rows.firstOrNull()?.split(",") ?: return emptySet()
        val name = header.indexOf("Name")
        val confidence = header.indexOf("Confidence")
        val loader = header.indexOf("Loader")
        val platform = header.indexOf("Platform")
        if (listOf(name, confidence, loader, platform).any { it < 0 }) {
            println("[audit] the CSV header does not carry the columns this audit needs: $header")
            return emptySet()
        }
        return rows.drop(1)
            .map { splitCsv(it) }
            .filter { it.getOrNull(confidence) == "HIGH" }
            .mapNotNull { row ->
                val parts = listOf(row.getOrNull(platform), row.getOrNull(name), row.getOrNull(loader))
                if (parts.any { it.isNullOrBlank() }) null else parts.joinToString("-")
            }
            .toSet()
    }

    /**
     * Every kept `console.log`, with the tuple it belongs to.
     *
     * `/boot-logs` is an HTML page — there is no machine-readable listing — so the names are scraped out of
     * its `?name=` hrefs. A stored name is `<tuple>~<attempt>~<artifact>`, and the tuple is everything before
     * the first `~`, which is safe because slugs and loaders contain `-` but never `~`.
     */
    private fun consoleArtifacts(): List<Artifact> {
        val index = fetch("/boot-logs") ?: return emptyList()
        return Regex("""\?name=([^"'&]+)""").findAll(index)
            .map { URLDecoder.decode(it.groupValues[1], StandardCharsets.UTF_8) }
            .filter { it.endsWith("~console.log") }
            .map { Artifact(it, it.substringBefore('~')) }
            .distinctBy { it.name }
            .toList()
    }

    /** RFC-4180-enough split: honours quoted fields, which `Detail` routinely needs. */
    private fun splitCsv(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                quoted && char == '"' && line.getOrNull(index + 1) == '"' -> { current.append('"'); index++ }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> { fields.add(current.toString()); current.setLength(0) }
                else -> current.append(char)
            }
            index++
        }
        fields.add(current.toString())
        return fields
    }

    private fun enc(value: String) = URI(null, null, null, -1, null, "name=$value", null).rawQuery.removePrefix("name=")

    /** One GET, or `null` on anything other than a 200 — a missing artifact must not fail the whole audit. */
    private fun fetch(path: String): String? {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl$path"))
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "ServerPackCreator-GrinderAuditIT")
            .GET()
            .build()
        return runCatching {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) response.body() else null
        }.getOrNull()
    }
}
