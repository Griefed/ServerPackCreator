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
import mslinks.data.CNRLink
import mslinks.data.LinkInfoFlags
import mslinks.data.VolumeID
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

@Suppress("MemberVisibilityCanBePrivate")
class LinkInfo : Serializable {
    private var lif: LinkInfoFlags
    var volumeID: VolumeID? = null
        private set
    var localBasePath: String? = null
        private set
    var commonNetworkRelativeLink: CNRLink? = null
        private set
    var commonPathSuffix: String? = null
        private set

    constructor() {
        lif = LinkInfoFlags(0)
    }

    constructor(data: ByteReader) {
        val pos = data.position
        val size = data.read4bytes().toInt()
        val hsize = data.read4bytes().toInt()
        lif = LinkInfoFlags(data)
        val vidoffset = data.read4bytes().toInt()
        val lbpoffset = data.read4bytes().toInt()
        val cnrloffset = data.read4bytes().toInt()
        val cpsoffset = data.read4bytes().toInt()
        var lbpoffsetU = 0
        var cpfoffsetU = 0
        if (hsize >= 0x24) {
            lbpoffsetU = data.read4bytes().toInt()
            cpfoffsetU = data.read4bytes().toInt()
        }
        if (lif.hasVolumeIDAndLocalBasePath()) {
            data.seek(pos + vidoffset - data.position)
            volumeID = VolumeID(data)
            data.seek(pos + lbpoffset - data.position)
            localBasePath = data.readString(pos + size - data.position)
        }
        if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
            data.seek(pos + cnrloffset - data.position)
            commonNetworkRelativeLink = CNRLink(data)
            data.seek(pos + cpsoffset - data.position)
            commonPathSuffix = data.readString(pos + size - data.position)
        }
        if (lif.hasVolumeIDAndLocalBasePath() && lbpoffsetU != 0) {
            data.seek(pos + lbpoffsetU - data.position)
            localBasePath = data.readUnicodeStringNullTerm(pos + size - data.position shr 1)
        }
        if (lif.hasCommonNetworkRelativeLinkAndPathSuffix() && cpfoffsetU != 0) {
            data.seek(pos + cpfoffsetU - data.position)
            commonPathSuffix = data.readUnicodeStringNullTerm(pos + size - data.position shr 1)
        }
        data.seek(pos + size - data.position)
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        val pos = bw.position
        var hsize = 28
        val ce = StandardCharsets.US_ASCII.newEncoder()
        if (localBasePath != null && !ce.canEncode(localBasePath) || commonPathSuffix != null && !ce.canEncode(
                commonPathSuffix
            )
        ) hsize += 8
        var vidB: ByteArray? = null
        var localbasepathB: ByteArray? = null
        var cnrlinkB: ByteArray? = null
        var commonpathsuffixB: ByteArray? = null
        if (lif.hasVolumeIDAndLocalBasePath()) {
            vidB = toByteArray(volumeID)
            localbasepathB = localBasePath!!.toByteArray()
            commonpathsuffixB = ByteArray(0)
        }
        if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
            cnrlinkB = toByteArray(commonNetworkRelativeLink)
            commonpathsuffixB = commonPathSuffix!!.toByteArray()
        }
        var size = (hsize
                + (vidB?.size ?: 0)
                + (if (localbasepathB == null) 0 else localbasepathB.size + 1)
                + (cnrlinkB?.size ?: 0)
                + commonpathsuffixB!!.size + 1)
        if (hsize > 28) {
            if (lif.hasVolumeIDAndLocalBasePath()) {
                size += localBasePath!!.length * 2 + 2
                size += 1
            }
            if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) size += commonPathSuffix!!.length * 2
            size += 2
        }
        bw.write4bytes(size.toLong())
        bw.write4bytes(hsize.toLong())
        lif.serialize(bw)
        var off = hsize
        if (lif.hasVolumeIDAndLocalBasePath()) {
            bw.write4bytes(off.toLong()) // volumeid offset
            off += vidB!!.size
            bw.write4bytes(off.toLong()) // localBasePath offset
            off += localbasepathB!!.size + 1
        } else {
            bw.write4bytes(0) // volumeid offset
            bw.write4bytes(0) // localBasePath offset
        }
        if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
            bw.write4bytes(off.toLong()) // CommonNetworkRelativeLink offset 
            off += cnrlinkB!!.size
            bw.write4bytes(off.toLong()) // commonPathSuffix
            off += commonpathsuffixB.size + 1
        } else {
            bw.write4bytes(0) // CommonNetworkRelativeLinkOffset
            bw.write4bytes((size - if (hsize > 28) 4 else 1).toLong()) // fake commonPathSuffix offset 
        }
        if (hsize > 28) {
            if (lif.hasVolumeIDAndLocalBasePath()) {
                bw.write4bytes(off.toLong()) // LocalBasePathOffsetUnicode
                off += localBasePath!!.length * 2 + 2
                bw.write4bytes((size - 2).toLong()) // fake CommonPathSuffixUnicode offset
            } else {
                bw.write4bytes(0)
                bw.write4bytes(off.toLong()) // CommonPathSuffixUnicode offset 
                off += commonPathSuffix!!.length * 2 + 2
            }
        }
        if (lif.hasVolumeIDAndLocalBasePath()) {
            if (vidB != null) {
                bw.write(vidB)
            }
            if (localbasepathB != null) {
                bw.write(localbasepathB)
            }
            bw.write(0)
        }
        if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
            if (cnrlinkB != null) {
                bw.write(cnrlinkB)
            }
            bw.write(commonpathsuffixB)
            bw.write(0)
        }
        if (hsize > 28) {
            if (lif.hasVolumeIDAndLocalBasePath()) {
                bw.writeUnicodeStringNullTerm(localBasePath)
            }
            if (lif.hasCommonNetworkRelativeLinkAndPathSuffix()) {
                bw.writeUnicodeStringNullTerm(commonPathSuffix)
            }
        }
        while (bw.position < pos + size) bw.write(0)
    }

    @Throws(IOException::class)
    private fun toByteArray(o: Serializable?): ByteArray {
        val arr = ByteArrayOutputStream()
        val bt = ByteWriter(arr)
        o!!.serialize(bt)
        return arr.toByteArray()
    }

    /**
     * Creates VolumeID and LocalBasePath that is empty string
     */
    fun createVolumeID(): VolumeID {
        volumeID = VolumeID()
        localBasePath = ""
        lif.setVolumeIDAndLocalBasePath()
        return volumeID!!
    }

    /**
     * Set LocalBasePath and creates new VolumeID (if it not exists)
     * If s is null takes no effect
     */
    fun setLocalBasePath(s: String?): LinkInfo {
        if (s == null) return this
        localBasePath = s
        if (volumeID == null) volumeID = VolumeID()
        lif.setVolumeIDAndLocalBasePath()
        return this
    }

    /**
     * Creates CommonNetworkRelativeLink and CommonPathSuffix that is empty string
     */
    fun createCommonNetworkRelativeLink(): CNRLink {
        commonNetworkRelativeLink = CNRLink()
        commonPathSuffix = ""
        lif.setCommonNetworkRelativeLinkAndPathSuffix()
        return commonNetworkRelativeLink!!
    }

    /**
     * Set CommonPathSuffix and creates new CommonNetworkRelativeLink (if it not exists)
     * If s is null takes no effect
     */
    fun setCommonPathSuffix(s: String?): LinkInfo {
        if (s == null) return this
        commonPathSuffix = s
        if (commonNetworkRelativeLink == null) commonNetworkRelativeLink = CNRLink()
        lif.setCommonNetworkRelativeLinkAndPathSuffix()
        return this
    }

    fun buildPath(): String? {
        if (localBasePath != null) {
            var path: String = localBasePath!!
            if (commonPathSuffix != null && commonPathSuffix != "") {
                if (path[path.length - 1] != File.separatorChar) path += File.separatorChar
                path += commonPathSuffix
            }
            return path
        }
        return if (commonNetworkRelativeLink != null && commonPathSuffix != null) commonNetworkRelativeLink!!.netName + "\\" + commonPathSuffix else null
    }
}
