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
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

@Suppress("unused")
abstract class ItemID : Serializable {
    protected var typeFlags = 0
        get() {
            return if (internalItemId != null) {
                internalItemId!!.typeFlags
            } else {
                field
            }
        }

    constructor()

    constructor(d: ByteArray) {
        val br = ByteReader(ByteArrayInputStream(d))
        val flags = br.read()
        internalItemId = createItem(flags)
        internalItemId!!.load(br, d.size - 1)
    }

    constructor(br: ByteReader, maxSize: Int) {
        val flags = br.read()
        internalItemId = createItem(flags)
        internalItemId!!.load(br, maxSize - 1)
    }

    constructor(flags: Int) {
        typeFlags = flags

        // for deprecated API: should not create instances of this class directly
        if (this.javaClass == ItemID::class.java) {
            internalItemId = try {
                createItem(flags)
            } catch (e: ShellLinkException) {
                ItemIDUnknown(flags)
            }
        }
    }

    @Throws(IOException::class, ShellLinkException::class)
    open fun load(br: ByteReader, maxSize: Int) {
        // DO NOT read type flags here as they have already been read
        // in order to determine the type of this item id
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        if (internalItemId != null) {
            internalItemId!!.serialize(bw)
        } else {
            bw.write(typeFlags)
        }
    }

    override fun toString(): String {
        return ""
    }

    @Throws(ShellLinkException::class)
    open fun setTypeFlags(flags: Int): ItemID {
        if (flags and ID_TYPE_GROUPMASK != 0) {
            throw ShellLinkException("ItemID group cannot be changed. Create a new instance of an appropriate type instead.")
        }
        if (flags and ID_TYPE_JUNCTION != 0) {
            throw ShellLinkException("Junctions are not supported")
        }
        typeFlags = typeFlags and ID_TYPE_GROUPMASK or (flags and ID_TYPE_INGROUPMASK)
        return this
    }

    @Deprecated("")
    private var internalItemId: ItemID? = null

    /**
     * @Deprecated Use [ItemIDDrive] or [ItemIDFS]
     */
    @get:Deprecated("")
    open val name: String?
        get() {
            if (internalItemId is ItemIDDrive) {
                return (internalItemId as ItemIDDrive).name
            } else if (internalItemId is ItemIDFS) {
                return (internalItemId as ItemIDFS).name
            }
            return ""
        }

    /**
     * @Deprecated Use [ItemIDDrive] or [ItemIDFS]
     */
    @Deprecated("Use [ItemIDDrive] or [ItemIDFS]")
    @Throws(ShellLinkException::class)
    open fun setName(s: String?): ItemID {
        if (internalItemId is ItemIDDrive) {
            (internalItemId as ItemIDDrive).setName(s)
        } else if (internalItemId is ItemIDFS) {
            (internalItemId as ItemIDFS).setName(s)
        }
        return this
    }

    /**
     * @Deprecated Use [ItemIDFS]
     */
    @get:Deprecated("")
    open val size: Int
        get() = if (internalItemId is ItemIDFS) {
            (internalItemId as ItemIDFS).size
        } else {
            0
        }

    /**
     * @Deprecated Use [ItemIDFS]
     */
    @Deprecated("")
    @Throws(ShellLinkException::class)
    open fun setSize(s: Int): ItemID {
        if (internalItemId is ItemIDFS) {
            (internalItemId as ItemIDFS).setSize(s)
            return this
        }
        throw ShellLinkException("only files has size")
    }

    @get:Deprecated("", ReplaceWith("typeFlags"))
    val type: Int
        get() = typeFlags

