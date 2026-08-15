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
package de.griefed.serverpackcreator.clientside

import de.griefed.serverpackcreator.api.modscanning.ModScanner
import de.griefed.serverpackcreator.api.modscanning.Sideness
import java.io.File

/**
 * Runs ServerPackCreator's own per-loader mod-scanners over a jar to read the sideness the mod
 * *declares* in its metadata (`fabric.mod.json`, `mods.toml`/`neoforge.mods.toml`, `quilt.mod.json`).
 * This is the jar-based half of the metadata signal. The loader→scanner choice is not made here: it
 * comes from [ModScanner.scannerFor], the same call a real generation makes, so the two cannot
 * disagree about what a jar declared.
 *
 * @param modScanner The bundle of per-loader scanners from [de.griefed.serverpackcreator.api.ApiWrapper].
 * @author Griefed
 */
class MetadataScanner(private val modScanner: ModScanner) {

    /** Outcome of scanning a single jar's declared sideness. */
    enum class Result {
        /** The jar declares itself client-only (would be excluded from a server pack). */
        CLIENT,

        /** The jar declares server/both, or no sideness at all (kept in a server pack). */
        SERVER_OR_BOTH,

        /** The jar could not be scanned (missing/malformed metadata). */
        ERROR
    }

    /**
     * Scan a single [jar] for the given [loader] and [minecraftVersion], returning whether the mod
     * declares itself client-only. The mod is [Result.CLIENT] when the loader's scanner reads its
     * descriptor as clientside.
     *
     * A loader no scanner knows yields [Result.SERVER_OR_BOTH] — nothing was read, so nothing
     * declared the mod client-only.
     */
    fun scan(jar: File, loader: String, minecraftVersion: String): Result = try {
        val scanner = modScanner.scannerFor(loader, minecraftVersion)
        val clientside = scanner
            ?.scan(listOf(jar))
            ?.any { it.file == jar && it.sideness == Sideness.CLIENT }
            ?: false
        if (clientside) Result.CLIENT else Result.SERVER_OR_BOTH
    } catch (_: Exception) {
        Result.ERROR
    }
}
