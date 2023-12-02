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
import mslinks.UnsupportedCLSIDException
import java.io.IOException

abstract class ItemIDRegItem(flags: Int) : ItemID(flags) {
    var clsid: GUID? = null
        protected set

    @Throws(IOException::class, ShellLinkException::class)
    override fun load(br: ByteReader, maxSize: Int) {
        super.load(br, maxSize)
        br.read() // order
        setClsid(GUID(br))
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        super.serialize(bw)
        bw.write(0) // order
        clsid!!.serialize(bw)
    }

    override fun toString(): String {
        val name: String? = try {
            Registry.getName(clsid)
        } catch (e: UnsupportedCLSIDException) {
            this.javaClass.simpleName
        }
        return "<$name>\\"
    }

    @Throws(UnsupportedCLSIDException::class)
    fun setClsid(clsid: GUID?): ItemIDRegItem {
        if (!Registry.canUseClsidIn(clsid, this.javaClass)) throw UnsupportedCLSIDException(clsid)
        this.clsid = clsid
        return this
    }
}
