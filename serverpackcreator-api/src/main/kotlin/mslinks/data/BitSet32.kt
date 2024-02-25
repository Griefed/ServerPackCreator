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

open class BitSet32 : Serializable {
    private var d: Int

    constructor(n: Int) {
        d = n
    }

    constructor(data: ByteReader) {
        d = data.read4bytes().toInt()
    }

    protected operator fun get(i: Int): Boolean {
        return d and (1 shl i) != 0
    }

    protected fun set(i: Int) {
        d = d and (1 shl i).inv() or (1 shl i)
    }

    protected fun clear(i: Int) {
        d = d and (1 shl i).inv()
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write4bytes(d.toLong())
    }
}
