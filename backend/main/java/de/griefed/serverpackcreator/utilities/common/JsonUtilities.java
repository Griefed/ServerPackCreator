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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import org.springframework.stereotype.Component;

@Component
public final class JsonUtilities {

  public JsonUtilities() {

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
  public boolean nestedTextMatches(JsonNode jsonNode, String matches, String... childNodes)
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
  public boolean nestedTextEqualsIgnoreCase(JsonNode jsonNode, String equalsIgnoreCase,
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
  public boolean nestedTextIsEmpty(JsonNode jsonNode, String... childNodes)
      throws NullPointerException {
    return getNestedText(jsonNode, childNodes).isEmpty();
  }

  /**
   * Get the boolean value from the nested child node(s).
   *
   * @param jsonNode   The JsonNode from which to acquire the boolean.
   * @param childNodes The nested nodes in the JsonNode to acquire the boolean from, in order.
   * @return <code>true</code> or <code>false</code>, based on the boolean in the specified child
   * node(s).
   * @throws JsonException        if the specified node(s) contain no boolean or the requested value
   *                              is not a parsable boolean value.
   * @throws NullPointerException if the requested element is not present in the JsonNode.
   * @author Griefed
   */
  public boolean getNestedBoolean(JsonNode jsonNode, String... childNodes)
      throws NullPointerException, JsonException {
    String bool = getNestedText(jsonNode, childNodes);

    /*
     * We do not use getNestedElement(...).asBoolean() because we need to throw if the value does
     * not exist or if it is otherwise not a valid boolean.
     */
    if (bool.equalsIgnoreCase("true")) {
      return true;
    } else if (bool.equalsIgnoreCase("false")) {
      return false;
    } else {
      throw new JsonException("Invalid boolean " + bool);
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
  public String[] getNestedTexts(JsonNode jsonNode, String split, String... childNodes)
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
  public boolean nestedTextContains(JsonNode jsonNode, String contains, String... childNodes)
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
  public String getNestedText(JsonNode jsonNode, String... childNodes) throws NullPointerException {
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
  public Iterator<String> getFieldNames(JsonNode jsonNode, String... childNodes)
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
  public JsonNode getNestedElement(JsonNode jsonNode, String... childNodes)
      throws NullPointerException {
    JsonNode child = jsonNode;
    for (String nested : childNodes) {
      child = child.get(nested);
    }
    return child;
  }

}
