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


class FileLinkIcon : FlatAbstractIcon(16, 16, UIManager.getColor("Objects.Grey")) {
    private var path: Path2D? = null
    private val blueColour = UIManager.getColor("Actions.Blue")
    override fun paintIcon(c: Component, g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.stroke = BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        if (path == null) {
            val arc = 1.5
            path = FlatUIUtils.createPath(
                false,
                // top-left
                2.5, 1.5 + arc, FlatUIUtils.QUAD_TO, 2.5, 1.5, 2.5 + arc, 1.5,
                // top-right
                8.8, 1.5, 13.5, 6.2,
                // bottom-right
                13.5, 14.5 - arc, FlatUIUtils.QUAD_TO, 13.5, 14.5, 13.5 - arc, 14.5,
                // bottom-left
                2.5 + arc, 14.5, FlatUIUtils.QUAD_TO, 2.5, 14.5, 2.5, 14.5 - arc,
                FlatUIUtils.CLOSE_PATH,
                FlatUIUtils.MOVE_TO, 8.5, 2.0,
                8.5, 6.5 - arc, FlatUIUtils.QUAD_TO, 8.5, 6.5, 8.5 + arc, 6.5,
                13.0, 6.5
            )
        }
        g.draw(path)

        g.color = blueColour
        g.draw(Line2D.Float(8f, 6.5f, 8f, 11.5f))
        g.draw(FlatUIUtils.createPath(false, 5.5, 9.0, 8.0, 6.5, 10.5, 9.0))
    }
}