/*
 *    Copyright (C) 2003 Per Cederberg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
@file:Suppress("unused")

package de.griefed.example.kotlin.gui.tab

import Example
import java.awt.*
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.util.*
import javax.swing.JComponent

/*
 * @(#)Main.java  (Tetris.java in this plugin example)
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 */

/**
 * A Tetris game, based on the incredible work of Per Cederberg, available at <a href="https://www.percederberg.net/games/tetris/index.html">www.percederberg.net/games/tetris</a>
 * and <a href="https://www.percederberg.net/games/tetris/mirrors.html">www.percederberg.net/games/tetris/mirrors</a>
 */
object Tetris {
    /**
     * The stand-alone main routine.
     *
     * @param args the command-line arguments
     */
    @JvmStatic
    fun main(args: Array<String>?) {
        val frame = Frame(Example.example_tetris_game_title.toString())
        val game = Game()
        val taHiScores = TextArea("", 10, 10, TextArea.SCROLLBARS_NONE)
        taHiScores.background = Color.black
        taHiScores.foreground = Color.white
        taHiScores.font = Font("monospaced", Font.PLAIN, 11)
        taHiScores.text = Example.example_tetris_game_highscore.toString()
        taHiScores.isEditable = false
        taHiScores.isFocusable = false
        val txt = TextField()
        txt.isEnabled = false
        game.addPropertyChangeListener { evt: PropertyChangeEvent ->
            if (evt.propertyName == "state") {
                val state = evt.newValue as Int
                if (state == Game.STATE_GAMEOVER) {
                    txt.isEnabled = true
                    txt.requestFocus()
                    txt.addActionListener {
                        txt.isEnabled = false
                        game.init()
                    }
                    // show score...
                }
            }
        }
        val btnStart = Button(Example.example_tetris_game_start.toString())
        btnStart.isFocusable = false
        btnStart.addActionListener { game.start() }
        val c = Container()
        c.layout = BorderLayout()
        c.add(txt, BorderLayout.NORTH)
        game.squareBoardComponent?.let { c.add(it, BorderLayout.CENTER) }
        c.add(btnStart, BorderLayout.SOUTH)
        val c2 = Container()
        c2.layout = GridLayout(1, 2)
        c2.add(c)
        c2.add(taHiScores)
        frame.add(c2)
        frame.pack()

        // Add frame window listener
        frame.addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    frame.dispose()
                    game.terminate()
                }
            })
        frame.setLocationRelativeTo(null)

        // Show frame (and start game)
        frame.isVisible = true
    }
}

/*
 * @(#)SquareBoard.java
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 */

/**
 * A Tetris square board. The board is rectangular and contains a grid of colored squares. The board
 * is considered to be constrained to both sides (left and right), and to the bottom. There is no
 * constraint to the top of the board, although colors assigned to positions above the board are not
 * saved.
 *
 * Creates a new square board with the specified size. The square board will initially be empty.
 *
 * @param boardWidth  the width of the board (in squares)
 * @param boardHeight the height of the board (in squares)
 *
 * @author Per Cederberg, per@percederberg.net
 * @version 1.2
 */
