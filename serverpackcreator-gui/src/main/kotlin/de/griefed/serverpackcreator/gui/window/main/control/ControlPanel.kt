package de.griefed.serverpackcreator.gui.window.main.control

import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.PackConfig
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Desktop
import java.awt.Window
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.*
import javax.swing.text.*

class ControlPanel(
    private val guiProps: GuiProps,
    private val configsTab: ConfigsTab,
    private val larsonScanner: LarsonScanner,
    private val configurationHandler: ConfigurationHandler,
    private val apiProperties: ApiProperties,
    private val serverPackHandler: ServerPackHandler,
    utilities: Utilities
) {
    private val log = cachedLoggerOf(this.javaClass)
    private val statusPanel = StatusPanel()
    private val generate = JButton(Gui.createserverpack_gui_buttongenerateserverpack.toString())
    private val serverPacks = JButton(Gui.createserverpack_gui_buttonserverpacks.toString())
    private val finishedGenerationDocument: StyledDocument = DefaultStyledDocument()
    private val finishedGenerationAttributeSet: SimpleAttributeSet = SimpleAttributeSet()
    private val finishedGenerationPane: JTextPane = JTextPane(finishedGenerationDocument)
    private val lazyModeDocument: StyledDocument = DefaultStyledDocument()
    private val lazyModeTextPane: JTextPane = JTextPane(lazyModeDocument)
    val panel = JPanel()

    init {
        finishedGenerationPane.isOpaque = false
        finishedGenerationPane.isEditable = false
        finishedGenerationPane.setCharacterAttributes(finishedGenerationAttributeSet, true)
        StyleConstants.setAlignment(finishedGenerationAttributeSet, StyleConstants.ALIGN_LEFT)
        try {
            finishedGenerationDocument.insertString(
                0,
                Gui.createserverpack_gui_createserverpack_openfolder_browse.toString() + "    ",
                finishedGenerationAttributeSet
            )
        } catch (ex: BadLocationException) {
            log.error("Error inserting text into aboutDocument.", ex)
        }

        generate.icon = guiProps.genIcon
        generate.addActionListener { generateServerPack() }
        generate.multiClickThreshhold = 1000
        generate.toolTipText = Gui.createserverpack_gui_buttongenerateserverpack_tip.toString()
        serverPacks.icon = guiProps.packsIcon
        serverPacks.addActionListener { utilities.fileUtilities.openFolder(apiProperties.serverPacksDirectory) }
        serverPacks.toolTipText = Gui.createserverpack_gui_buttonserverpacks_tip.toString()
        panel.layout = MigLayout(
            "",
            "0[200!]0[grow]0",
            "0[75!,bottom]10[75!,top]0"
        )
        panel.add(generate, "cell 0 0 1 1,grow,height 50!,width 150!,align center")
        panel.add(serverPacks, "cell 0 1 1 1,grow,height 50!,width 150!,align center")
        panel.add(statusPanel.panel, "cell 1 0 1 2,grow,push, h 160!")
    }

    /**
     * Upon button-press, check the entered configuration and if successfull, generate a server pack.
     *
     * @author Griefed
     */
    private fun generateServerPack() {
        generate.isEnabled = false
        larsonScanner.loadConfig(guiProps.busyConfig)
        var decision = 0
        if (configsTab.selectedEditor?.getCopyDirectories() == "lazy_mode") {
            decision = JOptionPane.showConfirmDialog(
                panel,
                lazyModeTextPane,
                Gui.createserverpack_gui_createserverpack_lazymode.toString(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            )
        }
        log.debug("Case $decision")
        when (decision) {
            0 -> generate()
            else -> ready()
        }
    }

    /**
     * Set the GUI ready for the next generation.
     *
     * @author Griefed
     */
    private fun ready() {
        generate.isEnabled = true
        larsonScanner.loadConfig(guiProps.idleConfig)
    }

    /**
     * Generate a server pack from the current configuration in the GUI.
     *
     * @author Griefed
     */
    private fun generate() {
        val activeTab = configsTab.selectedEditor
        if (activeTab == null) {
            log.error("No tab available")
            statusPanel.updateStatus("No tab available. Load or create a new configuration.")
        }
        val packConfig: PackConfig = activeTab!!.getCurrentConfiguration()
        val encounteredErrors: MutableList<String> = ArrayList(100)

        log.info("Checking entered configuration.")
        statusPanel.updateStatus(Gui.createserverpack_log_info_buttoncreateserverpack_start.toString())

        val executorService: ExecutorService = Executors.newSingleThreadExecutor()
        executorService.execute {

            if (!activeTab.checkServer()) {
                packConfig.isServerInstallationDesired = false
            }

            if (!configurationHandler.checkConfiguration(packConfig, encounteredErrors, true)) {
                log.info("Configuration checked successfully.")
                statusPanel.updateStatus(Gui.createserverpack_log_info_buttoncreateserverpack_checked.toString())
                //TODO store to modpackname.conf in configs dir
                packConfig.save(apiProperties.defaultConfig)
                log.info("Starting ServerPackCreator run.")
                statusPanel.updateStatus(Gui.createserverpack_log_info_buttoncreateserverpack_generating.toString())
                try {
                    serverPackHandler.run(packConfig)
                    configsTab.loadConfig(apiProperties.defaultConfig,activeTab)
                    statusPanel.updateStatus(Gui.createserverpack_log_info_buttoncreateserverpack_ready.toString())
                    finishedGenerationDocument.setParagraphAttributes(
                        0, finishedGenerationDocument.length, finishedGenerationAttributeSet, false
                    )
                    ready()
                    if (JOptionPane.showConfirmDialog(
                            Window.getWindows()[0],
                            finishedGenerationPane,
                            Gui.createserverpack_gui_createserverpack_openfolder_title.toString(),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            guiProps.infoIcon
                        ) == 0
                    ) {
                        try {
                            Desktop.getDesktop().open(File(serverPackHandler.getServerPackDestination(packConfig)))
                        } catch (ex: IOException) {
                            log.error("Error opening file explorer for server pack.", ex)
                        }
                    }
                } catch (ex: Exception) {
                    log.error("An error occurred when generating the server pack.", ex)
                }
            } else {
                statusPanel.updateStatus(Gui.createserverpack_gui_buttongenerateserverpack_fail.toString())
                if (encounteredErrors.isNotEmpty()) {
                    val errors = StringBuilder()
                    for (i in encounteredErrors.indices) {
                        errors.append(i + 1).append(": ").append(encounteredErrors[i]).append("    ").append("\n")
                    }
                    ready()
                    JOptionPane.showMessageDialog(
                        panel,
                        errors,
                        Gui.createserverpack_gui_createserverpack_errors_encountered(encounteredErrors.size),
                        JOptionPane.ERROR_MESSAGE,
                        guiProps.errorIcon
                    )
                }
            }
            encounteredErrors.clear()
            ready()
            executorService.shutdownNow()
        }
    }
}