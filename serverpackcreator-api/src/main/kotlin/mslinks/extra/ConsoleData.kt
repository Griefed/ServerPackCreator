/*
	https://github.com/BlackOverlord666/mslinks
	
	Copyright (c) 2015 Dmitrii Shamrikov

	Licensed under the WTFPL
	You may obtain a copy of the License at
 
	http://www.wtfpl.net/about/
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
package mslinks.extra

import io.ByteReader
import io.ByteWriter
import mslinks.Serializable
import mslinks.ShellLinkException
import mslinks.data.ConsoleFlags
import mslinks.data.Size
import java.io.IOException

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ConsoleData : Serializable {
    val consoleFlags = ConsoleFlags(0)

    /** get index in array returned by getColorTable() method  */
    var textColor: Int
        private set

    /** get index in array returned by getColorTable() method  */
    var textBackground: Int
        private set

    /** get index in array returned by getColorTable() method  */
    var popupTextColor: Int
        private set

    /** get index in array returned by getColorTable() method  */
    var popupTextBackground: Int
        private set
    var bufferSize: Size
        private set
    var windowSize: Size
        private set
    var windowPos: Size
        private set
    var fontSize: Int
        private set
    var font: Font? = null
        private set
    var cursorSize: CursorSize? = null
        private set
    var historySize: Int
        private set
    var historyBuffers: Int
        private set
    val colorTable = IntArray(16)

    constructor() {
        textColor = 7
        textBackground = 0
        popupTextColor = 5
        popupTextBackground = 15
        bufferSize = Size(80, 300)
        windowSize = Size(80, 25)
        windowPos = Size()
        fontSize = 14
        font = Font.Terminal
        cursorSize = CursorSize.Small
        historySize = 50
        historyBuffers = 4
        consoleFlags.setInsertMode()
        consoleFlags.setAutoPosition()
        var i = 0
        colorTable[i++] = rgb(0, 0, 0)
        colorTable[i++] = rgb(0, 0, 128)
        colorTable[i++] = rgb(0, 128, 0)
        colorTable[i++] = rgb(0, 128, 128)
        colorTable[i++] = rgb(128, 0, 0)
        colorTable[i++] = rgb(128, 0, 128)
        colorTable[i++] = rgb(128, 128, 0)
        colorTable[i++] = rgb(192, 192, 192)
        colorTable[i++] = rgb(128, 128, 128)
        colorTable[i++] = rgb(0, 0, 255)
        colorTable[i++] = rgb(0, 255, 0)
        colorTable[i++] = rgb(0, 255, 255)
        colorTable[i++] = rgb(255, 0, 0)
        colorTable[i++] = rgb(255, 0, 255)
        colorTable[i++] = rgb(255, 255, 0)
        colorTable[i++] = rgb(255, 255, 255)
    }

    constructor(br: ByteReader, sz: Int) {
        if (sz != SIZE) {
            throw ShellLinkException()
        }
        var t = br.read2bytes().toInt()
        textColor = t and 0xf
        textBackground = t and 0xf0 shr 4
        t = br.read2bytes().toInt()
        popupTextColor = t and 0xf
        popupTextBackground = t and 0xf0 shr 4
        bufferSize = Size(br.read2bytes().toInt(), br.read2bytes().toInt())
        windowSize = Size(br.read2bytes().toInt(), br.read2bytes().toInt())
        windowPos = Size(br.read2bytes().toInt(), br.read2bytes().toInt())
        br.read8bytes()
        fontSize = br.read4bytes().toInt() ushr 16
        br.read4bytes()
        if (br.read4bytes().toInt() >= 700) {
            consoleFlags.setBoldFont()
        }
        font = when (br.read().toChar()) {
            'T' -> Font.Terminal
            'L' -> Font.LucidaConsole
            'C' -> Font.Consolas
            else -> throw ShellLinkException("unknown font type")
        }
        br.seek(63)
        t = br.read4bytes().toInt()
        cursorSize = when {
            t <= 25 -> {
                CursorSize.Small
            }

            t <= 50 -> {
                CursorSize.Medium
            }

            else -> {
                CursorSize.Large
            }
        }
        if (br.read4bytes().toInt() != 0) {
            consoleFlags.setFullscreen()
        }
        if (br.read4bytes().toInt() != 0) {
            consoleFlags.setQuickEdit()
        }
        if (br.read4bytes().toInt() != 0) {
            consoleFlags.setInsertMode()
        }
        if (br.read4bytes().toInt() != 0) {
            consoleFlags.setAutoPosition()
        }
        historySize = br.read4bytes().toInt()
        historyBuffers = br.read4bytes().toInt()
        if (br.read4bytes().toInt() != 0) {
            consoleFlags.setHistoryDup()
        }
        for (i in 0..15) {
            colorTable[i] = br.read4bytes().toInt()
        }
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write4bytes(SIZE.toLong())
        bw.write4bytes(SIGNATURE.toLong())
        bw.write2bytes((textColor or (textBackground shl 4)).toLong())
        bw.write2bytes((popupTextColor or (popupTextBackground shl 4)).toLong())
        bufferSize.serialize(bw)
        windowSize.serialize(bw)
        windowPos.serialize(bw)
        bw.write8bytes(0)
        bw.write4bytes((fontSize shl 16).toLong())
        var byteToWrite = if (font == Font.Terminal) {
            0x30
        } else {
            0x36
        }
        bw.write4bytes(byteToWrite.toLong())
        byteToWrite = if (consoleFlags.isBoldFont) {
            700
        } else {
            0
        }
        bw.write4bytes(byteToWrite.toLong())
        var fn = ""
        when (font) {
            Font.Terminal -> fn = "Terminal"
            Font.LucidaConsole -> fn = "Lucida Console"
            Font.Consolas -> fn = "Consolas"
            else -> {}
        }
        bw.writeUnicodeStringNullTerm(fn)
        for (i in fn.length + 1..31) {
            bw.write2bytes(0)
        }
        when (cursorSize) {
            CursorSize.Small -> bw.write4bytes(0)
            CursorSize.Medium -> bw.write4bytes(26)
            CursorSize.Large -> bw.write4bytes(51)
            else -> {}
        }
        byteToWrite = if (consoleFlags.isFullscreen) {
            1
        } else {
            0
        }
        bw.write4bytes(byteToWrite.toLong())
        byteToWrite = if (consoleFlags.isQuickEdit) {
            1
        } else {
            0
        }
        bw.write4bytes(byteToWrite.toLong())
        byteToWrite = if (consoleFlags.isInsertMode) {
            1
        } else {
            0
        }
        bw.write4bytes(byteToWrite.toLong())
        byteToWrite = if (consoleFlags.isAutoPosition) {
            1
        } else {
            0
        }
        bw.write4bytes(byteToWrite.toLong())
        bw.write4bytes(historySize.toLong())
        bw.write4bytes(historyBuffers.toLong())
        byteToWrite = if (consoleFlags.isHistoryDup) {
            1
        } else {
            0
        }
        bw.write4bytes(byteToWrite.toLong())
        for (i in 0..15) {
            bw.write4bytes(colorTable[i].toLong())
        }
    }

    /** set index in array returned by getColorTable() method  */
    fun setTextColor(n: Int): ConsoleData {
        textColor = n
        return this
    }

    /** set index in array returned by getColorTable() method  */
    fun setTextBackground(n: Int): ConsoleData {
        textBackground = n
        return this
    }

    /** set index in array returned by getColorTable() method  */
    fun setPopupTextColor(n: Int): ConsoleData {
        popupTextColor = n
        return this
    }

    /** set index in array returned by getColorTable() method  */
    fun setPopupTextBackground(n: Int): ConsoleData {
        popupTextBackground = n
        return this
    }

    fun setFontSize(n: Int): ConsoleData {
        fontSize = n
        return this
    }

    fun setFont(f: Font?): ConsoleData {
        font = f
        return this
    }

    fun setCursorSize(cs: CursorSize?): ConsoleData {
        cursorSize = cs
        return this
    }

    fun setHistorySize(n: Int): ConsoleData {
        historySize = n
        return this
    }

    fun setHistoryBuffers(n: Int): ConsoleData {
        historyBuffers = n
        return this
    }

    /**
     * only these fonts are working...
     */
    enum class Font {
        Terminal,
        LucidaConsole,
        Consolas
    }

    enum class CursorSize {
        Small,
        Medium,
        Large
    }

    companion object {
        const val SIGNATURE = -0x5ffffffe
        const val SIZE = 0xcc
        fun rgb(r: Int, g: Int, b: Int): Int {
            return r and 0xff or (g and 0xff shl 8) or (b and 0xff shl 16)
        }

        fun r(rgb: Int): Int {
            return rgb and 0xff
        }

        fun g(rgb: Int): Int {
            return rgb and 0xff00 shr 8
        }

        fun b(rgb: Int): Int {
            return rgb and 0xff0000 shr 16
        }
    }
}
