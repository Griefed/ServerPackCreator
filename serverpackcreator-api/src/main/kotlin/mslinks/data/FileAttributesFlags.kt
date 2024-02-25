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
class FileAttributesFlags : BitSet32 {
    constructor(n: Int) : super(n) {
        reset()
    }

    constructor(data: ByteReader) : super(data) {
        reset()
    }

    private fun reset() {
        clear(3)
        clear(6)
        for (i in 15..31) clear(i)
    }

    val isReadonly: Boolean
        get() = get(0)
    val isHidden: Boolean
        get() = get(1)
    val isSystem: Boolean
        get() = get(2)
    val isDirectory: Boolean
        get() = get(4)
    val isArchive: Boolean
        get() = get(5)
    val isNormal: Boolean
        get() = get(7)
    val isTemporary: Boolean
        get() = get(8)
    val isSparseFile: Boolean
        get() = get(9)
    val isReparsePoint: Boolean
        get() = get(10)
    val isCompressed: Boolean
        get() = get(11)
    val isOffline: Boolean
        get() = get(12)
    val isNotContentIndexed: Boolean
        get() = get(13)
    val isEncrypted: Boolean
        get() = get(14)

    fun setReadonly(): FileAttributesFlags {
        set(0)
        return this
    }

    fun setHidden(): FileAttributesFlags {
        set(1)
        return this
    }

    fun setSystem(): FileAttributesFlags {
        set(2)
        return this
    }

    fun setDirectory(): FileAttributesFlags {
        set(4)
        return this
    }

    fun setArchive(): FileAttributesFlags {
        set(5)
        return this
    }

    fun setNormal(): FileAttributesFlags {
        set(7)
        return this
    }

    fun setTemporary(): FileAttributesFlags {
        set(8)
        return this
    }

    fun setSparseFile(): FileAttributesFlags {
        set(9)
        return this
    }

    fun setReparsePoint(): FileAttributesFlags {
        set(10)
        return this
    }

    fun setCompressed(): FileAttributesFlags {
        set(11)
        return this
    }

    fun setOffline(): FileAttributesFlags {
        set(12)
        return this
    }

    fun setNotContentIndexed(): FileAttributesFlags {
        set(13)
        return this
    }

    fun setEncrypted(): FileAttributesFlags {
        set(14)
        return this
    }

    fun clearReadonly(): FileAttributesFlags {
        clear(0)
        return this
    }

    fun clearHidden(): FileAttributesFlags {
        clear(1)
        return this
    }

    fun clearSystem(): FileAttributesFlags {
        clear(2)
        return this
    }

    fun clearDirectory(): FileAttributesFlags {
        clear(4)
        return this
    }

    fun clearArchive(): FileAttributesFlags {
        clear(5)
        return this
    }

    fun clearNormal(): FileAttributesFlags {
        clear(7)
        return this
    }

    fun clearTemporary(): FileAttributesFlags {
        clear(8)
        return this
    }

    fun clearSparseFile(): FileAttributesFlags {
        clear(9)
        return this
    }

    fun clearReparsePoint(): FileAttributesFlags {
        clear(10)
        return this
    }

    fun clearCompressed(): FileAttributesFlags {
        clear(11)
        return this
    }

    fun clearOffline(): FileAttributesFlags {
        clear(12)
        return this
    }

    fun clearNotContentIndexed(): FileAttributesFlags {
        clear(13)
        return this
    }

    fun clearEncrypted(): FileAttributesFlags {
        clear(14)
        return this
    }
}
