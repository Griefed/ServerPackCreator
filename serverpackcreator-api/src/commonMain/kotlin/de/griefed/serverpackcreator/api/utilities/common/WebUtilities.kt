/* Copyright (C) 2023  Griefed
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

import de.griefed.serverpackcreator.api.utilities.File
import de.griefed.serverpackcreator.api.utilities.URL

/**
 * Utility-class revolving around interactions with web-resources.
 *
 * @author Griefed
 */
expect class WebUtilities {
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
    fun downloadAndReplaceFile(fileDestination: File, downloadURL: URL): Boolean

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
    fun downloadFile(file: File, downloadURL: URL): Boolean

    /**
     * Open the given url in a browser.
     *
     * @param url The URI to the website you want to open.
     * @author Griefed
     */
    fun openLinkInBrowser(url: URL)

    /**
     * Checks the filesize of the given file whether it is smaller or bigger than 10 MB.
     *
     * @param fileToCheck The file or directory to check.
     * @return Boolean. True if the file is smaller, false if the file is bigger than 10 MB.
     * @author Griefed
     */
    fun hasteBinPreChecks(fileToCheck: File): Boolean

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
    fun createHasteBinFromFile(textFile: File): String

    /**
     * Get the response of a call to a URL as a string.
     *
     * @param url The URL you want to get the response from
     * @return The response.
     */
    fun getResponseAsString(url: URL): String

    /**
     * Get the response-code of a call to a URL as an integer.
     *
     * @param url The URL you want to get the response from
     * @return The response.
     */
    fun getResponseCode(url: URL): Int

    /**
     * Check the availability of the host of the given URL and whether the URL gives a status code of
     * 200. Only when both the host is available and the URL returns a status code of 200 does this
     * method return `true`.
     *
     * @param url The URL of which to check for host-availability.
     * @return `true` if, and only if, the host is available and the URL returns the status code 200.
     */
    fun isReachable(url: URL): Boolean

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
    fun createHasteBinFromString(text: String): String
}
