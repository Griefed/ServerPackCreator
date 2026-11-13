# Refactor state and lessons that generalise

Moved out of the root `CLAUDE.md`, where it was loaded into every session. The root file keeps a
pointer here; the per-sprint narrative is in `claude-docs/REFACTOR-LOG.md`, and per-module test counts
are re-derived from `<module>/build/test-results/test/*.xml` after a run rather than recorded in prose.

**Goal:** KISS/MVC/TDD/SOLID across api → app → plugin-example → web-frontend.
**Phases:** 0 baseline · 1 API · 2 app · 3 plugin-example · 4 frontend.

**Remaining hotspots:** `ConfigEditor.kt` 1,369 — assessed: the two pure pieces (dirty-check,
Java-version) are extracted; the rest is legitimate Swing view code, not worth mechanically
splitting. `LarsonScanner.kt` 2,217 — self-contained widget, low priority.

**Open issues (details + locations in the relevant module `CLAUDE.md`):**
The GUI `GlobalScope.launch` anti-pattern (app) is **resolved** — all 26 sites now use
`gui.utilities.ComponentCoroutineScope` (lifecycle-cancelled), GUI-verified. The frontend's
settings-store `$q` coupling (4b) and `jsconfig.json`/TS gap (4c) are **resolved**.

**Current phase — 4 (frontend) complete; GUI structured-concurrency done.** Frontend 4a–4e: Vitest,
settings-store `$q` decoupling, full TypeScript migration (all `src/` is TS, verified by
`quasar build`), a Quasar component test harness (Vue Test Utils), and broadened component coverage
(all cards + nav SFCs; suite at 32 across 14 files; tables left untested by design — trivial format-lambda logic vs.
brittle QTable rendering). The GUI `GlobalScope.launch` anti-pattern is resolved (see Open issues),
GUI-verified. **Next (optional):** broaden component-test coverage further.

**Lessons that generalise beyond their incident.** The narratives they came from — the grinder plugin
(2026-09-06), the `ERROR` bucket (2026-09-09) and the `UNVERIFIABLE` bucket (2026-09-10/11) — are in
`claude-docs/REFACTOR-LOG.md`; the module-specific detail is in each module's own `CLAUDE.md`.

- **A defect whose multiplier is the count of something the repo only ever has one of cannot be found by
  testing what the repo ships.** One installed plugin made a per-plugin loop invisible for months.
- **A fixture installed *after* the thing it is meant to exercise has already run is not a fixture.** The
  api suite stayed green for months against an unbounded plugin-loading recursion for exactly that reason.
- **A category defined by its consequence will accumulate everything with that consequence, whatever the
  cause** — so define it by the cause and make the type carry it (`PreventionCause`), not a sentence.
- **A defect whose evidence is a published report can be diagnosed without touching the host.** The
  report's wording is part of the evidence, which is the argument for it being precise.
- **A test can pass against unfixed code because one fixture value is a prefix of another.** Ask why a
  guard *passed*, not only why it failed, whenever fixture values could contain one another.
- **A guard that cannot compile is not a red pin.** It fails on a missing symbol, which says nothing about
  the logic. Three sanctioned ways out, all of which make the red a *wrong answer*: land the seam first as
  its own behaviour-preserving commit; say in the message that the boundary is missing and quote the
  mutation that reproduces the red; or — **for new code, where there is no behaviour to preserve and so no
  honest "behaviour-preserving" seam commit exists** — land the seam *with* the guard in the `test(...)`
  commit, with the implementation stubbed, and state in the message which assertions are vacuous against
  that stub. **Griefed's call, 2026-09-25**, after the `servertest` branch took the third route nine times
  and an audit flagged it: the branch is not being re-split, and the route is sanctioned rather than merely
  tolerated. It costs one thing — no commit is a pure "add tests" commit — so name the vacuous guards
  explicitly, because they are the ones that will otherwise be mistaken for pins.
