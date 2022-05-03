/* Copyright (C) 2022  Griefed
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
package de.griefed.serverpackcreator.utilities.commonutilities;

import de.griefed.serverpackcreator.Main;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.system.ApplicationHome;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Some utilities used across ServerPackCreator, revolving around interacting with JAR-files.
 * @author Griefed
 */
public class JarUtilities {

    private static final Logger LOG = LogManager.getLogger(JarUtilities.class);

    public JarUtilities() {

    }

    /**
     * Copy a file from inside our JAR-file to the host filesystem. The file will create exactly as specified in the parameter.
     * <br>
     * Example:<br>
     * <code>copyFileFromJar(new File("log4j2.xml"))</code> will result in the log4j2.xml file from inside the JAR-file to be copied
     * to the outside of the JAR-file as <code>log4j2.xml</code>
     * @author Griefed
     * @param fileToCopy File. The source-file in the JAR you wish to copy outside the JAR.
     * @param replace Boolean. Whether to replace the file, if it already exists.
     * @param classToCopyFrom Class. The class of the JAR from which you want to copy from.
     */
    public void copyFileFromJar(File fileToCopy, boolean replace, Class<?> classToCopyFrom) {

        if (fileToCopy.exists() && replace) {

            FileUtils.deleteQuietly(fileToCopy);
            copyFileFromJar(fileToCopy, classToCopyFrom);

        } else if (fileToCopy.exists() && !replace) {

            LOG.info("File " + fileToCopy + " already exists.");

        } else if (!fileToCopy.exists()) {

            copyFileFromJar(fileToCopy, classToCopyFrom);

        }

    }

    /**
     * Copy a file from inside our JAR-file to the host filesystem. The file will create exactly as specified in the parameter.
     * <br>
     * Example:<br>
     * <code>copyFileFromJar(new File("log4j2.xml"))</code> will result in the log4j2.xml file from inside the JAR-file to be copied
     * to the outside of the JAR-file as <code>log4j2.xml</code>
     * @author Griefed
     * @param fileToCopy File. The source-file in the JAR you wish to copy outside the JAR.
     * @param classToCopyFrom Class. The class of the JAR from which to get the resource.
     */
    public void copyFileFromJar(File fileToCopy, Class<?> classToCopyFrom) {

        if (!fileToCopy.exists()) {

            try {

                FileUtils.copyInputStreamToFile(
                        Objects.requireNonNull(classToCopyFrom.getResourceAsStream("/" + fileToCopy.getName())),
                        fileToCopy);

            } catch (IOException ex) {

                LOG.error("Error creating file: " + fileToCopy, ex);
            }

        } else {

            LOG.info("File " + fileToCopy + " already exists.");
        }

    }

    /**
     * Retrieve the ApplicationHome for a given class.
     * @author Griefed
     * @param classToRetrieveHomeFor Class. The class to retrieve the {@link ApplicationHome} for.
     * @return ApplicationHome. An instance of {@link ApplicationHome} for the given class.
     */
    public ApplicationHome getApplicationHomeForClass(Class<?> classToRetrieveHomeFor) {
        return new ApplicationHome(classToRetrieveHomeFor);
    }

    /**
     * Retrieve information about the environment for the given instance of {@link ApplicationHome}, stored in a {@link HashMap}.<br>
     * Available key-value-pairs:<br>
     * jarPath - The path to the JAR-file.<br>
     * jarName - The name of the JAR-file.<br>
     * javaVersion - The version of the Java installation used.<br>
     * osArch - Architecture of the system.<br>
     * osName - Name of the operating system.<br>
     * osVersion - Version of the operating system.<br>
     * @author Griefed
     * @param applicationHome Instance of {@link ApplicationHome} from which to gather information about the JAR-file and system.
     * @return HashMap String-String. A hashmap containing key-value-pairs with information about the JAR-file and system.
     */
    public HashMap<String, String> systemInformation(ApplicationHome applicationHome) {

        HashMap<String, String> sysInfo = new HashMap<>();

        try {
            sysInfo.put("jarPath"    ,applicationHome.getSource().toString().replace("\\", "/"));
        } catch (Exception ex) {
            sysInfo.put("jarPath"    ,applicationHome.getDir().toString().replace("\\", "/"));
        }

        try {
            sysInfo.put("jarName"    ,applicationHome.getSource().toString().replace("\\", "/").substring(applicationHome.getSource().toString().replace("\\", "/").lastIndexOf("/") + 1));
        } catch (Exception ex) {
            sysInfo.put("jarName"    ,applicationHome.getDir().toString().replace("\\", "/").substring(applicationHome.getDir().toString().replace("\\", "/").lastIndexOf("/") + 1));
        }

        sysInfo.put("javaVersion",System.getProperty("java.version"));
        sysInfo.put("osArch"     ,System.getProperty("os.arch"));
        sysInfo.put("osName"     ,System.getProperty("os.name"));
        sysInfo.put("osVersion"  ,System.getProperty("os.version"));

        return sysInfo;
    }

