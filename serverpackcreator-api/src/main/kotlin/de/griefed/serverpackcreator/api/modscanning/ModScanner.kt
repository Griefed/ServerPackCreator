/* Copyright (C) 2025 Griefed
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

/**
 * Easy-access class for scanning of mods inside a modpack. This class itself does not do much,
 * other than bringing the different mod-scanners to one place for ease-of-use.
 *
 * @param forgeAnnotationScanner For scanning `fml-cache-annotation.json`
 * @param fabricScanner     For scanning `fabric.mod.json`
 * @param quiltScanner      For scanning `quilt.mod.json`
 * @param forgeTomlScanner       For scanning `mods.toml`
 *
 * @author Griefed
 */
class ModScanner(
    val forgeAnnotationScanner: ForgeAnnotationScanner,
    val fabricScanner: FabricScanner,
    val quiltScanner: QuiltScanner,
    val forgeTomlScanner: ForgeTomlScanner,
    val neoForgeTomlScanner: NeoForgeTomlScanner
)