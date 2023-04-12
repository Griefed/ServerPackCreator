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
import mslinks.extra.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ShellLink {
    var header: ShellLinkHeader? = null
        private set
    var targetIdList: LinkTargetIDList? = null
        private set
    var linkInfo: LinkInfo? = null
        private set
    var name: String? = null
        private set
    var relativePath: String? = null
        private set
    var workingDir: String? = null
        private set
    var cMDArgs: String? = null
        private set
    var iconLocation: String? = null
        private set
    private val extra = HashMap<Int, Serializable?>()

    /**
     * linkFileSource is the location where the lnk file is stored
     * used only to build relative path and is not serialized
     */
    var linkFileSource: Path? = null
        private set

    constructor() {
        header = ShellLinkHeader()
        header!!.linkFlags.setIsUnicode()
    }

    constructor(file: String) : this(Paths.get(file))
    constructor(file: File) : this(file.toPath())
    constructor(file: Path) : this(Files.newInputStream(file)) {
        linkFileSource = file.toAbsolutePath()
    }

    constructor(`in`: InputStream) {
        ByteReader(`in`).use { reader -> parse(reader) }
    }

    constructor(reader: ByteReader) {
        reader.use { parse(reader) }
    }

    @Throws(ShellLinkException::class, IOException::class)
    private fun parse(data: ByteReader) {
        header = ShellLinkHeader(data)
        val lf = header!!.linkFlags
        if (lf.hasLinkTargetIDList()) {
            targetIdList = LinkTargetIDList(data)
        }
        if (lf.hasLinkInfo()) {
            linkInfo = LinkInfo(data)
        }
        if (lf.hasName()) {
            name = data.readUnicodeStringSizePadded()
        }
        if (lf.hasRelativePath()) {
            relativePath = data.readUnicodeStringSizePadded()
        }
        if (lf.hasWorkingDir()) {
            workingDir = data.readUnicodeStringSizePadded()
        }
        if (lf.hasArguments()) {
            cMDArgs = data.readUnicodeStringSizePadded()
        }
        if (lf.hasIconLocation()) {
            iconLocation = data.readUnicodeStringSizePadded()
        }
        while (true) {
            val size = data.read4bytes().toInt()
            if (size < 4) {
                break
            }
            val sign = data.read4bytes().toInt()
            try {
                val cl: Class<*>? = extraTypes[sign]
                if (cl != null) {
                    val constructor = cl.getConstructor(ByteReader::class.java, Int::class.javaPrimitiveType)
                    val instance = constructor.newInstance(data, size)
                    extra[sign] = instance as Serializable
                } else {
                    extra[sign] = Stub(data, size, sign)
                }
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    fun serialize(out: OutputStream) {
        val bw = ByteWriter(out)
        serialize(bw)
        out.close()
    }

    @Throws(IOException::class)
    fun serialize(bw: ByteWriter) {
        val lf = header!!.linkFlags
        header!!.serialize(bw)
        if (lf.hasLinkTargetIDList()) {
            targetIdList!!.serialize(bw)
        }
        if (lf.hasLinkInfo()) {
            linkInfo!!.serialize(bw)
        }
        if (lf.hasName()) {
            bw.writeUnicodeStringSizePadded(name)
        }
        if (lf.hasRelativePath()) {
            bw.writeUnicodeStringSizePadded(relativePath)
        }
        if (lf.hasWorkingDir()) {
            bw.writeUnicodeStringSizePadded(workingDir)
        }
        if (lf.hasArguments()) {
            bw.writeUnicodeStringSizePadded(cMDArgs)
        }
        if (lf.hasIconLocation()) {
            bw.writeUnicodeStringSizePadded(iconLocation)
        }
        for (i in extra.values) {
            i!!.serialize(bw)
        }
        bw.write4bytes(0)
    }

    fun createLinkInfo(): LinkInfo {
        linkInfo = LinkInfo()
        header!!.linkFlags.setHasLinkInfo()
        return linkInfo!!
    }

    fun removeLinkInfo(): ShellLink {
        linkInfo = null
        header!!.linkFlags.clearHasLinkInfo()
        return this
    }

    fun createTargetIdList(): LinkTargetIDList {
        if (targetIdList == null) {
            targetIdList = LinkTargetIDList()
            header!!.linkFlags.setHasLinkTargetIDList()
        }
        return targetIdList!!
    }

    fun removeTargetIdList(): ShellLink {
        targetIdList = null
        header!!.linkFlags.clearHasLinkTargetIDList()
        return this
    }

    fun setName(s: String?): ShellLink {
        if (s == null) {
            header!!.linkFlags.clearHasName()
        } else {
            header!!.linkFlags.setHasName()
        }
        name = s
        return this
    }

    fun setRelativePath(s: String?): ShellLink {
        var temp = s
        if (temp == null) {
            header!!.linkFlags.clearHasRelativePath()
        } else {
            header!!.linkFlags.setHasRelativePath()
            if (!temp.startsWith(".")) {
                temp = ".\\$temp"
            }
        }
        relativePath = temp
        return this
    }

    fun setWorkingDir(s: String?): ShellLink {
        if (s == null) {
            header!!.linkFlags.clearHasWorkingDir()
        } else {
            header!!.linkFlags.setHasWorkingDir()
        }
        workingDir = s
        return this
    }

    fun setCMDArgs(s: String?): ShellLink {
        if (s == null) {
            header!!.linkFlags.clearHasArguments()
        } else {
            header!!.linkFlags.setHasArguments()
        }
        cMDArgs = s
        return this
    }

    fun setIconLocation(s: String?): ShellLink {
        if (s == null) {
            header!!.linkFlags.clearHasIconLocation()
        } else {
            header!!.linkFlags.setHasIconLocation()
        }
        iconLocation = s
        return this
    }

    val language: String?
        get() = consoleFEData!!.language

    fun setLanguage(s: String?): ShellLink {
        consoleFEData!!.setLanguage(s)
        return this
    }

    val consoleData: ConsoleData?
        get() = getExtraDataBlock(ConsoleData.signature) as ConsoleData?

    fun removeConsoleData(): ShellLink {
        extra.remove(ConsoleData.signature)
        return this
    }

    val consoleFEData: ConsoleFEData?
        get() = getExtraDataBlock(ConsoleFEData.signature) as ConsoleFEData?

    fun removeConsoleFEData(): ShellLink {
        extra.remove(ConsoleFEData.signature)
        return this
    }

    val environmentVariable: EnvironmentVariable?
        get() = getExtraDataBlock(EnvironmentVariable.signature) as EnvironmentVariable?

    fun removeEnvironmentVariable(): ShellLink {
        extra.remove(EnvironmentVariable.signature)
        return this
    }

    val tracker: Tracker?
        get() = getExtraDataBlock(Tracker.signature) as Tracker?

    fun removeTracker(): ShellLink {
        extra.remove(Tracker.signature)
        return this
    }

    val vistaIDList: VistaIDList?
        get() = getExtraDataBlock(VistaIDList.signature) as VistaIDList?

    fun removeVistaIDList(): ShellLink {
        extra.remove(VistaIDList.signature)
        return this
    }

    fun setLinkFileSource(path: Path?): ShellLink {
        linkFileSource = path
        return this
    }

    fun resolveTarget(): String? {
        if (header!!.linkFlags.hasLinkTargetIDList()
            && targetIdList != null
            && targetIdList!!.canBuildAbsolutePath()
        ) {
            return targetIdList!!.buildPath()
        }
        if (header!!.linkFlags.hasLinkInfo() && linkInfo != null) {
            val path = linkInfo!!.buildPath()
            if (path != null) {
                return path
            }
        }
        if (linkFileSource != null
            && header!!.linkFlags.hasRelativePath()
            && relativePath != null
        ) {
            return linkFileSource!!.resolveSibling(relativePath!!).normalize().toString()
        }
        val envBlock = extra[EnvironmentVariable.signature] as EnvironmentVariable?
        if (envBlock != null && envBlock.variable!!.isNotBlank()) {
            return envBlock.variable
        }
        return if (header!!.linkFlags.hasLinkTargetIDList()
            && targetIdList != null
            && targetIdList!!.canBuildPath()
        ) {
            targetIdList!!.buildPath()
        } else {
            "<unknown>"
        }
    }

    private fun getExtraDataBlock(signature: Int): Serializable? {
        var block = extra[signature]
        if (block == null) {
            val type: Class<*> = extraTypes[signature]!!
            try {
                block = type.getConstructor().newInstance() as Serializable
                extra[signature] = block
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        return block
    }

    /**
     * @Deprecated Use new ShellLinkHelper API: [ShellLinkHelper.saveTo]
     */
    @Deprecated("")
    @Throws(IOException::class)
    fun saveTo(path: String?): ShellLink {
        ShellLinkHelper(this).saveTo(path)
        return this
    }

    /**
     * Set path to target file or directory. Function accepts local paths and network paths.
     * Environment variables are accepted but resolved here and aren't kept in the link.
     * @Deprecated Use new ShellLinkHelper API: [ShellLinkHelper.setNetworkTarget] or [ShellLinkHelper.setLocalTarget]
     */
    @Deprecated("")
    fun setTarget(target: String): ShellLink {
        var tempTarget = target
        tempTarget = ShellLinkHelper.resolveEnvVariables(tempTarget)
        val targetAbsPath = Paths.get(tempTarget).toAbsolutePath().toString()
        try {
            val helper = ShellLinkHelper(this)
            if (targetAbsPath.startsWith("\\\\")) {
                helper.setNetworkTarget(targetAbsPath)
            } else {
                val parts = targetAbsPath.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size == 2) {
                    helper.setLocalTarget(parts[0], parts[1])
                }
            }
        } catch (_: ShellLinkException) {
        }
        return this
    }

    companion object {
        const val VERSION = "1.1.0"
        private val extraTypes = HashMap<Int, Class<out Serializable>>(
            hashMapOf(
                ConsoleData.signature to ConsoleData::class.java,
                ConsoleFEData.signature to ConsoleFEData::class.java,
                Tracker.signature to Tracker::class.java,
                VistaIDList.signature to VistaIDList::class.java,
                EnvironmentVariable.signature to EnvironmentVariable::class.java
            )
        )

        /**
         * @Deprecated Use new ShellLinkHelper API: [ShellLinkHelper.setNetworkTarget] or [ShellLinkHelper.setLocalTarget]
         */
        @Deprecated("")
        fun createLink(target: String): ShellLink {
            val sl = ShellLink()
            sl.setTarget(target)
            return sl
        }

        /**
         * @Deprecated Use new ShellLinkHelper API: [ShellLinkHelper.createLink]
         */
        @Deprecated("", ReplaceWith("createLink(target).saveTo(linkpath)", "mslinks.ShellLink.Companion.createLink"))
        @Throws(IOException::class)
        fun createLink(target: String, linkpath: String?): ShellLink {
            return createLink(target).saveTo(linkpath)
        }
    }
}
