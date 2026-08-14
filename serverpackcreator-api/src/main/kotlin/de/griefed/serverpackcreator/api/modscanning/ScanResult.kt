package de.griefed.serverpackcreator.api.modscanning

import java.io.File

class ScannedMod(val file: File) {

    var modID: String = "N/A"
    var sideness: Sideness = Sideness.SERVER
    val dependencies: MutableList<ModDependency> = mutableListOf()

    override fun toString(): String {
        return "ScannedMod(file=$file, modID='$modID', sideness=$sideness, dependencies=${dependencies.joinToString(", ")})"
    }
}

class ModDependency(val modID: String) {
    var sideness: Sideness = Sideness.SERVER
}

enum class Sideness {
    SERVER,
    CLIENT
}