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

import de.griefed.serverpackcreator.api.utilities.common.isNotValidZipFile
import net.lingala.zip4j.ZipFile
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.nio.file.FileSystemAlreadyExistsException
import java.nio.file.Paths
import java.nio.file.ProviderNotFoundException
import java.util.*

/**
 * Inspector for modpack ZIP-archives: lists their contents and checks their validity as
 * modpacks (full-pack layout with mods- and config-directories in the archive-root). Extracted
 * from ConfigurationHandler (refactor Phase 1c); ConfigurationHandler remains the facade
 * through which consumers access these checks.
 */
class ModpackZipInspector {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val zipCheck = "^\\w+[/\\\\]$".toRegex()

    /**
     * Check a given ZIP-archives contents. If the ZIP-archive only contains one directory, or if it
     * contains neither the mods nor the config directories, consider it invalid.
     *
     * @param pathToZip         Path to the ZIP-file to check.
     * @author Griefed
     */
    fun checkZipArchive(pathToZip: String, configCheck: ConfigCheck = ConfigCheck()): ConfigCheck {
        try {
            ZipFile(Paths.get(pathToZip).toFile()).use {
                if (it.isNotValidZipFile()) {
                    configCheck.modpackErrors.add("$pathToZip is not a valid ZIP-file.")
                    return configCheck
                }
            }
        } catch (ex: IOException) {
            log.error("Could not validate ZIP-file $pathToZip.", ex)
            configCheck.modpackErrors.add("Could not validate ZIP-file $pathToZip.")
            return configCheck
        }
        try {
            val foldersInModpackZip = getDirectoriesInModpackZipBaseDirectory(Paths.get(pathToZip).toFile())

            // If the ZIP-file only contains one directory, assume it is overrides and return true to
            // indicate invalid configuration.
            if (foldersInModpackZip.size == 1) {
                log.error(
                    "The ZIP-file you specified only contains one directory: ${foldersInModpackZip[0]}. "
                            + "ZIP-files for ServerPackCreator must be full modpacks, with all their contents being in the root of the ZIP-file."
                )

                
                configCheck.modpackErrors.add(Translations.configuration_log_error_zip_overrides(foldersInModpackZip[0]))
                return configCheck

                // If the ZIP-file does not contain the mods or config directories, consider it invalid.
            } else if (!foldersInModpackZip.contains("mods/") || !foldersInModpackZip.contains("config/")) {
                log.error(
                    "The ZIP-file you specified does not contain the mods or config directories. What use is a modded server without mods and their configurations?"
                )

                
                configCheck.modpackErrors.add(Translations.configuration_log_error_zip_modsorconfig.toString())
                return configCheck
            }
        } catch (ex: IOException) {
            log.error("Couldn't acquire directories in ZIP-file.", ex)

            
            configCheck.modpackErrors.add(Translations.configuration_log_error_zip_directories.toString())
            return configCheck
        }
        return configCheck
    }

    /**
     * Update the destination to which the ZIP-archive will the extracted to, based on whether a
     * directory of the same name already exists.
     *
     * @param destination The destination to where the ZIP-archive was about to be extracted to.
     * @return The destination where the ZIP-archive will be extracted to.
     * @author Griefed
     */
    @Suppress("unused")
    fun unzipDestination(destination: String): String {
        var dest = destination
        if (File(dest).isDirectory || File("${dest}_0").isDirectory) {
            var incrementation = 0
            while (File("${dest}_$incrementation").isDirectory) {
                incrementation++
            }
            dest = "${dest}_$incrementation"
        } else {
            dest += "_0"
        }
        return File(dest).path
    }

    /**
     * Acquire a list of directories in the base-directory of a ZIP-file.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All directories in the base-directory of the ZIP-file.
     * @author Griefed
     */
    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    fun getDirectoriesInModpackZipBaseDirectory(zipFile: File): List<String> {
        val baseDirectories: TreeSet<String> = TreeSet()
        var headerBeginning: String
        ZipFile(zipFile).use {
            val headers = it.fileHeaders
            for (header in headers) {
                try {
                    headerBeginning = header.fileName.substring(0,header.fileName.indexOfFirst { char -> char == '/' } + 1)
                    log.trace("Header beginning $headerBeginning")
                    if (headerBeginning.matches(zipCheck)) {
                        baseDirectories.add(headerBeginning)
                    }
                } catch (ex: StringIndexOutOfBoundsException) {
                    log.debug("Could not parse ${header.fileName}")
                }
            }
        }
        return baseDirectories.toList()
    }

    /**
     * Acquire a list of all files and directories in a ZIP-file.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All files and directories in the ZIP-file.
     * @author Griefed
     */
    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    fun getAllFilesAndDirectoriesInModpackZip(zipFile: File): List<String> {
        val filesAndDirectories: MutableList<String> = ArrayList(100)
        try {
            filesAndDirectories.addAll(getDirectoriesInModpackZip(zipFile))
        } catch (ex: IOException) {
            log.error("Could not acquire file or directory from ZIP-archive.", ex)
        }
        try {
            filesAndDirectories.addAll(getFilesInModpackZip(zipFile))
        } catch (ex: IOException) {
            log.error("Could not acquire file or directory from ZIP-archive.", ex)
        }
        return filesAndDirectories
    }

    /**
     * Acquire a list of all directories in a ZIP-file. The resulting list excludes files.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All directories in the ZIP-file.
     * @author Griefed
     */
    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    fun getDirectoriesInModpackZip(zipFile: File): List<String> {
        val directories: MutableList<String> = ArrayList(100)
        ZipFile(zipFile).use {
            for (header in it.fileHeaders) {
                if (header.isDirectory) {
                    directories.add(header.fileName)
                }
            }
        }
        return directories
    }

    /**
     * Acquire a list of all files in a ZIP-file. The resulting list excludes directories.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All files in the ZIP-file.
     * @author Griefed
     */
    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    fun getFilesInModpackZip(zipFile: File): List<String> {
        val files: MutableList<String> = ArrayList(100)
        ZipFile(zipFile).use {
            for (header in it.fileHeaders) {
                if (!header.isDirectory) {
                    files.add(header.fileName)
                }
            }
        }
        return files
    }
}
