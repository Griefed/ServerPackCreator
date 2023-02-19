package de.griefed.serverpackcreator.gui.window.configs.components

import java.awt.Graphics
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JTextArea
import javax.swing.border.Border
import javax.swing.event.DocumentListener

/**
 * Based on [IconTextField]
 *
 * [it_qna](https://itqna.net/questions/10833/decorating-jtextfield-icon)
 */
class IconTextArea(s: String?) : JTextArea(s) {
    private var mBorder: Border? = null
    private var mIcon: Icon? = null

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        if (mIcon != null) {
            val iconInsets = mBorder!!.getBorderInsets(this)
            mIcon!!.paintIcon(this, graphics, iconInsets.left, height - mIcon!!.iconHeight)
        }
    }

    override fun setBorder(border: Border?) {
        mBorder = border
        if (mIcon == null) {
            super.setBorder(border)
        } else {
            val margin = BorderFactory.createEmptyBorder(0, mIcon!!.iconWidth + ICON_SPACING, 0, 0)
            val compound: Border = BorderFactory.createCompoundBorder(border, margin)
            super.setBorder(compound)
        }
    }

    /**
     * TODO docs
     */
    fun setIcon(icon: Icon?) {
        mIcon = icon
        resetBorder()
    }

    /**
     * TODO docs
     */
    private fun resetBorder() {
        border = mBorder!!
    }

    /**
     * TODO docs
     */
    fun addDocumentListener(listener: DocumentListener?) {
        document.addDocumentListener(listener)
    }

    companion object {
        private const val ICON_SPACING = 4
    }
}