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
package de.griefed.serverpackcreator.utilities.common;

import de.griefed.serverpackcreator.ServerPackCreator;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

/**
 * Some utilities used across ServerPackCreator, revolving around interacting with JAR-files.
 *
 * @author Griefed
 */
@Component
public final class JarUtilities {

  private static final Logger LOG = LogManager.getLogger(JarUtilities.class);

  public JarUtilities() {
  }

  /**
   * Copy a file from inside our JAR-file to the host filesystem. The file will create exactly as
   * specified in the parameter. <br> Example:<br> {@code copyFileFromJar(new File("log4j2.xml"))}
   * will result in the log4j2.xml file from inside the JAR-file to be copied to the outside of the
   * JAR-file as {@code log4j2.xml}
   *
   * @param fileToCopy      File. The source-file in the JAR you wish to copy outside the JAR.
   * @param replaceIfExists Boolean. Whether to replace the file, if it already exists.
   * @param classToCopyFrom Class. The class of the JAR from which you want to copy from.
   * @param directory       The directory where the file should be copied to.
   * @author Griefed
   */
  public void copyFileFromJar(String fileToCopy, boolean replaceIfExists,
      Class<?> classToCopyFrom, String directory) {

    if (replaceIfExists) {
      FileUtils.deleteQuietly(new File(directory, fileToCopy));
    }
    copyFileFromJar(fileToCopy, classToCopyFrom, directory);
  }

  /**
   * Copy a file from inside our JAR-file to the host filesystem. The file will be created with
   * exactly the same path specified in the parameter. <br> Example:<br>
   * {@code copyFileFromJar(new File("log4j2.xml"))} will result in the log4j2.xml file from inside
   * the JAR-file to be copied to the outside of the JAR-file as {@code log4j2.xml}
   *
   * @param fileToCopy      The source-file in the JAR you wish to copy outside the JAR.
   * @param identifierClass The class of the JAR from which to get the resource.
   * @param directory       The directory to copy the file to.
   * @return {@code true} if the file was created, {@code false} otherwise.
   * @author Griefed
   */
  public boolean copyFileFromJar(String fileToCopy, Class<?> identifierClass, String directory) {

    if (!new File(directory, fileToCopy).exists()) {

      try {

        FileUtils.copyInputStreamToFile(
            Objects.requireNonNull(identifierClass.getResourceAsStream("/" + fileToCopy)),
            new File(directory, fileToCopy));

        return true;

      } catch (IOException ex) {

        LOG.error("Error creating file: " + fileToCopy, ex);
        return false;
      }

    } else {

      LOG.info("File " + fileToCopy + " already exists.");
      return false;
    }
  }

  /**
   * Copy a file from inside our JAR-file to the host filesystem to the specified destination,
   * replacing an already existing file. The file will be created with exactly the same path
   * specified in the parameter. <br> Example:<br> {@code copyFileFromJar(new File("log4j2.xml"))}
   * will result in the log4j2.xml file from inside the JAR-file to be copied to the outside of the
   * JAR-file as {@code log4j2.xml}
   *
   * @param fileToCopy      The source-file in the JAR you wish to copy outside the JAR.
   * @param destinationFile The file to which to copy to.
   * @param identifierClass The class of the JAR from which to get the resource.
   * @param replaceIfExists Boolean. Whether to replace the file, if it already exists.
   * @author Griefed
   */
  public void copyFileFromJar(String fileToCopy, File destinationFile, boolean replaceIfExists,
      Class<?> identifierClass) {

    if (replaceIfExists) {
      FileUtils.deleteQuietly(destinationFile);
    }
    copyFileFromJar(fileToCopy, destinationFile, identifierClass);
  }

  /**
   * Copy a file from inside our JAR-file to the host filesystem to the specified destination. The
   * file will be created with exactly the same path specified in the parameter. <br> Example:<br>
   * {@code copyFileFromJar(new File("log4j2.xml"))} will result in the log4j2.xml file from inside
   * the JAR-file to be copied to the outside of the JAR-file as {@code log4j2.xml}
   *
   * @param fileToCopy      The source-file in the JAR you wish to copy outside the JAR.
   * @param identifierClass The class of the JAR from which to get the resource.
   * @param destinationFile The file to which to copy to.
   * @return {@code true} if the file was created, {@code false} otherwise.
   * @author Griefed
   */
  public boolean copyFileFromJar(String fileToCopy, File destinationFile,
      Class<?> identifierClass) {

    if (!destinationFile.exists()) {

      try {

        FileUtils.copyInputStreamToFile(
            Objects.requireNonNull(identifierClass.getResourceAsStream("/" + fileToCopy)),
            destinationFile);
        return true;

      } catch (IOException ex) {

        LOG.error("Error creating file: " + destinationFile, ex);
        return false;
      }

    } else {

      LOG.info("File " + destinationFile + " already exists.");
      return false;
    }
  }

  /**
   * Retrieve the ApplicationHome for a given class.
   *
   * @param classToRetrieveHomeFor The class to retrieve the {@link ApplicationHome} for.
   * @return An instance of {@link ApplicationHome} for the given class.
   * @author Griefed
   */
  public ApplicationHome getApplicationHomeForClass(Class<?> classToRetrieveHomeFor) {
    return new ApplicationHome(classToRetrieveHomeFor);
  }

