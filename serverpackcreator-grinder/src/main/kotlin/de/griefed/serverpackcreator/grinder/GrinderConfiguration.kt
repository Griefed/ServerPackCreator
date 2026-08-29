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

import de.griefed.serverpackcreator.clientside.BootResult
import java.io.File
import java.time.Duration

/**
 * One environment variable the service reads, as the operator contract describes it.
 *
 * [literalDefault] is `null` for a knob whose default is *computed* — a path under the home, say — because
 * there is no fixed string for the README to quote. Everything else must appear in the README's table with
 * exactly this value, which [de.griefed.serverpackcreator.grinder.ReadmeConfigurationTest] now checks by
 * reading this list rather than by regexing Kotlin.
 *
 * @author Griefed
 */
internal data class Knob(
    /** The variable's name, exactly as the environment spells it. */
    val name: String,
    /** Its default as a literal string, or `null` when the default is computed and cannot be quoted. */
    val literalDefault: String?
)

/**
 * Every operator-facing setting, read from the environment **once**, in one place.
 *
 * **Why one place matters, and why it is this one.** The README's table and the shipped systemd unit are
 * the service's operator contract, and prose cannot be compiled — so both are guarded against drift. Those
 * guards used to *scan `GrinderApplication`'s source text* for `env("…")` calls, which worked only while
 * every read stayed inside one 294-line `main`, and which asserted the presence of a string rather than the
 * behaviour of anything. Reading the environment here instead keeps the same "one place" property while
 * making it **executable**: [KNOBS] is a real list the documentation guards iterate, and [from] takes its
 * lookup as a parameter, so a test can hand it an environment and assert what the daemon would actually do
 * with it — which no amount of grepping could.
 *
 * @author Griefed
 */
