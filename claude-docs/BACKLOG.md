# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

> **Numbering continues from the highest ID ever issued — it does not restart, and an ID is never reused.**
> A gap in this file means an item landed or was dropped, not that the counter reset; most IDs ever issued have
> no entry here any more, and some are still cited by name elsewhere in the repo, so reusing one silently
> repoints someone else's citation at the wrong item. `git log -S'B<n> —' -- claude-docs/BACKLOG.md` recovers
> what any past ID meant, and is also how to find the highest one rather than trusting a number written here.

Add the next item under a dated section, starting at **B54**, with the reason it waited and enough context to pick
it up cold. **B53 is issued and gone** — `quasar dev` dying on `process is not defined`, closed
2026-10-23 together with the production half of the same defect (`vueRouterMode` silently ignored); see
`REFACTOR-LOG.md`. **B47 is issued and gone** — `resolve-version`'s `branch` mode carrying a branch's slashes into the
version it outputs, closed 2026-10-16 in the same pass that found it; see `REFACTOR-LOG.md`. **B36 is issued and gone** — Sinytra Connector as a boot strategy, dropped 2026-09-12 when the
per-line axis made the shim cost a whole Minecraft line and the placeholder was redirected to Fabric instead;
see `REFACTOR-LOG.md`. **B38, B39 and B40 are issued and gone** — the Qodana work, all three closed
2026-09-21; B40 deliberately *not* by the baseline it proposed, see `REFACTOR-LOG.md`.

## 2026-10-21 — from the seven-round audit

### B52 — `start.fish` swallows every stop request, and the obvious fix breaks the exit code

`serverpackcreator-api/src/main/resources/de/griefed/resources/server_files/default_template.fish`

The bash template traps `TERM` and forwards it to the server; **fish has no handler at all** and runs
the server in the foreground, so there is nothing to forward from. `stopServer` is the only function in
`default_template.sh` that is absent from `default_template.fish`. The consequence is the one bash's own
comment spells out: under docker or systemd the script is PID 1, the kernel discards an unhandled signal
aimed at PID 1, the stop window expires, and **the server is SIGKILLed with its world unsaved**.

**Reproduced** with fish as PID 1 in `alpine:latest`, the template's own `runJavaCommand` verbatim and a
stand-in server that traps `TERM`: after `docker kill -s TERM`, the child never heard it and the
container was still running. The recorded bash figures for the same shape are *5096 ms, exit 137*
against *72 ms, exit 0* once forwarding works.

**Why it waited:** four shapes were executed against a real fish and **each satisfies one half of the
contract and breaks the other**:

| shape | forwards SIGTERM | preserves the server's exit code |
|---|---|---|
| foreground (today) | no | yes (42) |
| background + `wait` | no — the handler does not run while fish is blocked in `wait` | untested |
| background + poll loop | **yes** (`CHILD-GOT-TERM`, ~1 s) | **no** — `$status` is `0`, not `42` |
| background + poll + status file | handler did not fire | yes (42) |

The exit code is not optional: `runJavaCommand`'s `$status` becomes `server_exit_code`, and
`serverpackcreator-api/CLAUDE.md` records why — systemd, Docker restart policies, CI and the grinder's
boot classifier all read it. A fix that forwards the signal and zeroes the exit code swaps one silent
defect for another, which is exactly what the shell-template convention exists to prevent.

**To pick it up cold**, two facts are already established and need not be rediscovered:

- **fish does not dispatch an `--on-signal` handler while blocked in `wait`.** A poll loop between
  commands does let it fire. This is why bash's trap-plus-`wait` shape does not translate.
- **fish does not redirect a background job's stdin to `/dev/null`**, unlike bash — so the fish fix
  needs no equivalent of bash's `0<&0`, and the console survives backgrounding (verified in every
  backgrounded shape).

The open question is a shape that forwards the signal *and* returns the child's real status. Candidates
not yet executed: `$last_pid` semantics for a `begin ... end &` block (the status-file shape's handler
did not fire, and whether that is a wrong pid or a dispatch problem was not isolated); fish's
`--on-process-exit`; or capturing the status inside the handler rather than after the loop.

