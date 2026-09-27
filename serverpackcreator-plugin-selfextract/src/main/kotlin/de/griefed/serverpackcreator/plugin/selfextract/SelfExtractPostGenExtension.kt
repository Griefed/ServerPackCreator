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
package de.griefed.serverpackcreator.plugin.selfextract

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PostGenExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.selfextract.core.SelfExtractingArchive
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.pf4j.Extension
import java.io.File
import java.util.Optional

/**
 * Wraps the pack ServerPackCreator has just finished into its two self-extracting artifacts.
 *
 * A `PostGenExtension` because it needs the finished article: it runs after the files are copied, the
 * scripts written and the ZIP closed, at the end of `ServerPackHandler.run`, which is the choke point
 * every GUI, CLI and web generation passes through.
 *
 * @author Griefed
 */
@Suppress("unused")
@Extension
class SelfExtractPostGenExtension : PostGenExtension {

    /**
     * Wrap the pack at [destination] into a `.bsx` and a `.cmd` beside it.
     *
     * Every other parameter is part of the extension point's contract and unused: what the artifacts
     * need is the directory that was just written.
     */
    override fun run(
        versionMeta: VersionMeta,
        utilities: Utilities,
        apiProperties: ApiProperties,
        packConfig: PackConfig,
        destination: String,
        pluginConfig: Optional<CommentedConfig>,
        packSpecificConfigs: ArrayList<CommentedConfig>
    ) {
        val pack = File(destination)
        if (!pack.isDirectory) {
            log.warn("No server pack at $destination - nothing to wrap.")
            return
        }
        // Reported and skipped rather than followed. Windows cannot recreate a symbolic link without
        // Developer Mode, and one pointing out of the pack would let extraction write outside the
        // destination directory - a vulnerability, not a packaging quirk.
        val links = pack.walkTopDown().filter { java.nio.file.Files.isSymbolicLink(it.toPath()) }.toList()
        if (links.isNotEmpty()) {
            log.warn("Not wrapping ${pack.name}: it contains symbolic links, which neither artifact can carry.")
            links.forEach { log.warn("  symbolic link: $it") }
            return
        }
        // Nothing here may throw. The generation is already finished and the pack already on disk, so
        // the only thing an exception could achieve is turning a good generation into a reported one.
        runCatching { SelfExtractingArchive.wrap(pack) }
            .onSuccess { written -> written.forEach { log.info("Wrote ${it.name} (${it.length()} bytes).") } }
            .onFailure { log.error("Could not wrap $destination into a self-extracting server pack.", it) }
    }

    /** This extension's own log, named after the class the way every other SPC component is. */
    private val log = cachedLoggerOf(this.javaClass)

    /** This extension's name as SPC lists it in `plugins.log`. */
    override val name = "Self-extracting server pack"
    /** One line explaining the extension wherever SPC lists it. */
    override val description = "Wraps every generated server pack in a self-extracting .bsx and .cmd."
    /** Who to blame in `plugins.log` when this extension misbehaves. */
    override val author = "Griefed"
    /** This extension's own version, independent of the jar's. */
    override val version = "1.0.0"
    /** Identifies this extension among the plugin's own, for SPC's logs and configuration. */
    override val extensionId = "selfextractpostgen"
}
