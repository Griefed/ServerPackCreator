/*
 * Copyright (C) 2023 Griefed.
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
package de.griefed.serverpackcreator.api.versionmeta.quilt

import de.griefed.serverpackcreator.api.utilities.File
import de.griefed.serverpackcreator.api.utilities.Optional
import de.griefed.serverpackcreator.api.utilities.URL
import de.griefed.serverpackcreator.api.versionmeta.Meta

/**
 * Quilt meta containing information about available Quilt versions and installers.
 *
 * @author Griefed
 */
expect class QuiltMeta : Meta {
    override fun latestLoader(): String
    override fun releaseLoader(): String
    override fun latestInstaller(): String
    override fun releaseInstaller(): String
    override fun loaderVersionsListAscending(): MutableList<String>
    override fun loaderVersionsListDescending(): List<String>
    override fun loaderVersionsArrayAscending(): Array<String>
    override fun loaderVersionsArrayDescending(): Array<String>
    override fun installerVersionsListAscending(): MutableList<String>
    override fun installerVersionsListDescending(): List<String>
    override fun installerVersionsArrayAscending(): Array<String>
    override fun installerVersionsArrayDescending(): Array<String>
    override fun latestInstallerUrl(): URL
    override fun releaseInstallerUrl(): URL
    override fun installerFor(version: String): Optional<File>
    override fun isInstallerUrlAvailable(version: String): Boolean
    override fun getInstallerUrl(version: String): Optional<URL>
    override fun isVersionValid(version: String): Boolean
    override fun isMinecraftSupported(minecraftVersion: String): Boolean
}