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
package de.griefed.serverpackcreator.grinder.loader

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the image-supported-Java gate: a Minecraft version is bootable only when SPC's declared
 * required-Java is both known *and* bundled in the image — the guard that stops a too-new Minecraft
 * (whose Java the image lacks) from being mis-scored as a clientside crash.
 */
internal class ImageJavaRuntimesTest {

    /** Stand-in for SPC's authoritative `MinecraftMeta.requiredJavaVersion`, keyed by version. */
    private val declaredRequiredJava = mapOf(
        "1.12.2" to 8, "1.20.1" to 17, "1.20.6" to 21,
        "1.21.1" to 21, "1.21.4" to 21, "1.21.11" to 21, "26.2" to 25
    )
    private val runtimes = ImageJavaRuntimes(
        requiredJavaMajor = { declaredRequiredJava[it] },
        bundledMajors = setOf(8, 17, 21)
    )

    @Test
    fun resolvesTheBundledJdkForASupportedVersion() {
        Assertions.assertEquals("/opt/java-8/bin/java", runtimes.javaPath("1.12.2"))
        Assertions.assertEquals("/opt/java-17/bin/java", runtimes.javaPath("1.20.1"))
        Assertions.assertEquals("/opt/java-21/bin/java", runtimes.javaPath("1.21.4"))
        // The 1.21 line, including a two-digit patch, must resolve like any other bundled-Java version.
        Assertions.assertEquals("/opt/java-21/bin/java", runtimes.javaPath("1.21.1"))
        Assertions.assertEquals("/opt/java-21/bin/java", runtimes.javaPath("1.21.11"))
    }

    @Test
    fun supportsOnlyVersionsWhoseRequiredJavaIsBundled() {
        Assertions.assertTrue(runtimes.supports("1.20.6"), "Java 21 is bundled")
        Assertions.assertTrue(runtimes.supports("1.21.1"), "Java 21 is bundled")
        Assertions.assertTrue(runtimes.supports("1.21.11"), "Java 21 is bundled")
        Assertions.assertFalse(runtimes.supports("26.2"), "Java 25 is not in this set — must be skipped, not booted on 21")
    }

    @Test
    fun bundlingTheRequiredJavaMakesTheVersionSupported() {
        // The shipped default bundles 25, so the current release (26.2 -> Java 25) boots rather than being skipped.
        val withJava25 = ImageJavaRuntimes(requiredJavaMajor = { declaredRequiredJava[it] }, bundledMajors = setOf(8, 17, 21, 25))
        Assertions.assertTrue(withJava25.supports("26.2"))
        Assertions.assertEquals("/opt/java-25/bin/java", withJava25.javaPath("26.2"))
    }

    /**
     * The installer JDK is independent of the server's: modloader installers can need a newer Java than
     * the Minecraft they install (Quilt's needs 17+ while 1.16.1 runs on 8), so this picks the newest
     * bundled major at or above the minimum — and reports nothing when the image ships nothing new enough.
     */
    @Test
    fun installerJavaPathPicksTheNewestBundledJdkAtOrAboveTheMinimum() {
        // This fixture bundles 8/17/21, so the newest satisfying the Java-17 minimum is 21.
        Assertions.assertEquals("/opt/java-21/bin/java", runtimes.installerJavaPath())
        val onlyJava8 = ImageJavaRuntimes(requiredJavaMajor = { 8 }, bundledMajors = setOf(8))
        Assertions.assertNull(
            onlyJava8.installerJavaPath(),
            "an image bundling only Java 8 cannot run an installer that needs 17+"
        )
        val withJava17Only = ImageJavaRuntimes(requiredJavaMajor = { 8 }, bundledMajors = setOf(8, 17))
        Assertions.assertEquals("/opt/java-17/bin/java", withJava17Only.installerJavaPath())
    }

    /**
     * The override is supplied *only* where the server's own Java is too old for the installers. A modern
     * Minecraft needs none, so the pack is left without `JAVA_INSTALLER` — which keeps the templates'
     * plain-`JAVA` fallback on the path the matrix actually boots, instead of it going untested.
     */
    @Test
    fun installerOverrideIsSuppliedOnlyWhereTheServerJavaIsTooOld() {
        Assertions.assertEquals("/opt/java-21/bin/java", runtimes.installerJavaPathFor("1.12.2"), "Java 8 server needs an override")
        Assertions.assertNull(runtimes.installerJavaPathFor("1.20.1"), "Java 17 server already satisfies the installers")
        Assertions.assertNull(runtimes.installerJavaPathFor("1.21.11"), "Java 21 server already satisfies the installers")
        Assertions.assertNull(runtimes.installerJavaPathFor("99.99"), "unknown version -> no claim either way")
    }

    @Test
    fun isUnsupportedWhenTheRequiredJavaIsUnknown() {
        Assertions.assertFalse(runtimes.supports("99.99"), "unknown required-Java → not bootable")
        Assertions.assertNull(runtimes.javaPath("99.99"))
        Assertions.assertNull(runtimes.javaPath("26.2"), "no bundled JDK → no path")
    }
}
