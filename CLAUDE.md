# ServerPackCreator — Claude Code context

> **Purpose of this file:** the durable, *current-state* context for Claude Code sessions on
> ServerPackCreator. Read it before touching code.
>
> - Per-sprint **narrative** history → `git log` and `claude-docs/REFACTOR-LOG.md`.
> - **Deferred-but-agreed work** → `claude-docs/BACKLOG.md` (why it waited + context to pick it up cold).
> - **Behaviour changes on the published API** → `claude-docs/API-BEHAVIOUR-CHANGES.md` (one row per
>   change, what an embedder sees). The *policy* stays below; that file is its evidence.
> - **CI secrets — what each one is, its scopes, and which job dies without it** →
>   `claude-docs/CI-SECRETS.md`. Read it before touching a `secrets.*` reference: Forgejo rejects the
>   `FORGEJO_`/`GITEA_`/`GITHUB_` prefixes, so the credentials are `FJ_*`/`GH_*` on purpose.
> - **Runner-host registry caching — how to stop CI meeting ghcr's per-host burst limiter** →
>   `claude-docs/RUNNER-REGISTRY-CACHE.md`. Mostly host configuration; the workflow half has landed and is
>   fail-soft, probing each cache and going upstream for any that does not answer, so the two halves can
>   land in either order. Read section 0 before touching it: jobs run inside a `docker:dind` daemon, so
>   the caches are addressed by **fixed IP** — service names do not cross that boundary.
> - **A Windows runner, for the self-contained Windows artifact** → `claude-docs/WINDOWS-RUNNER.md`.
>   Host configuration, and unlike the registry cache it is **not** fail-soft: jpackage cannot
>   cross-compile, so the app-image job needs a `:host`-scheme Windows runner and QUEUES rather than fails
>   without one. The host half comes first. x86_64 only — an ARM64 guest would have to be emulated.
> - Module-specific facts, patterns and landmines → each module's own `CLAUDE.md`
>   (lazy-loaded by Claude Code when you work in that module).
> - Personal working preferences (general approach, organization, no-shortcuts ethos,
>   communication style) reach a session from the **user's own settings**, not from this repo — they
>   deliberately don't ship in a public shared file. Follow them as given; they outrank style choices here.
>
> **Engineering principles (binding):** KISS, MVC, TDD, SOLID. Operational naming, documentation,
> module-boundary and Kotlin rules are in **## Conventions** below — follow them for all code.

---

## Module map

What each module *is* comes from `settings.gradle.kts`, `README.md` and the module's own `CLAUDE.md`
(lazy-loaded when you work there). What is listed below is only what those cannot tell you: the
constraints.

- **`serverpackcreator-api` is published to Maven Central, so its public surface is a compatibility
  constraint** — plugins compile against it. Governed by the **API compatibility policy** below.
- **Every other module is unpublished and therefore churns freely** (`-clientside`, `-app`,
  `-grinder`, every plugin module, the frontend). `-clientside` in particular is free to change shape;
  `-plugin-example` is the exception that must always reflect *current* API idiom, because it is
  documentation by example.
- **Dependencies point inward toward `-api`, never outward** — see **Module boundaries** below.
- Not in the Gradle build, so `settings.gradle.kts` will not mention them:
  `serverpackcreator-help` (docs), `serverpackcreator-homepage` (the website), `buildSrc`, `docker`,
  `misc`.
- **`serverpackcreator-homepage` is a git subtree of a separate repository, and it must only ever be
  pulled with `--squash`** — see `serverpackcreator-homepage/CLAUDE.md`. A non-squash
  `git subtree pull` makes every website commit an ancestor of `develop`, and because `.releaserc.yml`
  uses a bare `tagFormat: '${version}'`, semantic-release reads their subjects as SPC's own. The
  original `git subtree add` did this: 39 website commits reaching back to 2023 landed in the next
  SPC changelog, and one `breaking:` among them would have released 10.0.0 instead of 9.3.0.
