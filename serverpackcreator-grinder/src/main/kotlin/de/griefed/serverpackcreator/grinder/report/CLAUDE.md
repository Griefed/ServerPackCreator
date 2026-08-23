<!-- Loads when Claude works with files in this package. Module-wide context (package map, the
cross-cutting landmines, remaining work) lives in serverpackcreator-grinder/CLAUDE.md. -->

# grinder.report — verdict persistence and the web/CSV output

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
