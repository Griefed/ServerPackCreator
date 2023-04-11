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
package mslinks

import io.ByteReader
import io.ByteWriter
import mslinks.data.*
import java.io.IOException

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ShellLinkHeader : Serializable {
    var linkFlags: LinkFlags
        private set
    var fileAttributesFlags: FileAttributesFlags
        private set
    var creationTime: Filetime
        private set
    var accessTime: Filetime
        private set
    var writeTime: Filetime
        private set
    var fileSize = 0
        private set
    var iconIndex = 0
        private set
    var showCommand: Int
        private set
    var hotKeyFlags: HotKeyFlags
        private set

    constructor() {
        linkFlags = LinkFlags(0)
        fileAttributesFlags = FileAttributesFlags(0)
        creationTime = Filetime()
        accessTime = Filetime()
        writeTime = Filetime()
        showCommand = SW_SHOWNORMAL
        hotKeyFlags = HotKeyFlags()
    }

    constructor(data: ByteReader) {
        val size = data.read4bytes().toInt()
        if (size != headerSize) throw ShellLinkException()
        val g = GUID(data)
        if (g != clsid) throw ShellLinkException()
        linkFlags = LinkFlags(data)
        fileAttributesFlags = FileAttributesFlags(data)
        creationTime = Filetime(data)
        accessTime = Filetime(data)
        writeTime = Filetime(data)
        fileSize = data.read4bytes().toInt()
        iconIndex = data.read4bytes().toInt()
        showCommand = data.read4bytes().toInt()
        if (showCommand != SW_SHOWNORMAL && showCommand != SW_SHOWMAXIMIZED && showCommand != SW_SHOWMINNOACTIVE) throw ShellLinkException()
        hotKeyFlags = HotKeyFlags(data)
        data.read2bytes()
        data.read8bytes()
    }

    fun setFileSize(n: Long): ShellLinkHeader {
        fileSize = n.toInt()
        return this
    }

    fun setIconIndex(n: Int): ShellLinkHeader {
        iconIndex = n
        return this
    }

    @Throws(ShellLinkException::class)
    fun setShowCommand(n: Int): ShellLinkHeader {
        return if (n == SW_SHOWNORMAL || n == SW_SHOWMAXIMIZED || n == SW_SHOWMINNOACTIVE) {
            showCommand = n
            this
        } else throw ShellLinkException()
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        bw.write4bytes(headerSize.toLong())
        clsid.serialize(bw)
        linkFlags.serialize(bw)
        fileAttributesFlags.serialize(bw)
        creationTime.serialize(bw)
        accessTime.serialize(bw)
        writeTime.serialize(bw)
        bw.write4bytes(fileSize.toLong())
        bw.write4bytes(iconIndex.toLong())
        bw.write4bytes(showCommand.toLong())
        hotKeyFlags.serialize(bw)
        bw.write2bytes(0)
        bw.write8bytes(0)
    }

    companion object {
        private const val headerSize = 0x0000004C
        private val clsid = GUID("00021401-0000-0000-C000-000000000046")
        const val SW_SHOWNORMAL = 1
        const val SW_SHOWMAXIMIZED = 3
        const val SW_SHOWMINNOACTIVE = 7
    }
}