- **CI lives in `.forgejo/workflows`** — Forgejo (`git.griefed.de`) is the canonical CI and the origin of
  every release; `.gitlab-ci.yml` is gone. The wiring, the all-or-nothing `.forgejo`/`.github` landmine and
  the two deliberately-dropped GitLab capabilities are in `.claude/rules/ci-workflows.md`, which loads when
  you touch a workflow. Secrets, scopes and which job dies without which → `claude-docs/CI-SECRETS.md`.
- **The seven root-level documents are the source of truth, and both of their copies are GENERATED and
  gitignored.** `CHANGELOG.md`, `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `HELP.md`, `LICENSE`,
  `README.md` and `SECURITY.md` live at the repository root. Two `Copy` tasks in
  `serverpackcreator-api/build.gradle.kts` mirror them — `shipRootDocuments` into
  `serverpackcreator-api/src/main/resources/` (shipped in the jar; `ApiWrapper.setup()` writes them into
  the user's home) and `shipWritersideDocuments` into `serverpackcreator-help/Writerside/topics/` — and
  the CI step *Stage documents and images* in `.forgejo/workflows/docs.yml` copies the same set again
  before the help site is built. **Editing either copy is silently undone by the next build**, and both
  are in `.gitignore`, so `git status` will not tell you. Edit the root file. `LICENSE` is the one with a
  wrinkle: it has no extension, and Writerside needs it as `LICENSE.md` to render it as a topic. (Written
  after a doc change in this session was made against `Writerside/topics/HELP.md` and had to be replayed
  onto the root — the staged copy happened to be present and looked like the file to edit.)
- **`serverpackcreator-help/Writerside/api-docs.yaml` is GENERATED, not hand-maintained** — springdoc
  is wired into `-app` as `developmentOnly`, and the regeneration command sits beside that dependency
  in `serverpackcreator-app/build.gradle.kts`. It had drifted to 25 of 44 endpoints while being edited
  by hand, including two schemas for classes that no longer existed. Regenerate it rather than patching
  it, and regenerate it again after any change to a controller or an entity it serialises.

## Build & test commands

- `./gradlew build` — full build. The app build depends on the frontend build and license report, and
  since 2026-08-14 it also **runs the frontend's Vitest suite** (`checkFrontend` → `npm run test`). Before
  that, `checkScript` was unset, so the plugin SKIPped `checkFrontend` and a green `build` had executed
  zero frontend tests while still compiling and bundling the SPA.
- `./gradlew :serverpackcreator-api:test` — API suite (runs against fixture modpacks in
  `serverpackcreator-api/tests/` and `src/test/resources/testresources/`). **Offline for every Minecraft version in
  the shipped manifest snapshot**, which `ApiWrapper.setup()` seeds from the jar; a version newer than that snapshot
  costs one fetch of its `mcserver/<version>.json`. The snapshot **no longer lags its own parent
  manifest** — the release named in `minecraft-manifest.json`'s `latest.release` now has a matching
  `mcserver/<version>.json`, which is the check worth re-running rather than trusting a file count. That was the open
  deferral B25, closed as a side effect of the `updateManifests` retarget. The test home is wiped before each run **except** `manifests/`, so that cache persists and accumulates.
- `./gradlew :serverpackcreator-app:test` — app suite.
- `./gradlew :<module>:koverHtmlReport` / `koverXmlReport` — coverage (Kover), report under
  `<module>/build/reports/kover/`.
- Frontend: `npm install && npx quasar dev` in `serverpackcreator-web-frontend/` (dev server),
  `npx quasar build` for production build, `npm test` (Vitest).
- Run the app locally: `./gradlew :serverpackcreator-app:bootRun` (GUI by default; CLI/web via args,
  **not `:run`** — `-app` applies the Spring Boot plugin, not `application`, so `run` does not exist there;
  `:serverpackcreator-grinder:run` does, because the grinder applies `application`; see `Mode.kt` /
  `CommandlineParser.kt` for the arguments).
- `media` task needs install4j installed locally — not part of regular dev loop.

### Build layout

**ServerPackCreator targets Java 21, from ONE place: the `java` entry in `gradle/libs.versions.toml`.**
The Gradle toolchain, the Kotlin `jvmTarget`, Dokka's JDK and the IDEA language level all read it.
Everything Gradle cannot reach — fourteen CI `setup-java` steps, Qodana's linter, install4j, two Dockerfile
stages, the AppImage script — is mapped in **`misc/Java-References.md`**, which is the file to open before
bumping, not after. It also names what must NOT move with it: the server-pack script templates track
Mojang's per-Minecraft `requiredJavaVersion`, which is a different number that happens to share values.

**Where every build declaration lives, and the landmines protecting them, are in
`.claude/rules/build-layout.md`** — it loads whenever you touch `build.gradle.kts`,
`settings.gradle.kts`, `buildSrc/`, or the version catalog. Read it before changing any of those:
it is the difference between a two-line bump and re-introducing a failure this project already paid
for. `BUILD.md` is the contributor-facing tour of the same ground.

## Branching & git workflow

- PRs target **`develop`**; `main` is the release branch.
- One branch per feature/fix, prefixed `claude-` when created by Claude. Group related fixes.
- **Never push. Keep all changes local — the user pushes.**

## API compatibility policy (adopted default — Griefed may override)

- The plugin-facing API (everything `serverpackcreator-api` exports, esp. `plugins`,
  `ApiWrapper`, `PackConfig`) stays source-compatible within a major version.
- Refactors keep old entry points as thin deprecated facades (`@Deprecated` with `ReplaceWith`)
  for at least one major release before removal.
- Internal-only types may move/change freely once they are no longer exported.
- **Source-compatible is not the same as behaviour-compatible.** A change that keeps every signature but
  alters what an exported call *returns* is still a contract change for embedders, and belongs in the
  release notes even though nothing fails to compile.

**The behaviour-change record lives in `claude-docs/API-BEHAVIOUR-CHANGES.md`** — one row per change,
with what an embedder actually sees. Append to it whenever you change what an exported call *does*,
and read it before answering "will this break an embedder?". It is out of this file because it is
evidence consulted occasionally, not context every session needs.

---

## Conventions

- **"Make it work, make it right, make it fast." — Kent Beck.** A more detailed variation often cited is
  *"First, make it. Then, make it work. Lastly, if you can, make it pretty."* The sequence exists to head
  off perfectionism and analysis paralysis: functionality comes before form, and the core logic has to be
  solid before anyone spends effort on readability or speed.
  - **Avoiding premature optimization.** Knuth's "root of all evil" — you cannot predict bottlenecks
    without a working system to measure. This project has the receipts: B30 was a real 121,492-byte
    saving per startup that bought **~0 ms**, because the twelve manifest checks run concurrently and the
    slowest one gated the batch. Measured, it was the wrong thing to optimise; the right one (B31, taking
    the refresh off the startup path) was ~392 ms and only visible once something was running.
  - **Managing technical debt.** Shortcuts may be taken first, but the bargain is that you come back and
    polish. Many developers argue "fix it later" is a myth, and that is the risk this convention set
    exists to contain — which is why `claude-docs/BACKLOG.md` demands a *stated reason* per deferral and
    enough context to pick it up cold, rather than a wish-list.
  - **Iterative improvement.** A messy first draft, then refinement.

  **How this squares with TDD and "no shortcuts", which it looks like it contradicts:** the ordering is
  about which *concern* you attack first, not permission to skip pinning. "Make it work" is what the
  characterization test asserts; "make it right" and "make it fast" are the steps the test then protects.
  Read the other way round it licenses exactly the failure this file already documents at length — the
  performance branch whose tests were written by the same pass that changed the code and therefore passed
  by construction. Draft messily, but pin before you refine, and never let "make it fast" arrive before
  there is something whose behaviour is known.

- **A published verdict must rest on decisive evidence, and the report must say which rung produced it.**
  `CRASHED` is reachable both from a marker no broken harness can fabricate and from a bare non-zero exit that
  means only *"nothing recognised why"* — and until `BootDecision` those were indistinguishable downstream, so
  the grinder published both alike. Measured against the live daemon on 2026-08-31: **27 of 43 published
  `HIGH` verdicts rested on no decisive evidence**, and one poisoned loader-cache entry (`NeoForge 21.11.45 /
  MC 1.21.11`) had produced identical failures across all **90** boots against it — a library mod and a
  server-side building mod among them. Two lessons generalise beyond the grinder: **a verdict that cannot name
  its own evidence cannot be audited**, and **an environment defect looks exactly like a subject defect unless
  something distinguishes them**.
- **Question the requirement before you optimise the cost of meeting it.** On 2026-09-02 a circuit breaker
  was designed, pinned with 189 lines of guards, implemented, wired through two modules and documented — and
  deleted 34 minutes later, when Griefed asked whether the thing it protected was needed at all. It was not:
  the route it bounded existed only to circumvent CurseForge's distribution opt-out, had stopped working
  entirely, served under 1% of candidates, and cost 192.9 MB in every artifact. Every commit in that sequence
  was correctly shaped, which is exactly why the shaping did not save it. The evidence to ask the prior
  question was already in hand. This is Knuth's rule one level up: *measure before optimising* presumes the
  thing should exist, so establish that first — "should this code exist?" is cheaper to answer than "how do I
  make its failure cheap?", and one of the two answers deletes the other's work.

- **Cite names, not snapshots.** Three consecutive audits of the performance branches found the same
  defect class and nothing else: a fact quoted in prose going stale the moment the code moved — 54 commit
  hashes killed by a rebase, a landmine still describing a flaw that had been fixed, a line number shifted
  by the very commit that cited it, and suite counts left behind by the tests that were just added. **It
  recurred on 2026-09-01:** that rebase killed 13 more hashes in `claude-docs/REFACTOR-AUDIT.md`, five of
  which are now reachable from no ref at all and will stop resolving entirely once gc runs. That file cites
  hashes at volume, so it is the guaranteed casualty of every history rewrite — write subjects there the
  first time. Prefer the **commit subject** over its hash (subjects survive rebase, cherry-pick and squash),
  the **symbol name** over `File.kt:123`, and "what the guard asserts" over "how many tests exist". Where a
  number genuinely earns its place — a measurement, a byte count — say what produced it, so a reader can
  re-run it instead of trusting it.

- **Fix bugs when you find them and read before you advise.** A defect noticed in passing gets surfaced
  and fixed in its own commit, never deferred as "out of scope"; a claim about this codebase gets checked
  against the code or the docs first. (The other two bullets that used to sit here — the principles list and
  the git workflow — are stated in the header and in **## Branching & git workflow**.)

### Module boundaries (architecture — SOLID / MVC)

- `serverpackcreator-api` is the domain core. It must **not** gain compile dependencies on Spring web
  or the web frontend. Dependencies point inward toward `-api`, never outward; the app and frontend are
  adapters around it. Flag any inward-pointing violation.
  - **Swing has one carve-out, and it is the `plugins.swinggui` package plus the `ApiPlugins` methods
    that serve it** — `TabExtension`, `ExtensionTab`, `ConfigPanelExtension`, `ExtensionConfigPanel`,
    and `ApiPlugins.addTabExtensionTabs`/`getConfigPanels`. Same reason `java.awt` is carved out in
    `serverpackcreator-api/CLAUDE.md`: **only `-api` is published**, so a GUI extension point has
    nowhere else to live, and inverting it behind an app-side adapter would remove the capability from
    plugins entirely. Nothing else in `-api` may import `javax.swing`; those five files are the whole
    list, and it is re-derivable with
    `grep -rl "javax\.swing" serverpackcreator-api/src/main/kotlin`.
    Stated here because the rule above and the code have disagreed in writing since the extension
    points were added, which costs every audit the same rediscovery.
- Domain logic in `-api` must be unit-testable **without** booting a Spring context. A class that
  needs the Spring container to be tested is a design smell — flag it.
- (Plugin-API stability is governed by the **API compatibility policy** above — don't duplicate it.)

### Package structure (organize by feature, not by layer)

- Organize code by business feature, not by global technical layer.
- Create a dedicated package for each feature and keep its related classes together.
- Introduce subpackages such as `ui`, `service`, `domain`, and `persistence` within a feature only when
  they improve clarity.
- Define explicit APIs and enforce clear dependency boundaries between features.
- Minimize coupling and avoid unnecessary layers, abstractions, and indirection.
- Use shared packages only for genuinely cross-cutting concerns.
- Keep the architecture simple, cohesive, and maintainable. Do not impose rigid package structures
  without a concrete benefit.
- Follow these rules consistently for all newly created code and when modifying existing code.

### Refactor discipline (behavior-preserving by default)

- **One commit per GOAL, not per edit — Griefed's call, 2026-10-12.** A commit is the unit at which
  somebody later asks *"why was this done?"*, so everything sharing one answer belongs in one commit,
  however many files, modules or languages it spans. Atomic commits blow up the history: they record
  the *process* of arriving at a change rather than the change, and a reader paging through twenty of
  them has to reconstruct the one intent that produced them.
  - **The test for whether two changes belong together is whether they share a reason**, not whether
    they touch the same file. A workflow edit and the shell script it calls are one commit when one
    goal needs both; two unrelated fixes in the same file are two commits.
  - **It is worth the most where the first attempt was wrong.** A feature plus the four repairs its
    own audit then found is **one** commit, because the repairs were never separately true — nothing
    shipped in between. The history should show the feature that works, not a broken one followed by
    apologies. Measured instance: `CI-NOTES.md` was written by one commit of a session and deleted by
    a later one in the same session, so the atomic history recorded a 156-line file arriving and
    leaving; grouped, it never existed.
  - **This does not license a grab-bag.** "Everything I did today" is not a goal. If the commit message
    needs the word *"and"* between two unrelated clauses, it is two commits — `fix(ci): the continuous
    build's GitHub half still prints the traceback` stayed separate from the homepage work for exactly
    that reason.
  - **Keep "add tests", "pure refactor" and "change behaviour" separable where they are separable**, and
    see the pin-first rule below for what survives when they are not. A `refactor:` label is still a
    claim about behaviour, so a commit that groups a refactor with a behaviour change is labelled for
    the behaviour change.
