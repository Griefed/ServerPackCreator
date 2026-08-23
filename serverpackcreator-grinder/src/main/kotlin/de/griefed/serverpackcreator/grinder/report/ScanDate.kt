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
package de.griefed.serverpackcreator.grinder.report

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Writes a verdict's `verifiedAt` as the `YEAR/MM/DD` the overview and the CSV both show.
 *
 * Shared by [VerdictReportRenderer] and [VerdictCsvExporter] rather than duplicated, because the two are
 * meant to agree — the table's download button hands out the exporter's own output, so a divergence would
 * show up as one page disagreeing with the file it produced.
 *
 * **UTC, not the host's zone.** A stored `Instant` renders the same wherever the report is read, and the
 * grinder's own logs are already UTC-stamped; a local zone would make the same verdict read as two different
 * days either side of midnight. **Zero-padded**, because the table sorts its columns as text: `2026/1/5`
 * would sort after `2026/11/…`.
 *
 * @author Griefed
 */
internal object ScanDate {

    /** The one format both outputs use. Fixed to UTC at construction, so no call site can pick a zone. */
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC)

    /** [instant] as `YEAR/MM/DD` in UTC. */
    fun of(instant: Instant): String = formatter.format(instant)
}