The harness is reproducible in about a minute:
`docker run -d -i -v <dir>:/t alpine:latest sh -c 'apk add --no-cache fish && exec fish /t/harness.fish'`,
then `docker kill -s TERM <id>` and read `docker logs`. Run the container **without** `--rm` or the exit
evidence is removed with it. `ScriptTemplateContentTest.theBashTemplateForwardsATerminationSignalToTheServerAndKeepsItsConsole`
is the executed guard to mirror; `TemplateInterpreterRunner` is how this repository reaches fish when it
is not installed locally.

**`start.bat` and `start.ps1` have no handler either**, and are deliberately out of this item's scope:
Windows does not deliver SIGTERM, so the equivalent question there is a different one.

## 2026-10-20 — from the documentation-policy pass

Three dormant GUI features were kept alive as commented-out code — 21 blocks, 270 lines across three
files — which the root `CLAUDE.md` has always forbidden and which the new documentation policy forbids
again. The code is **deleted**; what it was and how to revive it is recorded here, per *the history is
not lost, it moves to where it is looked up*. Recover any of it with
`git log -S'<symbol>' -- <path>` on the commit that removed it.

### B49 — the install4j in-app updater in `UpdateDialogs`

`serverpackcreator-app/.../gui/window/UpdateDialogs.kt` carried **10 commented blocks, 200 lines**:
three `i4J*` state flags and six private functions — `checkForUpdateI4J`, `isI4JUpdatable`,
`checkForUpdateWithApi`, `downloadAndUpdate`, `setProgressText`, `executeUpdate` — plus the three live
call sites that would have branched on them (the dialog's button label, the `0 ->` action, and the
`checkForUpdate` entry point). What ships instead is the one surviving branch:
`webUtilities.openLinkInBrowser(update.get().url().toURI())`.

It launched install4j's *Standalone update downloader* (`ApplicationLauncher.launchApplication("442", …)`)
and its updater application (`"380"`), read `sys.updatesUrl` through `Variables.getCompilerVariable`, and
gated itself on `Files.getFileStore(installationDir).isReadOnly` plus `Util.isWindows()/isMacOS()/isLinux()
&& !isArchive()`.

**Why it waited:** it only ever works for users who installed from install4j **media**, which is one of
four distribution shapes this project ships — the AppImages, the Windows app-image and the plain jar
cannot use it, and the AppImage is explicitly excluded by that `!Util.isArchive()` check. Reviving it
means deciding what the other three do, and the fallback it would replace (open the release page in a
browser) already works for all four.

**To pick it up cold:** `media` needs install4j installed locally and is not part of the dev loop, so
there is no way to rehearse this without a licensed build — which is also why the code sat commented
rather than behind a feature flag. The two application ids above are the install4j *Installer → Screens
& Actions* ids, found with the IDE's "Show IDs" toggle; they are the only part not re-derivable from
install4j's own documentation.

### B50 — the font-family picker in `GuiSettings`

`serverpackcreator-app/.../gui/window/settings/GuiSettings.kt` carried **7 blocks, 39 lines**: the
`fontIcon`/`fontLabel`/`fontSetting`/`fontRevert`/`fontReset` widget row over
`GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames`, its `loadFontProperties()`
loader, its `panel.add(…)` layout rows, its `saveSettings` write of
`FontUIResource(fontSetting.selectedItem.toString(), font.size, font.style)`, and its clause in the
dirty-check.

**Why it waited:** the setting is half-present in the shipped product. `guiProps.fontSize` is live and
adjustable; only the *family* is not, and the reset button hard-coded `"JetBrains Mono"` as the default
rather than reading it from `GuiProps`. The translation keys `settings_gui_font_family_label` and
`settings_gui_font_family_tooltip` still exist, so the UI strings are already written.

**To pick it up cold:** the open question is whether changing font family needs the restart warning the
neighbouring `manualEditSetting` shows (`settings_gui_restart_manualedit`), since Swing's `FontUIResource`
is applied through the look-and-feel defaults and existing components do not all re-read it.

### B51 — the Java-executable path setting in `GlobalSettings`

`serverpackcreator-app/.../gui/window/settings/GlobalSettings.kt` carried **4 blocks, 31 lines**: the
`javaIcon`/`javaLabel`/`javaSetting`/`javaRevert`/`javaReset`/`javaChoose` row over
`ScrollTextFileField` and `JavaChooser`, its layout rows, its validation (file exists, readable,
executable → `settings_check_java`), and its clause in the dirty-check.

