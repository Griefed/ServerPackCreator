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
import java.io.IOException
import java.util.*

@Suppress("unused")
class VistaIDList : Serializable {
    private val list = LinkedList<ByteArray>()

    constructor()
    constructor(br: ByteReader, size: Int) {
        if (size < 0xa) {
            throw ShellLinkException()
        }
        var s = br.read2bytes().toInt()
        while (s != 0) {
            s -= 2
            val b = ByteArray(s)
            for (i in 0 until s) {
                b[i] = br.read().toByte()
            }
            list.add(b)
            s = br.read2bytes().toInt()
        }
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        var size = 10
        for (i in list) {
            size += i.size + 2
        }
        bw.write2bytes(size.toLong())
        for (i in list) {
            bw.write2bytes((i.size + 2).toLong())
            for (j in i) {
                bw.write(j.toInt())
            }
        }
        bw.write2bytes(0)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (b in list) {
            sb.append(String(b) + "\n")
        }
        return sb.toString()
    }

    companion object {
        const val signature = -0x5ffffff4
    }
}
