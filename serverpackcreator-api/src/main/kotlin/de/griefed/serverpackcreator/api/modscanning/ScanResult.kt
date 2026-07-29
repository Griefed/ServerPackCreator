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
    val exclusions: MutableList<Exclusion> = ArrayList(400)
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
class Exclusion(val modId: String, val excludedMod: File)

/**
 * Combination of a dependency mod ID, its filename and the mod ID which depends on it, if any.
 *
 * @author Griefed
 */
class Dependency(val dependencyID: String, val fileName: String, val modID: String = "N/A") {
    val identifier: String = "$fileName ($modID)"
}