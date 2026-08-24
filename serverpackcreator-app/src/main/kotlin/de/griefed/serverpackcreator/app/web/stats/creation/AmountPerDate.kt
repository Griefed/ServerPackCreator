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
package de.griefed.serverpackcreator.app.web.stats.creation

/**
 * One day's count, for the dashboard's time series. The date is a pre-formatted string rather than a `Date`
 * because it is a bucket label, not an instant — the aggregation has already decided the granularity.
 */
data class AmountPerDate(
    /** How many things happened on [date]. */
    val creations: Int,
    /** The day, pre-formatted — a bucket label rather than an instant, since the granularity is already decided. */
    val date: String
)
