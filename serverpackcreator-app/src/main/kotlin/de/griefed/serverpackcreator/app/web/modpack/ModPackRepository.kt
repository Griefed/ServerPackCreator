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
package de.griefed.serverpackcreator.app.web.modpack

import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

import java.util.*

/** Stored modpacks, with the two lookups the upload path needs. */
@Repository
interface ModPackRepository : MongoRepository<ModPack, String> {
    /** The modpack whose server packs include the given one — the reverse of the `@DBRef` list. */
    fun findByServerPacksContains(serverPack: ServerPack): Optional<ModPack>

    /**
     * The first stored modpack whose contents hash to [sha256], if any.
     *
     * Backs the upload duplicate-check, which previously loaded the whole collection and compared in
     * memory. `ModPack.sha256` carries `@Indexed` and `application.properties` enables index creation, so
     * this is a single indexed lookup rather than a scan that also drags in the eager `@DBRef` graph
     * behind every document.
     *
     * **`First` is load-bearing, not decoration.** Without it a derived query returning [Optional] raises
     * `IncorrectResultSizeDataAccessException` as soon as two documents share a hash — which the scan this
     * replaced tolerated, because it returned on the first match. Duplicates are reachable through a race
     * between concurrent uploads of one file, and through any database predating the check. Pinned by
     * `ModPackHashQueryTest`.
     */
    fun findFirstBySha256(sha256: String?): Optional<ModPack>
}