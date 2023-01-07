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

import de.griefed.serverpackcreator.api.utilities.common.escapePath
import mu.KotlinLogging

abstract class ServerPack<F, TS, TF> {
    protected val log = KotlinLogging.logger {}
    protected val modFileEndings = listOf("jar", "disabled")
    protected val ending = "^\\.[0-9a-zA-Z]+$".toRegex()
    protected val variables = """
        MINECRAFT_VERSION=SPC_MINECRAFT_VERSION_SPC
        MODLOADER=SPC_MODLOADER_SPC
        MODLOADER_VERSION=SPC_MODLOADER_VERSION_SPC
        LEGACYFABRIC_INSTALLER_VERSION=SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC
        FABRIC_INSTALLER_VERSION=SPC_FABRIC_INSTALLER_VERSION_SPC
        QUILT_INSTALLER_VERSION=SPC_QUILT_INSTALLER_VERSION_SPC
        MINECRAFT_SERVER_URL=SPC_MINECRAFT_SERVER_URL_SPC
        JAVA_ARGS="SPC_JAVA_ARGS_SPC"
        JAVA="SPC_JAVA_SPC"
    """.trimIndent()

    /**
     * Acquire the destination directory in which the server pack will be generated. The directory in
     * which the server pack will be created has all its spaces replaces with underscores, so
     * `Survive Create Prosper 4 - 5.0.1` would become `Survive_Create_Prosper_4_-_5.0.1 `
     * Even though it is the year 2022, spaces in paths can and do still cause trouble. Such as for
     * Powershell scripts. Powershell throws a complete fit if the path contains spaces....soooooo, we
     * remove them. Better safe than sorry.
     *
     * @param packConfig Model containing the modpack directory of the modpack from which the
     * server pack will be generated.
     * @return The complete path to the directory in which the server pack will be generated.
     * @author Griefed
     */
    abstract fun getServerPackDestination(packConfig: Pack<*, *, *>): String

    /**
     * Create a server pack from a given instance of [PackConfig].
     *
     * @param packConfig An instance of [PackConfig] which contains the
     * configuration of the modpack from which the server pack is to be
     * created.
     * @return `true` if the server pack was successfully generated.
     * @author Griefed
     */
    abstract fun run(packConfig: PackConfig): Boolean

    /**
     * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure
     * newly generated server pack is as clean as possible. This will completely empty the server pack
     * directory, so use with caution!
     *
     * @param deleteZip   Whether to delete the server pack ZIP-archive.
     * @param destination The destination at which to clean up in.
     * @author Griefed
     */
    abstract fun cleanupEnvironment(deleteZip: Boolean, destination: String)

    /**
     * Copies all specified directories and mods, excluding clientside-only mods, from the modpack
     * directory into the server pack directory. If a `source/file;destination/file`
     * -combination is provided, the specified source-file is copied to the specified
     * destination-file. One of the reasons as to why it is recommended to run a given
     * ConfigurationModel through the ConfigurationHandler first, is because the ConfigurationHandler
     * will resolve links to their actual files first before then correcting the given
     * ConfigurationModel.
     *
     * @param packConfig ConfigurationModel containing the modpack directory, list of
     * directories and files to copy, list of clientside-only mods to
     * exclude, the Minecraft version used by the modpack and server pack,
     * and the modloader used by the modpack and server pack.
     * @author Griefed
     */
    fun copyFiles(packConfig: Pack<*, *, *>) = copyFiles(
        packConfig.modpackDir,
        packConfig.copyDirs,
        packConfig.clientMods,
        packConfig.minecraftVersion,
        getServerPackDestination(packConfig),
        packConfig.modloader
    )

    /**
     * Download and provide the improved Fabric Server Launcher, if it is available for the given
     * Minecraft and Fabric version.
     *
     * @param packConfig ConfigurationModel containing the Minecraft and Fabric version for
     * which to acquire the improved Fabric Server Launcher.
     * @author Griefed
     */
    fun provideImprovedFabricServerLauncher(packConfig: Pack<*, *, *>) = provideImprovedFabricServerLauncher(
        packConfig.minecraftVersion, packConfig.modloaderVersion, getServerPackDestination(packConfig)
    )

