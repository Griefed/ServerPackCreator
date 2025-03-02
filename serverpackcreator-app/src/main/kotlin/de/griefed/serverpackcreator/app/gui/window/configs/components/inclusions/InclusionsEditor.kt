/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.app.gui.window.configs.components.inclusions

import Translations
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.*
import de.griefed.serverpackcreator.app.gui.window.configs.ConfigEditor
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionListener
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
    private val exclusionFilter: ScrollTextField,
    exclusionSettings: ScrollTextArea,
    whitelistSettings: ScrollTextArea
) : JSplitPane(HORIZONTAL_SPLIT) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val expertInclusionSettingsPanel = JPanel(
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
    private val inclusionList = JList(inclusionModel)
    private val listScroller = JScrollPane(inclusionList)

    private val sourceIcon = StatusIcon(guiProps,Translations.createserverpack_gui_inclusions_editor_source_info.toString())
    private val sourceLabel = ElementLabel(Translations.createserverpack_gui_inclusions_editor_source.toString())
    private val sourceSelect = BalloonTipButton(null, guiProps.folderIcon, Translations.settings_gui_manualedit_select.toString(), guiProps) { selectSource() }

    private val destinationIcon = StatusIcon(guiProps,Translations.createserverpack_gui_inclusions_editor_destination_info.toString())
    private val destinationLabel = ElementLabel(Translations.createserverpack_gui_inclusions_editor_destination.toString())

    private val inclusionIcon = StatusIcon(guiProps,Translations.createserverpack_gui_inclusions_editor_inclusion_info.toString())
    private val inclusionLabel = ElementLabel(Translations.createserverpack_gui_inclusions_editor_inclusion.toString())

    private val exclusionIcon = StatusIcon(guiProps,Translations.createserverpack_gui_inclusions_editor_exclusion_info.toString())
    private val exclusionLabel = ElementLabel(Translations.createserverpack_gui_inclusions_editor_exclusion.toString())

    private val toggleDetailsDisplay = JToggleButton(guiProps.toggleHelpIcon)
    private val addInclEntry = BalloonTipButton(null, guiProps.entriesAddIcon,Translations.createserverpack_gui_inclusions_editor_add.toString(), guiProps) { addEntry("") }
    private val remInclEntry = BalloonTipButton(null, guiProps.entriesRemoveIcon,Translations.createserverpack_gui_inclusions_editor_delete.toString(), guiProps) { removeSelectedEntry() }
    private val selectedInclusionDetailsScrollPanel = SelectedInclusionDetails(Translations.createserverpack_gui_inclusions_editor_tip_name.toString(), guiProps, exclusionSettings, whitelistSettings, inclusionList)
    private val inclusionsSelection = BalloonTipButton(null, guiProps.folderAddIcon, Translations.createserverpack_gui_inclusions_editor_select.toString(), guiProps) { selectInclusions() }
    private val inclusionsRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.createserverpack_gui_buttoncopydirs_revert_tip.toString(), guiProps) { revertInclusions() }
    private val inclusionsReset = BalloonTipButton(null, guiProps.resetIcon, Translations.createserverpack_gui_buttoncopydirs_reset_tip.toString(), guiProps) { setInclusionsFromStringList(apiWrapper.apiProperties.directoriesToInclude.toMutableList()) }

    private var selectedInclusion: InclusionSpecification? = null
    private val tipUpdateTimer: TipUpdateTimer = TipUpdateTimer(500)

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
            val index = inclusionList.locationToIndex(e.point)
            val bounds = inclusionList.getCellBounds(index, index)
            if (bounds == null || !bounds.contains(e.point) || inclusionList.model.size <= 0) {
                emptySelection()
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
            if (inclusionList.model.size <= 0 || inclusionList.isSelectionEmpty) {
                emptySelection()
            }
        }
    }

    init {
        source.isEditable = guiProps.allowManualEditing
        dividerLocation = 150
        setLeftComponent(leftPanel)
        setRightComponent(rightPanel)
        inclusionList.visibleRowCount = -1
        inclusionList.layoutOrientation = JList.HORIZONTAL_WRAP
        inclusionList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        inclusionList.cellRenderer = InclusionSpecificationRenderer()
        inclusionList.addListSelectionListener { event -> selectionOccurred(event) }
        inclusionList.dropMode = DropMode.INSERT
        inclusionList.transferHandler = InclusionsListHandler(this)

        leftPanel.add(listScroller, BorderLayout.CENTER)
        selectedInclusionDetailsScrollPanel.text = Translations.createserverpack_gui_inclusions_editor_tip_default.toString()
        expertInclusionSettingsPanel.isVisible = false
        toggleDetailsDisplay.addActionListener { toggleVisibility() }

        expertInclusionSettingsPanel.add(destinationIcon, "cell 0 0")
        expertInclusionSettingsPanel.add(destinationLabel, "cell 1 0")
        expertInclusionSettingsPanel.add(destination, "cell 2 0, grow,w 50:50:")
        expertInclusionSettingsPanel.add(inclusionIcon, "cell 0 1")
        expertInclusionSettingsPanel.add(inclusionLabel, "cell 1 1")
        expertInclusionSettingsPanel.add(inclusionFilter, "cell 2 1, grow,w 50:50:")
        expertInclusionSettingsPanel.add(exclusionIcon, "cell 0 2")
        expertInclusionSettingsPanel.add(exclusionLabel, "cell 1 2")
        expertInclusionSettingsPanel.add(exclusionFilter, "cell 2 2, grow,w 50:50:")

        rightPanel.add(sourceIcon, "cell 0 0")
        rightPanel.add(toggleDetailsDisplay, "cell 0 1")
        rightPanel.add(inclusionsSelection, "cell 0 2")
        rightPanel.add(addInclEntry, "cell 0 3")
        rightPanel.add(remInclEntry, "cell 0 4")
        rightPanel.add(sourceLabel, "cell 1 0")
        rightPanel.add(source, "cell 2 0 2 1, grow, w 50:50:")
        rightPanel.add(sourceSelect, "cell 2 0")
        rightPanel.add(expertInclusionSettingsPanel, "cell 1 1 2 4, grow, w 50:50:, h 150:200:, hidemode 3")
        rightPanel.add(selectedInclusionDetailsScrollPanel, "cell 1 1 2 4, grow, w 50:50:, h 150:200:, hidemode 3")

        rightPanel.add(inclusionsRevert, "cell 3 2")
        rightPanel.add(inclusionsReset, "cell 3 3")
        source.addDocumentListener(sourceListener)
        destination.addDocumentListener(destinationListener)
        inclusionFilter.addDocumentListener(inclusionListener)
        exclusionFilter.addDocumentListener(exclusionListener)

        inclusionList.addMouseListener(listMouseAdapter)
        inclusionList.model.addListDataListener(listModelDataAdapter)
        if (inclusionList.model.size <= 0) {
            emptySelection()
        }
    }

    private fun emptySelection() {
        if (inclusionList.model.size > 0 || inclusionList.valueIsAdjusting) {
            return
        }
        inclusionList.clearSelection()
        selectedInclusionDetailsScrollPanel.text = Translations.createserverpack_gui_inclusions_editor_tip_default.toString()
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
            selectedInclusionDetailsScrollPanel.isEnabled = false
            inclusionList.isEnabled = false
            selectedInclusionDetailsScrollPanel.text = Translations.createserverpack_gui_inclusions_editor_tip_updating.toString()
            selectedInclusionDetailsScrollPanel.updateUI()
            val inclusionSelection = selectedInclusion!!
            var tipContent = ""
            try {
                if (inclusionSelection.source.isBlank()) {
                    tipContent = Translations.createserverpack_gui_inclusions_editor_tip_blank.toString()
                } else if (!File(configEditor.getModpackDirectory(), inclusionSelection.source).exists() && !File(inclusionSelection.source).exists()) {
                    tipContent = Translations.createserverpack_gui_inclusions_editor_tip_invalid.toString()
                } else if (inclusionSelection.isGlobalFilter()) {
                    tipContent = if (inclusionSelection.hasInclusionFilter()) {
                        Translations.createserverpack_gui_inclusions_editor_tip_global_inclusions.toString()
                    } else {
                        Translations.createserverpack_gui_inclusions_editor_tip_global_exclusions(inclusionSelection.exclusionFilter!!)
                    }
                } else {
                    tipContent = Translations.createserverpack_gui_inclusions_editor_tip_prefix.toString()

                    /*val prefix = if (!inclusionSelection.destination.isNullOrBlank()) {
                        inclusionSelection.destination + File.separator
                    } else {
                        File(inclusionSelection.source).name + File.separator
                    }*/

                    if (File(configEditor.getModpackDirectory(), inclusionSelection.source).isFile) {
                        tipContent += File(configEditor.getModpackDirectory(), inclusionSelection.source).absolutePath.replace(
                            configEditor.getModpackDirectory() + File.separator,
                            ""
                        ) + "\n"
                    } else if (File(inclusionSelection.source).isFile) {
                        tipContent += File(inclusionSelection.source).absolutePath + "\n"
                    } else {
                        val acquired = apiWrapper.serverPackHandler.getServerFiles(
                            inclusionSelection,
                            configEditor.getModpackDirectory(),
                            apiWrapper.serverPackHandler.getServerPackDestination(configEditor.getCurrentConfiguration()),
                            mutableListOf(),
                            configEditor.getClientSideModsList(),
                            configEditor.getWhitelistList(),
                            configEditor.getMinecraftVersion(),
                            configEditor.getModloader()
                        )
                        for (file in acquired) {
                                    /*${prefix}*/
                            tipContent += "${file.sourcePath.toString().replace(configEditor.getModpackDirectory(),"")}\n"
                        }

                    }
                }
            } catch (_: ArrayIndexOutOfBoundsException) {
            } catch (ex: Exception) {
                log.error("Couldn't acquire files to include for ${inclusionSelection.source}. ", ex)
            }
            if (inclusionSelection == selectedInclusion) {
                try {
                    selectedInclusionDetailsScrollPanel.text = tipContent
                    selectedInclusionDetailsScrollPanel.updateUI()
                } catch (_: NullPointerException) {
                }
                selectedInclusionDetailsScrollPanel.isEnabled = true
                inclusionList.isEnabled = true
            } else {
                tipUpdateTimer.restart()
            }
        }
    }

    /**
     * @author Griefed
     */
    fun updateIndex() {
        if (!inclusionList.valueIsAdjusting) {
            inclusionList.selectedIndex = 0
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
            if (inclusionList.model.size > 0 && !inclusionList.isSelectionEmpty && !inclusionList.valueIsAdjusting) {
                if (File(configEditor.getModpackDirectory(), source.text).exists() || File(source.text).exists()) {
                    inclusionList.selectedValue.source = source.text
                    tipUpdateTimer.restart()
                    inclusionList.updateUI()
                    sourceIcon.info()
                } else {
                    tipUpdateTimer.stop()
                    sourceIcon.error(Translations.createserverpack_gui_inclusions_editor_source_error(source.text))
                }
                configEditor.validateInputFields()
            }
        }
    }

    /**
     * @author Griefed
     */
    fun destinationWasEdited() {
        if (inclusionList.model.size > 0 && !inclusionList.isSelectionEmpty) {
            if (StringUtilities.checkForInvalidPathCharacters(destination.text)) {
                inclusionList.selectedValue.destination = destination.text
                destinationIcon.info()
                inclusionList.updateUI()
            } else {
                tipUpdateTimer.stop()
                destinationIcon.error(Translations.createserverpack_gui_inclusions_editor_destination_error(destination.text))
            }
        }
    }

    /**
     * @author Griefed
     */
    fun inclusionFilterWasEdited() {
        if (inclusionList.model.size > 0 && !inclusionList.isSelectionEmpty) {
            try {
                inclusionFilter.text.toRegex()
                inclusionList.selectedValue.inclusionFilter = inclusionFilter.text
                tipUpdateTimer.restart()
                inclusionIcon.info()
                inclusionList.updateUI()
            } catch (ex: PatternSyntaxException) {
                tipUpdateTimer.stop()
                var exception = ex.message ?: ex.description
                exception = exception
                    .replace("\t", "%20")
                    .replace("\n", "<br>")
                    .replace(" ", "&nbsp;")
                inclusionIcon.error("<html>${Translations.createserverpack_gui_inclusions_editor_filter_error(exception)}</html>")
            }
        }
    }

    /**
     * @author Griefed
     */
    fun exclusionFilterWasEdited() {
        if (inclusionList.model.size > 0 && !inclusionList.isSelectionEmpty) {
            try {
                exclusionFilter.text.toRegex()
                inclusionList.selectedValue.exclusionFilter = exclusionFilter.text
                tipUpdateTimer.restart()
                exclusionIcon.info()
                inclusionList.updateUI()
            } catch (ex: PatternSyntaxException) {
                tipUpdateTimer.stop()
                var exception = ex.message ?: ex.description
                exception = exception
                    .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                    .replace("\n", "<br>")
                    .replace(" ", "&nbsp;")
                exclusionIcon.error("<html>${Translations.createserverpack_gui_inclusions_editor_filter_error(exception)}</html>")
            }
        }
    }

    /**
     * @author Griefed
     */
    private fun selectionOccurred(event: ListSelectionEvent) {
        when {
            inclusionList.valueIsAdjusting -> return
            event.valueIsAdjusting -> return
            inclusionList.selectedIndex == -1 || inclusionList.model.size <= 0 -> {
                emptySelection()
                return
            }
        }
        selectedInclusion = inclusionList.selectedValue
        source.text = selectedInclusion!!.source
        destination.text = selectedInclusion!!.destination ?: ""
        inclusionFilter.text = selectedInclusion!!.inclusionFilter ?: ""
        exclusionFilter.text = selectedInclusion!!.exclusionFilter ?: ""
        if (source.text.isNotBlank()) {
            remInclEntry.toolTipText = Translations.createserverpack_gui_inclusions_editor_delete_withselection(source.text)
        } else {
            remInclEntry.toolTipText = Translations.createserverpack_gui_inclusions_editor_delete.toString()
        }
        tipUpdateTimer.restart()
    }

    /**
     * @author Griefed
     */
    private fun toggleVisibility() {
        expertInclusionSettingsPanel.isVisible = !expertInclusionSettingsPanel.isVisible
        selectedInclusionDetailsScrollPanel.isVisible = !selectedInclusionDetailsScrollPanel.isVisible
        if (expertInclusionSettingsPanel.isVisible) {
            toggleDetailsDisplay.icon = guiProps.toggleExpertIcon
        } else {
            toggleDetailsDisplay.icon = guiProps.toggleHelpIcon
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
        for (entry in entries) {
            if (inclusionModel.toArray().all { (it as InclusionSpecification).source != entry.source }) {
                inclusionModel.addElement(entry)
            }
        }
        validate()
    }

    override fun validate() {
        super.validate()
        inclusionList.updateUI()
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
        addEntry(createInclusionSpec(File(entry)))
    }

    /**
     * @author Griefed
     */
    fun addEntry(entry: InclusionSpecification) {
        inclusionModel.addElement(entry)
        inclusionList.selectedIndex = inclusionList.lastVisibleIndex
        if (inclusionList.model.size == 1) {
            inclusionList.selectedIndex = 0
        }
        validate()
    }

    /**
     * @author Griefed
     */
    private fun removeEntry(index: Int): InclusionSpecification {
        val removed = inclusionModel.remove(index)
        if (inclusionList.model.size == 1) {
            inclusionList.selectedIndex = 0
        }
        if (inclusionList.model.size <= 0) {
            emptySelection()
        }
        validate()
        return removed
    }

    /**
     * @author Griefed
     */
    private fun removeSelectedEntry() {
        var selected = inclusionList.selectedIndex
        removeEntry(inclusionList.selectedIndex)
        if (selected++ < inclusionList.lastVisibleIndex) {
            inclusionList.selectedIndex = --selected
        } else {
            inclusionList.selectedIndex = inclusionList.lastVisibleIndex
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
            for (added in serverFiles) {
                log.debug("Selected directories: ${added.source}")
            }
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
    fun createInclusionSpec(sourceFile: File): InclusionSpecification {
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

    class InclusionsListHandler(private val editor: InclusionsEditor): TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDrop) {
                return false
            }
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }

            val transferable = support.transferable
            val files: List<File>
            try {
                files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
            } catch (e: Exception) {
                return false
            }

            val inclSpecs = files.map { editor.createInclusionSpec(it) }

            for (spec in inclSpecs) {
                editor.addEntry(spec)
            }

            return true
        }
    }

    /**
     * Timer responsible for updating the tip in the inclusions-editor.
     *
     * @author Griefed
     */
    inner class TipUpdateTimer(delay: Int) :
        Timer(delay, ActionListener {
            updateTip()
        }) {
        init {
            stop()
            isRepeats = false
        }
    }
}