**Why it waited:** `ApiProperties.javaPath` and `acquireJavaPath()` are live and still back the setting;
only the GUI for overriding it is absent, and the server packs resolve Java through the start scripts'
own `install_java` path at runtime, which is the mechanism that made the override less load-bearing than
it looks. `JavaChooser` and the `settings_global_java_*` translation keys all still exist.

**To pick it up cold:** decide whether this setting is global at all — the start-script templates track
Mojang's per-Minecraft `requiredJavaVersion` (see `misc/Java-References.md`), so one global Java path can
be wrong for a given pack, which is the reason to prefer the scripts' own resolution over an override.

## 2026-10-11 — from the documentation and call-depth pass

### B46 — `checkConfiguration` downloads a CurseForge thumbnail

`ConfigurationHandler.checkConfiguration` reaches `WebUtilities.downloadFile` at depth 13:

```
checkConfiguration -> isZip -> checkManifests -> ModpackManifestParser.checkManifests
  -> updateConfigModelFromMinecraftInstance -> getAndSetIcon -> WebUtilities.downloadFile
```

`getAndSetIcon` fetches the pack's icon from a third-party host. A call whose whole contract is *check
this configuration* therefore performs network I/O against CurseForge, and an embedder calling the
published `checkConfiguration` has no way to know that from its signature or its documentation.

**It is not a hang** — the call is bounded by `NetworkConfig`'s timeouts, and the manifest landmine in
`serverpackcreator-api/CLAUDE.md` covers the rest of that path — so this is a responsibility leak
rather than a defect. It is also on the GUI's config-check timer, which is why
`ConfigEditorViewModel` memoises around it.

**Why it waited:** splitting the fetch out of validation is a behaviour change on published API —
`checkConfiguration` currently *populates* `packConfig` as a side effect, and the icon is part of what
it populates, so a caller relying on the icon being set after a check would stop getting it. That
belongs in a major, with a row in `API-BEHAVIOUR-CHANGES.md`.

**To pick it up cold:** the decision is whether `getAndSetIcon` moves out of `checkManifests` into an
explicit "enrich from manifest" step a caller opts into, or whether validation keeps it and the
documentation simply says so. Found by `misc/call-depth.py`; `claude-docs/CALL-DEPTH.md` has the full
chain and the measurement.

## 2026-10-03 — from the AppImage portability pass

### B45 — bundle glibc so the AppImage runs on musl and non-FHS systems

The AppImages were reported as not self-contained, "references glibc 2.15". Measured against the JDK
the build bundles (Temurin `jdk-21.0.12.1+1`, every ELF scanned): the highest symbol version really is
**`GLIBC_2.15`**, raised by `lib/libjava.so` and `lib/server/libjvm.so`. The AppImage runtime
contributes none — it is `type2-runtime`, static-pie — and the JDK ships no libc, libstdc++ or libgcc
of its own, so the bundled JDK alone sets the floor.

A maintained toolchain for closing it exists: **`sharun` + `uruntime`** (pkgforge-dev's
*Anylinux-AppImages*), which bundles the loader, glibc, NSS, gconv and the dlopened libraries, and
packs with DwarFS rather than squashfs — plausibly *shrinking* the current 256 MB artifact.

