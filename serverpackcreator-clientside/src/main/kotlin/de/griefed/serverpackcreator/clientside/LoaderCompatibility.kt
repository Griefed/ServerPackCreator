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
 * Which modloaders run **another** loader's builds, and on which Minecraft versions.
 *
 * One home for a fact two very different callers need: [JarSelfDeclaration] asks it before refusing a boot
 * the staged jar's descriptor seems to contradict, and [BootCandidateSelector] asks it before giving up on a
 * dependency that publishes nothing for the loader being booted. They used to hold separate, silently
 * diverging copies — a `Quilt to Fabric` map in each — and only one of them could ever have learned the
 * NeoForge rule below.
 *
 * **Deliberately one-way and deliberately minimal.** Every entry is a claim that a loader really does load
 * the other's jars unchanged; guessing wider stages a jar the loader cannot use, and the resulting failure is
 * scored against the *mod*. Where a claim is version-dependent it is stated as the exact versions, never as
 * "roughly around there".
 *
 * @author Griefed
 */
object LoaderCompatibility {

    /**
     * Loaders that run another loader's builds on **every** Minecraft version.
     *
     * Quilt runs Fabric mods by design — which is why the canonical dependency of a Quilt mod is Fabric API, a
     * project shipping only Fabric-tagged files — and LegacyFabric reads the same `fabric.mod.json`. Neither
     * holds in reverse: Fabric cannot load a Quilt mod.
     *
     * What the Quilt entry is worth, measured 2026-07-30: without it every such dependency was silently
     * dropped and the mod hard-failed with *"requires fabric-api"*, wasting the whole boot — **210 dropped
     * dependencies, all but 44 of them on Quilt**.
     */
    private val universallyCompatible = mapOf(
        "Quilt" to setOf("Fabric"),
        "LegacyFabric" to setOf("Fabric")
    )

    /**
     * The one Minecraft version on which NeoForge also runs Forge builds, and it is exactly one.
     *
     * NeoForge 20.1.x is a fork of Forge 47 that kept the `net.minecraftforge` packages, the `javafml`
     * language provider and `META-INF/mods.toml`, so a Forge 1.20.1 jar and a NeoForge 1.20.1 jar are the same
     * file. NeoForge renamed those packages to `net.neoforged` for 1.20.2, and from there nothing crosses over
     * without a separate build — so this is a single version, not the lower bound of a range.
     */
    private const val NEOFORGE_FORGE_PARITY_MINECRAFT = "1.20.1"

    /**
     * The other loaders whose builds [loader] can run on [minecraftVersion] — empty for a loader that runs
     * only its own, which is the common case and the safe default.
     *
     * Answers the version-independent map first and then adds the one version-dependent claim, so a caller
     * that has a Minecraft version in hand never has to remember which kind of rule it is asking about.
     */
    fun alsoRuns(loader: String, minecraftVersion: String): Set<String> =
        universallyCompatible[loader].orEmpty() + neoForgeParity(loader, minecraftVersion)

    /** `Forge` where [loader] is NeoForge on the one Minecraft version the two share builds, else empty. */
    private fun neoForgeParity(loader: String, minecraftVersion: String): Set<String> =
        if (loader == "NeoForge" && minecraftVersion == NEOFORGE_FORGE_PARITY_MINECRAFT) setOf("Forge") else emptySet()
}
