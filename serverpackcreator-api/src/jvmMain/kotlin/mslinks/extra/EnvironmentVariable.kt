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

@Suppress("unused")
class EnvironmentVariable : Serializable {
    var variable: String?
        private set

    constructor() {
        variable = ""
    }

    constructor(br: ByteReader, sz: Int) {
        if (sz != size) throw ShellLinkException()
        var pos = br.position
        variable = br.readString(260)
        br.seekTo(pos + 260)
        pos = br.position
        val unicodeStr = br.readUnicodeStringNullTerm(260)
        br.seekTo(pos + 520)
        if (unicodeStr != null && unicodeStr != "") variable = unicodeStr
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write4bytes(size.toLong())
        bw.write4bytes(signature.toLong())
        val b = variable!!.toByteArray()
        bw.write(b)
        for (i in 0 until 260 - b.size) bw.write(0)
        for (i in 0 until variable!!.length) bw.write2bytes(variable!![i].code.toLong())
        for (i in 0 until 260 - variable!!.length) bw.write2bytes(0)
    }

    fun setVariable(s: String?): EnvironmentVariable {
        variable = s
        return this
    }

    companion object {
        const val signature = -0x5fffffff
        const val size = 0x314
    }
}
