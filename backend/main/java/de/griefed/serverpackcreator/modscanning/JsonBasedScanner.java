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
package de.griefed.serverpackcreator.modscanning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Helper-class containing methods implemented and used by JSON-based scanners, like the
 * {@link AnnotationScanner}, {@link FabricScanner} and {@link QuiltScanner}.
 *
 * @author Griefed
 */
abstract class JsonBasedScanner {

  private static final Logger LOG = LogManager.getLogger(JsonBasedScanner.class);

  /**
   * Acquire a JsonNode from the specified file in the specified file.
   *
   * @param file         The file from which to get the JsonNode from.
   * @param entryInJar   The file in the jar from which to get the JsonNode form.
   * @param objectMapper The ObjectMapper with which to parse the Json to a JsonNode.
   * @return A JsonNode containing all information from the requested file in the specified jar.
   * @throws IOException           if the file could not be opened or read from, if the file could
   *                               not be read or if the json from the file could not be parsed into
   *                               a JsonNode.
   * @throws SecurityException     if an error occurs reading the file in the jar.
   * @throws IllegalStateException if an error occurs reading the file in the jar.
   * @throws NullPointerException  if the jar does not contain the specified entry.
   * @author Griefed
   */
  @NotNull JsonNode getJarJson(@NotNull File file,
                               @NotNull String entryInJar,
                               @NotNull ObjectMapper objectMapper)
      throws NullPointerException, IOException, SecurityException, IllegalStateException {

    JarFile jarFile = new JarFile(file);
    JarEntry jarEntry = jarFile.getJarEntry(entryInJar);
    InputStream inputStream = jarFile.getInputStream(jarEntry);

    JsonNode jsonNode = objectMapper.readTree(inputStream);

    jarFile.close();
    inputStream.close();

    return jsonNode;
  }

  /**
   * Remove any dependency from the list of clientside-only mods to prevent excluding dependencies
   * of other mods, resulting in a potentially broken server pack.
   *
   * @param modDependencies A set of modIds of dependencies.
   * @param clientMods      A set of modIds of clientside-only mods.
   * @author Griefed
   */
  void cleanupClientMods(@NotNull TreeSet<String> modDependencies,
                         @NotNull TreeSet<String> clientMods) {
    for (String dependency : modDependencies) {

      clientMods.removeIf(mod -> {
        if (mod.equals(dependency)) {
          LOG.debug(
              "Removing "
                  + dependency
                  + " from list of clientmods as it is a dependency for another mod.");
          return true;

        } else {
          return false;
        }
      });
    }
  }

  /**
   * Check every file and fill the {@code clientMods} and {@code modDependencies} sets with ids of
   * mods which are clientside-only or dependencies of a mod.
   *
   * @param filesInModsDir  Collection of files to check.
   * @param clientMods      Set of clientside-only mod-ids.
   * @param modDependencies Set dependencies of other mods.
   * @author Griefed
   */
  abstract void checkForClientModsAndDeps(@NotNull Collection<File> filesInModsDir,
                                          @NotNull TreeSet<String> clientMods,
                                          @NotNull TreeSet<String> modDependencies);

  /**
   * Get a list of mod-jars which can safely be excluded from the server pack.
   *
   * @param filesInModsDir A collection of files from which to exclude mods.
   * @param clientMods     A set of modIds which are clientside-only.
   * @return A set of all files from the passed collection which can safely be excluded from the
   * server pack.
   * @author Griefed
   */
  abstract @NotNull TreeSet<File> getModsDelta(@NotNull Collection<File> filesInModsDir,
                                               @NotNull TreeSet<String> clientMods);
}