internal class SquareBoard(
    val boardWidth: Int,
    val boardHeight: Int
) {
    /**
     * Returns the board width (in squares). This method returns, i.e, the number of horizontal
     * squares that fit on the board.
     *
     * @return the board width in squares
     */
    /**
     * Returns the board height (in squares). This method returns, i.e, the number of vertical squares
     * that fit on the board.
     *
     * @return the board height in squares
     */

    /**
     * The graphical sqare board component. This graphical representation is created upon the first
     * call to getComponent().
     */
    private val component: SquareBoardComponent?

    /**
     * The square board color matrix. This matrix (or grid) contains a color entry for each square in
     * the board. The matrix is indexed by the vertical, and then the horizontal coordinate.
     */
    private val matrix: Array<Array<Color?>> = Array(boardHeight) { arrayOfNulls(boardWidth) }

    /**
     * An optional board message. The board message can be set at any time, printing it on top of the
     * board.
     */
    private var message: String? = null
    /**
     * Returns the number of lines removed since the last clear().
     *
     * @return the number of lines removed since the last clear call
     */
    /**
     * The number of lines removed. This counter is increased each time a line is removed from the
     * board.
     */
    var removedLines = 0
        private set

    init {
        component = SquareBoardComponent()
        clear()
    }

    /**
     * Checks if a specified square is empty, i.e. if it is not marked with a color. If the square is
     * outside the board, false will be returned in all cases except when the square is directly above
     * the board.
     *
     * @param x the horizontal position (0 &#60;= x &#60; width)
     * @param y the vertical position (0 &#60;= y &#60; height)
     * @return true if the square is emtpy, or false otherwise
     */
    fun isSquareEmpty(x: Int, y: Int): Boolean {
        return if (x < 0 || x >= boardWidth || y < 0 || y >= boardHeight) {
            x in 0 until boardWidth && y < 0
        } else {
            matrix[y][x] == null
        }
    }

    /**
     * Checks if a specified line is empty, i.e. only contains empty squares. If the line is outside
     * the board, false will always be returned.
     *
     * @param y the vertical position (0 &#60;= y &#60; height)
     * @return true if the whole line is empty, or false otherwise
     */
    fun isLineEmpty(y: Int): Boolean {
        if (y < 0 || y >= boardHeight) {
            return false
        }
        for (x in 0 until boardWidth) {
            if (matrix[y][x] != null) {
                return false
            }
        }
        return true
    }

    /**
     * Checks if a specified line is full, i.e. only contains no empty squares. If the line is outside
     * the board, true will always be returned.
     *
     * @param y the vertical position (0 &#60;= y &#60; height)
     * @return true if the whole line is full, or false otherwise
     */
    private fun isLineFull(y: Int): Boolean {
        if (y < 0 || y >= boardHeight) {
            return true
        }
        for (x in 0 until boardWidth) {
            if (matrix[y][x] == null) {
                return false
            }
        }
        return true
    }

    /**
     * Checks if the board contains any full lines.
     *
     * @return true if there are full lines on the board, or false otherwise
     */
    fun hasFullLines(): Boolean {
        for (y in boardHeight - 1 downTo 0) {
            if (isLineFull(y)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns a graphical component to draw the board. The component returned will automatically be
     * updated when changes are made to this board. Multiple calls to this method will return the same
     * component, as a square board can only have a single graphical representation.
     *
     * @return a graphical component that draws this board
     */
    fun getComponent(): Component? {
        return component
    }

    /**
     * Returns the color of an individual square on the board. If the square is empty or outside the
     * board, null will be returned.
     *
     * @param x the horizontal position (0 &#60;= x &#60; width)
     * @param y the vertical position (0 &#60;= y &#60; height)
     * @return the square color, or null for none
     */
    fun getSquareColor(x: Int, y: Int): Color? {
        return if (x < 0 || x >= boardWidth || y < 0 || y >= boardHeight) {
            null
        } else {
            matrix[y][x]
        }
    }

    /**
     * Changes the color of an individual square on the board. The square will be marked as in need of
     * a repaint, but the graphical component will NOT be repainted until the update() method is
     * called.
     *
     * @param x     the horizontal position (0 &#60;= x &#60; width)
     * @param y     the vertical position (0 &#60;= y &#60; height)
     * @param color the new square color, or null for empty
     */
    fun setSquareColor(x: Int, y: Int, color: Color?) {
        if (x < 0 || x >= boardWidth || y < 0 || y >= boardHeight) {
            return
        }
        matrix[y][x] = color
        component?.invalidateSquare(x, y)
    }

    /**
     * Sets a message to display on the square board. This is supposed to be used when the board is
     * not being used for active drawing, as it slows down the drawing considerably.
     *
     * @param message a message to display, or null to remove a previous message
     */
    fun setMessage(message: String?) {
        this.message = message
        component?.redrawAll()
    }

    /**
     * Clears the board, i.e. removes all the colored squares. As side effects, the number of removed
     * lines will be reset to zero, and the component will be repainted immediately.
     */
    fun clear() {
        removedLines = 0
        for (y in 0 until boardHeight) {
            for (x in 0 until boardWidth) {
                matrix[y][x] = null
            }
        }
        component?.redrawAll()
    }

    /**
     * Removes all full lines. All lines above a removed line will be moved downward one step, and a
     * new empty line will be added at the top. After removing all full lines, the component will be
     * repainted.
     *
     * @see .hasFullLines
     */
    fun removeFullLines() {
        var repaint = false

        // Remove full lines
        var y = boardHeight - 1
        while (y >= 0) {
            if (isLineFull(y)) {
                removeLine(y)
                removedLines++
                repaint = true
                y++
            }
            y--
        }

        // Repaint if necessary
        if (repaint && component != null) {
            component.redrawAll()
        }
    }

    /**
     * Removes a single line. All lines above are moved down one step, and a new empty line is added
     * at the top. No repainting will be done after removing the line.
     *
     * @param y the vertical position (0 &#60;= y &#60; height)
     */
    private fun removeLine(y: Int) {
        var yAxis = y
        if (yAxis < 0 || yAxis >= boardHeight) {
            return
        }
        while (yAxis > 0) {
            if (boardWidth >= 0) {
                System.arraycopy(matrix[yAxis - 1], 0, matrix[yAxis], 0, boardWidth)
            }
            yAxis--
        }
        for (x in 0 until boardWidth) {
            matrix[0][x] = null
        }
    }

    /**
     * Updates the graphical component. Any squares previously changed will be repainted by this
     * method.
     */
    fun update() {
        component!!.redraw()
    }

    /**
     * The graphical component that paints the square board. This is implemented as an inner class in
     * order to better abstract the detailed information that must be sent between the square board
     * and its graphical representation.
     */
    private inner class SquareBoardComponent : JComponent() {
        /**
         * The square size in pixels. This value is updated when the component size is changed, i.e.
         * when the `size` variable is modified.
         */
        private val squareSize = Dimension(0, 0)

        /**
         * A clip boundary buffer rectangle. This rectangle is used when calculating the clip
         * boundaries, in order to avoid allocating a new clip rectangle for each board square.
         */
        private val bufferRect = Rectangle()

        /**
         * The board message color.
         */
        private val messageColor: Color

        /**
         * A lookup table containing lighter versions of the colors. This table is used to avoid
         * calculating the lighter versions of the colors for each and every square drawn.
         */
        private val lighterColors = Hashtable<Color?, Color?>()

        /**
         * A lookup table containing darker versions of the colors. This table is used to avoid
         * calculating the darker versions of the colors for each and every square drawn.
         */
        private val darkerColors = Hashtable<Color?, Color?>()

        /**
         * A bounding box of the squares to update. The coordinates used in the rectangle refers to the
         * square matrix.
         */
        private val updateRect = Rectangle()

        /**
         * The component size. If the component has been resized, that will be detected when the paint
         * method executes. If this value is set to null, the component dimensions are unknown.
         */
        private var componentSize: Dimension? = null

        /**
         * An image used for double buffering. The board is first painted onto this image, and that
         * image is then painted onto the real surface in order to avoid making the drawing process
         * visible to the user. This image is recreated each time the component size changes.
         */
        private var bufferImage: Image? = null

        /**
         * A flag set when the component has been updated.
         */
        private var updated = true

        /**
         * Creates a new square board component.
         */
        init {
            insets.set(0, 0, 0, 0)
            background = Configuration.getColor("board.background", "#000000")
            messageColor = Configuration.getColor("board.message", "#ffffff")
        }

        /**
         * Adds a square to the set of squares in need of redrawing.
         *
         * @param x the horizontal position (0 &#60;= x &#60; width)
         * @param y the vertical position (0 &#60;= y &#60; height)
         */
        fun invalidateSquare(x: Int, y: Int) {
            if (updated) {
                updated = false
                updateRect.x = x
                updateRect.y = y
                updateRect.width = 0
                updateRect.height = 0
            } else {
                if (x < updateRect.x) {
                    updateRect.width += updateRect.x - x
                    updateRect.x = x
                } else if (x > updateRect.x + updateRect.width) {
                    updateRect.width = x - updateRect.x
                }
                if (y < updateRect.y) {
                    updateRect.height += updateRect.y - y
                    updateRect.y = y
                } else if (y > updateRect.y + updateRect.height) {
                    updateRect.height = y - updateRect.y
                }
            }
        }

        /**
         * Redraws all the invalidated squares. If no squares have been marked as in need of redrawing,
         * no redrawing will occur.
         */
        fun redraw() {
            val g: Graphics?
            if (!updated) {
                updated = true
                g = graphics
                if (g == null) {
                    return
                }
                g.setClip(
                    insets.left + updateRect.x * squareSize.width,
                    insets.top + updateRect.y * squareSize.height,
                    (updateRect.width + 1) * squareSize.width,
                    (updateRect.height + 1) * squareSize.height
                )
                paint(g)
            }
        }

        /**
         * Redraws the whole component.
         */
        fun redrawAll() {
            updated = true
            val g: Graphics = graphics ?: return
            g.setClip(insets.left, insets.top, width * squareSize.width, height * squareSize.height)
            paint(g)
        }

        /**
         * Returns true as this component is double buffered.
         *
         * @return true as this component is double buffered
         */
        override fun isDoubleBuffered(): Boolean {
            return true
        }

        /**
         * Returns the preferred size of this component.
         *
         * @return the preferred component size
         */
        override fun getPreferredSize(): Dimension {
            return Dimension(width * 20, height * 20)
        }

        /**
         * Returns the minimum size of this component.
         *
         * @return the minimum component size
         */
        override fun getMinimumSize(): Dimension {
            return preferredSize
        }

        /**
         * Returns the maximum size of this component.
         *
         * @return the maximum component size
         */
        override fun getMaximumSize(): Dimension {
            return preferredSize
        }

        /**
         * Returns a lighter version of the specified color. The lighter color will looked up in a
         * hashtable, making this method fast. If the color is not found, the ligher color will be
         * calculated and added to the lookup table for later reference.
         *
         * @param c the base color
         * @return the lighter version of the color
         */
        private fun getLighterColor(c: Color?): Color? {
            var lighter: Color?
            lighter = lighterColors[c]
            if (lighter == null) {
                lighter = c!!.brighter().brighter()
                lighterColors[c] = lighter
            }
            return lighter
        }

        /**
         * Returns a darker version of the specified color. The darker color will looked up in a
         * hashtable, making this method fast. If the color is not found, the darker color will be
         * calculated and added to the lookup table for later reference.
         *
         * @param c the base color
         * @return the darker version of the color
         */
        private fun getDarkerColor(c: Color?): Color? {
            var darker: Color?
            darker = darkerColors[c]
            if (darker == null) {
                darker = c!!.darker().darker()
                darkerColors[c] = darker
            }
            return darker
        }

        /**
         * Paints this component indirectly. The painting is first done to a buffer image, that is then
         * painted directly to the specified graphics context.
         *
         * @param g the graphics context to use
         */
        @Synchronized
        override fun paint(g: Graphics) {

            // Handle component size change
            if (componentSize == null || componentSize != size) {
                componentSize = size
                squareSize.width = componentSize!!.width / width
                squareSize.height = componentSize!!.height / height
                insets.left = (componentSize!!.width - width * squareSize.width) / 2
                insets.right = insets.left
                insets.top = 0
                insets.bottom = componentSize!!.height - height * squareSize.height
                bufferImage = createImage(width * squareSize.width, height * squareSize.height)
            }

            // Paint component in buffer image
            val rect: Rectangle = g.clipBounds
            val bufferGraphics: Graphics = bufferImage!!.graphics
            bufferGraphics.setClip(rect.x - insets.left, rect.y - insets.top, rect.width, rect.height)
            doPaintComponent(bufferGraphics)

            // Paint image buffer
            g.drawImage(bufferImage, insets.left, insets.top, background, null)
        }

        /**
         * Paints this component directly. All the squares on the board will be painted directly to the
         * specified graphics context.
         *
         * @param g the graphics context to use
         */
        private fun doPaintComponent(g: Graphics) {

            // Paint background
            g.color = background
            g.fillRect(0, 0, width * squareSize.width, height * squareSize.height)

            // Paint squares
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (matrix[y][x] != null) {
                        paintSquare(g, x, y)
                    }
                }
            }

            // Paint message
            if (message != null) {
                paintMessage(g, message!!)
            }
        }

        /**
         * Paints a single board square. The specified position must contain a color object.
         *
         * @param g the graphics context to use
         * @param x the horizontal position (0 &#60;= x &#60; width)
         * @param y the vertical position (0 &#60; y &#60; height)
         */
        private fun paintSquare(g: Graphics, x: Int, y: Int) {
            val color = matrix[y][x]
            val xMin = x * squareSize.width
            val yMin = y * squareSize.height
            val xMax = xMin + squareSize.width - 1
            val yMax = yMin + squareSize.height - 1

            // Skip drawing if not visible
            bufferRect.x = xMin
            bufferRect.y = yMin
            bufferRect.width = squareSize.width
            bufferRect.height = squareSize.height
            if (!bufferRect.intersects(g.clipBounds)) {
                return
            }

            // Fill with base color
            g.color = color
            g.fillRect(xMin, yMin, squareSize.width, squareSize.height)

            // Draw brighter lines
            g.color = getLighterColor(color)
            var i = 0
            while (i < squareSize.width / 10) {
                g.drawLine(xMin + i, yMin + i, xMax - i, yMin + i)
                g.drawLine(xMin + i, yMin + i, xMin + i, yMax - i)
                i++
            }

            // Draw darker lines
            g.color = getDarkerColor(color)
            i = 0
            while (i < squareSize.width / 10) {
                g.drawLine(xMax - i, yMin + i, xMax - i, yMax - i)
                g.drawLine(xMin + i, yMax - i, xMax - i, yMax - i)
                i++
            }
        }

        /**
         * Paints a board message. The message will be drawn at the center of the component.
         *
         * @param g   the graphics context to use
         * @param msg the string message
         */
        private fun paintMessage(g: Graphics, msg: String) {
            // Find string font width
            g.font = Font("SansSerif", Font.BOLD, squareSize.width + 4)
            val fontWidth: Int = g.fontMetrics.stringWidth(msg)
            val x: Int = (width * squareSize.width - fontWidth) / 2
            val y: Int = height * squareSize.height / 2
            val offset: Int = squareSize.width / 10
            g.color = Color.black
            g.drawString(msg, x - offset, y - offset)
            g.drawString(msg, x - offset, y)
            g.drawString(msg, x - offset, y - offset)
            g.drawString(msg, x, y - offset)
            g.drawString(msg, x, y + offset)
            g.drawString(msg, x + offset, y - offset)
            g.drawString(msg, x + offset, y)
            g.drawString(msg, x + offset, y + offset)

            // Draw white version of the string
            g.color = messageColor
            g.drawString(msg, x, y)
        }
    }
}

/*
 * @(#)Game.java
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 */

/**
 * The Tetris game. This class controls all events in the game and handles all the game logics. The
 * game is started through user interaction with the graphical game component provided by this
 * class.
 *
 * @author Per Cederberg, per@percederberg.net
 * @version 1.2
 */
internal class Game @JvmOverloads constructor(width: Int = 10, height: Int = 20) {
    /**
     * The PropertyChangeSupport Object able to register listener and dispatch events to them.
     */
    private val propertyChangeSupport = PropertyChangeSupport(this)

    /**
     * The main square board. This board is used for the game itself.
     */
    private val board: SquareBoard = SquareBoard(width, height)

    /**
     * The preview square board. This board is used to display a preview of the figures.
     */
    private val previewBoard = SquareBoard(5, 5)

    /**
     * The thread that runs the game. When this variable is set to null, the game thread will
     * terminate.
     */
    private val thread: GameThread

    /**
     * The figures used on both boards. All figures are reutilized in order to avoid creating new
     * objects while the game is running. Special care has to be taken when the preview figure and the
     * current figure refers to the same object.
     */
    private val figures = arrayOf(
        Figure(Figure.SQUARE_FIGURE),
        Figure(Figure.LINE_FIGURE),
        Figure(Figure.S_FIGURE),
        Figure(Figure.Z_FIGURE),
        Figure(Figure.RIGHT_ANGLE_FIGURE),
        Figure(Figure.LEFT_ANGLE_FIGURE),
        Figure(Figure.TRIANGLE_FIGURE)
    )
    /**
     * Gets the current level.
     *
     * @return the current level.
     */
    /**
     * The game level. The level will be increased for every 20 lines removed from the square board.
     */
    var level = 1
        private set

    /**
     * The current score. The score is increased for every figure that is possible to place on the
     * main board.
     *
     * @return the current score.
     */
    private var score = 0

    /**
     * The current figure. The figure will be updated when
     */
    private var figure: Figure? = null

    /**
     * The next figure.
     */
    private var nextFigure: Figure? = null

    /**
     * The rotation of the next figure.
     */
    private var nextRotation = 0

    /**
     * The figure preview flag. If this flag is set, the figure will be shown in the figure preview
     * board.
     */
    private var preview = true

    /**
     * The move lock flag. If this flag is set, the current figure cannot be moved. This flag is set
     * when a figure is moved all the way down, and reset when a new figure is displayed.
     */
    private var moveLock = false

    /**
     * Gets the current 'state'. One of the following:
     * STATE_GETREADY,STATE_PLAYING,STATE_PAUSED,STATE_GAMEOVER.
     *
     * @return the current state.
     */
    private var state = 0

    /**
     * Creates a new Tetris game. The square board will be given the specified size.
     *
     * @param width  the width of the square board (in positions)
     * @param height the height of the square board (in positions)
     */
    /**
     * Creates a new Tetris game. The square board will be given the default size of 10x20.
     */
    init {
        thread = GameThread()
        handleGetReady()
        board.getComponent()!!.isFocusable = true
        board
            .getComponent()!!
            .addKeyListener(
                object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        handleKeyEvent(e)
                    }
                })
    }

    /**
     * Adds a PropertyChangeListener to this Game.
     *
     *
     * This is the list the Events that can be fired:
     *
     *
     * name: "state" value: new current state (int) one of those:
     * STATE_OVER,STATE_PLAYING,STATE_PAUSED when: fired when the state changes.
     *
     *
     * name: "level" value: current level (int) when: fired when the player moves to the next
     * level.
     *
     *
     * name: "score" value: current score (int) when: fired when the player increases his/her
     * score.
     *
     *
     * name: "lines" value: number of 'removed' lines (int) when: fired when the player removes
     * one or more lines.
     *
     * @param l the property change listener which is going to be notified.
     */
    fun addPropertyChangeListener(l: PropertyChangeListener?) {
        propertyChangeSupport.addPropertyChangeListener(l)
    }

    /**
     * Removes this propertyChangeListener
     *
     * @param l the PropertyChangeListener object to remove.
     */
    fun removePropertyChangeListener(l: PropertyChangeListener?) {
        propertyChangeSupport.removePropertyChangeListener(l)
    }

    /**
     * Gets the number of lines that have been removed since the game started.
     *
     * @return the number of removed lines.
     */
    val removedLines: Int
        get() = board.removedLines

    /**
     * Gets the java.awt.Component for the board.
     *
     * @return the gui component for the board.
     */
    val squareBoardComponent: Component?
        get() = board.getComponent()

    /**
     * Gets the java.awt.Component for the preview board (5x5)
     *
     * @return the gui component for the board.
     */
    val previewBoardComponent: Component?
        get() = previewBoard.getComponent()

    /**
     * Initializes the game ready if the state is on STATE_GAMEOVER otherwise it does nothing.
     */
    fun init() {
        if (state == STATE_GAMEOVER) {
            handleGetReady()
        }
    }

    /**
     * Starts the game. (No matter what the current state is)
     */
    fun start() {
        handleStart()
    }

    /**
     * Pauses the game if the state is on STATE_PLAYING otherwise it does nothing.
     */
    fun pause() {
        if (state == STATE_PLAYING) {
            handlePause()
        }
    }

    /**
     * Resumes the game if the state is on STATE_PAUSED otherwise it does nothing.
     */
    fun resume() {
        if (state == STATE_PAUSED) {
            handleResume()
        }
    }

    /**
     * Terminates the game. (No matter what the current state is)
     */
    fun terminate() {
        handleGameOver()
    }

    /**
     * Handles a game start event. Both the main and preview square boards will be reset, and all
     * other game parameters will be reset. Finally, the game thread will be launched.
     */
    private fun handleStart() {

        // Reset score and figures
        level = 1
        score = 0
        figure = null
        nextFigure = randomFigure()
        nextFigure!!.rotateRandom()
        nextRotation = nextFigure!!.rotation

        // Reset components
        state = STATE_PLAYING
        board.setMessage(null)
        board.clear()
        previewBoard.clear()
        handleLevelModification()
        handleScoreModification()
        propertyChangeSupport.firePropertyChange("state", -1, STATE_PLAYING)

        // Start game thread
        thread.reset()
    }

    /**
     * Handles a game over event. This will stop the game thread, reset all figures and print a game
     * over message.
     */
    private fun handleGameOver() {

        // Stop game thred
        thread.isPaused = true

        // Reset figures
        if (figure != null) {
            figure!!.detach()
        }
        figure = null
        if (nextFigure != null) {
            nextFigure!!.detach()
        }
        nextFigure = null

        // Handle components
        state = STATE_GAMEOVER
        board.setMessage(Example.example_tetris_game_over.toString())
        propertyChangeSupport.firePropertyChange("state", -1, STATE_GAMEOVER)
    }

    /**
     * Handles a getReady event. This will print a 'get ready' message on the game board.
     */
    private fun handleGetReady() {
        board.setMessage(Example.example_tetris_game_ready.toString())
        board.clear()
        previewBoard.clear()
        state = STATE_GETREADY
        propertyChangeSupport.firePropertyChange("state", -1, STATE_GETREADY)
    }

    /**
     * Handles a game pause event. This will pause the game thread and print a pause message on the
     * game board.
     */
    private fun handlePause() {
        thread.isPaused = true
        state = STATE_PAUSED
        board.setMessage(Example.example_tetris_game_pause.toString())
        propertyChangeSupport.firePropertyChange("state", -1, STATE_PAUSED)
    }

    /**
     * Handles a game resume event. This will resume the game thread and remove any messages on the
     * game board.
     */
    private fun handleResume() {
        state = STATE_PLAYING
        board.setMessage(null)
        thread.isPaused = false
        propertyChangeSupport.firePropertyChange("state", -1, STATE_PLAYING)
    }

    /**
     * Handles a level modification event. This will modify the level label and adjust the thread
     * speed.
     */
    private fun handleLevelModification() {
        propertyChangeSupport.firePropertyChange("level", -1, level)
        thread.adjustSpeed()
    }

    /**
     * Handle a score modification event. This will modify the score label.
     */
    private fun handleScoreModification() {
        propertyChangeSupport.firePropertyChange("score", -1, score)
    }

    /**
     * Handles a figure start event. This will move the next figure to the current figure position,
     * while also creating a new preview figure. If the figure cannot be introduced onto the game
     * board, a game over event will be launched.
     */
    private fun handleFigureStart() {
        // Move next figure to current
        val rotation: Int = nextRotation
        figure = nextFigure
        moveLock = false
        nextFigure = randomFigure()
        nextFigure!!.rotateRandom()
        nextRotation = nextFigure!!.rotation

        // Handle figure preview
        if (preview) {
            previewBoard.clear()
            nextFigure!!.attach(previewBoard, true)
            nextFigure!!.detach()
        }

        // Attach figure to game board
        figure!!.rotation = rotation
        if (!figure!!.attach(board, false)) {
            previewBoard.clear()
            figure!!.attach(previewBoard, true)
            figure!!.detach()
            handleGameOver()
        }
    }

    /**
     * Handles a figure landed event. This will check that the figure is completely visible, or a game
     * over event will be launched. After this control, any full lines will be removed. If no full
     * lines could be removed, a figure start event is launched directly.
     */
    private fun handleFigureLanded() {

        // Check and detach figure
        if (figure!!.isAllVisible) {
            score += 10
            handleScoreModification()
        } else {
            handleGameOver()
            return
        }
        figure!!.detach()
        figure = null

        // Check for full lines or create new figure
        if (board.hasFullLines()) {
            board.removeFullLines()
            propertyChangeSupport.firePropertyChange("lines", -1, board.removedLines)
            if (level < 9 && board.removedLines / 20 > level) {
                level = board.removedLines / 20
                handleLevelModification()
            }
        } else {
            handleFigureStart()
        }
    }

    /**
     * Handles a timer event. This will normally move the figure down one step, but when a figure has
     * landed or isn't ready other events will be launched. This method is synchronized to avoid race
     * conditions with other asynchronous events (keyboard and mouse).
     */
    @Synchronized
    private fun handleTimer() {
        if (figure == null) {
            handleFigureStart()
        } else if (figure!!.hasLanded()) {
            handleFigureLanded()
        } else {
            figure!!.moveDown()
        }
    }

    /**
     * Handles a button press event. This will launch different events depending on the state of the
     * game, as the button semantics change as the game changes. This method is synchronized to avoid
     * race conditions with other asynchronous events (timer and keyboard).
     */
    @Synchronized
    private fun handlePauseOnOff() {
        if (nextFigure == null) {
            handleStart()
        } else if (thread.isPaused) {
            handleResume()
        } else {
            handlePause()
        }
    }

    /**
     * Handles a keyboard event. This will result in different actions being taken, depending on the
     * key pressed. In some cases, other events will be launched. This method is synchronized to avoid
     * race conditions with other asynchronous events (timer and mouse).
     *
     * @param e the key event
     */
    @Synchronized
    private fun handleKeyEvent(e: KeyEvent) {
        // Handle start (any key to start !!!)
        if (state == STATE_GETREADY) {
            handleStart()
            return
        }

        // pause and resume
        if (e.keyCode == KeyEvent.VK_P) {
            handlePauseOnOff()
            return
        }

        // Don't proceed if stopped or paused
        if (figure == null || moveLock || thread.isPaused) {
            return
        }
        when (e.keyCode) {
            KeyEvent.VK_LEFT -> figure!!.moveLeft()
            KeyEvent.VK_RIGHT -> figure!!.moveRight()
            KeyEvent.VK_DOWN -> {
                figure!!.moveAllWayDown()
                moveLock = true
            }

            KeyEvent.VK_UP, KeyEvent.VK_SPACE -> if (e.isControlDown) {
                figure!!.rotateRandom()
            } else if (e.isShiftDown) {
                figure!!.rotateClockwise()
            } else {
                figure!!.rotateCounterClockwise()
            }

            KeyEvent.VK_S -> if (level < 9) {
                level++
                handleLevelModification()
            }

            KeyEvent.VK_N -> {
                preview = !preview
                if (preview && figure !== nextFigure) {
                    nextFigure!!.attach(previewBoard, true)
                    nextFigure!!.detach()
                } else {
                    previewBoard.clear()
                }
            }
        }
    }

    /**
     * Returns a random figure. The figures come from the figures array, and will not be initialized.
     *
     * @return a random figure
     */
    private fun randomFigure(): Figure {
        return figures[(Math.random() * figures.size).toInt()]
    }

    /**
     * The game time thread. This thread makes sure that the timer events are launched appropriately,
     * making the current figure fall. This thread can be reused across games, but should be set to
     * paused state when no game is running.
     */
    private inner class GameThread : Thread() {
        /**
         * The game pause flag. This flag is set to true while the game should pause.
         */
        var isPaused = true

        /**
         * The number of milliseconds to sleep before each automatic move. This number will be lowered
         * as the game progresses.
         */
        private var sleepTime = 500

        /**
         * Resets the game thread. This will adjust the speed and start the game thread if not
         * previously started.
         */
        fun reset() {
            adjustSpeed()
            isPaused = false
            if (!isAlive) {
                this.start()
            }
        }

        /**
         * Adjusts the game speed according to the current level. The sleeping time is calculated with a
         * function making larger steps initially a smaller as the level increases. A level above ten
         * (10) doesn't have any further effect.
         */
        fun adjustSpeed() {
            sleepTime = 4500 / (level + 5) - 250
            if (sleepTime < 50) {
                sleepTime = 50
            }
        }

        /**
         * Runs the game.
         */
        override fun run() {
            while (true) {
                // Make the time step
                handleTimer()

                // Sleep for some time
                try {
                    sleep(sleepTime.toLong())
                } catch (ignore: InterruptedException) {
                    // Do nothing
                }

                // Sleep if paused
                while (isPaused && thread === this) {
                    try {
                        sleep(1000)
                    } catch (ignore: InterruptedException) {
                        // Do nothing
                    }
                }
            }
        }
    }

    companion object {
        const val STATE_GETREADY = 1
        const val STATE_PLAYING = 2
        const val STATE_PAUSED = 3
        const val STATE_GAMEOVER = 4
    }
}

