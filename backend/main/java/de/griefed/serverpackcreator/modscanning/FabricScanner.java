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

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Collection;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <code>fabric.mod.json</code>-based scanning of Fabric-Minecraft mods.
 *
 * @author Griefed
 */
@Component
public class FabricScanner extends JsonBasedScanner implements
    Scanner<TreeSet<File>, Collection<File>> {

  private static final Logger LOG = LogManager.getLogger(FabricScanner.class);
  private final ObjectMapper OBJECT_MAPPER;

  @Autowired
  public FabricScanner(ObjectMapper objectMapper) {
    this.OBJECT_MAPPER = objectMapper.enable(
        JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
  }

  /**
   * Scan the <code>fabric.mod.json</code>-files in mod JAR-files of a given directory for their
   * sideness.<br> If <code>environment</code> specifies <code>client</code>, and is not listed as a
   * dependency for another mod, it is added and therefore later on excluded from the server pack.
   *
   * @param filesInModsDir A list of files in which to check the
   *                       <code>fabric.mod.json</code>-files.
   * @return List String. List of mods not to include in server pack based on
   * fabric.mod.json-content.
   * @author Griefed
   */
  @Override
  public TreeSet<File> scan(Collection<File> filesInModsDir) {
    LOG.info("Scanning Fabric mods for sideness...");

    TreeSet<String> modDependencies = new TreeSet<>();
    TreeSet<String> clientMods = new TreeSet<>();

    /*
     * Go through all mods in our list and acquire a list of clientside-only mods as well as any
     * dependencies of the mods.
     */
    checkForClientModsAndDeps(filesInModsDir, clientMods, modDependencies);

    //Remove any dependency from out list of clientside-only mods, so we do not exclude any dependency.
    cleanupClientMods(modDependencies, clientMods);

    /*
     * After removing dependencies from the list of potential clientside mods, we can check whether
     * any of the remaining clientmods is available in our list of files. The resulting set is the
     * set of mods we can safely exclude from our server pack.
     */
    return getModsDelta(filesInModsDir, clientMods);
  }

  @Override
  void checkForClientModsAndDeps(Collection<File> filesInModsDir, TreeSet<String> clientMods,
      TreeSet<String> modDependencies) {
    for (File mod : filesInModsDir) {
      if (mod.toString().endsWith("jar")) {

        String modId;

        try {

          JsonNode modJson = getJarJson(mod, "fabric.mod.json", OBJECT_MAPPER);

          modId = getNestedText(modJson, "id");

          // Get this mods' id/name
          try {
            if (nestedTextEqualsIgnoreCase(modJson, "client", "environment")) {

              clientMods.add(modId);
              LOG.debug("Added clientMod: " + modId);
            }
          } catch (NullPointerException ignored) {

          }

          // Get this mods dependencies
          try {
            getFieldNames(modJson, "depends").forEachRemaining(modDependencies::add);
          } catch (NullPointerException ignored) {

          }

        } catch (Exception ex) {

          LOG.error("Couldn't scan " + mod, ex);
        }
      }
    }
  }

  @Override
  TreeSet<File> getModsDelta(Collection<File> filesInModsDir, TreeSet<String> clientMods) {
    TreeSet<File> modsDelta = new TreeSet<>();
    for (File mod : filesInModsDir) {

      String modIdTocheck;

      boolean addToDelta = false;

      try {

        JsonNode modJson =
            getJarJson(mod, "fabric.mod.json", OBJECT_MAPPER);

        // Get the modId
        modIdTocheck = getNestedText(modJson, "id");

        try {
          if (nestedTextEqualsIgnoreCase(modJson, "client", "environment")) {
            if (clientMods.contains(modIdTocheck)) {
              addToDelta = true;
            }
          }
        } catch (NullPointerException ignored) {

        }

        if (addToDelta) {
          modsDelta.add(mod);
        }

      } catch (Exception ignored) {

      }
    }
    return modsDelta;
  }
}
