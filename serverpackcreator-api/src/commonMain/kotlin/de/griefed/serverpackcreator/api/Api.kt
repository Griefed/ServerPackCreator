/* Copyright (C) 2024  Griefed
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

import de.griefed.serverpackcreator.api.utilities.TomlParser
import de.griefed.serverpackcreator.api.utilities.common.BooleanUtilities
import de.griefed.serverpackcreator.api.utilities.common.ListUtilities
import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import mu.KotlinLogging

/**
 * Basic implementations of the API-class holding properties and functions/methods which should be provided to the given
 * implementer.
 *
 * @author Griefed
 */
abstract class Api<F> {
    protected val log = KotlinLogging.logger {}
    protected val versionsRegex = ".*(alpha|beta|dev).*".toRegex()
    protected val xmlJsonRegex = ".*\\.(xml|json)".toRegex()
    protected var setupWasRun: Boolean = false

    /**
     * This instances common boolean utilities used across ServerPackCreator.
     *
     * @return Common boolean utilities used across ServerPackCreator.
     * @author Griefed
     */
    val booleanUtilities: BooleanUtilities = BooleanUtilities()

    /**
     * This instances common String utilities used across ServerPackCreator.
     *
     * @return Common String utilities used across ServerPackCreator.
     * @author Griefed
     */
    val stringUtilities: StringUtilities = StringUtilities()


    /**
     * This instances common list utilities used across ServerPackCreator.
     *
     * @return Common list utilities used across ServerPackCreator.
     * @author Griefed
     */
    val listUtilities: ListUtilities = ListUtilities()

    /**
     * Convenience method to set up ServerPackCreator.
     *
     * @author Griefed
     */
    abstract fun setup(force: Boolean = false): ApiWrapper

    /**
     * Stage one of starting ServerPackCreator.
     *
     * Creates and prepares the environment for ServerPackCreator to run by creating required
     * directories and copying required files from the JAR-file to the filesystem. Some of these files
     * can and should be edited by a given user, others however, not.
     *
     *  * Checks the read- and write-permissions of ServerPackCreators base-directory.
     *  * Copies the `README.md` from the JAR to the home-directory.
     *  * Copies the `HELP.md` from the JAR to the home-directory.
     *  * Copies the `CHANGELOG.md` from the JAR to the home-directory.
     *  * Copies the `LICENSE` from the JAR to the home-directory.
     *  * Copies the fallback version-manifests to the manifests.
     *  * Creates default directories:
     *
     *  * server_files
     *  * work
     *  * temp
     *  * work/modpacks
     *  * server-packs (depending on the users settings, this may be anywhere on the users system)
     *  * plugins
     *  * plugins/config
     *
     *  * Example `disabled.txt`-file in plugins/disabled.txt.
     *  * Creates the default `server.properties` if it doesn't exist.
     *  * Creates the default `server-icon.png` if it doesn't exist.
     *  * Creates the default PowerShell and Shell script templates or overwrites them if they already exist.
     *  * Determines whether this instance of ServerPackCreator was updated from a previous version.
     *
     * If an update was detected, and migrations are available for any of the steps of the update, they are executed,
     * thus ensuring users are safe to update their instances. Writes ServerPackCreator and system information to the
     * console and logs, important for error reporting and debugging.
     *
     * @author Griefed
     */
    abstract fun stageOne()

    /**
     * Initialize [de.griefed.serverpackcreator.api.versionmeta.VersionMeta], [ConfigurationHandler].
     *
     * @author Griefed
     */
    abstract fun stageTwo()

    /**
     * Initialize [ApiPlugins], [de.griefed.serverpackcreator.api.modscanning.ModScanner] (consisting of [TomlParser],
     * [de.griefed.serverpackcreator.api.modscanning.AnnotationScanner],
     * [de.griefed.serverpackcreator.api.modscanning.FabricScanner],
     * [de.griefed.serverpackcreator.api.modscanning.TomlScanner],
     * [de.griefed.serverpackcreator.api.modscanning.QuiltScanner]),
     * [ServerPackHandler].
     *
     * @author Griefed
     */
    abstract fun stageThree()

    /**
     * Check whether the specified server-files file exists and create it if it doesn't.
     *
     * @param fileToCheckFor The file which is to be checked for whether it exists and if it doesn't,
     * should be created.
     * @return `true` if the file was generated.
     * @author Griefed
     */
    abstract fun checkServerFilesFile(fileToCheckFor: F): Boolean

    /**
     * Overwrite the specified server-files file, even when it exists. Used to ensure files like the
     * default script templates are always up-to-date.
     *
     * @param fileToOverwrite The file which is to be overwritten. If it exists. it is first deleted,
     * then extracted from our JAR-file.
     * @author Griefed
     */
    abstract fun overwriteServerFilesFile(fileToOverwrite: F)
}