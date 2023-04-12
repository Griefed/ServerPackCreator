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
package mslinks.data

import io.ByteReader
import io.ByteWriter
import mslinks.Serializable
import java.io.IOException

@Suppress("unused")
class HotKeyFlags : Serializable {
    private var low: Byte
    private var high: Byte

    constructor() {
        high = 0
        low = high
    }

    constructor(data: ByteReader) {
        low = data.read().toByte()
        high = data.read().toByte()
    }

    val key: String?
        get() = keys[low]

    fun setKey(k: String?): HotKeyFlags {
        if (k != null && k != "") low = keysr[k]!!
        return this
    }

    val isShift: Boolean
        get() = high.toInt() and 1 != 0
    val isCtrl: Boolean
        get() = high.toInt() and 2 != 0
    val isAlt: Boolean
        get() = high.toInt() and 4 != 0

    fun setShift(): HotKeyFlags {
        high = (1 or (high.toInt() and 6)).toByte()
        return this
    }

    fun setCtrl(): HotKeyFlags {
        high = (2 or (high.toInt() and 5)).toByte()
        return this
    }

    fun setAlt(): HotKeyFlags {
        high = (4 or (high.toInt() and 3)).toByte()
        return this
    }

    fun clearShift(): HotKeyFlags {
        high = (high.toInt() and 6).toByte()
        return this
    }

    fun clearCtrl(): HotKeyFlags {
        high = (high.toInt() and 5).toByte()
        return this
    }

    fun clearAlt(): HotKeyFlags {
        high = (high.toInt() and 3).toByte()
        return this
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write(low.toInt())
        bw.write(high.toInt())
    }

    companion object {
        private val keys = hashMapOf(
            0x30.toByte() to "0",
            0x31.toByte() to "1",
            0x32.toByte() to "2",
            0x33.toByte() to "3",
            0x34.toByte() to "4",
            0x35.toByte() to "5",
            0x36.toByte() to "6",
            0x37.toByte() to "7",
            0x38.toByte() to "8",
            0x39.toByte() to "9",
            0x41.toByte() to "A",
            0x42.toByte() to "B",
            0x43.toByte() to "C",
            0x44.toByte() to "D",
            0x45.toByte() to "E",
            0x46.toByte() to "F",
            0x47.toByte() to "G",
            0x48.toByte() to "H",
            0x49.toByte() to "I",
            0x4A.toByte() to "J",
            0x4B.toByte() to "K",
            0x4C.toByte() to "L",
            0x4D.toByte() to "M",
            0x4E.toByte() to "N",
            0x4F.toByte() to "O",
            0x50.toByte() to "P",
            0x51.toByte() to "Q",
            0x52.toByte() to "R",
            0x53.toByte() to "S",
            0x54.toByte() to "T",
            0x55.toByte() to "U",
            0x56.toByte() to "V",
            0x57.toByte() to "W",
            0x58.toByte() to "X",
            0x59.toByte() to "Y",
            0x5A.toByte() to "Z",
            0x70.toByte() to "F1",
            0x71.toByte() to "F2",
            0x72.toByte() to "F3",
            0x73.toByte() to "F4",
            0x74.toByte() to "F5",
            0x75.toByte() to "F6",
            0x76.toByte() to "F7",
            0x77.toByte() to "F8",
            0x78.toByte() to "F9",
            0x79.toByte() to "F10",
            0x7A.toByte() to "F11",
            0x7B.toByte() to "F12",
            0x7C.toByte() to "F13",
            0x7D.toByte() to "F14",
            0x7E.toByte() to "F15",
            0x7F.toByte() to "F16",
            0x80.toByte() to "F17",
            0x81.toByte() to "F18",
            0x82.toByte() to "F19",
            0x83.toByte() to "F20",
            0x84.toByte() to "F21",
            0x85.toByte() to "F22",
            0x86.toByte() to "F23",
            0x87.toByte() to "F24",
            0x90.toByte() to "NUM LOCK",
            0x91.toByte() to "SCROLL LOCK",
            0x01.toByte() to "SHIFT",
            0x02.toByte() to "CTRL",
            0x04.toByte() to "ALT"
        )
        private val keysr = HashMap<String, Byte>()

        init {
            for ((key, value) in keys) keysr[value] = key
        }
    }
}
