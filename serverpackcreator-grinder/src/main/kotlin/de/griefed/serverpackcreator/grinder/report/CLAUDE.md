<!-- Loads when Claude works with files in this package. Module-wide context (package map, the
cross-cutting landmines, remaining work) lives in serverpackcreator-grinder/CLAUDE.md. -->

# grinder.report — verdict persistence and the web/CSV output

- **`/as-properties` publishes only a crash that is *decisive evidence*, not every `HIGH`.**
  `FallbackPropertiesRenderer.decisive` reads `GrindVerdict.decidedBy` and admits exactly two decisions:
  `CLIENT_ONLY_CLASS` and `OPERATOR_RULE`. `HIGH` alone was never enough — `CRASHED` is reachable both from
  the client-only-class marker, which no broken harness can fabricate, and from the bare exit-code rung, which
  means only *"exited non-zero and nothing recognised why"* — and the two were indistinguishable afterwards.
  - **Measured against the live daemon on 2026-08-31: 27 of 43 published `HIGH` verdicts rested on no
    decisive evidence** (`EXIT_CODE` 12, `DEPENDENCY_FAILURE` 8, `MIXIN_APPLY_FAILURE` 4, one each of
    `RUNTIME_MISMATCH`, `LAUNCH_FAILURE`, `LOADER_BOOTSTRAP_FAILURE`). `created_ltab-` was in the served list
    off a mixin refmap mismatch and a missing Fabric API.
  - **A legacy verdict reads `null` and does NOT publish.** That deliberately empties the grinder's
    contribution until a sweep re-grinds — an empty contribution beats a wrong one, and grandfathering the old
    rows in would keep exactly the entries the gate exists to remove.
  - **The gate is conservative, not precise, and the numbers say so.** Among the 27 were `sodium-extra` and
    `reeses-sodium-options`, which genuinely *are* client-only but crashed without decisive evidence. The
    recovery path is a rule: a signature verified by hand counts as `OPERATOR_RULE`. Measured, the shipped
    LWJGL rule recovered exactly 3, taking 27 → 24 — so expect a rule to buy single digits, not tens.
  - `VerdictField.DECISION` exposes it as a filterable `CHOICE` column, so `?f.decision=EXIT_CODE` lists the
    whole undefensible population. `GrinderAuditIT` grades it in bulk; details in the module `CLAUDE.md`.

