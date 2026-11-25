# Conventions history

The incidents, measurements and worked examples behind the rules in the root `CLAUDE.md`, moved here on 2026-11-25
so the always-loaded file states each rule and this file, read on demand, keeps its evidence. Each section is the
removed text, verbatim, under the rule it supported. Hashes are as they were cited; some predate history rewrites.

## Build & test commands: the app suite (derivable)

- `./gradlew :serverpackcreator-app:test` — app suite.

## Build & test commands: Kover (derivable)

- `./gradlew :<module>:koverHtmlReport` / `koverXmlReport` — coverage (Kover), report under
`<module>/build/reports/kover/`.

## Build & test commands: frontend (derivable from its package.json scripts)

- Frontend: `npm install && npx quasar dev` in `serverpackcreator-web-frontend/` (dev server),
`npx quasar build` for production build, `npm test` (Vitest).

## Avoiding premature optimization: the B30/B31 receipts

This project has the receipts: B30 was a real 121,492-byte
  saving per startup that bought **~0 ms**, because the twelve manifest checks run concurrently and the
  slowest one gated the batch. Measured, it was the wrong thing to optimise; the right one (B31, taking
  the refresh off the startup path) was ~392 ms and only visible once something was running.

## A published verdict must rest on decisive evidence: the 2026-08-31 measurement

Measured against the live daemon on 2026-08-31: **27 of 43 published
`HIGH` verdicts rested on no decisive evidence**, and one poisoned loader-cache entry (`NeoForge 21.11.45 /
MC 1.21.11`) had produced identical failures across all **90** boots against it — a library mod and a
server-side building mod among them.

## Question the requirement: the circuit breaker of 2026-09-02

On 2026-09-02 a circuit breaker
was designed, pinned with 189 lines of guards, implemented, wired through two modules and documented — and
deleted 34 minutes later, when Griefed asked whether the thing it protected was needed at all. It was not:
the route it bounded existed only to circumvent CurseForge's distribution opt-out, had stopped working
entirely, served under 1% of candidates, and cost 192.9 MB in every artifact. Every commit in that sequence
was correctly shaped, which is exactly why the shaping did not save it. The evidence to ask the prior
question was already in hand. This

## Cite names, not snapshots: the audits and the 2026-09-01 rebase

Three consecutive audits of the performance branches found the same
defect class and nothing else: a fact quoted in prose going stale the moment the code moved — 54 commit
hashes killed by a rebase, a landmine still describing a flaw that had been fixed, a line number shifted
by the very commit that cited it, and suite counts left behind by the tests that were just added. **It
recurred on 2026-09-01:** that rebase killed 13 more hashes in `claude-docs/REFACTOR-AUDIT.md`, five of
which are now reachable from no ref at all and will stop resolving entirely once gc runs. That file cites
hashes at volume, so it is the guaranteed casualty of every history rewrite — write subjects there the
first time.

## Fix bugs when you find them: where the removed bullets went