/*
 * @(#)Configuration.java
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 */

/**
 * A program configuration. This class provides static methods for simplifying the reading of
 * configuration parameters. It also provides some methods for transforming string values into more
 * useful objects.
 *
 * @author Per Cederberg, per@percederberg.net
 * @version 1.2
 */
internal object Configuration {
    /**
     * The internal configuration property values. This lookup table is used to avoid setting
     * configuration parameters in the system properties, as some programs (applets) do not have the
     * security permissions to set system properties.
     */
    private val config = Hashtable<String, String>()

    /**
     * Returns a configuration parameter value.
     *
     * @param key the configuration parameter key
     * @return the configuration parameter value, or null if not set
     */
    fun getValue(key: String): String? {
        return if (config.containsKey(key)) {
            config[key]
        } else {
            try {
                System.getProperty(key)
            } catch (ignore: SecurityException) {
                null
            }
        }
    }

    /**
     * Returns a configuration parameter value. If the configuration parameter is not set, a default
     * value will be returned instead.
     *
     * @param key the configuration parameter key
     * @param def the default value to use
     * @return the configuration parameter value, or the default value if not set
     */
    fun getValue(key: String, def: String): String {
        val value = getValue(key)
        return value ?: def
    }

    /**
     * Sets a configuration parameter value.
     *
     * @param key   the configuration parameter key
     * @param value the configuration parameter value
     */
    fun setValue(key: String, value: String) {
        config[key] = value
    }

