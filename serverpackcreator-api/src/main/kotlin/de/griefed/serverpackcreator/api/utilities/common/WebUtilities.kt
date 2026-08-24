/* Copyright (C) 2026 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.api.utilities.common

import de.griefed.serverpackcreator.api.ApiProperties
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Desktop
import java.io.*
import java.net.*
import java.nio.channels.Channels
import java.util.*
import javax.net.ssl.HttpsURLConnection

/**
 * Opens a connection to this URL with [connectTimeout] and [readTimeout] applied, in milliseconds.
 *
 * The single place that knows *how* a timeout is applied. [WebUtilities.openTimedConnection] is the
 * usual way in and supplies the configured values, but the settings groups cannot use it — `WebUtilities`
 * is constructed *from* `ApiProperties`, so a group inside `ApiProperties` reaching for it would close a
 * cycle. They call this directly with values from their own [de.griefed.serverpackcreator.api.settings.NetworkConfig]
 * instead, which keeps the mechanism in one place even though the values arrive by two routes.
 *
 * Returns [URLConnection], not `HttpURLConnection`: the timeout setters live on `URLConnection`, and a
 * `file:` URL yields a `FileURLConnection` whose cast would throw `ClassCastException` — which is not an
 * `IOException`, and so escapes callers' error handling. Callers needing `responseCode` cast themselves.
 *
 * @param connectTimeout Milliseconds to wait for the connection. `0` is the JDK's "wait forever".
 * @param readTimeout Milliseconds a single read may block. `0` is the JDK's "wait forever".
 */
@Throws(IOException::class)
fun URL.timedConnection(connectTimeout: Int, readTimeout: Int): URLConnection {
    val connection = this.openConnection()
    connection.connectTimeout = connectTimeout
    connection.readTimeout = readTimeout
    return connection
}

/**
 * Utility-class revolving around interactions with web-resources.
 *
 * @param apiProperties API configuration of this instance.
 *
 * @author Griefed
 */
@Suppress("unused")
class WebUtilities(private val apiProperties: ApiProperties) {
    /**
     * HasteBin's two upload ceilings. Private on purpose — they are that service's limits rather than anything
     * this class promises, so they are checked here and never handed out.
     */
    companion object {
        /** HasteBin's upload ceiling in bytes; a larger file is rejected before any request is made. */
        private const val MAX_HASTEBIN_BYTES = 10_000_000.0

        /** HasteBin's upload ceiling in characters, which is a different measurement from the bytes above. */
        private const val MAX_HASTEBIN_CHARACTERS = 400_000
    }

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Opens a connection to [url] with the configured connect- and read-timeouts applied, defaulting
     * [readTimeout] to the metadata timeout.
     *
     * **Every outbound call goes through here.** The JDK's default is to wait forever, so a
     * connection opened anywhere else is a hang waiting to happen — the same single-source-of-truth
     * rule this module applies to `SupportedModloaders` and `ModScanner.scannerFor`, and for the same
     * reason: the copies drift, and the one without the timeout is the one that strands a user.
     *
     * Redirects are deliberately left following (the JDK default): `maven.legacyfabric.net` answers
     * `302`, so a connection which stopped following them would report that host as unreachable.
     *
     * Returns [URLConnection] rather than `HttpURLConnection` **on purpose.** The timeout setters live
     * on `URLConnection`, so narrowing buys nothing — and it costs correctness: [downloadFile] is
     * published API that accepts any URL, a `file:` URL yields a `FileURLConnection`, and casting that
     * throws `ClassCastException`, which is not an `IOException` and so escapes every caller's error
     * handling. Callers needing `responseCode` cast for themselves, exactly as they did before.
     *
     * @param url The URL to connect to.
     * @param readTimeout Milliseconds a single read may block. Pass
     * [ApiProperties.networkDownloadReadTimeout] for file-downloads.
     * @return The opened, timeout-carrying connection.
     */
    @Throws(IOException::class)
    fun openTimedConnection(url: URL, readTimeout: Int = apiProperties.networkReadTimeout): URLConnection =
        url.timedConnection(apiProperties.networkConnectTimeout, readTimeout)

    /**
     * Opens an input-stream on [url] with the configured timeouts applied — the timeout-carrying
     * replacement for `URL.openStream()`, which inherits the JDK's unbounded default.
     *
     * @param url The URL to read from.
     * @param readTimeout Milliseconds a single read may block.
     * @return The response body's stream. Closing it releases the connection.
     */
    @Throws(IOException::class)
    fun openTimedStream(url: URL, readTimeout: Int = apiProperties.networkReadTimeout): InputStream =
        openTimedConnection(url, readTimeout).inputStream