(The other two bullets that used to sit here — the principles list and
the git workflow — are stated in the header and in **## Branching & git workflow**.)

## One commit per goal: the CI-NOTES.md instance

Measured instance: `CI-NOTES.md` was written by one commit of a session and deleted by
  a later one in the same session, so the atomic history recorded a 156-line file arriving and
  leaving; grouped, it never existed.

## Shell templates, manifests and version parsing: the three audited instances

Two audits found three instances (`28a786b58` NeoForge mapping, no test;
`2e16bf0c8` fish `--no-empty`, test five commits later; `1a55797df` Fabric fall-through, whose guard
asserted *ordering* and stayed green while the behaviour was still broken). The cost is measurable:
the Forge launcher-era bug wasted **24 boots** before anything noticed, and 820 NeoForge versions were
mis-attributed to one Minecraft release.

## A performance branch is proven equivalent: iteration 7's result

Done for this branch
(`claude-docs/REFACTOR-AUDIT.md` iteration 7): **490 pre-existing guards, zero failures**, with exactly two files
uncompilable — one adapted by adding two constructor arguments and *no* assertion edits (7 guards green), one
legitimately unadaptable because it asserted behaviour the branch removed.

## What only a real runtime can answer: iteration 7 against MongoDB

Iteration 7 ran the actual `bootJar` in `-web` mode against MongoDB 8.0.5 in Docker, seeded with pre-branch
shaped documents, and that is what confirmed the `sha256` index really exists, the migration really converts
legacy documents and really skips already-migrated ones, and `/api/v2/runconfigs/all` really returns the
documented shape. It also surfaced what was filed at the time as B33 — the web application
  writing to MongoDB's default `test` database instead of the configured one, which no test could have caught, and
  which the Spring Boot 4 property-key fix has since closed. Cost: about fifteen minutes.

## Pin first means commit first: the 2026-07-31 audit

Audited 2026-07-31 — all **eight** code
commits of that day's plan (`2a9a03473`, `30f6cbded`, `c571e2d7f`, `07a647f01`, `aa2d27f7f`, `91ac0e1a9`,
`1f92f585c`, `5caa6833f`) bundled guard and change, so nobody can check out `2a9a03473^` and watch the pin
go red. The tests were written first; only the boundary collapsed, which is the part that costs nothing to
keep and everything to reconstruct later.

## Run the pin before you commit it red: the two 2026-09-01 cases

Twice on 2026-09-01 it was not: a fixture omitted a
collaborator, so four asserted field paths could not have resolved even against correct code; and a Kotlin
`${'$'}` escape produced the literal `...-v${'$'}version`, so the guard asserted a string no implementation
would ever return. Both were fixed in the very commit that was supposed to turn them green, which quietly
undoes the boundary the previous rule exists to create — `git checkout <fix>^` then shows a failure that is
partly the guard's own fault.

## `refactor:` is a claim about behaviour: the draft that cried wolf, and the three mislabelled commits

(Written after the rule's first draft flagged `b6b778b82`, a clean cross-module move, as
mislabelled — a convention that cries wolf on legitimate refactors gets ignored wholesale.) Three commits got
this wrong: `5f138ef8a` (`refactor(app)`) moved four call-sites onto a *resolved* Preferences node, changing where
any host with its own node reads and writes, and had to edit `CommandlineParserTest`; `7815d5960`
(`refactor(api)`) added an operator-editable template path plus a delete-watcher branch in `-app`; `358675fbf`
(`refactor(ci)`) stopped 13 of the pipeline's 20 jobs from starting a dind service, cutting ~34 s off each. All
three described the change honestly in the body — only the type lied. **`358675fbf` also shows why the label is
worth getting right the first time:** it was audited only after being merged into `develop` and `alpha`, at which
point the honest remedy is this entry, because the alternative is force-pushing two shared branches. The rule is
cheap before the merge and unfixable after it.

## Documenting a single-line constructor: the 2026-07-31 reshape

Four declarations were reshaped that way on 2026-07-31
(`ContainerRunOutput`, `CatalogCursor`, `Exclusion`, `Dependency`); an audit flagged it as scope creep, which it is
not — it is the enabling change.

## Definition of done, item 3: the zero-warnings reading and the 40-warning baseline

That is how a "zero warnings, none at all"
 reading reached `claude-docs/REFACTOR-AUDIT.md`. The honest baseline on `77fdd27d6`, from
 `./gradlew :serverpackcreator-{api,app,clientside,grinder}:compileKotlin --rerun-tasks`, is **40**: 31
 `Deprecated in Java` (the `Locale(String)` constructors, Jackson's `fields()`/`readTree(URL)`,
 night-config's `valueMap()`), 3 unchecked casts in drag-and-drop handlers, 4 `@OptIn` delicate-API
 notices for `CoroutineStart.ATOMIC`, 1 redundant conversion. Several are library-driven and move on a
 dependency bump without a source change, which is the reason to compare against a number that was taken
 the same way.
