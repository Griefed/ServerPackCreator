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

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import de.griefed.serverpackcreator.api.utilities.common.escapePath
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.util.*
import javax.imageio.ImageIO

/**
 * Provisioner for everything a server pack needs besides the modpack-files themselves: the
 * server-icon, server.properties, start-scripts with variables.txt and HOW-TO-RUN.md, the
 * ZIP-archive, the improved Fabric-launcher, installer-availability checks and pre-/post-install
 * cleanups. Extracted from ServerPackHandler (refactor Phase 1d); ServerPackHandler remains the
 * facade through which consumers access these operations.
 */
class ServerPackProvisioner(
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta,
    private val utilities: Utilities
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    //TODO move to template file, just like the scripts.
    val variables = """ 
        ###
        # REMEMBER:
        #   Escape \ and : in your Java path on Windows with another \
        #   Example:
        #     From: C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\java.exe
        #     To:   C\:\\Program Files\\Eclipse Adoptium\\jdk-17.0.9.9-hotspot\\bin\\java.exe
        #   More on escape characters at https://en.wikipedia.org/wiki/Escape_character
        #
        # WAIT_FOR_USER_INPUT true/false allows you to enable/disable user confirmation upon
        #   graceful script ending.
        # RESTART true/false allows you to enable/disable automatically restarting the server
        #   should it crash.
        # JAVA points towards the Java executable/binary the server should use for running. Default is `java`, so it
        #   points towards the system-default, if you have one. Set this to an absolute path, as per the example above
        #   in the "REMEMBER"-part, if you want to force the server to use a different Java installation/version.
        #   When setting a custom path, set SKIP_JAVA_CHECK to true.
        # JAVA_ARGS are arguments to pass to the JVM / your server. Typical args are 'Xmx4G Xms4g'. Arguments in this
        #   variable are also written to the 'user_jvm_args.txt' when using modloaders such as Forge.
        #   More information at https://minecraft.fandom.com/wiki/Tutorials/Setting_up_a_server
        #   I recommend you read this page at least once.
        # ADDITIONAL_ARGS are, as the name implies, additional arguments to pass to the server. These arguments are not
        #   written to any file, they are directly used in the command to run the server.
        # SKIP_JAVA_CHECK true/false allows you to disable/enable the compatibility check
        #   of your Minecraft version and the provided Java version, as well as the automatic
        #   installation of a compatible Java version, should JAVA be set to 'java'.
        # JDK_VENDOR is for the automatic installation of a JDK compatible with the Minecraft
        #   version of your server pack. For an extensive list of available vendors, check out
        #   https://github.com/Jabba-Team/index/blob/main/index.json
        #   Note - For the installation to take place:
        #   - SKIP_JAVA_CHECK must be set to 'false'
        #   - JAVA be set to 'java'
        #   - No 'java' command be available OR
        #   - The available Java version behind 'java' be incompatible with your Minecraft version.
        # JABBA_INSTALL_VERSION has no effect on the installation of Jabba when using PowerShell.
        # MINECRAFT_VERSION is tightly coupled with the modloader version. Be careful when changing this, as the new
        #   new version you set may not be compatible with the modloader and modloader version combination.
        # MODLOADER and MODLOADER_VERSION same thing as with MINECRAFT_VERSION. Changing any of these three values may
        #   have unforseen consequences. Well, I say unforseen, it mostly causes the server to straight up not start,
        #   because of incompatibilities. Be very careful when changing these!
        # SERVERSTARTERJAR_FORCE_FETCH true/false allows you to enable/disable the force-refreshing of the server.jar
        #   when using Forge or NeoForge as your modloader. Force-refreshing means the file is replaced with a freshly
        #   downloaded one every time you run the start scripts.
        # SERVERSTARTERJAR_VERSION allows you to manually set the version of the server.jar downloaded by the scripts.
        #   If you want to always use the latest version, set this to exactly "latest". For a specific version, see
        #   https://github.com/neoforged/ServerStarterJar/releases and use the tags on the left as the version,
        #   e.g. 0.1.24 or 0.1.25. When setting a specific version, make sure the release you pick actually has a server.jar
        #   available for download. When the download fails with the "latest"-setting, then pick a specific one and/or
        #   contact the devs of the ServerStarterJar about the latest release not having a server.jar to download.
        # USE_SSJ true/false allows you to enable/disable the usage of the ServerStarterJar by the NeoForge project when you are
        #   using Forge. Some Forge versions may be incompatible with said ServerStarterJar. As of right now, people
        #   ran into trouble when using Forge and Minecraft 1.20.2 and 1.20.3.
        # SSJ_FORGE_ARGS are additional arguments to use when using the Server Starter Jar from the NeoForge project in
        #   combination with the Forge-modloader. Some java versions require explicit allowing of the security manager,
        #   for example.
        # CLEANUP is a list of comma-separated files which get deleted permanently upon calling the start-script with either
        #   the --cleanup argument, or when the script detected a previous run with differing versions/modloaders.
        #   Edit with care!
        #   Edit at your own risk!
        #   Editing might lead to unwanted data corruption or deletion!
        #
        # DO NOT EDIT THE FOLLOWING VARIABLES MANUALLY
        #   - FABRIC_INSTALLER_VERSION
        #   - QUILT_INSTALLER_VERSION
        #   - LEGACYFABRIC_INSTALLER_VERSION
        #
        # Variables are not reloaded between automatic restarts. If you've made changes to your
        #   variables and you want them to take effect, stop the server and script, then
        #   re-run it.
        ###
        MINECRAFT_VERSION=SPC_MINECRAFT_VERSION_SPC
        MODLOADER=SPC_MODLOADER_SPC
        MODLOADER_VERSION=SPC_MODLOADER_VERSION_SPC
        LEGACYFABRIC_INSTALLER_VERSION=SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC
        FABRIC_INSTALLER_VERSION=SPC_FABRIC_INSTALLER_VERSION_SPC
        QUILT_INSTALLER_VERSION=SPC_QUILT_INSTALLER_VERSION_SPC
        RECOMMENDED_JAVA_VERSION=SPC_RECOMMENDED_JAVA_VERSION_SPC
        WAIT_FOR_USER_INPUT=SPC_WAIT_FOR_USER_INPUT_SPC
        JAVA="SPC_JAVA_SPC"
        JAVA_ARGS="SPC_JAVA_ARGS_SPC"
        ADDITIONAL_ARGS="SPC_ADDITIONAL_ARGS_SPC"
        SSJ_FORGE_ARGS="SPC_SSJ_FORGE_ARGS_SPC"
        RESTART=SPC_RESTART_SPC
        SKIP_JAVA_CHECK=SPC_SKIP_JAVA_CHECK_SPC
        JDK_VENDOR=SPC_JDK_VENDOR_SPC
        JABBA_INSTALL_URL_SH=SPC_JABBA_INSTALL_URL_SH_SPC
        JABBA_INSTALL_URL_PS=SPC_JABBA_INSTALL_URL_PS_SPC
        JABBA_INSTALL_VERSION=SPC_JABBA_INSTALL_VERSION_SPC
        SERVERSTARTERJAR_FORCE_FETCH=SPC_SERVERSTARTERJAR_FORCE_FETCH_SPC
        SERVERSTARTERJAR_VERSION=SPC_SERVERSTARTERJAR_VERSION_SPC
        USE_SSJ=SPC_USE_SSJ_SPC
        CLEANUP="SPC_CLEANUP_SPC"
    """.trimIndent()
    private val howToStartTheServer = """
        # How To Start / Run The Server
        
        If your `variables.txt` has `JAVA=java` set, then a suitable Java version for your Minecraft server will
        be installed automatically.
        
        Forge and NeoForge 1.17 and up will create run.xx-scripts due to the ServerStarterJar being used to install
        and run the server. It is safe to ignore these and continue using the start.xx-scripts.
        Deleting the run.xx-scripts will result in the server being installed again by the ServerStarterJar. More about
        the ServerStarterJar at https://github.com/neoforged/ServerStarterJar
        
        ## Linux
        
        Run `.\start.sh` or `bash start.sh` to start the server.
        
        ## Windows
        
        Run `start.bat`.
        Do **not** delete the PowerShell (ps1) files!
        
        ### Convenience
        
        You may run `start.ps1` from a console-window manually, but using the Batch-script is recommended.
        Running PowerShell-scripts requires changing the ExecutionPolicy of your Windows-system. The Batch-script
        can bypass this for the start-script.
        
        TL;DR: start.bat better than start.ps1
        
        ## MacOS
        
        Run `.\start.sh` or `bash start.sh` to start the server.
        
        # Issues with this server pack
        
        If you downloaded this server pack from the internet and you run into issues with this server pack, then please
        contact the creators of the server pack about your issue(s).
        
        If you've created this server pack yourself and you run into issues, feel free to contact the developers of
        ServerPackCreator for support.
    """.trimIndent()


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
    fun getImprovedFabricLauncher(minecraftVersion: String, fabricVersion: String, destination: String) {
        val fileDestination = File(destination, "fabric-server-launcher.jar")
        if (versionMeta.fabric.launcherFor(minecraftVersion, fabricVersion).isPresent) {
            versionMeta.fabric.launcherFor(minecraftVersion, fabricVersion).get().copyTo(fileDestination)
            log.info("Successfully provided improved Fabric Server Launcher.")
            val text = """
                |If you are using this server pack on a managed server, meaning you can not execute scripts, please use the fabric-server-launcher.jar instead of the fabric-server-launch.jar. Note the extra "er" at the end of "launcher".
                |This is the improved Fabric Server Launcher, which will take care of downloading and installing the Minecraft server and any and all libraries needed for running the Fabric server.
                |
                |The downside of this method is the occasional incompatibility of mods with the Fabric version, as the new Fabric Server Launcher always uses the latest available Fabric version.
                |If a mod is incompatible with said latest Fabric version, contact the mod-author and ask them to remedy the situation.
                |The official Fabric Discord had the following to add to this:
                |    Fabric loader however is cross version, so unless there is a mod incompatibility (which usually involves the mod being broken / using non-api internals)
                |    there is no good reason to use anything but the latest. I.e. the latest loader on any Minecraft version works with the new server launcher.
            """.trimMargin()
            File(destination, "SERVER_PACK_INFO.txt").writeText(text)
        }
    }


    /**
     * Copies the server-icon.png into server pack. The sever-icon is automatically scaled to a
     * resolution of 64x64 pixels.
     *
     * @param destination      The destination where the icon should be copied to.
     * @param pathToServerIcon The path to the custom server-icon.
     * @author Griefed
     */
    fun copyIcon(destination: String, pathToServerIcon: String) {
        log.info("Copying server-icon.png...")
        val customIcon = File(destination, apiProperties.defaultServerIcon.name)
        if (File(pathToServerIcon).exists()) {
            try {
                val originalImage: BufferedImage = ImageIO.read(File(pathToServerIcon))
                if (originalImage.height == 64 && originalImage.width == 64) {
                    try {
                        File(pathToServerIcon).copyTo(customIcon, true)
                    } catch (e: IOException) {
                        log.error("An error occurred trying to copy the server-icon.", e)
                    }
                } else {
                    val scaledImage: Image = originalImage.getScaledInstance(64, 64, Image.SCALE_SMOOTH)
                    val outputImage = BufferedImage(
                        scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_INT_ARGB
                    )
                    outputImage.graphics.drawImage(scaledImage, 0, 0, null)
                    try {
                        ImageIO.write(outputImage, "png", customIcon)
                    } catch (ex: IOException) {
                        log.error("Error scaling image.", ex)
                    }
                }
            } catch (ex: Exception) {
                log.error("Error reading server-icon image.", ex)
            }
        } else if (pathToServerIcon.isEmpty()) {
            log.info("No custom icon specified or the file doesn't exist.")
            apiProperties.defaultServerIcon.copyTo(customIcon, true)
        } else {
            log.error("The specified server-icon does not exist: $pathToServerIcon")
        }
    }


    /**
     * Copies the server.properties into server pack.
     *
     * @param destination            The destination where the properties should be copied to.
     * @param pathToServerProperties The path to the custom server.properties.
     * @author Griefed
     */
    fun copyProperties(destination: String, pathToServerProperties: String) {
        log.info("Copying server.properties...")
        val customProperties = File(destination, apiProperties.defaultServerProperties.name)
        if (File(pathToServerProperties).exists()) {
            File(pathToServerProperties).copyTo(customProperties, true)
        } else if (pathToServerProperties.isEmpty()) {
            log.info("No custom properties specified or the file doesn't exist.")
            apiProperties.defaultServerProperties.copyTo(customProperties, true)
        } else {
            log.error("The specified server.properties does not exist: $pathToServerProperties")
        }
    }


    /**
     * Create start-scripts for the generated server pack using the templates the user has defined for
     * their instance of ServerPackCreator.
     *
     * @param scriptSettings Key-value pairs to replace in the script. A given key in the script is
     * replaced with its value.
     * @param destination    The destination where the scripts should be created in.
     * @param isLocal        Whether the start scripts should be created for a locally usable server
     * pack. Use `false` if the start scripts should be created for a
     * server pack about to be zipped.
     * @author Griefed
     */
    fun createServerRunFiles(scriptSettings: HashMap<String, String>, destination: String, isLocal: Boolean) {
        var script: File
        var content: String
        val scripts = mutableListOf<File>()
        for ((key, value) in apiProperties.startScriptTemplates) {
            try {
                script = File(destination, "start.$key")
                content = replacePlaceholders(isLocal, File(value).readText(), scriptSettings).replace("\r", "")
                if (script.exists()) {
                    script.setWritable(true)
                }
                script.writeText(content)
                scripts.add(script)
            } catch (ex: Exception) {
                log.error("$key-File not accessible: $value.", ex)
            }
        }

        for ((key, value) in apiProperties.javaScriptTemplates) {
            try {
                script = File(destination, "install_java.$key")
                content = replacePlaceholders(isLocal, File(value).readText(), scriptSettings).replace("\r", "")
                if (script.exists()) {
                    script.setWritable(true)
                }
                script.writeText(content)
                scripts.add(script)
            } catch (ex: Exception) {
                log.error("$key-File not accessible: $value.", ex)
            }
        }
        for (scriptFile in scripts) {
            scriptFile.setExecutable(true)
            scriptFile.setReadable(true)
            scriptFile.setWritable(false)
        }

        try {
            val destinationVariables = File(destination, "variables.txt")
            var variablesContent = variables
            variablesContent = replacePlaceholders(isLocal, variablesContent, scriptSettings)
            for ((key, value) in scriptSettings) {
                if (key.startsWith("CUSTOM_") && key.endsWith("_CUSTOM")) {
                    val varKey = key.replace("CUSTOM_","").replace("_CUSTOM","")
                    variablesContent += "\n$varKey=$value"
                }
            }
            destinationVariables.writeText(variablesContent.replace("\r", ""))
            destinationVariables.setReadable(true)
            destinationVariables.setWritable(true)
            destinationVariables.setExecutable(false)
        } catch (ex: Exception) {
            log.error("File not accessible: ${File(destination, "variables.txt")}.", ex)
        }

        try {
            val howToStartTheScriptReadme = File(destination, "HOW-TO-RUN.md")
            if (howToStartTheScriptReadme.exists()) {
                howToStartTheScriptReadme.setWritable(true)
            }
            howToStartTheScriptReadme.writeText(howToStartTheServer.replace("\r", ""))
            howToStartTheScriptReadme.setExecutable(false)
            howToStartTheScriptReadme.setReadable(true)
            howToStartTheScriptReadme.setWritable(false)
        } catch (ex: Exception) {
            log.error("File not accessible: ${File(destination, "HOW-TO-RUN.md")}.", ex)
        }
    }


    /**
     * Creates a ZIP-archive of specified directory. Depending on the property `de.griefed.serverpackcreator.serverpack.zip.exclude.enabled`,
     * files will be excluded. To customize the files which will be excluded, the property `de.griefed.serverpackcreator.serverpack.zip.exclude`
     * must be configured accordingly. The created ZIP-archive will be stored alongside the specified
     * destination, with `_server_pack.zip` appended to its name.
     *
     * @param minecraftVersion          Determines the name of the Minecraft server JAR to exclude
     * from the ZIP-archive if the modloader is Forge.
     * @param destination               The destination where the ZIP-archive should be created in.
     * @param modloader                 The modloader the modpack and server pack use.
     * @param modloaderVersion          The modloader version the modpack and server pack use.
     * @author Griefed
     */
    fun zipBuilder(
        minecraftVersion: String,
        destination: String,
        modloader: String,
        modloaderVersion: String
    ) : Optional<File> {
        log.info("Creating zip archive of serverpack...")
        val zipParameters = ZipParameters()
        var zip: ZipFile? = null
        val filesToExclude: MutableList<File> = ArrayList(100)
        if (apiProperties.isZipFileExclusionEnabled) {
            for (entry in apiProperties.zipArchiveExclusions) {
                filesToExclude.add(
                    File(
                        destination,
                        entry.replace("MINECRAFT_VERSION", minecraftVersion).replace("MODLOADER", modloader)
                            .replace("MODLOADER_VERSION", modloaderVersion)
                    )
                )
            }
            val excludeFileFilter = ExcludeFileFilter { o: File -> filesToExclude.contains(o) }
            zipParameters.excludeFileFilter = excludeFileFilter
        } else {
            log.info("File exclusion from ZIP-archives deactivated.")
        }
        val comment = ("Server pack made with ServerPackCreator ${apiProperties.apiVersion} by Griefed.")
        zipParameters.isIncludeRootFolder = false
        zipParameters.fileComment = comment
        try {
            zip = ZipFile("${destination}_server_pack.zip")
            zip.use {
                it.addFolder(File(destination), zipParameters)
                it.comment = comment
            }
        } catch (ex: IOException) {
            log.error("There was an error during zip creation.", ex)
        }
        log.info("Finished creation of zip archive.")
        return Optional.ofNullable(zip?.file)
    }


    /**
     * Delete files and folders from previous installations to prevent errors during server installation due to already
     * existing files.
     * @param destination The folder in which to perform the cleanup operations.
     *
     * @author Griefed
     */
    @Suppress("unused")
    fun preInstallationCleanup(destination: String) {
        log.info("Pre server installation cleanup.")
        var fileToDelete: File
        for (file in apiProperties.preInstallCleanupFiles) {
            fileToDelete = File(destination,file)
            if (fileToDelete.deleteQuietly()) {
                log.info("Deleted $fileToDelete")
            }
        }
    }


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
    fun serverDownloadable(mcVersion: String, modloader: String, modloaderVersion: String) = when (modloader) {
        "Fabric" -> utilities.webUtilities.isReachable(versionMeta.fabric.releaseInstallerUrl())

        "Forge" -> {
            val instance = versionMeta.forge.getForgeInstance(mcVersion, modloaderVersion)
            instance.isPresent && utilities.webUtilities.isReachable(instance.get().installerUrl)
        }

        "Quilt" -> utilities.webUtilities.isReachable(versionMeta.quilt.releaseInstallerUrl())

        "LegacyFabric" -> {
            try {
                utilities.webUtilities.isReachable(versionMeta.legacyFabric.releaseInstallerUrl())
            } catch (_: MalformedURLException) {
                // No valid release-installer URL could be built -> treat the installer as
                // not downloadable.
                false
            }
        }

        "NeoForge" -> {
            val instance = versionMeta.neoForge.getNeoForgeInstance(mcVersion,modloaderVersion)
            instance.isPresent && utilities.webUtilities.isReachable(instance.get().installerUrl)
        }

        else -> false
    }


    /**
     * Cleans up the server_pack directory by deleting left-over files from modloader installations
     * and version checking.
     *
     * @param destination      The destination where we should clean up in.
     * @author Griefed
     */
    @Suppress("unused")
    fun postInstallCleanup(destination: String) {
        log.info("Cleanup after modloader server installation.")
        var fileToDelete: File
        for (file in apiProperties.postInstallCleanupFiles) {
            fileToDelete = File(destination, file)
            if (fileToDelete.deleteQuietly()) {
                log.info("  Deleted $fileToDelete")
            }
        }
    }


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
            } else if (!isLocal && key == "SPC_RESTART_SPC") {
                result.replace(key, "true")
            } else {
                result.replace(key, value)
            }
        }
        return result
    }
}
