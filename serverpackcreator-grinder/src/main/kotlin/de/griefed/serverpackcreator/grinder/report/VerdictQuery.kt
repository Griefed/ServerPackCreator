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

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.GrindVerdict
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** How a column is filtered: an exact choice from a short list, or a substring of free text. */
internal enum class FilterKind {
    /** Few distinct values, so the UI can offer them all and the match is exact. */
    CHOICE,

    /** Free text, matched case-insensitively as a substring — which is what makes a search box useful. */
    TEXT
}

/**
 * **The single declaration of a report column**, consumed by the HTML headers, the HTML cells, the CSV
 * header and the CSV rows.
 *
 * Those four were hand-synchronised, and drifted: the CSV carried seven fields while the table carried
 * eight for a long time, and `everyHeaderHasACellBeneathIt` had to exist because adding a header without
 * its cell still rendered — shifting every column past the gap onto its neighbour's data. One list makes
 * that impossible rather than merely tested for.
 *
 * The **Logs** column is deliberately not here: it is not derivable from a [GrindVerdict], it is exempt
 * from filtering, and sorting it is meaningless.
 *
 * @author Griefed
 */
internal enum class VerdictField(
    /** The `<th>` text. */
    val header: String,
    /** The CSV column name, which differs in spelling from [header] for historical reasons. */
    val csvHeader: String,
    /** The token this column is addressed by in a URL (`sort=`, `f.<param>=`). */
    val param: String,
    /** How this column filters. */
    val filter: FilterKind,
    /** The cell's plain text — also what filtering and searching match against. */
    val text: (GrindVerdict) -> String
) {
    NAME("Name", "Name", "name", FilterKind.TEXT, { it.slug }),
    PROJECT("Project", "Project", "project", FilterKind.TEXT, { it.projectUrl }),
    PATTERN("Name-pattern", "NamePattern", "pattern", FilterKind.TEXT, { it.suggestedEntry ?: "" }),
    CONFIDENCE("Confidence", "Confidence", "confidence", FilterKind.CHOICE, { it.confidence.name }),
    LOADER("Loader", "Loader", "loader", FilterKind.CHOICE, { it.loader }),
    PLATFORM("Platform", "Platform", "platform", FilterKind.CHOICE, { it.platform }),
    PROJECT_SIDENESS(
        "Project sideness", "ProjectSideness", "project-sideness", FilterKind.CHOICE,
        { verdict ->
            // Rendered honestly rather than as a bare enum: CurseForge publishes no sideness for ANY
            // project, and a null means nobody ever asked. "UNKNOWN" for both would tell a reader we
            // checked and found it server-safe.
            when {
                verdict.declaredClientSide == null && verdict.declaredServerSide == null -> "not recorded"
                verdict.platform == "CurseForge" -> "not published by CurseForge"
                else -> "${verdict.declaredClientSide} / ${verdict.declaredServerSide}"
            }
        }
    ),
    JAR_SIDENESS("Jar sideness", "JarSideness", "jar-sideness", FilterKind.CHOICE, { it.jarScan?.name ?: "not recorded" }),
    DETAIL("Detail", "Detail", "detail", FilterKind.TEXT, { it.detail }),
    RULE("Rule", "Rule", "rule", FilterKind.CHOICE, { it.firedRule ?: "" }),
    DEPENDENCIES("Dependencies", "Dependencies", "dependencies", FilterKind.TEXT, { it.stagedDependencies.joinToString(", ") }),
    SCANNED("Scanned (UTC)", "Scanned", "scanned", FilterKind.TEXT, { ScanDate.of(it.verifiedAt) });

    companion object {
        /** The column addressed by [param], or `null` — an unknown one is ignored rather than fatal. */
        fun byParam(param: String?): VerdictField? = entries.firstOrNull { it.param == param }
    }
}

/**
 * A parsed query string as a multi-map. Hand-rolled because the JDK offers nothing for this and the report
 * takes no dependencies; tolerant because every value here arrives from a bookmark or an address bar.
 *
 * @author Griefed
 */
internal object QueryParams {