    /**
     * Download the file from the specified URL to the specified destination, replacing the file if it
     * already exists. The destination should end in a valid filename. Any directories up to the
     * specified file will be created.
     *
     * @param fileDestination The file to store the web-resource in. Examples:
     * * /tmp/some_folder/foo.bar
     * * C:/temp/some_folder/bar.foo
     * @param downloadURL     The URL to the file you want to download.
     * @return Boolean. Returns true if the file could be found on the hosts' filesystem.
     * @author Griefed
     */
    fun downloadAndReplaceFile(
        fileDestination: File,
        downloadURL: URL
    ): Boolean {
        fileDestination.deleteQuietly()
        return downloadFile(fileDestination, downloadURL)
    }

    /**
     * Download the file from the specified URL to the specified destination. The destination should
     * end in a valid filename. Any directories up to the specified file will be created.
     *
     * @param file The destination where the file should be stored. Must include the filename as well. Examples:
     * * /tmp/some_folder/foo.bar
     * * C:/temp/some_folder/bar.foo
     * @param downloadURL     The URL to the file you want to download.
     * @return true if the file was created.
     * @author Griefed
     */
    fun downloadFile(
        file: File,
        downloadURL: URL
    ): Boolean {
        file.create()
        try {
            openTimedStream(downloadURL, apiProperties.networkDownloadReadTimeout).use { url ->
                Channels.newChannel(url).use { channel ->
                    file.outputStream().use { stream ->
                        stream.channel.transferFrom(channel, 0, Long.MAX_VALUE)
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("An error occurred downloading $file from $downloadURL.", ex)
            file.deleteQuietly()
        }
        return file.isFile
    }

    /**
     * Open the given url in a browser.
     *
     * @param url The URI to the website you want to open.
     * @author Griefed
     */
    fun openLinkInBrowser(url: URL) {
        try {
            openLinkInBrowser(url.toURI())
        } catch (ex: URISyntaxException) {
            log.error("Error opening browser with link $url.", ex)
        }
    }

    /**
     * Open the given uri in a browser.
     *
     * @param uri The URI to the website you want to open.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun openLinkInBrowser(uri: URI) {
        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri)
            }
        } catch (ex: IOException) {
            log.error("Error opening browser with link $uri.", ex)
        }
    }

    /**
     * Checks the filesize of the given file whether it is smaller or bigger than 10 MB.
     *
     * @param fileToCheck The file or directory to check.
     * @return Boolean. True if the file is smaller, false if the file is bigger than 10 MB.
     * @author Griefed
     */
    fun hasteBinPreChecks(fileToCheck: File): Boolean {
        val fileSize = fileToCheck.size()
        try {
            return if (fileSize < MAX_HASTEBIN_BYTES
                && fileToCheck.hasFewerCharactersThan(MAX_HASTEBIN_CHARACTERS)
            ) {
                log.debug("Smaller. $fileSize byte.")
                true
            } else {
                log.debug("Bigger. $fileSize byte.")
                false
            }
        } catch (ex: IOException) {
            log.error("Couldn't read file: $fileToCheck", ex)
        }
        return false
    }

    /**
     * Whether this file holds fewer than [limit] characters, without materialising it.
     *
     * Counts through an 8 KB buffer and stops at the first character past the limit, so the answer costs
     * 16 KB of `char` regardless of file size — where `readText().length` allocated the whole file as a
     * String, up to 20 MB of `char` for a 10 MB log, purely to count it.
     *
     * **Characters, not bytes**, deliberately: UTF-8 spends up to four bytes on one character, so a file
     * can be past the byte limit while well under the character one. The one shortcut that *is* sound is
     * the early return — every character occupies at least one byte, so a file shorter than [limit] bytes
     * cannot hold [limit] characters, and the common case never opens the file at all.
     *
     * `isFile` guards that shortcut for a reason a test caught: `File.length()` on a **directory** returns
     * some small unspecified number, which would short-circuit to `true` and make the caller accept a
     * directory. Falling through to [reader] instead throws, which is how the caller has always arrived at
     * `false` for one.
     */
    private fun File.hasFewerCharactersThan(limit: Int): Boolean {
        if (isFile && length() < limit) {
            return true
        }
        val buffer = CharArray(8192)
        var counted = 0L
        reader().use { reader ->
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) {
                    break
                }
                counted += read
                if (counted >= limit) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Create a HasteBin post from a given string. The text provided passed onto [Haste zneix](https://haste.zneix.eu)
     * which creates a HasteBin post out of the passed String and returns the URL to the newly created post.
     *
     * Created with the help of [kaimu-ken's hastebin.java (MIT License)](https://github.com/kaimu-kun/hastebin.java)
     * and edited to use HasteBin fork [zneix/haste-server](https://github.com/zneix/haste-server). My fork of kaimu-kun's
     * hastebin.java is available at [Griefed/hastebin.java](https://github.com/Griefed/hastebin.java).
     *
     * @param text The file which will be read into a String of which then to create a HasteBin
     * post of.
     * @return String. Returns a String containing the URL to the newly created HasteBin post.
     * @author [kaimu-kun/hastebin.java](https://github.com/kaimu-kun)
     * @author Griefed
     */
    fun createHasteBinFromString(text: String): String {
        val requestURL: String = apiProperties.hasteBinServerUrl
        var response: String? = null
        val url = URI(requestURL).toURL()
        var conn: HttpsURLConnection? = null
        val postData: ByteArray = text.toByteArray()
        val postDataLength = postData.size

        try {
            // Through the shared opener like everything else, then narrowed: this call needs the
            // HttpsURLConnection type for its POST. Setting the two timeouts by hand here worked, but it
            // was a third way of applying them, and the whole point of one opener is that there is not one.
            conn = openTimedConnection(url) as HttpsURLConnection
        } catch (ex: IOException) {
            log.error("Error during opening of connection to URL.", ex)
        }
        Objects.requireNonNull(conn)?.doOutput = true
        conn?.instanceFollowRedirects = false
        try {
            conn?.requestMethod = "POST"
            conn?.setRequestProperty("User-Agent", "HasteBin-Creator for ServerPackCreator")
            conn?.setRequestProperty("Content-Length", postDataLength.toString())
            conn?.useCaches = false
        } catch (ex: ProtocolException) {
            log.error("Error during request of POST method.", ex)
        }

        try {
            if (conn != null) {
                DataOutputStream(conn.outputStream).use { dataOutputStream ->
                    dataOutputStream.write(postData)
                    try {
                        conn.inputStream.use { conStream ->
                            conStream.bufferedReader().use {
                                response = it.readLine()
                            }
                        }
                    } catch (ex: IOException) {
                        log.error("Error encountered when acquiring HasteBin.", ex)
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("Error encountered when acquiring HasteBin.", ex)
        }
        if (response!!.contains("\"key\"")) {
            response = (requestURL.replace("/documents", "/")
                    + response.substring(response.indexOf(":") + 2, response.length - 2))
        }
        return if (response.contains(requestURL.replace("/documents", ""))) {
            response
        } else {
            "Error encountered when acquiring response from URL."
        }
    }

    /**
     * Create a HasteBin post from a given text file. The text file provided is read into a string and
     * then passed onto [Haste zneix](https://haste.zneix.eu) which creates a HasteBin post
     * out of the passed String and returns the URL to the newly created post.
     *
     * Created with the help of [kaimu-ken's hastebin.java (MIT License)](https://github.com/kaimu-kun/hastebin.java)
     * and edited to use HasteBin fork [zneix/haste-server](https://github.com/zneix/haste-server). My fork of kaimu-kun's
     * hastebin.java is available at [Griefed/hastebin.java](https://github.com/Griefed/hastebin.java).
     *
     * @param textFile The file which will be read into a String of which then to create a HasteBin
     * post of.
     * @return String. Returns a String containing the URL to the newly created HasteBin post.
     * @author [kaimu-kun/hastebin.java](https://github.com/kaimu-kun)
     * @author Griefed
     */
    fun createHasteBinFromFile(textFile: File): String {
        return createHasteBinFromString(textFile.readText())
    }

    /**
     * Get the response of a call to a URL as a string.
     *
     * @param url The URL you want to get the response from
     * @return The response.
     * @throws IOException if the URL could not be called or a communication error occurred.
     */
    @Throws(IOException::class)
    fun getResponseAsString(url: URL): String {
        val `in` = BufferedReader(InputStreamReader(openTimedStream(url)))
        val response = StringBuilder()
        var currentLine: String?
        while (`in`.readLine().also { currentLine = it } != null) {
            response.append(currentLine)
        }
        `in`.close()
        return response.toString()
    }

    /**
     * Get the response-code of a call to a URL as an integer.
     *
     * @param url The URL you want to get the response from
     * @return The response.
     * @throws IOException if the URL could not be called or a communication error occurred.
     */
    @Throws(IOException::class)
    fun getResponseCode(url: URL): Int {
        val connection = openTimedConnection(url) as HttpURLConnection
        return try {
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Check the availability of the host of the given URL and whether the URL gives a status code of
     * 200. Only when both the host is available and the URL returns a status code of 200 does this
     * method return `true`.
     *
     * @param url The URL of which to check for host-availability.
     * @return `true` if, and only if, the host is available and the URL returns the status code 200.
     */
    fun isReachable(url: URL): Boolean {
        var available: Boolean
        var connection: HttpURLConnection? = null
        try {
            val host = url.host
            log.trace("URL:  $url")
            log.trace("Host: $host")
            connection = openTimedConnection(url) as HttpURLConnection
            available = connection.responseCode == 200
        } catch (e: IOException) {
            available = false
        } finally {
            connection?.disconnect()
        }
        if (!available) {
            log.warn("Could not successfully connect to $url")
        }
        return available
    }
}