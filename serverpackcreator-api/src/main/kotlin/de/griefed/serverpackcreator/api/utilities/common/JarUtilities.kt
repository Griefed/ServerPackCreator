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
package de.griefed.serverpackcreator.api.utilities.common

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.io.path.toPath

/**
 * Some utilities used across ServerPackCreator, revolving around interacting with JAR-files.
 *
 * @author Griefed
 */
@Suppress("unused")
class JarUtilities {
    companion object {
        private val log by lazy { cachedLoggerOf(JarUtilities::class.java) }

        /**
         * Copy a file from inside our JAR-file to the host filesystem. The file will create exactly as specified in the
         * parameter.
         *
         * Example:
         * * `copyFileFromJar(File("log4j2.xml"))` will result in the log4j2.xml file from inside the JAR-file to be
         * copied to the outside of the JAR-file as `log4j2.xml`
         *
         * @param fileToCopy      The source-file in the JAR you wish to copy outside the JAR.
         * @param replaceIfExists Whether to replace the file, if it already exists.
         * @param classToCopyFrom The class of the JAR from which you want to copy from.
         * @param directory       The directory where the file should be copied to.
         * @author Griefed
         */
        fun copyFileFromJar(
            fileToCopy: String,
            replaceIfExists: Boolean,
            classToCopyFrom: Class<*>,
            directory: String
        ) {
            if (replaceIfExists) {
                File(directory, fileToCopy).deleteQuietly()
            }
            copyFileFromJar(fileToCopy, classToCopyFrom, directory)
        }

        /**
         * Copy a file from inside our JAR-file to the host filesystem. The file will be created with
         * exactly the same path specified in the parameter.
         *
         * Example:
         * * `copyFileFromJar(File("log4j2.xml"))` will result in the log4j2.xml file from inside the JAR-file to be
         * copied to the outside of the JAR-file as `log4j2.xml`
         *
         * @param fileToCopy      The source-file in the JAR you wish to copy outside the JAR.
         * @param identifierClass The class of the JAR from which to get the resource.
         * @param directory       The directory to copy the file to.
         * @return `true` if the file was created, `false` otherwise.
         * @author Griefed
         */
        fun copyFileFromJar(fileToCopy: String, identifierClass: Class<*>, directory: String) =
            if (!File(directory, fileToCopy).exists()) {
                try {
                    val file = File(directory, fileToCopy).absoluteFile
                    identifierClass.getResourceAsStream("/$fileToCopy").use {
                        file.outputStream().use { out -> it?.transferTo(out) }
                    }
                    if (file.exists()) {
                        true
                    } else {
                        throw JarAccessException("$file was not created!")
                    }
                } catch (ex: IOException) {
                    log.error("Error creating file: $fileToCopy", ex)
                    false
                }
            } else {
                log.info("File $fileToCopy already exists.")
                false
            }

        /**
         * Copy a file from inside our JAR-file to the host filesystem to the specified destination,
         * replacing an already existing file. The file will be created with exactly the same path
         * specified in the parameter.
         *
         * Example:
         * * `copyFileFromJar(File("log4j2.xml"))` will result in the log4j2.xml file from inside the JAR-file to be copied
         * to the outside of the JAR-file as `log4j2.xml`
         *
         * @param fileToCopy      The source-file in the JAR you wish to copy outside the JAR.
         * @param destinationFile The file to which to copy to.
         * @param identifierClass The class of the JAR from which to get the resource.
         * @param replaceIfExists Whether to replace the file, if it already exists.
         * @author Griefed
         */
        fun copyFileFromJar(
            fileToCopy: String,
            destinationFile: File,
            replaceIfExists: Boolean,
            identifierClass: Class<*>
        ) {
            if (replaceIfExists) {
                destinationFile.deleteQuietly()
            }
            copyFileFromJar(fileToCopy, destinationFile, identifierClass)
        }

        /**
         * Copy a file from inside our JAR-file to the host filesystem to the specified destination. The
         * file will be created with exactly the same path specified in the parameter.
         *
         * Example:
         * * `copyFileFromJar(File("log4j2.xml"))` will result in the log4j2.xml file from inside the JAR-file to be copied
         * to the outside of the JAR-file as `log4j2.xml`
         *
         * @param fileToCopy      The source-file in the JAR you wish to copy outside the JAR.
         * @param identifierClass The class of the JAR from which to get the resource.
         * @param destinationFile The file to which to copy to.
         * @return `true` if the file was created, `false` otherwise.
         * @author Griefed
         */
        fun copyFileFromJar(fileToCopy: String, destinationFile: File, identifierClass: Class<*>) =
            if (!destinationFile.absoluteFile.exists()) {
                destinationFile.create()
                try {
                    identifierClass.getResourceAsStream("/$fileToCopy").use {
                        destinationFile.absoluteFile.outputStream().use { out -> it?.transferTo(out) }
                    }
                    if (destinationFile.absoluteFile.exists()) {
                        true
                    } else {
                        throw JarAccessException("$destinationFile was not created!")
                    }
                } catch (ex: IOException) {
                    log.error("Error creating file: $destinationFile", ex)
                    false
                }
            } else {
                log.info("File $destinationFile already exists.")
                false
            }

        /**
         * Copy a folder from inside a JAR-file to the host filesystem. The specified folder will be
         * copied, along with all resources inside it, recursively, to the specified destination.<br>
         *
         * @param classToRetrieveHomeFor Path to either the JAR-file from which to copy a folder from, or
         *                               to the class when running in a dev-environment. This parameter
         *                               decides whether
         *                               {@link #copyFolderFromJar(Class, String, String, String)} or
         *                               {@link #copyFolderFromJar(JarFile, String, String, String,
         *                               String)} is called.<br> Example for JAR-file:
         *                               {@code /home/griefed/serverpackcreator/serverpackcreator.jar}
         *                               <br>
         *                               Example for dev-environment:
         *                               {@code G:/GitLab/ServerPackCreator/build/classes/java/main }<br>
         *                               See {@link ServerPackCreator#main(String[])} source code for an
         *                               example on how this is acquired automatically.
         * @param directoryToCopy        Path to the directory inside the JAR-file you want to copy.
         * @param destinationDirectory   Path to the destination directory you want to copy source to.
         * @param jarDirectoryPrefix     A prefix to remove when checking for existence of source inside
         *                               the JAR-file. For example, the ServerPackCreator files inside
         *                               it's JAR-File are located in {@code BOOT-INF/classes} due to
         *                               SpringBoot. In order to correctly scan for source, we need to
         *                               remove that prefix, so we receive a path that looks like a
         *                               regular path inside a JAR-file.
         * @param fileEnding             The file-ending to filter for.
         * @throws IOException Exception thrown if a file can not be accessed, found or otherwise worked
         *                     with.
         * @author Griefed
         */
        @Throws(IOException::class)
        fun copyFolderFromJar(
            classToRetrieveHomeFor: Class<*>,
            directoryToCopy: String,
            destinationDirectory: String,
            jarDirectoryPrefix: String,
            fileEnding: Regex,
            tempDir: File = File(System.getProperty("user.home"),".spc")
        ) {
            if (!tempDir.exists() || !tempDir.isDirectory) {
                tempDir.create(createFileOrDir = true, asDirectory = true)
            }
            val systemInformation: HashMap<String, String> = jarInformation(classToRetrieveHomeFor)
            val source = systemInformation["jarPath"]?.let { File(it) }
            if (source != null) {
                if (!source.isFile && source.isDirectory) {
                    // Dev environment
                    copyFolderFromDevEnv(
                        classToRetrieveHomeFor,
                        directoryToCopy,
                        destinationDirectory,
                        fileEnding
                    )
                } else {
                    JarFile(classToRetrieveHomeFor.source(tempDir)).use {
                        // Production
                        copyFolderFromJar(
                            it,
                            directoryToCopy,
                            destinationDirectory,
                            jarDirectoryPrefix,
                            fileEnding
                        )
                    }
                }
            }
        }

        /**
         * Copy a folder from inside our dev-resources to the host filesystem, using a source and a
         * destination.<br> This method is used in case we are running in a dev environment, where files
         * are not wrapped in a JAR-file, but instead exist as regular files on the host filesystem, thus
         * enabling us to iterate through 'em.
         *
         * @param classToCopyFrom Class in the JAR from which to copy, to identify the JAR.
         * @param dirToCopy       The source-directory in the JAR-file you wish to copy.
         * @param destination     The destination where the source-file should be copied to.
         * @param fileEnding      The file-ending to filter for.
         * @throws NullPointerException When the specified resource can not be found.
         * @author Griefed
         */
        @Throws(NullPointerException::class)
        private fun copyFolderFromDevEnv(
            classToCopyFrom: Class<*>,
            dirToCopy: String,
            destination: String,
            fileEnding: Regex
        ) {
            val filesFromJar: MutableList<String> = ArrayList(1000)
            val source = if (dirToCopy.startsWith("/")) {
                dirToCopy
            } else {
                "/$dirToCopy"
            }
            try {
                val classSource = classToCopyFrom.getResource(source)
                val classUri = classSource.toURI()
                val walkPath = classUri.toPath()
                Files.walk(walkPath).use {
                    for (path in it) {
                        val fileName = path.toString().replace("\\", "/")
                        if (fileName.matches(fileEnding) && !fileName.endsWith(dirToCopy)) {
                            filesFromJar.add(
                                fileName.substring(fileName.lastIndexOf(dirToCopy) + dirToCopy.length + 1)
                            )
                        }
                    }
                }
            } catch (ex: IOException) {
                log.error("Error walking source-directory.", ex)
            } catch (ex: URISyntaxException) {
                log.error("Error walking source-directory.", ex)
            }
            try {
                File(destination).create()
            } catch (ignored: FileAlreadyExistsException) {
            } catch (ex: IOException) {
                log.error("Error creating language directory.", ex)
            }
            for (entry in filesFromJar) {
                if (!File(destination, entry).absoluteFile.exists()) {
                    val extract = File(destination, entry).absoluteFile
                    extract.create()
                    try {
                        classToCopyFrom.getResourceAsStream("$source/$entry").use {
                            extract.outputStream().use { out -> it?.transferTo(out) }
                            log.debug("Copying from JAR: $entry")
                        }
                    } catch (ex: IOException) {
                        log.error("Error extracting files.", ex)
                    }
                }
            }
        }

        /**
         * Copy a folder from inside a JAR-file to the host filesystem. The specified folder will be
         * copied, along with all resources inside it, recursively, to the specified destination.<br> This
         * method is used when we are running in a JAR-file.
         *
         * @param jarToCopyFrom        The JAR-file to copy directoryToCopy to destinationDirectory from.
         * @param directoryToCopy      Path to the directory inside the JAR-file you want to copy.
         * @param destinationDirectory Path to the destination directory you want to copy source to.
         * @param jarDirectoryPrefix   A prefix to remove when checking for existence of source inside the
         *                             JAR-file. For example, the ServerPackCreator files inside it's
         *                             JAR-File are located in {@code BOOT-INF/classes} due to SpringBoot.
         *                             In order to correctly scan for source, we need to remove that
         *                             prefix, so we receive a path that looks like a regular path inside
         *                             a JAR-file.
         * @param fileEnding           The file-ending to filter for. If you want to filter for multiple
         *                             file-endings, separate them using {@code |}. Do not include the
         *                             {@code .}.
         * @throws IOException Thrown if no streams of the files can be created, indicating that they are
         *                     inaccessible for some reason.
         * @author Griefed
         */
        @Throws(IOException::class)
        private fun copyFolderFromJar(
            jarToCopyFrom: JarFile,
            directoryToCopy: String,
            destinationDirectory: String,
            jarDirectoryPrefix: String,
            fileEnding: Regex,
        ) {
            val entries: Enumeration<JarEntry> = jarToCopyFrom.entries()
            while (entries.hasMoreElements()) {
                val entry: JarEntry = entries.nextElement()
                val entryName = entry.name
                val check = if (jarDirectoryPrefix.isNotEmpty()) {
                    "$jarDirectoryPrefix/$directoryToCopy"
                } else {
                    directoryToCopy
                }
                if (entryName.startsWith(check) && entryName.matches(fileEnding)) {
                    val destination = File(
                        destinationDirectory,
                        entryName.replace(check, "")
                    ).absoluteFile
                    log.debug("Destination: $destination")
                    if (!destination.exists()) {
                        destination.create()
                        try {
                            jarToCopyFrom.getInputStream(entry).use {
                                destination.outputStream().use { out -> it.transferTo(out) }
                            }
                        } catch (ex: IOException) {
                            log.error("Couldn't acquire input stream for entry $entryName.", ex)
                        }
                    }
                }
            }
        }

        /**
         * Retrieve information about the environment for the given class.
         *
         * Available key-value-pairs:
         * * jarPath - The path to the JAR-file.
         * * jarName - The name of the JAR-file.
         * * javaVersion - The version of the Java
         * * installation used.
         * * osArch - Architecture of the system.
         * * osName - Name of the operating system.
         * * osVersion - Version of the operating system.
         *
         * @param classInJar Class for which to acquire information about its JAR-file and system.
         * @return A hashmap containing key-value-pairs with information about the JAR-file and system.
         * @author Griefed
         */
        fun jarInformation(classInJar: Class<*>): HashMap<String, String> {
            val sysInfo = HashMap<String, String>(10)
            sysInfo["jarPath"] = classInJar.source().path
            sysInfo["jarName"] = classInJar.source().name
            sysInfo["javaVersion"] = System.getProperty("java.version")
            sysInfo["osArch"] = System.getProperty("os.arch")
            sysInfo["osName"] = System.getProperty("os.name")
            sysInfo["osVersion"] = System.getProperty("os.version")
            return sysInfo
        }
    }
}