    /** Percent-decoded, multi-value. A pair whose escape will not decode is dropped rather than thrown. */
    fun parse(rawQuery: String?): Map<String, List<String>> =
        rawQuery.orEmpty().split('&')
            .filter { it.isNotBlank() }
            .mapNotNull { pair ->
                val key = decode(pair.substringBefore('=')) ?: return@mapNotNull null
                val value = decode(pair.substringAfter('=', "")) ?: return@mapNotNull null
                key.takeIf { it.isNotBlank() }?.let { it to value }
            }
            .groupBy({ it.first }, { it.second })

    /** One value for [key], or `null` when absent or blank — an empty control reads as absent. */
    fun first(params: Map<String, List<String>>, key: String): String? =
        params[key]?.firstOrNull()?.takeIf { it.isNotBlank() }

    private fun decode(value: String): String? =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8) }.getOrNull()
}

/**
 * What the reader asked for: a search, per-column filters, a sort, and where in the results they are.
 *
 * Every field has a safe fallback, because these values arrive from a URL somebody may have typed, edited
 * or bookmarked before a column existed. Nothing here throws.
 *
 * @author Griefed
 */
internal data class VerdictQuery(
    /** Free text matched against every column, so a reader need not know which one holds their term. */
    val search: String? = null,
    /** Per-column filters. OR within a column, AND across columns. */
    val filters: Map<VerdictField, List<String>> = emptyMap(),
    /** The column to sort by, or `null` for the default confidence-then-slug-then-loader order. */
    val sort: VerdictField? = null,
    /** Whether [sort] runs backwards. */
    val descending: Boolean = false,
    /** 1-based page, clamped to the available range when applied. */
    val page: Int = 1,
    /** Rows per page, or `null` for "all" — a real choice rather than a very large number. */
    val size: Int? = DEFAULT_PAGE_SIZE
) {
    /**
     * This query as a query string, defaults and empties omitted so a shared link stays readable.
     *
     * Empties are dropped because a GET form submits its empty controls: without this every link would
     * carry `?f.name=&f.detail=&q=` and the tail would grow with each column added.
     */
    fun toQueryString(): String {
        // Bound outside `buildList` on purpose. Inside it the receiver is a MutableList, whose own `size`
        // SHADOWS this property -- so `size != DEFAULT_PAGE_SIZE` silently compared the list's length and
        // emitted it as the value, turning size=250 into "size=0" and size=2 into "size=4". Caught by
        // theAppliedQueryRoundTrips; invisible by reading.
        val pageSize = size
        val parts = buildList {
            search?.let { add("q=" + encode(it)) }
            filters.forEach { (field, values) ->
                values.filter { it.isNotBlank() }.forEach { add("f.${field.param}=" + encode(it)) }
            }
            sort?.let { add("sort=" + it.param) }
            if (descending) add("dir=desc")
            if (page > 1) add("page=$page")
            if (pageSize != DEFAULT_PAGE_SIZE) add("size=" + (pageSize?.toString() ?: "all"))
        }
        return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)

    companion object {
        /** Rows per page when nothing says otherwise — small enough to render fast, large enough to scan. */
        const val DEFAULT_PAGE_SIZE = 250

        /** The ladder a reader may choose from, smallest first. `null` ("all") is always appended. */
        val OFFERED_SIZES = listOf(100, 250, 500, 1_000, 2_000, 5_000, 10_000, 20_000, 50_000, 100_000)

        /**
         * Read a query, falling back rather than failing on anything unreadable.
         *
         * [defaultSize] differs per endpoint on purpose: the table pages by default, while `/export.csv`
         * with no query must keep exporting everything, because it is a documented endpoint operators
         * script against.
         */
        fun parse(params: Map<String, List<String>>, defaultSize: Int?): VerdictQuery {
            val filters = VerdictField.entries.mapNotNull { field ->
                params["f.${field.param}"]?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
                    ?.let { field to it }
            }.toMap()
            val declaredSize = QueryParams.first(params, "size")
            return VerdictQuery(
                search = QueryParams.first(params, "q"),
                filters = filters,
                sort = VerdictField.byParam(QueryParams.first(params, "sort")),
                descending = QueryParams.first(params, "dir").equals("desc", ignoreCase = true),
                page = QueryParams.first(params, "page")?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                size = when {
                    declaredSize == null -> defaultSize
                    declaredSize.equals("all", ignoreCase = true) -> null
                    else -> declaredSize.toIntOrNull()?.takeIf { it > 0 } ?: defaultSize
                }
            )
        }

        /**
         * The page sizes worth offering for [matched] rows, always including `all` (as `null`) and always
         * including [current].
         *
         * Offering 100,000 for 2,000 rows is noise. But **[current] must be in the list whatever the count
         * says**: filter a large store down to a handful while a big size is in force and a strictly
         * count-derived list would not contain it, so the control would show its first option while the URL
         * said another — the page silently disagreeing with itself.
         */
        fun offeredSizes(matched: Int, current: Int?): List<Int?> {
            val justified = OFFERED_SIZES.filter { it < matched }
            val withCurrent = (justified + listOfNotNull(current)).distinct().sorted()
            return withCurrent + listOf(null)
        }
    }
}

