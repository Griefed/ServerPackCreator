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
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditor
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.regex.PatternSyntaxException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
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
    private val expertPanel = JPanel(
        MigLayout(
            "left,wrap",
            "0[left,::64]5[left,::150]5[left,grow]0",
            "30"
        )
    )
    private val rightPanel = JPanel(
        MigLayout(
            "left,wrap",
            "0[left,::64]5[left,::150]5[left,grow]5[left,::64]5[left,::64]0",
            "30"
        )
    )
    private val leftPanel = JPanel(BorderLayout())
    private val inclusionModel = DefaultListModel<InclusionSpecification>()
    private val list = JList(inclusionModel)
    private val listScroller = JScrollPane(list)

    private val sourceIcon = StatusIcon(guiProps,Gui.createserverpack_gui_inclusions_editor_source_info.toString())
    private val sourceLabel = ElementLabel(Gui.createserverpack_gui_inclusions_editor_source.toString())
    private val sourceAdd = BalloonTipButton(null, guiProps.folderIcon, Gui.settings_gui_manualedit_select.toString(), guiProps) { selectSource() }

    private val destinationIcon = StatusIcon(guiProps,Gui.createserverpack_gui_inclusions_editor_destination_info.toString())
    private val destinationLabel = ElementLabel(Gui.createserverpack_gui_inclusions_editor_destination.toString())

    private val inclusionIcon = StatusIcon(guiProps,Gui.createserverpack_gui_inclusions_editor_inclusion_info.toString())
    private val inclusionLabel = ElementLabel(Gui.createserverpack_gui_inclusions_editor_inclusion.toString())

    private val exclusionIcon = StatusIcon(guiProps,Gui.createserverpack_gui_inclusions_editor_exclusion_info.toString())
    private val exclusionLabel = ElementLabel(Gui.createserverpack_gui_inclusions_editor_exclusion.toString())

    private val toggleVisibility = JToggleButton(guiProps.toggleHelpIcon)
    private val fileAdd = BalloonTipButton(null, guiProps.addIcon,Gui.createserverpack_gui_inclusions_editor_add.toString(), guiProps) { addEntry("") }
    private val fileRemove = BalloonTipButton(null, guiProps.deleteIcon,Gui.createserverpack_gui_inclusions_editor_delete.toString(), guiProps) { removeSelectedEntry() }
    private val tip = InclusionTip(Gui.createserverpack_gui_inclusions_editor_tip_name.toString(), guiProps)
    private val filesShowBrowser = BalloonTipButton(null, guiProps.folderIcon, Gui.createserverpack_gui_browser.toString(), guiProps) { selectInclusions() }
    private val filesRevert = BalloonTipButton(null, guiProps.revertIcon, Gui.createserverpack_gui_buttoncopydirs_revert_tip.toString(), guiProps) { revertInclusions() }
    private val filesReset = BalloonTipButton(null, guiProps.resetIcon, Gui.createserverpack_gui_buttoncopydirs_reset_tip.toString(), guiProps) { setInclusionsFromStringList(apiWrapper.apiProperties.directoriesToInclude.toMutableList()) }

    private var selectedInclusion: InclusionSpecification? = null
    private val delay = 250
    private val tipUpdateTimer: Timer = Timer(delay) { updateTip() }

    private val sourceListener = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            sourceWasEdited()
        }
    }
    private val destinationListener = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            destinationWasEdited()
        }
    }
    private val inclusionListener = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            inclusionFilterWasEdited()
        }
    }
    private val exclusionListener = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            exclusionFilterWasEdited()
        }
    }

    private val listMouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            super.mouseClicked(e)
            clearSelection(e)
        }

        override fun mousePressed(e: MouseEvent) {
            super.mousePressed(e)
            clearSelection(e)
        }

        override fun mouseReleased(e: MouseEvent) {
            super.mouseReleased(e)
            clearSelection(e)
        }

        fun clearSelection(e: MouseEvent) {
            val index = list.locationToIndex(e.point)
            val bounds = list.getCellBounds(index, index)
            if (bounds == null || !bounds.contains(e.point) || list.model.size <= 0) {
                emtpySelection()
            } else {
                enableInputs()
            }
        }
    }

    private val listModelDataAdapter = object : ListDataListener {
        override fun intervalAdded(e: ListDataEvent?) {

            checkSize()
        }

        override fun intervalRemoved(e: ListDataEvent?) {
            checkSize()
        }

        override fun contentsChanged(e: ListDataEvent?) {
            checkSize()
        }

        fun checkSize() {
            if (list.model.size <= 0 || list.isSelectionEmpty) {
                emtpySelection()
            }
        }
    }

    init {
        source.isEditable = guiProps.allowManualEditing
        tipUpdateTimer.stop()
        tipUpdateTimer.delay = delay
        tipUpdateTimer.isRepeats = false
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

        expertPanel.add(destinationIcon, "cell 0 0")
        expertPanel.add(destinationLabel, "cell 1 0")
        expertPanel.add(destination, "cell 2 0, grow,w 50:50:")
        expertPanel.add(inclusionIcon, "cell 0 1")
        expertPanel.add(inclusionLabel, "cell 1 1")
        expertPanel.add(inclusionFilter, "cell 2 1, grow,w 50:50:")
        expertPanel.add(exclusionIcon, "cell 0 2")
        expertPanel.add(exclusionLabel, "cell 1 2")
        expertPanel.add(exclusionFilter, "cell 2 2, grow,w 50:50:")

        rightPanel.add(sourceIcon, "cell 0 0")
        rightPanel.add(toggleVisibility, "cell 0 1")
        rightPanel.add(fileAdd, "cell 0 2")
        rightPanel.add(fileRemove, "cell 0 3")
        rightPanel.add(sourceLabel, "cell 1 0")
        rightPanel.add(source, "cell 2 0, grow, w 50:50:")
        rightPanel.add(sourceAdd, "cell 2 0")
        rightPanel.add(expertPanel, "cell 1 1 2 3, grow, w 50:50:, hidemode 3")
        rightPanel.add(tip, "cell 1 1 2 3, grow, w 50:50:, h 150:200:, hidemode 3")
        rightPanel.add(filesShowBrowser, "cell 3 0")
        rightPanel.add(filesRevert, "cell 3 1")
        rightPanel.add(filesReset, "cell 3 2")
        source.addDocumentListener(sourceListener)
        destination.addDocumentListener(destinationListener)
        inclusionFilter.addDocumentListener(inclusionListener)
        exclusionFilter.addDocumentListener(exclusionListener)

        list.addMouseListener(listMouseAdapter)
        list.model.addListDataListener(listModelDataAdapter)
        if (list.model.size <= 0) {
            emtpySelection()
        }
    }

    private fun emtpySelection() {
        if (list.model.size > 0 || list.valueIsAdjusting) {
            return
        }
        list.clearSelection()
        tip.text = Gui.createserverpack_gui_inclusions_editor_tip_default.toString()
        source.isEditable = guiProps.allowManualEditing
        destination.isEditable = false
        inclusionFilter.isEditable = false
        exclusionFilter.isEditable = false
        source.text = ""
        destination.text = ""
        inclusionFilter.text = ""
        exclusionFilter.text = ""
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun updateTip() {
        GlobalScope.launch(guiProps.miscDispatcher) {
            //TODO prevent selection during scanning
            //TODO check why sometimes gui errors during resize....
            tip.isEnabled = false
            list.isEnabled = false
            tip.text = Gui.createserverpack_gui_inclusions_editor_tip_updating.toString()
            tip.updateUI()
            val inclusionSelection = selectedInclusion!!
            var tipContent = ""
            try {
                if (inclusionSelection.source.isBlank()) {
                    tipContent = Gui.createserverpack_gui_inclusions_editor_tip_blank.toString()
                } else if (!File(configEditor.getModpackDirectory(), inclusionSelection.source).exists() && !File(inclusionSelection.source).exists()) {
                    tipContent = Gui.createserverpack_gui_inclusions_editor_tip_invalid.toString()
                } else if (inclusionSelection.isGlobalFilter()) {
                    tipContent = if (inclusionSelection.hasInclusionFilter()) {
                        Gui.createserverpack_gui_inclusions_editor_tip_global_inclusions.toString()
                    } else {
                        Gui.createserverpack_gui_inclusions_editor_tip_global_exclusions(inclusionSelection.exclusionFilter!!)
                    }
                } else {
                    tipContent = Gui.createserverpack_gui_inclusions_editor_tip_prefix.toString()
                    val prefix = if (!inclusionSelection.destination.isNullOrBlank()) {
                        inclusionSelection.destination + File.separator
                    } else {
                        ""
                    }
                    if (File(configEditor.getModpackDirectory(), inclusionSelection.source).isFile) {
                        tipContent += File(configEditor.getModpackDirectory(), inclusionSelection.source).absolutePath.replace(
                            configEditor.getModpackDirectory() + File.separator,
                            ""
                        ) + "\n"
                    } else if (File(inclusionSelection.source).isFile) {
                        tipContent += File(inclusionSelection.source).absolutePath + "\n"
                    } else {
                        val acquired = apiWrapper.serverPackHandler!!.getServerFiles(
                            inclusionSelection,
                            configEditor.getModpackDirectory(),
                            apiWrapper.serverPackHandler!!.getServerPackDestination(configEditor.getCurrentConfiguration()),
                            mutableListOf(),
                            configEditor.getClientSideModsList(),
                            configEditor.getWhitelistList(),
                            configEditor.getMinecraftVersion(),
                            configEditor.getModloader()
                        )
                        for (file in acquired) {
                            tipContent += file.sourceFile.absolutePath.replace(
                                configEditor.getModpackDirectory() + File.separator,
                                prefix
                            ) + "\n"
                        }

                    }
                }
            } catch (_: ArrayIndexOutOfBoundsException) {
            } catch (ex: Exception) {
                log.error("Couldn't acquire files to include for ${inclusionSelection.source}. ", ex)
            }
            if (inclusionSelection == selectedInclusion) {
                try {
                    tip.text = tipContent
                    tip.updateUI()
                } catch (_: NullPointerException) {
                }
                tip.isEnabled = true
                list.isEnabled = true
            } else {
                updateTip()
            }
        }
    }

    /**
     * @author Griefed
     */
    fun updateIndex() {
        if (!list.valueIsAdjusting) {
            list.selectedIndex = 0
            enableInputs()
        }
    }

    private fun enableInputs() {
        source.isEditable = guiProps.allowManualEditing
        destination.isEditable = true
        inclusionFilter.isEditable = true
        exclusionFilter.isEditable = true
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun sourceWasEdited() {
        GlobalScope.launch(Dispatchers.Swing) {
            delay(200)
            if (list.model.size > 0 && !list.isSelectionEmpty && !list.valueIsAdjusting) {
                if (File(configEditor.getModpackDirectory(), source.text).exists() || File(source.text).exists()) {
                    list.selectedValue.source = source.text
                    tipUpdateTimer.restart()
                    list.updateUI()
                    sourceIcon.info()
                } else {
                    tipUpdateTimer.stop()
                    sourceIcon.error(Gui.createserverpack_gui_inclusions_editor_source_error(source.text))
                }
                configEditor.validateInputFields()
            }
        }
    }

    /**
     * @author Griefed
     */
    fun destinationWasEdited() {
        if (list.model.size > 0 && !list.isSelectionEmpty) {
            if (apiWrapper.stringUtilities.checkForInvalidPathCharacters(destination.text)) {
                list.selectedValue.destination = destination.text
                destinationIcon.info()
                list.updateUI()
            } else {
                tipUpdateTimer.stop()
                destinationIcon.error(Gui.createserverpack_gui_inclusions_editor_destination_error(destination.text))
            }
        }
    }

    /**
     * @author Griefed
     */
    fun inclusionFilterWasEdited() {
        if (list.model.size > 0 && !list.isSelectionEmpty) {
            try {
                inclusionFilter.text.toRegex()
                list.selectedValue.inclusionFilter = inclusionFilter.text
                tipUpdateTimer.restart()
                inclusionIcon.info()
                list.updateUI()
            } catch (ex: PatternSyntaxException) {
                tipUpdateTimer.stop()
                var exception = ex.message ?: ex.description
                exception = exception
                    .replace("\t", "%20")
                    .replace("\n", "<br>")
                    .replace(" ", "&nbsp;")
                inclusionIcon.error("<html>${Gui.createserverpack_gui_inclusions_editor_filter_error(exception)}</html>")
            }
        }
    }

    /**
     * @author Griefed
     */
    fun exclusionFilterWasEdited() {
        if (list.model.size > 0 && !list.isSelectionEmpty) {
            try {
                exclusionFilter.text.toRegex()
                list.selectedValue.exclusionFilter = exclusionFilter.text
                tipUpdateTimer.restart()
                exclusionIcon.info()
                list.updateUI()
            } catch (ex: PatternSyntaxException) {
                tipUpdateTimer.stop()
                var exception = ex.message ?: ex.description
                exception = exception
                    .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                    .replace("\n", "<br>")
                    .replace(" ", "&nbsp;")
                exclusionIcon.error("<html>${Gui.createserverpack_gui_inclusions_editor_filter_error(exception)}</html>")
            }
        }
    }

    /**
     * @author Griefed
     */
    private fun selectionOccurred(event: ListSelectionEvent) {
        when {
            list.valueIsAdjusting -> return
            event.valueIsAdjusting -> return
            list.selectedIndex == -1 || list.model.size <= 0 -> {
                emtpySelection()
                return
            }
        }
        selectedInclusion = list.selectedValue
        source.text = selectedInclusion!!.source
        destination.text = selectedInclusion!!.destination ?: ""
        inclusionFilter.text = selectedInclusion!!.inclusionFilter ?: ""
        exclusionFilter.text = selectedInclusion!!.exclusionFilter ?: ""
        tipUpdateTimer.restart()
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
        validate()
    }

    override fun validate() {
        super.validate()
        list.updateUI()
        listScroller.updateUI()
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
        if (list.model.size == 1) {
            list.selectedIndex = 0
        }
        validate()
    }

    /**
     * @author Griefed
     */
    private fun removeEntry(index: Int): InclusionSpecification {
        val removed = inclusionModel.remove(index)
        if (list.model.size == 1) {
            list.selectedIndex = 0
        }
        if (list.model.size <= 0) {
            emtpySelection()
        }
        validate()
        return removed
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
                serverFiles.add(createInclusionSpec(entry))
            }
            setServerFiles(serverFiles.toMutableList())
            log.debug("Selected directories: $serverFiles")
        }
        validate()
    }

    private fun selectSource() {
        if (selectedInclusion == null) {
            return
        }
        val inclusionSourceChooser = if (File(configEditor.getModpackDirectory()).isDirectory) {
            InclusionSourceChooser(File(configEditor.getModpackDirectory()), chooserDimension)
        } else {
            InclusionSourceChooser(chooserDimension)
        }
        inclusionSourceChooser.isMultiSelectionEnabled = false
        if (inclusionSourceChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            source.text = createInclusionSpec(inclusionSourceChooser.selectedFile).source
        }
        validate()
    }

    /**
     * @author Griefed
     */
    private fun createInclusionSpec(sourceFile: File): InclusionSpecification {
        return if (sourceFile.path.startsWith(configEditor.getModpackDirectory())) {
            val cleaned = sourceFile.path.replace(configEditor.getModpackDirectory() + File.separator, "")
            InclusionSpecification(cleaned)
        } else {
            InclusionSpecification(sourceFile.absolutePath)
        }
    }

    /**
     * @author Griefed
     */
    private fun revertInclusions() {
        if (configEditor.lastConfig != null) {
            configEditor.setInclusions(configEditor.lastConfig!!.inclusions)
            configEditor.validateInputFields()
        }
        validate()
    }

    /**
     * @author Griefed
     */
    fun saveSuggestions() {
        val sourceSuggestions = source.suggestionProvider!!.allSuggestions()
        val destinationSuggestions = destination.suggestionProvider!!.allSuggestions()
        val inclusionSuggestions = inclusionFilter.suggestionProvider!!.allSuggestions()
        val exclusionSuggestions = exclusionFilter.suggestionProvider!!.allSuggestions()
        for (spec in getServerFiles()) {
            sourceSuggestions.add(spec.source)
            spec.destination?.let { destinationSuggestions.add(it) }
            spec.inclusionFilter?.let { inclusionSuggestions.add(it) }
            spec.exclusionFilter?.let { exclusionSuggestions.add(it) }
        }
        sourceSuggestions.removeIf { entry -> entry.isBlank() }
        destinationSuggestions.removeIf { entry -> entry.isBlank() }
        inclusionSuggestions.removeIf { entry -> entry.isBlank() }
        exclusionSuggestions.removeIf { entry -> entry.isBlank() }
        guiProps.storeGuiProperty(
            "autocomplete.${source.identifier}",
            sourceSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        guiProps.storeGuiProperty(
            "autocomplete.${destination.identifier}",
            destinationSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        guiProps.storeGuiProperty(
            "autocomplete.${inclusionFilter.identifier}",
            inclusionSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })

        guiProps.storeGuiProperty(
            "autocomplete.${exclusionFilter.identifier}",
            exclusionSuggestions.joinToString(",") { entry -> entry.trim { it <= ' ' } }.trim { it <= ' ' })
    }
}