    /**
     * Returns the color configured for the specified key. The key will be prepended with
     * "tetris.color." and the value will be read from the system properties. The color value must be
     * specified in hexadecimal web format, i.e. in the "#RRGGBB" format. If the default color isn't
     * in a valid format, white will be returned.
     *
     * @param key the configuration parameter key
     * @param def the default value
     * @return the color specified in the configuration, or a default color value
     */
    fun getColor(key: String, def: String): Color {
        val value = getValue("tetris.color.$key", def)
        var color: Color? = parseColor(value)
        if (color != null) {
            return color
        }
        color = parseColor(def)
        return color ?: Color.white
    }

    /**
     * Parses a web color string. If the color value couldn't be parsed correctly, null will be
     * returned.
     *
     * @param value the color value to parse
     * @return the color represented by the string, or null if the string was malformed
     */
    private fun parseColor(value: String): Color? {
        return if (!value.startsWith("#")) {
            null
        } else try {
            Color(value.substring(1).toInt(16))
        } catch (ignore: NumberFormatException) {
            null
        }
    }
}

/*
 * @(#)Figure.java
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 */

/**
 * A class representing a Tetris square figure. Each figure consists of four connected squares in
 * one of seven possible constellations. The figures may be rotated in 90 degree steps and have
 * sideways and downwards movability.
 *
 *
 * Each figure instance can have two states, either attached to a square board or not. When
 * attached, all move and rotation operations are checked so that collisions do not occur with other
 * squares on the board. When not attached, any rotation can be made (and will be kept when attached
 * to a new board).
 *
 * Creates a new figure of one of the seven predefined types. The figure will not be attached to
 * any square board and default colors and orientations will be assigned.
 *
 * @param type the figure type (one of the figure constants)
 * @throws IllegalArgumentException if the figure type specified is not recognized
 * @see .SQUARE_FIGURE
 *
 * @see .LINE_FIGURE
 *
 * @see .S_FIGURE
 *
 * @see .Z_FIGURE
 *
 * @see .RIGHT_ANGLE_FIGURE
 *
 * @see .LEFT_ANGLE_FIGURE
 *
 * @see .TRIANGLE_FIGURE
 * @author Per Cederberg, per@percederberg.net
 * @version 1.2
 */
