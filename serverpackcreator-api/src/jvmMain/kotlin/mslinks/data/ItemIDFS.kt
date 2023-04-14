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
import java.io.ByteArrayInputStream
import java.io.IOException

@Suppress("unused")
open class ItemIDFS(flags: Int) : ItemID(flags or GROUP_FS) {
    override var size = 0
        protected set
    var attributes: Short = 0
        protected set

    @Suppress("MemberVisibilityCanBePrivate")
    protected var shortname: String? = null

    @Suppress("MemberVisibilityCanBePrivate")
    protected var longname: String? = null

    init {
        onTypeFlagsChanged()
    }

    @Throws(UnsupportedItemIDException::class)
    private fun onTypeFlagsChanged() {
        val subType = typeFlags and ID_TYPE_INGROUPMASK
        if (subType and TYPE_FS_DIRECTORY == 0 && subType and TYPE_FS_FILE == 0) {
            throw UnsupportedItemIDException(typeFlags)
        }

        // don't allow flipping unicode flag at will to avoid inconsistency 
        if (longname != null) {
            typeFlags = if (isLongFilename(longname!!)) {
                typeFlags or TYPE_FS_UNICODE
            } else {
                typeFlags and TYPE_FS_UNICODE.inv()
            }
        }

        // attribute directory flag should match the typeFlag directory flag
        attributes = if (subType and TYPE_FS_DIRECTORY != 0) {
            (attributes.toInt() or FILE_ATTRIBUTE_DIRECTORY).toShort()
        } else {
            (attributes.toInt() and FILE_ATTRIBUTE_DIRECTORY.inv()).toShort()
        }
    }

    @Throws(IOException::class, ShellLinkException::class)
    override fun load(br: ByteReader, maxSize: Int) {
        // 3 bytes are the size (2) and the type (1) initially parsed in LinkTargetIDList,
        // but they are considered part of the ItemID for calculating offsets
        val startPos = br.position - 3
        val endPos = startPos + maxSize + 3
        super.load(br, maxSize)
        br.read() // IDFOLDER struct doesn't have this byte, but it does exist in data. Probably it's just padding
        size = br.read4bytes().toInt()
        br.read2bytes() // date modified
        br.read2bytes() // time modified
        attributes = br.read2bytes().toShort()
        if (typeFlags and TYPE_FS_UNICODE != 0) {
            longname = br.readUnicodeStringNullTerm(endPos - br.position)
        }
        shortname = br.readString(endPos - br.position)
        val restOfDataSize = endPos - br.position
        if (restOfDataSize <= 2) {
            br.seek(restOfDataSize)
            return
        }

        // last 2 bytes are the offset to the hidden list
        val bytesParsed = br.position - startPos
        val dataChunk = ByteArray(restOfDataSize - 2)
        br.read(dataChunk, 0, dataChunk.size)
        val hiddenOffset = br.read2bytes().toInt()
        if (hiddenOffset == 0 || hiddenOffset < bytesParsed) {
            return
        }
        val offsetInDataChunk = hiddenOffset - bytesParsed
        val hbr = ByteReader(ByteArrayInputStream(dataChunk, offsetInDataChunk, dataChunk.size))
        loadHiddenPart(hbr, dataChunk.size + 2 - offsetInDataChunk)
    }

    @Throws(IOException::class)
    protected fun loadHiddenPart(br: ByteReader, maxSize: Int) {
        while (true) {
            val startPos = br.position
            val hiddenSize = br.read2bytes().toInt()
            val hiddenVersion = br.read2bytes().toInt()
            val hiddenIdField = br.read4bytes().toInt()
            val hiddenIdMagic = hiddenIdField and -0x10000 ushr 16
            val hiddenId = hiddenIdField and 0xFFFF
            val hiddenEndPos = br.position - 8 + hiddenSize
            if (hiddenEndPos > maxSize) {
                break
            }
            if (hiddenIdMagic != 0xBEEF) {
                br.seek(hiddenSize - 8)
                continue
            }
            if (hiddenId == HIDDEN_ID_IDFOLDEREX && hiddenVersion >= 3) { // IDFX_V1
                br.read4bytes() // date & time created
                br.read4bytes() // date & time accessed
                val offsetNameUnicode = br.read2bytes().toInt()
                br.read2bytes() // offResourceA
                val unicodeNamePos = startPos + offsetNameUnicode
                br.seek(unicodeNamePos - br.position)
                longname = br.readUnicodeStringNullTerm(startPos + hiddenSize - br.position)

                // we don't serialize hidden parts so add unicode flag
                if (longname != shortname) {
                    typeFlags = typeFlags or TYPE_FS_UNICODE
                }
                break
            }
        }
    }