**Why it waited** (Griefed's call, 2026-10-03, with the measurement in hand): the floor is already
March 2012, so what it buys is musl systems (Alpine, Void-musl), non-FHS ones (NixOS, GUIX) and
pre-2012 glibc, not the mainstream. Three things argue against doing it now:

- **glibc alone would not make the artifact self-contained.** `libawt_xawt.so` needs host
  `libX11.so.6`, `libXext.so.6`, `libXi.so.6`, `libXrender.so.1` and `libXtst.so.6`; `libjsound.so`
  needs `libasound.so.2`. Bundling the C library moves the floor without removing the dependency class.
- **`libnet.so` calls `getaddrinfo`**, and NSS is where bundled-glibc builds characteristically break:
  glibc dlopens `libnss_*.so.2` matching the *running* glibc, and a host `/etc/nsswitch.conf` can name
  modules the bundle does not carry. ServerPackCreator's whole job is downloading modpacks.
- The cheap half landed instead and removes the way this failed silently: the JDK is pinned, the floor
  is asserted at build time, and the launcher now errors instead of quietly using the system's Java.

**To pick it up cold:** the measurement, the method to re-run it and the full host-library list are in
`misc/AppImage-Portability.md`. The decision to revisit is a report from an actual musl or NixOS user,
or a wish to drop the artifact's size — not the glibc number on its own, which is already as low as a
prebuilt Temurin goes.

## 2026-09-25 — from the server-test plugin branch

> Both defects this branch found were **fixed** on it, so they are not listed here. What is listed is
> the residue each fix deliberately left behind. **B44 was then fixed too**, on the same branch, and is
> kept below as a closed entry rather than deleted — the reason the fix went wider than the entry
> proposed is worth more than the entry was.

### B43 — `ExtensionTab.log` still names `AddonsLogger`, not `PluginsLogger`

`log4j2.xml` now declares an `AddonsLogger` routed to the plugins appender, so a plugin's output finally
lands in `plugins.log` where the example plugin's KDoc always claimed it did. The *name* was left alone
on purpose: `AddonsLogger` is baked into every third-party plugin already compiled against the published
API, so renaming it would move their output a second time and break anything filtering on the logger
name. Two names for one appender is the cost.

**Why it waited:** collapsing them is a behaviour change on published API and belongs in a major, beside
whatever else moves then. **To pick it up cold:** change `ExtensionTab.log` and
`ExtensionConfigPanel.pluginsLog` to `LogManager.getLogger("PluginsLogger")`, drop the `AddonsLogger`
block from both `log4j2.xml` copies, and update `PluginLoggingRoutingTest` — which asserts the *resolved
appenders*, so it keeps its teeth either way. Add a row to `API-BEHAVIOUR-CHANGES.md`; there is already
one for the addition.

**Second half, easy to miss:** `ApiProperties.init` rewrites the home's `log4j2.xml` only
`if (!log4jXml.isFile || devBuild || preRelease)`. An existing *stable* installation keeps its own copy
and never sees either change until that file is deleted. If this is ever worth migrating, the app's
`MigrationManager` is where it belongs.

### B44 — `ApiPlugins.addTabExtensionTabs` calls `getTab` outside its own try-block — **ISSUED AND GONE**

Closed 2026-09-25 on the same branch that filed it, at Griefed's request. The fix went wider than the
entry proposed: `name`, `title`, `icon` and `tooltip` are third-party code on that path too, and were
outside the guard alongside `getTab`. The whole registration is now inside it, the body lives in an
**internal** `addTabExtensionTab` so the containment could be pinned without a fixture jar, and the
failure message reads the extension's `name` through a guard of its own — building it eagerly is what
put a `name` call outside the try in the first place. See `TabExtensionFailureContainmentTest` and the
row in `API-BEHAVIOUR-CHANGES.md`.

## 2026-09-23 — from the modpack upload/check/storage pass

### B41 — the web service's extracted modpacks share a parent, and `checkManifests` reads it

**What:** `ModpackManifestParser.manifestCandidates` consults three files in the modpack's **parent**
directory — `instance.json`, `mmc-pack.json`, `instance.cfg` — because a MultiMC/Prism instance keeps
them beside its `.minecraft` folder. The web service extracts every upload to
`<modpacksDirectory>/<fileID>`, so that parent is `<modpacksDirectory>`, shared by every upload. Any of
those three names landing there would be read by every later upload carrying no manifest of its own,
silently overwriting its Minecraft version, modloader and loader version with another pack's.

**Why it waited.** Latent, not live: nothing in the upload path writes a file with one of those names
into `modpacksDirectory`. The landing copy is `<millis>-orig-<name>`, the committed archive is
`<objectId>.zip`, the extraction is a directory, and the one route that could have produced an
arbitrary name — the unsanitised upload filename — is now closed and never wrote outside the root
anyway. Meanwhile both cheap fixes cost something real: refusing parent-directory manifests outright
breaks MultiMC/Prism detection for the GUI and CLI users those three entries exist for, and a
heuristic ("only look at the parent when the modpack directory is named `.minecraft`") is a guess with
no evidence behind it.

**Pick it up when** the web service gains a path that writes a user- or operator-named file into
`modpacksDirectory`, or someone reports a modpack detected with another pack's Minecraft version. The
shape that generalises is to make the **caller** state whether a parent lookup is meaningful:
`checkManifests` cannot currently distinguish "this directory is an instance folder" from "this
directory is one of many extraction targets", and that distinction is the fix. Note `manifestCandidates`
is published API with an `API-BEHAVIOUR-CHANGES.md` row and the GUI keys a memo on it, so changing its
shape is not free.

### B42 — the web API's public surface is unauthenticated by policy, not by oversight

**What:** every controller carries `@CrossOrigin(origins = ["*"])`, Spring Security is not on the
classpath at all, there is no CSRF token and no rate limiting. `GET /api/v2/serverpacks/vote/{id}&{vote}`
mutates state on a GET, so any page can move the counter with an `<img>` tag. `/api/v2/modpacks/all`,
`/serverpacks/all`, `/runconfigs/all` and `/events/all` return whole collections, and `queueEvent`,
`modPackDownload`, `serverPackDownload` and `runConfiguration` grow without a TTL or any retention
policy — `queueEvent` gains several rows per upload, forever, with no index on the fields it is
queried by.

**Why it waited.** None of it is a defect in the sense the rest of this pass dealt with; each is a
deliberate-looking product decision that only Griefed can make. Adding authentication changes who can
use a public instance at all. Making `vote` a POST breaks any existing client. Deleting old packs on a
schedule is a data-retention promise to the people who uploaded them. Rate limiting needs a number
nobody has measured. Writing any of these in without that call would be choosing a product direction
under cover of a bug fix.

**Pick it up when** there is a decision on each. What *was* fixed in the meantime is the part that is
unambiguously a defect regardless of the policy: an anonymous caller no longer receives stack traces or
absolute server paths, no longer costs the server three writes and a GridFS document for an upload that
is then rejected, and can no longer steer the path an upload is written to.

## 2026-09-11 — from the UNVERIFIABLE pass

### B37 — search-then-confirm for a mod id no registry resolves

**What:** when a required manifest id resolves nowhere, search Modrinth's `/v2/search` faceted by loader and
game version, download the best candidate, read its **own** mod id, and accept it only if it matches —
feeding `LearnedModIds` so the cost amortises across candidates.

**Why it waited.** Three cheaper routes landed first and cover the measured rows: the fork table
(`create` → `create-fabric`, `tacz` → `timeless-and-classics-guns`), the cross-platform fallback, and
`askLinkedProjects`, which already does "probe, learn, re-plan" for an id the *page* links. What is left for
a search is an id that is declared by a jar, linked by nobody, absent from the registry and absent from both
platforms' slug namespaces — a set this pass produced no instance of. Building the machinery for it now
would be a guess at demand, and a name-only match that skipped the confirm step would stage the wrong mod.

**Pick it up when** a refusal names an id that none of the four routes above reach. The confirm step is not
optional: `askLinkedProjects` is the template — never trust a name, download and read the id.

## 2026-10-12 — from the `9.1.0..develop` audit, iteration 1

### B38 — the homepage's eslint 8 island, which nothing runs

**What:** `serverpackcreator-homepage/` carries `.eslintrc.js`, `.eslintignore` and `eslint 8.37.0` with
`@babel/eslint-parser`, `eslint-config-prettier 8.6.0`, `eslint-plugin-vue 9.8.0` and
`eslint-webpack-plugin 4.0.0`, plus a `"lint": "eslint --ext .js,.vue ./"` script. Migrate it to the flat
config and the eslint the rest of the repository is on, or decide deliberately that the homepage is not
linted and delete the config, the script and the five dependencies.

**Why it waited.** It is **inert, not wrong**. No workflow invokes it — `grep -rn homepage
.forgejo/workflows/*.yml | grep -i lint` finds nothing — and `"test"` is
`echo "No test specified" && exit 0`, so neither can fail a build or mislead a reader the way the deleted
`CI-NOTES.md` could. The audit iteration it was found in was fixing things that break the release, and
this breaks nothing.

It is worth doing because the repository now holds **both** eslint generations:
`build(deps-dev): bump eslint 9.39.5 -> 10.12.0, and @eslint/js with it` has just moved
`serverpackcreator-web-frontend` to eslint 10 flat config, and that module's `CLAUDE.md` records that its
legacy `.eslintrc.cjs`/`.eslintignore` "are gone". Two generations of one tool, one of them unreachable,
is the duplicated-knowledge shape this project has paid for four times.

**Pick it up when** the homepage SPA is touched for anything else, or when a dependency alert names one of
those five packages — at which point the honest answer is the migration rather than a bump of a linter
nobody runs. Note the homepage is Quasar 1 / webpack (`quasar.conf.js`, `babel.config.js`), not the
app-vite 3 the other frontend uses, so its flat config is not a copy of that one.

## 2026-10-16 — from the slashed-`-Pversion` fix

### B48 — move `buildSrc/` to a `build-logic` included build

**What:** convert `buildSrc/` into an ordinary included build (`pluginManagement { includeBuild("build-logic") }`)
so the root `check` can depend on `gradle.includedBuild("build-logic").task(":check")` and the build logic's
tests become part of one honest gate.

**Why it waited.** The present wiring works and is pinned — `buildSrc/build.gradle.kts` finalizes `jar` with
`test`, and `./gradlew help` runs the suite in ~3 s — but it is a *workaround* for Gradle 8.0 having stopped
running buildSrc's lifecycle tasks ([gradle#25439](https://github.com/gradle/gradle/issues/25439), closed as
intended). Hanging a test off a jar is not what either edge is for, and the obvious alternatives are all dead
ends on 9.8.1: `:buildSrc:check` is inert, `gradle.includedBuild("buildSrc")` does not resolve, and
`-p buildSrc check` fails in buildSrc's own settings, which declare the foojay resolver with no version
because only the root build puts it on the classpath. The conversion touches `settings.gradle.kts`, both
settings files' catalog and toolchain wiring, and anything that reaches buildSrc classes directly — far
beyond the one-goal commit that introduced the sanitiser.