- Pin current behavior with characterization tests **before** restructuring legacy code; refactor
  in small steps behind source-compatible facades (Strangler-Fig), keep the suite green at every
  commit.
- A pure-refactor commit must keep the **existing** assertions green. If a test must change for a
  "refactor", that's a signal the change is **not** behavior-preserving — stop and flag it.
- Boy-Scout rule: leave touched files cleaner than you found them, but stay within the commit's
  stated scope; don't let cleanup sprawl into unrelated files.
- **Shell templates, manifests and version parsing get their test written and observed failing FIRST.**
  Not because the rule is different there, but because these fail *silently* — a wrong branch or a
  mis-sliced version produces a plausible value, not an error — so they get verified by hand and pinned
  afterwards, if at all. Two audits found three instances (`28a786b58` NeoForge mapping, no test;
  `2e16bf0c8` fish `--no-empty`, test five commits later; `1a55797df` Fabric fall-through, whose guard
  asserted *ordering* and stayed green while the behaviour was still broken). The cost is measurable:
  the Forge launcher-era bug wasted **24 boots** before anything noticed, and 820 NeoForge versions were
  mis-attributed to one Minecraft release.
- **A test that only asserts shape is not a pin.** Prefer *executing* the unit — `ScriptTemplateContentTest`
  extracts a shell function and runs it — over asserting substring positions or that a symbol exists. And
  **confirm the test fails before the fix**: a guard whose teeth were never checked has repeatedly turned
  out to assert nothing (twice in one session, when a mis-indented edit meant the "broken" run was
  actually unmodified code).
