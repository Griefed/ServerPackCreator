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
import de.griefed.serverpackcreator.utilities.common.JsonException;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <code>fml-cache-annotation.json</code>-based scanning of Forge-Minecraft mods of older versions.
 *
 * @author Griefed
 */
@Component
public final class AnnotationScanner extends JsonBasedScanner implements
    Scanner<TreeSet<File>, Collection<File>> {

  private static final Logger LOG = LogManager.getLogger(AnnotationScanner.class);
  private final ObjectMapper OBJECT_MAPPER;
  private final Utilities UTILITIES;
  private final String DEPENDENCY_REPLACE_REGEX = "(@.*|\\[.*)";
  private final String DEPENDENCY_CHECK_REGEX = "(before:.*|after:.*|required-after:.*|)";

  @Autowired
  public AnnotationScanner(ObjectMapper objectMapper, Utilities utilities) {
    this.OBJECT_MAPPER = objectMapper;
    this.UTILITIES = utilities;
  }

  /**
   * Scan the <code>fml-cache-annotation.json</code>-files in mod JAR-files of a given directory for
   * their sideness.<br> If <code>clientSideOnly</code> specifies <code>"value": "true"</code>, and
   * is not listed as a dependency for another mod, it is added and therefore later on excluded from
   * the server pack.
   *
   * @param filesInModsDir A list of files in which to check the <code>fml-cache-annotation.json
   *                       </code>-files.
   * @return List of mods not to include in server pack based on fml-cache-annotation.json-content.
   * @author Griefed
   */
  @Override
  public TreeSet<File> scan(Collection<File> filesInModsDir) {

    LOG.info("Scanning Minecraft 1.12.x and older mods for sideness...");

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
  void checkForClientModsAndDeps(Collection<File> filesInModsDir,
      TreeSet<String> clientMods, TreeSet<String> modDependencies) {
    for (File mod : filesInModsDir) {
      if (mod.toString().endsWith("jar")) {

        String modId = null;
        TreeSet<String> additionalMods = new TreeSet<>();

        try {

          JsonNode modJson = getJarJson(mod, "META-INF/fml_cache_annotation.json", OBJECT_MAPPER);

          // base of json
          for (JsonNode node : modJson) {

            try {
              // iterate though annotations
              for (JsonNode child : node.get("annotations")) {

                // Get the mod ID and check for clientside only, if we have not yet received a
                // modID
                if (modId == null) {

                  try {

                    modId = getModId(child);
                    // Get the modId

                    // Add mod to list of clientmods if clientSideOnly is true
                    checkForClientSide(child, modId, clientMods);

                  } catch (NullPointerException | JsonException ignored) {

                  }

                  // We already received a modId, perform additional checks to prevent false
                  // positives
                } else {

                  try {
                    // Get the additional modID
                    checkAdditionalId(child, modId, clientMods, additionalMods);

                  } catch (NullPointerException ignored) {

                  }
                }

                // Get dependency modIds
                checkDependencies(child, modDependencies);
              }

            } catch (NullPointerException ignored) {

            }
          }

          if (!additionalMods.isEmpty()) {
            checkAdditionalMods(modId, additionalMods, modJson, clientMods);
          }
        } catch (Exception ex) {
          LOG.error("Couldn't scan " + mod, ex);
        }

      }
    }
  }

  /**
   * Get the id of the mod currently being checked.
   *
   * @param jsonNode The JSON node containing the modId.
   * @return The id of the mod.
   * @throws NullPointerException if the JSON node does not contain the modId.
   * @author Griefed
   */
  private String getModId(JsonNode jsonNode) throws NullPointerException {
    if (!UTILITIES.JsonUtilities().nestedTextIsEmpty(jsonNode, "values", "modid", "value")) {
      return UTILITIES.JsonUtilities().getNestedText(jsonNode, "values", "modid", "value");
    } else {
      throw new NullPointerException("No modId present.");
    }
  }

  /**
   * Check whether the mod is clientside only.
   *
   * @param jsonNode   The JSON node containing information about the sideness.
   * @param modId      The id of the mod.
   * @param clientMods Set to the <code>modId</code> if the mod is clientside-only.
   * @throws NullPointerException if the JSON node does not contain sideness information.
   * @throws JsonException        if the text in the boolean-field is neither <code>true</code> nor
   *                              <code>false</code>.
   * @author Griefed
   */
  private void checkForClientSide(JsonNode jsonNode, String modId, TreeSet<String> clientMods)
      throws NullPointerException, JsonException {
    if (UTILITIES.JsonUtilities().getNestedBoolean(jsonNode, "values", "clientSideOnly", "value")) {

      clientMods.add(modId);
      LOG.debug("Added clientMod: " + modId);
    }
  }

  /**
   * Compare the additional id in a JSON node for match with the parent modId. If the additional id
   * is the same as the parent id, check for sideness and add it to our set of clientMods, otherwise
   * add the id to our set of additionalMods.
   *
   * @param child          JSON node containing information about the additional id.
   * @param modId          The id of the parent mod.
   * @param clientMods     Set containing our clientside-only mod ids.
   * @param additionalMods Set containing our additional mod ids.
   * @throws NullPointerException if the JSON node contains no additional mod id.
   * @author Griefed
   */
  private void checkAdditionalId(JsonNode child, String modId, TreeSet<String> clientMods,
      TreeSet<String> additionalMods) throws NullPointerException {
    if (!UTILITIES.JsonUtilities().nestedTextIsEmpty(child, "values", "modid", "value")) {

      // ModIDs are the same, so check for clientside-only
      if (UTILITIES.JsonUtilities()
          .nestedTextEqualsIgnoreCase(child, modId, "values", "modid", "value")) {

        try {
          // Add mod to list of clientmods if clientSideOnly is true
          if (UTILITIES.JsonUtilities()
              .getNestedBoolean(child, "values", "clientSideOnly", "value")) {

            clientMods.add(modId);
            LOG.debug("Added clientMod: " + modId);
          }
        } catch (NullPointerException | JsonException ignored) {

        }

        // ModIDs are different, possibly two mods in one JAR-file.......
      } else {

        // Add additional modId to list, so we can check those later
        additionalMods.add(
            UTILITIES.JsonUtilities().getNestedText(child, "values", "modid", "value"));
      }
    }
  }

  /**
   * Check the dependencies of our mod for sideness. Any dependency that is not <code>forge</code>,
   * and whose sideness is clientside-only, gets added to the list of required dependencies.
   *
   * @param child           JSON node containing information about our dependencies.
   * @param modDependencies Set containing our dependency ids.
   * @author Griefed
   */
  private void checkDependencies(JsonNode child, TreeSet<String> modDependencies) {
    try {
      if (!UTILITIES.JsonUtilities().nestedTextIsEmpty(child, "values", "dependencies", "value")) {

        // There are multiple dependencies for this mod
        if (UTILITIES.JsonUtilities()
            .nestedTextContains(child, ";", "values", "dependencies", "value")) {

          String[] dependencies = UTILITIES.JsonUtilities()
              .getNestedTexts(child, ";", "values", "dependencies", "value");

          for (String dependency : dependencies) {

            if (dependency.matches(DEPENDENCY_CHECK_REGEX)) {

              dependency =
                  getDependency(dependency);

              if (!dependency.equalsIgnoreCase("forge") && !dependency.equals("*")) {
                modDependencies.add(dependency);

                LOG.debug("Added dependency " + dependency);
              }
            }
          }

          // There is only one dependency, or it is a regular minecraft/forge dependency.
        } else {
          if (UTILITIES.JsonUtilities().nestedTextMatches(
              child,
              DEPENDENCY_CHECK_REGEX,
              "values", "dependencies", "value")
          ) {

            String dependencies = UTILITIES.JsonUtilities()
                .getNestedText(child, "values", "dependencies", "value");
            String dependency = getDependency(dependencies);

            if (!dependency.equalsIgnoreCase("forge") && !dependency.equals("*")) {
              modDependencies.add(dependency);

              LOG.debug("Added dependency " + dependency);
            }
          }
        }
      }
    } catch (NullPointerException ignored) {

    }
  }

  /**
   * Get the id of a dependency.
   *
   * @param dependency The full text of a dependency previously acquired from a JSON node.
   * @return The pure id of the dependency.
   * @author Griefed
   */
  private String getDependency(String dependency) {
    return dependency
        .substring(dependency.lastIndexOf(":") + 1)
        .replaceAll(DEPENDENCY_REPLACE_REGEX, "");
  }

  /**
   * Check for additional mods in the mod-jar. Sometimes, a single mod-jar can contain multiple mods
   * at once.
   *
   * @param modId          The id of the parent mod.
   * @param additionalMods A set of additional mod ids found so far, to which any additional mod
   *                       will be added to.
   * @param modJson        The JsonNode containing all relevant information about any additional
   *                       mods.
   * @param clientMods     A set of already discovered clientside-only mods, to which any additional
   *                       mod will be added to.
   * @author Griefed
   */
  private void checkAdditionalMods(String modId, TreeSet<String> additionalMods, JsonNode modJson,
      TreeSet<String> clientMods) {
    for (String additionalModId : additionalMods) {

      // base of json
      for (JsonNode node : modJson) {

        try {
          // iterate though annotations again but this time for the modID of the
          // second
          // mod
          for (JsonNode child : node.get("annotations")) {
            boolean additionalModDependsOnFirst = false;

            // check if second mod depends on first
            try {
              // if the modId is that of our additional mod, check the dependencies
              // whether the first modId is present
              if (UTILITIES.JsonUtilities()
                  .nestedTextEqualsIgnoreCase(child, additionalModId, "values", "modid", "value")) {
                if (!UTILITIES.JsonUtilities()
                    .nestedTextIsEmpty(child, "values", "dependencies", "value")) {

                  if (UTILITIES.JsonUtilities()
                      .nestedTextContains(child, ";", "values", "dependencies", "value")) {

                    if (additionalDepsDepend(child, modId)) {
                      additionalModDependsOnFirst = true;
                    }

                  } else {

                    if (additionalDepDepends(child, modId)) {
                      additionalModDependsOnFirst = true;
                    }
                  }
                }
              }

            } catch (NullPointerException ignored) {

            }

            // If the additional mod depends on the first one, check if the additional one
            // is clientside-only
            if (additionalModDependsOnFirst) {

              // if the additional mod is NOT clientside-only, we have to remove this mod
              // from the list of clientside-only mods
              if (!isAdditionalModClientSide(node, additionalModId)) {
                if (clientMods.removeIf(n -> n.equals(modId))) {
                  LOG.info(
                      "Removing "
                          + modId
                          + " from list of clientside-only mods. It contains multiple mods at once, and one of them is NOT clientside-only.");
                }
              }
            }
          }
        } catch (NullPointerException ignored) {

        }
      }
    }
  }

  /**
   * Check whether the passed mod id is present as a dependency in any of the mods dependencies. If
   * it is, then the mod of the modId is required.
   *
   * @param child The child-JSON node containing dependency information.
   * @param modId The ID of the mod for which to check for dependencies.
   * @return <code>true</code> if the modId is a dependency.
   * @author Griefed
   */
  private boolean additionalDepsDepend(JsonNode child, String modId) {
    boolean depends = false;
    String[] dependencies = UTILITIES.JsonUtilities()
        .getNestedTexts(child, ";", "values", "dependencies", "value");

    for (String dependency : dependencies) {

      if (dependency.matches(DEPENDENCY_CHECK_REGEX)) {

        dependency =
            dependency
                .substring(dependency.lastIndexOf(":") + 1)
                .replaceAll("(@.*|\\[.*)", "");

        if (dependency.equals(modId)) {
          depends = true;
        }
      }
    }

    return depends;
  }

  /**
   * Check whether the passed mod id is present as a dependency. If it is, then the mod of the modId
   * is required.
   *
   * @param child The child-JSON node containing dependency information.
   * @param modId The ID of the mod for which to check for dependencies.
   * @return <code>true</code> if the modId is a dependency.
   * @author Griefed
   */
  private boolean additionalDepDepends(JsonNode child, String modId) {
    boolean depends = false;
    if (UTILITIES.JsonUtilities()
        .nestedTextMatches(child, DEPENDENCY_CHECK_REGEX, "values", "dependencies", "value")) {

      String dependencies = UTILITIES.JsonUtilities()
          .getNestedText(child, "values", "dependencies", "value");

      String dependency = dependencies
          .substring(
              dependencies.lastIndexOf(":") + 1)
          .replaceAll("(@.*|\\[.*)", "");

      if (dependency.equals(modId)) {
        depends = true;
      }
    }
    return depends;
  }

  /**
   * Check whether the additional mod is a clientside-only mod.
   *
   * @param node            The JSON-node containing information about the additional mod.
   * @param additionalModId The ID of the additional mod
   * @return <code>true</code> if the additional mod is clientside-only.
   * @author Griefed
   */
  private boolean isAdditionalModClientSide(JsonNode node, String additionalModId) {
    boolean clientSide = false;

    try {
      // iterate though annotations
      for (JsonNode children : node.get("annotations")) {

        try {
          if (UTILITIES.JsonUtilities()
              .nestedTextEqualsIgnoreCase(children, additionalModId, "values", "modid", "value")
              && UTILITIES.JsonUtilities()
              .getNestedBoolean(children, "values", "clientSideOnly", "value")) {

            clientSide = true;
          }
        } catch (NullPointerException | JsonException ignored) {

        }
      }
    } catch (NullPointerException ignored) {

    }
    return clientSide;
  }

  @Override
  TreeSet<File> getModsDelta(Collection<File> filesInModsDir, TreeSet<String> clientMods) {
    TreeSet<File> modsDelta = new TreeSet<>();
    for (File mod : filesInModsDir) {

      try {

        if (addToDelta(mod, clientMods)) {
          modsDelta.add(mod);
        }
      } catch (Exception ignored) {

      }

    }
    return modsDelta;
  }

  /**
   * Check whether the mod-jar should be added to the modsDelta list.
   *
   * @param file       The mod-jar to check.
   * @param clientMods A set of modIds of clientside-only mods already discovered previously..
   * @return <code>true</code> if the modJar can be added to the modsDelta set.
   * @throws IOException if the fml_cache_annotation could not be read.
   * @author Griefed
   */
  private boolean addToDelta(File file, TreeSet<String> clientMods)
      throws IOException {

    JsonNode modJson = getJarJson(file, "META-INF/fml_cache_annotation.json", OBJECT_MAPPER);
    boolean addToDelta = false;

    for (JsonNode node : modJson) {

      try {
        // iterate though annotations
        for (JsonNode child : node.get("annotations")) {

          // Get the modId
          try {

            String modIdTocheck = getModId(child);

            // Add mod to list of clientmods if clientSideOnly is true
            if (modIdTocheck != null && UTILITIES.JsonUtilities()
                .getNestedBoolean(child, "values", "clientSideOnly",
                    "value")) {
              if (clientMods.contains(modIdTocheck)) {
                addToDelta = true;
              }
            }

          } catch (NullPointerException | JsonException ignored) {

          }
        }

      } catch (NullPointerException ignored) {

      }
    }

    return addToDelta;
  }
}
