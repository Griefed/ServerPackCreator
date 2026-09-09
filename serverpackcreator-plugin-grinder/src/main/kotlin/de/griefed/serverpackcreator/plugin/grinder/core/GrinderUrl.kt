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
package de.griefed.serverpackcreator.plugin.grinder.core

import java.net.URI

/**
 * The single rule for turning an operator-typed grinder address into something requestable, plus the
 * endpoints derived from it. Both the Settings pane and [GrinderClient] go through here, so what the
 * pane accepts and what the client requests cannot drift apart.
 *
 * @author Griefed
 */
object GrinderUrl {

    /** The endpoint carrying the verdict rows — see the grinder's README. */
    private const val VERDICTS_PATH = "/verdicts.json"

    /** The endpoint the Dashboard polls; the same document the daemon's own `/dashboard` page reads. */
    private const val STATUS_PATH = "/status"

    /** The only two schemes the JDK HTTP client speaks, and the only two the daemon ever binds. */
    private val supportedSchemes = setOf("http", "https")

    /**
     * Reduce [raw] to a base URL with no trailing slash, or `null` when it could never be requested.
     *
     * A string without a scheme is assumed to be `http`, because `localhost:8757` is what the grinder
     * prints in its own startup log and refusing the likeliest input teaches the operator nothing. Blank
     * input is `null` rather than an error: it is the shipped default and means "idle".
     */
    fun normalise(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }
        val schemed = if (supportedSchemes.any { trimmed.startsWith("$it://", ignoreCase = true) }) {
            trimmed
        } else if (trimmed.contains("://")) {
            // A scheme was typed and it is not one we can speak. Prefixing http:// would silently turn
            // ftp://host into http://ftp://host, so this is refused rather than repaired.
            return null
        } else {
            "http://$trimmed"
        }
        val withoutTrailingSlashes = schemed.trimEnd('/')
        val parsed = runCatching { URI(withoutTrailingSlashes) }.getOrNull() ?: return null
        return if (parsed.host.isNullOrBlank()) null else withoutTrailingSlashes
    }

    /** The verdict feed of the grinder at [base], which must already be [normalise]d. */
    fun verdicts(base: String) = base + VERDICTS_PATH

    /** The status document of the grinder at [base], which must already be [normalise]d. */
    fun status(base: String) = base + STATUS_PATH
}