- **An expression statement reads exactly like an assertion and asserts nothing.** `suggestInclusionsTest`
  called `dirs.any { it.source == "config" }` six times and discarded every result — valid Kotlin, no
  warning, and indistinguishable from a guard at a glance. It passed against *any* return value, proven by
  mutating `suggestInclusions` to `return ArrayList()`: the old body stayed green, the replacement fails with
  `'config' must be suggested, got []`. Its last line was also the wrong question — `any { it != x }` is true
  the moment a second element exists, where the intent was `none { it == x }`. A whole-repository scan for
  `@Test` functions with no assertion reachable through their same-file helpers found **three real cases in
  2094** (the other two only smoke-tested a logging call), so the class is rare but silent, and the two ways
  it hides are a bare `any`/`map`/`filter` whose result is dropped, and a call that can fail only by throwing.
  Frontend specs were scanned the same way and were clean.
- **A benchmark is not a test, and naming it one costs a guard.** `StoreWriteBenchTest`'s three methods
  printed timings and could not fail on them. The fix was not to assert the timings — this project pins I/O
  by request, read and open counts, never by wall-clock, and `CoalescedVerdictWritesTest` already pins the
  behaviour the numbers motivated. It is now `StoreWriteBenchmark`, its methods say `measure…`, and it
  asserts its own *fixture* so a run that mis-seeds fails loudly instead of reporting a confident number
  about nothing. Ask of a measurement what it would take to make it red; if the answer is "nothing", it
  belongs in a file that does not claim otherwise.
- **A guard gated on something nobody supplies is indistinguishable from no guard, and it reports as a
  skip rather than a gap.** Measured on 2026-09-26: 30 of the suite's tests were gated on an env var, a
  secret or an installed interpreter, and **not one of those gates was set in any workflow** — `test.yml`
  requires no secrets at all, by design. Nine of them (`DockerJavaContainerEngineIT`) needed nothing but
  `busybox` and now run on every push in 30 s; three more — the fish and PowerShell parse checks and the
  `JAVA_INSTALLER` probe — were the guards for the two shells this repo has twice shipped silent template
  bugs in, and the third guarded a branch every hand-made pack takes. Ask of a skipped
  test *who is supposed to set its gate*, and if the answer is nobody, it is not gated — it is absent.
  The corollary for writing one: a check that cannot run must **skip, never pass**, or the gap disappears
  entirely.
- **A container's mount is resolved where the daemon is, not where the test is — and a wrong one is
  silent.** Those three ungated checks ran for the first time on 2026-09-26 (run 646) and all three
  answered about an empty directory: `docker run -v <hostPath>:/templates` resolves the source on the
  **daemon's** filesystem, and the Forgejo runner's job container talks to a sibling daemon that holds no
  copy of its `/tmp`, so Docker created the missing directory and mounted that. Zero exit, no warning.
  One transport defect produced three different-looking verdicts — fish FAILED on a glob it could not
  expand, the PowerShell parse check **PASSED** having parsed nothing, and the installer-Java probe
  SKIPPED for want of its script — and only the middle one is dangerous, because it is the one that reads
  as a clean bill. `docker cp` streams through the daemon API and fails loudly where the bind fails
  silently. The transferable part is the reproduction: a developer machine cannot show this, because its
  daemon *is* its filesystem. `docker run -d --privileged -e DOCKER_TLS_CERTDIR= -p 12375:2375
  docker:dind` plus `DOCKER_HOST=tcp://127.0.0.1:12375` reproduces the runner's topology in about a
  minute, and is what turned an argument into a measurement.
- **When a guard is green on one host and red on another, suspect the question before the hosts.** CI run 629
  failed `HostProcessDescendantTeardownTest` against a process the runner had killed correctly. `ProcessHandle.isAlive`
  answers *"is this PID in the table"*; the guard meant *"is this process running"*, and the two differ only for a
  process that has exited and not been reaped — which is permanent under a container PID 1 that is not an init, and
  invisible on a developer machine where launchd reaps. Both the fix and the guard were right; the *predicate* was the
  wrong predicate. Two containers differing only in PID 1 separated them in minutes, and the same difference was
  costing the production teardown its whole 5 s budget (5000+ ms → 3 ms) on a process that had already stopped. An
  environment-shaped failure is evidence that an assertion is asking something narrower or wider than it means.
