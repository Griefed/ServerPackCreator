package de.griefed.serverpackcreator.api.modscanning

import de.griefed.serverpackcreator.api.utilities.TomlParser

expect class NeoForgeTomlScanner(tomlParser: TomlParser) : ForgeTomlScanner {
    override val modsToml: String
}