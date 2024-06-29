/*
 * MIT License
 *
 * Copyright (c) 2022 Griefed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.griefed.serverpackcreator.app.updater.versionchecker

import java.net.URL

/**
 * A release asset is usually some form of compiled program or library.
 * Create a new release asset from an asset name and the [URL] to said asset download.
 * @author Griefed
 * @param assetName Asset name.
 * @param assetUrl Asset download URL.
*/
class ReleaseAsset(private val assetName: String, private val assetUrl: URL) {
    /**
     * Get the name of this asset. Usually a filename.
     * @author Griefed
     * @return The name of this asset. Usually a filename.
     */
    fun name(): String {
        return assetName
    }

    /**
     * Get the download-[URL] to this asset.
     * @author Griefed
     * @return Download-[URL] to this asset.
     */
    fun url(): URL {
        return assetUrl
    }
}
