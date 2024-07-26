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
import java.nio.charset.StandardCharsets

@Suppress("unused")
class VolumeID : Serializable {
    @Suppress("MemberVisibilityCanBePrivate")
    var driveType: Int
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    var serialNumber: Int
        private set
    var label: String? = null
        private set

    constructor() {
        driveType = DRIVE_UNKNOWN
        serialNumber = 0
        label = ""
    }

    constructor(data: ByteReader) {
        val pos = data.position
        val size = data.read4bytes().toInt()
        if (size <= 0x10) throw ShellLinkException()
        driveType = data.read4bytes().toInt()
        if (driveType != DRIVE_NO_ROOT_DIR && driveType != DRIVE_REMOVABLE && driveType != DRIVE_FIXED && driveType != DRIVE_REMOTE && driveType != DRIVE_CDROM && driveType != DRIVE_RAMDISK) driveType =
            DRIVE_UNKNOWN
        serialNumber = data.read4bytes().toInt()
        var vloffset = data.read4bytes().toInt()
        var u = false
        if (vloffset == 0x14) {
            vloffset = data.read4bytes().toInt()
            u = true
        }
        data.seek(pos + vloffset - data.position)
        var i = 0
        if (u) {
            val buf = CharArray(size - vloffset shr 1)
            while (true) {
                val c = Char(data.read2bytes().toUShort())
                if (c.code == 0) break
                buf[i] = c
                i++
            }
            label = String(buf, 0, i)
        } else {
            val buf = ByteArray(size - vloffset)
            while (true) {
                val b = data.read()
                if (b == 0) break
                buf[i] = b.toByte()
                i++
            }
            label = String(buf, 0, i)
        }
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        var size = 16
        val labelB = label!!.toByteArray()
        size += labelB.size + 1
        var u = false
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(label)) {
            size += 4 + 1 + label!!.length * 2 + 2
            u = true
        }
        bw.write4bytes(size.toLong())
        bw.write4bytes(driveType.toLong())
        bw.write4bytes(serialNumber.toLong())
        var off = 16
        if (u) off += 4
        bw.write4bytes(off.toLong())
        off += labelB.size + 1
        if (u) {
            off++
            bw.write4bytes(off.toLong())
            off += label!!.length * 2 + 2
        }
        bw.write(labelB)
        bw.write(0)
        if (u) {
            bw.write(0)
            for (i in 0 until label!!.length) bw.write2bytes(label!![i].code.toLong())
            bw.write2bytes(0)
        }
    }

    @Throws(ShellLinkException::class)
    fun setDriveType(n: Int): VolumeID {
        return when (n) {
            DRIVE_UNKNOWN, DRIVE_NO_ROOT_DIR, DRIVE_REMOVABLE, DRIVE_FIXED, DRIVE_REMOTE, DRIVE_CDROM, DRIVE_RAMDISK -> {
                driveType = n
                this
            }

            else -> throw ShellLinkException("incorrect drive type")
        }
    }

    fun setSerialNumber(n: Int): VolumeID {
        serialNumber = n
        return this
    }

    /**
     * if s is null take no effect
     */
    fun setLabel(s: String?): VolumeID {
        if (s != null) label = s
        return this
    }

    companion object {
        const val DRIVE_UNKNOWN = 0
        const val DRIVE_NO_ROOT_DIR = 1
        const val DRIVE_REMOVABLE = 2
        const val DRIVE_FIXED = 3
        const val DRIVE_REMOTE = 4
        const val DRIVE_CDROM = 5
        const val DRIVE_RAMDISK = 6
    }
}
