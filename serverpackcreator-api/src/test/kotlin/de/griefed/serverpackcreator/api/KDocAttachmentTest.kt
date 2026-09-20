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
package de.griefed.serverpackcreator.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Pins that no KDoc block in the repository has come loose from its declaration.
 *
 * A doc block whose next non-blank line opens *another* doc block attaches to nothing: dokka drops it,
 * and the declaration it was written for reads as undocumented. It happens whenever a new doc is written
 * above an existing one instead of replacing it, or a declaration is inserted between a doc and its
 * target -- both of which are invisible in review, because the diff shows only added lines.
 *
 * Eighteen instances had accrued by 2026-09-20, among them the rationale for `BootLogClassifier`'s
 * largest single failure class and the entire case for `ClientsideVerifier.propagateClientOnlyProof`.
 * Qodana reports this only when a stranded block happens to contain a `[link]` that no longer resolves,
 * which was **4 of the 18** -- so the tool that found the defect cannot be the thing that guards it.
 *
 * Lives in `-api`, and scans every module rather than its own, because the rule is a repository-wide
 * documentation convention rather than a fact about this module. A test source set creates no compile
 * dependency, so nothing about the module graph is weakened by reading another module's files.
 */
internal class KDocAttachmentTest {

    /** Directories that hold no hand-written source, or none this convention governs. */
    private val ignoredDirectories = setOf("build", ".git", ".gradle", "node_modules", "dist", ".idea")

    /**
     * The repository root, found by walking up until `settings.gradle.kts` appears.
     *
     * Gradle runs a test with the module directory as its working directory, but that is a default a
     * build script may change, so the root is located rather than assumed to be `..`.
     */
    private fun repositoryRoot(): File {
        var candidate: File? = File(".").absoluteFile.normalize()
        while (candidate != null && !File(candidate, "settings.gradle.kts").isFile) {
            candidate = candidate.parentFile
        }
        Assertions.assertNotNull(candidate, "no settings.gradle.kts above ${File(".").absolutePath}")
        return candidate!!
    }

    /**
     * Every orphaned block in [file], reported as `path:line` pointing at the block that could not
     * attach. Only doc-comment openers are tracked, so plain block comments and line comments are left
     * alone, and a block closed on its own opening line is handled.
     *
     * Note for anyone editing the doc comments in this file: Kotlin **nests** block comments, so writing
     * a comment opener inside one silently swallows the rest of the file. Say it in words instead.
     */
    private fun orphansIn(root: File, file: File): List<String> {
        val lines = file.readText().split('\n')
        val found = mutableListOf<String>()
        var insideDoc = false
        var blockStart = 0
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (!insideDoc && trimmed.startsWith("/**")) {
                blockStart = index
                if (!trimmed.removePrefix("/**").contains("*/")) {
                    insideDoc = true
                    continue
                }
            } else if (insideDoc && trimmed.endsWith("*/")) {
                insideDoc = false
            } else {
                continue
            }
            var next = index + 1
            while (next < lines.size && lines[next].isBlank()) {
                next++
            }
            if (next < lines.size && lines[next].trim().startsWith("/**")) {
                found.add("${file.relativeTo(root).path}:${blockStart + 1}")
            }
        }
        return found
    }

    @Test
    fun noKDocBlockIsOrphanedFromItsDeclaration() {
        val root = repositoryRoot()
        val sources = root.walkTopDown()
            .onEnter { it.name !in ignoredDirectories }
            .filter { it.isFile && (it.extension == "kt" || it.extension == "kts") }
            .toList()

        Assertions.assertTrue(
            sources.size > 100,
            "only ${sources.size} Kotlin files found under $root -- the scan is not reaching the source tree"
        )

        val orphans = sources.flatMap { orphansIn(root, it) }.sorted()

        Assertions.assertTrue(
            orphans.isEmpty(),
            "these KDoc blocks are followed by another KDoc block, so they attach to no declaration and " +
                "dokka drops them -- move each one onto the declaration it was written for, or delete it " +
                "if the block below already says what it said:\n" + orphans.joinToString("\n")
        )
    }
}