internal data class GrinderConfiguration(
    /** The daemon's own directory: everything else defaults to a path beneath it. */
    val home: File,
    /** The runtime image every boot and install container is created from. */
    val image: String,
    /** Scratch root for staging; everything below it is reclaimable by the reaper. */
    val work: File,
    /** Cached per-tuple loader installs, so a mod boot needs no network. */
    val cache: File,
    /** The verdict store. */
    val store: File,
    /** The immediate re-grind queue — operator-authored state, deliberately not under [work]. */
    val requeue: File,
    /** The crawl position per platform. */
    val cursors: File,
    /** Console, server logs and crash reports of every boot that did not survive. */
    val bootLogs: File,
    /** Ceiling for [bootLogs], in bytes; oldest attempts are dropped once it is passed. */
    val bootLogBudgetBytes: Long,
    /** The operator's console-rule file; absent means the built-in classification alone. */
    val bootRules: File,
    /** What a rule stating no verdict means: `null` lets the ladder decide. */
    val ruleFallback: BootResult?,
    /** Report server port. */
    val port: Int,
    /** Report server bind address. Loopback by default — the report is unauthenticated. */
    val host: String,
    /** Concurrent grinds; each holds a booting container, so this is really a memory decision. */
    val workers: Int,
    /** Candidates taken from the crawl per pass. */
    val batch: Int,
    /** Cores per container. `0` means uncapped. */
    val containerCpus: Double,
    /** Memory per container in GiB — also the divisor in the README's worker-sizing advice. */
    val containerMemoryGiB: Double,
    /** `uid:gid` the containers run as, or `null` to derive it from [work]'s owner. */
    val containerUser: String?,
    /** An operator-supplied `serverpackcreator.properties`, or `null` to use one inside [home]. */
    val spcProperties: String?,
    /** How long a verdict stays fresh before its project is re-ground. */
    val reverifyTtl: Duration,
    /** How long an unused cached loader install is kept. */
    val cacheTtl: Duration,
    /** Pause after a pass that completed a sweep with nothing due. */
    val betweenSweeps: Duration,
    /** Pause after a pass that found nothing due but has catalog left. */
    val whileCrawling: Duration,
    /** CurseForge API key, or `null` — without it CurseForge cannot be resolved at all. */
    val curseForgeApiKey: String?
) {
    companion object {
        /**
         * Every variable the service reads, with the default the README must quote.
         *
         * **This list is the operator contract.** Adding a knob without adding it here means the
         * documentation guards never learn about it — which is the same failure the old source-scan had,
         * only now it is one obvious line instead of a regex over Kotlin.
         */
        val KNOBS: List<Knob> = listOf(
            Knob("SPC_GRINDER_HOME", null),
            Knob("SPC_GRINDER_IMAGE", "spc-grinder-runtime:latest"),
            Knob("SPC_GRINDER_WORK", null),
            Knob("SPC_GRINDER_CACHE", null),
            Knob("SPC_GRINDER_STORE", null),
            Knob("SPC_GRINDER_REQUEUE", null),
            Knob("SPC_GRINDER_CURSORS", null),
            Knob("SPC_GRINDER_BOOT_LOGS", null),
            Knob("SPC_GRINDER_BOOT_LOG_BUDGET_MIB", "2048"),
            Knob("SPC_GRINDER_BOOT_RULES", null),
            Knob("SPC_GRINDER_RULE_FALLBACK", "grinder"),
            Knob("SPC_GRINDER_PORT", "8757"),
            Knob("SPC_GRINDER_HOST", "127.0.0.1"),
            Knob("SPC_GRINDER_WORKERS", "2"),
            Knob("SPC_GRINDER_BATCH", "25"),
            Knob("SPC_GRINDER_CPUS", "2"),
            Knob("SPC_GRINDER_MEMORY_GIB", "3"),
            Knob("SPC_GRINDER_CONTAINER_USER", null),
            Knob("SPC_GRINDER_SPC_PROPERTIES", null),
            Knob("SPC_GRINDER_REVERIFY_TTL_DAYS", "30"),
            Knob("SPC_GRINDER_CACHE_TTL_DAYS", "7"),
            Knob("SPC_GRINDER_INTERVAL", "21600"),
            Knob("SPC_GRINDER_SCAN_DELAY", "15")
        )

        /** The default home, used when `SPC_GRINDER_HOME` says nothing. */
        fun defaultHome(): File = File(System.getProperty("user.home"), ".spc-grinder")

        /**
         * Read the configuration from [lookup], which defaults to the real environment.
         *
         * Taking the lookup as a parameter is what makes this testable at all: a test hands it a map and
         * asserts what the daemon *would do*, where the previous shape could only be grepped.
         *
         * Nothing here throws on a malformed value — a bad number falls back to the documented default —
         * because a typo in a unit file should not stop a service that has verdicts to serve. The value the
         * daemon actually used is logged on the startup line either way.
         */
        fun from(lookup: (String) -> String? = System::getenv): GrinderConfiguration {
            fun text(name: String, fallback: String) =
                lookup(name)?.takeIf { it.isNotBlank() } ?: fallback

            fun optional(name: String) = lookup(name)?.takeIf { it.isNotBlank() }

            val home = File(text("SPC_GRINDER_HOME", defaultHome().path))
            fun under(name: String, child: String) = File(text(name, File(home, child).path))
            fun number(name: String, fallback: String) = text(name, fallback)

            return GrinderConfiguration(
                home = home,
                image = text("SPC_GRINDER_IMAGE", "spc-grinder-runtime:latest"),
                work = under("SPC_GRINDER_WORK", "work"),
                cache = under("SPC_GRINDER_CACHE", "cache"),
                store = under("SPC_GRINDER_STORE", "verdicts.json"),
                requeue = under("SPC_GRINDER_REQUEUE", "requeue.json"),
                cursors = under("SPC_GRINDER_CURSORS", "cursors.json"),
                bootLogs = under("SPC_GRINDER_BOOT_LOGS", "boot-logs"),
                bootLogBudgetBytes = (number("SPC_GRINDER_BOOT_LOG_BUDGET_MIB", "2048").toLongOrNull() ?: 2048L) *
                    1024 * 1024,
                bootRules = under("SPC_GRINDER_BOOT_RULES", "boot-rules.json"),
                ruleFallback = if (text("SPC_GRINDER_RULE_FALLBACK", "grinder").equals("inconclusive", true)) {
                    BootResult.INCONCLUSIVE
                } else {
                    null
                },
                port = number("SPC_GRINDER_PORT", "8757").toIntOrNull() ?: 8757,
                host = text("SPC_GRINDER_HOST", "127.0.0.1"),
                workers = number("SPC_GRINDER_WORKERS", "2").toIntOrNull() ?: 2,
                batch = number("SPC_GRINDER_BATCH", "25").toIntOrNull() ?: 25,
                containerCpus = number("SPC_GRINDER_CPUS", "2").toDoubleOrNull() ?: 2.0,
                containerMemoryGiB = number("SPC_GRINDER_MEMORY_GIB", "3").toDoubleOrNull() ?: 3.0,
                containerUser = optional("SPC_GRINDER_CONTAINER_USER"),
                spcProperties = optional("SPC_GRINDER_SPC_PROPERTIES"),
                reverifyTtl = Duration.ofDays(number("SPC_GRINDER_REVERIFY_TTL_DAYS", "30").toLongOrNull() ?: 30),
                cacheTtl = Duration.ofDays(number("SPC_GRINDER_CACHE_TTL_DAYS", "7").toLongOrNull() ?: 7),
                betweenSweeps = Duration.ofSeconds(number("SPC_GRINDER_INTERVAL", "21600").toLongOrNull() ?: 21600),
                whileCrawling = Duration.ofSeconds(number("SPC_GRINDER_SCAN_DELAY", "15").toLongOrNull() ?: 15),
                curseForgeApiKey = optional("CURSEFORGE_API_KEY")
            )
        }
    }
}
