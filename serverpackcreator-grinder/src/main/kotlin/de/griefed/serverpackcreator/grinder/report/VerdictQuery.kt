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

import de.griefed.serverpackcreator.clientside.Verdict
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
 * The **Logs** column is deliberately not here: it is not derivable from a [GrindVerdict], so it cannot
 * carry a [text] lambda and stays exempt from filtering, searching and the CSV. It *is* sortable — see
 * [SortKey.Logs], which is why the sort key is its own type rather than a [VerdictField].
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
    val text: (GrindVerdict) -> String,
    /**
     * What this column *orders* by, defaulting to [text].
     *
     * Overridden only by [CONFIDENCE], whose cell text is an enum name: sorted as text it runs
     * alphabetically, and alphabetically `INCONCLUSIVE` — which means nothing was learned — outranks both
     * `MEDIUM` and `LOW`. Observed live before this existed. Keeping it here rather than in the sorter is
     * also what collapses the rank table onto one declaration, instead of one copy per layer.
     */
    val sortKey: (GrindVerdict) -> String = text
) {
    NAME("Name", "Name", "name", FilterKind.TEXT, { it.slug }),
    PROJECT("Project", "Project", "project", FilterKind.TEXT, { it.projectUrl }),
    PATTERN("Name-pattern", "NamePattern", "pattern", FilterKind.TEXT, { it.suggestedEntry ?: "" }),
    // Beside the pattern it narrows, never instead of it: PATTERN is what `/as-properties` publishes and
    // has to match every build ever released, while this names the one artifact that was sampled. Blank
    // when nothing was sampled -- repeating the broad stem here would imply a file was examined.
    FILENAME("Filename", "Filename", "filename", FilterKind.TEXT, { it.fileName ?: "" }),
    VERDICT(
        "Verdict", "Verdict", "verdict", FilterKind.CHOICE, { it.verdict.name },
        // Zero-padded so the rank sorts as text alongside every other column, without the sorter needing
        // to know this one is numeric. One digit is plenty and the padding keeps it honest past nine.
        sortKey = { "%02d".format(VERDICT_RANK[it.verdict] ?: 99) }
    ),
    DECLARED(
        "Declared", "Declared", "declared", FilterKind.CHOICE,
        // Blank, never "UNKNOWN" or "null": every CurseForge project declares nothing at all, and a word
        // here would tell a reader we asked and were told rather than that nobody ever said.
        { it.declared?.name ?: "" }
    ),
    // The row's identity, so it sits beside the loader that produced its evidence rather than instead of
    // it. A CHOICE, because a reader's question is "what does this mod do on 1.12?" -- a small, closed set
    // per catalogue -- and ordered numerically, or `1.9` would outrank `1.20` in the table exactly as it
    // did in the selector before `minecraftComparator` existed.
    MINECRAFT(
        "Minecraft", "Minecraft", "minecraft", FilterKind.CHOICE, { it.minecraftLine ?: "" },
        sortKey = { verdict ->
            verdict.minecraftLine.orEmpty().split('.').joinToString(".") { "%04d".format(it.toIntOrNull() ?: 0) }
        }
    ),
    // The exact build behind the line, beside it for the same reason FILENAME sits beside PATTERN: one is
    // the row's identity, the other is what a maintainer reproduces the boot with.
    MINECRAFT_VERSION(
        "Version", "MinecraftVersion", "minecraft-version", FilterKind.TEXT, { it.minecraftVersion ?: "" }
    ),
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

    /**
     * Which classifier rung decided the boot. A **CHOICE** column on purpose: filtering the table to
     * `decision=EXIT_CODE` is how a maintainer finds every verdict reached because a process exited non-zero
     * and nothing recognised why — the population that produced the false positives of 2026-08-31, and the
     * one the publication gate now refuses to publish.
     */
    DECISION("Decision", "Decision", "decision", FilterKind.CHOICE, { it.decidedBy ?: "" }),
    // Beside the decision, never instead of it: DECISION is what *this* row's own boot did, and this is the
    // sibling whose proof it inherited. A row showing `READY_LINE` here and a loader there is a clean boot
    // excluded because another build of the same mod reached client-only code -- which is the one shape a
    // reader cannot otherwise tell from a published CONFIRMED resting on nothing.
    PROOF(
        "Inherited proof", "InheritedProof", "proof", FilterKind.CHOICE,
        { verdict ->
            verdict.inheritedProofFrom?.let { from ->
                verdict.inheritedProofRule?.let { rule -> "$from ($rule)" } ?: from
            } ?: ""
        }
    ),
    DEPENDENCIES("Dependencies", "Dependencies", "dependencies", FilterKind.TEXT, { it.stagedDependencies.joinToString(", ") }),
    SCANNED("Scanned (UTC)", "Scanned", "scanned", FilterKind.TEXT, { ScanDate.of(it.verifiedAt) });

    companion object {
        /**
         * Verdict ordering, the findings first — **the** rank table.
         *
         * Both the report's default order and `/export.csv`'s hand-maintained copy used to declare this
         * separately, so the table and the export could drift into disagreeing about what "highest
         * confidence first" means. Pinned by `theCsvDefaultOrderIsTheSameOrdering`.
         */
        val VERDICT_RANK = mapOf(
            // What a maintainer came for, in order: the findings; then the consoles a new rule gets written
            // from; then the host's own problems, which are the ones somebody can act on; then the two that
            // nobody can -- a distribution opt-out first, since it names a project and a file where the
            // other names an absence; then the rows with nothing left to do.
            Verdict.CONFIRMED to 0,
            Verdict.INCONCLUSIVE to 1,
            Verdict.ERROR to 2,
            Verdict.LOCKED to 3,
            Verdict.UNVERIFIABLE to 4,
            Verdict.CLEAR to 5
        )

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
 * A column the table can be ordered by.
 *
 * Not simply a [VerdictField], because the sortable columns and the verdict-derived ones are not the same
 * set: **Logs** is rendered from a directory listing rather than from the verdict, so it can be sorted but
 * never filtered, searched or exported. Modelling that as a type keeps the difference in the compiler
 * instead of in a comment — a [VerdictField] cannot be asked for a log count, and [Logs] cannot be asked
 * for cell text.
 *
 * @author Griefed
 */
internal sealed interface SortKey {
    /** The token this key is addressed by in a URL (`sort=`). */
    val param: String

    /**
     * Order by one of the verdict's own columns, using its [VerdictField.text].
     *
     * The property is `column` and **must not** be called `field`: inside a property getter `field` is the
     * backing-field keyword, so `field.param` binds to a backing field this property does not have — which
     * the compiler reports as "Property must be initialized", naming neither the cause nor the collision.
     */
    data class Column(val column: VerdictField) : SortKey {
        override val param: String get() = column.param
    }

    /** Order by how many kept artifacts a row has. The count comes from the caller, not the verdict. */
    data object Logs : SortKey {
        override val param: String get() = PARAM

        /** The URL token and the `<th>` text, shared so the link and the header cannot drift apart. */
        const val PARAM = "logs"
        const val HEADER = "Logs"
    }

    companion object {
        /** The key named by [param], or `null` — an unknown one is ignored rather than fatal. */
        fun byParam(param: String?): SortKey? = when {
            param == null -> null
            param.equals(Logs.PARAM, ignoreCase = true) -> Logs
            else -> VerdictField.byParam(param)?.let(::Column)
        }
    }
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
    val sort: SortKey? = null,
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
                sort = SortKey.byParam(QueryParams.first(params, "sort")),
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

    /**
     * Apply [query] to [verdicts].
     *
     * [logCount] answers how many kept artifacts a verdict has, and is only ever consulted for
     * [SortKey.Logs]. It defaults to "nothing has logs" so every caller that does not sort by them — the
     * CSV, and every test of the other columns — needs no directory listing at all. Pass the *same*
     * per-request snapshot the renderer uses; a lookup that lists the store per row would cost one listing
     * per verdict.
     */
    fun select(
        verdicts: List<GrindVerdict>,
        query: VerdictQuery,
        logCount: (GrindVerdict) -> Int = { 0 }
    ): VerdictPage {
        val matched = verdicts.filter { verdict -> matchesFilters(verdict, query) && matchesSearch(verdict, query) }
        val ordered = order(matched, query, logCount)

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
    private fun order(
        matched: List<GrindVerdict>,
        query: VerdictQuery,
        logCount: (GrindVerdict) -> Int
    ): List<GrindVerdict> = when (val sort = query.sort) {
        // The default order IS the verdict sort, expressed through the same key, so the two can never
        // disagree about what "the findings first" means.
        null -> matched.sortedWith(
            compareBy<GrindVerdict> { VerdictField.VERDICT.sortKey(it) }
                .thenBy { it.slug }
                // Newest era first inside a project, which is both the order the grind produces and the one
                // a reader wants: the line a pack is most likely being built on leads. The loader stays the
                // last tie-break, because a legacy row carries no line and two of them would otherwise be
                // ordered arbitrarily.
                .thenByDescending { VerdictField.MINECRAFT.sortKey(it) }
                .thenBy { it.loader }
        )

        // Only the COUNT is reversed, and the slug/loader tie-break is appended afterwards so it runs the
        // same way in both directions. Reversing the whole comparator, as the field sorts do, would reshuffle
        // every log-less row whenever a reader merely flipped the arrow -- and those rows are the majority,
        // because artifacts are kept only for boots that did not survive.
        is SortKey.Logs -> {
            val byCount = compareBy<GrindVerdict> { logCount(it) }
            matched.sortedWith(
                (if (query.descending) byCount.reversed() else byCount)
                    .thenBy { it.slug }
                    .thenByDescending { VerdictField.MINECRAFT.sortKey(it) }
                    .thenBy { it.loader }
            )
        }

        is SortKey.Column -> {
            val byField = compareBy<GrindVerdict> { sort.column.sortKey(it).lowercase() }
            matched.sortedWith(if (query.descending) byField.reversed() else byField)
        }
    }
}
