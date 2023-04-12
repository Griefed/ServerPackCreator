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

import io.ByteWriter
import mslinks.Serializable
import java.io.IOException

@Suppress("unused")
class Size : Serializable {
    var x: Int
        private set
    var y: Int
        private set

    constructor() {
        y = 0
        x = y
    }

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun setX(x: Int): Size {
        this.x = x
        return this
    }

    fun setY(y: Int): Size {
        this.y = y
        return this
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write2bytes(x.toLong())
        bw.write2bytes(y.toLong())
    }
}
