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
package de.griefed.serverpackcreator.grinder.loader

import java.io.File
import java.security.MessageDigest

/**
 * Identifies the start-script templates a cached loader install was produced with.
 *
 * A tuple's cached layer is whatever the pack's own `start.sh` created at install time, so it is a product of the
 * templates then in force. Recording a digest of those templates in the completion marker lets the cache notice
 * that they have since changed, instead of silently serving a layer built by different logic.
 *
 * @author Griefed
 */
object TemplateProvenance {

    /**
     * A content digest over [templates], or `null` when none of them can be read. Sorted by absolute path first, so
     * the digest describes the *set* of templates rather than the order a caller happened to list them in; each
     * file's path is mixed in alongside its bytes so swapping two templates' contents is still a change.
     *
     * `null` is the honest answer for an unreadable home and deliberately means "unknown": callers must treat it as
     * no information rather than as a mismatch, or a broken home would invalidate the entire cache.
     */
    fun digestOf(templates: List<File>): String? {
        val readable = templates.filter { it.isFile }.sortedBy { it.absolutePath }
        if (readable.isEmpty()) {
            return null
        }
        val digest = MessageDigest.getInstance("SHA-256")
        for (template in readable) {
            val bytes = runCatching { template.readBytes() }.getOrNull() ?: continue
            digest.update(template.name.toByteArray())
            digest.update(bytes)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