**Pick it up when** buildSrc grows a second kind of verification (a TestKit harness, or a check that must run
on its own in CI), at which point the workaround stops carrying its weight. Note the foojay asymmetry is the
first thing to re-solve: an included build evaluates its settings standalone, so the resolver needs a version
there, which is exactly what fails today under the root build.

## 2026-10-17 — from merging the open renovate PRs

### B49 — the homepage's Quasar toolchain caps below the `quasar` CVE fix, and the way out is Vite

**What:** `serverpackcreator-homepage` cannot take PR #715 (`quasar` 2.11.10 → 2.32.2, closing
CVE-2026-73647 / GHSA-3r53-75j5-3g7j, prototype pollution in `extend()`). The advisory's
`first_patched_version` is **2.22.0**; the homepage's `@quasar/app` 3.3.3 cannot build anything
above roughly 2.14.5. Fixing the CVE means migrating the build, and the only viable target is
`@quasar/app-vite` — not `@quasar/app-webpack`.

**Measured, so it does not need re-deriving:**

- `@quasar/app` 3.3.3 + `quasar` 2.32.2 fails first in sass — `SassError: expected ")"` on
  `&:has(+ :is(…))` in `quasar/dist/quasar.sass`, because 3.3.3 pins `sass` **1.32.12** (April 2021)
  exactly. An npm `overrides` of `sass` clears it and reveals the real wall underneath.
