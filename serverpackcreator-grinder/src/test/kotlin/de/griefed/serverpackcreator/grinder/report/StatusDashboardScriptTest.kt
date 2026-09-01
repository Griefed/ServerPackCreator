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
package de.griefed.serverpackcreator.grinder.report

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * **Executes** the dashboard's JavaScript, rather than asserting that certain substrings appear in it.
 *
 * The page is a string constant in a Kotlin file: nothing compiles it, so a typo ships a dashboard that
 * loads, polls, and renders nothing at all. That is the same class of silent failure the shipped shell
 * templates have, and it gets the same treatment `ScriptTemplateContentTest` gives them — run the real
 * interpreter when the host has one, skip when it does not, so CI never needs the toolchain.
 *
 * Two of the page's helpers are pure and carry the logic worth pinning: `duration`, which is the whole
 * "human readable" claim, and `safeHref`, which is the only place a value reaches an HTML attribute.
 * `safeHref` earned its guards by being wrong: parsed against `window.location.origin`, a null or
 * unparseable `projectUrl` resolved to a same-origin link like `<report>/null`, so a worker row rendered
 * something that looked like a project link and led to a 404 on the report itself.
 */
internal class StatusDashboardScriptTest {

    /** The `<script>` body of the page, which is what a browser would actually run. */
    private fun script(): String =
        StatusDashboardRenderer.toHtml().substringAfter("<script>").substringBeforeLast("</script>")

    /** Whether this host can run the checks at all — CI is not required to have node. */
    private fun node(): String? =
        System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .map { File(it, "node") }
            .firstOrNull { it.canExecute() }
            ?.absolutePath

    /** Run [source] under node, returning exit status and combined output. */
    private fun run(node: String, source: File): Pair<Int, String> {
        val process = ProcessBuilder(node, source.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        Assertions.assertTrue(process.waitFor(60, TimeUnit.SECONDS), "node did not finish")
        return process.exitValue() to output
    }

    /**
     * The page's script parses. Cheapest possible guard against the failure that a string constant cannot
     * otherwise report: a stray brace or a bad token leaves the whole dashboard inert with no error anywhere
     * a build can see.
     */
    @Test
    fun theDashboardScriptParses(@TempDir dir: File) {
        val node = node()
        Assumptions.assumeTrue(node != null, "no node on PATH; skipping the dashboard script check")
        val nodeBinary = node!!

        val file = File(dir, "page.js").apply { writeText(script()) }
        val process = ProcessBuilder(nodeBinary, "--check", file.absolutePath).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(60, TimeUnit.SECONDS)

        Assertions.assertEquals(0, process.exitValue(), "the dashboard's script does not parse:\n$output")
    }

    /**
     * `duration` is the "human readable" part of the request, so it is asserted at every scale it switches
     * behaviour at — including the two edges that produce nonsense if got wrong: a null field (the daemon
     * reports several as null before a pass has run) and a negative, which a clock adjustment can produce.
     */
    @Test
    fun theDurationHelperRendersEveryScale(@TempDir dir: File) {
        val node = node()
        Assumptions.assumeTrue(node != null, "no node on PATH; skipping the dashboard script check")
        val nodeBinary = node!!

        val probe = File(dir, "duration.js").apply {
            writeText(harness() + """
                check("0s",        duration(0));
                check("45s",       duration(45));
                check("4m 23s",    duration(263));
                check("2h 1m",     duration(7301));
                check("2d 3h 2m",  duration(183742));
                check("—",         duration(null));
                check("—",         duration(undefined));
                check("0s",        duration(-5));
                console.log("OK");
            """.trimIndent())
        }

        val (status, output) = run(nodeBinary, probe)
        Assertions.assertEquals(0, status, output)
        Assertions.assertTrue(output.contains("OK"), output)
    }

    /**
     * `safeHref` is the page's only attribute sink, and this server is unauthenticated, so both directions
     * are pinned: a real project link survives, and everything else yields no link at all rather than a
     * plausible-looking one. `javascript:` and `data:` are the injection cases; the relative ones are the
     * regression that a base URL re-introduces.
     */
    @Test
    fun theLinkHelperOnlyAcceptsAbsoluteHttpUrls(@TempDir dir: File) {
        val node = node()
        Assumptions.assumeTrue(node != null, "no node on PATH; skipping the dashboard script check")
        val nodeBinary = node!!

        val probe = File(dir, "href.js").apply {
            writeText(harness() + """
                check("https://modrinth.com/mod/x", safeHref("https://modrinth.com/mod/x"));
                check("http://a.b/c",               safeHref("http://a.b/c"));
                check(null, safeHref("javascript:alert(1)"));
                check(null, safeHref("data:text/html,x"));
                check(null, safeHref("vbscript:x"));
                check(null, safeHref("::::"));
                check(null, safeHref(""));
                check(null, safeHref(null));
                check(null, safeHref(undefined));
                check(null, safeHref("/relative/path"));
                console.log("OK");
            """.trimIndent())
        }

        val (status, output) = run(nodeBinary, probe)
        Assertions.assertEquals(0, status, output)
        Assertions.assertTrue(output.contains("OK"), output)
    }

    /**
     * The page's own script with its IIFE wrapper removed, plus the handful of browser globals it touches at
     * load time, so its pure helpers can be called directly. Taken from the shipped page rather than copied,
     * which is the point — a copy would pass while the page was broken.
     */
    private fun harness(): String {
        val body = script().substringAfter("\"use strict\";").substringBeforeLast("})();")
        return """
            globalThis.window = { location: { origin: "http://127.0.0.1:8080" } };
            globalThis.document = { getElementById: function () {
                return { addEventListener: function () {}, value: "5", textContent: "", className: "" };
            } };
            globalThis.fetch = function () { return { then: function () { return this; }, catch: function () {} }; };
            globalThis.setInterval = function () { return 0; };
            globalThis.clearInterval = function () {};
            function check(expected, actual) {
                if (actual !== expected) {
                    console.error("expected " + JSON.stringify(expected) + " but got " + JSON.stringify(actual));
                    process.exit(1);
                }
            }
            $body
        """.trimIndent()
    }
}