- **`VerdictField` is the single declaration of a column** — header, CSV header, URL token, filter kind and
  cell text in one enum, consumed by the HTML headers, the HTML cells, the CSV header and the CSV rows.
  Those four were hand-synced and *had* drifted (CSV seven fields against the table's eight). Adding a
  column now means one entry. It also carries `sortKey`, defaulting to `text` and overridden **only** by
  `CONFIDENCE`: that column's text is an enum name, so sorting it as text ran alphabetically and put
  `INCONCLUSIVE` above `MEDIUM` and `LOW` — shipped and observed live before it was fixed. The rank lives
  once, as `VerdictField.CONFIDENCE_RANK`; the report's default order and `VerdictCsvExporter` both used to
  declare their own copy, so the table and the export could drift on what "highest confidence first" means.
  `theCsvDefaultOrderIsTheSameOrdering` fails if they ever do. **Logs is deliberately not a `VerdictField`**: it is not derivable from a
  `GrindVerdict` — it is a listing of files on disk — so it carries no `text` lambda and stays exempt from
  filtering, searching and the CSV.
- **Logs *is* sortable, and the sort key is therefore its own type.** `SortKey` is a sealed interface over
  `Column(VerdictField)` and `Logs`, because the sortable columns and the verdict-derived ones are not the
  same set. Sorting it is far from meaningless — as an earlier version of this file claimed — because **not
  every entry has logs**: artifacts are kept only for boots that did not survive, and the reaper drops the
  oldest once the budget is passed, so `?sort=logs&dir=desc` is how a maintainer finds the rows with
  anything to read. `VerdictSelection.select` takes an optional log-count lookup defaulting to "nothing has
  logs", so the CSV and every other-column test need no directory listing.
  - **The count and the links must come from one snapshot.** `ReportServer.logNamesFor` is shared by the
    sort and the cell for that reason; two separate lookups is how a row sorts as having logs and then
    renders an em-dash. `ReportServerTest.sortsTheTableByHowManyLogsEachRowHas` drives the real handler
    against a real store, because that wiring is not observable from the pure unit.
  - **`SortKey.Column`'s property is `column`, never `field`.** Inside a property getter `field` is the
    backing-field keyword, so `field.param` binds to a backing field the property does not have — reported
    as "Property must be initialized", naming neither the cause nor the collision.
  - The Logs tie-break runs by slug in **both** directions, unlike the field sorts which reverse their whole
    comparator: reversing it would reshuffle every log-less row when a reader merely flips the arrow, and
    those rows are the majority.
- **Filtering, sorting and paging are a pure unit** (`QueryParams` → `VerdictQuery` → `VerdictSelection` →
  `VerdictPage`), not handler code. Every edge case is three lines here instead of a socket round trip
  against a 60 KB blob — and it is what makes `/` and `/export.csv` *provably* agree: they share the
  function rather than being kept in step by hand.
  - **Nothing a URL can carry may throw.** Page 0, `size=banana`, a `sort=` naming a dropped column, a
    malformed escape — all fall back. These arrive from bookmarks and address bars, and a report that 500s
    is worse than one that quietly recovers.
  - **`offeredSizes` always includes the size in force**, even when the result count would not justify it.
    Filter a large store to a handful while a big size is set and a purely count-derived list leaves the
    `<select>` with no matching option — the browser then shows its first, and the page silently disagrees
    with its own URL. Sizes at or above the count are omitted as duplicates of `all`, not as "too big".
  - **LANDMINE — bind `size` outside `buildList`.** Its `MutableList` receiver's own `size` shadows the
    property, so `toQueryString` compared the *list length* and emitted it: `size=250` became `size=0`.
    Invisible by reading; caught by `theAppliedQueryRoundTrips`.
  - Measured on the real 875-row store: 20 filtered+sorted selections in **13 ms**, page bytes **110,703**
    at `size=250` against **369,873** for `size=all`. The cost was never the filtering — it is the HTML.
- **The filter bar needs no JavaScript.** CHOICE columns render a `<select>` of the values actually
  present, TEXT columns an `<input>`, inside one GET form; submitting *is* the URL update. Sorting is
  header links. The old DOM sort is gone — it was lost on reload and could not be shared.
- **Project sideness is rendered honestly**, never as a bare enum: `not recorded` when nobody asked,
  `not published by CurseForge` where the platform publishes none for *any* project. `UNKNOWN` for both
  would tell a reader the mod was checked and found server-safe.

- **`JsonVerdictStore` tolerates what it cannot read, and preserves it.** The mapper disables
  `FAIL_ON_UNKNOWN_PROPERTIES` and `load()` reads **row by row**, so a store written by a *newer* build stays
  readable after a downgrade and one unreadable verdict costs that verdict rather than all of them. Anything
  unread is copied to `<name>.unreadable-<epoch>` **before returning**, because `record()` persists the whole
  map on the very next call — the previous behaviour turned one unknown field into a wiped store while
  `aCorruptFileDegradesToEmpty` stayed green, since it pins "start empty", not "and then don't destroy it".
- **`BootLogStore` (was `CrashLogStore`) keeps every *attempt* of every boot that did not survive** — the
  container console, the server's own `logs/`, and its `crash-reports/`, collected by `BootArtifacts` in
  `-clientside` (which is also where the size cap and the never-read-a-huge-file-whole guarantee now live).
  - **Names are never parsed apart.** A stored name is `<tuple>~<attempt>~<artifact>.log`; both a slug and a
    loader contain `-`, so `namesFor` rebuilds the tuple prefix and filters on it. `~` is the separator
    precisely because no part uses it.
  - **`pruneExcept` is the bound that actually holds.** Naming an attempt after what it booted means a
    re-grind replaces the attempts it writes *again* — but a re-check sampling a different loader or
    Minecraft line writes new names, so the previous grind's files would live forever. `enforceBudget`
    (`SPC_GRINDER_BOOT_LOG_BUDGET_MIB`, default 2048) is the backstop behind it.
  - **LANDMINE — what a grind wrote is per *invocation*, never a field.** One `ContainerCandidateVerifier`
    serves every `GrindPool` worker, so a shared collection lets one candidate's prune delete logs another
    just wrote — the same cross-candidate class that had an unqualified attempt directory wiping a pack
    mid-boot. `candidatesPrunedInParallelDoNotDeleteEachOthersLogs` pins it.
  - `/boot-log?name=` and `/boot-logs` are the routes; `/crash-log(s)` remain as aliases because both are
    documented and an operator has them bookmarked. The untrusted-name handling is carried verbatim from the
    old store — do not retype it.

- **Persistence + web interface**: `JsonVerdictStore` (file-backed, loads on start, whole-file
  temp-then-atomic-move write, corrupt-file → empty) makes a multi-day run restart-safe.
  `VerdictReportRenderer` renders a **self-contained** HTML page with HTML-escaped cells. *(Superseded
  2026-08-29: it used to have click-to-sort JS, an embedded-CSV download button and `\uXXXX`-escaped
  CSV-in-`<script>` to stop a mod-supplied `</script>` breaking out. Sorting is server-side header links
  now, the button is a link to `/export.csv`, and the page no longer puts mod-supplied text inside a
  `<script>` block at all — so that escape hatch is gone rather than merely re-defended.)*
  `ReportServer` serves the table (`/`) and CSV (`/export.csv`) live off the store via the **JDK's built-in
  `com.sun.net.httpserver.HttpServer`** —
  **no Spring, no new dependency**. *Deliberately standalone:* the report is self-contained rather than
  rendered through the app's Quasar frontend, because the grinder must not depend on `-app` (that would
  drag in Spring/Mongo/Swing and break its standalone nature).