  /**
   * Retrieve information about the environment for the given instance of {@link ApplicationHome},
   * stored in a {@link HashMap}.<br> Available key-value-pairs:<br> jarPath - The path to the
   * JAR-file.<br> jarName - The name of the JAR-file.<br> javaVersion - The version of the Java
   * installation used.<br> osArch - Architecture of the system.<br> osName - Name of the operating
   * system.<br> osVersion - Version of the operating system.<br>
   *
   * @param classInJar The class in the jar for which you want to acquire the information for.
   * @return HashMap String-String. A hashmap containing key-value-pairs with information about the
   * JAR-file and system.
   * @author Griefed
   */
  public HashMap<String, String> jarInformation(Class<?> classInJar) {
    return jarInformation(getApplicationHomeForClass(classInJar));
  }

  /**
   * Retrieve information about the environment for the given instance of {@link ApplicationHome},
   * stored in a {@link HashMap}.<br> Available key-value-pairs:<br> jarPath - The path to the
   * JAR-file.<br> jarName - The name of the JAR-file.<br> javaVersion - The version of the Java
   * installation used.<br> osArch - Architecture of the system.<br> osName - Name of the operating
   * system.<br> osVersion - Version of the operating system.<br>
   *
   * @param applicationHome Instance of {@link ApplicationHome} from which to gather information
   *                        about the JAR-file and system.
   * @return HashMap String-String. A hashmap containing key-value-pairs with information about the
   * JAR-file and system.
   * @author Griefed
   */
  public HashMap<String, String> jarInformation(ApplicationHome applicationHome) {

    HashMap<String, String> sysInfo = new HashMap<>(10);

    try {
      sysInfo.put("jarPath", applicationHome.getSource().toString());
    } catch (Exception ex) {
      sysInfo.put("jarPath", applicationHome.getDir().toString());
    }

    try {
      sysInfo.put("jarName", applicationHome.getSource().getName());
    } catch (Exception ex) {
      sysInfo.put("jarName", applicationHome.getDir().getName());
    }

    sysInfo.put("javaVersion", System.getProperty("java.version"));
    sysInfo.put("osArch", System.getProperty("os.arch"));
    sysInfo.put("osName", System.getProperty("os.name"));
    sysInfo.put("osVersion", System.getProperty("os.version"));

    return sysInfo;
  }

  /**
   * Retrieve the JAR-file for a given class.
   *
   * @param classToRetrieveJarFor The class to retrieve the JAR-file path for.
   * @return JarFile. Returns the JarFile for the given class.
   * @throws IOException Thrown if the JAR-file could not be determined or otherwise accessed.
   * @author Griefed
   */
  private JarFile retrieveJarFromClass(Class<?> classToRetrieveJarFor) throws IOException {
    return new JarFile(new ApplicationHome(classToRetrieveJarFor).getSource());
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
  public void copyFolderFromJar(
      Class<?> classToRetrieveHomeFor,
      String directoryToCopy,
      String destinationDirectory,
      String jarDirectoryPrefix,
      String fileEnding)
      throws IOException {

    HashMap<String, String> systemInformation =
        jarInformation(classToRetrieveHomeFor);

    File source = new File(systemInformation.get("jarPath"));
    if (!source.isFile() && source.isDirectory()) {
      // Dev environment
      copyFolderFromJar(classToRetrieveHomeFor, directoryToCopy, destinationDirectory, fileEnding);
    } else {
      // Production
      copyFolderFromJar(
          retrieveJarFromClass(classToRetrieveHomeFor),
          directoryToCopy,
          destinationDirectory,
          jarDirectoryPrefix,
          fileEnding);
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
   * @param fileEnding           The file-ending to filter for.
   * @throws IOException Thrown if no streams of the files can be created, indicating that they are
   *                     inaccessible for some reason.
   * @author Griefed
   */
  private void copyFolderFromJar(
      JarFile jarToCopyFrom,
      String directoryToCopy,
      String destinationDirectory,
      String jarDirectoryPrefix,
      String fileEnding)
      throws IOException {

    for (Enumeration<JarEntry> entries = jarToCopyFrom.entries(); entries.hasMoreElements(); ) {

      JarEntry entry = entries.nextElement();

      String entryName = entry.getName();

      if (entryName.replace(jarDirectoryPrefix, "").startsWith(directoryToCopy + "/")
          && !entry.isDirectory()
          && entryName.endsWith(fileEnding)) {

        File destination =
            new File(
                destinationDirectory,
                entryName.replace(jarDirectoryPrefix, "").replace(directoryToCopy, ""));

        LOG.debug("Destination: " + destination);

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

            int length;

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

            LOG.debug("Copied from JAR:" + entryName + ".");
          }
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
   * @param source          The source-file in the JAR you wish to copy outside the JAR.
   * @param destination     The destination where the source-file should be copied to.
   * @param fileEnding      The file-ending to filter for.
   * @author Griefed
   */
  private void copyFolderFromJar(
      Class<?> classToCopyFrom, String source, String destination, String fileEnding) {

    FileFilter fileFilter = pathname -> pathname.toString().endsWith(fileEnding);

    File[] files;
    List<String> filesFromJar = new ArrayList<>(1000);

    try {

      files =
          new File(Objects.requireNonNull(classToCopyFrom.getResource(source)).toURI())
              .listFiles(fileFilter);

      assert files != null;

      for (File file : files) {
        filesFromJar.add(file.getName());
      }

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

    filesFromJar.forEach(
        file -> {
          if (!new File(destination, file).exists()) {

            try (InputStream inputStream =
                classToCopyFrom.getResourceAsStream(source + "/" + file)) {

              assert inputStream != null;
              FileUtils.copyInputStreamToFile(inputStream, new File(destination, file));

              LOG.debug("Copying from JAR: " + file);
            } catch (IOException ex) {
              LOG.error("Error extracting files.", ex);
            }
          }
        });
  }
}
