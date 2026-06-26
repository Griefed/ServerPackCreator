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
package de.griefed.serverpackcreator.grinder

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Installs a fully-resolved server base (loader jars + libraries) for one version-tuple into a target
 * directory. The production implementation boots a one-off setup container **with** network so the
 * ServerStarterJar self-installs; tests supply a fake. This is the *only* place network egress is
 * allowed in the grinder — every actual mod-boot then runs offline against the cached result.
 *
 * @author Griefed
 */
fun interface LoaderInstaller {
    /**
     * Populate [target] with the installed server for the given tuple, returning whether it succeeded.
     * Must be self-contained (no reliance on the candidate mod); the result is reused across all mods
     * sharing that loader/Minecraft combination.
     */
    fun install(target: File, loader: String, loaderVersion: String, minecraftVersion: String): Boolean
}

/**
 * Per-`(loader, loaderVersion, minecraftVersion)` cache of an installed server base, so the expensive,
 * network-dependent install runs **once** and every actual mod-boot mounts the cached tree and runs
 * offline (`--network none`). A tuple counts as installed only once its [MARKER] file is present, so a
 * crash mid-install is redone rather than served half-baked; installs of the same tuple are serialized
 * so concurrent workers can't install it twice.
 *
 * @param cacheRoot Root directory under which per-tuple base trees live.
 * @param installer Performs the one-off, network-using install on a cache miss.
 * @author Griefed
 */
class LoaderCache(
    private val cacheRoot: File,
    private val installer: LoaderInstaller
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Per-tuple locks so an install serializes by tuple without blocking unrelated tuples. */
    private val installLocks = ConcurrentHashMap<String, Any>()

    /** The cache directory for a version-tuple, whether or not it has been installed yet. */
    fun baseDirFor(loader: String, loaderVersion: String, minecraftVersion: String): File =
        File(cacheRoot, "${sanitize(minecraftVersion)}/${sanitize(loader)}/${sanitize(loaderVersion)}")

    /** Whether the tuple's base is fully installed (its completion marker is present). */
    fun isInstalled(loader: String, loaderVersion: String, minecraftVersion: String): Boolean =
        File(baseDirFor(loader, loaderVersion, minecraftVersion), MARKER).isFile

    /**
     * Return the installed base directory for the tuple, running [installer] (once, with network) on a
     * miss. Returns `null` when the install fails — the partial tree is removed so the next attempt
     * retries cleanly. The install is serialized per tuple, so parallel workers share a single install.
     */
    fun ensureInstalled(loader: String, loaderVersion: String, minecraftVersion: String): File? {
        val baseDir = baseDirFor(loader, loaderVersion, minecraftVersion)
        if (File(baseDir, MARKER).isFile) {
            return baseDir
        }
        synchronized(lockFor(loader, loaderVersion, minecraftVersion)) {
            // Re-check under the lock: another worker may have installed it while we waited.
            if (File(baseDir, MARKER).isFile) {
                return baseDir
            }
            baseDir.deleteRecursively()
            baseDir.mkdirs()
            val installed = runCatching { installer.install(baseDir, loader, loaderVersion, minecraftVersion) }
                .onFailure { log.warn("Loader install threw for $loader $loaderVersion / Minecraft $minecraftVersion: ${it.message}") }
                .getOrDefault(false)
            if (!installed) {
                baseDir.deleteRecursively()
                return null
            }
            File(baseDir, MARKER).writeText("loader=$loader\nloaderVersion=$loaderVersion\nminecraftVersion=$minecraftVersion\n")
            return baseDir
        }
    }

    /** Intern a per-tuple lock; the map is bounded by the finite loader/version combination space. */
    private fun lockFor(loader: String, loaderVersion: String, minecraftVersion: String): Any =
        installLocks.computeIfAbsent("$loader/$loaderVersion/$minecraftVersion") { Any() }

    /** Make a token safe to use as a path segment, collapsing anything unusual to an underscore. */
    private fun sanitize(token: String): String = token.replace(Regex("[^A-Za-z0-9._-]"), "_")

    companion object {
        /** Completion marker, written only after a successful install; its presence means cache-hit. */
        const val MARKER = ".spc-installed"
    }
}