- **A performance branch is not done until it is proven equivalent to its base.** Green tests are not that
  proof: they are HEAD's tests, written by the same pass that changed the code, and they pass by construction.
  The check that *is* proof is cheap and repeatable — run the **base branch's unmodified test tree against the
  branch's production code**:

  ```
  git worktree add --detach <tmp> HEAD
  cd <tmp> && rm -rf <module>/src/test && git checkout develop -- <module>/src/test
  ./gradlew :<module>:test --continue
  ```

  Every failure is either a regression or a deliberate change; every *compile* error is a signature change,
  which is a finding in itself and must be enumerated rather than worked around. Done for this branch
  (`claude-docs/REFACTOR-AUDIT.md` iteration 7): **490 pre-existing guards, zero failures**, with exactly two files
  uncompilable — one adapted by adding two constructor arguments and *no* assertion edits (7 guards green), one
  legitimately unadaptable because it asserted behaviour the branch removed. Also check *which* changed classes
  the base's tests actually name, so the residual risk is stated rather than assumed; a class-name grep
  under-reports, since `QuiltPackScanner` is exercised only through `ModScannerSidenessTest`.
- **What only a real runtime can answer, ask a real runtime.** Anything whose point is what an external system
  does — an index, a data migration, a REST response shape — is not verified by a mocked test, however good.
  Iteration 7 ran the actual `bootJar` in `-web` mode against MongoDB 8.0.5 in Docker, seeded with pre-branch
  shaped documents, and that is what confirmed the `sha256` index really exists, the migration really converts
  legacy documents and really skips already-migrated ones, and `/api/v2/runconfigs/all` really returns the
  documented shape. It also surfaced what was filed at the time as B33 — the web application
    writing to MongoDB's default `test` database instead of the configured one, which no test could have caught, and
    which the Spring Boot 4 property-key fix has since closed. Cost: about fifteen minutes.
