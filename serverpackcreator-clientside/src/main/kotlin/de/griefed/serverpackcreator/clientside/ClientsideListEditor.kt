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
package de.griefed.serverpackcreator.clientside

/**
 * Inserts approved clientside-list entries into the two files that ship the official fallback-list:
 * the `fallbackMods` `listOf(...)` block in `GenerationConfig.kt` and the backslash-continued
 * `fallbackmodslist` value in `serverpackcreator.properties`. Pure string transforms (no I/O) so the
 * insertion is unit-tested; entries land in case-insensitive sorted position, already-present ones
 * are skipped, and the diff stays minimal (only inserted lines, plus a delimiter on the previous
 * last line when appending at the end).
 *
 * @author Griefed
 */
object ClientsideListEditor {

    /** A list-entry to add: the file-name stem and the project-link that documents it. */
    data class Entry(
        /** The file-name stem itself — what SPC matches a jar against with `startsWith`. */
        val value: String,
        /** The project link written as the aligned trailing `//` comment, or `null` to write none. */
        val comment: String?
    )

    /** Column the trailing `//link` comment is aligned to in `GenerationConfig.kt`. */
    private const val COMMENT_COLUMN = 44

    /** An entry-line in the Kotlin `listOf` block: indent, quoted value, optional comma, optional comment. */
    private val kotlinEntryLine = Regex("""^(\s*)"([^"]*)"\s*,?\s*(//.*)?$""")

    /** Case-insensitive-then-case-sensitive ordering, matching how the lists are kept sorted. */
    private val entryOrder = compareBy<String>({ it.lowercase() }, { it })

    /**
     * Insert [entries] into the `fallbackMods` block of [content] (the text of `GenerationConfig.kt`),
     * returning the updated text. Entries already present are skipped; if nothing changes the input
     * is returned unchanged.
     */
    fun addToKotlin(content: String, entries: List<Entry>): String {
        val lines = content.split("\n").toMutableList()
        val declaration = lines.indexOfFirst { it.contains("fallbackMods = TreeSet(") }
        if (declaration < 0) {
            return content
        }
        var listOfLine = declaration
        while (listOfLine < lines.size && !lines[listOfLine].contains("listOf(")) {
            listOfLine++
        }
        val first = listOfLine + 1
        var last = first
        while (last < lines.size && kotlinEntryLine.matches(lines[last])) {
            last++
        }
        if (last == first) {
            return content
        }
        // lines[first] is guaranteed to match (the loop above only advances over matching lines), but
        // resolve the indent defensively rather than asserting non-null.
        val indent = kotlinEntryLine.find(lines[first])?.groupValues?.get(1) ?: "            "
        val block = lines.subList(first, last)
        insertSorted(block, entries, ::valueOfKotlin) { entry -> renderKotlin(indent, entry) }.also {
            ensureTrailingDelimiter(block, ::hasKotlinComma, ::addKotlinComma)
        }
        return lines.joinToString("\n")
    }

    /**
     * Insert [entries] into the `fallbackmodslist` value of [content] (the text of
     * `serverpackcreator.properties`), returning the updated text. Entries already present are
     * skipped; if nothing changes the input is returned unchanged.
     */
    fun addToProperties(content: String, entries: List<Entry>): String {
        val lines = content.split("\n").toMutableList()
        val keyLine = lines.indexOfFirst { it.startsWith("de.griefed.serverpackcreator.configuration.fallbackmodslist=") }
        if (keyLine < 0) {
            return content
        }
        val first = keyLine + 1
        var last = first
        // Continuation runs while the *previous* physical line ends with a backslash.
        while (last < lines.size && lines[last - 1].trimEnd().endsWith("\\")) {
            last++
        }
        if (last <= first) {
            return content
        }
        val block = lines.subList(first, last)
        insertSorted(block, entries, ::valueOfProperty) { entry -> "    ${entry.value},\\" }
        ensureTrailingDelimiter(block, { it.trimEnd().endsWith("\\") }, { "${it.trimEnd()},\\" })
        // The final entry must NOT carry a `,\` continuation, or it would bleed into the next property.
        val lastIndex = block.size - 1
        block[lastIndex] = block[lastIndex].trimEnd().removeSuffix("\\").trimEnd().removeSuffix(",")
        return lines.joinToString("\n")
    }

    /**
     * Merge [entries] into the mutable [block] of raw entry-lines in sorted position, skipping values
     * already present. [valueOf] extracts the bare value of an existing line; [render] formats a new
     * line. New lines always carry a trailing delimiter; the previous last line is fixed separately.
     */
    private fun insertSorted(
        block: MutableList<String>,
        entries: List<Entry>,
        valueOf: (String) -> String,
        render: (Entry) -> String
    ) {
        for (entry in entries.distinctBy { it.value }) {
            if (block.any { valueOf(it).equals(entry.value, ignoreCase = false) }) {
                continue
            }
            val insertAt = block.indexOfFirst { entryOrder.compare(valueOf(it), entry.value) > 0 }
                .let { if (it < 0) block.size else it }
            block.add(insertAt, render(entry))
        }
    }

    /**
     * Ensure every line of [block] except the last carries the trailing delimiter, so an entry
     * appended at the very end does not leave the formerly-last line undelimited. Only the previous
     * last line is ever touched.
     */
    private fun ensureTrailingDelimiter(block: MutableList<String>, hasDelimiter: (String) -> Boolean, addDelimiter: (String) -> String) {
        for (index in 0 until block.size - 1) {
            if (!hasDelimiter(block[index])) {
                block[index] = addDelimiter(block[index])
            }
        }
    }

    /** Bare value of a Kotlin entry-line (the text between the quotes). */
    private fun valueOfKotlin(line: String): String = kotlinEntryLine.find(line)?.groupValues?.get(2) ?: ""

    /** Bare value of a properties entry-line (trim, drop trailing backslash then comma). */
    private fun valueOfProperty(line: String): String = line.trim().removeSuffix("\\").trim().removeSuffix(",")

    /** Whether a Kotlin entry-line already has its comma (right after the closing quote). */
    private fun hasKotlinComma(line: String): Boolean = Regex("""^\s*"[^"]*"\s*,""").containsMatchIn(line)

    /** Add the missing comma to a Kotlin entry-line, right after the closing quote. */
    private fun addKotlinComma(line: String): String = line.replaceFirst(Regex("""("[^"]*")(\s*)(//.*)?$"""), "$1,$2$3")

    /** Render a new Kotlin entry-line with aligned `//link` comment. */
    private fun renderKotlin(indent: String, entry: Entry): String {
        val prefix = "$indent\"${entry.value}\","
        if (entry.comment.isNullOrBlank()) {
            return prefix
        }
        val padding = if (prefix.length < COMMENT_COLUMN) " ".repeat(COMMENT_COLUMN - prefix.length) else " "
        return "$prefix$padding//${entry.comment}"
    }
}
