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
package io

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteOrder

@Suppress("unused")
class ByteWriter(private val stream: OutputStream) : OutputStream() {
    private var le = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
    var position = 0
        private set

    fun changeEndiannes(): ByteWriter {
        le = !le
        return this
    }

    fun setLittleEndian(): ByteWriter {
        le = true
        return this
    }

    fun setBigEndian(): ByteWriter {
        le = false
        return this
    }

    @Throws(IOException::class)
    override fun close() {
        stream.close()
        super.close()
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        position += len
        stream.write(b, off, len)
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        position++
        stream.write(b)
    }

    @Throws(IOException::class)
    fun write(b: Long) {
        write(b.toInt())
    }

    @Throws(IOException::class)
    fun write2bytes(n: Long) {
        val b0 = n and 0xffL
        val b1 = n and 0xff00L shr 8
        if (le) {
            write(b0)
            write(b1)
        } else {
            write(b1)
            write(b0)
        }
    }

    @Throws(IOException::class)
    fun write3bytes(n: Long) {
        val b0 = n and 0xffL
        val b1 = n and 0xff00L shr 8
        val b2 = n and 0xff0000L shr 16
        if (le) {
            write(b0)
            write(b1)
            write(b2)
        } else {
            write(b2)
            write(b1)
            write(b0)
        }
    }

    @Throws(IOException::class)
    fun write4bytes(n: Long) {
        val b0 = n and 0xffL
        val b1 = n and 0xff00L shr 8
        val b2 = n and 0xff0000L shr 16
        val b3 = n and 0xff000000L ushr 24
        if (le) {
            write(b0)
            write(b1)
            write(b2)
            write(b3)
        } else {
            write(b3)
            write(b2)
            write(b1)
            write(b0)
        }
    }

    @Throws(IOException::class)
    fun write5bytes(n: Long) {
        val b0 = n and 0xffL
        val b1 = n and 0xff00L shr 8
        val b2 = n and 0xff0000L shr 16
        val b3 = n and 0xff000000L ushr 24
        val b4 = n and 0xff00000000L shr 32
        if (le) {
            write(b0)
            write(b1)
            write(b2)
            write(b3)
            write(b4)
        } else {
            write(b4)
            write(b3)
            write(b2)
            write(b1)
            write(b0)
        }
    }

    @Throws(IOException::class)
    fun write6bytes(n: Long) {
        val b0 = n and 0xffL
        val b1 = n and 0xff00L shr 8
        val b2 = n and 0xff0000L shr 16
        val b3 = n and 0xff000000L ushr 24
        val b4 = n and 0xff00000000L shr 32
        val b5 = n and 0xff0000000000L shr 40
        if (le) {
            write(b0)
            write(b1)
            write(b2)
            write(b3)
            write(b4)
            write(b5)
        } else {
            write(b5)
            write(b4)
            write(b3)
            write(b2)
            write(b1)
            write(b0)
        }
    }

    @Throws(IOException::class)
    fun write7bytes(n: Long) {
        val b0 = n and 0xffL
        val b1 = n and 0xff00L shr 8
        val b2 = n and 0xff0000L shr 16
        val b3 = n and 0xff000000L ushr 24
        val b4 = n and 0xff00000000L shr 32
        val b5 = n and 0xff0000000000L shr 40
        val b6 = n and 0xff000000000000L shr 48
        if (le) {
            write(b0)
            write(b1)
            write(b2)
            write(b3)
            write(b4)
            write(b5)
            write(b6)
        } else {
            write(b6)
            write(b5)
            write(b4)
            write(b3)
            write(b2)
            write(b1)
            write(b0)
        }
    }

    @Throws(IOException::class)
    fun write8bytes(n: Long) {
        val b0 = n and 0xffL
        val b1 = n and 0xff00L shr 8
        val b2 = n and 0xff0000L shr 16
        val b3 = n and 0xff000000L ushr 24
        val b4 = n and 0xff00000000L shr 32
        val b5 = n and 0xff0000000000L shr 40
        val b6 = n and 0xff000000000000L shr 48
        val b7 = n and -0x100000000000000L ushr 56
        if (le) {
            write(b0)
            write(b1)
            write(b2)
            write(b3)
            write(b4)
            write(b5)
            write(b6)
            write(b7)
        } else {
            write(b7)
            write(b6)
            write(b5)
            write(b4)
            write(b3)
            write(b2)
            write(b1)
            write(b0)
        }
    }

    @Throws(IOException::class)
    fun writeString(s: String?) {
        write(s!!.toByteArray())
        write(0)
    }

    @Throws(IOException::class)
    fun writeUnicodeStringNullTerm(s: String?) {
        for (i in 0 until s!!.length) write2bytes(s[i].code.toLong())
        write2bytes(0)
    }

    @Throws(IOException::class)
    fun writeUnicodeStringSizePadded(s: String?) {
        write2bytes(s!!.length.toLong())
        for (element in s) write2bytes(element.code.toLong())
    }
}
