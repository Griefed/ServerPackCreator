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

/**
 * The human-readable face of `/status`: a page that polls the JSON endpoint and renders what the daemon is
 * doing right now — pass, workers and what each is holding, crawl position, cache size, rule health.
 *
 * `/status` stays exactly as it is. It is a monitoring endpoint that operators script against, so this is a
 * second route rather than content negotiation on the first: a machine reader that suddenly received HTML
 * because a browser-shaped `Accept` header leaked into it would be a silent breakage of the thing it is for.
 *
 * **No framework, and nothing fetched off the network.** Vanilla JS against the JDK's own HTTP server. The
 * report is documented as loopback-bound behind a reverse proxy, so a browser reaching it may have no route
 * to a CDN at all, and a page that needs one would be blank exactly where it is most needed.
 *
 * **The page is a constant, and that is a security property rather than a shortcut.** This server has no
 * authentication and the values on display include internet-supplied mod slugs. Because no data is
 * interpolated server-side, there is no escaping to get wrong: every value arrives as JSON and is written
 * with `textContent`, which cannot become markup. The one place a value reaches an attribute is a project
 * link, and `safeHref` refuses anything but http/https there so a crafted URL cannot smuggle in `javascript:`.
 *
 * @author Griefed
 */
object StatusDashboardRenderer {

    /**
     * Every field of the `/status` document this page reads, as dotted paths.
     *
     * It exists so the two sides can be cross-checked: `StatusDashboardTest` resolves each path against a
     * document a real [ReportServer] serves, which is the only way a rename can fail a build instead of
     * silently blanking a panel. Keep it in step when the page learns to read something new.
     */
    val READ_FIELDS: List<String> = listOf(
        "verdicts",
        "requeued",
        "activity.uptimeSeconds",
        "activity.startedAt",
        "activity.pass",
        "activity.passCandidates",
        "activity.passRunningSeconds",
        "activity.verified",
        "activity.failed",
        "activity.skippedFresh",
        "activity.workers",
        "bootRules.source",
        "bootRules.ruleCount",
        "bootRules.undecidedVerdict",
        "bootRules.errors",
        "crawl",
        "loaderCache.installedTuples",
        "loaderCache.path"
    )

    /** Default seconds between polls. Five is live enough to watch a worker change, cheap enough to leave open. */
    private const val DEFAULT_INTERVAL_SECONDS = 5

    /**
     * The complete page. Constant by design — see the class doc; nothing here interpolates request or store
     * data, so there is no escaping in it and no way for a mod slug to reach the markup.
     */
    fun toHtml(): String = PAGE