- **Build logic is verified by measurement where it cannot be unit-tested, and `buildSrc` has a test source
  set for its pure functions** — the rule, and the landmine that `./gradlew build` does not run
  buildSrc's tests, are in `.claude/rules/build-layout.md` (loads when you touch `buildSrc/` or a build script).
- **Pin first means *commit* first, not just write first — and when grouping absorbs the boundary, the
  message carries it.** The ideal is still a failing `test(...)` commit, **red**, with the fix in the next
  one, because that is the only form a reader can re-run (`git checkout <fix>^`). Where the goal-grouping
  rule above puts the guard and the fix in one commit, the message must state that the pin was written and
  **observed red first**, and quote the mutation that reproduces the red — `8f2ac90ce` is the worked
  example, a squash whose message reproduces its own `test(api): pin that a stop signal reaches the
  server, red` sub-commit verbatim along with the two mutations that fail against it. A grouped commit
  that merely asserts "tests added" has thrown the evidence away, which is the thing this rule exists to
  prevent. In-session verification with no written trace is not a substitute: it leaves no evidence, and
  it is exactly what silently passed twice above. Audited 2026-07-31 — all **eight** code
  commits of that day's plan (`2a9a03473`, `30f6cbded`, `c571e2d7f`, `07a647f01`, `aa2d27f7f`, `91ac0e1a9`,
  `1f92f585c`, `5caa6833f`) bundled guard and change, so nobody can check out `2a9a03473^` and watch the pin
  go red. The tests were written first; only the boundary collapsed, which is the part that costs nothing to
  keep and everything to reconstruct later.
