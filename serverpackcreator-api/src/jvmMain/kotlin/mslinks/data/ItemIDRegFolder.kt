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

@Suppress("unused")
class ItemIDRegFolder : ItemIDRegItem {
    constructor() : super(GROUP_COMPUTER or TYPE_DRIVE_REGITEM)
    constructor(flags: Int) : super(flags or GROUP_COMPUTER) {
        val subType = typeFlags and ID_TYPE_INGROUPMASK
        if (subType != TYPE_DRIVE_REGITEM) throw UnsupportedItemIDException(typeFlags)
    }

    @Throws(IOException::class, ShellLinkException::class)
    override fun load(br: ByteReader, maxSize: Int) {
        val endPos = br.position + maxSize
        super.load(br, maxSize)
        // depending on ItemIDRoot there might be some padding before this item's clsid,
        // but it is 0 anyway since only CLSID_COMPUTER is supported for now

        // see CRegFolder::_bFlagsLegacy, CRegFolder::_cbPadding and IDREGITEMEX (regfldr.cpp)
        // search for CRegFolder_CreateInstance, there are several cases where regitem is used, not only 0x2e
        // the padding and the legacy flags are actually used for TYPE_CONTROL_REGITEM/TYPE_CONTROL_REGITEM_EX
        br.seekTo(endPos)
    }
}
