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

import de.griefed.serverpackcreator.clientside.boot.ImageJavaRuntimes
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * What [ContainerCandidateVerifier] does with a Minecraft line whose JDK the runtime image lacks, and where its
 * CurseForge key comes from.
 *
 * The daemon drops such a line at selection: an ERROR it can never fix would be re-ground every sweep. The
 * clientside report keeps it, as `-verifyclientside` does, so a maintainer reading the issue sees which line went
 * unverified and why instead of a report silently missing it. `verify` itself needs an `ApiWrapper`, live
 * platforms and a daemon, so the decisions are executed here and their join into `verifyStaged` is read from the
 * source, the way the `*WiringTest`s read `main`.
 */
internal class ContainerCandidateVerifierLineGateTest {

    private val imageJava = ImageJavaRuntimes(
        requiredJavaMajor = mapOf("1.20.1" to 17, "26.3" to 26)::get,
        bundledMajors = setOf(17)
    )

    private val verifierSource =
        File("src/main/kotlin/de/griefed/serverpackcreator/grinder/ContainerCandidateVerifier.kt")

    @Test
    fun theDaemonDropsALineWhoseJdkTheImageLacks() {
        val gate = ContainerCandidateVerifier.minecraftGate(imageJava, keepUnbootableLines = false)

        Assertions.assertTrue(gate("1.20.1"))
        Assertions.assertFalse(gate("26.3"), "the daemon must never select a line it cannot boot")
        Assertions.assertFalse(gate("99.9"), "nor one whose Java requirement is unknown")
    }

    @Test
    fun theClientsideReportKeepsEveryLineSoTheRefusalReachesTheReader() {
        val gate = ContainerCandidateVerifier.minecraftGate(imageJava, keepUnbootableLines = true)

        Assertions.assertTrue(gate("26.3"), "kept, so the boot's refusal becomes an ERROR row naming the missing Java")
        Assertions.assertTrue(gate("99.9"))
    }

    @Test
    fun aKeptLineIsRefusedBeforeAnyInstallWithAMessageNamingTheMissingJava() {
        Assertions.assertEquals("/opt/java-17/bin/java", ContainerCandidateVerifier.bundledJavaPath(imageJava, "1.20.1"))

        val unbundled = Assertions.assertThrows(IllegalStateException::class.java) {
            ContainerCandidateVerifier.bundledJavaPath(imageJava, "26.3")
        }.message.orEmpty()
        Assertions.assertTrue(
            "Minecraft 26.3 requires Java 26" in unbundled && "runtime image" in unbundled,
            "the ERROR row must say which Java is missing and where: $unbundled"
        )

        val unknown = Assertions.assertThrows(IllegalStateException::class.java) {
            ContainerCandidateVerifier.bundledJavaPath(imageJava, "99.9")
        }.message.orEmpty()
        Assertions.assertTrue(
            "does not know which Java Minecraft 99.9 requires" in unknown,
            "an unknown requirement is a metadata gap, not a missing JDK, and must read as one: $unknown"
        )
    }

    @Test
    fun theVerifierAppliesTheGateAndTheRefusalItIsTestedWith() {
        val source = withoutComments(verifierSource.readText())

        Assertions.assertTrue(
            "minecraftAcceptable = minecraftGate(imageJava, keepUnbootableLines)" in source,
            "verifyStaged must hand BootVerifier the gate pinned above, or the flag decides nothing"
        )
        Assertions.assertTrue(
            "bundledJavaPath(imageJava, pack.minecraftVersion)" in source,
            "overlayLoaderInstall must refuse through bundledJavaPath, or a kept line's ERROR names nothing"
        )
    }

    /**
     * The key is read through `CurseForgeApiKey`, which honours `CURSEFORGE_API_KEY_FILE` and deletes that file,
     * so a caller that hands the key over in a file keeps it out of every environment. The daemon passes its
     * configured key explicitly, so its verifier and its CurseForge source can never disagree about it.
     */
    @Test
    fun theCurseForgeKeyIsReadThroughTheReaderThatHonoursAKeyFile() {
        val source = withoutComments(verifierSource.readText())

        Assertions.assertTrue(
            "curseForgeApiKey: String? = CurseForgeApiKey.fromEnvironment" in source,
            "the verifier's default key must come from CurseForgeApiKey"
        )
        Assertions.assertFalse(
            "System.getenv(\"CURSEFORGE_API_KEY\")" in source,
            "a raw environment read ignores CURSEFORGE_API_KEY_FILE"
        )
        Assertions.assertTrue(
            Regex("""ContainerCandidateVerifier\((.*?)\n\s*\)""", RegexOption.DOT_MATCHES_ALL)
                .find(grinderMainBody())?.groupValues?.get(1).orEmpty()
                .contains("curseForgeApiKey = config.curseForgeApiKey"),
            "the daemon's verifier must use the key its CurseForge source uses"
        )
    }
}
