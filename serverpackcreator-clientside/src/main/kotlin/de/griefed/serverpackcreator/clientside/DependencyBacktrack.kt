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
 * Decides whether the jars a boot pack holds actually satisfy each other, and which staged **dependency**
 * to drop to an older build when they do not.
 *
 * **The gap this closes.** Staging resolves each dependency on its own — the newest file of that project the
 * platform tags for the pack's Minecraft version — and never asks whether the resulting *set* is coherent.
 * It usually is. Where it is not, the loader refuses the pack, the boot burns ~70 s and the **candidate**
 * wears the INCONCLUSIVE, exactly like every other "the mod never got a fair run" failure this engine keeps
 * guards for.
 *
 * **Measured live 2026-09-06, `Modrinth/zoomify` on Quilt / Minecraft 1.20.5.** Modrinth tags
 * `yet_another_config_lib_v3-3.6.6+1.20.6-fabric.jar` for 1.20.5 *and* 1.20.6, and the jar's own descriptor
 * declares `"minecraft": "~1.20.5"`, so nothing in selection or in the descriptor gate objects to it — but it
 * also declares `"fabric-api": ">=0.100.0+1.20.6"`, and the newest Fabric API Modrinth publishes for 1.20.5
 * is `0.97.8+1.20.5`. There is no fabric-api that satisfies it at that Minecraft version, so the pack cannot
 * be made to work by staging *more*; only by staging an **older YACL**. `3.4.2+1.20.5` requires nothing but
 * `fabric-resource-loader-v0` and boots.
 *
 * **The candidate is never demoted.** It is the subject of the experiment; replacing it would test a
 * different mod. A conflict only the candidate declares is left alone and the boot proceeds as before.
 *
 * @author Griefed
 */
object DependencyBacktrack {

    /**
     * How many times one staging attempt may drop a dependency to an older build before giving up.
     *
     * **Measured, not guessed:** the live `zoomify` case needs **seven** — YACL publishes 3.6.6, 3.6.5,
     * 3.6.4, 3.6.3, 3.6.2, 3.6.1 and 3.6.0 tagged for Minecraft 1.20.5, every one of them a `+1.20.6` build
     * demanding the same unavailable Fabric API, before `3.4.2+1.20.5` answers. Ten leaves headroom without
     * letting a pathological project spend a whole worker on downloads.
     */
    const val MAX_BACKTRACKS = 10

    /** One requirement a staged jar declares, kept until every jar is in so the set can be judged as a whole. */
    data class Requirement(
        /** The jar that declared it, named as it was staged. */
        val requiringFileName: String,
        /** Whether that jar is the mod being verified rather than a dependency — only the latter may be demoted. */
        val requiringIsCandidate: Boolean,
        /** The mod id the descriptor names, which may be an `id` or something another jar `provides`. */
        val requiredModId: String,
        /** The range the descriptor spelled, verbatim and unparsed — `VersionConstraint` reads what it can. */
        val versionConstraint: String
    )

    /**
     * A [Requirement] the staged set contradicts, carrying both halves so the log can state the reason.
     *
     * The first four properties are the requirement verbatim; [stagedVersion] is what the pack actually
     * holds, and the pair of it and [versionConstraint] is the whole finding.
     */
    data class Conflict(
        /** The jar whose descriptor demanded something the pack does not hold; the demotion candidate. */
        val requiringFileName: String,
        /** Whether that jar is the mod being verified — a conflict only it declares is left alone. */
        val requiringIsCandidate: Boolean,
        /** The mod id that was demanded. */
        val requiredModId: String,
        /** The range demanded, verbatim, so the log can quote what the author wrote. */
        val versionConstraint: String,
        /** The version of [requiredModId] the pack really staged, which is what contradicts the range. */
        val stagedVersion: String
    )

    /**
     * Every requirement in [requirements] that [stagedVersions] positively contradicts.
     *
     * [stagedVersions] maps a **declared mod id** (a jar's own `modID` and everything it `provides`) to the
     * version the platform published the staged file under, so a requirement is matched against the thing
     * that will really be on the classpath rather than against a file name.
     *
     * **Only a staged mod can conflict.** A requirement naming something absent is the *missing* dependency
     * case, which `BootVerifier.refuseForMissingDependencies` already owns; reporting it here as well would
     * demote a jar over a gap that dropping it cannot close.
     *
     * `VersionConstraint` accepts anything it cannot read, so an unparseable range never produces a conflict
     * — the same fail-toward-proceed rule the rest of this module runs on.
     */
    fun conflicts(requirements: List<Requirement>, stagedVersions: Map<String, String>): List<Conflict> {
        val staged = stagedVersions.mapKeys { (modId, _) -> modId.lowercase() }
        return requirements.mapNotNull { requirement ->
            val stagedVersion = staged[requirement.requiredModId.lowercase()] ?: return@mapNotNull null
            if (VersionConstraint.satisfies(stagedVersion, requirement.versionConstraint)) {
                null
            } else {
                Conflict(
                    requirement.requiringFileName,
                    requirement.requiringIsCandidate,
                    requirement.requiredModId,
                    requirement.versionConstraint,
                    stagedVersion
                )
            }
        }
    }

    /**
     * Which staged file to drop to an older build, or `null` when nothing may be dropped.
     *
     * The first conflict whose requirer is a **dependency**; a conflict the candidate itself declares yields
     * `null`, because demoting the candidate would verify a different mod than the one that was asked about.
     */
    fun fileToDemote(conflicts: List<Conflict>): String? =
        conflicts.firstOrNull { !it.requiringIsCandidate }?.requiringFileName
}
