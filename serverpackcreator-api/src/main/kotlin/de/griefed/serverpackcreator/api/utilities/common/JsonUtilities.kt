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
package de.griefed.serverpackcreator.api.utilities.common

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

/**
 * JSON-based utility methods for acquiring and working with JSON from files and objects.
 *
 * @author Griefed
 */
@Suppress("unused")
class JsonUtilities(
    /** Used for JSON-parsing. */
    val objectMapper: ObjectMapper = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
) {

    /**
     * Whether the text in [jsonNode]'s nested [childNodes], read in order, matches [matches].
     * @throws NullPointerException if the requested element is not present in the JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    fun nestedTextMatches(jsonNode: JsonNode, matches: Regex, vararg childNodes: String) =
        getNestedText(jsonNode, *childNodes).matches(matches)

    /**
     * The text from [jsonNode]'s nested [childNodes], read in order.
     * @throws NullPointerException if the requested element is not present in the JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    fun getNestedText(jsonNode: JsonNode, vararg childNodes: String): String =
        getNestedElement(jsonNode, *childNodes).asText()

    /**
     * Get a nested element from a JsonNode.
     *
     * @throws NullPointerException if the requested element is not present in the JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    fun getNestedElement(
        /** The JsonNode from which to acquire the nested element. */
        jsonNode: JsonNode,
        /** The nested elements, in order. */
        vararg childNodes: String
    ): JsonNode {
        var child: JsonNode = jsonNode
        for (nested in childNodes) {
            child = child.get(nested)
        }
        return child
    }

    /**
     * Whether the text in [jsonNode]'s nested [childNodes], read in order, equals [equalsIgnoreCase],
     * ignoring case.
     * @throws NullPointerException if the requested element is not present in the JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    fun nestedTextEqualsIgnoreCase(jsonNode: JsonNode, equalsIgnoreCase: String, vararg childNodes: String) =
        getNestedText(jsonNode, *childNodes).equals(equalsIgnoreCase, ignoreCase = true)

    /**
     * Whether the text in [jsonNode]'s nested [childNodes], read in order, is empty.
     * @throws NullPointerException if the requested element is not present in the JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    fun nestedTextIsEmpty(jsonNode: JsonNode, vararg childNodes: String) =
        getNestedText(jsonNode, *childNodes).isEmpty()

    /**
     * The boolean value from the nested child node(s).
     * @throws JsonException        if the specified node(s) contain no boolean or the requested value
     * is not a parsable boolean value.
     * @throws NullPointerException if the requested element is not present in the JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class, JsonException::class)
    fun getNestedBoolean(
        /** The JsonNode from which to acquire the boolean. */
        jsonNode: JsonNode,
        /** The nested nodes in the JsonNode to acquire the boolean from, in order. */
        vararg childNodes: String
    ): Boolean {
        val bool = getNestedText(jsonNode, *childNodes)

        /*
         * We do not use getNestedElement(...).asBoolean() because we need to throw if the value does
         * not exist or if it is otherwise not a valid boolean.
         */
        return when {
            bool.equals("true", ignoreCase = true) -> true
            bool.equals("false", ignoreCase = true) -> false
            // Deliberately not toBooleanStrictOrNull(): that is case-sensitive, and JSON in the wild
            // carries "True"/"FALSE".
            else -> throw JsonException("Invalid boolean $bool")
        }
    }

    /**
     * The texts from [jsonNode]'s nested [childNodes], read in order, with the last node's text split
     * into an array on [split].
     * @throws NullPointerException if the requested element is not present in the JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    fun getNestedTexts(jsonNode: JsonNode, split: String, vararg childNodes: String) =
        getNestedText(jsonNode, *childNodes).split(split.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    /**
     * Whether the text in [jsonNode]'s nested [childNodes], read in order, contains [contains].
     * @throws NullPointerException if the requested element is not present in the JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    fun nestedTextContains(jsonNode: JsonNode, contains: String, vararg childNodes: String) =
        getNestedText(jsonNode, *childNodes).contains(contains)

    /**
     * A string iterator over the field names of the last of [jsonNode]'s nested [childNodes], read in
     * order.
     * @throws NullPointerException if any of the specified child nodes can not be found in the provided
     * JsonNode.
     * @author Griefed
     */
    @Throws(NullPointerException::class)
    fun getFieldNames(jsonNode: JsonNode, vararg childNodes: String): MutableIterator<String> =
        getNestedElement(jsonNode, *childNodes).fieldNames()

    /**
     * The [JsonNode] parsed from [inputStream].
     * @throws IOException when the file could not be parsed/read into a [JsonNode].
     * @author Griefed
     */
    @Throws(IOException::class)
    fun getJson(inputStream: InputStream) = getJson(inputStream.readText())

    /**
     * The [JsonNode] parsed from [string].
     * @throws IOException when the file could not be parsed/read into a [JsonNode].
     * @author Griefed
     */
    @Throws(IOException::class)
    fun getJson(string: String): JsonNode = objectMapper.readTree(string)

    /**
     * The [JsonNode] parsed from [file].
     * @throws IOException when the file could not be parsed/read into a [JsonNode].
     * @author Griefed
     */
    @Throws(IOException::class)
    fun getJson(file: File): JsonNode = getJson(file.readText(Charsets.UTF_8))

    /**
     * The [JsonNode] parsed from the data at [url].
     * @throws IOException when the file could not be parsed/read into a [JsonNode].
     * @author Griefed
     */
    @Throws(IOException::class)
    fun getJson(url: URL): JsonNode = objectMapper.readTree(url)
}