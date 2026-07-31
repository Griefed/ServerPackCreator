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
import java.util.Properties

/**
 * Pins that the suite's own configuration is machine-independent.
 *
 * 38 test call-sites boot an `ApiWrapper` from `build/resources/test/serverpackcreator.properties`, and the
 * committed source of that file carried one developer's absolute paths — an SDKMAN JDK under their home and an
 * absolute `server.tomcat.basedir`. Harmless while only that machine runs the suite; meaningless everywhere else,
 * and exactly the kind of value that gets copied forward for years because nothing fails visibly when it is wrong.
 *
 * The resolved values now belong to the build, which writes them into `build/resources/test/` at test time. So
 * there are two obligations: the committed file must name no host, and the generated one must be genuinely
 * resolved for the machine running.
 */
internal class TestPropertiesTest {

    /** The committed template — machine-independent by contract. */
    private val committed = File("src/test/resources/serverpackcreator.properties")

    /** What the suite actually reads, filled in by `processTestResources`. */
    private val generated = File("build/resources/test/serverpackcreator.properties")

    /** Read a properties file, unescaping as the `Properties` loader does — the paths contain escaped colons. */
    private fun read(file: File): Properties = Properties().apply {
        Assertions.assertTrue(file.isFile, "expected $file to exist")
        file.inputStream().use { load(it) }
    }

    /** No absolute host path may be committed: it is wrong on every machine but the one it came from. */
    @Test
    fun theCommittedTestPropertiesNameNoHost() {
        val offenders = committed.readLines()
            .withIndex()
            .filter { (_, line) -> !line.trimStart().startsWith("#") }
            .filter { (_, line) -> Regex("""=\s*(/Users/|/home/|[A-Za-z]:\\\\)""").containsMatchIn(line) }
            .map { (index, line) -> "line ${index + 1}: ${line.take(90)}" }

        Assertions.assertTrue(
            offenders.isEmpty(),
            "the committed test properties must not carry one machine's absolute paths — the build fills these in " +
                "at test time:\n" + offenders.joinToString("\n")
        )
    }

    /** The generated copy must point at a real JDK, or a generated pack's `JAVA` is a path to nothing. */
    @Test
    fun theGeneratedTestPropertiesResolveThisMachinesJava() {
        val javaPath = read(generated).getProperty("de.griefed.serverpackcreator.java")

        Assertions.assertFalse(
            javaPath.isNullOrBlank(),
            "the build must fill de.griefed.serverpackcreator.java in; the suite writes it into generated packs"
        )
        Assertions.assertTrue(
            File(javaPath).canExecute(),
            "de.griefed.serverpackcreator.java points at $javaPath, which is not an executable on this machine"
        )
    }

    /** And at this module's own reserved test directory, matching the isolated home the build injects. */
    @Test
    fun theGeneratedTestPropertiesResolveThisModulesTestDirectory() {
        val basedir = read(generated).getProperty("server.tomcat.basedir")

        Assertions.assertFalse(basedir.isNullOrBlank(), "the build must fill server.tomcat.basedir in")
        Assertions.assertEquals(
            File("tests").absoluteFile.canonicalFile,
            File(basedir).canonicalFile,
            "server.tomcat.basedir must be this module's own tests directory, the same home the build injects"
        )
    }
}
