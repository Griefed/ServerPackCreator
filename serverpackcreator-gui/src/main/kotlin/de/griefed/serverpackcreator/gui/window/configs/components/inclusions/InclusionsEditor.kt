/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.gui.window.configs.components.inclusions

import Gui
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.InclusionSpecification
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditor
import de.griefed.serverpackcreator.gui.window.configs.components.DocumentChangeListener
import de.griefed.serverpackcreator.gui.window.configs.components.ElementLabel
import de.griefed.serverpackcreator.gui.window.configs.components.ScrollTextField
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.util.regex.PatternSyntaxException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.ListSelectionEvent

/**
 * Editor for [InclusionSpecification] for a given server pack. This editor allows you to specify the
 * * source
 * * destination
 * * inclusion-filter
 * * exclusion-filter
 *
 * A text-pane will also be updated when selecting an InclusionSpecification and show you the files which would be included
 * depending on your source and filters.
 *
 * @author Griefed
 */
class InclusionsEditor(
    private val chooserDimension: Dimension,
    private val guiProps: GuiProps,
    private val configEditor: ConfigEditor,
    private val apiWrapper: ApiWrapper,
    private val source: ScrollTextField,
    private val destination: ScrollTextField,
    private val inclusionFilter: ScrollTextField,
    private val exclusionFilter: ScrollTextField
) : JSplitPane(HORIZONTAL_SPLIT) {
    private val log = cachedLoggerOf(this.javaClass)
    private val inclusionModel = DefaultListModel<InclusionSpecification>()
    private val list = JList(inclusionModel)
    private val sourceInfo = SourceInfo(guiProps)
    private val destinationInfo = DestinationInfo(guiProps)
    private val inclusionInfo = InclusionInfo(guiProps)
    private val exclusionInfo = ExclusionInfo(guiProps)
    private val tip = InclusionTip(Gui.createserverpack_gui_inclusions_editor_tip_name.toString(), guiProps)
    private var selectedInclusion: InclusionSpecification? = null
    private val timer: Timer
    private val toggleVisibility = JToggleButton(guiProps.toggleHelpIcon)
    private val delay = 250
    private val expertPanel = JPanel(
        MigLayout(
            "left,wrap",
            "0[left,::64]5[left,::150]5[left,grow]0",
            "30"
        )
    )

    init {
        val listScroller = JScrollPane(list)
        val sourceLabel = ElementLabel(Gui.createserverpack_gui_inclusions_editor_source.toString())
        val destinationLabel = ElementLabel(Gui.createserverpack_gui_inclusions_editor_destination.toString())
        val inclusionLabel = ElementLabel(Gui.createserverpack_gui_inclusions_editor_inclusion.toString())
        val exclusionLabel = ElementLabel(Gui.createserverpack_gui_inclusions_editor_exclusion.toString())
        val leftPanel = JPanel(BorderLayout())
        val fileAdd = BalloonTipButton(
            null,
            guiProps.addIcon,
            Gui.createserverpack_gui_inclusions_editor_add.toString(),
            guiProps
        ) { addEntry("") }
        val fileRemove =
            BalloonTipButton(
                null,
                guiProps.deleteIcon,
                Gui.createserverpack_gui_inclusions_editor_delete.toString(),
                guiProps
            ) { removeSelectedEntry() }
        val filesShowBrowser = BalloonTipButton(
            null, guiProps.folderIcon, Gui.createserverpack_gui_browser.toString(), guiProps
        ) { selectInclusions() }
        val filesRevert = BalloonTipButton(
            null, guiProps.revertIcon, Gui.createserverpack_gui_buttoncopydirs_revert_tip.toString(), guiProps
        ) { revertInclusions() }
        val filesReset = BalloonTipButton(
            null, guiProps.resetIcon, Gui.createserverpack_gui_buttoncopydirs_reset_tip.toString(), guiProps
        ) { setInclusionsFromStringList(apiWrapper.apiProperties.directoriesToInclude.toMutableList()) }
        val rightPanel = JPanel(
            MigLayout(
                "left,wrap",
                "0[left,::64]5[left,::150]5[left,grow]5[left,::64]5[left,::64]0",
                "30"
            )
        )
        val sourceListener = object : DocumentChangeListener {
            override fun update(e: DocumentEvent) {
                sourceWasEdited()
            }
        }
        val destinationListener = object : DocumentChangeListener {
            override fun update(e: DocumentEvent) {
                destinationWasEdited()
            }
        }
        val inclusionListener = object : DocumentChangeListener {
            override fun update(e: DocumentEvent) {
                inclusionFilterWasEdited()
            }
        }
        val exclusionListener = object : DocumentChangeListener {
            override fun update(e: DocumentEvent) {
                exclusionFilterWasEdited()
            }
        }

        dividerLocation = 150
        setLeftComponent(leftPanel)
        setRightComponent(rightPanel)
        list.visibleRowCount = -1
        list.layoutOrientation = JList.HORIZONTAL_WRAP
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = InclusionSpecificationRenderer()
        list.addListSelectionListener { event -> selectionOccurred(event) }
        leftPanel.add(listScroller, BorderLayout.CENTER)
        tip.text = Gui.createserverpack_gui_inclusions_editor_tip_default.toString()
        expertPanel.isVisible = false
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
        rightPanel.add(tip, "cell 1 1 2 3, grow, w 50:50:, h 150:200:, hidemode 3")
        rightPanel.add(filesShowBrowser, "cell 3 0")
        rightPanel.add(filesRevert, "cell 3 1")
        rightPanel.add(filesReset, "cell 3 2")
        source.addDocumentListener(sourceListener)
        destination.addDocumentListener(destinationListener)
        inclusionFilter.addDocumentListener(inclusionListener)
        exclusionFilter.addDocumentListener(exclusionListener)
        timer = Timer(delay) {
            updateTip()
        }
        timer.stop()
        timer.delay = delay
        timer.isRepeats = false
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun updateTip() {
        GlobalScope.launch(guiProps.fileBrowserDispatcher) {
            try {
                if (source.text.isBlank()) {
                    tip.text = ""
                    return@launch
                }
                if (selectedInclusion!!.isGlobalFilter()) {
                    tip.text = if (selectedInclusion!!.hasInclusionFilter()) {
                        Gui.createserverpack_gui_inclusions_editor_tip_global_inclusions.toString()
                    } else {
                        Gui.createserverpack_gui_inclusions_editor_tip_global_exclusions(selectedInclusion!!.exclusionFilter!!)
                    }
                    return@launch
                }
                val acquired = apiWrapper.serverPackHandler!!.getServerFiles(
                    selectedInclusion!!,
                    configEditor.getModpackDirectory(),
                    apiWrapper.serverPackHandler!!.getServerPackDestination(configEditor.getCurrentConfiguration()),
                    mutableListOf(),
                    configEditor.getClientSideModsList(),
                    configEditor.getMinecraftVersion(),
                    configEditor.getModloader()
                )
                var tipContent = Gui.createserverpack_gui_inclusions_editor_tip_prefix.toString()
                for (file in acquired) {
                    tipContent += file.sourceFile.absolutePath.replace(
                        configEditor.getModpackDirectory() + File.separator,
                        ""
                    ) + "\n"
                }
                tip.text = tipContent
                //tip.grabFocus()
                try {
                    tip.updateUI()
                } catch (_: NullPointerException) {}
            } catch (ex: Exception) {
                log.error("Couldn't acquire files to include. ", ex)
            }
        }
    }

    fun updateIndex() {
        list.selectedIndex = 0
    }

    /**
     * @author Griefed
     */
    fun sourceWasEdited() {
        if (File(configEditor.getModpackDirectory(), source.text).exists() || File(source.text).exists()) {
            list.selectedValue.source = source.text
            timer.restart()
            list.updateUI()
            sourceInfo.info()
        } else {
            timer.stop()
            sourceInfo.error(Gui.createserverpack_gui_inclusions_editor_source_error(source.text))
        }
        configEditor.validateInputFields()
    }

    /**
     * @author Griefed
     */
    fun destinationWasEdited() {
        if (apiWrapper.stringUtilities.checkForInvalidPathCharacters(destination.text)) {
            list.selectedValue.destination = destination.text
            destinationInfo.info()
        } else {
            timer.stop()
            destinationInfo.error(Gui.createserverpack_gui_inclusions_editor_destination_error(destination.text))
        }
    }

    /**
     * @author Griefed
     */
    fun inclusionFilterWasEdited() {
        try {
            inclusionFilter.text.toRegex()
            list.selectedValue.inclusionFilter = inclusionFilter.text
            timer.restart()
            inclusionInfo.info()
        } catch (ex: PatternSyntaxException) {
            timer.stop()
            var exception = ex.message ?: ex.description
            exception = exception
                .replace("\t", "%20")
                .replace("\n", "<br>")
                .replace(" ", "&nbsp;")
            inclusionInfo.error("<html>${Gui.createserverpack_gui_inclusions_editor_filter_error(exception)}</html>")
        }
    }

    /**
     * @author Griefed
     */
    fun exclusionFilterWasEdited() {
        try {
            exclusionFilter.text.toRegex()
            list.selectedValue.exclusionFilter = exclusionFilter.text
            timer.restart()
            exclusionInfo.info()
        } catch (ex: PatternSyntaxException) {
            timer.stop()
            var exception = ex.message ?: ex.description
            exception = exception
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                .replace("\n", "<br>")
                .replace(" ", "&nbsp;")
            exclusionInfo.error("<html>${Gui.createserverpack_gui_inclusions_editor_filter_error(exception)}</html>")
        }
    }

    /**
     * @author Griefed
     */
    private fun selectionOccurred(event: ListSelectionEvent) {
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

    /**
     * @author Griefed
     */
    private fun toggleVisibility() {
        expertPanel.isVisible = !expertPanel.isVisible
        tip.isVisible = !tip.isVisible
        if (expertPanel.isVisible) {
            toggleVisibility.icon = guiProps.toggleExpertIcon
        } else {
            toggleVisibility.icon = guiProps.toggleHelpIcon
        }
    }

    /**
     * @author Griefed
     */
    private fun setInclusionsFromStringList(entries: List<String>) {
        val specifications = mutableListOf<InclusionSpecification>()
        for (entry in entries) {
            specifications.add(InclusionSpecification(entry))
        }
        setServerFiles(specifications)
    }

    /**
     * @author Griefed
     */
    fun setServerFiles(entries: List<InclusionSpecification>) {
        inclusionModel.clear()
        inclusionModel.addAll(entries)
        list.updateUI()
        configEditor.validateInputFields()
    }

    /**
     * @author Griefed
     */
    fun getServerFiles(): MutableList<InclusionSpecification> {
        return inclusionModel.elements().toList().toMutableList()
    }

    /**
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun addEntry(entry: String) {
        addEntry(InclusionSpecification(entry))
    }

    /**
     * @author Griefed
     */
    private fun addEntry(entry: InclusionSpecification) {
        inclusionModel.addElement(entry)
        list.selectedIndex = list.lastVisibleIndex
    }

    /**
     * @author Griefed
     */
    private fun removeEntry(index: Int): InclusionSpecification {
        return inclusionModel.remove(index)
    }

    /**
     * @author Griefed
     */
    private fun removeSelectedEntry() {
        var selected = list.selectedIndex
        removeEntry(list.selectedIndex)
        if (selected++ < list.lastVisibleIndex) {
            list.selectedIndex = --selected
        } else {
            list.selectedIndex = list.lastVisibleIndex
        }
    }

    /**
     * @author Griefed
     */
    private fun selectInclusions() {
        val inclusionSourceChooser = if (File(configEditor.getModpackDirectory()).isDirectory) {
            InclusionSourceChooser(File(configEditor.getModpackDirectory()), chooserDimension)
        } else {
            InclusionSourceChooser(chooserDimension)
        }
        if (inclusionSourceChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            val selection: Array<File> = inclusionSourceChooser.selectedFiles
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
     * @author Griefed
     */
    private fun revertInclusions() {
        if (configEditor.lastSavedConfig != null) {
            configEditor.setInclusions(configEditor.lastSavedConfig!!.inclusions)
            configEditor.validateInputFields()
        }
    }
}