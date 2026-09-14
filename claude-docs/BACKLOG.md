# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

> **Numbering continues from the highest ID ever issued — it does not restart, and an ID is never reused.**
> A gap in this file means an item landed or was dropped, not that the counter reset; most IDs ever issued have
> no entry here any more, and some are still cited by name elsewhere in the repo, so reusing one silently
> repoints someone else's citation at the wrong item. `git log -S'B<n> —' -- claude-docs/BACKLOG.md` recovers
> what any past ID meant, and is also how to find the highest one rather than trusting a number written here.

Add the next item under a dated section, starting at **B38**, with the reason it waited and enough context to pick
it up cold. **B36 is issued and gone** — Sinytra Connector as a boot strategy, dropped 2026-09-12 when the
per-line axis made the shim cost a whole Minecraft line and the placeholder was redirected to Fabric instead;
see `REFACTOR-LOG.md`.

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
