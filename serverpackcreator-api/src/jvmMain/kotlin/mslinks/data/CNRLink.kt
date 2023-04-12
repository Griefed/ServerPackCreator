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
import mslinks.ShellLinkException
import java.io.IOException
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets

@Suppress("unused")
class CNRLink : Serializable {
    private var flags: CNRLinkFlags?
    @Suppress("MemberVisibilityCanBePrivate")
    var networkType: Int
        private set
    @Suppress("MemberVisibilityCanBePrivate")
    var netName: String?
        private set
    @Suppress("MemberVisibilityCanBePrivate")
    var deviceName: String? = null
        private set

    constructor() {
        flags = CNRLinkFlags(0).setValidNetType()
        networkType = WNNC_NET_DECORB
        netName = ""
    }

    constructor(data: ByteReader) {
        val pos = data.position
        val size = data.read4bytes().toInt()
        if (size < 0x14) throw ShellLinkException()
        flags = CNRLinkFlags(data)
        val nnoffset = data.read4bytes().toInt()
        var dnoffset = data.read4bytes().toInt()
        if (!flags!!.isValidDevice) dnoffset = 0
        networkType = data.read4bytes().toInt()
        if (flags!!.isValidNetType) checkNpType(networkType) else networkType = 0
        var nnoffsetU = 0
        var dnoffsetU = 0
        if (nnoffset > 0x14) {
            nnoffsetU = data.read4bytes().toInt()
            dnoffsetU = data.read4bytes().toInt()
        }
        data.seek(pos + nnoffset - data.position)
        netName = data.readString(pos + size - data.position)
        if (dnoffset != 0) {
            data.seek(pos + dnoffset - data.position)
            deviceName = data.readString(pos + size - data.position)
        }
        if (nnoffsetU != 0) {
            data.seek(pos + nnoffsetU - data.position)
            netName = data.readUnicodeStringNullTerm(pos + size - data.position)
        }
        if (dnoffsetU != 0) {
            data.seek(pos + dnoffsetU - data.position)
            deviceName = data.readUnicodeStringNullTerm(pos + size - data.position)
        }
    }

    @Throws(ShellLinkException::class)
    private fun checkNpType(type: Int) {
        val mod = Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL
        for (f in this.javaClass.fields) {
            try {
                if (f.modifiers and mod == mod && type == f[null] as Int) return
            } catch (_: Exception) {
            }
        }
        throw ShellLinkException("incorrect network type")
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        var size = 20
        val u: Boolean
        val ce = StandardCharsets.US_ASCII.newEncoder()
        u = !ce.canEncode(netName) || deviceName != null && !ce.canEncode(deviceName)
        if (u) size += 8
        val netnameB: ByteArray?
        var devnameB: ByteArray? = null
        netnameB = netName!!.toByteArray()
        if (deviceName != null) devnameB = deviceName!!.toByteArray()
        size += netnameB.size + 1
        if (devnameB != null) size += devnameB.size + 1
        if (u) {
            size += netName!!.length * 2 + 2
            if (deviceName != null) size += deviceName!!.length * 2 + 2
        }
        bw.write4bytes(size.toLong())
        flags!!.serialize(bw)
        var off = 20
        if (u) off += 8
        bw.write4bytes(off.toLong()) // netname offset
        off += netnameB.size + 1
        if (devnameB != null) {
            bw.write4bytes(off.toLong()) // devname offset
            off += devnameB.size + 1
        } else bw.write4bytes(0)
        bw.write4bytes(networkType.toLong())
        if (u) {
            bw.write4bytes(off.toLong())
            off += netName!!.length * 2 + 2
            if (deviceName != null) {
                bw.write4bytes(off.toLong())
                off += deviceName!!.length * 2 + 2
            } else bw.write4bytes(0)
        }
        bw.write(netnameB)
        bw.write(0)
        if (devnameB != null) {
            bw.write(devnameB)
            bw.write(0)
        }
        if (u) {
            for (i in 0 until netName!!.length) bw.write2bytes(netName!![i].code.toLong())
            bw.write2bytes(0)
            if (deviceName != null) {
                for (i in 0 until deviceName!!.length) bw.write2bytes(deviceName!![i].code.toLong())
                bw.write2bytes(0)
            }
        }
    }

    /**
     * pass zero to switch off network type
     */
    @Throws(ShellLinkException::class)
    fun setNetworkType(n: Int): CNRLink {
        networkType = if (n == 0) {
            flags!!.clearValidNetType()
            n
        } else {
            checkNpType(n)
            flags!!.setValidNetType()
            n
        }
        return this
    }

    /**
     * if s is null take no effect
     */
    fun setNetName(s: String?): CNRLink {
        if (s != null) netName = s
        return this
    }

    /**
     * pass null to switch off device info
     */
    fun setDeviceName(s: String?): CNRLink {
        if (s == null) {
            deviceName = null
            flags!!.clearValidDevice()
        } else {
            deviceName = s
            flags!!.setValidDevice()
        }
        return this
    }

    companion object {
        const val WNNC_NET_AVID = 0x001A000
        const val WNNC_NET_DOCUSPACE = 0x001B000
        const val WNNC_NET_MANGOSOFT = 0x001C000
        const val WNNC_NET_SERNET = 0x001D000
        const val WNNC_NET_RIVERFRONT1 = 0X001E000
        const val WNNC_NET_RIVERFRONT2 = 0x001F000
        const val WNNC_NET_DECORB = 0x0020000
        const val WNNC_NET_PROTSTOR = 0x0021000
        const val WNNC_NET_FJ_REDIR = 0x0022000
        const val WNNC_NET_DISTINCT = 0x0023000
        const val WNNC_NET_TWINS = 0x0024000
        const val WNNC_NET_RDR2SAMPLE = 0x0025000
        const val WNNC_NET_CSC = 0x0026000
        const val WNNC_NET_3IN1 = 0x0027000
        const val WNNC_NET_EXTENDNET = 0x0029000
        const val WNNC_NET_STAC = 0x002A000
        const val WNNC_NET_FOXBAT = 0x002B000
        const val WNNC_NET_YAHOO = 0x002C000
        const val WNNC_NET_EXIFS = 0x002D000
        const val WNNC_NET_DAV = 0x002E000
        const val WNNC_NET_KNOWARE = 0x002F000
        const val WNNC_NET_OBJECT_DIRE = 0x0030000
        const val WNNC_NET_MASFAX = 0x0031000
        const val WNNC_NET_HOB_NFS = 0x0032000
        const val WNNC_NET_SHIVA = 0x0033000
        const val WNNC_NET_IBMAL = 0x0034000
        const val WNNC_NET_LOCK = 0x0035000
        const val WNNC_NET_TERMSRV = 0x0036000
        const val WNNC_NET_SRT = 0x0037000
        const val WNNC_NET_QUINCY = 0x0038000
        const val WNNC_NET_OPENAFS = 0x0039000
        const val WNNC_NET_AVID1 = 0X003A000
        const val WNNC_NET_DFS = 0x003B000
        const val WNNC_NET_KWNP = 0x003C000
        const val WNNC_NET_ZENWORKS = 0x003D000
        const val WNNC_NET_DRIVEONWEB = 0x003E000
        const val WNNC_NET_VMWARE = 0x003F000
        const val WNNC_NET_RSFX = 0x0040000
        const val WNNC_NET_MFILES = 0x0041000
        const val WNNC_NET_MS_NFS = 0x0042000
        const val WNNC_NET_GOOGLE = 0x0043000
    }
}
