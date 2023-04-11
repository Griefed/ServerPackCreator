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
class ConsoleFlags : BitSet32 {
    constructor(n: Int) : super(n)
    constructor(data: ByteReader) : super(data)

    val isBoldFont: Boolean
        get() = get(0)
    val isFullscreen: Boolean
        get() = get(1)
    val isQuickEdit: Boolean
        get() = get(2)
    val isInsertMode: Boolean
        get() = get(3)
    val isAutoPosition: Boolean
        get() = get(4)
    val isHistoryDup: Boolean
        get() = get(5)

    fun setBoldFont(): ConsoleFlags {
        set(0)
        return this
    }

    fun setFullscreen(): ConsoleFlags {
        set(1)
        return this
    }

    fun setQuickEdit(): ConsoleFlags {
        set(2)
        return this
    }

    fun setInsertMode(): ConsoleFlags {
        set(3)
        return this
    }

    fun setAutoPosition(): ConsoleFlags {
        set(4)
        return this
    }

    fun setHistoryDup(): ConsoleFlags {
        set(5)
        return this
    }

    fun clearBoldFont(): ConsoleFlags {
        clear(0)
        return this
    }

    fun clearFullscreen(): ConsoleFlags {
        clear(1)
        return this
    }

    fun clearQuickEdit(): ConsoleFlags {
        clear(2)
        return this
    }

    fun clearInsertMode(): ConsoleFlags {
        clear(3)
        return this
    }

    fun clearAutoPosition(): ConsoleFlags {
        clear(4)
        return this
    }

    fun clearHistoryDup(): ConsoleFlags {
        clear(5)
        return this
    }
}