    /**
     * Copies the server-icon.png into server pack. The sever-icon is automatically scaled to a
     * resolution of 64x64 pixels.
     *
     * @param packConfig Containing the modpack directory to acquire the destination of the
     * server pack and the path to the server icon to copy.
     * @author Griefed
     */
    fun copyIcon(packConfig: Pack<*, *, *>) = copyIcon(getServerPackDestination(packConfig), packConfig.serverIconPath)

    /**
     * Copies the server.properties into server pack.
     *
     * @param packConfig Containing the modpack directory to acquire the destination of the
     * server pack and the path to the server properties to copy.
     * @author Griefed
     */
    fun copyProperties(packConfig: Pack<*, *, *>) =
        copyProperties(getServerPackDestination(packConfig), packConfig.serverPropertiesPath)

    /**
     * Create start-scripts for the generated server pack using the templates the user has defined for
     * their instance of ServerPackCreator in the property `de.griefed.serverpackcreator.serverpack.script.template`.
     *
     * @param packConfig Configuration model containing modpack specific values. keys to be
     * replaced with their respective values in the start scripts, as well
     * as the modpack directory from which the destination of the server
     * pack is acquired.
     * @param isLocal            Whether the start scripts should be created for a locally usable
     * server pack. Use `false` if the start scripts should be created
     * for a server pack about to be zipped.
     * @author Griefed
     */
    fun createStartScripts(packConfig: Pack<*, *, *>, isLocal: Boolean) =
        createStartScripts(packConfig.scriptSettings, getServerPackDestination(packConfig), isLocal)

    /**
     * Creates a ZIP-archive of the server pack previously generated. Depending on the property
     * `de.griefed.serverpackcreator.serverpack.zip.exclude.enabled`, files will be excluded. To customize
     * the files which will be excluded, see the property `de.griefed.serverpackcreator.serverpack.zip.exclude`
     *
     * @param packConfig Contains the Minecraft version used by the modpack and server pack,
     * whether the modloader server was installed, the modpack directory to
     * acquire the destination of the server pack, the modloader used by the
     * modpack and server pack and the modloader version.
     * @author Griefed
     */
    fun zipBuilder(packConfig: Pack<*, *, *>) = zipBuilder(
        packConfig.minecraftVersion,
        packConfig.isServerInstallationDesired,
        getServerPackDestination(packConfig),
        packConfig.modloader,
        packConfig.modloaderVersion
    )

    /**
     * Installs the modloader server for the specified modloader, modloader version and Minecraft
     * version.
     *
     * @param packConfig Contains the used modloader, Minecraft version, modloader version,
     * path to the java executable/binary and modpack directory in order to
     * acquire the destination at which to install the server.
     * @author Griefed
     */
    fun installServer(packConfig: Pack<*, *, *>) = installServer(
        packConfig.modloader,
        packConfig.minecraftVersion,
        packConfig.modloaderVersion,
        getServerPackDestination(packConfig)
    )

    /**
     * Copies all specified directories and mods, excluding clientside-only mods, from the modpack
     * directory into the server pack directory. If a `source/file;destination/file`
     * -combination is provided, the specified source-file is copied to the specified
     * destination-file. One of the reasons as to why it is recommended to run a given
     * ConfigurationModel through the ConfigurationHandler first, is because the ConfigurationHandler
     * will resolve links to their actual files first before then correcting the given
     * ConfigurationModel.
     *
     * @param modpackDir        Files and directories are copied into the server_pack directory inside
     * the modpack directory.
     * @param directoriesToCopy All directories and files therein to copy to the server pack.
     * @param clientMods        List of clientside-only mods to exclude from the server pack.
     * @param minecraftVersion  The Minecraft version the modpack uses.
     * @param destination       The destination where the files should be copied to.
     * @param modloader         The modloader used for mod sideness detection.
     * @author Griefed
     */
    abstract fun copyFiles(
        modpackDir: String,
        directoriesToCopy: ArrayList<String>,
        clientMods: List<String>,
        minecraftVersion: String,
        destination: String,
        modloader: String
    )

