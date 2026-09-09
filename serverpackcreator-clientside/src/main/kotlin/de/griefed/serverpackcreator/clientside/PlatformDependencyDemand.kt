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

/**
 * Whether a jar actually asks for a dependency its **platform** attributes to it.
 *
 * A platform dependency list is a self-report an author attaches to a project, and CurseForge carries one
 * per file that authors routinely maintain per project. The jar's descriptor is what the loader enforces.
 * Where the two disagree the descriptor wins — the same relationship the boot rules encode one layer up,
 * where the console decides and the metadata only declares.
 *
 * **The case this exists for:** `CurseForge/aether` on Forge / Minecraft 1.20.2 was refused for `owo-lib`,
 * which publishes no Forge build. True, and irrelevant: only Aether's Fabric and Quilt jars declare it.
 *
 * **Why the comparison is between an id and a slug.** A project that cannot be staged cannot be downloaded,
 * so its declared mod id is unknowable here; all that is in hand is the slug the platform publishes it
 * under. Once a dependency *is* downloaded, its real ids are read from its descriptor and no guessing is
 * needed — that is the learned mapping, and this is only the fallback for the case where there is no jar to
 * read. Both mistakes cost at most one container and neither can reach a sideness verdict: a false
 * "demanded" refuses a boot exactly as before this existed, and a false "not demanded" spends a boot the
 * loader then refuses, which is INCONCLUSIVE.
 *
 * @author Griefed
 */
object PlatformDependencyDemand {

    /**
     * Shortest fragment allowed to match across the two vocabularies. Three characters is enough for `jei`
     * and `owo` while `ae` would claim half the catalogue — measured against nothing, chosen as the length
     * of the shortest real mod id in the live store, and stated here so the next reader can widen it
     * deliberately rather than by accident.
     */
    private const val MIN_FRAGMENT = 3

    /**
     * Whether the jar that declared [declaredIds] asks for [project].
     *
     * `null` [declaredIds] means the descriptor could not be read, and answers **true**: with nothing to
     * compare against, the platform's claim is all there is, which is the behaviour that predates this
     * class. An empty set is different — the descriptor *was* read and named nothing.
     */
    fun isDemanded(declaredIds: Set<String>?, project: ProjectFiles): Boolean {
        val declared = declaredIds ?: return true
        val slug = normalise(project.slug)
        return declared.any { id -> namesTheSameThing(normalise(id), slug) || resolvesTo(id, project) }
    }

    /** Punctuation is spelling, not identity: `ftb-library-forge` and `ftblibrary` are one project. */
    private fun normalise(text: String): String =
        text.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Whether two normalised names denote the same project. Equality, or one containing the other — a mod id
     * is routinely the slug plus a loader suffix (`balm-fabric` for `balm`) or the slug minus its suffix
     * (`ftblibrary` for `ftb-library-forge`), and neither direction is the general case.
     */
    private fun namesTheSameThing(id: String, slug: String): Boolean {
        if (id.length < MIN_FRAGMENT || slug.length < MIN_FRAGMENT) {
            return id == slug
        }
        return id == slug || id.contains(slug) || slug.contains(id)
    }

    /**
     * Whether [id] is one the registry already resolves to this very [project] — `fabric` is Fabric API
     * however either platform spells the project, and no amount of string comparison would find that.
     */
    private fun resolvesTo(id: String, project: ProjectFiles): Boolean {
        val ref = KnownModIds.refFor(id, project.platform) ?: return false
        return ref.equals(project.slug, ignoreCase = true) ||
            KnownModIds.refFor(project.slug, project.platform)?.equals(ref, ignoreCase = true) == true
    }
}