internal class Figure(type: Int) {
    /**
     * The horizontal coordinates of the figure shape. The coordinates are relative to the current
     * figure position and orientation.
     */
    private val shapeX = IntArray(4)

    /**
     * The vertical coordinates of the figure shape. The coordinates are relative to the current
     * figure position and orientation.
     */
    private val shapeY = IntArray(4)

    /**
     * The square board to which the figure is attached. If this variable is set to null, the figure
     * is not attached.
     */
    private var board: SquareBoard? = null

    /**
     * The horizontal figure position on the board. This value has no meaning when the figure is not
     * attached to a square board.
     */
    private var xPos = 0

    /**
     * The vertical figure position on the board. This value has no meaning when the figure is not
     * attached to a square board.
     */
    private var yPos = 0

    /**
     * The figure orientation (or rotation). This value is normally between 0 and 3, but must also be
     * less than the maxOrientation value.
     *
     * @see .maxOrientation
     */
    private var orientation = 0

    /**
     * The maximum allowed orientation number. This is used to reduce the number of possible rotations
     * for some figures, such as the square figure. If this value is not used, the square figure will
     * be possible to rotate around one of its squares, which gives an erroneous effect.
     *
     * @see .orientation
     */
    private var maxOrientation = 4

    /**
     * The figure color.
     */
    private var color = Color.white

