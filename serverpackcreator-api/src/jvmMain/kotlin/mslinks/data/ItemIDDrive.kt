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
import mslinks.UnsupportedItemIDException
import java.io.IOException
import java.util.regex.Pattern

open class ItemIDDrive(flags: Int) : ItemID(flags or GROUP_COMPUTER) {
    override var name: String? = null
        protected set

    init {
        val subType = typeFlags and ID_TYPE_INGROUPMASK
        if (subType == 0) throw UnsupportedItemIDException(typeFlags)
    }

    @Throws(IOException::class, ShellLinkException::class)
    override fun load(br: ByteReader, maxSize: Int) {
        val startPos = br.position
        val endPos = startPos + maxSize
        super.load(br, maxSize)
        setName(br.readString(4))
        // 8 bytes: drive size
        // 8 bytes: drive free size
        // 1 byte: 0/1 - has drive extension
        // 1 byte: 0/1 - drive extension has class id
        // 16 bytes: clsid - only possible value is CDBurn
        br.seekTo(endPos)
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        super.serialize(bw)
        bw.writeString(name)
        bw.write8bytes(0) // drive size
        bw.write8bytes(0) // drive free size
        bw.write(0) // no extension
        bw.write(0) // no clsid
    }

    override fun toString(): String {
        return name!!
    }

    @Deprecated("")
    @Throws(ShellLinkException::class)
    override fun setName(s: String?): ItemIDDrive {
        if (s == null) return this
        name = if (Pattern.matches("\\w:\\\\", s)) s else if (Pattern.matches(
                "\\w:",
                s
            )
        ) s + "\\" else if (Pattern.matches(
                "\\w",
                s
            )
        ) "$s:\\" else throw ShellLinkException("wrong drive name: $s")
        return this
    }
}
