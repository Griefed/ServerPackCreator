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
package de.griefed.serverpackcreator.plugin.grinder.gui

import com.fasterxml.jackson.databind.JsonNode

/**
 * Turns fields of a `/status` document into the strings the Dashboard shows.
 *
 * Separate from the panel because it is the only part of that panel which is not layout, and because it
 * reads a document produced by a daemon the user upgrades independently — "the field is missing" and "the
 * field changed type" are the ordinary cases here, not the exotic ones, and they deserve guards.
 *
 * @author Griefed
 */
object StatusFormatting {

    /** What a missing, null or unusable value renders as, so a gap is visibly a gap. */
    const val ABSENT = "—"

    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
    private const val SECONDS_PER_DAY = 86_400L

    /**
     * [node] as something fit to display: its text, or [ABSENT] when it is missing, null or blank.
     *
     * Blank counts as absent because a daemon reporting `""` for a worker's slug is telling us nothing,
     * and an empty table cell reads as a rendering bug rather than as missing data.
     */
    fun text(node: JsonNode): String =
        if (node.isMissingNode || node.isNull) ABSENT else node.asText().ifBlank { ABSENT }

    /**
     * A span of seconds as a human would say it — `4h 12m` rather than `15134`, the same rendering the
     * daemon's own dashboard does in JavaScript.
     *
     * Only a JSON **number** is a duration: a string holding digits means the daemon changed its contract,
     * and quietly parsing it would hide that. A negative one cannot happen and renders as absent rather
     * than as a nonsense span.
     */
    fun duration(node: JsonNode): String {
        if (!node.isNumber) {
            return ABSENT
        }
        val total = node.asLong()
        if (total < 0) {
            return ABSENT
        }
        val days = total / SECONDS_PER_DAY
        val hours = (total % SECONDS_PER_DAY) / SECONDS_PER_HOUR
        val minutes = (total % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = total % SECONDS_PER_MINUTE
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
