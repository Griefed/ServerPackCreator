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
package de.griefed.serverpackcreator.grinder.container

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

/**
 * Pins [ContainerUser] — the resolution that decides which uid:gid the boot and install containers run
 * as. The image bakes in `USER 1000:1000`, and every container bind-mounts a directory the *host*
 * process created; when the two identities differ, every write inside the pack is refused and the boot
 * fails in a way that points nowhere near permissions (observed live 2026-08-23: `start.sh` ran to
 * completion and died on `Error: could not open 'user_jvm_args.txt'`).
 */
internal class ContainerUserTest {

    @Test
    fun resolvesTheOwnerOfTheDirectoryTheContainerWillWriteInto(@TempDir workDirectory: File) {
        val uid = runCatching { Files.getAttribute(workDirectory.toPath(), "unix:uid") }.getOrNull()
        Assumptions.assumeTrue(uid != null, "POSIX-only: the host uid is what the container must match")
        val gid = Files.getAttribute(workDirectory.toPath(), "unix:gid")

        Assertions.assertEquals("$uid:$gid", ContainerUser.forDirectory(workDirectory, override = null))
    }

    @Test
    fun prefersAnExplicitOverrideSoAnOperatorCanPinAnUnusualSetup(@TempDir workDirectory: File) {
        Assertions.assertEquals("4242:99", ContainerUser.forDirectory(workDirectory, override = "4242:99"))
    }

    @Test
    fun ignoresABlankOverrideRatherThanBindingTheContainerToNobody(@TempDir workDirectory: File) {
        val resolved = ContainerUser.forDirectory(workDirectory, override = "   ")

        Assertions.assertNotEquals("   ", resolved)
        Assertions.assertTrue(resolved.matches(Regex("""\d+:\d+""")), "must still resolve a real uid:gid, was '$resolved'")
    }

    @Test
    fun fallsBackToTheImagesOwnUserWhenTheOwnerCannotBeRead() {
        val missing = File("/definitely/not/a/directory/on/this/host")

        Assertions.assertEquals(ContainerUser.IMAGE_DEFAULT, ContainerUser.forDirectory(missing, override = null))
    }
}
