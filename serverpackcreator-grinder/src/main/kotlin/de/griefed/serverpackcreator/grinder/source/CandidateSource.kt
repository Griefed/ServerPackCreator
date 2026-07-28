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
package de.griefed.serverpackcreator.grinder.source

import de.griefed.serverpackcreator.grinder.GrindCandidate

/**
 * A hosting platform the grinder can enumerate for mods to verify. Implementations paginate their
 * platform's catalog **most-downloaded first** so the popularity ranking that decides grind order is
 * seeded correctly. The grinder wires all available sources (Modrinth always; CurseForge when an API
 * key is present) and lets [de.griefed.serverpackcreator.grinder.GrindPool] re-sort the union by
 * popularity.
 *
 * @author Griefed
 */
fun interface CandidateSource {
    /** Up to [limit] mod projects, most-downloaded first, as queue candidates. */
    fun candidates(limit: Int): List<GrindCandidate>
}