    init {
        initialize(type)
    }

    /**
     * Initializes the instance variables for a specified figure type.
     *
     * @param type the figure type (one of the figure constants)
     * @throws IllegalArgumentException if the figure type specified is not recognized
     * @see .SQUARE_FIGURE
     *
     * @see .LINE_FIGURE
     *
     * @see .S_FIGURE
     *
     * @see .Z_FIGURE
     *
     * @see .RIGHT_ANGLE_FIGURE
     *
     * @see .LEFT_ANGLE_FIGURE
     *
     * @see .TRIANGLE_FIGURE
     */
    @Throws(IllegalArgumentException::class)
    private fun initialize(type: Int) {

        // Initialize default variables
        board = null
        xPos = 0
        yPos = 0
        orientation = 0
        when (type) {
            SQUARE_FIGURE -> {
                maxOrientation = 1
                color = Configuration.getColor("figure.square", "#ffd8b1")
                shapeX[0] = -1
                shapeY[0] = 0
                shapeX[1] = 0
                shapeY[1] = 0
                shapeX[2] = -1
                shapeY[2] = 1
                shapeX[3] = 0
                shapeY[3] = 1
            }

            LINE_FIGURE -> {
                maxOrientation = 2
                color = Configuration.getColor("figure.line", "#ffb4b4")
                shapeX[0] = -2
                shapeY[0] = 0
                shapeX[1] = -1
                shapeY[1] = 0
                shapeX[2] = 0
                shapeY[2] = 0
                shapeX[3] = 1
                shapeY[3] = 0
            }

            S_FIGURE -> {
                maxOrientation = 2
                color = Configuration.getColor("figure.s", "#a3d5ee")
                shapeX[0] = 0
                shapeY[0] = 0
                shapeX[1] = 1
                shapeY[1] = 0
                shapeX[2] = -1
                shapeY[2] = 1
                shapeX[3] = 0
                shapeY[3] = 1
            }

            Z_FIGURE -> {
                maxOrientation = 2
                color = Configuration.getColor("figure.z", "#f4adff")
                shapeX[0] = -1
                shapeY[0] = 0
                shapeX[1] = 0
                shapeY[1] = 0
                shapeX[2] = 0
                shapeY[2] = 1
                shapeX[3] = 1
                shapeY[3] = 1
            }

            RIGHT_ANGLE_FIGURE -> {
                maxOrientation = 4
                color = Configuration.getColor("figure.right", "#c0b6fa")
                shapeX[0] = -1
                shapeY[0] = 0
                shapeX[1] = 0
                shapeY[1] = 0
                shapeX[2] = 1
                shapeY[2] = 0
                shapeX[3] = 1
                shapeY[3] = 1
            }

            LEFT_ANGLE_FIGURE -> {
                maxOrientation = 4
                color = Configuration.getColor("figure.left", "#f5f4a7")
                shapeX[0] = -1
                shapeY[0] = 0
                shapeX[1] = 0
                shapeY[1] = 0
                shapeX[2] = 1
                shapeY[2] = 0
                shapeX[3] = -1
                shapeY[3] = 1
            }

            TRIANGLE_FIGURE -> {
                maxOrientation = 4
                color = Configuration.getColor("figure.triangle", "#a4d9b6")
                shapeX[0] = -1
                shapeY[0] = 0
                shapeX[1] = 0
                shapeY[1] = 0
                shapeX[2] = 1
                shapeY[2] = 0
                shapeX[3] = 0
                shapeY[3] = 1
            }

            else -> throw IllegalArgumentException("No figure constant: $type")
        }
    }