    /**
     * @Deprecated Use [.setTypeFlags]. However, in new API you should create instances
     * of an appropriate class extending this one and use `setTypeFlags(int flags)` only to set
     * type-specific flags corresponding to the [.ID_TYPE_INGROUPMASK]
     */
    @Deprecated("")
    @Throws(ShellLinkException::class)
    fun setType(t: Int): ItemID {
        if (t == TYPE_CLSID) {
            internalItemId = ItemIDRoot().setClsid(Registry.CLSID_COMPUTER)
            return this
        }
        if (t == TYPE_FILE || t == TYPE_DIRECTORY || t == TYPE_FILE_OLD || t == TYPE_DIRECTORY_OLD) {
            when (internalItemId) {
                is ItemIDFS -> {
                    (internalItemId as ItemIDFS).setTypeFlags(t and ID_TYPE_INGROUPMASK)
                }

                is ItemIDDrive -> {
                    val driveId = internalItemId as ItemIDDrive
                    internalItemId = ItemIDFS(t).setName(driveId.name)
                }

                null -> {
                    internalItemId = ItemIDFS(t)
                }
            }
            return this
        }
        if (t == TYPE_DRIVE || t == TYPE_DRIVE_OLD) {
            when (internalItemId) {
                is ItemIDDrive -> {
                    (internalItemId as ItemIDDrive).setTypeFlags(t and ID_TYPE_INGROUPMASK)
                }

                is ItemIDFS -> {
                    val fsId = internalItemId as ItemIDFS
                    internalItemId = ItemIDDrive(t).setName(fsId.name)
                }

                null -> {
                    internalItemId = ItemIDDrive(t)
                }
            }
            return this
        }
        throw ShellLinkException("wrong type")
    }

