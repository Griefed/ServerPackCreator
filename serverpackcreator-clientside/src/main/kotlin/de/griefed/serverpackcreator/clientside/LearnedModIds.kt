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

import java.util.concurrent.ConcurrentHashMap

/**
 * The id-to-project bridge **learned from jars this process has already downloaded**, as opposed to the
 * hand-written [KnownModIds].
 *
 * A descriptor names a mod **id**; a platform serves **refs** (a Modrinth slug or base62 id, a CurseForge
 * number). Bridging the two by hand means a human notices a wasted boot, reads a log, looks the project up
 * on both platforms and writes an entry — for a fact staging already had in its hands, because it fetched
 * the jar and the jar says what it is.
 *
 * **A learned mapping is evidence and is treated as one.** A jar staged under ref `R` whose descriptor
 * declares id `X` proves this platform serves `X` at `R`, so [mappingFor] answers [ModIdMapping.Alias] —
 * with the right to refuse a boot that an alias carries — while an id nothing has proved falls through to
 * whatever the registry makes of it, usually a [ModIdMapping.Guess].
 *
 * **It compounds, which is the point.** `yet_another_config_lib_v3` cannot be resolved by spelling, since
 * both platforms publish YACL as `yacl`; the first candidate that stages YACL through a platform ref
 * teaches every later one.
 *
 * **What it deliberately does not do:** go looking. It records what staging downloads anyway and never
 * fetches a project to find out what is inside it. Downloading a project *because* an id is unresolved —
 * the last step of the algorithm Griefed described — would close the remaining gap (an id whose project no
 * candidate has ever staged) at the cost of downloads on the path that currently fails for free. Worth
 * doing next; deliberately not smuggled in here.
 *
 * **Only a jar's own identity is learned, never what it bundles.** A nested `fabric-api-base` inside some
 * mod is provided by *that jar* on *that classpath*, but the id belongs to Fabric API — recording the host
 * as its project would send a later candidate to download the wrong mod entirely.
 *
 * Thread-safe: the grinder shares one instance across its grind workers, which is where the compounding
 * comes from.
 *
 * @author Griefed
 */
class LearnedModIds(
    /** Not implemented yet — see `LearnedModIdsTest.onlySomethingNewAnnouncesItself`. */
    private val onLearned: () -> Unit = {}
) {

    /**
     * `platform -> (lowercased mod id -> ref)`. Nested per platform because a ref is meaningless on the
     * other one, and a single map keyed by a pair would let that mistake compile.
     */
    private val byPlatform = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

    /**
     * Record that [platform] serves every id in [ids] at [ref], as proved by a jar staged from it.
     *
     * **The first project to prove an id keeps it.** Two projects declaring one id is an upstream collision
     * this cannot adjudicate, and letting the later one win would make the answer depend on the order
     * candidates happened to be ground in.
     */
    fun learn(platform: String, ref: String, ids: Collection<String>) {
        if (ref.isBlank()) {
            return
        }
        val known = byPlatform.computeIfAbsent(platform) { ConcurrentHashMap() }
        ids.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .forEach { id -> known.putIfAbsent(id, ref) }
    }

    /** The ref [platform] is known to serve [modId] at, or `null` when no staged jar has proved one. */
    fun refFor(modId: String, platform: String): String? =
        byPlatform[platform]?.get(modId.trim().lowercase())

    /**
     * [refFor] as a mapping, falling back to [orElse] for an id nothing has proved.
     *
     * @param orElse The unlearned answer, normally [KnownModIds.mappingFor] bound to this platform.
     */
    fun mappingFor(modId: String, platform: String, orElse: (String) -> ModIdMapping): ModIdMapping =
        refFor(modId, platform)?.let { ModIdMapping.Alias(it) } ?: orElse(modId)

    /** Everything learned so far as plain data, so an owner can write it somewhere. */
    fun snapshot(): Map<String, Map<String, String>> = TODO("nothing can be carried across a restart yet")

    /** Adopt [snapshot] wholesale, as read back from wherever an owner wrote it. */
    fun restore(snapshot: Map<String, Map<String, String>>): Unit =
        TODO("nothing can be carried across a restart yet")
}
