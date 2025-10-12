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
import mslinks.data.GUID
import java.io.IOException

@Suppress("MemberVisibilityCanBePrivate", "unused")
class Tracker : Serializable {
    var netbiosName: String?
        private set
    private var d1: GUID
    private var d2: GUID
    private var db1: GUID
    private var db2: GUID

    constructor() {
        netbiosName = "localhost"
        db1 = GUID()
        d1 = db1
        db2 = GUID("539D9DC6-8293-11E3-8FB0-005056C00008")
        d2 = db2
    }

    constructor(br: ByteReader, sz: Int) {
        if (sz != SIZE) {
            throw ShellLinkException()
        }
        val len = br.read4bytes().toInt()
        if (len < 0x58) {
            throw ShellLinkException()
        }
        br.read4bytes()
        val pos = br.position
        netbiosName = br.readString(16)
        br.seek(pos + 16 - br.position)
        d1 = GUID(br)
        d2 = GUID(br)
        db1 = GUID(br)
        db2 = GUID(br)
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write4bytes(SIZE.toLong())
        bw.write4bytes(SIGNATURE.toLong())
        bw.write4bytes(0x58)
        bw.write4bytes(0)
        val b = netbiosName!!.toByteArray()
        bw.write(b)
        for (i in 0 until 16 - b.size) {
            bw.write(0)
        }
        d1.serialize(bw)
        d2.serialize(bw)
        db1.serialize(bw)
        db2.serialize(bw)
    }

    @Throws(ShellLinkException::class)
    fun setNetbiosName(s: String): Tracker {
        if (s.length > 16) {
            throw ShellLinkException("netbios name length must be <= 16")
        }
        netbiosName = s
        return this
    }

    companion object {
        const val SIGNATURE = -0x5ffffffd
        const val SIZE = 0x60
    }
}
