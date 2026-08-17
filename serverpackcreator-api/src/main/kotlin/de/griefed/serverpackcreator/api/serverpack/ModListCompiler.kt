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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.modscanning.ModScanner
import de.griefed.serverpackcreator.api.modscanning.ScannedMod
import de.griefed.serverpackcreator.api.modscanning.Sideness
import de.griefed.serverpackcreator.api.utilities.SimpleStopWatch
import de.griefed.serverpackcreator.api.utilities.common.FilterType
import de.griefed.serverpackcreator.api.utilities.common.ListUtilities
import de.griefed.serverpackcreator.api.utilities.common.filteredWalk
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.*
import java.util.regex.PatternSyntaxException

/**
 * Compiler of the list of mods to include in a server pack: walks the mods-directory, excludes
 * user-specified and automatically discovered clientside-only mods, and honors the
 * mod-whitelist. Extracted from ServerPackHandler (refactor Phase 1d); ServerPackHandler
 * remains the facade through which consumers access these operations.
 */
class ModListCompiler(
    private val apiProperties: ApiProperties,
    private val modScanner: ModScanner
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Extensions treated as mod files. `disabled` is included deliberately: a launcher marks a mod off by
     * renaming it, and such a file must still be recognised so it can be excluded rather than copied blindly.
     *
     * The single source of truth: [ServerPackHandler.modFileEndings] reads this rather than holding its
     * own copy, so the published constant and the list generation walks with cannot drift apart.
     */
    val modFileEndings = listOf("jar", "disabled")

    /**
     * The entry in this list describing the same jar as [mod], or `null`. Scanning a directory twice —
     * or re-walking one result set against another — yields distinct [ScannedMod] instances for one
     * file, and they may disagree on mod id, so the jar is the only identity that holds across them.
     *
     * [ScannedMod] deliberately has no `equals`: two entries for one jar can carry different
     * [Sideness], and value-equality would let a `Set` or `distinct()` silently keep whichever landed
     * first and drop the other verdict. The merge those verdicts feed is a decision, not a de-duplication.
     */
    private fun List<ScannedMod>.forJarOf(mod: ScannedMod): ScannedMod? = find { it.file == mod.file }

    /** Whether this list already holds an entry for [mod]'s jar. */
    private fun List<ScannedMod>.holds(mod: ScannedMod): Boolean = forJarOf(mod) != null

    /** Drops every entry describing [mod]'s jar. */
    private fun MutableList<ScannedMod>.removeJar(mod: ScannedMod) = removeIf { it.file == mod.file }

    /**
     * Decides whether a mod-name matches a list entry, for one generation.
     *
     * Built once per [compileModList] and reused for every comparison, which is the point: the loop runs
     * mods x list-entries times — of the order of 165,000 for a 300-mod pack against the ~550-entry
     * default list — and both things this used to do per comparison are invariant across it. Reading
     * `apiProperties.exclusionFilter` costs two synchronized `Hashtable` lookups (via
     * `PropertyStore.acquire`), and `entry.toRegex()` costs a `Pattern.compile`.
     *
     * The same comparison decides both the clientside-mod list and the whitelist, so it is defined once
     * here rather than spelled out at each site — three copies of it had drifted apart in formatting
     * already.
     *
     * @param filter The matching mode, read once by the caller.
     * @param entries Every list entry to be compared against, so their regexes can be compiled up front.
     */
    private class FilterMatcher(private val filter: ExclusionFilter, entries: Collection<String>) {
        private val log by lazy { cachedLoggerOf(this.javaClass) }

        /**
         * The compiled pattern per entry, for the two filters that need one. An entry whose pattern does
         * not compile is absent, and never matches.
         *
         * Compiling up front is what turns a malformed entry from a thrown `PatternSyntaxException` —
         * which used to escape `compileModList` and abort the whole generation — into one logged, skipped
         * entry, reported once instead of once per mod.
         */
        private val patterns: Map<String, Regex> =
            if (filter == ExclusionFilter.REGEX || filter == ExclusionFilter.EITHER) {
                entries.mapNotNull { entry ->
                    try {
                        entry to entry.toRegex()
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid regex specified in mod-list: $entry. Ignoring this entry.", ex)
                        null
                    }
                }.toMap()
            } else {
                emptyMap()
            }

        /** Whether [modName] matches [entry] under this matcher's filter. */
        fun matches(modName: String, entry: String): Boolean = when (filter) {
            ExclusionFilter.START -> modName.startsWith(entry)
            ExclusionFilter.END -> modName.endsWith(entry)
            ExclusionFilter.CONTAIN -> modName.contains(entry)
            ExclusionFilter.REGEX -> patterns[entry]?.let { modName.matches(it) } == true
            ExclusionFilter.EITHER -> modName.startsWith(entry) || modName.endsWith(entry) ||
                    modName.contains(entry) || patterns[entry]?.let { modName.matches(it) } == true
        }
    }

    /**
     * Generates a list of all mods to include in the server pack. If the user specified
     * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
     * active, they will be excluded, too.
     *
     * @param packConfig The configurationModel containing the modpack directory, list of
     * clientside-only mods to exclude, Minecraft version used by the
     * modpack and server pack and the modloader used by the modpack and
     * server pack.
     * @return A list of all mods to include in the server pack.
     * @author Griefed
     */
    @Suppress("unused")
    fun compileModList(packConfig: PackConfig) = compileModList(
        "${packConfig.modpackDir}${File.separator}mods",
        packConfig.clientMods,
        packConfig.modsWhitelist,
        packConfig.minecraftVersion,
        packConfig.modloader
    )

    /**
     * Generates a list of all mods to include in the server pack. If the user specified
     * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
     * active, they will be excluded, too.
     *
     * @param modsDir The mods-directory of the modpack of which to generate a list of all its contents.
     * @param clientsideModsList A list of all clientside-only mods.
     * @param modWhitelist A list of mods to include regardless if a match was found in [clientsideModsList].
     * @param minecraftVersion The Minecraft version the modpack uses. When the modloader is Forge, this determines
     * whether Annotations or Tomls are scanned.
     * @param modloader The modloader the modpack uses.
     * @return A list of all mods to include in the server pack.
     * @author Griefed
     */
    fun compileModList(
        modsDir: String,
        clientsideModsList: List<String>,
        modWhitelist: List<String>,
        minecraftVersion: String,
        modloader: String
    ): Pair<List<File>,List<File>> {
        log.info("Preparing a list of mods to include in server pack...")
        val filesInModsDir: Collection<File> = File(modsDir).filteredWalk(modFileEndings, FilterType.ENDS_WITH, FileWalkDirection.TOP_DOWN, recursive = false)
        val serverMods: MutableList<ScannedMod> = mutableListOf()
        val disabledMods: MutableList<ScannedMod> = mutableListOf()
        val scannedMods: MutableList<ScannedMod> = mutableListOf()
        val scanningStopWatch = SimpleStopWatch().start()

        val scanner = modScanner.scannerFor(modloader, minecraftVersion)
        if (scanner != null) {
            scannedMods.addAll(scanner.scan(filesInModsDir))
        } else {
            // No scanner knows this loader, so nothing can be judged clientside. Keeping every mod
            // leaves a pack the user can trim; returning none would look like a successful run that
            // silently produced nothing.
            log.warn("Unrecognised modloader '$modloader'. Skipping sideness detection and including every mod.")
            scannedMods.addAll(filesInModsDir.map { ScannedMod(it) })
        }


        log.info("Scanned mods: ${scannedMods.size}")
        log.debug("Scanning of ${filesInModsDir.size} mods took ${scanningStopWatch.stop().getTime()}")

        if (apiProperties.isAutoExcludingModsEnabled) {
            for (mod in scannedMods) {
                val modName = mod.file.name
                if (mod.sideness == Sideness.CLIENT) {
                    log.warn("Automatically disabling mod: $modName")
                    disabledMods.add(mod)
                } else {
                    serverMods.add(mod)
                }
            }
        } else {
            log.info("Automatic clientside-only mod detection disabled.")
        }

        // Read once, not once per comparison: the value cannot change mid-generation, and its getter
        // reaches java.util.Properties -- a synchronized Hashtable -- twice on every read.
        val exclusionFilter = apiProperties.exclusionFilter
        val matcher = FilterMatcher(exclusionFilter, clientsideModsList + modWhitelist)
        log.info("Performing $exclusionFilter-type checks for user-specified clientside-only mod exclusion.")
        for (mod in scannedMods) {
            log.debug("Checking ${mod.file.name} (${mod.modID})")
            val modName = mod.file.name

            //Perform exclusions based on clientside-mods list
            val exclusionMatch = clientsideModsList.find { entry -> matcher.matches(modName, entry) }

            if (exclusionMatch != null) {
                if (!disabledMods.holds(mod)) {
                    disabledMods.add(mod)
                }
                serverMods.removeJar(mod)
                log.info("Disabling ${mod.file.name}. It matched clientside-mod: $exclusionMatch")
            } else if (disabledMods.holds(mod)) {
                log.debug("${mod.file.name} already disabled.")
            } else if (!serverMods.holds(mod)) {
                serverMods.add(mod)
                log.debug("No clientside-match, no whitelist-match. Keeping ${mod.file.name} enabled.")
            }
        }

        // A single pass suffices: the predicate reads only the mod and the whitelist, so nothing a removal
        // does can make a remaining entry start matching. (The dependency rescue below is different — each
        // rescue adds to serverMods and so can put further dependencies in play, hence the loop there.)
        disabledMods.removeIf { disabledMod ->
            val match = modWhitelist.find { entry -> matcher.matches(disabledMod.file.name, entry) }
            return@removeIf if (match != null) {
                log.info("Disabled mod ${disabledMod.file.name} is whitelisted by $match. Not disabling.")
                serverMods.add(disabledMod)
                true
            } else {
                false
            }
        }

        while (disabledMods.any { disabledMod ->                                    // Rip and tear until it is done.
                serverMods.find { serverMod ->                                      // There mustn't be a single dependency
                    serverMod.dependencies.filter { dependency ->                   // of a server mod left in the list of
                        dependency.sideness == Sideness.SERVER }.map { dep ->       // disabled mods.
                            dep.modID }.contains(disabledMod.modID)} != null}) {

            disabledMods.removeIf { disabledMod ->
                val match = serverMods.find { serverMod ->
                    serverMod.dependencies.filter { dependency ->
                        dependency.sideness == Sideness.SERVER }.map { dep ->
                            dep.modID }.contains(disabledMod.modID)}

                return@removeIf if (match != null) {
                    log.info("Disabled mod ${disabledMod.file.name} is a dependency for ${match.file.name}. Not disabling.")
                    serverMods.add(disabledMod)
                    true
                } else {
                    false
                }
            }
        }

        log.info("Mods included:")
        ListUtilities.printListToLogChunked(serverMods.map { it.file.name }, 5, "    ", true)
        log.info("Mods disabled:")
        ListUtilities.printListToLogChunked(disabledMods.map { it.file.name }, 5, "    ", true)

        return Pair(
            TreeSet<File>(serverMods.map { it.file }).toList(),
            TreeSet<File>(disabledMods.map { it.file }).toList()
        )
    }

}