    /**
     * Retrieve the JAR-file for a given class.
     * @author Griefed
     * @param classToRetrieveJarFor The class to retrieve the JAR-file path for.
     * @return JarFile. Returns the JarFile for the given class.
     * @throws IOException Thrown if the JAR-file could not be determined or otherwise accessed.
     */
    private JarFile retrieveJarFromClass(Class<?> classToRetrieveJarFor) throws IOException {
        return new JarFile(
                new File(
                        new ApplicationHome(classToRetrieveJarFor)
                                .getSource().toString().replace("\\","/")
                )
        );
    }

    /**
     * Copy a folder from inside a JAR-file to the host filesystem. The specified folder will be copied, along with all
     * resources inside it, recursively, to the specified destination.<br>
     * @author Griefed
     * @param classToRetrieveHomeFor String. Path to either the JAR-file from which to copy a folder from, or to the class when running
     *                in a dev-environment. This parameter decides whether {@link #copyFolderFromJar(Class,String, String, String)} or
     *                {@link #copyFolderFromJar(JarFile, String, String, String, String)} is called.<br>
     *                Example for JAR-file: <code>/home/griefed/serverpackcreator/serverpackcreator.jar</code><br>
     *                Example for dev-environment: <code>G:/GitLab/ServerPackCreator/build/classes/java/main</code><br>
     *                See {@link Main#main(String[])} source code for an example on how this is acquired automatically.
     * @param directoryToCopy String. Path to the directory inside the JAR-file you want to copy.
     * @param destinationDirectory String. Path to the destination directory you want to copy source to.
     * @param jarDirectoryPrefix String. A prefix to remove when checking for existence of source inside the JAR-file. For example,
     *                           the ServerPackCreator files inside it's JAR-File are located in <code>BOOT-INF/classes</code> due
     *                           to SpringBoot. In order to correctly scan for source, we need to remove that prefix, so we receive
     *                           a path that looks like a regular path inside a JAR-file.
     * @param fileEnding String. The file-ending to filter for.
     * @throws IOException Exception thrown if a file can not be accessed, found or otherwise worked with.
     */
    public void copyFolderFromJar(Class<?> classToRetrieveHomeFor, String directoryToCopy, String destinationDirectory, String jarDirectoryPrefix,
                                  String fileEnding) throws IOException {

        HashMap<String,String> systemInformation = systemInformation(
                getApplicationHomeForClass(classToRetrieveHomeFor)
        );

        if (!new File(systemInformation.get("jarName")).isFile()) {
            // Dev environment
            copyFolderFromJar(classToRetrieveHomeFor,directoryToCopy, destinationDirectory, fileEnding);
        } else {
            // Production
            copyFolderFromJar(retrieveJarFromClass(classToRetrieveHomeFor), directoryToCopy, destinationDirectory, jarDirectoryPrefix, fileEnding);
        }

    }

