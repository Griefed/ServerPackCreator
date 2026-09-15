# serverpackcreator-clientside

The **clientside-mod verification engine**: given a CurseForge or Modrinth project link, it works out
whether a mod is *client-only* — i.e. whether it belongs on ServerPackCreator's clientside-mod list and can
be stripped from a server pack.

It answers two questions:

1. **What does the mod say?** Read the declared sideness from the platform and from the jar's own metadata.
2. **What does the mod do?** Optionally boot a real server with the mod force-included and watch for a crash.

Question 2 exists because question 1 lies: a mod can declare `server`/`both` and still crash a server,
because the author implemented their clientside code wrong.

> **This module is the engine, not the command.** You drive it through the ServerPackCreator **application
> jar**, which ships the CLI verbs documented below. There is nothing to run inside this module directly.

---

## 1. Prerequisites

| Requirement               | Needed for                            | Notes                                                                               |
|---------------------------|---------------------------------------|-------------------------------------------------------------------------------------|
| Java 21+                  | everything                            | The installers ship their own Java runtime                                          |
| The ServerPackCreator jar | everything                            | From the [latest release](https://github.com/Griefed/ServerPackCreator/releases)    |
| `CURSEFORGE_API_KEY`      | CurseForge links only                 | Modrinth needs no key. CurseForge's API refuses requests without one                |
| Network                   | everything                            | Resolving projects, downloading jars                                                |

```bash
export CURSEFORGE_API_KEY="your-key"      # only if you pass curseforge.com links
```

**Distribution-locked CurseForge files cannot be verified.** Some authors disable third-party downloads
(`allowModDistribution=false`), and CurseForge then publishes no download URL for the file — so there is
nothing to fetch and the mod cannot be scanned or boot-tested from that platform. The report says so per
mod rather than failing the run, and every other mod verifies normally.

If the project is also on Modrinth, verify it from there: Modrinth files always carry a download URL.

> Earlier versions drove the CurseForge website with a headless Chromium to fetch these anyway, which is why
> older docs mention Playwright and `playwright install-deps`. That was removed in 2026-09: it existed only
> to work around the author's opt-out, it stopped working when CurseForge moved behind a bot challenge, and
> it cost ~193 MB of bundled browser runtimes in every download. **No browser is needed any more, and none
> of those prerequisites apply.**

Everything below is run as `java -jar serverpackcreator.jar <verb>`. Adjust the jar name to your release.

---

## 2. Scan local mod jars for their declared sideness

Use this when you already have the jars on disk and just want to know what their metadata claims.

```bash
java -jar serverpackcreator.jar -scan /path/to/mods --loader Fabric --minecraft 1.20.1
```

| Argument            | Short | Required | Meaning                                                  |
|---------------------|-------|----------|----------------------------------------------------------|
| `-scan <dir>`       |       | yes      | Directory containing the mod jars                        |
| `--loader <name>`   | `-l`  | yes      | `Forge`, `NeoForge`, `Fabric`, `Quilt` or `LegacyFabric` |
| `--minecraft <ver>` | `-m`  | yes      | Minecraft version, e.g. `1.20.1`                         |

Prints **JSON** to stdout — one entry per jar with the sideness it declares. No network, no boot.

---

## 3. Report on a project link (metadata only — fast)

The cheap check. Resolves the project, derives the clientside-list name pattern, and prints a **Markdown**
report with a confidence per modloader.

```bash
java -jar serverpackcreator.jar -clientsidereport https://modrinth.com/mod/modmenu

# write it to a file instead of stdout
java -jar serverpackcreator.jar -clientsidereport https://modrinth.com/mod/modmenu --output report.md
```

| Argument                  | Short | Required | Meaning                                 |
|---------------------------|-------|----------|-----------------------------------------|
| `-clientsidereport <url>` |       | yes      | CurseForge or Modrinth project link     |
| `--output <file>`         | `-o`  | no       | Write the report here instead of stdout |

---

## 4. Verify by booting a server (slow — the decisive check)

Same as above **plus** it downloads the mod and its required dependencies, generates a server pack, and
boots it once per modloader, watching for a crash.

```bash
java -jar serverpackcreator.jar -verifyclientside https://modrinth.com/mod/modmenu --output verified.md
```

Takes minutes per modloader: it downloads a Minecraft server and the loader on first boot. Expect network
traffic and a few hundred MB of disk.

**How to read the verdict:**

| Result                  | Meaning                                                                                                                                                                         |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CRASHED` → **HIGH**    | Decisive. The server died with the mod in place — it is clientside-only, whatever it declares                                                                                   |
| `SURVIVED` → **MEDIUM** | The server booted fine. This is *not* proof of server-safety, only absence of a crash                                                                                           |
| `INCONCLUSIVE`          | Nothing was learned: no bootable loader/Minecraft combination, a download failed, or the boot aborted *before* the mod loaded (e.g. the loader has no build for that Minecraft) |

The asymmetry is deliberate. Only a crash proves anything.

---

## 5. Apply accepted entries to the fallback lists

Once you've accepted a report, this writes the suggested name patterns into both files that ship the
fallback list — no manual editing, no staging.

```bash
java -jar serverpackcreator.jar -clientsideapply --report report.json
```

| Argument                     | Short | Required | Meaning                                                               |
|------------------------------|-------|----------|-----------------------------------------------------------------------|
| `--report <file>`            | `-r`  | yes      | The clientside-report **JSON** to read entries from                   |
| `--generation-config <file>` |       | no       | Path to `GenerationConfig.kt`; defaults to the in-repo location       |
| `--properties <file>`        |       | no       | Path to the `serverpackcreator.properties` holding `fallbackmodslist` |

The JSON comes from the report verbs: their Markdown embeds a hidden
`<!-- clientside-report-data … -->` block that the accept step reads back.

---

## 6. Interactive shell

The same four verbs exist as commands inside ServerPackCreator's interactive CLI, with flag-style
arguments:

```bash
java -jar serverpackcreator.jar -cli
```

```
scan -d /path/to/mods -l Fabric -m 1.20.1
clientsidereport -u https://modrinth.com/mod/modmenu
verifyclientside -u https://modrinth.com/mod/modmenu -o verified.md
clientsideapply -r report.json
```

Every short flag has a long form, so the above can also be written out in full:

| Short | Long                  | Used by                                |
|-------|-----------------------|----------------------------------------|
| `-d`  | `--directory`         | `scan`                                 |
| `-l`  | `--loader`            | `scan`                                 |
| `-m`  | `--minecraft`         | `scan`                                 |
| `-u`  | `--url`               | `clientsidereport`, `verifyclientside` |
| `-o`  | `--output`            | `clientsidereport`, `verifyclientside` |
| `-r`  | `--report`            | `clientsideapply`                      |
|       | `--generation-config` | `clientsideapply`                      |
|       | `--properties`        | `clientsideapply`                      |

Each command accepts `--help`.

---

## 7. Troubleshooting

| Symptom                              | Cause & fix                                                                                                                                                                                                                 |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| CurseForge link fails to resolve     | `CURSEFORGE_API_KEY` unset or invalid — Modrinth links are unaffected                                                                                                                                                       |
| A file won't download | The author disabled third-party distribution (`allowModDistribution=false`), so CurseForge publishes no download URL. This is permanent, not a misconfiguration — verify the project from Modrinth instead. |
| Everything comes back `INCONCLUSIVE` | Usually no bootable combination: the mod's newest file may target a Minecraft version its loader has no build for, or only pre-releases. Check the report's detail line                                                     |
| Boot takes very long                 | Expected on a cold run — the first boot downloads the Minecraft server plus the loader                                                                                                                                      |
| I need this at catalogue scale       | Use [`serverpackcreator-grinder`](../serverpackcreator-grinder/README.md), which runs these boots in parallel, isolated Docker containers                                                                                   |

---

## For developers

Embedding the engine (platforms, scanners, downloaders, the boot verifier) in your own code is documented
in [`module.md`](module.md) and the module's Dokka pages. The engine depends only on
`serverpackcreator-api`; it is **not** published to Maven Central, so use it from a source checkout.
