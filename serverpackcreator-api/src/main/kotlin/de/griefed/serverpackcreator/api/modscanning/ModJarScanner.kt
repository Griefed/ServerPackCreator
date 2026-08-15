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
package de.griefed.serverpackcreator.api.modscanning

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Reads mod jars and reports what each one declared about the side it belongs on.
 *
 * One implementation per descriptor format, because each modloader states sideness its own way. The
 * contract every implementation owes its caller is total: **one [ScannedMod] comes back per jar
 * handed in, in the same order, whatever happened to that jar.** The include-list a server pack is
 * built from is assembled solely from these results, so a dropped entry is a mod missing from the
 * finished pack — see [ScannedMod] for why the fallbacks default to keeping a mod rather than
 * excluding it.
 *
 * This is the type to dispatch on rather than a concrete scanner; [ModScanner.scannerFor] picks the
 * right one for a modloader and Minecraft version.
 */
interface ModJarScanner {
    /**
     * Read every jar in [jarFiles] and report what each declared.
     *
     * @return One entry per input jar, in input order — never fewer, whatever a jar contained.
     */
    fun scan(jarFiles: Collection<File>): List<ScannedMod>
}

/**
 * Base for scanners that read one descriptor file out of each jar, owning the part every one of them
 * shares: walk the jars, and turn any failure on a single jar into that jar's default [ScannedMod]
 * instead of letting it abort the scan.
 *
 * Subclasses implement [read] for one jar and may simply throw — which is why [scan] is `final`. The
 * total-result contract of [ModJarScanner] is the one thing no scanner may get wrong, and putting it
 * here means it cannot drift between the five implementations that used to each hold a copy.
 */
abstract class DescriptorScanner : ModJarScanner {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Logged once before a scan starts, or `null` to announce nothing. Exists only because the
     * per-loader wording is user-facing progress output.
     */
    protected open val scanAnnouncement: String? = null

    /**
     * Read a single jar's descriptor into the verdict for that mod.
     *
     * Implementations are free to throw: [scan] catches everything and falls back to the defaults,
     * so there is no need to guard the jar-level failure modes here.
     */
    @Throws(Exception::class)
    protected abstract fun read(modJar: File): ScannedMod

    final override fun scan(jarFiles: Collection<File>): List<ScannedMod> {
        scanAnnouncement?.let { log.info(it) }
        return jarFiles.map { modJar ->
            try {
                read(modJar)
            } catch (e: Exception) {
                log.error("Could not scan ${modJar.name}. Consider reporting this to the mod-author:",e)
                ScannedMod(modJar)
            }
        }
    }
}