    /**
     * Download and provide the improved Fabric Server Launcher, if it is available for the given
     * Minecraft and Fabric version.
     *
     * @param minecraftVersion The Minecraft version the modpack uses and the Fabric Server Launcher
     * should be downloaded for.
     * @param fabricVersion    The modloader version the modpack uses and the Fabric Server Launcher
     * should be downloaded for.
     * @param destination      The destination of the server pack.
     * @author Griefed
     */
    abstract fun provideImprovedFabricServerLauncher(
        minecraftVersion: String, fabricVersion: String, destination: String
    )

    /**
     * Copies the server-icon.png into server pack. The sever-icon is automatically scaled to a
     * resolution of 64x64 pixels.
     *
     * @param destination      The destination where the icon should be copied to.
     * @param pathToServerIcon The path to the custom server-icon.
     * @author Griefed
     */
    abstract fun copyIcon(destination: String, pathToServerIcon: String)

    /**
     * Copies the server.properties into server pack.
     *
     * @param destination            The destination where the properties should be copied to.
     * @param pathToServerProperties The path to the custom server.properties.
     * @author Griefed
     */
    abstract fun copyProperties(destination: String, pathToServerProperties: String)

    /**
     * Create start-scripts for the generated server pack using the templates the user has defined for
     * their instance of ServerPackCreator in the property `de.griefed.serverpackcreator.serverpack.script.template`.
     *
     * @param scriptSettings Key-value pairs to replace in the script. A given key in the script is
     * replaced with its value.
     * @param destination    The destination where the scripts should be created in.
     * @param isLocal        Whether the start scripts should be created for a locally usable server
     * pack. Use `false` if the start scripts should be created for a
     * server pack about to be zipped.
     * @author Griefed
     */
    abstract fun createStartScripts(
        scriptSettings: HashMap<String, String>, destination: String, isLocal: Boolean
    )

    /**
     * Creates a ZIP-archive of specified directory. Depending on the property `de.griefed.serverpackcreator.serverpack.zip.exclude.enabled`,
     * files will be excluded. To customize the files which will be excluded, the property `de.griefed.serverpackcreator.serverpack.zip.exclude`
     * must be configured accordingly. The created ZIP-archive will be stored alongside the specified
     * destination, with `_server_pack.zip` appended to its name.
     *
     * @param minecraftVersion          Determines the name of the Minecraft server JAR to exclude
     * from the ZIP-archive if the modloader is Forge.
     * @param includeServerInstallation Determines whether the Minecraft server JAR info should be
     * printed.
     * @param destination               The destination where the ZIP-archive should be created in.
     * @param modloader                 The modloader the modpack and server pack use.
     * @param modloaderVersion          The modloader version the modpack and server pack use.
     * @author Griefed
     */
    abstract fun zipBuilder(
        minecraftVersion: String,
        includeServerInstallation: Boolean,
        destination: String,
        modloader: String,
        modloaderVersion: String
    )

    /**
     * Installs the modloader server for the specified modloader, modloader version and Minecraft
     * version.
     *
     * @param modLoader        The modloader for which to install the server software. Either Forge or
     * Fabric.
     * @param minecraftVersion The Minecraft version for which to install the modloader and Minecraft
     * server.
     * @param modLoaderVersion The modloader version for which to install the modloader and Minecraft
     * server.
     * @param destination      The destination where the modloader server should be installed into.
     * @author Griefed
     */
    abstract fun installServer(
        modLoader: String, minecraftVersion: String, modLoaderVersion: String, destination: String
    )