/**
 * One page of results, together with everything a renderer needs to describe it: the counts before and
 * after filtering, and the query **as applied** so every link on the page is built from what actually
 * happened rather than from what was asked for.
 *
 * @author Griefed
 */
internal data class VerdictPage(
    /** This page's rows, already filtered, sorted and sliced. */
    val rows: List<GrindVerdict>,
    /** Rows in the store, before filtering. */
    val total: Int,
    /** Rows after filtering and searching. */
    val matched: Int,
    /** The page actually served, after clamping. */
    val page: Int,
    /** How many pages the current size yields; at least 1, so "page 1 of 1" reads correctly when empty. */
    val pages: Int,
    /** The query as applied — clamped page included, so links cannot propagate an impossible one. */
    val query: VerdictQuery,
    /** The distinct values of each CHOICE column over the **whole** store, for the filter controls. */
    val choices: Map<VerdictField, List<String>>
)

/**
 * Filters, sorts and slices verdicts. Pure: no store, no server, no clock.
 *
 * @author Griefed
 */
internal object VerdictSelection {

    /** Confidence order for the default sort — highest first, since that is what a reader came for. */
    private val confidenceRank = mapOf(
        Confidence.HIGH to 0, Confidence.MEDIUM to 1, Confidence.LOW to 2, Confidence.INCONCLUSIVE to 3
    )

    /** Apply [query] to [verdicts]. */
    fun select(verdicts: List<GrindVerdict>, query: VerdictQuery): VerdictPage {
        val matched = verdicts.filter { verdict -> matchesFilters(verdict, query) && matchesSearch(verdict, query) }
        val ordered = order(matched, query)

        val size = query.size ?: matched.size.coerceAtLeast(1)
        val pages = if (matched.isEmpty()) 1 else ((matched.size + size - 1) / size)
        val page = query.page.coerceIn(1, pages)
        val rows = ordered.drop((page - 1) * size).take(size)

        return VerdictPage(
            rows = rows,
            total = verdicts.size,
            matched = matched.size,
            page = page,
            pages = pages,
            query = query.copy(page = page),
            choices = VerdictField.entries.filter { it.filter == FilterKind.CHOICE }.associateWith { field ->
                verdicts.map { field.text(it) }.filter { it.isNotBlank() }.distinct().sorted()
            }
        )
    }

    /** OR within a column, AND across columns — the shape a reader expects from a filter bar. */
    private fun matchesFilters(verdict: GrindVerdict, query: VerdictQuery): Boolean =
        query.filters.all { (field, values) ->
            val cell = field.text(verdict)
            values.any { value ->
                when (field.filter) {
                    FilterKind.CHOICE -> cell.equals(value, ignoreCase = true)
                    FilterKind.TEXT -> cell.contains(value, ignoreCase = true)
                }
            }
        }

    private fun matchesSearch(verdict: GrindVerdict, query: VerdictQuery): Boolean {
        val term = query.search ?: return true
        return VerdictField.entries.any { it.text(verdict).contains(term, ignoreCase = true) }
    }

    /**
     * The default order is the one the report has always used — highest confidence, then slug, then loader
     * — because that is what a reader scanning for findings wants. A named sort replaces it entirely.
     */
    private fun order(matched: List<GrindVerdict>, query: VerdictQuery): List<GrindVerdict> {
        val sort = query.sort
            ?: return matched.sortedWith(compareBy({ confidenceRank[it.confidence] ?: 99 }, { it.slug }, { it.loader }))
        val byField = compareBy<GrindVerdict> { sort.text(it).lowercase() }
        return matched.sortedWith(if (query.descending) byField.reversed() else byField)
    }
}
