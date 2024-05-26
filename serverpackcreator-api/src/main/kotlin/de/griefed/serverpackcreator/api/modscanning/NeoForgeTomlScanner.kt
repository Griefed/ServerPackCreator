package de.griefed.serverpackcreator.api.modscanning

import com.electronwill.nightconfig.toml.TomlParser

class NeoForgeTomlScanner(tomlParser: TomlParser): ForgeTomlScanner(tomlParser) {
    override val modsToml: String
        get() = "META-INF/neoforge.mods.toml"
}