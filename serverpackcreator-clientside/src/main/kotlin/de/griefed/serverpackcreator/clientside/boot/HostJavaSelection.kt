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
package de.griefed.serverpackcreator.clientside.boot

/**
 * The [BootVerifier] pack post-processor for a boot on this host: points the staged pack's `JAVA` at the JDK
 * its Minecraft version requires and sets the unattended-boot levers ([PackVariables.prepareUnattended]).
 *
 * `start.sh` installs its own Java through an interactive prompt whenever the `java` it runs is not the pack's
 * required major, and a boot without a terminal can only decline it. So a version whose JDK this host lacks is
 * refused here by throwing, which [BootVerifier] reports as an `ERROR` carrying this message — never as a crash.
 */
class HostJavaSelection(private val runtimes: ImageJavaRuntimes) : (BootVerifier.Prepared.Ready) -> Unit {

    override fun invoke(pack: BootVerifier.Prepared.Ready) {
        val minecraftVersion = pack.minecraftVersion
        val javaPath = runtimes.javaPath(minecraftVersion) ?: throw IllegalStateException(refusal(minecraftVersion))
        PackVariables.prepareUnattended(
            pack.serverPack, javaPath, offline = false,
            installerJavaPath = runtimes.installerJavaPathFor(minecraftVersion)
        )
    }

    private fun refusal(minecraftVersion: String): String {
        val major = runtimes.requiredJava(minecraftVersion)
            ?: return "SPC does not know which Java Minecraft $minecraftVersion requires, so no server was booted for it."
        return "Minecraft $minecraftVersion requires Java $major, which this host does not provide. Install it and " +
            "expose its home as JAVA_HOME_${major}_<arch> (for example JAVA_HOME_${major}_X64), as actions/setup-java does."
    }
}