- **Run the pin before you commit it red, and read *why* it failed.** Committing a guard red is only
  evidence if the red is the missing implementation. Twice on 2026-09-01 it was not: a fixture omitted a
  collaborator, so four asserted field paths could not have resolved even against correct code; and a Kotlin
  `${'$'}` escape produced the literal `...-v${'$'}version`, so the guard asserted a string no implementation
  would ever return. Both were fixed in the very commit that was supposed to turn them green, which quietly
  undoes the boundary the previous rule exists to create — `git checkout <fix>^` then shows a failure that is
  partly the guard's own fault. One run before `git commit` distinguishes the two, and costs seconds.

- **`refactor:` is a claim about behaviour, not about intent.** Use it only when behaviour is preserved; label
  a behaviour change `fix:` or `feat:` however tidy it looks. If an **existing** test's *assertion, argument or
  expected value* has to change, the label is already wrong — that is the stop-and-flag signal, not a formality.
  **Carve-out: a reference-only update is not the signal.** Moving a symbol between modules necessarily updates
  imports and receivers in its tests, and Strangler-Fig moves are exactly what the conventions ask for — so
  `- Old.isSuspendGap(gap, poll)` / `+ New.isSuspendGap(gap, poll)`, with every assertion byte-identical, stays a
  `refactor:`. Judge the diff, not the file list: if no expectation changed, the test did not change in the sense
  this rule means. (Written after the rule's first draft flagged `b6b778b82`, a clean cross-module move, as
  mislabelled — a convention that cries wolf on legitimate refactors gets ignored wholesale.) Three commits got
  this wrong: `5f138ef8a` (`refactor(app)`) moved four call-sites onto a *resolved* Preferences node, changing where
  any host with its own node reads and writes, and had to edit `CommandlineParserTest`; `7815d5960`
  (`refactor(api)`) added an operator-editable template path plus a delete-watcher branch in `-app`; `358675fbf`
  (`refactor(ci)`) stopped 13 of the pipeline's 20 jobs from starting a dind service, cutting ~34 s off each. All
  three described the change honestly in the body — only the type lied. **`358675fbf` also shows why the label is
  worth getting right the first time:** it was audited only after being merged into `develop` and `alpha`, at which
  point the honest remedy is this entry, because the alternative is force-pushing two shared branches. The rule is
  cheap before the merge and unfixable after it.

