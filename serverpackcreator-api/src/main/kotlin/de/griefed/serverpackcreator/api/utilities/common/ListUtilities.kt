/* Copyright (C) 2025 Griefed
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
@file:Suppress("DuplicatedCode", "unused")

package de.griefed.serverpackcreator.api.utilities.common

import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import kotlin.coroutines.CoroutineContext

/**
 * Utility-class revolving around Lists.
 *
 * @author Griefed
 */
@Suppress("unused")
class ListUtilities {

    companion object {
        private val log by lazy { cachedLoggerOf(ListUtilities::class.java) }
        private val whitespace = "\\s+".toRegex()

        /**
         * Encapsulate every element of the passed String List in quotes. Returns the list as
         * `["element1","element2","element3"` etc.
         *
         * @param listToEncapsulate The String List of which to encapsulate every element in.
         * @return Returns a concatenated String with all elements of the passed list encapsulated.
         * @author Griefed
         */
        fun encapsulateListElements(listToEncapsulate: List<String>): String {
            if (listToEncapsulate.isEmpty()) {
                return "[]"
            }
            val stringBuilder = StringBuilder()
            var encapsulated = listToEncapsulate[0].replace("\\", "/")
            stringBuilder.append("[\"").append(encapsulated).append("\"")
            for (i in 1 until listToEncapsulate.size) {
                encapsulated = listToEncapsulate[i].replace("\\", "/")
                stringBuilder.append(",\"").append(encapsulated).append("\"")
            }
            stringBuilder.append("]")
            return stringBuilder.toString()
        }

        /**
         * Prompts the user to enter the values which will make up a String List in the new configuration
         * file. If the user enters an empty line, the method is exited and the String List returned.
         *
         * @return Returns the list of values entered by the user.
         * @author whitebear60
         */
        fun readStringList(): List<String> {
            val result = ArrayList<String>(100)
            var stringArray: String
            while (true) {
                stringArray = readln()
                if (stringArray.isEmpty()) {
                    break
                } else {
                    result.add(stringArray)
                }
            }
            return result
        }

        /**
         * Clean a given String List of any entry consisting only of whitespace or a length of
         * `0 `.
         *
         * @param listToCleanUp The list from which to delete all entries consisting only of whitespace or
         * with a length of zero.
         * @return Returns the cleaned up list.
         * @author Griefed
         */
        fun cleanList(listToCleanUp: MutableList<String>): MutableList<String> {
            listToCleanUp.removeIf { entry: String -> entry.matches(whitespace) || entry.isEmpty() }
            return listToCleanUp
        }

        /**
         * Print a list to console in chunks. If a chunk size of 5 is set for a list with 20 entries, the
         * result would be 4 lines printed, with 5 entries each.
         *
         * @param list         The list to print to the console.
         * @param chunkSize    The chunk size to print the list with.
         * @param prefix       A prefix to add to each line printed to the console.
         * @param printIndexes Whether to print the indexes of the entries.
         * @author Griefed
         */
        fun printListToConsoleChunked(
            list: List<String>,
            chunkSize: Int,
            prefix: String,
            printIndexes: Boolean
        ) {
            val text = StringBuilder()
            var i = 0
            while (i < list.size) {
                text.clear()
                val m = i + chunkSize
                var n: Int = i
                while (n < m) {
                    if (n >= list.size) {
                        break
                    } else if (n == i) {
                        text.append(list[n])
                    } else {
                        text.append(", ").append(list[n])
                    }
                    n++
                }
                if (printIndexes) {
                    val from = i + 1
                    println("$prefix($from to $n) $text")
                } else {
                    println("$prefix$text")
                }
                i = n - 1
                i++
            }
        }

        /**
         * Print a list to our log at info level, in chunks. If a chunk size of 5 is set for a list with
         * 20 entries, the result would be 4 lines printed, with 5 entries each.
         *
         * @param list         The list to print to the console.
         * @param chunkSize    The chunk size to print the list with.
         * @param prefix       A prefix to add to each line printed to the console.
         * @param printIndexes Whether to print the indexes of the entries.
         * @author Griefed
         */
        fun printListToLogChunked(
            list: List<String>,
            chunkSize: Int,
            prefix: String,
            printIndexes: Boolean
        ) {
            val text = StringBuilder()
            var i = 0
            while (i < list.size) {
                text.clear()
                val m = i + chunkSize
                var n: Int = i
                while (n < m) {
                    if (n >= list.size) {
                        break
                    } else if (n == i) {
                        text.append(list[n])
                    } else {
                        text.append(", ").append(list[n])
                    }
                    n++
                }
                if (printIndexes) {
                    val from = i + 1
                    log.info { "$prefix($from to $n) $text" }
                } else {
                    log.info { "$prefix$text" }
                }
                i = n - 1
                i++
            }
        }
    }
}

/**
 * Check whether any entry in this list ends with the given string.
 * @param string The text to check entries with.
 * @author Griefed
 */
fun List<String>.endsWith(string: String): Boolean {
    var found = false
    for (entry in this) {
        if (string.endsWith(entry)) {
            found = true
        }
    }
    return found
}

/**
 * Check whether any entry in this list starts with the given string.
 * @param string The text to check entries with.
 * @author Griefed
 */
fun List<String>.startsWith(string: String): Boolean {
    var found = false
    for (entry in this) {
        if (string.startsWith(entry)) {
            found = true
        }
    }
    return found
}

/**
 * Compute all elements in the list in parallel and continue when every element was computed.
 *
 * @author Griefed
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
inline fun <A, B> List<A>.parallelMap(
    context: CoroutineContext = newSingleThreadContext("parallelMap"),
    crossinline function: suspend (A) -> B
): List<B> = runBlocking(context) {
    map { return@map this.async { function(it) } }.awaitAll()
}

/**
 * Add multiple elements to a list in one go.
 *
 * @author Griefed
 */
fun <T> MutableList<T>.addMultiple(vararg entries: T) {
    entries.forEach { add(it) }
}

fun <T> concatenate(vararg lists: List<T>): List<T> {
    return listOf(*lists).flatten()
}