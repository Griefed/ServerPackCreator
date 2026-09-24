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
package de.griefed.serverpackcreator.app.cli

import java.io.File
import java.io.InputStream
import java.util.Scanner

/**
 * Asks a question on the console and keeps asking until the answer is usable.
 *
 * Every prompting command used to carry its own copy of this loop, and the copies drifted: one asked
 * for a home-directory while validating a config file, and one listed the locales it would then
 * refuse. Taking [input] and [output] as parameters rather than reaching for `System.in`/`System.out`
 * is what lets the loop be driven by a test instead of a terminal.
 *
 * @author Griefed
 */
class ConsolePrompt(
    private val input: InputStream = System.`in`,
    private val output: Appendable = System.out
) {

    /**
     * The one Scanner in the application, created on first use so a command that never prompts never
     * makes one.
     *
     * LANDMINE - do not close it. `Scanner.close()` closes its source, `System.in` cannot be
     * reopened, and JLine's POSIX terminal holds that same descriptor as its pty slave, so the shell
     * that called a prompt dies on its next readLine with `ioctl(TIOCGWINSZ) = -1`. The Error that
     * raises is not an Exception, so the shell's per-line handler misses it and the session ends.
     */
    private val scanner by lazy { Scanner(input) }

    /**
     * Ask [question], then read whole lines after [prompt] until [rejection] accepts one.
     *
     * [rejection] returns the complaint to print for an unusable answer, or `null` to accept it, so
     * that what is rejected and what is said about it stay in one place.
     */
    fun readUntil(question: String, prompt: String, rejection: (String) -> String?): String {
        say(question)
        while (true) {
            output.append(prompt)
            val answer = scanner.nextLine()
            val complaint = rejection(answer) ?: return answer
            say(complaint)
        }
    }

    /**
     * Ask [question] until the answer names a directory that exists, and return it.
     */
    fun readExistingDirectory(question: String): File = File(
        readUntil(question, PATH_PROMPT) { answer ->
            if (File(answer).isDirectory) null else "Directory '$answer' does not exist."
        }
    )

    /**
     * Ask [question] until the answer names a file that exists, and return it.
     */
    fun readExistingFile(question: String): File = File(
        readUntil(question, PATH_PROMPT) { answer ->
            if (File(answer).isFile) null else "File '$answer' does not exist."
        }
    )

    /**
     * List [choices] by the form they are displayed under, ask [question], and return the value behind
     * whichever the user picks.
     *
     * Displaying and matching read the same map, which is what makes "offers a choice it will not
     * accept" unrepresentable rather than merely fixed.
     */
    fun <T> readChoice(question: String, prompt: String, choices: Map<String, T>): T {
        for (displayed in choices.keys) {
            say(displayed)
        }
        val chosen = readUntil(question, prompt) { answer ->
            if (choices.containsKey(answer)) null else "Unsupported choice $answer."
        }
        return choices.getValue(chosen)
    }

    /** Write [line] followed by a line separator, which is every message except the inline prompt. */
    private fun say(line: String) {
        output.append(line).append(System.lineSeparator())
    }

    /**
     * The prompt text callers and tests share, so neither has to spell it. Constant rather than a
     * literal at each use because a test asserting on what the console printed has to match it exactly.
     */
    companion object {
        /** What a prompt asking for a path puts in front of the cursor. */
        const val PATH_PROMPT = "Path: "
    }
}
