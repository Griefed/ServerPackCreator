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
import java.util.*

class Filetime : GregorianCalendar, Serializable {
    private var residue: Long = 0

    constructor() : super()
    constructor(data: ByteReader) : this(data.read8bytes())
    constructor(time: Long) {
        val t = time / 10000
        residue = time - t
        timeInMillis = t
        add(YEAR, -369)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val obj = other as Filetime
        return residue == obj.residue
    }

    override fun hashCode(): Int {
        return (super.hashCode().toLong() xor (residue and -0x100000000L shr 32) xor (residue and 0xffffffffL)).toInt()
    }

    fun toLong(): Long {
        val tmp = clone() as GregorianCalendar
        tmp.add(YEAR, 369)
        return tmp.timeInMillis + residue
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write8bytes(toLong())
    }

    override fun toString(): String {
        return String.format(
            "%02d:%02d:%02d %02d.%02d.%04d",
            get(HOUR_OF_DAY), get(MINUTE), get(SECOND),
            get(DAY_OF_MONTH), get(MONTH) + 1, get(YEAR)
        )
    }
}