- The real wall is JS: `TypeError: importTransformation is not a function`, from 3.3.3's
  `lib/webpack/loader.js.transform-quasar-imports.js`. Reproduced at `quasar` **2.17.0** and
  **2.22.0**; **2.14.5** still builds. So the ceiling sits between 2.14.5 and 2.17.0, below the fix.
- **`@quasar/app-webpack` 4.4.5 is not the way out**, even though its peer range (`quasar ^2.16.0`)
  would allow 2.32.2. The site uses `<q-flashcard>` across the whole of `src/pages/Index.vue`, and
  `@quasar/quasar-app-extension-qflashcard` ships **no webpack-4-compatible release**: 2.0.0-alpha.4
  declares `@quasar/app-webpack ^3.0.0`, and 3.0.0 declares `peerDependencies: {"@quasar/app-vite":
  ">=3.0.0"}`. The CLI refuses outright — *"Extension(@quasar/qflashcard): is not compatible with
  @quasar/app-webpack v4.4.5. Required version: ^3.0.0"*.
- Node is **not** a blocker, but it is tight. `@quasar/app-vite` 3.10.3 wants
  `^30 || ^28 || ^26 || ^24 || ^22.22.0`, and all three places that build this site clear it only just:
  `homepage-build` in `.forgejo/workflows/docs.yml` pins `node-version: '22'`, and the Dockerfile's
  builder `griefed/gitlab-ci-cd:2.8.0` ships **Node v22.22.0 exactly** (verified with
  `docker run --platform linux/amd64 … node -v`; it also carries a global `@quasar/cli` **v2.5.0**,
  which is what its `RUN quasar build` actually invokes). One patch release of the floor breaks the
  image build, so the migration should pin a newer Node in that stage rather than rely on it.