    /**
     * Generates a list of all mods to include in the server pack. If the user specified
     * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
     * active, they will be excluded, too.
     *
     * @param modsDir                 The mods-directory of the modpack of which to generate a list of
     * all its contents.
     * @param userSpecifiedClientMods A list of all clientside-only mods.
     * @param minecraftVersion        The Minecraft version the modpack uses. When the modloader is
     * Forge, this determines whether Annotations or Tomls are
     * scanned.
     * @param modloader               The modloader the modpack uses.
     * @return A list of all mods to include in the server pack.
     * @author Griefed
     */
    abstract fun getModsToInclude(
        modsDir: String, userSpecifiedClientMods: List<String>, minecraftVersion: String, modloader: String
    ): List<F>

    /**
     * Check whether the installer for the given combination of Minecraft version, modloader and
     * modloader version is available/reachable.
     *
     * @param mcVersion        The Minecraft version.
     * @param modloader        The modloader.
     * @param modloaderVersion The modloader version.
     * @return `true` if the installer can be downloaded.
     * @author Griefed
     */
    abstract fun serverDownloadable(
        mcVersion: String, modloader: String, modloaderVersion: String
    ): Boolean

    /**
     * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure
     * newly generated server pack is as clean as possible. This will completely empty the server pack
     * directory, so use with caution!
     *
     * @param deleteZip          Whether to delete the server pack ZIP-archive.
     * @param packConfig ConfigurationModel containing the modpack directory from which the
     * destination of the server pack is acquired.
     * @author Griefed
     */
    fun cleanupEnvironment(deleteZip: Boolean, packConfig: Pack<*, *, *>) =
        cleanupEnvironment(deleteZip, getServerPackDestination(packConfig))

    /**
     * Generates a list of all mods to include in the server pack. If the user specified
     * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
     * active, they will be excluded, too.
     *
     * @param packConfig The configurationModel containing the modpack directory, list of
     * clientside-only mods to exclude, Minecraft version used by the
     * modpack and server pack and the modloader used by the modpack and
     * server pack.
     * @return A list of all mods to include in the server pack.
     * @author Griefed
     */
    fun getModsToInclude(packConfig: Pack<*, *, *>) = getModsToInclude(
        "${packConfig.modpackDir}${ApiProperties.getSeparator()}mods",
        packConfig.clientMods,
        packConfig.minecraftVersion,
        packConfig.modloader
    )

    /**
     * Gather a list of all files from an explicit source;destination-combination. If the source is a
     * file, a singular [ServerPackFile] is returned. If the source is a directory, then all
     * files in said directory are returned.
     *
     * @param combination Array containing a source-file/directory;destination-file/directory
     * combination.
     * @param modpackDir  The modpack-directory.
     * @param destination The destination, normally the server pack-directory.
     * @return List of [ServerPackFile].
     * @author Griefed
     */
    abstract fun getExplicitFiles(
        combination: Array<String>, modpackDir: String, destination: String
    ): MutableList<ServerPackFile>

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
    abstract fun getSaveFiles(
        clientDir: String, directory: String, destination: String
    ): List<ServerPackFile>

    /**
     * Acquire all files and directories for the given regex in a list of [ServerPackFile] for a
     * given regex-entry from the configuration models copyDirs.
     *
     * @param modpackDir  The path to the modpack directory in which to check for existence of the
     * passed list of directories.
     * @param destination The destination where the files should be copied to.
     * @param entry       The regex, or file/directory and regex, combination.
     * @return List of [ServerPackFile] which will be included in the server pack.
     * @author Griefed
     */
    abstract fun getRegexMatches(
        modpackDir: String, destination: String, entry: String
    ): List<ServerPackFile>

    /**
     * Recursively acquire all files and directories inside the given directory as a list of
     * [ServerPackFile].
     *
     * @param source      The source-directory.
     * @param destination The server pack-directory.
     * @return List of files and folders of the server pack.
     * @author Griefed
     */
    abstract fun getDirectoryFiles(
        source: String, destination: String
    ): List<ServerPackFile>

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
    abstract fun excludeFileOrDirectory(modpackDir: String, fileToCheckFor: F, exclusions: TS): Boolean