    /**
     * Checks if this figure is attached to a square board.
     *
     * @return true if the figure is already attached, or false otherwise
     */
    private val isAttached: Boolean
        get() = board != null

    /**
     * Attaches the figure to a specified square board. The figure will be drawn either at the
     * absolute top of the board, with only the bottom line visible, or centered onto the board. In
     * both cases, the squares on the new board are checked for collisions. If the squares are already
     * occupied, this method returns false and no attachment is made.
     *
     *
     * The horizontal and vertical coordinates will be reset for the figure, when centering the
     * figure on the new board. The figure orientation (rotation) will be kept, however. If the figure
     * was previously attached to another board, it will be detached from that board before attaching
     * to the new board.
     *
     * @param board  the square board to attach to
     * @param center the centered position flag
     * @return true if the figure could be attached, or false otherwise
     */
    fun attach(board: SquareBoard, center: Boolean): Boolean {
        val newX: Int = board.boardWidth / 2
        var newY: Int
        var i: Int

        // Check for previous attachment
        if (isAttached) {
            detach()
        }

        // Reset position (for correct controls)
        xPos = 0
        yPos = 0

        // Calculate position
        if (center) {
            newY = board.boardHeight / 2
        } else {
            newY = 0
            i = 0
            while (i < shapeX.size) {
                if (getRelativeY(i, orientation) - newY > 0) {
                    newY = -getRelativeY(i, orientation)
                }
                i++
            }
        }

        // Check position
        this.board = board
        if (!canMoveTo(newX, newY, orientation)) {
            this.board = null
            return false
        }

        // Draw figure
        xPos = newX
        yPos = newY
        paint(color)
        board.update()
        return true
    }

    /**
     * Detaches this figure from its square board. The figure will not be removed from the board by
     * this operation, resulting in the figure being left intact.
     */
    fun detach() {
        board = null
    }

    /**
     * Checks if the figure is fully visible on the square board. If the figure isn't attached to a
     * board, false will be returned.
     *
     * @return true if the figure is fully visible, or false otherwise
     */
    val isAllVisible: Boolean
        get() {
            if (!isAttached) {
                return false
            }
            for (i in shapeX.indices) {
                if (yPos + getRelativeY(i, orientation) < 0) {
                    return false
                }
            }
            return true
        }

    /**
     * Checks if the figure has landed. If this method returns true, the moveDown() or the
     * moveAllWayDown() methods should have no effect. If no square board is attached, this method
     * will return true.
     *
     * @return true if the figure has landed, or false otherwise
     */
    fun hasLanded(): Boolean {
        return !isAttached || !canMoveTo(xPos, yPos + 1, orientation)
    }

    /**
     * Moves the figure one step to the left. If such a move is not possible with respect to the
     * square board, nothing is done. The square board will be changed as the figure moves, clearing
     * the previous cells. If no square board is attached, nothing is done.
     */
    fun moveLeft() {
        if (isAttached && canMoveTo(xPos - 1, yPos, orientation)) {
            paint(null)
            xPos--
            paint(color)
            board!!.update()
        }
    }

