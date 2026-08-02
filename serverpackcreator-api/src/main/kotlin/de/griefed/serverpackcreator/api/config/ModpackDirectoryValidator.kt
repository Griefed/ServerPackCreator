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
package de.griefed.serverpackcreator.api.config

import Translations
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File

/**
 * Validator for the modpack-directory-concern of a server pack configuration: existence, type
 * and the absence of the overrides-directory which marks uninstalled modpack-exports. Extracted
 * from ConfigurationHandler (refactor Phase 1c); ConfigurationHandler remains the facade through
 * which consumers access these checks.
 */
class ModpackDirectoryValidator {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Check the passed directory for existence and whether it is a directory, rather than a file.
     *
     * @param modpackDir The modpack directory.
     * @param configCheck Collection of encountered errors, if any, for convenient result-checks.
     * @return `true` if the directory exists.
     * @author Griefed
     */
    fun checkModpackDir(
        modpackDir: String,
        configCheck: ConfigCheck = ConfigCheck(),
        printLog: Boolean = true
    ): ConfigCheck {
        val modpack = File(modpackDir)
        if (modpackDir.isEmpty()) {
            if (printLog) {
                log.error("Modpack directory not specified. Please specify an existing directory.")
            }
            configCheck.modpackErrors.add(Translations.configuration_log_error_checkmodpackdir.toString())
        } else if (!modpack.exists()) {
            if (printLog) {
                log.warn("Couldn't find directory $modpackDir.")
            }
            configCheck.modpackErrors.add(Translations.configuration_log_error_modpackdirectory(modpackDir))
        } else if (modpack.isDirectory){
            val files = modpack.listFiles { entry -> entry.isDirectory }
            if (files.any { entry -> entry.name == "overrides" }) {
                log.error("Modpack contains directory \"overrides\". Modpacks must be installed through a client such as CurseForge, GDLauncher, MultiMC etc. Full modpacks shouldn't contain the overrides directory anymore.")
                configCheck.modpackErrors.add(Translations.configuration_log_error_modpack_overrides.toString())
            }
        }
        return configCheck
    }
}
