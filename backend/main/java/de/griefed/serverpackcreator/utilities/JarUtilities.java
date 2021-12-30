/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.utilities;

import de.griefed.serverpackcreator.Main;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.system.ApplicationHome;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
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
     */
    public void copyFileFromJar(File fileToCopy) {

        if (!fileToCopy.exists()) {

            try {

                FileUtils.copyInputStreamToFile(
                        Objects.requireNonNull(Main.class.getResourceAsStream("/" + fileToCopy.getName())),
                        fileToCopy);

            } catch (IOException ex) {

                LOG.error("Error creating file: " + fileToCopy, ex);
            }

        }

    }

    /**
     * Retrieve the JAR-file for a given class.
     * @author Griefed
     * @param classToRetrieveJarFor The class to retrieve the JAR-file path for.
     * @return JarFile. Returns the JarFile for the given class.
     * @throws IOException Thrown if the JAR-file could not be determined or otherwise accessed.
     */
    public JarFile retrieveJarFromClass(Class<?> classToRetrieveJarFor) throws IOException {

        ApplicationHome applicationHome = new ApplicationHome(classToRetrieveJarFor);

        return new JarFile(new File(applicationHome.getSource().toString().replace("\\","/")));

    }

    /**
     * Copy a folder from inside a JAR-file to the host filesystem. The specified folder will be copied, along with all
     * resources inside it, recursively, to the specified destination.<br>
     * @author Griefed
     * @param jarPath String. Path to either the JAR-file from which to copy a folder from, or to the class when running
     *                in a dev-environment. This parameter decides whether {@link #copyFolderFromJar(String, String)} or
     *                {@link #copyFolderFromJar(JarFile, String, String, String)} is called.<br>
     *                Example for JAR-file: <code>/home/griefed/serverpackcreator/serverpackcreator.jar</code><br>
     *                Example for dev-environment: <code>G:/GitLab/ServerPackCreator/build/classes/java/main</code><br>
     *                See {@link Main#main(String[])} source code for an example on how this is acquired automatically.
     * @param directoryToCopy String. Path to the directory inside the JAR-file you want to copy.
     * @param destinationDirectory String. Path to the destination directory you want to copy source to.
     * @param jarDirectoryPrefix String. A prefix to remove when checking for existence of source inside the JAR-file. For example,
     *                           the ServerPackCreator files inside it's JAR-File are located in <code>BOOT-INF/classes</code> due
     *                           to SpringBoot. In order to correctly scan for source, we need to remove that prefix, so we receive
     *                           a path that looks like a regular path inside a JAR-file.
     * @throws IOException Exception thrown by {@link #copyFolderFromJar(JarFile, String, String, String)}
     */
    public void copyFolderFromJar(String jarPath, String directoryToCopy, String destinationDirectory, String jarDirectoryPrefix) throws IOException {

        LOG.debug("jarPath: " + jarPath);

        // Depending on dev or JAR-environment, folders need to be copied a different way
        if (jarPath.endsWith("main")) {
            copyFolderFromJar(directoryToCopy, destinationDirectory);
        } else {
            copyFolderFromJar(retrieveJarFromClass(Main.class), directoryToCopy, destinationDirectory, jarDirectoryPrefix);
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
     * @throws IOException Thrown if no streams of the files can be created, indicating that they are inaccessible for some reason.
     */
    private void copyFolderFromJar(JarFile jarToCopyFrom, String directoryToCopy, String destinationDirectory, String jarDirectoryPrefix) throws IOException {

        for (Enumeration<JarEntry> entries = jarToCopyFrom.entries(); entries.hasMoreElements();) {

            JarEntry entry = entries.nextElement();

            String entryName = entry.getName();

            if (entryName.replace(jarDirectoryPrefix,"").startsWith(directoryToCopy + "/") && !entry.isDirectory()) {

                File destination = new File(destinationDirectory + "/" + entryName.substring(entryName.replace("\\","/").lastIndexOf("/") + 1));

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

                    while ((length = inputStream.read(bytes)) > 0) {
                        fileOutputStream.write(bytes, 0, length);
                    }

                } catch (IOException ex) {

                    throw new IOException("Couldn't copy asset " + entryName + " from JAR-file.", ex);

                } finally {

                    try {
                        inputStream.close();
                    } catch (IOException ignored) {}

                    try {
                        fileOutputStream.close();
                    } catch (IOException ignored) {}

                }

            }
        }
    }

    /**
     * Copy a folder from inside our JAR to the host filesystem, using a source and a destination.<br>
     * This method is used in case we are running in a dev environment, where files are not wrapped in a JAR-file, but instead
     * exist as regular files on the host filesystem, thus enabling us to iterate through 'em.
     * @author Griefed
     * @param source File. The source-file in the JAR you wish to copy outside the JAR.
     * @param destination File. The destination where the source-file should be copied to.
     */
    private void copyFolderFromJar(String source, String destination) {

        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.toString().endsWith(".properties");
            }
        };

        File[] files;
        List<String> langFiles = new ArrayList<>(1000);

        try {

            files = new File(Objects.requireNonNull(Main.class.getResource(source)).toURI()).listFiles(fileFilter);

            assert files != null;
            for (File value : files) {

                String file = value.toString().replace("\\", "/");

                langFiles.add(file.substring(file.lastIndexOf("/") + 1));
            }

            langFiles.forEach(System.out::println);

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

        langFiles.forEach(file -> {

            try (InputStream inputStream = Main.class.getResourceAsStream(source + "/" + file)) {

                assert inputStream != null;
                FileUtils.copyInputStreamToFile(inputStream, new File(destination + "/" + file));

            } catch (IOException ex) {
                LOG.error("Error extracting files.", ex);
            }

        });

    }

}
