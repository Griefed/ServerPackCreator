# ServerPackCreator 9.0.0-beta.1

The first **beta** of the 9.0.0 line, and the first 9.0.0 build aimed at people who are not
already following the alphas.

Most of the work since 8.1.2 went into one question: **which mods are client-only, and how do we
stop guessing?** The answer has two halves, and they reach you very differently.

The first half is the automatic detection built into every server pack generation, and it got
substantially more accurate. **You get that one for free** — in the GUI, the CLI and the web UI
alike, without doing anything.

The second half is new tooling that can *prove* a mod is clientside, by booting a real server with
it and watching it die. **That half is command-line only and needs Docker. There is no GUI for it
in this release** — if you use ServerPackCreator through its window, it is not something you will
see. It is there for people comfortable running the jar with arguments.

Alongside both sit a pile of fixes to the generated start scripts, without which a lot of modern
Forge packs simply never started.

> **This is a beta.** It has had far less real-world mileage than 8.1.2. Keep a backup of your
> configuration, and — as always — **test your server pack before you hand it to anyone.**

---

## Clientside-mod detection

**Forge packs on Minecraft's newer year-based versions got detection at all.** The scanner was
picked by comparing only the *minor* part of the Minecraft version, so on a `26.x` version every
jar failed the scan and was kept. Those packs now get the `mods.toml` scan that actually works.

Be aware of what that means in practice: **if your pack quietly relied on nothing ever being
excluded, mods will now start being excluded.** That is the bug being fixed rather than a new one,
but it is a visible change — check your first server pack after upgrading.

Related, on the same code path: a NeoForge pack on a Minecraft version the version parser could not
hold (`26`, for instance) used to throw and **abort the whole generation**. Both cases now fall back
to the modern scanner instead.

**Fabric, Quilt and their API jars stopped being thrown away.**

- Fabric API, QFAPI and `quilt_base` were being treated as "the platform" rather than as real
  dependencies. They are now reported properly, which means a Fabric API jar that a custom
  clientside list had disabled gets **pulled back into** the server pack when something you kept
  depends on it.
- Mods now resolve dependencies through a jar's declared aliases. Fabric API calls itself
  `fabric-api` but *provides* `fabric`, so a mod depending on `fabric` was naming an id no jar
  answered to — and the pack it produced installed and then died on load.
- A Quilt jar with no `quilt.mod.json` — which is most of them, since Quilt runs Fabric mods —
  now reads its Fabric descriptor instead of discarding everything the jar declared.

**The bundled clientside-mod list grew from 496 to 548 entries.** One of those is a repair rather
than an addition: in 8.1.2 the `configured-` line was missing its trailing comma, welding it to
`connectedness-` into a single nonsense entry, so **neither mod was ever excluded.** They are two
entries again.

**One bad list entry no longer kills the generation.** With regex filtering enabled, a single
malformed entry used to throw straight out of the mod-list compiler and abort everything. Now that
entry simply never matches, the problem is logged once, and every other entry still applies.

---

## New: check a mod yourself — command line only

**Not available in the GUI.** These are four new commands for the jar, usable as launch arguments
or inside the interactive CLI (`-cli`). If you only ever use the graphical window, skip this
section — nothing here changes what that window does.

| Command | What it does |
|---|---|
| `-scan <dir> --loader <L> --minecraft <V>` | Reads the declared sideness of local jars and prints JSON. No network, no boot. |
| `-clientsidereport <url>` | Metadata-only report for a CurseForge or Modrinth project. Fast. |
| `-verifyclientside <url>` | The decisive one: downloads the mod and its dependencies, builds a server pack, and **boots a real dedicated server** in a container to see what actually happens. |
| `-clientsideapply --report <json>` | Feeds accepted entries from a report into your clientside-mod list. |

The reason `-verifyclientside` exists is that **what a mod says about itself is not reliable.** A mod
can declare itself server-safe and still crash a server, because its client-only code was wired up
wrong — and CurseForge has no sideness field at all, so for CurseForge mods there is nothing to
declare. Only a crash proves anything.

Each check ends in one of six verdicts:

| | Meaning |
|---|---|
| 🟢 **CONFIRMED** | A server actually died with this mod installed, and a rule named why. The only verdict strong enough to act on. |
| 🔵 **CLEAR** | The server reached its ready line and nothing went wrong — proven fine *for that build*. |
| ⚪ **INCONCLUSIVE** | The boot ran and did something odd, with nothing explaining why. |
| 🛠 **ERROR** | The check could not be run and it is fixable at your end — a failed download, a pack that would not generate. |
| 🔒 **LOCKED** | The mod author opted out of CurseForge distribution, so there is no file to download. Retrying never helps; try the same project on Modrinth. |
| 🚫 **UNVERIFIABLE** | The check was never possible for reasons outside both you and the mod — a required dependency nobody published for that loader and Minecraft version, for example. |

Read `CLEAR` honestly: it means *this* build, with *these* dependencies, on *that* Minecraft
version, started cleanly. It is not a general guarantee.

---

## Server packs that actually start

All three start-script templates were fixed, and these are the difference between a server that
boots and one that does not:

