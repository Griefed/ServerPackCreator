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
package de.griefed.serverpackcreator.app.cli.commands

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.clientside.MetadataScanner
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands.ClearScreen
import java.io.File

/**
 * Scans the jars in a directory for the sideness they *declare* in their metadata, using SPC's own
 * per-loader scanners, and prints the result as JSON. This is the reusable, pipeline-friendly
 * primitive behind the clientside-verification workflow — `java -jar … -scan <dir> --loader Forge
 * --minecraft 1.20.1` answers "which of these mods declare themselves client-only?".
 *
 * @param apiWrapper Provides the [de.griefed.serverpackcreator.api.modscanning.ModScanner].
 * @author Griefed
 */
@CommandLine.Command(
    name = "scan", mixinStandardHelpOptions = true,
    description = [
        "Scan the mods in a directory for the sideness they declare in their metadata.",
        "Prints a JSON map of file-name to CLIENT/SERVER_OR_BOTH/ERROR."
    ],
    subcommands = [ClearScreen::class, CommandLine.HelpCommand::class]
)
class ScanCommand(private val apiWrapper: ApiWrapper = ApiWrapper.api()) : Command {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter()

    /** Mods-directory to scan, supplied interactively via the option below. */
    @CommandLine.Option(
        names = ["-d", "--directory"],
        description = ["The directory containing the mod-jars to scan."],
        required = true
    )
    private var directory: String? = null

    /** Canonical loader-name the mods target (Forge, NeoForge, Fabric, Quilt, LegacyFabric). */
    @CommandLine.Option(
        names = ["-l", "--loader"],
        description = ["The modloader: Forge, NeoForge, Fabric, Quilt or LegacyFabric."],
        required = true
    )
    private var loader: String? = null

    /** Minecraft version, used to pick the correct Forge/NeoForge scanner variant. */
    @CommandLine.Option(
        names = ["-m", "--minecraft"],
        description = ["The Minecraft version (selects the correct Forge/NeoForge scanner)."],
        required = true
    )
    private var minecraftVersion: String? = null

    override fun run() {
        scan(File(directory ?: return), loader ?: return, minecraftVersion ?: return)
    }

    /**
     * Scan every jar in [directory] for the given [loader]/[minecraftVersion] and print a JSON map of
     * file-name to declared sideness to stdout.
     */
    fun scan(directory: File, loader: String, minecraftVersion: String) {
        if (!directory.isDirectory) {
            log.warn("${directory.absolutePath} is not a directory.")
            println("""{"error":"not a directory: ${directory.absolutePath}"}""")
            return
        }
        val scanner = MetadataScanner(apiWrapper.modScanner)
        val jars = directory.listFiles { file -> file.isFile && file.name.endsWith(".jar") }?.sorted() ?: emptyList()
        val results = jars.associate { jar -> jar.name to scanner.scan(jar, loader, minecraftVersion).name }
        println(json.writeValueAsString(results))
    }
}
