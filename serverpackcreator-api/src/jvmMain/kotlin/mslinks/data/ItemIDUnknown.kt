/*
	https://github.com/DmitriiShamrikov/mslinks
	
	Copyright (c) 2022 Dmitrii Shamrikov

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
import mslinks.ShellLinkException
import java.io.IOException

open class ItemIDUnknown(flags: Int) : ItemID(flags) {
    protected lateinit var data: ByteArray

    @Throws(IOException::class, ShellLinkException::class)
    override fun load(br: ByteReader, maxSize: Int) {
        val startPos = br.position
        super.load(br, maxSize)
        val bytesRead = br.position - startPos
        data = ByteArray(maxSize - bytesRead)
        br.read(data)
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        super.serialize(bw)
        bw.write(data)
    }

    override fun toString(): String {
        return String.format("<ItemIDUnknown 0x%02X>", typeFlags)
    }
}
