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
class LinkInfoFlags : BitSet32 {
    constructor(n: Int) : super(n) {
        reset()
    }

    constructor(data: ByteReader) : super(data) {
        reset()
    }

    private fun reset() {
        for (i in 2..31) clear(i)
    }

    fun hasVolumeIDAndLocalBasePath(): Boolean {
        return get(0)
    }

    fun hasCommonNetworkRelativeLinkAndPathSuffix(): Boolean {
        return get(1)
    }

    fun setVolumeIDAndLocalBasePath(): LinkInfoFlags {
        set(0)
        return this
    }

    fun setCommonNetworkRelativeLinkAndPathSuffix(): LinkInfoFlags {
        set(1)
        return this
    }

    fun clearVolumeIDAndLocalBasePath(): LinkInfoFlags {
        clear(0)
        return this
    }

    fun clearCommonNetworkRelativeLinkAndPathSuffix(): LinkInfoFlags {
        clear(1)
        return this
    }
}
