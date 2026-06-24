# Deferred-Work Plan

> Roadmap for the items deliberately deferred during the test-coverage push and the quality
> analysis (see `TEST-COVERAGE-AUDIT.md`). Each entry is one mergeable branch in the
> **branch → work → merge-into-develop → delete-branch** workflow. Behavior-preserving unless a
> PR's "Goal" says otherwise; suites must be green at every merge.
>
> Status legend: ☐ not started · ◐ in progress · ☑ done.

## Suggested order

1. **PR1** MockK enabler + network coverage — quick enabler, independent.
2. **PR2** ignored-catch documentation pass — low risk.
3. **PR3** versionmeta URL/tag-name externalization — bounded refactor.
4. **PR4** detekt + baseline + `!!` guard — ⛔ **deferred / blocked on upstream**: no stable detekt
   supports Kotlin 2.3.x (see PR4 below).
5. **PR5** remove 6.0.0-deprecated facades — **hold for the next major (9.0.0)**.
6. **PR6** `java.awt.Desktop` → adapter — optional, low priority.

PR1–PR3 are independent and reorderable. PR4 benefits from following the cleanups. PR5 is the only
one with an external (release) gate.

---

## ☑ PR1 — MockK enabler + network-bound coverage
- **Branch:** `claude-mockk-network-coverage` *(merged into `develop`)*
- **Goal:** unlock the coverage deferred in P7/P9 (network-bound branches).
- **Scope:** add MockK as `testImplementation` to `serverpackcreator-api/build.gradle.kts`
  (+ version-catalog entry); new tests stubbing `WebUtilities`/`VersionMeta` to drive
  `ServerPackProvisioner.serverDownloadable` (per-loader reachability),
  `getImprovedFabricLauncher` (launcher-present branch) and
  `ModpackManifestParser.getAndSetIcon` (download success/fail).
- **Approach:** hand-build the unit-under-test with mocked collaborators; no live HTTP. TDD.
- **Risk:** low (test-only dependency). **Size:** M.
- **DoD:** provisioner/manifest branch% up; api + app suites green; zero production change.
- **Note:** the app module already uses `springmockk`; the api module currently has no mocking lib.

## ☑ PR2 — ignored-catch documentation pass  *(merged into `develop`)*
- **Branch:** `claude-ignored-catch-comments` (two slices/commits: **api ~48**, **app ~12**)
- **Outcome:** every silent swallow now carries a why-comment (sites that already had real handling
  — `KeyComboManager`, `WebserviceSettings`, the `*Config` legacy-skip catches — were left as-is).
  No swallow was found to hide a real bug. Behavior-preserving (comments only).
- **Goal:** satisfy the error-handling convention — every swallowed exception carries a why-comment
  or gets real handling.
- **Scope:** the ~60 `catch (_/ignored)` sites. Per site: add an intent comment, or convert to
  proper handling. **Any swallow that turns out to hide a real bug is flagged separately, not
  silently commented.**
- **Risk:** low, but watch for latent bugs. **Size:** M (mechanical, high file-count → slice by
  module to respect the no-sprawl rule).
- **DoD:** no unexplained ignored catches; suites green.

## ☑ PR3 — versionmeta URL / tag-name externalization  *(merged into `develop`)*
- **Branch:** `claude-versionmeta-config`
- **Outcome:** all 59 TODOs cleared. Introduced `VersionMetaConfig` (internal object) centralizing
  every manifest URL, installer/launcher template and tag-name as named compile-time constants;
  each class sources its value from there. Done as a single cohesive slice (not per-loader) since
  the constants share one registry. Behavior-preserving — string-identical, guarded by `VersionMetaTest`
  + a new URL characterization test. **Note:** the constants are centralized but kept compile-time
  (not made runtime-overridable settings), since making manifest URLs user-configurable would not be
  behavior-preserving and is out of scope.
