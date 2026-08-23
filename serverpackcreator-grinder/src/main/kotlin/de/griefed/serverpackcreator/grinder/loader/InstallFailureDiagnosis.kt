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

import de.griefed.serverpackcreator.grinder.container.ContainerUser

/**
 * Reads a failed install's console for a cause worth naming, scanning **all** of it rather than the
 * tail the failure warning quotes.
 *
 * The tail is the wrong slice for exactly the failure that motivated this: an unwritable mount refuses
 * the install's very first writes, the start script carries on regardless, and twenty-odd lines later
 * the JVM complains about an argfile that was never written — so the last 25 lines describe a
 * consequence and the cause has scrolled away. One recognised pattern beats a scrollback every time.
 *
 * Deliberately narrow: it explains only what it can actually recognise and returns `null` otherwise,
 * because a confidently wrong diagnosis costs more than no diagnosis. Consulted by
 * [DockerLoaderInstaller] when an install produces no library layer.
 *
 * @author Griefed
 */
object InstallFailureDiagnosis {

    /** How many offending lines to quote as evidence; enough to recognise the pattern, short enough to read. */
    private const val EVIDENCE_LINES = 3

    /**
     * A one-paragraph explanation of why the install failed, or `null` when the console shows nothing
     * recognisable and the caller should fall back to quoting the raw output.
     */
    fun of(consoleLines: List<String>): String? = unwritableMount(consoleLines)

    /**
     * The container could not write into the bind-mounted pack — the uid it runs as does not own the
     * directory the host created for it. Observed live 2026-08-23 across every loader at once, which is
     * the tell: a permission wall is indifferent to which loader is being installed.
     */
    private fun unwritableMount(consoleLines: List<String>): String? {
        val refusals = consoleLines.filter { it.contains("Permission denied", ignoreCase = true) }
        if (refusals.isEmpty()) {
            return null
        }
        val evidence = refusals.take(EVIDENCE_LINES).joinToString("; ") { it.trim() }
        return "the container could not write into the mounted pack — it runs as a uid that does not own the " +
            "staging directory, so the loader install had nowhere to put its files. Set ${ContainerUser.ENV_KEY} " +
            "to the owning uid:gid, or make the work directory writable by the container's user. Evidence: $evidence"
    }
}
