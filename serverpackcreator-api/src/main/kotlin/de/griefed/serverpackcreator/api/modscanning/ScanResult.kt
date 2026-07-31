package de.griefed.serverpackcreator.api.modscanning

import java.io.File

/**
 * Result of a mod-directory scan. Contains the information required to determine which mods get actually excluded.
 * Contains a list of mods determined to be excluded: [exclusions]
 * Contains a list of [dependencies] required by the mods which have been found to be excludable.
 *
 * @author Griefed
 */
class ScanResult() {
    /** Mods the scanners judged clientside, each with the mod id that decided it. */
    val exclusions: MutableList<Exclusion> = ArrayList(400)
    /** Mods kept because something excluded depends on them — removing these would break the server. */
    val dependencies: MutableList<Dependency> = ArrayList(400)

    constructor(exclusions: List<Exclusion>, dependencies: List<Dependency>) : this() {
        this.exclusions.addAll(exclusions)
        this.dependencies.addAll(dependencies)
    }
}

/**
 * Combination of the excluded mods ID and the file.
 *
 * @author Griefed
 */
class Exclusion(
    /** The mod id that matched, i.e. *why* this file is excluded. */
    val modId: String,
    /** The jar being excluded from the server pack. */
    val excludedMod: File
)

/**
 * Combination of a dependency mod ID, its filename and the mod ID which depends on it, if any.
 *
 * @author Griefed
 */
class Dependency(
    /** Id of the dependency that is required. */
    val dependencyID: String,
    /** Filename of the jar providing it. */
    val fileName: String,
    /** Id of the mod that declares the requirement, or `N/A` when it could not be read. */
    val modID: String = "N/A"
) {
    /** `filename (modId)`, the form used in logs and reports so a reader can find the jar. */
    val identifier: String = "$fileName ($modID)"
}