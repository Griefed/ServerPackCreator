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
import java.io.InputStream
import java.nio.ByteOrder

@Suppress("unused")
class ByteReader(private val stream: InputStream) : InputStream() {
    private var le = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
    var position = 0
        private set

    fun changeEndiannes(): ByteReader {
        le = !le
        return this
    }

    fun setLittleEndian(): ByteReader {
        le = true
        return this
    }

    fun setBigEndian(): ByteReader {
        le = false
        return this
    }

    @Throws(IOException::class)
    fun seek(n: Int): Boolean {
        if (n <= 0) {
            return false
        }
        for (i in 0 until n) {
            read()
        }
        return true
    }

    @Throws(IOException::class)
    fun seekTo(newPos: Int): Boolean {
        return seek(newPos - position)
    }

    @Throws(IOException::class)
    override fun close() {
        stream.close()
        super.close()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val result = stream.read(b, off, len)
        position += result
        return result
    }

    @Throws(IOException::class)
    override fun read(): Int {
        position++
        return stream.read()
    }

    @Throws(IOException::class)
    fun read2bytes(): Long {
        val b0 = read().toLong()
        val b1 = read().toLong()
        return if (le) {
            b0 or (b1 shl 8)
        } else {
            b1 or (b0 shl 8)
        }
    }

    @Throws(IOException::class)
    fun read3bytes(): Long {
        val b0 = read().toLong()
        val b1 = read().toLong()
        val b2 = read().toLong()
        return if (le) {
            b0 or (b1 shl 8) or (b2 shl 16)
        } else {
            b2 or (b1 shl 8) or (b0 shl 16)
        }
    }

    @Throws(IOException::class)
    fun read4bytes(): Long {
        val b0 = read().toLong()
        val b1 = read().toLong()
        val b2 = read().toLong()
        val b3 = read().toLong()
        return if (le) {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        } else {
            b3 or (b2 shl 8) or (b1 shl 16) or (b0 shl 24)
        }
    }

    @Throws(IOException::class)
    fun read5bytes(): Long {
        val b0 = read().toLong()
        val b1 = read().toLong()
        val b2 = read().toLong()
        val b3 = read().toLong()
        val b4 = read().toLong()
        return if (le) {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or (b4 shl 32)
        } else {
            b4 or (b3 shl 8) or (b2 shl 16) or (b1 shl 24) or (b0 shl 32)
        }
    }

    @Throws(IOException::class)
    fun read6bytes(): Long {
        val b0 = read().toLong()
        val b1 = read().toLong()
        val b2 = read().toLong()
        val b3 = read().toLong()
        val b4 = read().toLong()
        val b5 = read().toLong()
        return if (le) {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or (b4 shl 32) or (b5 shl 40)
        } else {
            b5 or (b4 shl 8) or (b3 shl 16) or (b2 shl 24) or (b1 shl 32) or (b0 shl 40)
        }
    }

    @Throws(IOException::class)
    fun read7bytes(): Long {
        val b0 = read().toLong()
        val b1 = read().toLong()
        val b2 = read().toLong()
        val b3 = read().toLong()
        val b4 = read().toLong()
        val b5 = read().toLong()
        val b6 = read().toLong()
        return if (le) {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or (b4 shl 32) or (b5 shl 40) or (b6 shl 48)
        } else {
            b6 or (b5 shl 8) or (b4 shl 16) or (b3 shl 24) or (b2 shl 32) or (b1 shl 40) or (b0 shl 48)
        }
    }

    @Throws(IOException::class)
    fun read8bytes(): Long {
        val b0 = read().toLong()
        val b1 = read().toLong()
        val b2 = read().toLong()
        val b3 = read().toLong()
        val b4 = read().toLong()
        val b5 = read().toLong()
        val b6 = read().toLong()
        val b7 = read().toLong()
        return if (le) {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or (b4 shl 32) or (b5 shl 40) or (b6 shl 48) or (b7 shl 56)
        } else {
            b7 or (b6 shl 8) or (b5 shl 16) or (b4 shl 24) or (b3 shl 32) or (b2 shl 40) or (b1 shl 48) or (b0 shl 56)
        }
    }

    /**
     * reads 0-terminated string in default code page
     * @param sz - maximum size in bytes
     */
    @Throws(IOException::class)
    fun readString(sz: Int): String? {
        if (sz == 0) {
            return null
        }
        val buf = ByteArray(sz)
        var i = 0
        while (i < sz) {
            val b = read()
            if (b == 0) {
                break
            }
            buf[i] = b.toByte()
            i++
        }
        return if (i == 0) {
            null
        } else {
            String(buf, 0, i)
        }
    }

    /**
     * reads 0-terminated string in unicode
     * @param sz - maximum size in characters
     */
    @Throws(IOException::class)
    fun readUnicodeStringNullTerm(sz: Int): String? {
        if (sz == 0) {
            return null
        }
        val buf = CharArray(sz)
        var i = 0
        while (i < sz) {
            val c = Char(read2bytes().toUShort())
            if (c.code == 0) {
                break
            }
            buf[i] = c
            i++
        }
        return if (i == 0) {
            null
        } else {
            String(buf, 0, i)
        }
    }

    /**
     * reads unicode string that has 2 bytes at start indicates length of string
     */
    @Throws(IOException::class)
    fun readUnicodeStringSizePadded(): String {
        val c = read2bytes().toInt()
        val buf = CharArray(c)
        for (i in 0 until c) {
            buf[i] = Char(read2bytes().toUShort())
        }
        return String(buf)
    }
}