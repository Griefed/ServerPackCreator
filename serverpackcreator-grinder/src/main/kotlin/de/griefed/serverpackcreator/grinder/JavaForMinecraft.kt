/* Copyright (C) 2025 Griefed
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

/**
 * Maps a Minecraft version to the JDK the runtime image should boot it with — a single JDK can't run
 * every version. The grinder points the pack's `$JAVA` at the matching bundled JDK
 * (`/opt/java-{8,17,21}`) so no Java is downloaded, which is what keeps the cached boots offline.
 *
 * The cut-offs follow Mojang's bumps: ≤1.16 → 8, 1.17–1.20.4 → 17, 1.20.5+ → 21.
 *
 * @author Griefed
 */
object JavaForMinecraft {

    /** The in-image `java` binary path for [minecraftVersion]. */
    fun javaPath(minecraftVersion: String): String = "/opt/java-${majorJava(minecraftVersion)}/bin/java"

    /** The major Java version [minecraftVersion] needs (8, 17 or 21). */
    fun majorJava(minecraftVersion: String): Int {
        val parts = minecraftVersion.split('.').map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrElse(0) { 1 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return when {
            major != 1 -> 21          // any future 2.x+ → newest bundled JDK
            minor <= 16 -> 8
            minor <= 19 -> 17
            minor == 20 -> if (patch <= 4) 17 else 21
            else -> 21                // 1.21+
        }
    }
}