**What a prototype of the Vite migration cost, and where it stopped.** Built in a throwaway worktree:
`quasar.conf.js` → `quasar.config.js` in app-vite form; root `index.html` with
`<!-- quasar:entry-point -->`; `.postcssrc.js` → ESM `postcss.config.js`; `babel.config.js` deleted;
`quasar/wrappers` → `@quasar/app-vite` (`defineBoot`, `defineRouter`); every `~assets/…` and
`url("~assets/…")` rewritten to the single `@` alias app-vite 3 generates; `import('layouts/…')` →
`import('@/layouts/…')`; and the three `require(\`assets/serverpackcreator/cards/${…}.png\`)` sites
in `Index.vue` — which Vite has no equivalent for — replaced with an `import.meta.glob` lookup.
**`quasar build` goes green from there**, emits all nine card images and a `dist/spa/index.html`,
and most of the page renders. It is still **not shippable**: the production build throws
`cardImage is not a function` nine times, no flashcard receives the `styleTop`/`styleBottom` inline
width, and `quasar dev` serves a **blank page**. The unresolved question is why the compiled render's
`$options`-class accesses (`c.styleTop`, `c.hoverOver`, `c.cardImage` alike) do not resolve while
`$data` and `$setup` ones do.

**Why it waited.** Three reasons, in order of weight. It is a **webpack → Vite migration of the public
website**, not the toolchain bump the PR looks like. The homepage has **no test suite** — `npm test` is
`echo "No test specified" && exit 0`, and this module's `CLAUDE.md` says to treat a change to `src/` as
verified only by looking at it — so the only gate is a human comparing the rendered site against
serverpackcreator.de. And `serverpackcreator-homepage` is a **squashed subtree** of
`Griefed/serverpackcreator-de`: a change of this size landed here and not upstream collides with the
next `git subtree pull --squash`, so it belongs in the website's own repository, pulled back as one
squashed commit.

**Pick it up when** the site is worked on deliberately, in `Griefed/serverpackcreator-de`. It also closes
B47, whose eslint-generation note is a symptom of the same staleness. Until then PR #715 stays open and
the advisory stays open with it; the exposure is `extend()` prototype pollution in a **static marketing
SPA with no user input**, which is why leaving it open for one deliberate migration is defensible rather
than merely convenient.

**Dependabot PR #1310 is the same wall, and is not mergeable here either (checked 2026-10-18).** It is
`build(deps): Bump the npm_and_yarn group across 1 directory with 20 updates`, against **`main`**, and
its three direct bumps are `@quasar/cli` 5.0.4, `axios` 1.20.0 and `quasar` 2.32.2 — the first two of
which renovate already merged into `develop` as #716 and #714. All it adds over `develop` is the quasar
bump this entry exists to refuse. Its *lockfile* cannot be taken either, and that is the part worth
recording: it is computed against `main`, so applying it to `develop` upgrades 57 packages and
**downgrades 19**, among them `@quasar/extras` 1.18.0 → 1.16.2, `core-js` 3.50.0 → 3.27.2, `eslint`
8.57.1 → 8.37.0 and `js-yaml` 4.3.2 → **4.1.0**, which is inside the advisory range `4.0.0 - 4.3.1`.
Taking it would re-open a vulnerability to close others. The equivalent that *is* safe is regenerating
`develop`'s own lockfile — done, see the commit of this entry.

**Expect the same shape from every future npm PR raised against `main`.** `main` is three releases
behind `develop` here, so a bot PR's lockfile is always rebased on the wrong base; read the direct-dep
table against `develop`'s `package.json` before spending time on the lockfile. And `npm audit` says what
this entry says, independently: `@quasar/app` is reported `critical`, `range=*`, **`fixAvailable=false`**
— npm agrees there is no bump out of this, only the migration.

### B50 — the container has no seam for a remote debugger