- **`-Djava.security.manager=allow` is fatal from Java 24 onward** — the JVM refuses to start at
  all. The templates now pass it only below Java 24. Minecraft 26.x requires Java 25, so before
  this every Forge pack on a modern Minecraft was dying before Forge even loaded.
- The check guarding that flag read the Java version *before* the script had worked one out, so it
  was still the literal placeholder `do_not_manually_edit` — meaning **a pack that installs its own
  Java passed the fatal flag anyway.** The version is now read after the Java check, and the guard
  fails safe.
- From Java 24 the templates run the Forge installer themselves and launch from `unix_args.txt`
  (`win_args.txt` on Windows) instead of handing the install to the ServerStarterJar. This fixes the
  maddening one where a fresh Forge pack printed *"The server installed successfully"*, **exited 0,
  and never launched** — and then worked if you ran it a second time.
- The ServerStarterJar is also bypassed for Forge on Minecraft 1.20.2 and 1.20.3, which it cannot
  launch at all.
- **fish start scripts** were added alongside bash and PowerShell.
- The server's real exit code is now passed back out of the script, so service wrappers and
  restart-on-crash setups see what actually happened.

---

## Faster, and it no longer freezes on a bad connection

- **Startup no longer waits on the network.** Version metadata refreshes in the background instead
  of during construction: roughly **399 ms → 47 ms** in the median case, and far more than that when
  you are offline. Version dropdowns still wait for real data before they are shown, so they stay
  correct.
- **Every outbound request now has a timeout** — 5 s connect, 15 s read, 60 s for downloads, all
  configurable. Previously nothing had one, so a host that silently dropped packets could leave the
  splash screen **stuck at 20 % with no way out but killing the process.** Setting a timeout to `0`
  restores the old unbounded behaviour if you want it.
- **Startup makes half the requests it used to** — 24 down to 12, and 489 KB down to 214 KB — by
  asking whether a manifest changed instead of re-downloading it.
- **Typing in the config editor is much cheaper.** The editor's background check used to make an
  HTTP request and re-parse the whole launcher manifest on every pause in typing. Both are now
  remembered. Failures deliberately are not cached, so a momentary network blip does not leave the
  editor insisting the server is unavailable until you restart.
- Autocomplete no longer reinstalls the entire look-and-feel on every keystroke.
- Saving settings no longer leaves the unsaved-changes marker stuck on.
- If you run two or more plugins, their tabs now appear once each instead of once per installed
  plugin.

---

## Docker and the web UI

**Read this one if you run ServerPackCreator as a service.**

Spring Boot 4 retired the `spring.data.mongodb.uri` property. A retired property does not warn — it
is simply not read — so ServerPackCreator was **silently ignoring every configured database host,
credential and database name** and falling back to Spring's own default. The property is now
`spring.mongodb.uri`.

- **Your `serverpackcreator.properties` needs no edit.** ServerPackCreator reads the new key, falls
  back to the old one if that is all it finds, and rewrites it under the new name. The old line is
  deliberately left in place so a downgrade still finds its URI.
- **Docker users setting `SPC_DATABASE_HOST` / `_PORT` / `_DB` / `_USERNAME` / `_PASSWORD` need
  change nothing** — the container builds the new property itself.
- **You do need to update anything of your own that writes the old key** — your own
  `overrides.properties`, a deployment script, an environment variable you set by hand. Nothing
  ServerPackCreator can do will fix those, and Spring will ignore them without saying so.
- A side effect worth knowing: URI query parameters work again, because the property they were on
  was being ignored entirely.

Also in this area:

- **The container no longer loses its `--home` argument.** The web service was overwriting the last
  element of its argument list rather than appending to it, so the value of the final argument was
  silently dropped — and in the container that argument is `--home`.
- **Startup no longer dies if the database is not up yet.** Index creation moved off the startup
  path; previously, losing the race with the `db` service meant a 30-second wait, a timeout, and a
  dead application — which is the normal first boot of a compose stack.
- The fallback database URI was never a valid URI (it carried literal backslashes), so every fresh
  web installation started from something the driver rejects. Fixed, and existing installs repair
  themselves.
- **Run-configuration mod lists are now stored in the document itself.** This removes roughly 550
  database round-trips per created run configuration. Stored documents are migrated automatically on
  first start. **If you consume `/api/v2/runconfigs` directly**, `startArgs`, `clientMods` and
  `whitelistedMods` are now plain string arrays.
- Duplicate run-configuration detection was matching configurations that shared a *single* mod.
  It now requires an exact match.

---

## The update checker

**Pre-release ordering was broken, and this release is the one that fixes it.** The check compared
only the number after the dot, ignoring the channel, so with `beta.3` published:

- someone on `alpha.2` was offered `beta.3` — correct by accident, `3 > 2`
- someone on `alpha.5` was offered **nothing at all** — `3 > 5` is false

Channels are now ordered properly (alpha → beta → release) with the number as a tie-break. Update
checks are also bounded by the new network timeouts, rather than potentially hanging forever.

---

## Breaking changes

