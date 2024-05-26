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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

@Suppress("unused")
class LinkTargetIDList : LinkedList<ItemID?>, Serializable {
    constructor()
    constructor(data: ByteReader) {
        val size = data.read2bytes().toInt()
        var pos = data.position
        while (true) {
            val itemSize = data.read2bytes().toInt()
            if (itemSize == 0) {
                break
            }
            val typeFlags = data.read()
            val item: ItemID = ItemID.createItem(typeFlags)
            item.load(data, itemSize - 3)
            add(item)
        }
        pos = data.position - pos
        if (pos != size) {
            throw ShellLinkException("unexpected size of LinkTargetIDList")
        }
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        var size = 2
        val b = arrayOfNulls<ByteArray>(size)
        for ((i, j) in this.withIndex()) {
            val ba = ByteArrayOutputStream()
            val w = ByteWriter(ba)
            j?.serialize(w)
            b[i] = ba.toByteArray()
        }
        for (j in b) size += j!!.size + 2
        bw.write2bytes(size.toLong())
        for (j in b) {
            bw.write2bytes((j!!.size + 2).toLong())
            bw.write(j)
        }
        bw.write2bytes(0)
    }

    @get:Deprecated("", ReplaceWith("canBuildPath()"))
    val isCorrect: Boolean
        get() = canBuildPath()

    fun canBuildPath(): Boolean {
        for (i in this) {
            if (i is ItemIDUnknown) {
                return false
            }
        }
        return true
    }

    fun canBuildAbsolutePath(): Boolean {
        if (size < 2) {
            return false
        }
        val firstId = first as? ItemIDRoot ?: return false
        if (firstId.clsid != Registry.CLSID_COMPUTER) {
            return false
        }
        val secondId = get(1)
        return secondId is ItemIDDrive
    }

    fun buildPath(): String {
        val path = StringBuilder()
        if (isNotEmpty()) {
            // when a link created by drag'n'drop menu from desktop, id list starts from filename directly
            val firstId = first
            if (firstId is ItemIDFS) {
                path.append("<Desktop>\\")
            }
            for (i in this) {
                path.append(i.toString())
            }
        }
        return path.toString()
    }
}
