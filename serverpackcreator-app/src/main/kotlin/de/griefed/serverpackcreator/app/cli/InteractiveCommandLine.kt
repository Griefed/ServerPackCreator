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
import org.jline.jansi.AnsiConsole
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
import java.io.PrintWriter
import java.nio.file.Path
import java.util.function.Supplier


class InteractiveCommandLine(private val apiWrapper: ApiWrapper, updateChecker: UpdateChecker) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    @get:Synchronized
    val configurationEditor: ConfigurationEditor by lazy {
        ConfigurationEditor(
            apiWrapper.configurationHandler,
            apiWrapper.apiProperties,
            apiWrapper.versionMeta
        )
    }

    val configGenCommand = ConfigGenCommand(apiWrapper, configurationEditor)
    val helpCommand = HelpCommand()
    val homeDirCommand = HomeDirCommand()
    val languageCommand = LanguageCommand(apiWrapper)
    val runHeadlessCommand = RunHeadlessCommand(apiWrapper)
    val runHeadlessCustomConfigCommand = RunHeadlessCustomConfigCommand(apiWrapper)
    val setupCommand = SetupCommand(apiWrapper)
    val updateCommand = UpdateCommand(updateChecker)


    @CommandLine.Command(
        name = "",
        description = [
            "Example interactive shell with completion and autosuggestions. Hit @|magenta <TAB>|@ to see available commands.",
            "Hit @|magenta ALT-S|@ to toggle tailtips.",
            ""
        ],
        footer = ["", "Press Ctl-D to exit."],
        subcommands = [
            ConfigGenCommand::class,
            HelpCommand::class,
            HomeDirCommand::class,
            LanguageCommand::class,
            RunHeadlessCommand::class,
            RunHeadlessCustomConfigCommand::class,
            SetupCommand::class,
            UpdateCommand::class,
            ClearScreen::class,
            CommandLine.HelpCommand::class
        ]
    )
    class CliCommands internal constructor() : Runnable {
        private var reader: LineReaderImpl? = null
        var out: PrintWriter? = null

        fun setReader(reader: LineReader) {
            this.reader = reader as LineReaderImpl
            out = reader.terminal.writer()
        }

        override fun run() {
            out!!.println(CommandLine(this).usageMessage)
        }
    }

    fun run(args: Array<String> = arrayOf("")) {
        try {
            val workDir: Supplier<Path> = Supplier<Path> { apiWrapper.apiProperties.homeDirectory.absoluteFile.toPath() }
            val builtins = Builtins(workDir, ConfigurationPath(workDir.get(), workDir.get()), null)
            builtins.rename(Builtins.Command.TTOP, "top")
            builtins.alias("zle", "widget")
            builtins.alias("bindkey", "keymap")

            val commands = CliCommands()
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