- **`/dashboard` is the human face of `/status`, and `/status` is unchanged.** `StatusDashboardRenderer`
  serves a hand-written page that polls the JSON endpoint and renders pass, workers, crawl, cache and rule
  health with human durations. A **second route rather than content negotiation**: `/status` is scripted
  against, and handing a machine reader HTML because an `Accept` header looked browser-shaped would break
  what it exists for. No framework and nothing off the network — the report is documented as loopback-bound
  behind a reverse proxy, so a browser reaching it may have no route to a CDN at all.
  - **The page is a constant, which is a security property rather than a shortcut.** Nothing is interpolated
    server-side, so there is no escaping to get wrong on a server that has no authentication and displays
    internet-supplied slugs; every value arrives as JSON and is written with `textContent`.
  - **LANDMINE — `READ_FIELDS` is what makes a rename fail a build.** `statusJson()` builds its document from
    **string-literal keys no compiler checks**, so renaming `"loaderCache"` compiles clean and silently blanks
    a panel. `everyFieldTheDashboardReadsExistsInTheStatusDocument` resolves each declared path against a
    document a real `ReportServer` serves. Verified to have teeth: with that key renamed it is the **only**
    failure in all 434 grinder tests. Kotlin *property* renames are already caught by `GrinderStatusTest` at
    compile time — this guard is for the untyped half. Keep it in step when the page reads something new.
  - **The script is executed, not grepped** (`StatusDashboardScriptTest`, node when present, skipped
    otherwise — the treatment `ScriptTemplateContentTest` gives the shipped shells). Nothing compiles a
    string constant, so a typo ships an inert dashboard. It earned itself immediately: `safeHref` parsed
    against `window.location.origin`, so a null or unparseable `projectUrl` resolved to a same-origin link
    (`<report>/null`) that looked like a project and 404'd. Parsed with **no base**, anything not an absolute
    http/https URL now yields no link at all.
- **The tab icon is bundled, and needs its own routes.** `favicon.png` (a byte-identical copy of
  `img/config.png`) ships in this package's resources and is served from the classpath, so the page still
  fetches nothing from outside itself. Registered under **both** `/favicon.ico` and `/favicon.png`: the HTML
  pages link the latter, browsers request the former unprompted, and without a context of its own either
  request falls through to the catch-all `/` and is answered with the verdict table as `text/html`. A missing
  resource logs a warning and 404s — a decoration must never 500 an endpoint.
- **The `Scanned` column comes from one shared `ScanDate`**, used by the HTML table *and* the CSV, because the
  download button hands out the exporter's own output and a divergence would show as the page disagreeing with
  its own file. `YEAR/MM/DD` in **UTC** (a stored `Instant` then reads the same on any host) and zero-padded
  (the table sorts columns as text, so `2026/1/5` would sort after `2026/11/…`). The data was always there —
  `GrindVerdict.verifiedAt` drives the re-verify TTL — it was simply never shown.
