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
import java.util.concurrent.CopyOnWriteArrayList

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
    /**
     * Called when a genuinely **new** pair is recorded, which is what a file-backed owner persists on.
     *
     * Only news, never a re-statement: every staged dependency declares its own id again on every candidate
     * that uses it, so announcing those would mean a write per staged jar for a document that did not
     * change. Restoring a snapshot is likewise silent — loading a file must not ask to write it back.
     */
    private val onLearned: () -> Unit = {}
) {

    /**
     * `platform -> (lowercased mod id -> refs, in the order they proved it)`. Nested per platform because a
     * ref is meaningless on the other one, and a single map keyed by a pair would let that mistake compile.
     *
     * The value is a **list** because one mod id is genuinely served by several projects — see [learn].
     * [CopyOnWriteArrayList] rather than a set: order is the answer's ranking, and a contested id holds two
     * or three entries at most, so copy-on-write costs nothing and keeps reads lock-free.
     */
    private val byPlatform = ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<String>>>()

    /**
     * Record that [platform] serves every id in [ids] at [ref], as proved by a jar staged from it.
     *
     * **The first project to prove an id leads, and every later one is kept behind it.** Two projects
     * declaring one id is an upstream collision this cannot adjudicate — letting the later one win would
     * make the answer depend on grind order — but *discarding* it is worse than either, because the ecosystem
     * is full of cross-loader forks and unofficial ports that deliberately keep the original's mod id.
     * `create` is Create and Create Fabric; `farmersdelight` is Farmer's Delight and its Fabric port;
     * `sophisticatedcore` is Sophisticated Core and its unofficial Fabric port. Whichever was ground first
     * owned the id for every loader afterwards, and a learned mapping's alias-strength then let the wrong
     * project's empty file list refuse a boot (measured on `chefs-delight`, 2026-09-09).
     *
     * Keeping every prover needs no loader dimension to be loader-aware:
     * [BootCandidateSelector.pickDependencyFile] already filters by loader and Minecraft version, so the
     * project with a build for the boot in hand is the one that stages.
     */
    fun learn(platform: String, ref: String, ids: Collection<String>) {
        if (ref.isBlank()) {
            return
        }
        val known = byPlatform.computeIfAbsent(platform) { ConcurrentHashMap() }
        val learnedSomething = ids.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .count { id -> known.computeIfAbsent(id) { CopyOnWriteArrayList() }.addIfAbsent(ref) } > 0
        if (learnedSomething) {
            onLearned()
        }
    }

    /**
     * The ref [platform] is known to serve [modId] at, or `null` when no staged jar has proved one — the
     * **first** prover where several exist.
     *
     * The single-answer view, for callers that only need one canonical ref: deduping what is already staged
     * against what a requirement names. [refsFor] is what staging itself asks, because a ref that cannot
     * serve this boot is not a reason to stop looking.
     */
    fun refFor(modId: String, platform: String): String? = refsFor(modId, platform).firstOrNull()

    /** Every ref [platform] is known to serve [modId] at, in the order they proved it. */
    fun refsFor(modId: String, platform: String): List<String> =
        byPlatform[platform]?.get(modId.trim().lowercase())?.toList().orEmpty()

    /**
     * Everything worth trying for [modId], learned aliases first and [orElse]'s answer last.
     *
     * The registry comes last rather than instead: a jar this process actually read outranks a slug guess,
     * and it outranks the hand-written table too — the table is a snapshot of what somebody looked up once,
     * while a learned ref is a descriptor read from the project it names. Trying it *afterwards* costs one
     * resolve on the path that was already failing and is the only route left for an id whose projects have
     * all been ground but none of which fits.
     *
     * @param orElse The unlearned answers, normally [KnownModIds.mappingsFor] bound to this platform.
     */
    fun mappingsFor(
        modId: String,
        platform: String,
        orElse: (String) -> List<ModIdMapping>
    ): List<ModIdMapping> {
        val learnedRefs = refsFor(modId, platform)
        // A list rather than one mapping because the registry can legitimately offer several: an id served
        // by a project and by a cross-loader fork of it. Anything already proved by a staged jar is dropped
        // here rather than in the registry, which knows nothing about what this process has learned.
        val registry = orElse(modId).filter { it.ref != null && it.ref !in learnedRefs }
        return learnedRefs.map { ModIdMapping.Alias(it) } + registry
    }

    /**
     * Everything learned so far as plain data, so an owner can write it somewhere.
     *
     * `platform -> id -> refs`, nested rather than keyed by a joined string, for the same reason the
     * in-memory shape is: a ref is meaningless on the other platform, and a flat key would let that mistake
     * through both here and in whatever reads the file back.
     */
    fun snapshot(): Map<String, Map<String, List<String>>> = byPlatform.entries.associate { (platform, ids) ->
        platform to ids.toSortedMap().mapValues { (_, refs) -> refs.toList() }
    }

    /**
     * Adopt [snapshot] wholesale, as read back from wherever an owner wrote it.
     *
     * Silent by design — see [onLearned]. Restored refs are **appended** behind whatever this process has
     * already proved with a jar in hand, so a restore can never displace first-hand evidence and can never
     * lose a prover either.
     */
    fun restore(snapshot: Map<String, Map<String, List<String>>>) {
        snapshot.forEach { (platform, ids) ->
            val known = byPlatform.computeIfAbsent(platform) { ConcurrentHashMap() }
            ids.forEach { (id, refs) ->
                val cleanId = id.trim().lowercase()
                val usable = refs.filter { it.isNotBlank() }
                // Filtered *before* the entry is claimed: an id whose stored value contributes nothing --
                // a document holding a JSON null, a number, or an empty array -- must leave no trace, or
                // `snapshot()` writes it back as `"id": []` and the file grows an entry per restart that
                // asserts nothing. Harmless to read, which is why nothing would have noticed.
                if (cleanId.isEmpty() || usable.isEmpty()) {
                    return@forEach
                }
                val entry = known.computeIfAbsent(cleanId) { CopyOnWriteArrayList() }
                usable.forEach { entry.addIfAbsent(it) }
            }
        }
    }
}
