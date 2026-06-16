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
import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.regex.PatternSyntaxException

/**
 * Validator for the inclusions-concern of a server pack configuration: existing sources, valid
 * destinations, valid in-/exclusion-filter regexes and the lazy-mode special-case. Extracted
 * from ConfigurationHandler (refactor Phase 1c); ConfigurationHandler remains the facade through
 * which consumers access these checks.
 */
class InclusionsValidator {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Checks whether the passed list of directories which are supposed to be in the modpack directory
     * is empty, or whether all directories in the list exist in the modpack directory. If the user
     * specified a `source/file;destination/file`-combination, it is checked whether the
     * specified source-file exists on the host.
     *
     * @param inclusions Directories, or `source/file;destination/file`-combinations, to
     * check for existence.
     * `source/file;destination/file`-combinations must be absolute
     * paths to the source-file.
     * @param modpackDir        Path to the modpack directory in which to check for existence of the
     * passed list of directories.
     * @return `true` if every directory was found in the modpack directory. If any single one
     * was not found, false is returned.
     * @author Griefed
     */
    fun checkInclusions(
        inclusions: MutableList<InclusionSpecification>,
        modpackDir: String,
        configCheck: ConfigCheck = ConfigCheck(),
        printLog: Boolean = true
    ): ConfigCheck {
        val hasLazy = inclusions.any { entry -> entry.source == "lazy_mode" }
        if (inclusions.isEmpty()) {
            if (printLog) {
                log.error("No directories or files specified for copying. This would result in an empty server pack.")
            }
            configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_empty.toString())
        } else if (inclusions.size == 1 && hasLazy) {
            if (printLog) {
                log.warn(
                    "!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!"
                )
                log.warn(
                    "Lazy mode specified. This will copy the WHOLE modpack to the server pack. No exceptions."
                )
                log.warn(
                    "You will not receive support from me for a server pack generated this way."
                )
                log.warn(
                    "Do not open an issue on GitHub if this configuration errors or results in a broken server pack."
                )
                log.warn(
                    "!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!"
                )
            }
        } else {
            if (inclusions.size > 1 && hasLazy && printLog) {
                log.warn(
                    "You specified lazy mode in your configuration, but your copyDirs configuration contains other"
                            + " entries. To use the lazy mode, only specify \"lazy_mode\" and nothing else. Ignoring lazy mode."
                )
            }
            inclusions.removeIf { entry -> entry.source == "lazy_mode" }
            for (inclusion in inclusions) {
                if (inclusion.isGlobalFilter()) {
                    continue
                }
                val modpackSource = File(modpackDir, inclusion.source).absoluteFile
                if (!File(inclusion.source).absoluteFile.exists() && !modpackSource.exists()) {
                    if (printLog) {
                        log.error("Source ${inclusion.source} does not exist. Please specify existing files.")
                    }
                    configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_filenotfound(inclusion.source))
                }
                if (inclusion.hasDestination()
                    && !StringUtilities.checkForInvalidPathCharacters(inclusion.destination!!)) {
                    log.warn("Invalid destination specified: ${inclusion.destination}.")
                    inclusion.destination = null
                    configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_destination(inclusion.source))
                }
                if (inclusion.hasInclusionFilter()) {
                    try {
                        inclusion.inclusionFilter!!.toRegex()
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid inclusion-regex specified: ${inclusion.inclusionFilter}.", ex)
                        configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_inclusion(inclusion.inclusionFilter ?: ""))
                    }
                }
                if (inclusion.hasExclusionFilter()) {
                    try {
                        inclusion.exclusionFilter!!.toRegex()
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid exclusion-regex specified: ${inclusion.exclusionFilter}.", ex)
                        configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_inclusion(inclusion.exclusionFilter ?: ""))
                    }
                }
            }
        }
        return configCheck
    }
}
