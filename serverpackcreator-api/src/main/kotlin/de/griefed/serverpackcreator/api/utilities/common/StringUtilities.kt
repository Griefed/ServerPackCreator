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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package de.griefed.serverpackcreator.api.utilities.common

import de.griefed.serverpackcreator.api.utilities.common.StringUtilities.Companion.pathSecureText


/**
 * Utility-class revolving around Strings.
 *
 * @author Griefed
 */
@Suppress("unused")
class StringUtilities {

    companion object {
        /**
         * Converts a list of Strings, for example from a list, into a concatenated String.
         *
         * @param strings List of strings that will be concatenated into one string
         * @return Returns concatenated string that contains all provided values.
         * @author Griefed
         */
        fun buildString(strings: List<String>) = buildString(strings.toString())

        /**
         * Converts a sequence of Strings, for example from a list, into a concatenated String.
         *
         * @param args Strings that will be concatenated into one string
         * @return Returns concatenated string that contains all provided values.
         * @author whitebear60
         * @author Griefed
         */
        fun buildString(vararg args: String) =
            StringBuilder(args.contentToString()).removeRange(0, 2).reversed().removeRange(0, 2).reversed().toString()

        /**
         * Remove commonly forbidden characters from the passed string, making the resulting String safe
         * to use for files, paths, directories etc. If the passed text ends with a
         * SPACE`(&#32;&#32;)` or a DOT`(&#32;.&#32;)`, they are also removed.
         *
         * Replaced/removed are:
         *  * **&#47;**
         *  * **&#60;**
         *  * **&#62;**
         *  * **&#58;**
         *  * **&#34;**
         *  * **&#92;**
         *  * **&#124;**
         *  * **&#63;**
         *  * **&#42;**
         *  * **&#35;**
         *  * **&#37;**
         *  * **&#38;**
         *  * **&#123;**
         *  * **&#125;**
         *  * **&#36;**
         *  * **&#33;**
         *  * **&#39;**
         *  * **&#64;**
         *  * **&#43;**
         *  * **&#180;**
         *  * **&#96;**
         *  * **&#61;**
         *  * **&#91;**
         *  * **&#93;**
         *
         * @param text The text which you want to be made safe.
         * @return The passed String safe for use for files, paths, directories etc.
         * @author Griefed
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun pathSecureText(text: String): String {
            var secured = text
            while (secured.endsWith(".") || secured.endsWith(" ")) {
                val toReplace = secured.substring(secured.length - 1)
                secured = secured.replace(toReplace, "")
            }
            return secured
                .replace("/", "")
                .replace("<", "")
                .replace(">", "")
                .replace(":", "")
                .replace("\"", "")
                .replace("\\", "")
                .replace("|", "")
                .replace("?", "")
                .replace("*", "")
                .replace("#", "")
                .replace("%", "")
                .replace("&", "")
                .replace("{", "")
                .replace("}", "")
                .replace("$", "")
                .replace("!", "")
                .replace("'", "")
                .replace("@", "")
                .replace("+", "")
                .replace("´", "")
                .replace("`", "")
                .replace("=", "")
                .replace("[", "")
                .replace("]", "")
        }

        /**
         * Remove commonly forbidden characters from the passed string, making the resulting String safe
         * to use for files, paths, directories etc. If the passed text ends with a
         * SPACE`(&#32;&#32;)` or a DOT`(&#32;.&#32;)`, they are also removed.
         *
         * Contrary to [pathSecureText], this method does **NOT** remove **&#47;** or **&#92;**.
         *
         *
         * Replaced/removed are:
         *  * **&#60;**
         *  * **&#62;**
         *  * **&#58;**
         *  * **&#34;**
         *  * **&#124;**
         *  * **&#63;**
         *  * **&#42;**
         *  * **&#35;**
         *  * **&#37;**
         *  * **&#38;**
         *  * **&#123;**
         *  * **&#125;**
         *  * **&#36;**
         *  * **&#33;**
         *  * **&#64;**
         *  * **&#43;**
         *  * **&#180;**
         *  * **&#96;**
         *  * **&#61;**
         *  * **&#91;**
         *  * **&#93;**
         *
         * @param text The text which you want to be made safe.
         * @return The passed String safe for use for files, paths, directories etc.
         * @author Griefed
         */
        fun pathSecureTextAlternative(text: String): String {
            var secured = text
            while (secured.endsWith(".") || secured.endsWith(" ")) {
                val toReplace = secured.substring(secured.length - 1)
                secured = secured.replace(toReplace, "")
            }
            return secured
                .replace("<", "")
                .replace(">", "")
                .replace(":", "")
                .replace("\"", "")
                .replace("|", "")
                .replace("?", "")
                .replace("*", "")
                .replace("#", "")
                .replace("%", "")
                .replace("&", "")
                .replace("{", "")
                .replace("}", "")
                .replace("$", "")
                .replace("!", "")
                .replace("@", "")
                .replace("+", "")
                .replace("´", "")
                .replace("`", "")
                .replace("=", "")
                .replace("[", "")
                .replace("]", "")
        }

        /**
         * Check the passed string whether it contains any of the following characters:
         *  * **&#47;**
         *  * **&#60;**
         *  * **&#62;**
         *  * **&#58;**
         *  * **&#34;**
         *  * **&#92;**
         *  * **&#124;**
         *  * **&#63;**
         *  * **&#42;**
         *  * **&#35;**
         *  * **&#37;**
         *  * **&#38;**
         *  * **&#123;**
         *  * **&#125;**
         *  * **&#36;**
         *  * **&#33;**
         *  * **&#64;**
         *  * **&#43;**
         *  * **&#180;**
         *  * **&#96;**
         *  * **&#61;**
         *
         * @param text The text you want to check.
         * @return `true` if none of these characters were found.
         * @author Griefed
         */
        fun checkForIllegalCharacters(text: String) =
            (!text.contains("/")
                    && !text.contains("<")
                    && !text.contains(">")
                    && !text.contains(":")
                    && !text.contains("\"")
                    && !text.contains("\\")
                    && !text.contains("|")
                    && !text.contains("?")
                    && !text.contains("*")
                    && !text.contains("#")
                    && !text.contains("%")
                    && !text.contains("&")
                    && !text.contains("{")
                    && !text.contains("}")
                    && !text.contains("$")
                    && !text.contains("!")
                    && !text.contains("@")
                    && !text.contains("+")
                    && !text.contains("`")
                    && !text.contains("´")
                    && !text.contains("="))

        /**
         * Check the passed string whether it contains characters invalid in a path-declaration:
         *  * **&#60;**
         *  * **&#62;**
         *  * **&#58;**
         *  * **&#34;**
         *  * **&#124;**
         *  * **&#63;**
         *  * **&#42;**
         *  * **&#35;**
         *  * **&#37;**
         *  * **&#38;**
         *  * **&#123;**
         *  * **&#125;**
         *  * **&#36;**
         *  * **&#33;**
         *  * **&#64;**
         *  * **&#180;**
         *  * **&#96;**
         *  * **&#61;**
         *
         * @param text The text you want to check.
         * @return `true` if none of these characters were found.
         * @author Griefed
         */
        fun checkForInvalidPathCharacters(text: String) =
            (!text.contains("<")
                    || !text.contains(">")
                    || !text.contains(":")
                    || !text.contains("\"")
                    || !text.contains("|")
                    || !text.contains("?")
                    || !text.contains("*")
                    || !text.contains("#")
                    || !text.contains("%")
                    || !text.contains("&")
                    || !text.contains("{")
                    || !text.contains("}")
                    || !text.contains("$")
                    || !text.contains("!")
                    || !text.contains("@")
                    || !text.contains("`")
                    || !text.contains("´")
                    || !text.contains("="))

        /**
         * Replace '$', ':', '/', '?', '#', '[', ']', '@' with percent-encoded characters, according to RFC3986.
         *
         * @author Griefed
         */
        fun percentEncode(input: String): String {
            val encoded = StringBuilder()
            for (char in input) {
                when (char) {
                    '$', ':', '/', '?', '#', '[', ']', '@' -> {
                        encoded.append("%${String.format("%02x", char.code)}")
                    }
                    else -> {
                        encoded.append(char)
                    }
                }
            }
            return encoded.toString()
        }

        fun createMongoUri(user: String, password: String, host: String, port: Int, database: String) =
            "mongodb://${percentEncode(user)}" +
                    ":${percentEncode(password)}" +
                    "@${host}" +
                    ":${port}" +
                    "/${database}"
    }
}