    private val PAGE = """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <title>ServerPackCreator — grinder status</title>
          <link rel="icon" type="image/png" href="/favicon.png">
          <style>
            body { font-family: system-ui, sans-serif; margin: 1.5rem; }
            h1 { margin: 0 0 1rem; font-size: 1.4rem; }
            h2 { font-size: 1rem; margin: 1.5rem 0 .5rem; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border: 1px solid #ccc; padding: 4px 8px; text-align: left; vertical-align: top; }
            th { background: #f3f3f3; }
            tr:nth-child(even) td { background: #fafafa; }
            .toolbar { display: flex; flex-wrap: wrap; gap: .5rem; align-items: center; margin-bottom: 1rem; }
            .toolbar button, .toolbar .btn, .toolbar select {
              font: inherit; padding: .35rem .75rem; border: 1px solid #bbb; border-radius: 4px;
              background: #f3f3f3; color: inherit; text-decoration: none; cursor: pointer;
            }
            .toolbar button:hover, .toolbar .btn:hover { background: #e6e6e6; }
            .cards { display: flex; flex-wrap: wrap; gap: .75rem; }
            .card { border: 1px solid #ccc; border-radius: 6px; padding: .6rem .9rem; min-width: 9rem; }
            .card .k { font-size: .75rem; color: #666; text-transform: uppercase; letter-spacing: .04em; }
            .card .v { font-size: 1.35rem; font-variant-numeric: tabular-nums; }
            .muted { color: #666; }
            .warn { color: #a40000; font-weight: 600; }
            #feed { margin-left: auto; font-size: .85rem; }
            .dot { display: inline-block; width: .55rem; height: .55rem; border-radius: 50%; margin-right: .35rem; }
            .ok  { background: #2e7d32; }
            .bad { background: #a40000; }
          </style>
        </head>
        <body>
          <h1>Grinder status</h1>
          <nav class="toolbar">
            <a class="btn" href="/">Verdict table</a>
            <a class="btn" href="/status">Raw JSON</a>
            <a class="btn" href="/boot-logs">Boot logs</a>
            <a class="btn" href="/as-properties">Fallback list</a>
            <button type="button" id="toggle">Pause</button>
            <select id="interval" title="Poll interval">
              <option value="2">every 2s</option>
              <option value="5" selected>every 5s</option>
              <option value="15">every 15s</option>
              <option value="60">every 60s</option>
            </select>
            <span id="feed"><span class="dot" id="dot"></span><span id="feedText">connecting…</span></span>
          </nav>

          <div class="cards" id="cards"></div>

          <h2>Workers</h2>
          <table><thead><tr>
            <th>Worker</th><th>Platform</th><th>Project</th><th>Busy</th>
          </tr></thead><tbody id="workers"></tbody></table>

          <h2>Crawl</h2>
          <table><thead><tr>
            <th>Platform</th><th>Offset</th><th>Sweeps</th><th>Partition</th>
          </tr></thead><tbody id="crawl"></tbody></table>

          <h2>Boot rules</h2>
          <div id="rules" class="muted"></div>

          <h2>Loader cache</h2>
          <div id="cache" class="muted"></div>

        <script>
        (function () {
          "use strict";

          // Human-readable durations: an operator reading "busy 4h 12m" learns something that "15134" hides.
          // Seconds are dropped past an hour on purpose -- at that scale they are noise, not information.
          function duration(totalSeconds) {
            if (totalSeconds === null || totalSeconds === undefined) return "—";
            var s = Math.max(0, Math.floor(totalSeconds));
            var d = Math.floor(s / 86400), h = Math.floor((s % 86400) / 3600);
            var m = Math.floor((s % 3600) / 60), sec = s % 60;
            if (d > 0) return d + "d " + h + "h " + m + "m";
            if (h > 0) return h + "h " + m + "m";
            if (m > 0) return m + "m " + sec + "s";
            return sec + "s";
          }

          function text(value) {
            return (value === null || value === undefined || value === "") ? "—" : String(value);
          }

          // The only value that reaches an attribute rather than a text node, so it is the only one that
          // needs checking: anything but an absolute http/https URL is dropped rather than linked.
          //
          // Deliberately parsed with NO base. Resolving against window.location.origin turns every
          // unparseable value into a same-origin link -- a null projectUrl became "<report>/null" -- so a
          // row would render something that looks like a project link and leads to a 404 on this server.
          // A platform's projectUrl is always absolute, so anything relative is bad data, not a link.
          function safeHref(url) {
            try {
              var parsed = new URL(url);
              return (parsed.protocol === "http:" || parsed.protocol === "https:") ? parsed.href : null;
            } catch (e) { return null; }
          }

          function cell(row, value) {
            var td = document.createElement("td");
            td.textContent = text(value);
            row.appendChild(td);
            return td;
          }

          function card(label, value) {
            var box = document.createElement("div");
            box.className = "card";
            var k = document.createElement("div");
            k.className = "k";
            k.textContent = label;
            var v = document.createElement("div");
            v.className = "v";
            v.textContent = text(value);
            box.appendChild(k);
            box.appendChild(v);
            return box;
          }

          function renderCards(doc) {
            var activity = doc.activity || {};
            var cards = document.getElementById("cards");
            cards.replaceChildren(
              card("Verdicts", doc.verdicts),
              card("Re-grind queue", doc.requeued),
              card("Pass", text(activity.pass) + " (" + text(activity.passCandidates) + " candidates)"),
              card("Pass running", duration(activity.passRunningSeconds)),
              card("Verified", activity.verified),
              card("Failed", activity.failed),
              card("Skipped (fresh)", activity.skippedFresh),
              card("Uptime", duration(activity.uptimeSeconds))
            );
            var started = document.createElement("div");
            started.className = "card";
            started.style.minWidth = "14rem";
            var sk = document.createElement("div");
            sk.className = "k";
            sk.textContent = "Started at";
            var sv = document.createElement("div");
            sv.textContent = text(activity.startedAt);
            started.appendChild(sk);
            started.appendChild(sv);
            cards.appendChild(started);
          }

          function renderWorkers(doc) {
            var body = document.getElementById("workers");
            var workers = (doc.activity && doc.activity.workers) || [];
            body.replaceChildren();
            if (workers.length === 0) {
              var idle = document.createElement("tr");
              var td = document.createElement("td");
              td.colSpan = 4;
              td.className = "muted";
              td.textContent = "No worker is holding a candidate right now.";
              idle.appendChild(td);
              body.appendChild(idle);
              return;
            }
            workers.forEach(function (w) {
              var row = document.createElement("tr");
              cell(row, w.worker);
              cell(row, w.platform);
              var project = cell(row, w.slug);
              var href = safeHref(w.projectUrl);
              if (href) {
                var link = document.createElement("a");
                link.href = href;
                link.rel = "noopener noreferrer";
                link.textContent = text(w.slug);
                project.replaceChildren(link);
              }
              cell(row, duration(w.busySeconds));
              body.appendChild(row);
            });
          }

          function renderCrawl(doc) {
            var body = document.getElementById("crawl");
            var crawl = doc.crawl || {};
            body.replaceChildren();
            Object.keys(crawl).forEach(function (platform) {
              var entry = crawl[platform] || {};
              var row = document.createElement("tr");
              cell(row, platform);
              cell(row, entry.offset);
              cell(row, entry.sweeps);
              cell(row, entry.partition);
              body.appendChild(row);
            });
          }

          function renderRules(doc) {
            var target = document.getElementById("rules");
            var rules = doc.bootRules;
            target.replaceChildren();
            if (!rules) {
              target.textContent = "No rule file is configured.";
              return;
            }
            var line = document.createElement("div");
            line.textContent = text(rules.ruleCount) + " rule(s) from " + text(rules.source) +
              " · undecided → " + text(rules.undecidedVerdict);
            target.appendChild(line);
            // Errors are why this panel exists: a broken file keeps the last good rule set, so a typo
            // disables an operator's rules in complete silence unless something says so out loud.
            var errors = rules.errors || [];
            if (errors.length > 0) {
              var heading = document.createElement("div");
              heading.className = "warn";
              heading.textContent = errors.length + " rule error(s) — the last good set is still in force:";
              target.appendChild(heading);
              var list = document.createElement("ul");
              errors.forEach(function (message) {
                var item = document.createElement("li");
                item.textContent = String(message);
                list.appendChild(item);
              });
              target.appendChild(list);
            }
          }

          function renderCache(doc) {
            var target = document.getElementById("cache");
            var cache = doc.loaderCache;
            target.textContent = cache
              ? text(cache.installedTuples) + " installed tuple(s) in " + text(cache.path)
              : "No loader cache is wired.";
          }

          function feed(ok, message) {
            document.getElementById("dot").className = "dot " + (ok ? "ok" : "bad");
            document.getElementById("feedText").textContent = message;
          }

          var timer = null;
          var running = true;

          function poll() {
            fetch("/status", { cache: "no-store" })
              .then(function (response) {
                if (!response.ok) throw new Error("HTTP " + response.status);
                return response.json();
              })
              .then(function (doc) {
                renderCards(doc);
                renderWorkers(doc);
                renderCrawl(doc);
                renderRules(doc);
                renderCache(doc);
                feed(true, "updated " + new Date().toLocaleTimeString());
              })
              .catch(function (error) {
                // Say so rather than freezing on stale numbers: a dashboard that silently keeps showing the
                // last good poll is worse than none when the daemon is the thing that stopped.
                feed(false, "unreachable (" + error.message + ") — showing the last successful poll");
              });
          }

          function schedule() {
            if (timer !== null) clearInterval(timer);
            var seconds = parseInt(document.getElementById("interval").value, 10) || $DEFAULT_INTERVAL_SECONDS;
            timer = setInterval(function () { if (running) poll(); }, seconds * 1000);
          }

          document.getElementById("interval").addEventListener("change", schedule);
          document.getElementById("toggle").addEventListener("click", function () {
            running = !running;
            this.textContent = running ? "Pause" : "Resume";
            if (running) poll();
          });

          poll();
          schedule();
        })();
        </script>
        </body>
        </html>
    """.trimIndent()
}
