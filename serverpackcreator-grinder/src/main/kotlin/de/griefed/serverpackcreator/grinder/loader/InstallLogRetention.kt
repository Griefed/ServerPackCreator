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

import java.io.File

/**
 * Carries one generation of install console across the wipe that starts a tuple's next attempt.
 *
 * `DockerLoaderInstaller` writes the live console to `<tuple>/install.log`, beside the generated pack rather than
 * inside the cache directory, because `LoaderCache` wipes the cache when an install fails. The pack's tuple
 * directory is wiped too, though — by the *next* attempt on that tuple — so without this the failing run's console
 * disappeared exactly when a retry made you want to compare the two.
 *
 * Split out of `ApiVanillaPackGenerator` because that class needs a live `ApiWrapper` and is integration-only,
 * while this is plain file handling worth pinning.
 *
 * @author Griefed
 */
object InstallLogRetention {

    /**
     * Read the console of the attempt that is about to be wiped, or `null` when there was none. Call *before*
     * deleting the tuple directory; the content is held in memory because the directory itself does not survive.
     */
    fun preserve(tupleDirectory: File): String? =
        File(tupleDirectory, INSTALL_LOG).takeIf { it.isFile }?.let { logFile ->
            runCatching { logFile.readText() }.getOrNull()
        }

    /**
     * Write [previousConsole] back as `install.log.previous` in the freshly recreated [tupleDirectory]. A `null`
     * writes nothing at all: an empty `.previous` would imply a console had been captured and then lost. Only this
     * one generation is kept, so a repeatedly failing tuple does not accumulate consoles.
     */
    fun writePrevious(tupleDirectory: File, previousConsole: String?) {
        val console = previousConsole ?: return
        runCatching { File(tupleDirectory, "$INSTALL_LOG.previous").writeText(console) }
    }
}
