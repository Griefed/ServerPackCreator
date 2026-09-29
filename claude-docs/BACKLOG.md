# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

> **Numbering continues from the highest ID ever issued — it does not restart, and an ID is never reused.**
> A gap in this file means an item landed or was dropped, not that the counter reset; most IDs ever issued have
> no entry here any more, and some are still cited by name elsewhere in the repo, so reusing one silently
> repoints someone else's citation at the wrong item. `git log -S'B<n> —' -- claude-docs/BACKLOG.md` recovers
> what any past ID meant, and is also how to find the highest one rather than trusting a number written here.

Add the next item under a dated section, starting at **B43**, with the reason it waited and enough context to pick
it up cold. **B36 is issued and gone** — Sinytra Connector as a boot strategy, dropped 2026-09-12 when the
per-line axis made the shim cost a whole Minecraft line and the placeholder was redirected to Fabric instead;
see `REFACTOR-LOG.md`. **B38, B39 and B40 are issued and gone** — the Qodana work, all three closed
2026-09-21; B40 deliberately *not* by the baseline it proposed, see `REFACTOR-LOG.md`.

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
