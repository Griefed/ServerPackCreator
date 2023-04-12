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

import mslinks.data.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * Helper class to manipulate ShellLink properties in batches for common tasks
 * ShellLink can be used directly without helper for more detailed set up
 */
@Suppress("unused")
class ShellLinkHelper(var link: ShellLink) {
    enum class Options {
        None,
        ForceTypeDirectory,
        ForceTypeFile
    }

    /**
     * Sets LAN target path
     * @param path is an absolute in the form '\\host\share\path\to\target'
     * @throws ShellLinkException
     */
    @Throws(ShellLinkException::class)
    fun setNetworkTarget(path: String): ShellLinkHelper {
        return setNetworkTarget(path, Options.None)
    }

    /**
     * Sets LAN target path
     * @param path is an absolute in the form '\\host\share\path\to\target'
     * @throws ShellLinkException
     */
    @Throws(ShellLinkException::class)
    fun setNetworkTarget(path: String, options: Options): ShellLinkHelper {
        var tempPath = path
        if (!tempPath.startsWith("\\")) tempPath = "\\" + tempPath
        if (!tempPath.startsWith("\\\\")) tempPath = "\\" + tempPath
        val p1 = tempPath.indexOf('\\', 2) // hostname
        val p2 = tempPath.indexOf('\\', p1 + 1) // share name
        if (p1 != -1) {
            val info = if (link.header!!.linkFlags.hasLinkInfo()) link.linkInfo else link.createLinkInfo()
            if (p2 != -1) {
                info!!.createCommonNetworkRelativeLink().setNetName(tempPath.substring(0, p2))
                info.setCommonPathSuffix(tempPath.substring(p2 + 1))
            } else {
                info!!.createCommonNetworkRelativeLink().setNetName(tempPath)
                info.setCommonPathSuffix("")
            }
            link.header!!.fileAttributesFlags.setDirectory()
            val forceFile = options == Options.ForceTypeFile
            val forceDirectory = options == Options.ForceTypeDirectory
            if (forceFile || !forceDirectory && Files.isRegularFile(Paths.get(tempPath))) {
                link.header!!.fileAttributesFlags.clearDirectory()
            }
        } else {
            link.header!!.fileAttributesFlags.clearDirectory()
        }
        link.header!!.linkFlags.setHasExpString()
        link.environmentVariable!!.setVariable(tempPath)
        return this
    }

    /**
     * Sets target on local computer, e.g. "C:\path\to\target"
     * @param drive is a letter part of the path, e.g. "C" or "D"
     * @param absolutePath is a path in the specified drive, e.g. "path\to\target"
     * @throws ShellLinkException
     */
    @Throws(ShellLinkException::class)
    fun setLocalTarget(drive: String?, absolutePath: String): ShellLinkHelper {
        return setLocalTarget(drive, absolutePath, Options.None)
    }