/**
 * Properly escape certain characters, so it can be safely used inside text-files, variables and
 * scripts.
 *
 * @author Griefed
 */
fun String.escapePath(): String {
    var escaped = false
    var escapedPath = this
    while (!escaped) {
        var matchedColon = false
        var matchedBackslash = false
        for (i in escapedPath.indices) {
            when (escapedPath[i]) {
                ':' -> {
                    if (escapedPath[i - 1] != '\\') {
                        escapedPath = escapedPath.insertCharacter(i, '\\')
                        matchedColon = true
                    }

                }

                '\\' -> {
                    if (i - 1 > 0 && escapedPath[i - 1] != '\\'
                        && i + 1 <= escapedPath.length && escapedPath[i + 1] != '\\'
                        && escapedPath[i + 1] != ':'
                    ) {
                        escapedPath = escapedPath.insertCharacter(i, '\\')
                        matchedBackslash = true
                    }
                }
            }
        }
        if (!matchedColon && !matchedBackslash) {
            escaped = true
        }
    }
    return escapedPath
}

/**
 * Replace all matches for the given [regex] with [replaceWith] and return the resulting string.
 *
 * @author Griefed
 */
fun String.regexReplace(regex: Regex, replaceWith: String): String {
    var i = 0
    var replaced = this
    while (i < length) {
        for (n in length downTo i) {
            if (substring(i, n).matches(regex)) {
                replaced = replaceRange(i, n, replaceWith)
                i = n
                break
            }
        }
        i++
    }
    return replaced
}

/**
 * Replace the character at the given [index] with the given new [character].
 * @author Griefed
 */
fun String.replaceCharacter(index: Int, character: Char): String {
    val characters = this.toCharArray()
    characters[index] = character
    return String(characters)
}

/**
 * Insert the given [character] at the given [index].
 * @author Griefed
 */
fun String.insertCharacter(index: Int, character: Char) =
    StringBuilder(this).apply {
        insert(index, character)
    }.toString()