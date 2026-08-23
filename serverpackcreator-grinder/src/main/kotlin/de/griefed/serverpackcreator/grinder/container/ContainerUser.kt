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

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.nio.file.Files

/**
 * Decides the `uid:gid` the grinder's containers run as, so a container can write into the host
 * directory it has bind-mounted.
 *
 * The runtime image declares `USER 1000:1000`, which is only correct while the daemon itself runs as
 * uid 1000. It stopped being correct when the grinder became a systemd service under its own account:
 * every pack directory is created by the host process, the container then ran as a stranger to it, and
 * *every* write was refused — the loader install could not save `server.jar` or write
 * `user_jvm_args.txt`, and the boot died on the JVM's `@argfile` error with the actual permission
 * failures scrolled far off the reported tail.
 *
 * Matching the host owner also keeps the work tree free of foreign-owned files, which the reaper would
 * otherwise be unable to clean up.
 *
 * @author Griefed
 */
object ContainerUser {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Environment variable letting an operator pin the container identity explicitly. Named here so the
     * diagnosis that suggests it and the entry point that reads it cannot drift apart; the *reading* happens
     * in `GrinderApplication`, with every other environment knob.
     */
    const val ENV_KEY = "SPC_GRINDER_CONTAINER_USER"

    /** The image's own `USER`, used when the host owner cannot be determined (non-POSIX, unreadable path). */
    const val IMAGE_DEFAULT = "1000:1000"

    /** `uid:gid` shape, so a malformed override cannot silently bind containers to a nonsense identity. */
    private val userAndGroup = Regex("""\d+:\d+""")

    /**
     * The identity containers should run as when they mount [directory]: an explicit [override] if it is
     * usable, otherwise that directory's owning `uid:gid`, otherwise [IMAGE_DEFAULT].
     *
     * The owner of the directory is the right question rather than the process's own uid: it is the
     * identity that must be able to write there, and it stays correct if an operator relocates
     * `SPC_GRINDER_WORK` onto a share owned by somebody else.
     */
    fun forDirectory(directory: File, override: String?): String {
        override?.trim()?.takeIf { isUsableOverride(it) }?.let { return it }
        val resolved = ownerOf(directory) ?: IMAGE_DEFAULT
        // Only when something was actually set: an unset variable is the normal case and needs no comment,
        // but a *set* one being ignored has to be said out loud, or an operator debugging the very failure
        // this knob exists for is looking at a value they did not choose.
        if (!override.isNullOrBlank()) {
            log.warn(
                "Ignoring $ENV_KEY='$override': it must be numeric `uid:gid` (e.g. 1000:1000). " +
                    "Using $resolved, the owner of ${directory.absolutePath}, instead."
            )
        }
        return resolved
    }

    /**
     * Whether [override] is something Docker can be handed: a numeric `uid:gid`. Names are rejected because the
     * ids are resolved against the *container's* `/etc/passwd`, not the host's, so a host account name either
     * fails to start the container or silently means somebody else inside it.
     */
    fun isUsableOverride(override: String?): Boolean {
        val trimmed = override?.trim()
        return !trimmed.isNullOrEmpty() && userAndGroup.matches(trimmed)
    }

    /**
     * The `uid:gid` owning [directory], or `null` where the filesystem cannot answer — a non-POSIX host
     * (Windows, and Docker Desktop's VM handles the mapping itself) or a path that does not exist.
     */
    private fun ownerOf(directory: File): String? = runCatching {
        val path = directory.toPath()
        "${Files.getAttribute(path, "unix:uid")}:${Files.getAttribute(path, "unix:gid")}"
    }.getOrNull()
}