    /**
     * Sets target on local computer, e.g. "C:\path\to\target"
     * @param drive is a letter part of the path, e.g. "C" or "D"
     * @param absolutePath is a path in the specified drive, e.g. "path\to\target"
     * @throws ShellLinkException
     */
    @Throws(ShellLinkException::class)
    fun setLocalTarget(drive: String?, absolutePath: String, options: Options): ShellLinkHelper {
        var tempAbsolutePath = absolutePath
        link.header!!.linkFlags.setHasLinkTargetIDList()
        val idList = link.createTargetIdList()
        // root is computer
        idList.add(ItemIDRoot().setClsid(Registry.CLSID_COMPUTER))

        // drive
        // windows usually creates TYPE_DRIVE_MISC here but TYPE_DRIVE_FIXED also works fine
        val driveItem = ItemIDDrive(ItemID.TYPE_DRIVE_MISC).setName(drive)
        idList.add(driveItem)

        // each segment of the path is directory
        tempAbsolutePath = tempAbsolutePath.replace("^(\\\\|\\/)".toRegex(), "")
        val absoluteTargetPath = driveItem.name + tempAbsolutePath
        val path = tempAbsolutePath.split("\\\\|\\/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in path) idList.add(ItemIDFS(ItemID.TYPE_FS_DIRECTORY).setName(i))
        val info = if (link.header!!.linkFlags.hasLinkInfo()) link.linkInfo else link.createLinkInfo()
        info!!.createVolumeID().setDriveType(VolumeID.DRIVE_FIXED)
        info.setLocalBasePath(absoluteTargetPath)
        link.header!!.fileAttributesFlags.setDirectory()
        val forceFile = options == Options.ForceTypeFile
        val forceDirectory = options == Options.ForceTypeDirectory
        if (forceFile || !forceDirectory && Files.isRegularFile(Paths.get(absoluteTargetPath))) {
            link.header!!.fileAttributesFlags.clearDirectory()
            idList.last!!.setTypeFlags(ItemID.TYPE_FS_FILE)
        }
        return this
    }

    /**
     * Sets target relative to a special folder defined by a GUID.
     * Use [Registry] class to get an available GUID by name or predefined constants.
     * Note that you can add your own GUIDs available on your system
     * @param root a GUID defining a special folder, e.g. Registry.CLSID_DOCUMENTS. Must be registered in the [Registry]
     * @param path a path relative to the special folder, e.g. "path\to\target"
     * @throws ShellLinkException
     */
    @Throws(ShellLinkException::class)
    fun setSpecialFolderTarget(root: GUID?, path: String, options: Options): ShellLinkHelper {
        var tempPath = path
        if (options != Options.ForceTypeFile && options != Options.ForceTypeDirectory) {
            throw ShellLinkException("The type of target is not specified. You have to specify whether it is a file or a directory.")
        }
        link.header!!.linkFlags.setHasLinkTargetIDList()
        val idList = link.createTargetIdList()
        // although later systems use ItemIDRoot(computer) + ItemIDRegFolder(root clsid) pair, always set root clsid as ItemIDRoot for simplicity
        idList.add(ItemIDRoot().setClsid(root))

        // each segment of the path is directory
        tempPath = tempPath.replace("^(\\\\|\\/)".toRegex(), "")
        val pathSegments = tempPath.split("\\\\|\\/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in pathSegments) {
            idList.add(ItemIDFS(ItemID.TYPE_FS_DIRECTORY).setName(i))
        }
        link.header!!.fileAttributesFlags.setDirectory()
        if (options == Options.ForceTypeFile) {
            link.header!!.fileAttributesFlags.clearDirectory()
            idList.last!!.setTypeFlags(ItemID.TYPE_FS_FILE)
        }
        return this
    }

    /**
     * Sets target relative to desktop directory of the user opening the link. This method is universal
     * because it works without Registry.CLSID_DESKTOP which is available only on later systems
     * @param path a path relative to the desktop, e.g. "path\to\target"
     * @throws ShellLinkException
     */
    @Throws(ShellLinkException::class)
    fun setDesktopRelativeTarget(path: String, options: Options): ShellLinkHelper {
        var tempPath = path
        if (options != Options.ForceTypeFile && options != Options.ForceTypeDirectory) {
            throw ShellLinkException("The type of target is not specified. You have to specify whether it is a file or a directory.")
        }
        link.header!!.linkFlags.setHasLinkTargetIDList()
        val idList = link.createTargetIdList()

        // no root item here

        // each segment of the path is directory
        tempPath = tempPath.replace("^(\\\\|\\/)".toRegex(), "")
        val pathSegments = tempPath.split("\\\\|\\/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in pathSegments) {
            idList.add(ItemIDFS(ItemID.TYPE_FS_DIRECTORY).setName(i))
        }
        link.header!!.fileAttributesFlags.setDirectory()
        if (options == Options.ForceTypeFile) {
            link.header!!.fileAttributesFlags.clearDirectory()
            idList.last!!.setTypeFlags(ItemID.TYPE_FS_FILE)
        }
        return this
    }

    /**
     * Serializes `ShellLink` to specified `path`. Sets appropriate relative path
     * and working directory if possible and if they are not already set
     */
    @Throws(IOException::class)
    fun saveTo(path: String?): ShellLinkHelper {
        val savingPath = Paths.get(path!!).toAbsolutePath().normalize()
        if (Files.isDirectory(savingPath)) {
            throw IOException("can't save ShellLink to \"$savingPath\" because there is a directory with this name")
        }
        link.setLinkFileSource(savingPath)
        val savingDir = savingPath.parent
        try {
            val target = Paths.get(link.resolveTarget()!!)
            if (!link.header!!.linkFlags.hasRelativePath()) {
                // this will always be false on linux
                if (savingDir.root == target.root) {
                    link.setRelativePath(savingDir.relativize(target).toString())
                }
            }
            if (!link.header!!.linkFlags.hasWorkingDir()) {
                // this will always be false on linux
                if (Files.isRegularFile(target)) {
                    link.setWorkingDir(target.parent.toString())
                }
            }
        } catch (e: InvalidPathException) {
            // skip automatic relative path and working dir if path is some special folder
        }
        Files.createDirectories(savingDir)
        link.serialize(Files.newOutputStream(savingPath))
        return this
    }

    companion object {
        /**
         * Universal all-by-default creation of the link
         * @param target - absolute path for the target file in windows format (e.g. C:\path\to\file.txt)
         * @param linkPath - where to save link file
         * @return
         * @throws IOException
         * @throws ShellLinkException
         */
        @Throws(IOException::class, ShellLinkException::class)
        fun createLink(target: String, linkPath: String?): ShellLinkHelper {
            var tempTarget = target
            tempTarget = resolveEnvVariables(tempTarget)
            val helper = ShellLinkHelper(ShellLink())
            if (tempTarget.startsWith("\\\\")) {
                helper.setNetworkTarget(tempTarget)
            } else {
                val parts = tempTarget.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size != 2) throw ShellLinkException("Wrong path '$tempTarget'")
                helper.setLocalTarget(parts[0], parts[1])
            }
            helper.saveTo(linkPath)
            return helper
        }

        fun resolveEnvVariables(path: String): String {
            var tempPath = path
            for ((key, value) in env) {
                val p = Pattern.quote(key)
                val r = value.replace("\\", "\\\\")
                tempPath = Pattern.compile("%$p%", Pattern.CASE_INSENSITIVE).matcher(tempPath).replaceAll(r)
            }
            return tempPath
        }

        private val env = System.getenv()
    }
}
