<!-- Loads when Claude works with files in this package. Module-wide context (package map, the
cross-cutting landmines, remaining work) lives in serverpackcreator-grinder/CLAUDE.md. -->

# grinder.report — verdict persistence and the web/CSV output

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
  `VerdictReportRenderer` renders a **self-contained** HTML page — click-to-sort columns, an
  embedded-CSV download button, HTML-escaped cells **and** `\uXXXX`-escaped CSV-in-`<script>` so a
  mod-supplied `</script>` can't break out. `ReportServer` serves the table (`/`) and CSV
  (`/export.csv`) live off the store via the **JDK's built-in `com.sun.net.httpserver.HttpServer`** —
  **no Spring, no new dependency**. *Deliberately standalone:* the report is self-contained rather than
  rendered through the app's Quasar frontend, because the grinder must not depend on `-app` (that would
  drag in Spring/Mongo/Swing and break its standalone nature).
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