    @Throws(IOException::class)
    override fun serialize(bw: ByteWriter) {
        super.serialize(bw)
        bw.write(0)
        bw.write4bytes(size.toLong())
        bw.write4bytes(0) // last modified
        bw.write2bytes(attributes.toLong())
        if (typeFlags and TYPE_FS_UNICODE != 0) {
            bw.writeUnicodeStringNullTerm(longname)
            bw.writeString(shortname)
        } else {
            bw.writeString(shortname)
            bw.write(0)
        }
    }

    override fun toString(): String {
        var name = if (typeFlags and TYPE_FS_UNICODE != 0) {
            longname
        } else {
            shortname
        }
        if (typeFlags and TYPE_FS_DIRECTORY != 0) {
            name += "\\"
        }
        return name!!
    }

    @Throws(ShellLinkException::class)
    override fun setTypeFlags(flags: Int): ItemID {
        super.setTypeFlags(flags)
        onTypeFlagsChanged()
        return this
    }

    @Deprecated("")
    override fun setSize(s: Int): ItemID {
        size = s
        return this
    }

    @Throws(ShellLinkException::class)
    fun setAttributes(attr: Short): ItemIDFS {
        attributes = attr
        if (attr.toInt() and FILE_ATTRIBUTE_DIRECTORY != 0) {
            typeFlags = typeFlags or TYPE_FS_DIRECTORY
            typeFlags = typeFlags and TYPE_FS_FILE.inv()
        } else {
            typeFlags = typeFlags and TYPE_FS_DIRECTORY.inv()
            typeFlags = typeFlags or TYPE_FS_FILE
        }
        return this
    }

    override val name: String?
        get() = if (longname != null && longname != "") {
            longname
        } else {
            shortname
        }

    @Deprecated("")
    @Throws(ShellLinkException::class)
    override fun setName(s: String?): ItemID {
        if (s == null) return this
        if (s.contains("\\")) throw ShellLinkException("wrong ItemIDFS name: $s")
        longname = s
        if (isLongFilename(s)) {
            shortname = generateShortName(s)
            typeFlags = typeFlags or TYPE_FS_UNICODE
        } else {
            shortname = s
            typeFlags = typeFlags and TYPE_FS_UNICODE.inv()
        }
        return this
    }

    companion object {
        // from NT\shell\inc\idhidden.h
        protected const val HIDDEN_ID_EMPTY = 0
        protected const val HIDDEN_ID_URLFRAGMENT = 1 //  Fragment IDs on URLs (#anchors)
        protected const val HIDDEN_ID_URLQUERY = 2 //  Query strings on URLs (?query+info)
        protected const val HIDDEN_ID_JUNCTION = 3 //  Junction point data
        protected const val HIDDEN_ID_IDFOLDEREX = 4 //  IDFOLDEREX, extended data for CFSFolder
        protected const val HIDDEN_ID_DOCFINDDATA = 5 //  DocFind's private attached data (not persisted)
        protected const val HIDDEN_ID_PERSONALIZED = 6 //  personalized like (My Docs/Zeke's Docs)
        protected const val HIDDEN_ID_recycle2 = 7 //  recycle
        protected const val HIDDEN_ID_RECYCLEBINDATA = 8 //  RecycleBin private data (not persisted)
        protected const val HIDDEN_ID_RECYCLEBINORIGINAL = 9 //  the original unthunked path for RecycleBin items
        protected const val HIDDEN_ID_PARENTFOLDER = 10 //  merged folder uses this to encode the source folder.
        protected const val HIDDEN_ID_STARTPANEDATA = 11 //  Start Pane's private attached data
        protected const val HIDDEN_ID_NAVIGATEMARKER = 12 //  Used by Control Panel's 'Category view'

        // from NT\public\sdk\inc\ntioapi.h
        const val FILE_ATTRIBUTE_READONLY = 0x00000001
        const val FILE_ATTRIBUTE_HIDDEN = 0x00000002
        const val FILE_ATTRIBUTE_SYSTEM = 0x00000004
        const val FILE_ATTRIBUTE_DIRECTORY = 0x00000010
        const val FILE_ATTRIBUTE_ARCHIVE = 0x00000020
        const val FILE_ATTRIBUTE_DEVICE = 0x00000040
        const val FILE_ATTRIBUTE_NORMAL = 0x00000080
        const val FILE_ATTRIBUTE_TEMPORARY = 0x00000100
        const val FILE_ATTRIBUTE_SPARSE_FILE = 0x00000200
        const val FILE_ATTRIBUTE_REPARSE_POINT = 0x00000400
        const val FILE_ATTRIBUTE_COMPRESSED = 0x00000800
        const val FILE_ATTRIBUTE_OFFLINE = 0x00001000
        const val FILE_ATTRIBUTE_NOT_CONTENT_INDEXED = 0x00002000
        const val FILE_ATTRIBUTE_ENCRYPTED = 0x00004000
    }
}
