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
@file:Suppress("unused")

package io

object Bytes {
    fun reverse(n: Short): Short {
        return (n.toInt() and 0xff shl 8 or (n.toInt() and 0xff00 shr 8)).toShort()
    }

    fun reverse(n: Int): Int {
        return n and 0xff shl 24 or (n and 0xff00 shl 8) or (n and 0xff0000 shr 8) or (n and -0x1000000 ushr 24)
    }

    fun reverse(n: Long): Long {
        return n and 0xffL shl 56 or (n and 0xff00L shl 40) or (n and 0xff0000L shl 24) or (n and 0xff000000L shl 8) or
                (n and 0xff00000000L shr 8) or (n and 0xff0000000000L shr 24) or (n and 0xff000000000000L shr 40) or (n and -0x100000000000000L ushr 56)
    }

    fun makeShortB(b0: Byte, b1: Byte): Short {
        return (i(b0) shl 8 or i(b1)).toShort()
    }

    fun makeIntB(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
        return i(b0) shl 24 or (i(b1) shl 16) or (i(b2) shl 8) or i(b3)
    }

    fun makeLongB(b0: Byte, b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte, b7: Byte): Long {
        return l(b0) shl 56 or (l(b1) shl 48) or (l(b2) shl 40) or (l(b3) shl 32) or (l(b4) shl 24) or (l(b5) shl 16) or (l(
            b6
        ) shl 8) or l(b7)
    }

    fun makeShortL(b0: Byte, b1: Byte): Short {
        return (i(b1) shl 8 or i(b0)).toShort()
    }

    fun makeIntL(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
        return i(b3) shl 24 or (i(b2) shl 16) or (i(b1) shl 8) or i(b0)
    }

    fun makeLongL(b0: Byte, b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte, b7: Byte): Long {
        return l(b7) shl 56 or (l(b6) shl 48) or (l(b5) shl 40) or (l(b4) shl 32) or (l(b3) shl 24) or (l(b2) shl 16) or (l(
            b1
        ) shl 8) or l(b0)
    }

    fun l(b: Byte): Long {
        return b.toLong() and 0xffL
    }

    fun i(b: Byte): Int {
        return b.toInt() and 0xff
    }
}
