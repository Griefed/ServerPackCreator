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
package de.griefed.serverpackcreator.app.web.storage

import java.nio.file.Path

/** A file that has been stored: where it went, what it was called, and the hash the duplicate-check keys on. */
class SavedFile(
    /** The id the file is stored and retrieved under — not its name. */
    val id: String,
    /** SHA256 of the contents, which is what makes a re-upload recognisable. */
    val sha256: String,
    /** Where it actually landed on disk. */
    val file: Path,
    /** The name it was uploaded as, kept for display and for serving it back. */
    val originalName: String,
    /** Size in bytes. */
    val size: Int
)