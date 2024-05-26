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
import java.io.IOException

class Stub(br: ByteReader, sz: Int, sgn: Int) : Serializable {
    private val sign: Int
    private val data: ByteArray

    init {
        val len = sz - 8
        sign = sgn
        data = ByteArray(len)
        for (i in 0 until len) {
            data[i] = br.read().toByte()
        }
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write4bytes((data.size + 8).toLong())
        bw.write4bytes(sign.toLong())
        bw.write(data)
    }
}
