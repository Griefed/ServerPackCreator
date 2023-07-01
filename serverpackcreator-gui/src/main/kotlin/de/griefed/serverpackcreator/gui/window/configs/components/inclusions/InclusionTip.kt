package de.griefed.serverpackcreator.gui.window.configs.components.inclusions

import Gui
import de.griefed.serverpackcreator.gui.GuiProps
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*
import javax.swing.text.DefaultHighlighter

class InclusionTip(
    name: String,
    private val guiProps: GuiProps,
    private val textPane: JTextPane = JTextPane(),
    verticalScrollbarVisibility: Int = VERTICAL_SCROLLBAR_ALWAYS,
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_NEVER
) : JScrollPane(
    textPane,
    verticalScrollbarVisibility,
    horizontalScrollbarVisibility
), KeyListener {

    private val loadingAnimation = LoadingAnimation()
    private val searchFor = JTextField(100)
    private val search = arrayOf<Any>(
        Gui.createserverpack_gui_textarea_search_message.toString(),
        searchFor
    )
    private val searchRegex = arrayOf<Any>(
        Gui.createserverpack_gui_textarea_search_regex_message.toString(),
        searchFor
    )

    init {
        setName(name)
        textPane.isEditable = false
        textPane.addKeyListener(this)
    }

    var text: String = ""
        set(value) {
            field = value
            textPane.text = value
        }
        get() {
            return textPane.text
        }

    override fun keyTyped(e: KeyEvent) {}

    override fun keyPressed(e: KeyEvent) {
        textPane.highlighter.removeAllHighlights()
        when {
            e.keyCode == KeyEvent.VK_F && e.isControlDown && !e.isShiftDown -> searchDialog()
            e.keyCode == KeyEvent.VK_F && e.isControlDown && e.isShiftDown -> searchRegexDialog()
        }
    }

    override fun keyReleased(e: KeyEvent) {}


    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun requestFocus(component: JComponent) {
        GlobalScope.launch {
            delay(250)
            component.requestFocus()
            component.grabFocus()
        }
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun searchDialog() {
        requestFocus(searchFor)
        if (JOptionPane.showConfirmDialog(
                parent,
                search,
                Gui.createserverpack_gui_textarea_search_title(name),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                guiProps.inspectMediumIcon
            ) == JOptionPane.OK_OPTION
        ) {
            loadingAnimation.showAnimation()
            GlobalScope.launch {
                var i = 0
                while (i < textPane.text.length) {
                    val end = i + searchFor.text.length
                    if (end <= textPane.text.length && textPane.text.substring(i, end).equals(searchFor.text, true)) {
                        textPane.highlighter.addHighlight(
                            i,
                            end,
                            DefaultHighlighter.DefaultHighlightPainter(UIManager.getColor("textHighlight"))
                        )
                        i = end
                    } else {
                        i++
                    }
                }
                loadingAnimation.hideAnimation()
                textPane.isEnabled = true
            }
        }
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun searchRegexDialog() {
        requestFocus(searchFor)
        if (JOptionPane.showConfirmDialog(
                parent,
                searchRegex,
                Gui.createserverpack_gui_textarea_search_regex_title(name),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                guiProps.inspectMediumIcon
            ) == JOptionPane.OK_OPTION
        ) {
            loadingAnimation.showAnimation()
            textPane.isEnabled = false
            GlobalScope.launch {
                val regex = searchFor.text.toRegex()
                var i = 0
                while (i < textPane.text.length) {
                    for (n in textPane.text.length downTo i) {
                        if (textPane.text.substring(i, n).matches(regex)) {
                            textPane.highlighter.addHighlight(
                                i,
                                n,
                                DefaultHighlighter.DefaultHighlightPainter(UIManager.getColor("textHighlight"))
                            )
                            i = n
                            break
                        }
                    }
                    i++
                }
                loadingAnimation.hideAnimation()
                textPane.isEnabled = true
            }
        }

    }

    /**
     * @author Griefed
     */
    inner class LoadingAnimation : JWindow() {

        init {
            contentPane = JPanel()
            contentPane.layout = BorderLayout()
            contentPane.add(JLabel(guiProps.loadingAnimation), BorderLayout.CENTER)
            setSize(guiProps.loadingAnimation.iconWidth, guiProps.loadingAnimation.iconHeight)
            isVisible = false
        }

        /**
         * @author Griefed
         */
        fun showAnimation() {
            setLocationRelativeTo(this@InclusionTip)
            isVisible = true
            toFront()
        }

        /**
         * @author Griefed
         */
        fun hideAnimation() {
            isVisible = false
        }
    }
}