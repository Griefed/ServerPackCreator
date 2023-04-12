package de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.renderer

import com.formdev.flatlaf.icons.FlatAbstractIcon
import com.formdev.flatlaf.ui.FlatUIUtils
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.UIManager


class DirectoryLinkIcon : FlatAbstractIcon(16, 16, UIManager.getColor("Objects.Grey")) {
    private var path: Path2D? = null
    private val blueColour = UIManager.getColor("Actions.Blue")
    override fun paintIcon(c: Component, g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.stroke = BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        if (path == null) {
            val arc = 1.5
            val arc2 = 0.5
            path = FlatUIUtils.createPath(
                // bottom-right
                14.5,13.5-arc,  FlatUIUtils.QUAD_TO, 14.5,13.5, 14.5-arc,13.5,
                // bottom-left
                1.5+arc,13.5,   FlatUIUtils.QUAD_TO, 1.5,13.5,  1.5,13.5-arc,
                // top-left
                1.5,2.5+arc,    FlatUIUtils.QUAD_TO, 1.5,2.5,   1.5+arc,2.5,
                // top-mid-left
                6.5-arc2,2.5,   FlatUIUtils.QUAD_TO, 6.5,2.5,   6.5+arc2,2.5+arc2,
                // top-mid-right
                8.5,4.5,
                // top-right
                14.5-arc,4.5,   FlatUIUtils.QUAD_TO, 14.5,4.5,  14.5,4.5+arc )
        }
        g.draw(path)

        g.color = blueColour
        g.draw(Line2D.Float(8f, 6.5f, 8f, 11.5f))
        g.draw(FlatUIUtils.createPath(false, 5.5, 9.0, 8.0, 6.5, 10.5, 9.0))
    }
}