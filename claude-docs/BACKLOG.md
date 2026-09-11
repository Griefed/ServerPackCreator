# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

> **Numbering continues from B34 — it does not restart, and an ID is never reused.** This file is empty because
> every item landed or was dropped, not because the counter reset. B1–B34 have all been issued, and some are still
> cited by name elsewhere in the repo after their entries went away, so reusing an ID silently repoints someone
> else's citation at the wrong item. `git log -S'B<n> —' -- claude-docs/BACKLOG.md` recovers what any past ID meant.

Add the next item under a dated section, starting at **B36**, with the reason it waited and enough context to pick
it up cold.

## 2026-09-11 — from the UNVERIFIABLE pass

### B36 — Sinytra Connector as a boot strategy

**What:** stage Sinytra Connector plus Forgified Fabric API so a Fabric-only mod can be boot-verified under
Forge, instead of the Connector-wrapped jar being judged on its own.

**Why it waited.** The row that prompted it turned out not to need it. `hybrid-aquatic` published
`UNVERIFIABLE` off `[1.20.4] [Sinytra] Hybrid Aquatic 1.4.4.jar`, a **beta** whose only descriptor is a
`fabric.mod.json` — while the project ships **16 stable Forge releases** (`1.5.0-forge` … `1.6.9-forge`, all
Minecraft 1.20.1, all carrying a real `META-INF/mods.toml`). The release-channel preference picks one of
those now, so the Connector path buys nothing there.

**And the repo has already measured the attempt failing for reasons that are not ours.** The grinder staged
the newest Connector (`1.0.0-beta.49+1.20.1`) and the newest Forgified Fabric API
(`0.92.6+1.11.15+1.20.1`) — the only ones Modrinth publishes for 1.20.1 — and Connector under Forge 47.4.23
still logged *"Dependency resolution found 0 candidates to load"* and never converted the jar. Every
dependency was staged correctly; chasing it from here is unbounded. See the Connector-placeholder entry in
`serverpackcreator-clientside/CLAUDE.md` for what *is* implemented (a placeholder jar is scanned as the
Fabric mod it wraps).

**Pick it up when** a row appears that has no stable build for any loader *and* whose Connector jar is the
only thing a user could install — i.e. when the cost buys a verdict nothing else can produce.

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