### Kotlin idioms

- Prefer `val` over `var`; immutable data by default.
- `data class` for value types, `sealed class` + exhaustive `when` for state, Kotlin null-safety
  instead of defensive null checks. Don't port Java patterns 1:1.
- No **new** `!!` non-null assertions in refactored code — handle nullability explicitly.
- **Naming — speaking names:** variables, parameters, constants, types and methods get names a
  reader can derive meaning and context from (`configRepo`, `attemptCount`, `tokenOverridesJSON`),
  not single letters. The one carve-out is the throwaway loop counter; it generalises only to a
  small, closed set of established Kotlin idioms whose meaning is universal and whose scope is a few
  lines. The line is "would a newcomer have to scroll up to learn what this is?" — if yes, name it.
- **Documentation — as little as possible, as much as necessary.** Document at the **class, method or
  function level**. Add an **inline** comment only where it explains complex logic, non-obvious
  behaviour, an assumption, a constraint, a side effect, or a design decision that cannot readily be
  inferred from the code. Before writing a comment, ask whether an experienced developer could
  reasonably infer it from the code — if yes, omit it. Prefer clear naming and simple structure over
  explanatory prose. Never restate self-explanatory code, and never add a redundant, obvious or purely
  descriptive comment. Where a doc comment *is* warranted, state briefly WHAT the unit is for and HOW
  it achieves it; one or two sentences is the target, and needing more is a sign the unit is doing too
  much (KISS). Preserve existing documentation that carries meaning; remove or update what is
  redundant, misleading or outdated whenever you touch the code — a stale comment is worse than none.
  **This supersedes the former "comment everything" rule**, which required a doc comment on every
  function, method and exported constant including unexported ones.
- **Documentation describes the present tense — Griefed's call, 2026-10-02.** A doc comment says what the
  code is and does, for a reader who has never seen any other version of it. It does not say what the code
  *used to* be, which CI run made it change, what an earlier comment got wrong, or why today's shape is
  better than yesterday's. "This was 60 s, failed run 716, and was raised to 90 s" is four facts a reader
  cannot act on; "five minutes, because a loaded CI host must never be able to decide a verdict" is one
  they can. Same for code: no commented-out code, no "kept for the old call-site", no parallel
  explanation of a removed branch.
  - **Constraints stay, history goes.** "BuildKit defaults a mirror to `https`, so the second stanza is
    required or it silently falls back" is current mechanism and belongs in the file. "Three runs died
    before we learned that" does not. The test is whether the sentence changes what a reader *does*.
  - **The history is not lost, it moves to where it is looked up.** Incidents, measurements, run numbers
    and dates go in the **commit message** (which is what `git log -S` and `git blame` are for),
    `claude-docs/REFACTOR-LOG.md`, or a landmine in the relevant `CLAUDE.md`. Those files exist to be
    consulted occasionally; a doc comment is read every time someone opens the function.
  - **One statement, one place.** If a fact is in the module's `CLAUDE.md` and in a doc comment and in the
    workflow that depends on it, two of those will go stale. Keep it where it is enforced and reference it
    from the others by name.
