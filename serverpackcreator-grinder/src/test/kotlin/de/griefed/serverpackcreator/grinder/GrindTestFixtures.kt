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

/** Build a minimal [GrindTargetVerdict] for tests, defaulting the signals not under test. */
internal fun targetVerdict(
    loader: String,
    suggestedEntry: String?,
    verdict: Verdict = Verdict.INCONCLUSIVE,
    note: String? = null,
    declaredClientSide: DeclaredSupport = DeclaredSupport.UNKNOWN,
    declaredServerSide: DeclaredSupport = DeclaredSupport.UNKNOWN,
    jarScan: JarScan = JarScan.ERROR,
    bootedLoader: String? = null,
    sampleFile: String? = null,
    minecraftLine: String = "1.20",
    minecraftVersion: String = "1.20.1"
) = GrindTargetVerdict(
    loader = loader,
    suggestedEntry = suggestedEntry,
    declaredClientSide = declaredClientSide,
    declaredServerSide = declaredServerSide,
    jarScan = jarScan,
    bootResult = null,
    bootedLoader = bootedLoader,
    bootCrashExcerpt = null,
    verdict = verdict,
    sampleFile = sampleFile,
    note = note,
    minecraftLine = minecraftLine,
    minecraftVersion = minecraftVersion
)

/** Build a [ClientsideReport] from a set of per-loader verdicts. */
internal fun clientsideReport(
    slug: String,
    perTarget: List<GrindTargetVerdict>,
    platform: String = "Modrinth",
    projectUrl: String = "https://modrinth.com/mod/$slug"
) = ClientsideReport(
    platform = platform,
    slug = slug,
    projectUrl = projectUrl,
    phase = "metadata + server-boot",
    suggestedEntries = perTarget.mapNotNull { it.suggestedEntry }.distinct().sorted(),
    perTarget = perTarget,
    fileNames = emptyList()
)

/** Build a [GrindVerdict] for store/CSV tests. [verifiedAt] matters only for freshness/TTL tests. */
internal fun grindVerdict(
    slug: String,
    loader: String,
    // Defaults to the verdict that claims nothing, so a fixture written before the redesign stands for an
    // unmigrated row rather than silently for a finding. Tests about publication state it explicitly.
    verdict: Verdict = Verdict.INCONCLUSIVE,
    suggestedEntry: String? = "$slug-",
    projectUrl: String = "https://modrinth.com/mod/$slug",
    detail: String = "",
    platform: String = "Modrinth",
    verifiedAt: Instant = Instant.EPOCH,
    // A fixture standing for a finding should stand for a LEGITIMATE one, so it defaults to the
    // decision that is decisive evidence. The publication gate's refusal of a non-decisive verdict is
    // pinned explicitly in VerdictPublicationTest rather than implied by every fixture here.
    decidedBy: String? = BootDecision.CLIENT_ONLY_CLASS.name,
    // A row's identity is its Minecraft line, so a fixture without one stands for a *legacy* row -- which is
    // a different thing and has its own guards. Defaulted here so the ordinary fixture is an ordinary row.
    minecraftLine: String? = "1.20",
    minecraftVersion: String? = "1.20.1"
) = GrindVerdict(
    platform, slug, projectUrl, loader, suggestedEntry, detail, verifiedAt,
    decidedBy = decidedBy, verdict = verdict,
    minecraftLine = minecraftLine, minecraftVersion = minecraftVersion
)

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
