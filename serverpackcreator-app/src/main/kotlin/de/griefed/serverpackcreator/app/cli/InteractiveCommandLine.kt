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
package de.griefed.serverpackcreator.app.cli

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.cli.commands.*
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.jline.builtins.ConfigurationPath
import org.jline.console.CmdLine
import org.jline.console.SystemRegistry
import org.jline.console.impl.Builtins
import org.jline.console.impl.SystemRegistryImpl
import org.jline.keymap.KeyMap
import org.jline.reader.*
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.TerminalBuilder
import org.jline.widget.TailTipWidgets
import org.jline.widget.TailTipWidgets.TipType
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands
import picocli.shell.jline3.PicocliCommands.ClearScreen
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory
import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import java.util.*
import java.util.function.Supplier

class InteractiveCommandLine(private val apiWrapper: ApiWrapper, updateChecker: UpdateChecker) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    val cliCommands = CliCommands(apiWrapper)
    val configGenCommand = ConfigGenCommand(apiWrapper)
    val helpCommand = HelpCommand()
    val runHeadlessCommand = RunHeadlessCommand(apiWrapper)
    val setupCommand = SetupCommand(apiWrapper)
    val updateCommand = UpdateCommand(updateChecker)

    @CommandLine.Command(
        name = "",
        description = ["Interactive commandline with completion and autosuggestions."],
        subcommands = [
            ConfigGenCommand::class,
            HelpCommand::class,
            HomeDirCommand::class,
            LanguageCommand::class,
            RunHeadlessCommand::class,
            SetupCommand::class,
            UpdateCommand::class,
            ClearScreen::class,
            CommandLine.HelpCommand::class
        ]
    )
    class CliCommands(private val apiWrapper: ApiWrapper = ApiWrapper.api()) : Runnable {
        private val log by lazy { cachedLoggerOf(this.javaClass) }
        private var reader: LineReaderImpl? = null
        var out: PrintWriter? = null

        fun setReader(reader: LineReader) {
            this.reader = reader as LineReaderImpl
            out = reader.terminal.writer()
        }

        override fun run() {
            out!!.println(CommandLine(this).usageMessage)
        }

        @CommandLine.Command(
            mixinStandardHelpOptions = true, subcommands = [CommandLine.HelpCommand::class],
            description = [
                "Feeling lucky, Punk? This will generate a server pack config from a passed modpack-directory and generate a server pack in one go. No warranty. No guarantees."
            ]
        )
        fun feelingLucky(
            @CommandLine.Option(
                names = ["-m", "--modpackDir"],
                description = [
                    "The path to the modpack-directory.",
                    "Windows Users: You can use / instead of \\ in your paths, too."
                ],
                required = true
            ) modpackDir: String?,
            @CommandLine.Option(
                names = ["-d", "--destination"],
                description = [
                    "A destination in which the server pack will be created in.",
                    "All folders, including parent folders, will be created in the process.",
                    "Windows Users: You can use / instead of \\ in your paths, too."
                ],
                required = false
            ) destination: String?
        ) {
            if (modpackDir != null && File(modpackDir).isDirectory) {
                val modpack = File(modpackDir)
                val packConfig = apiWrapper.configurationHandler.generateConfigFromModpack(modpack)
                packConfig.customDestination = Optional.ofNullable(destination?.let { File(it) })
                val check = apiWrapper.configurationHandler.checkConfiguration(packConfig)
                packConfig.save(File(apiWrapper.apiProperties.configsDirectory, packConfig.name ?: modpack.name))
                if (!check.allChecksPassed) {
                    println("Encountered the following errors/problems with the config:")
                    for (error in check.encounteredErrors) {
                        println(error)
                    }
                } else {
                    val generation = apiWrapper.serverPackHandler.run(packConfig)
                    if (!generation.success) {
                        println("Error generating Server Pack:")
                        for (error in generation.errors) {
                            println(error)
                        }
                    } else {
                        println("Successfully generated Server Pack: ${generation.serverPack.absolutePath}")
                    }
                }
            } else {
                log.error("Modpack-directory $modpackDir doesn't exist.")
            }
        }
    }

    fun cli(args: Array<String> = arrayOf("")) {
        try {
            val workDir: Supplier<Path> = Supplier<Path> { apiWrapper.apiProperties.homeDirectory.absoluteFile.toPath() }
            val builtins = Builtins(workDir, ConfigurationPath(workDir.get(), workDir.get()), null)
            builtins.rename(Builtins.Command.TTOP, "top")
            builtins.alias("zle", "widget")
            builtins.alias("bindkey", "keymap")

            val commands = cliCommands
            val factory = PicocliCommandsFactory()
            val cmd = CommandLine(commands, factory)
            val picocliCommands = PicocliCommands(cmd)
            val parser: Parser = DefaultParser()

            TerminalBuilder.builder().build().use { terminal ->
                val systemRegistry: SystemRegistry = SystemRegistryImpl(parser, terminal, workDir, null)
                systemRegistry.setCommandRegistries(builtins, picocliCommands)
                systemRegistry.register("help", picocliCommands)

                val reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(systemRegistry.completer())
                    .parser(parser)
                    .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                    .build()
                builtins.setLineReader(reader)
                commands.setReader(reader)
                factory.setTerminal(terminal)
                val widgets = TailTipWidgets(
                    reader,
                    { line: CmdLine? -> systemRegistry.commandDescription(line) }, 5, TipType.COMPLETER
                )
                widgets.enable()
                val keyMap = reader.keyMaps["main"]!!
                keyMap.bind(Reference("tailtip-toggle"), KeyMap.alt("s"))

                val prompt = "ServerPackCreator> "
                val rightPrompt: String? = null

                // start the shell and process input until the user quits with Ctrl-D
                var line: String?
                while (true) {
                    try {
                        systemRegistry.cleanUp()
                        line = reader.readLine(prompt, rightPrompt, null as MaskingCallback?, null)
                        systemRegistry.execute(line)
                    } catch (e: UserInterruptException) {
                        // Ignore
                    } catch (e: EndOfFileException) {
                        return
                    } catch (e: Exception) {
                        systemRegistry.trace(e)
                    }
                }
            }
        } catch (t: Throwable) {
            log.error("Error initializing terminal.", t)
        }
    }
}