- **Duplicated knowledge drifts toward whichever copy is easier to reach** — three instances so far. Delete
  the duplicate rather than correcting it, and ask of any new lookup table which existing one already
  answers it.
- **An axis chosen for how work is *produced* asks the same question repeatedly and never asks the others.**
  The grinder ground a mod once per modloader because that is how a boot is parameterised — and spent 3.06
  boots per project on a mean of 1.6 distinct Minecraft eras, never booting `aether`'s 1.12.2 build at all.
  The axis worth keying on is the one along which the *answer* varies.
- **A report row that names a thing it did not use cannot be audited, and reads as a different bug.** The
  aether row named `aether-1.12.2-v1.5.4.1.jar` while its dependency failure belonged to the 1.20.1 jar
  staging had actually selected; checked against the platform page, that reads as broken dependency
  resolution rather than as broken attribution. Two selections for one row is the defect — make one.
- **Changing a key's shape is not the same as changing its value, and a one-hop migration does not
  generalise.** `supersededLegacyKey` computes the superseded key from fields the new row still carries,
  which works when the mapping is one-to-one and cannot work when several rows collapse into one. Removal by
  *prefix* is the shape that generalises, and it is why a key component worth migrating past needs a marker
  in it.
- **A fix written against one artifact is a fix for one artifact.** The Connector-placeholder redirect was
  correct, tested and documented — and blind to the second of the two shims the *same project* publishes,
  because it hard-coded `META-INF/mods.toml` while `LoaderDescriptors` already owned the versioned answer.
  That is the fourth instance of duplicated knowledge drifting toward the easier copy. When a fix keys on a
  file name, ask which other spellings of that name exist before calling it done.
- **A prior decision holds only under its premises, and a structural change is a change of premises.** B36
  recorded that a Connector boot is worth attempting anyway — right when the shim cost one of three boots,
  wrong once the per-line axis made it cost one of one. Nothing about Connector changed. Re-read the recorded
  calls a structural change touches instead of treating them as settled.
- **A setting whose destructive sibling is on by default is a setting that never runs.** "Update Server Packs"
  read its own manifest *after* the overwrite-cleanup had deleted it, and overwrite defaults to on — so the
  feature was a no-op in the configuration every user starts from, and took the world it existed to protect.
  The GUI hid it by greying the checkbox out, which is why it survived as "experimental" rather than being
  reported as broken: **a guard rail in one adapter is not a fix, it is a reason the bug stays unreported.**
  Ask of any pair of settings which one wins, and answer it where the behaviour lives.
- **"Protected" and "absent" are different, and conflating them breaks the artifact.** The same protected-paths
  predicate that must keep `server.properties` and `variables.txt` from being overwritten was also used to keep
  files out of the ZIP — which shipped an archive without them, whose start scripts had nothing to read. One
  list, two questions; the second needed the manifest ("did we produce this?"), not the first.
- **A selection made on metadata needs a way for the artifact to re-open it.** A platform's loader tick
  decided the boot, the downloaded jar disagreed, and only one narrow form of disagreement — *carries a
  different descriptor* — could reopen the choice. The re-selection machinery already existed; what was
  missing was reasons to invoke it. Carrying a loader's descriptor and being able to run under it are
  different questions.
