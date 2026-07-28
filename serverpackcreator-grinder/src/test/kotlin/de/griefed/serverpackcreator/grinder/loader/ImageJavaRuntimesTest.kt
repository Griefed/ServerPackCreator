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
        "1.12.2" to 8, "1.20.1" to 17, "1.20.6" to 21, "1.21.4" to 21, "26.2" to 25
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
    }

    @Test
    fun supportsOnlyVersionsWhoseRequiredJavaIsBundled() {
        Assertions.assertTrue(runtimes.supports("1.20.6"), "Java 21 is bundled")
        Assertions.assertFalse(runtimes.supports("26.2"), "Java 25 is not in this set — must be skipped, not booted on 21")
    }

    @Test
    fun bundlingTheRequiredJavaMakesTheVersionSupported() {
        // The shipped default bundles 25, so the current release (26.2 -> Java 25) boots rather than being skipped.
        val withJava25 = ImageJavaRuntimes(requiredJavaMajor = { declaredRequiredJava[it] }, bundledMajors = setOf(8, 17, 21, 25))
        Assertions.assertTrue(withJava25.supports("26.2"))
        Assertions.assertEquals("/opt/java-25/bin/java", withJava25.javaPath("26.2"))
    }

    @Test
    fun isUnsupportedWhenTheRequiredJavaIsUnknown() {
        Assertions.assertFalse(runtimes.supports("99.99"), "unknown required-Java → not bootable")
        Assertions.assertNull(runtimes.javaPath("99.99"))
        Assertions.assertNull(runtimes.javaPath("26.2"), "no bundled JDK → no path")
    }
}
