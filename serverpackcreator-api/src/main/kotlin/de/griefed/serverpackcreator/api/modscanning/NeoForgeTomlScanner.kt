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
package de.griefed.serverpackcreator.api.modscanning

import com.electronwill.nightconfig.toml.TomlParser

/**
 * `META-INF/neoforge.mods.toml`-based scanning of NeoForge mods for Minecraft **1.20.5** and newer.
 *
 * The boundary is NeoForge's descriptor rename, and [LoaderDescriptors.neoForgeUsesNeoToml] is where it is
 * stated — `ModScanner.scannerFor` asks that, never a literal. **A NeoForge mod below 1.20.5 still ships
 * `META-INF/mods.toml`** and is therefore read by [ForgeTomlScanner], which is why this scanner sees only
 * the newer era.
 *
 * @param tomlParser To parse .toml-files.
 * @Griefed
 */
class NeoForgeTomlScanner(tomlParser: TomlParser): ForgeTomlScanner(tomlParser) {
    override val modsToml: String
        get() = "META-INF/neoforge.mods.toml"
}