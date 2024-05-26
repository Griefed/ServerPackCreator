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
import mslinks.ShellLinkException
import mslinks.UnsupportedItemIDException
import java.io.IOException

class ItemIDRoot : ItemIDRegItem {
    constructor() : super(GROUP_ROOT or TYPE_ROOT_REGITEM)
    constructor(flags: Int) : super(flags or GROUP_ROOT) {
        val subType = typeFlags and ID_TYPE_INGROUPMASK
        if (subType != TYPE_ROOT_REGITEM) {
            throw UnsupportedItemIDException(typeFlags)
        }
    }

    @Throws(IOException::class, ShellLinkException::class)
    override fun load(br: ByteReader, maxSize: Int) {
        val endPos = br.position + maxSize
        super.load(br, maxSize)
        br.seekTo(endPos)
    }

    override fun toString(): String {
        return if (clsid == Registry.CLSID_COMPUTER) {
            ""
        } else {
            super.toString()
        }
    }
}