**What:** `docker/root/etc/s6-overlay/s6-rc.d/svc-spc/run` launches the jar with exactly one JVM
flag (`-Djava.io.tmpdir`) and no passthrough for any other. There is no way to attach IntelliJ to
the application running inside the image, because there is nowhere to put
`-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`. Adding a
`JAVA_TOOL_OPTIONS`-style passthrough plus an opt-in `5005` publication on
`docker-compose-dev.yml`'s `app` profile would close it.

**Why it waited.** The dev loop that landed on 2026-10-17 debugs the application **in IntelliJ**
against a containerised MongoDB, so the common case needs no container debugger at all. The
remaining case is narrow but real: a defect that only reproduces under the image — the s6 init
script, `overrides.properties` composition, the LSIO `PUID`/`PGID` remapping, the
`server.tomcat.basedir` the script appends. Today the only tool for those is log statements.

The change is also not free. `svc-spc/run` is one of the two files behind
`docker/tests/init-spc-config-test.sh`'s sibling contract, and its argument order is already
fragile: it is `java -jar -Djava.io.tmpdir=… <jar> -web --home …`, which works only because HotSpot
keeps parsing VM options until the first non-option. Inserting a variable expansion into that line
without a test is how the next silent container defect gets written, and shell templates are
exactly the category the root `CLAUDE.md` requires to be pinned first.

**Pick it up when** something has to be debugged inside the image. Write the guard into
`docker/tests/` first — it already runs the real script in the production base image, so the
pattern exists — and assert both that the flag appears when the variable is set and that the
command is unchanged when it is not.

### B51 — the image build needs more than 3.9 GB, and `:generateLicenseReport` is where it peaks

**What:** `docker compose -f docker/docker-compose-dev.yml --profile app build`, and any plain
`docker build -f docker/Dockerfile .`, cannot complete on a Docker Desktop given 4 GB. The builder
stage's Gradle run exhausts the VM and BuildKit reports `cannot allocate memory`.

**Measured 2026-10-17**, sampling `/proc/meminfo` from a privileged container every 5 s across a
full attempt (79 samples): `MemTotal` 3917 MB, `MemAvailable` **3444 MB at the start and 10 MB at
the trough** — a peak of 3907 MB of 3917 MB, with 10 MB of headroom left. It does not merely exceed
the budget, it saturates it.

**The task is consistent.** Three separate attempts after the `.forgejo` context fix all died at
`:generateLicenseReport`, immediately after `:serverpackcreator-api:compileKotlin`. (A fourth,
earlier attempt died at `:buildSrc:test`, for the unrelated `.dockerignore` reason that commit
`fix(docker): excluding `.forgejo/` from the build context broke the image build` repaired.)

**Capping the heap does NOT help — this was tested, and it is the useful part of this entry.** The
obvious theory is `gradle.properties`' `org.gradle.jvmargs=-Xms128m -Xmx4096m`: a 4 GB heap request
inside a 3.83 GB container, which cannot fit by construction. It is the wrong theory. A build with
`-Dorg.gradle.jvmargs="-Xms128m -Xmx1280m" -Dkotlin.daemon.jvmargs="-Xmx768m"` failed at the
**identical task** with the identical error, simply taking longer to get there (417 s against
293 s) from the added GC pressure. So the consumption is not the Gradle JVM's Java heap, and a
`docker/Dockerfile` heap flag buys nothing. Do not re-derive this; it costs seven minutes a run.

`org.gradle.parallel` is not set and `org.gradle.daemon=false`, so this is not worker concurrency
either. The remaining candidates, untested: the license-report plugin resolving every configuration
in every module at once, the Node toolchain the frontend build downloads and runs, and BuildKit's
own resident set — plausibly all three alive at the same moment.

**Why it waited.** It is a *host configuration* problem as far as anyone can currently tell — CI
builds this image on every push — and the local loop that matters does not need it: `./gradlew
build` on the host completes in about two minutes, and `docker-compose-dev.yml` defaults to
dependencies-only precisely so the image is never on the fast path. Raising Docker Desktop's memory
again also restarts the daemon, which on this machine bounces the containers serving DNS.

**Pick it up when** the image must be built locally. Start by giving Docker Desktop 8 GB and
re-running the measurement above — the trough value tells you immediately whether the requirement
is merely above 3.9 GB or far above it, which is the number this entry cannot supply. Only if 8 GB
also fails is it worth attacking `:generateLicenseReport` itself, and in that case measure which of
the three candidates above actually holds the memory before changing anything: two theories have
already been wrong here.
