package de.griefed.serverpackcreator.api.modscanning

import de.griefed.serverpackcreator.api.utilities.TomlParser

actual class NeoForgeTomlScanner actual constructor(private val tomlParser: TomlParser): ForgeTomlScanner(tomlParser) {
    actual override val modsToml: String
        get() = "META-INF/neoforge.mods.toml"
}