- **Documenting a single-line constructor means reshaping it, and that reshape is in scope.** Per-parameter KDoc
  cannot attach to a parameter sharing a line with others, so `data class X(val a: A, val b: B)` has to become one
  parameter per line before each can be documented. Four declarations were reshaped that way on 2026-07-31
  (`ContainerRunOutput`, `CatalogCursor`, `Exclusion`, `Dependency`); an audit flagged it as scope creep, which it is
  not — it is the enabling change. Keep it to the declaration being documented, and verify the obvious: parameter
  names, types, order and defaults must survive untouched. The alternative, a class-level `@param` block, leaves the
  properties themselves undocumented as far as dokka is concerned.
- **A doc that only restates the signature is omitted, not written.** For a pure accessor over a well-named
  field there is usually nothing to add, so add nothing. Put the shared meaning where it belongs instead:
  `ServerPackConfigTab`'s accessors were labels until the *interface* doc explained that they read live tab
  state rather than the saved configuration, which is the fact every one of them depends on. One honest
  paragraph on the type beats forty restatements on its members — and beats forty silences too, because the
  paragraph is the part a reader cannot infer.
  - **The build agrees: `reportUndocumented` is `false` in
    `buildSrc/src/main/kotlin/serverpackcreator.dokka-conventions.gradle.kts`.** It warns for every public,
    protected or package-visible declaration without KDoc, which is the opposite of this rule, so a doc
    deliberately left off no longer costs a dokka warning. Do not set it back to `true` without changing
    the policy above first — the two have to say the same thing.
- **Errors:** always handle, never ignore with `_` unless intentional (comment why).

## Definition of done (per change)

1. Tests written first and green (`./gradlew :<module>:test`).
2. Documentation per **## Conventions** — as little as possible, as much as necessary; no restatements.
3. No new compiler warnings; stale comments updated. **Measure with `--rerun-tasks`, or the number is
   about Gradle's cache rather than the code** — an UP-TO-DATE Kotlin compile does not run the compiler, so
   a grep for `w:` returns zero whatever the source says. That is how a "zero warnings, none at all"
   reading reached `claude-docs/REFACTOR-AUDIT.md`. The honest baseline on `77fdd27d6`, from
   `./gradlew :serverpackcreator-{api,app,clientside,grinder}:compileKotlin --rerun-tasks`, is **40**: 31
   `Deprecated in Java` (the `Locale(String)` constructors, Jackson's `fields()`/`readTree(URL)`,
   night-config's `valueMap()`), 3 unchecked casts in drag-and-drop handlers, 4 `@OptIn` delicate-API
   notices for `CoroutineStart.ATOMIC`, 1 redundant conversion. Several are library-driven and move on a
   dependency bump without a source change, which is the reason to compare against a number that was taken
   the same way.
4. `claude-docs/REFACTOR-STATE.md` (and the relevant module `CLAUDE.md`) updated when an architectural
   step lands. Append the blow-by-blow to `claude-docs/REFACTOR-LOG.md`, not here.

---

## Refactor state

**Goal:** KISS/MVC/TDD/SOLID across api → app → plugin-example → web-frontend; phases 1–4 complete.
Remaining hotspots, open issues and the **lessons that generalise beyond their incident** are in
`claude-docs/REFACTOR-STATE.md` — read it before a refactor, a CI or flaky-guard diagnosis, or writing a
new guard. Per-module test counts: re-derive from `<module>/build/test-results/test/*.xml`, never from
prose. Full history: `claude-docs/REFACTOR-LOG.md`.
