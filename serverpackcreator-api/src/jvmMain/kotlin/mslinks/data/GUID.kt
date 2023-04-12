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
import io.Bytes
import mslinks.Serializable
import java.io.IOException
import java.util.*

@Suppress("unused")
class GUID : Serializable {
    private var d1: Int
    private var d2: Short
    private var d3: Short
    private var d4: Short
    private var d5: Long

    constructor() {
        d1 = r.nextInt()
        d2 = r.nextInt().toShort()
        d3 = r.nextInt().toShort()
        d4 = r.nextInt().toShort()
        d5 = r.nextLong() and 0xffffffffffffL
    }

    constructor(d: ByteArray) {
        d1 = Bytes.makeIntL(d[0], d[1], d[2], d[3])
        d2 = Bytes.makeShortL(d[4], d[5])
        d3 = Bytes.makeShortL(d[6], d[7])
        d4 = Bytes.makeShortB(d[8], d[9])
        d5 = Bytes.makeLongB(0.toByte(), 0.toByte(), d[10], d[11], d[12], d[13], d[14], d[15])
    }

    constructor(data: ByteReader) {
        d1 = data.read4bytes().toInt()
        d2 = data.read2bytes().toShort()
        d3 = data.read2bytes().toShort()
        data.changeEndiannes()
        d4 = data.read2bytes().toShort()
        d5 = data.read6bytes()
        data.changeEndiannes()
    }

    constructor(id: String) {
        var s = id
        if (s[0] == '{' && s[s.length - 1] == '}') s = s.substring(1, s.length - 1)
        val p = s.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var b = parse(p[0])
        d1 = Bytes.makeIntB(b[0], b[1], b[2], b[3])
        b = parse(p[1])
        d2 = Bytes.makeShortB(b[0], b[1])
        b = parse(p[2])
        d3 = Bytes.makeShortB(b[0], b[1])
        d4 = p[3].toLong(16).toShort()
        d5 = p[4].toLong(16)
    }

    private fun parse(s: String): ByteArray {
        val b = ByteArray(s.length shr 1)
        var i = 0
        var j = 0
        while (j < s.length) {
            b[i] = s.substring(j, j + 2).toLong(16).toByte()
            i++
            j += 2
        }
        return b
    }

    override fun toString(): String {
        return String.format("%08X-%04X-%04X-%04X-%012X", d1, d2, d3, d4, d5)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || javaClass != other.javaClass) return false
        val g = other as GUID
        return d1 == g.d1 && d2 == g.d2 && d3 == g.d3 && d4 == g.d4 && d5 == g.d5
    }

    override fun hashCode(): Int {
        return ((d1 xor d2.toInt() xor d3.toInt() xor d4.toInt()).toLong() xor (d5 and -0x100000000L shr 32) xor (d5 and 0xffffffffL)).toInt()
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write4bytes(d1.toLong())
        bw.write2bytes(d2.toLong())
        bw.write2bytes(d3.toLong())
        bw.changeEndiannes()
        bw.write2bytes(d4.toLong())
        bw.write6bytes(d5)
        bw.changeEndiannes()
    }

    companion object {
        private val r = Random()
    }
}
