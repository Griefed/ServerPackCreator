package de.griefed.serverpackcreator.gui.window.configs.components.serverfiles

import Gui
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.InclusionSpecification
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditor
import de.griefed.serverpackcreator.gui.window.configs.components.DocumentChangeListener
import de.griefed.serverpackcreator.gui.window.configs.components.ElementLabel
import de.griefed.serverpackcreator.gui.window.configs.components.ScrollTextField
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.ListSelectionEvent

class ServerFilesEditor(
    private val chooserDimension: Dimension,
    private val guiProps: GuiProps,
    private val configEditor: ConfigEditor,
    private val apiWrapper: ApiWrapper

) : JSplitPane(HORIZONTAL_SPLIT) {
    private val log = cachedLoggerOf(this.javaClass)
    private val leftPanel = JPanel(BorderLayout())
    private val rightPanel = JPanel(
        MigLayout(
            "left,wrap",
            "0[left,::64]5[left,::150]5[left,grow]5[left,::64]5[left,::64]0",
            "30"
        )
    )
    private val inclusionModel = DefaultListModel<InclusionSpecification>()
    private val list = JList(inclusionModel)
    private val listScroller = JScrollPane(list)
    private val source = ScrollTextField("")
    private val sourceInfo = SourceInfo(guiProps)
    private val sourceLabel = ElementLabel("Source:")
    private val destination = ScrollTextField("")
    private val destinationInfo = DestinationInfo(guiProps)
    private val destinationLabel = ElementLabel("Destination:")
    private val inclusionFilter = ScrollTextField("")
    private val inclusionInfo = InclusionInfo(guiProps)
    private val inclusionLabel = ElementLabel("Inclusion-Filter:")
    private val exclusionFilter = ScrollTextField("")
    private val exclusionInfo = ExclusionInfo(guiProps)
    private val exclusionLabel = ElementLabel("Exclusion-Filter:")
    private val toggleVisibility = JToggleButton(guiProps.toggleHelpIcon)
    private val tip = JTextPane()
    private val scrollTip = JScrollPane(tip)
    private val expertPanel = JPanel(
        MigLayout(
            "left,wrap",
            "0[left,::64]5[left,::150]5[left,grow]0",
            "30"
        )
    )
    private val fileAdd = BalloonTipButton(null, guiProps.addIcon, "Add a new entry", guiProps) { addEntry("") }
    private val fileRemove =
        BalloonTipButton(null, guiProps.deleteIcon, "Delete selected entry", guiProps) { removeSelectedEntry() }
    private val filesShowBrowser = BalloonTipButton(
        null, guiProps.folderIcon, Gui.createserverpack_gui_browser.toString(), guiProps
    ) { selectInclusions() }
    val filesRevert = BalloonTipButton(
        null, guiProps.revertIcon, Gui.createserverpack_gui_buttoncopydirs_revert_tip.toString(), guiProps
    ) { revertInclusions() }
    val filesReset = BalloonTipButton(
        null, guiProps.resetIcon, Gui.createserverpack_gui_buttoncopydirs_reset_tip.toString(), guiProps
    ) { setInclusionsFromStringList(apiWrapper.apiProperties.directoriesToInclude.toMutableList()) }
    private var selectedInclusion: InclusionSpecification? = null
    val timer: Timer

    init {
        dividerLocation = 150
        setLeftComponent(leftPanel)
        setRightComponent(rightPanel)
        list.visibleRowCount = -1
        list.layoutOrientation = JList.HORIZONTAL_WRAP
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = InclusionSpecificationRenderer()
        list.addListSelectionListener { event -> selectionOccured(event) }
        leftPanel.add(listScroller, BorderLayout.CENTER)
        tip.isEditable = false
        tip.text =
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.\n\nDuis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet."
        expertPanel.isVisible = false
        scrollTip.isVisible = true
        toggleVisibility.addActionListener { toggleVisibility() }

        expertPanel.add(destinationInfo, "cell 0 0")
        expertPanel.add(destinationLabel, "cell 1 0")
        expertPanel.add(destination, "cell 2 0, grow,w 50:50:")
        expertPanel.add(inclusionInfo, "cell 0 1")
        expertPanel.add(inclusionLabel, "cell 1 1")
        expertPanel.add(inclusionFilter, "cell 2 1, grow,w 50:50:")
        expertPanel.add(exclusionInfo, "cell 0 2")
        expertPanel.add(exclusionLabel, "cell 1 2")
        expertPanel.add(exclusionFilter, "cell 2 2, grow,w 50:50:")

        rightPanel.add(sourceInfo, "cell 0 0")
        rightPanel.add(toggleVisibility, "cell 0 1")
        rightPanel.add(fileAdd, "cell 0 2")
        rightPanel.add(fileRemove, "cell 0 3")
        rightPanel.add(sourceLabel, "cell 1 0")
        rightPanel.add(source, "cell 2 0, grow, w 50:50:")
        rightPanel.add(expertPanel, "cell 1 1 2 3, grow, w 50:50:, hidemode 3")
        rightPanel.add(scrollTip, "cell 1 1 2 3, grow, w 50:50:, hidemode 3")
        rightPanel.add(filesShowBrowser, "cell 3 0")
        rightPanel.add(filesRevert, "cell 3 1")
        rightPanel.add(filesReset, "cell 3 2")
        source.addDocumentListener(object: DocumentChangeListener {
            override fun update(e: DocumentEvent) {
                sourceWasEdited()
            }
        })
        destination.addDocumentListener(object: DocumentChangeListener {
            override fun update(e: DocumentEvent) {
                destinationWasEdited()
            }
        })
        inclusionFilter.addDocumentListener(object: DocumentChangeListener {
            override fun update(e: DocumentEvent) {
                inclusionFilterWasEdited()
            }
        })
        exclusionFilter.addDocumentListener(object: DocumentChangeListener {
            override fun update(e: DocumentEvent) {
                exclusionFilterWasEdited()
            }
        })
        timer = Timer(2000) {
            updateTip()
        }
        timer.stop()
        timer.delay = 2000
        timer.isRepeats = false
    }

    fun updateTip() {
        try {
            val acquired = apiWrapper.serverPackHandler!!.getServerFiles(
                selectedInclusion!!,
                configEditor.getModpackDirectory(),
                apiWrapper.serverPackHandler!!.getServerPackDestination(configEditor.getCurrentConfiguration()),
                mutableListOf(),
                configEditor.getClientSideModsList(),
                configEditor.getMinecraftVersion(),
                configEditor.getModloader()
            )
            var tipContent = "The following files would be included with this Inclusion-Specification:\n"
            for (file in acquired) {
                tipContent += file.sourceFile.absolutePath.replace(configEditor.getModpackDirectory() + File.separator,"") + "\n"
            }
            tip.text = tipContent
        } catch (ex: Exception) {
            log.error("Couldn't acquire files to include. ",ex)
        }
    }

    fun sourceWasEdited() {
        list.selectedValue.source = source.text
        timer.restart()
        list.updateUI()
    }

    fun destinationWasEdited() {
        list.selectedValue.destination = destination.text
    }

    fun inclusionFilterWasEdited() {
        list.selectedValue.inclusionFilter = inclusionFilter.text
        timer.restart()
    }

    fun exclusionFilterWasEdited() {
        list.selectedValue.exclusionFilter = exclusionFilter.text
        timer.restart()
    }

    private fun selectionOccured(event: ListSelectionEvent) {
        when {
            event.valueIsAdjusting -> return
            list.selectedIndex == -1 -> return
        }
        selectedInclusion = list.selectedValue
        source.text = selectedInclusion!!.source
        destination.text = selectedInclusion!!.destination ?: ""
        inclusionFilter.text = selectedInclusion!!.inclusionFilter ?: ""
        exclusionFilter.text = selectedInclusion!!.exclusionFilter ?: ""
        timer.restart()
    }

    private fun toggleVisibility() {
        expertPanel.isVisible = !expertPanel.isVisible
        scrollTip.isVisible = !scrollTip.isVisible
        if (expertPanel.isVisible) {
            toggleVisibility.icon = guiProps.toggleExpertIcon
        } else {
            toggleVisibility.icon = guiProps.toggleHelpIcon
        }
    }

    fun setInclusionsFromStringList(entries: List<String>) {
        val specifications = mutableListOf<InclusionSpecification>()
        for (entry in entries) {
            specifications.add(InclusionSpecification(entry))
        }
        setServerFiles(specifications)
    }

    fun setServerFiles(entries: List<InclusionSpecification>) {
        inclusionModel.clear()
        inclusionModel.addAll(entries)
        list.updateUI()
        configEditor.validateInputFields()
    }

    fun getServerFiles(): MutableList<InclusionSpecification> {
        return inclusionModel.elements().toList().toMutableList()
    }

    fun addEntry(entry: String) {
        addEntry(InclusionSpecification(entry))
    }

    fun addEntry(entry: InclusionSpecification) {
        inclusionModel.addElement(entry)
        list.selectedIndex = list.lastVisibleIndex
    }

    fun removeEntry(index: Int): InclusionSpecification {
        return inclusionModel.remove(index)
    }

    private fun removeSelectedEntry() {
        var selected = list.selectedIndex
        removeEntry(list.selectedIndex)
        if (selected++ < list.lastVisibleIndex) {
            list.selectedIndex = --selected
        } else {
            list.selectedIndex = list.lastVisibleIndex
        }
    }

    private fun selectInclusions() {
        val serverFilesChooser = if (File(configEditor.getModpackDirectory()).isDirectory) {
            ServerFilesChooser(File(configEditor.getModpackDirectory()), chooserDimension)
        } else {
            ServerFilesChooser(chooserDimension)
        }
        if (serverFilesChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            val selection: Array<File> = serverFilesChooser.selectedFiles
            val serverFiles: MutableList<InclusionSpecification> = mutableListOf()
            serverFiles.addAll(getServerFiles())
            for (entry in selection) {
                if (entry.path.startsWith(configEditor.getModpackDirectory())) {
                    val cleaned = entry.path.replace(configEditor.getModpackDirectory() + File.separator, "")
                    serverFiles.add(InclusionSpecification(cleaned))
                } else {
                    serverFiles.add(InclusionSpecification(entry.absolutePath))
                }
            }
            setServerFiles(serverFiles.toMutableList())
            log.debug("Selected directories: $serverFiles")
        }
    }

    /**
     * Reverts the list of copy directories to the value of the last loaded configuration, if one is
     * available.
     *
     * @author Griefed
     */
    private fun revertInclusions() {
        if (configEditor.lastSavedConfig != null) {
            configEditor.setInclusions(configEditor.lastSavedConfig!!.inclusions)
            configEditor.validateInputFields()
        }
    }
}