    /**
     * Moves the figure one step to the right. If such a move is not possible with respect to the
     * square board, nothing is done. The square board will be changed as the figure moves, clearing
     * the previous cells. If no square board is attached, nothing is done.
     */
    fun moveRight() {
        if (isAttached && canMoveTo(xPos + 1, yPos, orientation)) {
            paint(null)
            xPos++
            paint(color)
            board!!.update()
        }
    }

    /**
     * Moves the figure one step down. If such a move is not possible with respect to the square
     * board, nothing is done. The square board will be changed as the figure moves, clearing the
     * previous cells. If no square board is attached, nothing is done.
     */
    fun moveDown() {
        if (isAttached && canMoveTo(xPos, yPos + 1, orientation)) {
            paint(null)
            yPos++
            paint(color)
            board!!.update()
        }
    }

    /**
     * Moves the figure all the way down. The limits of the move are either the square board bottom,
     * or squares not being empty. If no move is possible with respect to the square board, nothing is
     * done. The square board will be changed as the figure moves, clearing the previous cells. If no
     * square board is attached, nothing is done.
     */
    fun moveAllWayDown() {
        var y = yPos

        // Check for board
        if (!isAttached) {
            return
        }

        // Find the lowest position
        while (canMoveTo(xPos, y + 1, orientation)) {
            y++
        }

        // Update
        if (y != yPos) {
            paint(null)
            yPos = y
            paint(color)
            board!!.update()
        }
    }

    /**
     * Sets the figure rotation (orientation). If the desired rotation is not possible with respect to
     * the square board, nothing is done. The square board will be changed as the figure moves,
     * clearing the previous cells. If no square board is attached, the rotation is performed
     * directly.
     */
    var rotation: Int
        get() = orientation
        set(rotation) {

            // Set new orientation
            val newOrientation: Int = rotation % maxOrientation

            // Check new position
            if (!isAttached) {
                orientation = newOrientation
            } else if (canMoveTo(xPos, yPos, newOrientation)) {
                paint(null)
                orientation = newOrientation
                paint(color)
                board!!.update()
            }
        }

    /**
     * Rotates the figure randomly. If such a rotation is not possible with respect to the square
     * board, nothing is done. The square board will be changed as the figure moves, clearing the
     * previous cells. If no square board is attached, the rotation is performed directly.
     */
    fun rotateRandom() {
        rotation = (Math.random() * 4.0).toInt() % maxOrientation
    }

    /**
     * Rotates the figure clockwise. If such a rotation is not possible with respect to the square
     * board, nothing is done. The square board will be changed as the figure moves, clearing the
     * previous cells. If no square board is attached, the rotation is performed directly.
     */
    fun rotateClockwise() {
        if (maxOrientation != 1) {
            rotation = (orientation + 1) % maxOrientation
        }
    }

    /**
     * Rotates the figure counter-clockwise. If such a rotation is not possible with respect to the
     * square board, nothing is done. The square board will be changed as the figure moves, clearing
     * the previous cells. If no square board is attached, the rotation is performed directly.
     */
    fun rotateCounterClockwise() {
        if (maxOrientation != 1) {
            rotation = (orientation + 3) % 4
        }
    }

    /**
     * Checks if a specified pair of (square) coordinates are inside the figure, or not.
     *
     * @param x the horizontal position
     * @param y the vertical position
     * @return true if the coordinates are inside the figure, or false otherwise
     */
    private fun isInside(x: Int, y: Int): Boolean {
        for (i in shapeX.indices) {
            if (x == xPos + getRelativeX(i, orientation) && y == yPos + getRelativeY(i, orientation)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if the figure can move to a new position. The current figure position is taken into
     * account when checking for collisions. If a collision is detected, this method will return
     * false.
     *
     * @param newX           the new horizontal position
     * @param newY           the new vertical position
     * @param newOrientation the new orientation (rotation)
     * @return true if the figure can be moved, or false otherwise
     */
    private fun canMoveTo(newX: Int, newY: Int, newOrientation: Int): Boolean {
        var x: Int
        var y: Int
        for (i in 0..3) {
            x = newX + getRelativeX(i, newOrientation)
            y = newY + getRelativeY(i, newOrientation)
            if (!isInside(x, y) && !board!!.isSquareEmpty(x, y)) {
                return false
            }
        }
        return true
    }

    /**
     * Returns the relative horizontal position of a specified square. The square will be rotated
     * according to the specified orientation.
     *
     * @param square      the square to rotate (0-3)
     * @param orientation the orientation to use (0-3)
     * @return the rotated relative horizontal position
     */
    private fun getRelativeX(square: Int, orientation: Int) =
        when (orientation % 4) {
            0 -> shapeX[square]
            1 -> -shapeY[square]
            2 -> -shapeX[square]
            3 -> shapeY[square]
            else -> 0 // Should never occur
        }


    /**
     * Rotates the relative vertical position of a specified square. The square will be rotated
     * according to the specified orientation.
     *
     * @param square      the square to rotate (0-3)
     * @param orientation the orientation to use (0-3)
     * @return the rotated relative vertical position
     */
    private fun getRelativeY(square: Int, orientation: Int) =
        when (orientation % 4) {
            0 -> shapeY[square]
            1 -> shapeX[square]
            2 -> -shapeY[square]
            3 -> -shapeX[square]
            else -> 0 // Should never occur
        }


    /**
     * Paints the figure on the board with the specified color.
     *
     * @param color the color to paint with, or null for clearing
     */
    private fun paint(color: Color?) {
        var x: Int
        var y: Int
        for (i in shapeX.indices) {
            x = xPos + getRelativeX(i, orientation)
            y = yPos + getRelativeY(i, orientation)
            board!!.setSquareColor(x, y, color)
        }
    }

    companion object {
        /**
         * A figure constant used to create a figure forming a square.
         */
        const val SQUARE_FIGURE = 1

        /**
         * A figure constant used to create a figure forming a line.
         */
        const val LINE_FIGURE = 2

        /**
         * A figure constant used to create a figure forming an "S".
         */
        const val S_FIGURE = 3

        /**
         * A figure constant used to create a figure forming a "Z".
         */
        const val Z_FIGURE = 4

        /**
         * A figure constant used to create a figure forming a right angle.
         */
        const val RIGHT_ANGLE_FIGURE = 5

        /**
         * A figure constant used to create a figure forming a left angle.
         */
        const val LEFT_ANGLE_FIGURE = 6

        /**
         * A figure constant used to create a figure forming a triangle.
         */
        const val TRIANGLE_FIGURE = 7
    }
}