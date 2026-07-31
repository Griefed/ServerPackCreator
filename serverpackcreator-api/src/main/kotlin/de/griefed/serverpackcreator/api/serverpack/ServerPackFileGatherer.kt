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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.utilities.common.create
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.PatternSyntaxException
import kotlin.io.path.absolute

/**
 * Gatherer of all files which make up a server pack: resolves inclusion-specifications to
 * source-destination-pairs, applies in-/exclusion-filters, delegates the mods-directory to the
 * mod-list-compiler, and performs the copy itself. Extracted from ServerPackHandler (refactor
 * Phase 1d); ServerPackHandler remains the facade through which consumers access these
 * operations.
 */
class ServerPackFileGatherer(private val modListCompiler: ModListCompiler) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }


    /**
     * Copies all specified directories and mods, excluding clientside-only mods, from the modpack
     * directory into the server pack directory. If a `source/file;destination/file`
     * -combination is provided, the specified source-file is copied to the specified
     * destination-file. One of the reasons as to why it is recommended to run a given
     * ConfigurationModel through the ConfigurationHandler first, is because the ConfigurationHandler
     * will resolve links to their files first before then correcting the given
     * ConfigurationModel.
     *
     * @param modpackDir        Files and directories are copied into the server_pack directory inside
     * the modpack directory.
     * @param inclusions All directories and files therein to copy to the server pack.
     * @param clientMods        List of clientside-only mods to exclude from the server pack.
     * @param minecraftVersion  The Minecraft version the modpack uses.
     * @param destination       The destination where the files should be copied to.
     * @param modloader         The modloader used for mod sideness detection.
     * @author Griefed
     */
    fun copyFiles(
        modpackDir: String,
        inclusions: ArrayList<InclusionSpecification>,
        clientMods: List<String>,
        whitelist: List<String>,
        minecraftVersion: String,
        destination: String,
        modloader: String,
        overwrite: Boolean
    ) : List<File> {
        val exclusions = mutableListOf<Regex>()
        var acquired: List<ServerPackFile>
        val serverPackFiles: MutableList<ServerPackFile> = ArrayList(100000)
        val copiedFiles: MutableList<File> = ArrayList(10000)
        try {
            File(destination).create()
        } catch (ex: IOException) {
            log.error("Failed to create directory $destination")
        }

        if (inclusions.size == 1 && inclusions[0].source == "lazy_mode") {
            log.warn("!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!")
            log.warn("Lazy mode specified. This will copy the WHOLE modpack to the server pack. No exceptions.")
            log.warn("You will not receive any support for a server pack generated this way.")
            log.warn("Do not open an issue on GitHub if this configuration errors or results in a broken server pack.")
            log.warn("!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!")
            try {
                File(modpackDir).copyRecursively(File(destination), true)
            } catch (ex: IOException) {
                log.error("An error occurred copying the modpack to the server pack in lazy mode.", ex)
            }
            return copiedFiles
        }

        for (inclusion in inclusions) {
            acquired = getServerFiles(
                inclusion,
                modpackDir,
                destination,
                exclusions,
                clientMods,
                whitelist,
                minecraftVersion,
                modloader
            )
            serverPackFiles.addAll(acquired)
        }

        log.info("Ensuring files and/or directories are properly excluded.")
        serverPackFiles.removeIf { it: ServerPackFile ->
            excludeFileOrDirectory(modpackDir, it.sourceFile, exclusions)
        }
        log.info("Copying files to the server pack. This may take a while...")
        for (file in serverPackFiles) {
            try {
                copiedFiles.add(file.copy(overwrite))
            } catch (ex: IOException) {
                log.error(
                    "An error occurred trying to copy " + file.sourceFile + " to " + file.destinationFile + ".",
                    ex
                )
            }
        }
        return copiedFiles
    }

    /** Resolve one inclusion into the concrete files to copy, applying its filters and destination. */
    fun getServerFiles(
        inclusion: InclusionSpecification,
        modpackDir: String,
        destination: String,
        exclusions: MutableList<Regex>,
        clientMods: List<String>,
        modWhitelist: List<String>,
        minecraftVersion: String,
        modloader: String
    ): List<ServerPackFile> {
        val serverPackFiles = mutableListOf<ServerPackFile>()
        val clientDir = File(modpackDir, inclusion.source)
        val serverDir = File(destination, inclusion.source)
        val acquired: List<ServerPackFile>
        val processed: List<ServerPackFile>
        val serverPackFile: ServerPackFile
        val inclusionSourceFile = File(inclusion.source).absoluteFile
        val inclusionDestinationFile = File(destination, inclusionSourceFile.name).absoluteFile
        when {
            inclusion.isGlobalFilter() -> {
                if (inclusion.hasExclusionFilter()) {
                    try {
                        exclusions.add(inclusion.exclusionFilter!!.toRegex())
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid exclusion-regex specified: ${inclusion.exclusionFilter}.",ex)
                    }
                }
            }

            inclusion.hasDestination() -> {
                val destinationFile = File(destination,inclusion.destination ?: inclusionSourceFile.name)
                when {
                    clientDir.isDirectory -> {
                        acquired = getExplicitFiles(clientDir.absolutePath, inclusion.destination!!, modpackDir, destination)
                        processed = runFilters(acquired, inclusion, modpackDir)
                        serverPackFiles.addAll(processed)
                    }
                    clientDir.absoluteFile.isFile -> {
                        serverPackFile = ServerPackFile(clientDir, destinationFile)
                        serverPackFiles.add(serverPackFile)
                    }
                    inclusionSourceFile.isDirectory -> {
                        acquired = getExplicitFiles(inclusion.source, inclusion.destination!!, modpackDir, destination)
                        processed = runFilters(acquired, inclusion, modpackDir)
                        serverPackFiles.addAll(processed)
                    }
                    inclusionSourceFile.isFile -> {
                        serverPackFile = ServerPackFile(inclusionSourceFile, destinationFile)
                        serverPackFiles.add(serverPackFile)
                    }
                    else -> {
                        serverPackFile = ServerPackFile(inclusionSourceFile, destinationFile)
                        serverPackFiles.add(serverPackFile)
                    }
                }
            }

            inclusion.source == "mods" -> {
                try {
                    serverDir.create()
                } catch (ignored: IOException) {
                    // The server dir may already exist from an earlier step; an actual write
                    // failure would resurface when the mods are copied into it below.
                }
                acquired = mutableListOf()
                val mods = modListCompiler.compileModList(clientDir.absolutePath, clientMods, modWhitelist, minecraftVersion, modloader)
                for (mod in mods.first) {
                    acquired.add(ServerPackFile(mod, File(serverDir, mod.name)))
                }
                var destinationName: String
                for (disabled in mods.second) {
                    destinationName = if (disabled.name.endsWith("disabled")) {
                        disabled.name
                    } else {
                        "${disabled.name}.disabled"
                    }
                    acquired.add(ServerPackFile(disabled, File(serverDir, destinationName)))
                }
                processed = runFilters(acquired, inclusion, modpackDir)
                serverPackFiles.addAll(processed)
            }

            clientDir.absoluteFile.isDirectory -> {
                acquired = getDirectoryFiles(clientDir.absolutePath, serverDir.absolutePath)
                processed = runFilters(acquired, inclusion, modpackDir)
                serverPackFiles.addAll(processed)
            }

            clientDir.absoluteFile.isFile -> {
                serverPackFile = ServerPackFile(clientDir, serverDir)
                serverPackFiles.add(serverPackFile)
            }

            inclusionSourceFile.isFile -> {
                serverPackFile = ServerPackFile(inclusionSourceFile, inclusionDestinationFile)
                serverPackFiles.add(serverPackFile)
            }

            inclusionSourceFile.isDirectory -> {
                acquired = getDirectoryFiles(inclusionSourceFile.absolutePath, inclusionDestinationFile.absolutePath)
                processed = runFilters(acquired, inclusion, modpackDir)
                serverPackFiles.addAll(processed)
            }

            else -> {
                acquired = getDirectoryFiles(clientDir.absolutePath, serverDir.absolutePath)
                processed = runFilters(acquired, inclusion, modpackDir)
                serverPackFiles.addAll(processed)
            }
        }
        return serverPackFiles
    }


    /**
     * Check all files in [acquired] for matches with [inclusionSpec]. Every match found is returned as a compiled list.
     *
     * @author Griefed
     */
    private fun runFilters(
        acquired: List<ServerPackFile>,
        inclusionSpec: InclusionSpecification,
        modpackDir: String
    ): List<ServerPackFile> {
        val processed = mutableListOf<ServerPackFile>()
        val inclusionFilter = if (inclusionSpec.inclusionFilter.isNullOrBlank()) {
            null
        } else {
            try {
                inclusionSpec.inclusionFilter!!.toRegex()
            } catch (ex: PatternSyntaxException) {
                log.error("Invalid inclusion-regex specified: ${inclusionSpec.inclusionFilter}.",ex)
                null
            }
        }
        val exclusionFilter = if (inclusionSpec.exclusionFilter.isNullOrBlank()) {
            null
        } else {
            try {
                inclusionSpec.exclusionFilter!!.toRegex()
            } catch (ex: PatternSyntaxException) {
                log.error("Invalid exclusion-regex specified: ${inclusionSpec.exclusionFilter}.",ex)
                null
            }
        }
        if (inclusionFilter != null) {
            for (file in acquired) {
                if (file.sourceFile.absolutePath.replace(modpackDir + File.separator, "").matches(inclusionFilter)) {
                    processed.add(file)
                    log.info("Including ${file.sourceFile} due to inclusion-filter $inclusionFilter.")
                }
            }
        } else {
            processed.addAll(acquired)
        }
        if (exclusionFilter != null) {
            processed.removeIf { file ->
                val source = file.sourceFile.absolutePath.replace(modpackDir + File.separator, "")
                return@removeIf if (source.matches(exclusionFilter)) {
                    log.info("Excluding ${file.sourceFile} due to exclusion-filter $exclusionFilter.")
                    true
                } else {
                    false
                }
            }
        }
        return processed
    }


    /**
     * Gather a list of all files from an explicit source;destination-combination. If the source is a
     * file, a singular [ServerPackFile] is returned. If the source is a directory, then all
     * files in said directory are returned.
     *
     * @param source source-file/directory
     * @param destination destination-file/directory
     * @param modpackDir  The modpack-directory.
     * @param serverPackDestination The destination, normally the server pack-directory.
     * @return List of [ServerPackFile].
     * @author Griefed
     */
    fun getExplicitFiles(
        source: String,
        destination: String,
        modpackDir: String,
        serverPackDestination: String
    ): MutableList<ServerPackFile> {
        val serverPackFiles: MutableList<ServerPackFile> = ArrayList(100)
        if (File(modpackDir, source).isFile) {
            serverPackFiles.add(
                ServerPackFile(
                    File(modpackDir, source), File(serverPackDestination, destination)
                )
            )
        } else if (File(modpackDir, source).isDirectory) {
            serverPackFiles.addAll(
                getDirectoryFiles(
                    modpackDir + File.separator + source, serverPackDestination + File.separator + destination
                )
            )
        } else if (File(source).isFile) {
            serverPackFiles.add(
                ServerPackFile(
                    File(source), File(serverPackDestination, destination)
                )
            )
        } else if (File(source).isDirectory) {
            serverPackFiles.addAll(
                getDirectoryFiles(
                    source, serverPackDestination + File.separator + destination
                )
            )
        }
        return serverPackFiles
    }


    /**
     * Recursively acquire all files and directories inside the given save-directory as a list of
     * [ServerPackFile].
     *
     * @param clientDir   Target directory in the server pack. Usually the name of the world.
     * @param directory   The save-directory.
     * @param destination The destination of the server pack.
     * @return List of [ServerPackFile] which will be included in the server pack.
     * @author Griefed
     */
    @Suppress("unused")
    fun getSaveFiles(clientDir: String, directory: String, destination: String): List<ServerPackFile> {
        val serverPackFiles: MutableList<ServerPackFile> = ArrayList(2000)
        try {
            Files.walk(Paths.get(clientDir)).use {
                for (path in it) {
                    try {
                        serverPackFiles.add(
                            ServerPackFile(
                                path,
                                Paths.get(destination + File.separator + directory.substring(6))
                                    .resolve(Paths.get(clientDir).relativize(path))
                            )
                        )
                    } catch (ex: UnsupportedOperationException) {
                        log.error("Couldn't gather file $path from directory $clientDir.", ex)
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("An error occurred during the copy-procedure to the server pack.", ex)
        }
        return serverPackFiles
    }


    /**
     * Recursively acquire all files and directories inside the given directory as a list of
     * [ServerPackFile].
     *
     * @param source      The source-directory.
     * @param destination The server pack-directory.
     * @return List of files and folders of the server pack.
     * @author Griefed
     */
    fun getDirectoryFiles(source: String, destination: String): List<ServerPackFile> {
        val serverPackFiles: MutableList<ServerPackFile> = ArrayList(100)
        try {
            Files.walk(Paths.get(source).absolute()).use {
                for (path in it) {
                    try {
                        val pathFile = path.toFile().absolutePath
                        val sourceFile = File(source).absolutePath
                        val destFile = File(destination, pathFile.replace(sourceFile, ""))
                        serverPackFiles.add(
                            ServerPackFile(
                                path.toFile(),
                                destFile
                            )
                        )
                    } catch (ex: UnsupportedOperationException) {
                        log.error("Couldn't gather file $path from directory $source.", ex)
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("An error occurred gathering files to copy to the server pack for directory $source.", ex)
        }

        return serverPackFiles
    }


    /**
     * Check whether the given file or directory should be excluded from the server pack.
     *
     * @param modpackDir     The directory where the modpack resides in. Used to filter out any
     * unwanted directories using the property `de.griefed.serverpackcreator.configuration.directories.shouldexclude`.
     * @param fileToCheckFor The file or directory to check whether it should be excluded from the
     * server pack.
     * @param exclusions     Files or directories determined by ServerPackCreator to be excluded from
     * the server pack
     * @return `true` if the file or directory was determined to be excluded from the server
     * pack.
     * @author Griefed
     */
    fun excludeFileOrDirectory(modpackDir: String, fileToCheckFor: File, exclusions: List<Regex>): Boolean {
        val cleaned = fileToCheckFor.absolutePath.replace(File(modpackDir).absolutePath + File.separator, "")
        return exclusions.any { regex ->
            if (cleaned.matches(regex)) {
                log.info("Excluding '$cleaned' as per global exclusion filter '$regex'.")
                return@any true
            } else {
                return@any false
            }
        }
    }


    /**
     * Walk through the specified directory and add a [ServerPackFile] for every file/folder
     * which matches the given regex.
     *
     * @param source          The source-directory to walk through and perform regex-matches in.
     * @param destination     The destination-directory where a matched file should be copied to,
     * usually the server pack directory.
     * @param regex           Regex with which to perform matches against files in the
     * source-directory.
     * @param serverPackFiles List of files to copy to the server pack to which any matched file will
     * be added to.
     * @author Griefed
     */
    @Suppress("unused")
    fun regexWalk(
        source: File, destination: String, regex: Regex, serverPackFiles: MutableList<ServerPackFile>
    ) {
        var toMatch: String
        try {
            Files.walk(source.toPath()).use {
                for (path in it) {
                    toMatch = path.toFile().absolutePath.replace(source.absolutePath, "")
                    if (toMatch.startsWith(File.separator)) {
                        toMatch = toMatch.substring(1)
                    }
                    if (toMatch.matches(regex)) {
                        val add = Paths.get(destination + File.separator + source.name)
                            .resolve(source.toPath().relativize(path))
                        serverPackFiles.add(
                            ServerPackFile(
                                path, add
                            )
                        )
                        log.debug("Including through regex-match:")
                        log.debug("    SOURCE: $path")
                        log.debug("    DESTINATION: $add")
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("Couldn't gather all files from ${source.absolutePath} for filter \"$regex\".", ex)
        }
    }

}