    /**
     * Cleans up the server_pack directory by deleting left-over files from modloader installations
     * and version checking.
     *
     * @param destination      The destination where we should clean up in.
     * @author Griefed
     */
    abstract fun postInstallCleanup(destination: String)

    /**
     * Exclude every automatically discovered clientside-only mod from the list of mods in the
     * modpack.
     *
     * @param autodiscoveredClientMods Automatically discovered clientside-only mods in the modpack.
     * @param modsInModpack            All mods in the modpack.
     * @author Griefed
     */
    abstract fun excludeMods(autodiscoveredClientMods: List<F>, modsInModpack: TF)

    /**
     * Exclude user-specified mods from the server pack.
     *
     * @param userSpecifiedExclusions User-specified clientside-only mods to exclude from the server
     * pack.
     * @param modsInModpack           Every mod ending with `jar` or `disabled` in the
     * modpack.
     * @author Griefed
     */
    abstract fun excludeUserSpecifiedMod(userSpecifiedExclusions: List<String>, modsInModpack: TF)

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
    abstract fun regexWalk(source: F, destination: String, regex: Regex, serverPackFiles: MutableList<ServerPackFile>)

    /**
     * Go through the mods in the modpack and exclude any of the user-specified clientside-only mods
     * according to the filter method set in the serverpackcreator.properties. For available filters,
     * see [ExclusionFilter].
     *
     * @param userSpecifiedExclusion The client mod to check whether it needs to be excluded.
     * @param modsInModpack          All mods in the modpack.
     *
     * @author Griefed
     */
    abstract fun exclude(userSpecifiedExclusion: String, modsInModpack: TF)

    /**
     * Gather a list of all files from an explicit source;destination-combination. If the source is a
     * file, a singular [ServerPackFile] is returned. If the source is a directory, then all
     * files in said directory are returned.
     *
     * @param combination        Array containing a source-file/directory;destination-file/directory
     * combination.
     * @param packConfig ConfigurationModel containing the modpack directory so the
     * destination of the server pack can be acquired.
     * @return List of [ServerPackFile].
     * @author Griefed
     */
    fun getExplicitFiles(combination: Array<String>, packConfig: Pack<*, *, *>) =
        getExplicitFiles(combination, packConfig.modpackDir, getServerPackDestination(packConfig))

    /**
     * Cleans up the server_pack directory by deleting left-over files from modloader installations
     * and version checking.
     *
     * @param packConfig Containing the Minecraft version used by the modpack and server pack,
     * the modloader version used by the modpack and server pack and the
     * modpack directory to acquire the destination of the server pack.
     * @author Griefed
     */
    fun cleanUpServerPack(packConfig: Pack<*, *, *>) = postInstallCleanup(getServerPackDestination(packConfig))

    /**
     * Delete files and folders from previous installations to prevent errors during server installation due to already
     * existing files.
     * @param destination The folder in which to perform the cleanup operations.
     *
     * @author Griefed
     */
    abstract fun preInstallationCleanup(destination: String)

    /**
     * Replace placeholders for script settings in the given [content] with their respective values, both provided via the
     * HashMap [scriptSettings].
     *
     * @param isLocal Whether the start scripts should be created for a locally usable server pack. Use false if the
     * start scripts should be created for a server pack about to be zipped
     *
     * @author Griefed
     */
    fun replacePlaceholders(isLocal: Boolean, content: String, scriptSettings: HashMap<String, String>): String {
        var result = content
        for ((key, value) in scriptSettings) {
            result = if (isLocal && key == "SPC_JAVA_SPC") {
                result.replace(key, value.escapePath())
            } else if (!isLocal && key == "SPC_JAVA_SPC") {
                result.replace(key, "java")
            } else {
                result.replace(key, value)
            }
        }
        return result
    }
}