    /**
     * Copy a folder from inside a JAR-file to the host filesystem. The specified folder will be copied, along with all
     * resources inside it, recursively, to the specified destination.<br>
     * This method is used when we are running in a JAR-file.
     * @author Griefed
     * @param jarToCopyFrom JarFile. The JAR-file to copy directoryToCopy to destinationDirectory from.
     * @param directoryToCopy String. Path to the directory inside the JAR-file you want to copy.
     * @param destinationDirectory String. Path to the destination directory you want to copy source to.
     * @param jarDirectoryPrefix String. A prefix to remove when checking for existence of source inside the JAR-file. For example,
     *                           the ServerPackCreator files inside it's JAR-File are located in <code>BOOT-INF/classes</code> due
     *                           to SpringBoot. In order to correctly scan for source, we need to remove that prefix, so we receive
     *                           a path that looks like a regular path inside a JAR-file.
     * @param fileEnding String. The file-ending to filter for.
     * @throws IOException Thrown if no streams of the files can be created, indicating that they are inaccessible for some reason.
     */
    private void copyFolderFromJar(JarFile jarToCopyFrom, String directoryToCopy, String destinationDirectory, String jarDirectoryPrefix, String fileEnding) throws IOException {

        for (Enumeration<JarEntry> entries = jarToCopyFrom.entries(); entries.hasMoreElements();) {

            JarEntry entry = entries.nextElement();

            String entryName = entry.getName();

            if (entryName.replace(jarDirectoryPrefix,"").startsWith(directoryToCopy + "/") && !entry.isDirectory() && entryName.endsWith(fileEnding)) {

                File destination = new File(destinationDirectory + "/" + entryName.substring(entryName.replace("\\","/").lastIndexOf("/") + 1));

                if (!destination.exists()) {

                    File parent = destination.getParentFile();

                    if (parent != null && parent.mkdirs()) {

                        LOG.debug("Created directory " + parent + ".");

                    }

                    FileOutputStream fileOutputStream = null;
                    InputStream inputStream = null;

                    try {
                        fileOutputStream = new FileOutputStream(destination);
                    } catch (FileNotFoundException ex) {
                        LOG.error("File " + destination + " not found.", ex);
                    }

                    try {
                        inputStream = jarToCopyFrom.getInputStream(entry);
                    } catch (IOException ex) {
                        LOG.error("Couldn't acquire input stream for entry " + entryName + ".", ex);
                    }

                    try {

                        byte[] bytes = new byte[8192];

                        int length = 0;

                        if (inputStream != null) {

                            while ((length = inputStream.read(bytes)) > 0) {

                                if (fileOutputStream != null) {
                                    fileOutputStream.write(bytes, 0, length);
                                } else {
                                    LOG.error("FileOutputStream is null for " + destination + ".");
                                }

                            }

                        } else {
                            LOG.error("InputStream is null for " + destination + ".");
                        }


                    } catch (IOException ex) {

                        throw new IOException("Couldn't copy asset " + entryName + " from JAR-file.", ex);

                    } finally {

                        try {
                            if (inputStream != null) {
                                inputStream.close();
                            }
                        } catch (Exception ignored) {

                        }

                        try {
                            if (fileOutputStream != null) {
                                fileOutputStream.close();
                            }
                        } catch (Exception ignored) {

                        }

                    }

                }

            }
        }
    }

    /**
     * Copy a folder from inside our dev-resources to the host filesystem, using a source and a destination.<br>
     * This method is used in case we are running in a dev environment, where files are not wrapped in a JAR-file, but instead
     * exist as regular files on the host filesystem, thus enabling us to iterate through 'em.
     * @author Griefed
     * @param classToCopyFrom {@link Class}
     * @param source {@link File} The source-file in the JAR you wish to copy outside the JAR.
     * @param destination {@link File} The destination where the source-file should be copied to.
     * @param fileEnding {@link String} The file-ending to filter for.
     */
    private void copyFolderFromJar(Class<?> classToCopyFrom, String source, String destination, String fileEnding) {

        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.toString().endsWith(fileEnding);
            }
        };

        File[] files;
        List<String> filesFromJar = new ArrayList<>(1000);

        try {

            files = new File(Objects.requireNonNull(classToCopyFrom.getResource(source)).toURI()).listFiles(fileFilter);

            assert files != null;
            for (File value : files) {

                String file = value.toString().replace("\\", "/");

                filesFromJar.add(file.substring(file.lastIndexOf("/") + 1));
            }

            filesFromJar.forEach(file -> LOG.debug("Copying from JAR: " + file));

        } catch (URISyntaxException ex) {
            LOG.error("Error retrieving file list from JAR.", ex);
        }

        try {
            Files.createDirectory(Paths.get(destination));
        } catch (IOException ex) {
            if (!ex.toString().startsWith("java.nio.file.FileAlreadyExistsException")) {
                LOG.error("Error creating language directory.", ex);
            }
        }

        filesFromJar.forEach(file -> {

            if (!new File(destination + "/" + file).exists()) {

                try (InputStream inputStream = classToCopyFrom.getResourceAsStream(source + "/" + file)) {

                    assert inputStream != null;
                    FileUtils.copyInputStreamToFile(inputStream, new File(destination + "/" + file));

                } catch (IOException ex) {
                    LOG.error("Error extracting files.", ex);
                }

            }
        });

    }

}
