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

import de.griefed.serverpackcreator.api.versionmeta.minecraft.MinecraftMeta
import de.griefed.serverpackcreator.grinder.loader.ImageSupport.JDK_NOT_BUNDLED
import de.griefed.serverpackcreator.grinder.loader.ImageSupport.REQUIREMENT_UNKNOWN

/**
 * Whether the runtime image can boot a given Minecraft version, and when it cannot, **why**.
 *
 * The two failing cases are deliberately separate. [JDK_NOT_BUNDLED] is a decision — the image ships a fixed JDK
 * set and this version needs one outside it — so reporting the version as skipped is accurate. [REQUIREMENT_UNKNOWN]
 * is an *absence of information*: SPC could not tell us what the version needs, which
 * `MinecraftServer.javaVersion()` also returns when the per-version manifest is missing and its download fails.
 * Presenting that as "not applicable" is how the newest Minecraft versions quietly dropped out of the template
 * matrix while it still reported green.
 */
enum class ImageSupport {
    /** The required Java major is known and bundled — the version can be booted. */
    SUPPORTED,

    /** The required Java major is known, but this image does not ship it. A legitimate, permanent exclusion. */
    JDK_NOT_BUNDLED,

    /** Nothing is known about the version's Java requirement. A metadata failure, not an exclusion. */
    REQUIREMENT_UNKNOWN
}

/**
 * Decides which bundled JDK boots a given Minecraft version, and whether the runtime image can run it
 * at all. The required Java major comes from SPC's own version metadata
 * ([MinecraftMeta.requiredJavaVersion]) — Mojang's *declared* server requirement — not a hand-rolled
 * heuristic, so it stays correct across Minecraft versioning-scheme changes. The image ships a fixed
 * JDK set ([bundledMajors], default 8/17/21/25 to match the runtime Dockerfile); a version whose required
 * Java isn't bundled is reported **unsupported** so the grinder skips it. Skipping (rather than booting
 * on the wrong JDK) is the whole point: a Java-version crash would otherwise be mis-scored as a
 * clientside crash (a false HIGH).
 *
 * @param requiredJavaMajor Maps a Minecraft version to its required Java major, or `null` when unknown.
 *                          Injected (not bound to [MinecraftMeta]) so the gate is unit-testable.
 * @param bundledMajors     The Java majors the runtime image actually ships — must match the Dockerfile.
 * @author Griefed
 */
class ImageJavaRuntimes(
    private val requiredJavaMajor: (String) -> Int?,
    private val bundledMajors: Set<Int> = setOf(8, 17, 21, 25)
) {
    /** True when [minecraftVersion]'s required Java major is both known and bundled in the image. */
    fun supports(minecraftVersion: String): Boolean = supportFor(minecraftVersion) == ImageSupport.SUPPORTED

    /**
     * *Why* [minecraftVersion] can or cannot be booted here, which [supports] necessarily throws away by
     * answering a single boolean. Callers that report to a human need the distinction: a missing JDK is a
     * permanent, honest exclusion, whereas an unknown requirement means the metadata lookup failed and any
     * "skipped" message built from it is claiming knowledge nobody has.
     */
    fun supportFor(minecraftVersion: String): ImageSupport {
        val required = requiredJavaMajor(minecraftVersion) ?: return ImageSupport.REQUIREMENT_UNKNOWN
        return if (required in bundledMajors) ImageSupport.SUPPORTED else ImageSupport.JDK_NOT_BUNDLED
    }

    /** The in-image `java` binary path for [minecraftVersion], or `null` when its Java isn't bundled. */
    fun javaPath(minecraftVersion: String): String? =
        requiredJavaMajor(minecraftVersion)
            ?.takeIf { it in bundledMajors }
            ?.let { pathFor(it) }

    /**
     * A bundled JDK for running modloader **installers**, which can require a newer Java than the server
     * they install — the Quilt installer needs 17+ even for Minecraft 1.16.1, which itself must run on
     * Java 8. Returns the newest bundled major that is at least [minimumInstallerJava], or `null` if the
     * image ships nothing new enough. The pack's `JAVA` (the server's Java) is left untouched.
     */
    fun installerJavaPath(minimumInstallerJava: Int = MINIMUM_INSTALLER_JAVA): String? =
        bundledMajors.filter { it >= minimumInstallerJava }.maxOrNull()?.let { pathFor(it) }

    /**
     * The installer JDK **only when [minecraftVersion] actually needs one** — i.e. when the server's own
     * Java is older than [minimumInstallerJava]. For a modern Minecraft the server JDK already satisfies
     * every installer, so this returns `null` and the pack is left without a `JAVA_INSTALLER` entry, which
     * is exactly what a hand-made pack looks like. Keeping the common case on the templates' plain
     * `JAVA` fallback means that fallback stays the *exercised* path rather than dead weight.
     */
    fun installerJavaPathFor(minecraftVersion: String, minimumInstallerJava: Int = MINIMUM_INSTALLER_JAVA): String? {
        val serverJava = requiredJavaMajor(minecraftVersion) ?: return null
        return if (serverJava >= minimumInstallerJava) null else installerJavaPath(minimumInstallerJava)
    }

    /** The conventional in-image path for a bundled JDK [major] (see the runtime Dockerfile's symlinks). */
    private fun pathFor(major: Int) = "/opt/java-$major/bin/java"

    /** The installer-Java floor and the factory that sources required-Java from SPC's own metadata. */

    companion object {
        /**
         * Minimum Java the modloader installers need. Set by the Quilt installer, which refuses to run on
         * anything older ("Quilt Installer requires Java 17 or greater to run.") — found by booting the
         * template matrix on Minecraft 1.16.1, whose server Java is 8.
         */
        const val MINIMUM_INSTALLER_JAVA = 17

        /**
         * Build from SPC's version metadata: the required Java comes straight from Mojang's server
         * manifest via [MinecraftMeta.requiredJavaVersion] (a stringified major), parsed to an Int.
         */
        fun from(minecraftMeta: MinecraftMeta, bundledMajors: Set<Int> = setOf(8, 17, 21, 25)): ImageJavaRuntimes =
            ImageJavaRuntimes(
                requiredJavaMajor = { minecraftMeta.requiredJavaVersion(it).orElse(null)?.toIntOrNull() },
                bundledMajors = bundledMajors
            )
    }
}