- **Goal:** clear the ~59 `// TODO Move URL/tagName to property` in `versionmeta/*`.
- **Scope:** move hardcoded manifest URLs and XML/JSON tag-names into named constants / a
  `VersionMetaConfig` group, mirroring the established settings-group pattern.
- **Approach:** **characterization tests first** (extend `VersionMetaTest` to pin currently-resolved
  URLs/values against the cached manifests), then extract behind identical behavior. URLs are
  load-bearing → behavior-preserving, verified offline.
- **Risk:** medium (a wrong edit breaks version resolution). **Size:** L → slice.
- **DoD:** no such TODOs remain; `VersionMetaTest` green; URLs/tag-names centralized.

## ☐ PR4 — detekt + baseline + `!!` guard
- **Branch:** `claude-detekt-baseline`
- **Goal:** stop `!!` / generic-catch debt from growing (api ~135 / app ~149 `!!` today).
- **Scope:** apply a detekt convention plugin (detekt is resolvable but **not** currently applied —
  no config, no convention block), commit a **baseline** to grandfather existing violations, and
  enable rules (`UnsafeCallOnNullableType` for `!!`, `TooGenericExceptionCaught`, etc.) so only
  *new* violations fail.
- **Sequence:** do **after PR2/PR3** so the baseline is smaller and meaningful.
- **Risk:** medium (build/CI noise). **Size:** M.
- **DoD:** `./gradlew detekt` runs in the build; baseline committed; new violations fail; documented
  in `CLAUDE.md`.
- **⛔ DEFERRED — blocked on upstream (decided 2026-06-24).** This project is on **Kotlin 2.3.20**, and
  **no stable detekt release supports Kotlin 2.3.x** — per the detekt compatibility table, stable
  `1.23.8` targets Kotlin 2.0.21 and is incompatible with Kotlin 2.3 metadata (detekt issue #8865);
  the only build supporting Kotlin 2.3.21 is **`2.0.0-alpha.3`**. Adopting an alpha static-analysis
  plugin into the build of a Maven-Central-published library was judged not worth the instability
  (alpha DSL/baseline format may change and break the build). **Revisit when detekt ships a stable
  release supporting Kotlin 2.3+** (track the 2.0.0 line), or reconsider a lightweight bespoke
  `!!`-guard if the debt grows. Sources: detekt.dev compatibility table; github.com/detekt/detekt/issues/8865.

## ☐ PR5 — remove 6.0.0-deprecated facades  ⚠️ release-gated
- **Branch:** `claude-remove-6x-deprecations`
- **Goal:** drop the 4 facades + migrate any internal callers to the `ReplaceWith` targets:
  - `ApiProperties.kt:381` → `defaultStartScriptTemplates()`
  - `ApiProperties.kt:388` → `startScriptTemplates`
  - `ScriptTemplatesConfig.kt:63` → `defaultScriptTemplateMap`
  - `ScriptTemplatesConfig.kt:115` → `startScriptTemplates`
- **Constraint:** removing published API is a **breaking change**. Per the API-compatibility policy
  (keep deprecated facades ≥ one major before removal) this must land in the **next major (9.0.0)**,
  not a minor. Current line is 8.x. **Prepare the branch now; merge only when the major is cut.**
- **Risk:** breaking for plugins if mis-timed. **Size:** S (code), timing-gated.
- **DoD:** facades gone, no internal callers, merged into the major release branch.

## ☐ PR6 — `java.awt.Desktop` → adapter  *(optional, low priority)*
- **Branch:** `claude-desktop-adapter`
- **Note:** **not** a stated-boundary violation — the module rule names Swing / Spring-web /
  frontend; `java.awt` is core JDK. Only worth doing for strict core purity. Invert the
  `WebUtilities` / `FileUtilities` `Desktop` calls (open browser/file) behind an api interface
  implemented by the app.
- **Recommendation:** skip unless desired.

---

## Out of scope here (release/process decisions, not engineering tasks)
- Cutting the next **major** version to land PR5 — release-management decision.
- Whether to publish a detekt report to CI — covered by PR4's DoD if wanted.