- **A symptom shared by two layers is evidence for neither, and the layer you own is the tempting answer.**
  The grinder's report 502'd on every path at 131.3 s, including endpoints that do no work — read as
  thread-pool starvation, which fit. A cache and a bigger pool shipped against it; the 502s continued
  unchanged at 130.2–131.1 s. The requests had never arrived: a containerised nginx was dialling the Docker
  bridge gateway and the host firewall was dropping the SYN, and ~131 s is simply Linux's `tcp_syn_retries`
  budget. One line on the host (`curl 127.0.0.1:<port>/dashboard` → **200 in 0.368 s**) separated the layers,
  and the thread dump proved it by an **absence** — `newFixedThreadPool` creates workers lazily and never
  retires them, so no `pool-*` thread means no request ever reached a handler. Prove the request reaches your
  code before attributing an outage to it.
  - **Why the firewall was dismissed twice before it was confirmed, which is the transferable part:** there
    was no rule about that port. `Chain INPUT (policy DROP …)` was doing it, so `ufw status` showed nothing
    relevant and "ufw is not the issue" looked like a checked fact. **"No rule for this" and "not filtering
    this" are opposite statements**, and only the chain *policy* separates them — so two more hypotheses
    (conntrack at 569 of 262,144; a renumbered Docker network whose gateway turned out correct) were chased
    first. When a negative rules a layer out, ask what was actually read to rule it out.
  - **The second trap:** that same policy was the only access control on an unauthenticated report bound to
    `0.0.0.0`, so the obvious `ufw allow <port>` would have ended the outage and published the verdict table
    and full CSV export in one command. Ask what a guard rail is load-bearing for before removing it.
  - **The third rung, 2026-11-09:** the same outage recurred with the fix *present and correct in `ufw
    status` and absent from the kernel*. `ufw status` reports `/etc/ufw/user.rules`, not the running ruleset,
    and a stale snapshot had replaced the chain after boot. **A tool's report of its own configuration is not
    a reading of the system it configures** — `iptables -S ufw-user-input | grep <port>` is, and `ufw reload`
    was the entire repair. The errno is identical to a genuinely missing rule, so the documented triage sent
    the reader to add a rule they already had.
- **A green run and a red run of the same commit can be the same failure, and the green one is the one
  to worry about.** `921d46938` ran `test.yml` twice on 2026-10-01. Run 836 failed fourteen
  database-backed tests; run 833 was **green** — having *skipped* the same fourteen. One cause:
  flapdoodle's 30 s `mongod` start budget on a runner then doing three Gradle/npm builds at once. What
  differed was only which `mongod` lost the race — the Spring context's (a failure) or the
  `EmbeddedMongoAvailable` probe's (a skip, which the guard exists to produce). The guard worked exactly
  as designed and converted a red build into a green one that verified nothing about the database.
  **A skip-on-unavailable guard turns an environment problem into an invisible one**, so the pair to
  check after a red CI run is not "did the other run pass" but "did it *run*" — and a probe must never
  be more patient than the thing it vouches for, or it approves a start its subject then fails on.
- **The whole batch is the measurement, not the job that went red.** Reading run 836 alone says
  "mongod is flaky". Reading every run of that push says the host was 2–5x slower than the day before
  (`docker-test` 13–15 → 41.6 min, `devbuild` ~20 → 96.6, `docs` ~25–31 → 63.4, the container ITs
  5.3–8.8 → 18.1 and 23.3), and that nine runs had been queued at one instant. Every red in that batch
  was a deadline expiring, in four different places, for one reason. **When several unrelated guards go
  red at once, compare durations across the whole run list before diagnosing any of them** — it costs a
  single API call and it is the difference between fixing four tests and understanding one host.
- **A tool that detects a defect through a side effect can only see the share of it that has that side
  effect.** Qodana's `KDocUnresolvedReference` reports a doc block that has come loose from its declaration
  *only* when the stranded block happens to contain a `[link]` that no longer resolves — so it named **4 of
  the 18** instances in the repository on 2026-09-20 (22%), and the block holding the entire case for
  `ClientsideVerifier.propagateClientOnlyProof` was not among them. A structural scan for the defect itself
  — a doc block whose next non-blank line opens another doc block — found the rest, and found two more
  again when written as `KDocAttachmentTest` rather than as a throwaway script, because the script had
  silently skipped single-line blocks. Ask what a finding's *detection* depends on before treating its
  count as the size of the problem, and pin the defect by its own shape.
