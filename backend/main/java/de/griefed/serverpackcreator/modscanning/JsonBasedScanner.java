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
import java.util.Iterator;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
   * @author Griefed
   */
  JsonNode getJarJson(File file, String entryInJar, ObjectMapper objectMapper)
      throws IOException, SecurityException, IllegalStateException {

    JarFile jarFile = new JarFile(file);
    JarEntry jarEntry = jarFile.getJarEntry(entryInJar);
    InputStream inputStream = jarFile.getInputStream(jarEntry);

    JsonNode jsonNode = objectMapper.readTree(inputStream);

    jarFile.close();
    inputStream.close();

    return jsonNode;
  }

  /**
   * Check whether the text in specified child node(s) matches the specified text.
   *
   * @param jsonNode   The JsonNode from which to acquire the text.
   * @param matches    The text to check for a match.
   * @param childNodes The nested nodes in the JsonNode to acquire the text from, in order.
   * @return <code>true</code> if the texts match.
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  boolean nestedTextMatches(JsonNode jsonNode, String matches, String... childNodes)
      throws NullPointerException {
    return getNestedText(jsonNode, childNodes).matches(matches);
  }

  /**
   * Check whether the text in specified child node(s) is equal to the specified text,
   * case-insensitive.
   *
   * @param jsonNode         The JsonNode from which to acquire the text.
   * @param equalsIgnoreCase The text to check with whether it is equal, case-insensitive.
   * @param childNodes       The nested nodes in the JsonNode to acquire the text from, in order.
   * @return <code>true</code> if the texts are equal, case-insensitive.
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  boolean nestedTextEqualsIgnoreCase(JsonNode jsonNode, String equalsIgnoreCase,
      String... childNodes) throws NullPointerException {
    return getNestedText(jsonNode, childNodes).equalsIgnoreCase(equalsIgnoreCase);
  }

  /**
   * Check whether the text in the specified child node(s) is empty.
   *
   * @param jsonNode   The JsonNode from which to acquire the text.
   * @param childNodes The nested nodes in the JsonNode to acquire the text from, in order.
   * @return <code>true</code> if the text in the requested node is empty.
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  boolean nestedTextIsEmpty(JsonNode jsonNode, String... childNodes) throws NullPointerException {
    return getNestedText(jsonNode, childNodes).isEmpty();
  }

  /**
   * Get the boolean value from the nested child node(s).
   *
   * @param jsonNode   The JsonNode from which to acquire the boolean.
   * @param childNodes The nested nodes in the JsonNode to acquire the boolean from, in order.
   * @return <code>true</code> or <code>false</code>, based on the boolean in the specified child
   * node(s).
   * @throws ScanningException    if the specified node(s) contain no boolean or the requested value
   *                              is not a parsable boolean value.
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  boolean getNestedBoolean(JsonNode jsonNode, String... childNodes)
      throws NullPointerException, ScanningException {
    String bool = getNestedText(jsonNode, childNodes);

    if (bool.equalsIgnoreCase("true")) {
      return true;
    } else if (bool.equalsIgnoreCase("false")) {
      return false;
    } else {
      throw new ScanningException("Invalid boolean " + bool);
    }
  }

  /**
   * Get the array of texts from the nested child node(s) comma-separated text.
   *
   * @param jsonNode   The JsonNode from which to acquire the texts from.
   * @param split      The
   * @param childNodes The nested nodes in the JsonNode to acquire the texts from, in order.
   * @return An array of strings containing the texts from the specified node(s)
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  String[] getNestedTexts(JsonNode jsonNode, String split, String... childNodes)
      throws NullPointerException {
    return getNestedText(jsonNode, childNodes).split(split);
  }

  /**
   * Check whether a child node contains the specified text.
   *
   * @param jsonNode   The JsonNode from which to acquire the text for checks.
   * @param contains   The text to check with whether the node contains it.
   * @param childNodes The child node(s) from which to acquire the text for contain-checks from, in
   *                   order.
   * @return <code>true</code> if the child node contains the specified text.
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  boolean nestedTextContains(JsonNode jsonNode, String contains, String... childNodes)
      throws NullPointerException {
    return getNestedText(jsonNode, childNodes).contains(contains);
  }


  /**
   * Get the text from nested child node(s).
   *
   * @param jsonNode   The JsonNode from which to acquire the text from.
   * @param childNodes The child nodes which contain the requested text, in order.
   * @return The text from the requested child node(s).
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  String getNestedText(JsonNode jsonNode, String... childNodes) throws NullPointerException {
    return getNestedElement(jsonNode, childNodes).asText();
  }

  /**
   * Get a string iterator for the field names of the last specified child node in the specified
   * JsonNode.
   *
   * @param jsonNode   The JsonNode from which to get the string iterator.
   * @param childNodes The nested nodes from which to get the iterator, in order.
   * @return A string iterator for the field names in the last requested child node in the passed
   * JsonNode.
   * @throws NullPointerException if any of the specified child nodes can not be found in the passed
   *                              JsonNode.
   * @author Griefed
   */
  Iterator<String> getFieldNames(JsonNode jsonNode, String... childNodes)
      throws NullPointerException {
    return getNestedElement(jsonNode, childNodes).fieldNames();
  }

  /**
   * Get a nested element from a JsonNode.
   *
   * @param jsonNode   The JsonNode from which to acquire the nested element.
   * @param childNodes The nested elements, in order.
   * @return The nested element from the JsonNode.
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  JsonNode getNestedElement(JsonNode jsonNode, String... childNodes)
      throws NullPointerException {
    JsonNode child = jsonNode;
    for (String nested : childNodes) {
      child = child.get(nested);
    }
    return child;
  }

  /**
   * Remove any dependency from the list of clientside-only mods to prevent excluding dependencies
   * of other mods, resulting in a potentially broken server pack.
   *
   * @param modDependencies A set of modIds of dependencies.
   * @param clientMods      A set of modIds of clientside-only mods.
   * @author Griefed
   */
  void cleanupClientMods(TreeSet<String> modDependencies, TreeSet<String> clientMods) {
    for (String dependency : modDependencies) {

      clientMods.removeIf(mod -> {
        if (mod.contains(dependency)) {
          LOG.debug(
              "Removing "
                  + dependency
                  + " from list of clientmods as it is a dependency for " + mod + ".");
          return true;

        } else {
          return false;
        }
      });
    }
  }

  /**
   * Check every file and fill the <code>clientMods</code> and <code>modDependencies</code> sets
   * with ids of mods which are clientside-only or dependencies of a mod.
   *
   * @param filesInModsDir  Collection of files to check.
   * @param clientMods      Set of clientside-only mod-ids.
   * @param modDependencies Set dependencies of other mods.
   * @author Griefed
   */
  abstract void checkForClientModsAndDeps(Collection<File> filesInModsDir,
      TreeSet<String> clientMods, TreeSet<String> modDependencies);

  /**
   * Get a list of mod-jars which can safely be excluded from the server pack.
   *
   * @param filesInModsDir A collection of files from which to exclude mods.
   * @param clientMods     A set of modIds which are clientside-only.
   * @return A set of all files from the passed collection which can safely be excluded from the
   * server pack.
   * @author Griefed
   */
  abstract TreeSet<File> getModsDelta(Collection<File> filesInModsDir, TreeSet<String> clientMods);
}
