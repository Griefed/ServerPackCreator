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
package de.griefed.serverpackcreator.app.web.stats.disk

/**
 * One directory's disk usage, as the dashboard shows it: what the filesystem has, and how much of it is SPC's.
 * 
 * [usedBySPC] is reported separately from [freeSpace] because they answer different questions — how close the
 * disk is to full, versus how much of that is this installation's doing.
 */
data class DiskStatsData(
    /** Stable key for this row, so the SPA can match it across refreshes without matching on a path. */
    val identifier: String,
    /** The directory being reported. */
    val dirName: String,
    /** The filesystem root it sits on, which is what the space figures are actually about. */
    val rootName: String,
    /** Total bytes on that root. */
    val totalSpace: Long,
    /** Free bytes on that root. */
    val freeSpace: Long,
    /** Bytes under [dirName] specifically — SPC's own share, not the whole root's used space. */
    val usedBySPC: Long
)
