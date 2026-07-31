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
package de.griefed.serverpackcreator.grinder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins that the daemon's settings file is resolved against its own home, never against the working directory.
 *
 * `ApiProperties`' default is `File("serverpackcreator.properties")` — a **relative** path — and
 * `PropertyStore.loadProperties` adds every file it loads to `trackedPropertyFiles`, which `save()` then writes to
 * for the rest of the process. So relying on that default made the daemon create and rewrite a settings file in
 * whatever directory it happened to be launched from: started from a checkout, it dropped
 * `serverpackcreator.properties` into the repository root on every start, which is a permanently dirty working tree
 * and the way a genuinely unexpected file gets overlooked.
 *
 * The daemon already knows its home — it derives `work`, `cache` and the verdict store from it — so the settings
 * file belongs there. Absoluteness is the property under test: whether the path is *correct* follows from the home,
 * but whether it depends on the caller's working directory is what caused the defect.
 */
internal class GrinderPropertiesResolutionTest {

    /** With nothing configured, the file sits in the daemon's own home rather than wherever it was launched from. */
    @Test
    fun withoutAnOverrideTheSettingsFileLivesInTheDaemonsHome(@TempDir home: File) {
        val resolved = GrinderApplication.resolveSpcPropertiesFile(explicitPath = null, home = home)

        Assertions.assertEquals(
            File(home, "serverpackcreator.properties").canonicalFile,
            resolved.canonicalFile,
            "the settings file must be resolved against the daemon's home"
        )
        Assertions.assertTrue(
            resolved.isAbsolute,
            "a relative path here is the whole defect: PropertyStore keeps every loaded file as a write target, so " +
                "it would be created and rewritten in whatever directory the daemon was started from"
        )
    }

    /** An explicit override wins, and is made absolute so it cannot drift with the working directory either. */
    @Test
    fun anExplicitOverrideIsHonouredAndMadeAbsolute(@TempDir home: File, @TempDir elsewhere: File) {
        val chosen = File(elsewhere, "custom.properties")

        val resolved = GrinderApplication.resolveSpcPropertiesFile(chosen.absolutePath, home)

        Assertions.assertEquals(chosen.canonicalFile, resolved.canonicalFile, "an operator's choice must win")

        val relativeChoice = GrinderApplication.resolveSpcPropertiesFile("custom.properties", home)
        Assertions.assertTrue(
            relativeChoice.isAbsolute,
            "even an override given as a relative path must be pinned to something absolute, or it moves with the cwd"
        )
    }

    /** A blank or whitespace override is not a choice — it must fall back rather than resolve to the home itself. */
    @Test
    fun aBlankOverrideFallsBackToTheHome(@TempDir home: File) {
        val expected = File(home, "serverpackcreator.properties").canonicalFile

        for (blank in listOf("", "   ", "\t")) {
            Assertions.assertEquals(
                expected,
                GrinderApplication.resolveSpcPropertiesFile(blank, home).canonicalFile,
                "a blank override ('${blank.replace("\t", "\\t")}') must be treated as unset"
            )
        }
    }
}