    companion object {
        // from NT\shell\shell32\shitemid.h
        const val ID_TYPE_JUNCTION = 0x80
        const val ID_TYPE_GROUPMASK = 0x70
        const val ID_TYPE_INGROUPMASK = 0x0f
        const val GROUP_ROOT = 0x10
        const val GROUP_COMPUTER = 0x20
        const val GROUP_FS = 0x30
        const val GROUP_NET = 0x40
        const val GROUP_LOC = 0x50
        const val GROUP_CONTROLPANEL = 0x70

        // GROUP_ROOT
        const val TYPE_ROOT_REGITEM = 0x0f

        // GROUP_COMPUTER
        const val TYPE_DRIVE_RESERVED_1 = 0x1
        const val TYPE_DRIVE_REMOVABLE = 0x2
        const val TYPE_DRIVE_FIXED = 0x3
        const val TYPE_DRIVE_REMOTE = 0x4
        const val TYPE_DRIVE_CDROM = 0x5
        const val TYPE_DRIVE_RAMDISK = 0x6
        const val TYPE_DRIVE_RESERVED_7 = 0x7
        const val TYPE_DRIVE_DRIVE525 = 0x8
        const val TYPE_DRIVE_DRIVE35 = 0x9
        const val TYPE_DRIVE_NETDRIVE = 0xa // Network drive
        const val TYPE_DRIVE_NETUNAVAIL = 0xb // Network drive that is not restored.
        const val TYPE_DRIVE_RESERVED_C = 0xc
        const val TYPE_DRIVE_RESERVED_D = 0xd
        const val TYPE_DRIVE_REGITEM = 0xe // Controls, Printers, ... Do not confuse with TYPE_ROOT_REGITEM
        const val TYPE_DRIVE_MISC = 0xf

        // GROUP_FS - these values can be combined
        const val TYPE_FS_DIRECTORY = 0x1
        const val TYPE_FS_FILE = 0x2
        const val TYPE_FS_UNICODE = 0x4
        const val TYPE_FS_COMMON = 0x8

        // GROUP_NET
        const val TYPE_NET_DOMAIN = 0x1
        const val TYPE_NET_SERVER = 0x2
        const val TYPE_NET_SHARE = 0x3
        const val TYPE_NET_FILE = 0x4
        const val TYPE_NET_GROUP = 0x5
        const val TYPE_NET_NETWORK = 0x6
        const val TYPE_NET_RESTOFNET = 0x7
        const val TYPE_NET_SHAREADMIN = 0x8
        const val TYPE_NET_DIRECTORY = 0x9
        const val TYPE_NET_TREE = 0xa
        const val TYPE_NET_NDSCONTAINER = 0xb
        const val TYPE_NET_REGITEM = 0xd
        const val TYPE_NET_REMOTEREGITEM = 0xe
        const val TYPE_NET_PRINTER = 0xf

        // GROUP_LOC - ???
        // GROUP_CONTROLPANEL
        const val TYPE_CONTROL_REGITEM = 0x0
        const val TYPE_CONTROL_REGITEM_EX = 0x1

        @Throws(ShellLinkException::class)
        fun createItem(typeFlags: Int): ItemID {
            if (typeFlags and ID_TYPE_JUNCTION != 0) {
                throw ShellLinkException("junctions are not supported")
            }
            val group = typeFlags and ID_TYPE_GROUPMASK
            val subGroup = typeFlags and ID_TYPE_INGROUPMASK
            return when (group) {
                GROUP_ROOT -> ItemIDRoot(typeFlags)
                GROUP_COMPUTER -> {
                    if (subGroup == TYPE_DRIVE_REGITEM) {
                        ItemIDRegFolder(typeFlags)
                    } else {
                        ItemIDDrive(typeFlags)
                    }
                }

                GROUP_FS -> ItemIDFS(typeFlags)
                else -> ItemIDUnknown(typeFlags)
            }
        }

        @JvmStatic
        protected fun isLongFilename(filename: String): Boolean {
            if (filename[0] == '.' || filename[filename.length - 1] == '.') {
                return true
            }
            if (!filename.matches("^\\p{ASCII}+$".toRegex())) {
                return true
            }

            // no matter whether it is file or directory
            val dotIdx = filename.lastIndexOf('.')
            val baseName = if (dotIdx == -1) filename else filename.substring(0, dotIdx)
            val ext = if (dotIdx == -1) "" else filename.substring(dotIdx + 1)
            val wrongSymbolsPattern = ".*[\\.\"\\/\\\\\\[\\]:;=, ]+.*"
            return baseName.length > 8 || ext.length > 3 || baseName.matches(wrongSymbolsPattern.toRegex()) || ext.matches(
                wrongSymbolsPattern.toRegex()
            )
        }

        @JvmStatic
        protected fun generateShortName(longname: String): String {
            // assume that it is actually long, don't check it again
            var longName = longname
            longName = longName.replace("\\.$|^\\.".toRegex(), "")
            val dotIdx = longName.lastIndexOf('.')
            var baseName = if (dotIdx == -1) {
                longName
            } else {
                longName.substring(0, dotIdx)
            }
            var ext = if (dotIdx == -1) {
                ""
            } else {
                longName.substring(dotIdx + 1)
            }
            ext = ext.replace(" ", "").replace("[\\.\"\\/\\\\\\[\\]:;=,\\+]".toRegex(), "_")
            ext = ext.substring(0, 3.coerceAtMost(ext.length))
            baseName = baseName.replace(" ", "").replace("[\\.\"\\/\\\\\\[\\]:;=,\\+]".toRegex(), "_")
            baseName = baseName.substring(0, 6.coerceAtMost(baseName.length))

            // well, for same short names we should use "~2", "~3" and so on,
            // but actual index is generated by os while creating a file and stored in filesystem,
            // so it is not possible to get actual one
            val append = if (ext.isEmpty()) {
                ""
            } else {
                ".$ext"
            }
            val shortname = StringBuilder("$baseName~1$append")

            // i have no idea how non-asci symbols are converted in dos names
            val asciiEncoder = StandardCharsets.US_ASCII.newEncoder()
            for (i in shortname.indices) {
                if (!asciiEncoder.canEncode(shortname[i])) {
                    shortname.setCharAt(i, '_')
                }
            }
            return shortname.toString().uppercase(Locale.getDefault())
        }

        //////////////////////////////////////////////////////
        ////////////// Deprecated old API ////////////////////
        //////////////////////////////////////////////////////
        @Deprecated("")
        val TYPE_UNKNOWN = 0

        @Deprecated("")
        val TYPE_FILE_OLD = GROUP_FS or TYPE_FS_UNICODE or TYPE_FS_FILE

        @Deprecated("")
        val TYPE_DIRECTORY_OLD = GROUP_FS or TYPE_FS_UNICODE or TYPE_FS_DIRECTORY

        @Deprecated("")
        val TYPE_FILE = GROUP_FS or TYPE_FS_FILE

        @Deprecated("")
        val TYPE_DIRECTORY = GROUP_FS or TYPE_FS_DIRECTORY

        @Deprecated("")
        val TYPE_DRIVE_OLD = GROUP_COMPUTER or TYPE_DRIVE_FIXED

        @Deprecated("")
        val TYPE_DRIVE = GROUP_COMPUTER or TYPE_DRIVE_MISC

        @Deprecated("")
        val TYPE_CLSID = GROUP_ROOT or TYPE_ROOT_REGITEM
    }
}
