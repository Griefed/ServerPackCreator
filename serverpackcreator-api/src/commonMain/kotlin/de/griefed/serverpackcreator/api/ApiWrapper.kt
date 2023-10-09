/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.modscanning.*
import de.griefed.serverpackcreator.api.utilities.TomlParser
import de.griefed.serverpackcreator.api.utilities.common.*
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta

/**
 * API wrapper, allowing you to conveniently initialize, setup and use the different aspects of ServerPackCreator.
 *
 * @author Griefed
 */
expect class ApiWrapper {

    val apiProperties: ApiProperties
    val fileUtilities: FileUtilities
    val jarUtilities: JarUtilities
    val systemUtilities: SystemUtilities
    val jsonUtilities: JsonUtilities
    val xmlUtilities: XmlUtilities
    val tomlParser: TomlParser
    val tomlScanner: TomlScanner

    var webUtilities: WebUtilities?
        private set
    var utilities: Utilities?
        private set
    var versionMeta: VersionMeta?
        private set
    var configurationHandler: ConfigurationHandler?
        private set
    var apiPlugins: ApiPlugins?
        private set
    var serverPackHandler: ServerPackHandler?
        private set
    var modScanner: ModScanner?
        private set
    var annotationScanner: AnnotationScanner?
        private set
    var fabricScanner: FabricScanner?
        private set
    var quiltScanner: QuiltScanner?
        private set
}