Five, and **four of them affect nobody running ServerPackCreator normally.** Listed with who
actually needs to care:

| Change | Who it affects |
|---|---|
| Version metadata is handed out as immutable snapshots | **Plugin authors only.** Three return types narrowed: `LegacyFabricMeta.supportedMinecraftVersions()` to `List`, and `ForgeMeta.getForgeMeta()` / `NeoForgeMeta.getNeoForgeMeta()` to `Map`. Widen your declarations. If you were *modifying* what you got back, stop — you were corrupting shared state. This also fixes crashes and silently-empty reads while the background refresh was running. |
| `JsonBasedScanner` removed | **Plugin authors only**, and only one that subclassed it. Extend `JsonDescriptorScanner` instead — same `getJarJson`, plus the scanning contract. Every concrete scanner keeps its name and signature. |
| The headless-browser download route was removed | **Only the new `-verifyclientside` command**, which cannot verify a CurseForge project whose author opted out of distribution. It reports why and points you at Modrinth. It had already stopped working anyway. Nothing about normal server-pack generation changes. |
| `Confidence` and `aggregateFor` deleted | **Nobody.** Internal to an unpublished module, replaced by the six verdicts above. |
| One grinder deploy script instead of two | **Only people self-hosting a grinder instance.** `update-grinder.sh` is gone; `sudo ./install-grinder.sh` does that job. |

---

## Upgrading from 8.1.x

**If you use the GUI or the CLI:** nothing to do. Java 21 is still what you need, unchanged from
8.1.2. Do check your first generated server pack, since Forge clientside detection now works where
it previously did nothing.

**If you run Docker or the web UI:** see the database property above. In short — the application
migrates itself, but anything *you* wrote that sets `spring.data.mongodb.uri` must be changed to
`spring.mongodb.uri`.

**If you write plugins against `serverpackcreator-api`:**

- Widen the three narrowed return types listed above.
- Replace `JsonBasedScanner` with `JsonDescriptorScanner`.
- **Require kotlinx-coroutines 1.11.0 or newer.** If your build pins 1.10.x, you get a
  `NoSuchMethodError` at *runtime*, not a compile error.
- If you call `ListUtilities.parallelMap` without passing your own context, elements now run on the
  shared dispatcher instead of being accidentally serialised onto one thread. A lambda that mutates
  shared state without synchronisation can now race — pass a single-threaded context to keep the old
  behaviour.
- If you construct `ApiPlugins` directly, call `loadAndStart()` afterwards.
- If you read version metadata immediately after constructing it and need fresh upstream data, call
  `awaitManifestRefresh(timeoutMillis)`.
- An unusable home directory now throws `IllegalStateException`, not `IOException`.

---

## Known limitations

- **The clientside-checking commands have no GUI.** They are command-line only in this release.
- **`-verifyclientside` is slow and heavy.** Minutes per mod, and the first run downloads a
  Minecraft server and a modloader — a few hundred MB of disk. It needs Docker.
- **CurseForge needs an API key** (`CURSEFORGE_API_KEY`) for any of the clientside commands to
  resolve a CurseForge link. Modrinth does not.
- **Mods whose authors opted out of CurseForge distribution cannot be verified at all.** There is no
  file to download. Check the same project on Modrinth instead.
- The grinder daemon and its GUI plugin are **not** part of this download. What ships here is the
  detection built into generation, the four CLI commands, and the bundled clientside-mod list.

---

## Downloads

| File | What it is |
|---|---|
| `ServerPackCreator-9_0_0-beta_1-Installer-Windows-x86_64.exe` | Windows installer, bundles Java 21 |
| `ServerPackCreator-9_0_0-beta_1-Installer-Mac.dmg` | macOS installer, bundles Java 21 |
| `ServerPackCreator-9_0_0-beta_1-Installer-Linux-amd64.sh` | Linux installer, bundles Java 21 |
| `ServerPackCreator-9.0.0-beta.1.jar` | The application as a plain jar. **Requires Java 21** |
| `serverpackcreator-plugin-example-9.0.0-beta.1.jar` | Example plugin, for plugin authors |
| `serverpackcreator-api-9.0.0-beta.1.jar` + `-sources` / `-javadoc` | The API library, also on Maven Central |
| `serverpackcreator-api-9.0.0-beta.1-dokka-html.zip` | API documentation as browsable HTML |
| `serverpackcreator-app-9.0.0-beta.1-javadoc.jar` | Application javadoc |
| `checksum.txt` | SHA-256 of every file above |
| `updates.xml` | Update descriptor, used by the installers |

Docker images are published as `griefed/serverpackcreator:9.0.0-beta.1` on Docker Hub and ghcr.io.

---

## Full changelog

**1,121 changes since 8.1.2.** The complete commit-level list — including the 297 documentation and
277 test commits left out above — is in
[CHANGELOG.md at this tag](https://git.griefed.de/Griefed/ServerPackCreator/src/tag/9.0.0-beta.1/CHANGELOG.md).

Found a problem? [Open an issue](https://git.griefed.de/Griefed/ServerPackCreator/issues) — beta
feedback is the entire point of a beta.
