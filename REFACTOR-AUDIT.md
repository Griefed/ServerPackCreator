# Refactor audit — `claude-readme-refresh` + `claude-drop-jpa-relics`

**Scope:** `git log develop..HEAD` from `claude-drop-jpa-relics` — **2 commits**, `d1ece9bee` …
`24390e3ba`. The two branches are stacked, so this range covers both.
**Mode:** READ-ONLY. No source was modified while auditing.
**Supersedes** the previous audit in this file (the modscanning one, merged into `develop`); its
findings were all resolved or explicitly accepted and are recorded in that history.

**Verdict: no HIGH findings. One MEDIUM, three LOW.** Both commits are small, single-purpose and
green. Neither is a refactor in the sense these conventions are written for — one is documentation,
the other is dead-configuration removal — so several rules do not bite here, and this report says so
rather than manufacturing findings to fill the sections.

---

## HIGH

None.

- No behaviour change is mixed into a commit labelled `refactor:` — neither commit uses that label.
- No module boundary is crossed. `-api` gains no dependency on Swing, Spring or the frontend.
- No plugin-API contract changes. `24390e3ba` touches only `-app` runtime configuration, the build
  file and the catalog; nothing `serverpackcreator-api` exports is affected.

---

## MEDIUM

### M-1 · `24390e3ba` removes main-source runtime configuration with nothing able to catch a mistake

**Files:** `serverpackcreator-app/src/main/resources/application.properties` (4 lines removed),
`serverpackcreator-app/src/test/resources/application.properties` (12 lines removed)
**Rule:** *Before refactoring any unit, ensure characterization tests exist that pin its current
behavior. Never refactor untested code blind.*

The commit removes `spring.transaction.default-timeout` and three `spring.datasource.tomcat.*` pool
settings from the **main** properties — live web-service configuration, not build logic — and no test
in the repository asserts on any of them:

```
spring.jpa          test-source hits: 0
spring.datasource   test-source hits: 0
spring.jdbc         test-source hits: 0
spring.transaction  test-source hits: 0
```

There is also no test that boots a real Spring context: `WebServiceTest` is
`@SpringBootTest(classes = [WebServiceTest::class])`, a context of exactly one class, which this
module's `CLAUDE.md` already describes as asserting nothing. So had one of these properties been
load-bearing, the suite would have stayed green regardless.

**Substantially mitigated, which is why this is MEDIUM and not HIGH.** The commit does not assert the
properties were dead, it measures it, and records the measurement in the message:

```
@Transactional in main source                            0 files
JPA / JDBC / DataSource types in main source             0 files
hibernate, tomcat-jdbc or h2 on the runtime classpath    0 hits
```

Nothing on the runtime classpath can consume a `spring.datasource.*` or `spring.jpa.*` key, so the
removal is inert by construction rather than by inspection. That is the same measure-don't-test
standard the root `CLAUDE.md` sets for build logic — the gap is that these files are *application*
configuration, where that carve-out was not written to apply.

**Suggested remedy:** none for the removal itself. The real gap is the pre-existing one this commit's
own documentation points at — `WebServiceTest` cannot catch anything. Replacing it with a context test
that actually starts is the fix, and is now *possible* precisely because this commit removed the
malformed Mongo URI that would have made such a test fail on startup. A follow-up, not a defect here.

---

## LOW

### L-1 · `24390e3ba` bundles documentation with the change, against this session's own pattern

**File:** `serverpackcreator-app/CLAUDE.md` (+12)
**Rule:** *One concern per commit.*

Every earlier documentation update across this session's branches landed as its own `docs(...)` commit
(`d76e2edad`, `a1d81990f`, `94b6a8c1b`, `24e00c5c7`, `5fc3ca33d`). Here the `CLAUDE.md` entry rides
along inside the `chore(web)` commit.

Defensible — the Definition of Done requires docs to move with the change, and 12 lines describing
exactly what was removed is not sprawl. Flagged only because the branch is internally inconsistent
about it, and consistency is what makes a log skimmable.

### L-2 · `24390e3ba` makes a latent behaviour change under a `chore:` label

**File:** `serverpackcreator-app/src/test/resources/application.properties`

Removing `spring.data.mongodb.uri=jdbc:h2:mem:testdb` changes what a future Spring context would
resolve: previously a hard startup failure on a malformed connection string, now the Mongo
autoconfiguration default (`localhost:27017`). Unreachable today — no test boots a context that runs
that autoconfiguration — so nothing observable changes, and the new behaviour is strictly the better
of the two.

Recorded because "chore" reads as *no behaviour anywhere*, while the honest description is *no
behaviour that anything currently reaches*. The commit body does explain this, which is most of what
matters.

### L-3 · `24390e3ba` carries a whitespace-only line in its diff

**File:** `serverpackcreator-app/src/test/resources/application.properties`, last line

The diff shows `spring.config.import=…` deleted and re-added identically. The file had no trailing
newline; the rewrite added one, so git renders the last line as changed (confirmed: one
`\ No newline at end of file` marker in the diff). Cosmetic, and arguably an improvement, but it makes
the diff look like it touched a property it did not.

---

## Checked and clean

- **`d1ece9bee` (README) is a clean single-concern commit.** One file, one concern — documentation
  staleness. Its six sub-items are instances of that concern, not six separate ones.
- **The compiler-gated snippets were not edited.** Verified by diff: no line touching `ApiWrapper`,
  `PackConfig`, `configurationHandler` or `serverPackHandler` changed, so `ReadmeExamplesTest`'s
  subject matter is untouched. It passes, as does `ShippedResourceTrackingTest`.
- **The README's claims were verified against the code, not eyeballed** — 17 of 17 `Mode.kt` arguments
  documented afterwards, all 16 internal links resolving, all 8 project-owned external links returning
  200, and every `de.griefed.serverpackcreator.*` / `SPC_*` key still present in source.
- **A bug found mid-task was surfaced and fixed in its own commit, not worked around.** The
  `spring.data.mongodb.uri=jdbc:h2:mem:testdb` relic was found while checking a stale README line,
  reported before being touched, and fixed separately from the README commit — exactly what the
  convention asks for.
- **No `refactor:` commit changed an existing test's assertions**, because neither commit is labelled
  `refactor:` and neither touches a test file at all (0 test files across both).
- **No Kotlin-idiom regressions**: neither commit modifies Kotlin source.
- **Scope stayed inside each commit's stated subject.** `24390e3ba` reaches into
  `gradle/libs.versions.toml` and `serverpackcreator-app/build.gradle.kts`, but only to drop the H2
  dependency and its catalog entry — the same "JPA/H2/JDBC relics" concern the message names.
- **Both branches follow the `claude-` naming rule**, and neither has been pushed.
- `:serverpackcreator-app:test` and `./gradlew clean build` are green on the branch tip.

## Follow-ups already known, not defects in this range

- `WebServiceTest` still boots a one-class context and asserts nothing. Both this module's `CLAUDE.md`
  and M-1 point at it; replacing it is the highest-value test work left in `-app`.
- The README's `-web` first-run wording was corrected to what could be verified, deliberately without
  asserting a specific error message, since that failure was never reproduced. If anyone does run it,
  the sentence can be sharpened.
