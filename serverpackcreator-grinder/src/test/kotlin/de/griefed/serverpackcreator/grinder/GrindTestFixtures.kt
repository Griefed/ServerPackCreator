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

import de.griefed.serverpackcreator.clientside.*
import org.junit.jupiter.api.Assertions
import java.io.File
import java.time.Instant

/** Build a minimal [LoaderVerdict] for tests, defaulting the signals not under test. */
internal fun loaderVerdict(
    loader: String,
    suggestedEntry: String?,
    confidence: Confidence,
    note: String? = null,
    declaredClientSide: DeclaredSupport = DeclaredSupport.UNKNOWN,
    declaredServerSide: DeclaredSupport = DeclaredSupport.UNKNOWN,
    jarScan: JarScan = JarScan.ERROR,
    bootedLoader: String? = null
) = LoaderVerdict(
    loader = loader,
    suggestedEntry = suggestedEntry,
    declaredClientSide = declaredClientSide,
    declaredServerSide = declaredServerSide,
    jarScan = jarScan,
    bootResult = null,
    bootedLoader = bootedLoader,
    bootCrashExcerpt = null,
    confidence = confidence,
    sampleFile = null,
    note = note
)

/** Build a [ClientsideReport] from a set of per-loader verdicts. */
internal fun clientsideReport(
    slug: String,
    perLoader: List<LoaderVerdict>,
    platform: String = "Modrinth",
    projectUrl: String = "https://modrinth.com/mod/$slug"
) = ClientsideReport(
    platform = platform,
    slug = slug,
    projectUrl = projectUrl,
    phase = "metadata + server-boot",
    suggestedEntries = perLoader.mapNotNull { it.suggestedEntry }.distinct().sorted(),
    perLoader = perLoader,
    fileNames = emptyList()
)

/** Build a [GrindVerdict] for store/CSV tests. [verifiedAt] matters only for freshness/TTL tests. */
internal fun grindVerdict(
    slug: String,
    loader: String,
    confidence: Confidence = Confidence.HIGH,
    suggestedEntry: String? = "$slug-",
    projectUrl: String = "https://modrinth.com/mod/$slug",
    detail: String = "",
    platform: String = "Modrinth",
    verifiedAt: Instant = Instant.EPOCH
) = GrindVerdict(platform, slug, projectUrl, loader, suggestedEntry, confidence, detail, verifiedAt)

/** [GrinderApplication]'s source, for the guards that can only be stated against `main`'s own text. */
internal val grinderEntryPoint = File("src/main/kotlin/de/griefed/serverpackcreator/grinder/GrinderApplication.kt")

/**
 * `main`'s body and nothing else, cut by matching braces from its opening one. Asserts that the window stops
 * before the declarations that follow `main`, since a window that silently ran past them is exactly how a
 * guard built on it would keep passing while asserting nothing.
 */
internal fun grinderMainBody(): String {
    Assertions.assertTrue(grinderEntryPoint.isFile, "entry point not found at ${grinderEntryPoint.absolutePath}")
    val source = grinderEntryPoint.readText()
    val signature = source.indexOf("fun main(args: Array<String>) {")
    Assertions.assertTrue(signature > 0, "main(args) not found — did the entry point change shape?")

    val open = source.indexOf('{', signature)
    var depth = 0
    var index = open
    while (index < source.length) {
        when (source[index]) {
            '{' -> depth++
            '}' -> if (--depth == 0) break
        }
        index++
    }
    Assertions.assertTrue(depth == 0, "main's braces do not balance — the window would run to end of file")

    val body = source.substring(open + 1, index)
    Assertions.assertFalse(
        body.contains("internal fun pinSpcHomeDirectory"),
        "the window ran past main and into the declarations below it, so this guard would assert nothing"
    )
    return body
}
