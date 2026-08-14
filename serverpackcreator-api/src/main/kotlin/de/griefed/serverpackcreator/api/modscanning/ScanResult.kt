package de.griefed.serverpackcreator.api.modscanning

import java.io.File

class ScannedMod(
    val file: File,
    val modID: String = file.nameWithoutExtension,
    val sideness: Sideness = Sideness.SERVER,
    val dependencies: List<ModDependency> = emptyList()
) {
    override fun toString(): String {
        return "ScannedMod(file=$file, modID='$modID', sideness=$sideness, dependencies=${dependencies.joinToString(", ")})"
    }
}

class ModDependency(val modID: String, val sideness: Sideness = Sideness.SERVER) {
    override fun toString(): String {
        return "ModDependency(modID='$modID', sideness=$sideness)"
    }
}

enum class Sideness {
    SERVER,
    CLIENT
}

/**
 * Reduces the sidenesses a scanner read from one descriptor to the single verdict for that mod: it belongs
 * on a server unless *nothing* it declared put it there.
 *
 * A scanner appends one entry per signal it finds — a declared environment, the side demanded of the platform,
 * the sideness of a second mod bundled in the same jar. A mod is only clientside when every one of those said
 * so, because dropping a mod that does belong on the server breaks the pack, while keeping a superfluous one
 * costs a few megabytes. An empty list therefore reads as CLIENT by construction, which is why each scanner
 * appends SERVER on the paths where it could not determine anything.
 */
internal fun sidenessOf(sidenesses: List<Sideness>): Sideness =
    if (sidenesses.any { it == Sideness.SERVER }) Sideness.SERVER else Sideness.CLIENT
