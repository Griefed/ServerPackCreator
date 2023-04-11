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

@Suppress("unused")
class LinkFlags : BitSet32 {
    constructor(n: Int) : super(n) {
        reset()
    }

    constructor(data: ByteReader) : super(data) {
        reset()
    }

    private fun reset() {
        clear(11)
        clear(16)
        for (i in 27..31) clear(i)
    }

    fun hasLinkTargetIDList(): Boolean {
        return get(0)
    }

    fun hasLinkInfo(): Boolean {
        return get(1)
    }

    fun hasName(): Boolean {
        return get(2)
    }

    fun hasRelativePath(): Boolean {
        return get(3)
    }

    fun hasWorkingDir(): Boolean {
        return get(4)
    }

    fun hasArguments(): Boolean {
        return get(5)
    }

    fun hasIconLocation(): Boolean {
        return get(6)
    }

    val isUnicode: Boolean
        get() = get(7)

    fun forceNoLinkInfo(): Boolean {
        return get(8)
    }

    fun hasExpString(): Boolean {
        return get(9)
    }

    fun runInSeparateProcess(): Boolean {
        return get(10)
    }

    fun hasDarwinID(): Boolean {
        return get(12)
    }

    fun runAsUser(): Boolean {
        return get(13)
    }

    fun hasExpIcon(): Boolean {
        return get(14)
    }

    fun noPidlAlias(): Boolean {
        return get(15)
    }

    fun runWithShimLayer(): Boolean {
        return get(17)
    }

    fun forceNoLinkTrack(): Boolean {
        return get(18)
    }

    fun enableTargetMetadata(): Boolean {
        return get(19)
    }

    fun disableLinkPathTracking(): Boolean {
        return get(20)
    }

    fun disableKnownFolderTracking(): Boolean {
        return get(21)
    }

    fun disableKnownFolderAlias(): Boolean {
        return get(22)
    }

    fun allowLinkToLink(): Boolean {
        return get(23)
    }

    fun unaliasOnSave(): Boolean {
        return get(24)
    }

    fun preferEnvironmentPath(): Boolean {
        return get(25)
    }

    fun keepLocalIDListForUNCTarget(): Boolean {
        return get(26)
    }

    fun setHasLinkTargetIDList(): LinkFlags {
        set(0)
        return this
    }

    fun setHasLinkInfo(): LinkFlags {
        set(1)
        return this
    }

    fun setHasName(): LinkFlags {
        set(2)
        return this
    }

    fun setHasRelativePath(): LinkFlags {
        set(3)
        return this
    }

    fun setHasWorkingDir(): LinkFlags {
        set(4)
        return this
    }

    fun setHasArguments(): LinkFlags {
        set(5)
        return this
    }

    fun setHasIconLocation(): LinkFlags {
        set(6)
        return this
    }

    fun setIsUnicode(): LinkFlags {
        set(7)
        return this
    }

    fun setForceNoLinkInfo(): LinkFlags {
        set(8)
        return this
    }

    fun setHasExpString(): LinkFlags {
        set(9)
        return this
    }

    fun setRunInSeparateProcess(): LinkFlags {
        set(10)
        return this
    }

    fun setHasDarwinID(): LinkFlags {
        set(12)
        return this
    }

    fun setRunAsUser(): LinkFlags {
        set(13)
        return this
    }

    fun setHasExpIcon(): LinkFlags {
        set(14)
        return this
    }

    fun setNoPidlAlias(): LinkFlags {
        set(15)
        return this
    }

    fun setRunWithShimLayer(): LinkFlags {
        set(17)
        return this
    }

    fun setForceNoLinkTrack(): LinkFlags {
        set(18)
        return this
    }

    fun setEnableTargetMetadata(): LinkFlags {
        set(19)
        return this
    }

    fun setDisableLinkPathTracking(): LinkFlags {
        set(20)
        return this
    }

    fun setDisableKnownFolderTracking(): LinkFlags {
        set(21)
        return this
    }

    fun setDisableKnownFolderAlias(): LinkFlags {
        set(22)
        return this
    }

    fun setAllowLinkToLink(): LinkFlags {
        set(23)
        return this
    }

    fun setUnaliasOnSave(): LinkFlags {
        set(24)
        return this
    }

    fun setPreferEnvironmentPath(): LinkFlags {
        set(25)
        return this
    }

    fun setKeepLocalIDListForUNCTarget(): LinkFlags {
        set(26)
        return this
    }

    fun clearHasLinkTargetIDList(): LinkFlags {
        clear(0)
        return this
    }

    fun clearHasLinkInfo(): LinkFlags {
        clear(1)
        return this
    }

    fun clearHasName(): LinkFlags {
        clear(2)
        return this
    }

    fun clearHasRelativePath(): LinkFlags {
        clear(3)
        return this
    }

    fun clearHasWorkingDir(): LinkFlags {
        clear(4)
        return this
    }

    fun clearHasArguments(): LinkFlags {
        clear(5)
        return this
    }

    fun clearHasIconLocation(): LinkFlags {
        clear(6)
        return this
    }

    fun clearIsUnicode(): LinkFlags {
        clear(7)
        return this
    }

    fun clearForceNoLinkInfo(): LinkFlags {
        clear(8)
        return this
    }

    fun clearHasExpString(): LinkFlags {
        clear(9)
        return this
    }

    fun clearRunInSeparateProcess(): LinkFlags {
        clear(10)
        return this
    }

    fun clearHasDarwinID(): LinkFlags {
        clear(12)
        return this
    }

    fun clearRunAsUser(): LinkFlags {
        clear(13)
        return this
    }

    fun clearHasExpIcon(): LinkFlags {
        clear(14)
        return this
    }

    fun clearNoPidlAlias(): LinkFlags {
        clear(15)
        return this
    }

    fun clearRunWithShimLayer(): LinkFlags {
        clear(17)
        return this
    }

    fun clearForceNoLinkTrack(): LinkFlags {
        clear(18)
        return this
    }

    fun clearEnableTargetMetadata(): LinkFlags {
        clear(19)
        return this
    }

    fun clearDisableLinkPathTracking(): LinkFlags {
        clear(20)
        return this
    }

    fun clearDisableKnownFolderTracking(): LinkFlags {
        clear(21)
        return this
    }

    fun clearDisableKnownFolderAlias(): LinkFlags {
        clear(22)
        return this
    }

    fun clearAllowLinkToLink(): LinkFlags {
        clear(23)
        return this
    }

    fun clearUnaliasOnSave(): LinkFlags {
        clear(24)
        return this
    }

    fun clearPreferEnvironmentPath(): LinkFlags {
        clear(25)
        return this
    }

    fun clearKeepLocalIDListForUNCTarget(): LinkFlags {
        clear(26)
        return this
    }
}
