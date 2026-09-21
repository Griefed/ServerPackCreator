# Backlog — deferred, agreed-to work

Items consciously deferred, with the reason and enough context to pick them up cold. Newest section first.
Not a wish-list: everything here was looked at, judged worth doing, and postponed for a stated reason.
When an item lands, delete it here and record it in `REFACTOR-LOG.md`.

> **Numbering continues from the highest ID ever issued — it does not restart, and an ID is never reused.**
> A gap in this file means an item landed or was dropped, not that the counter reset; most IDs ever issued have
> no entry here any more, and some are still cited by name elsewhere in the repo, so reusing one silently
> repoints someone else's citation at the wrong item. `git log -S'B<n> —' -- claude-docs/BACKLOG.md` recovers
> what any past ID meant, and is also how to find the highest one rather than trusting a number written here.

Add the next item under a dated section, starting at **B41**, with the reason it waited and enough context to pick
it up cold. **B36 is issued and gone** — Sinytra Connector as a boot strategy, dropped 2026-09-12 when the
per-line axis made the shim cost a whole Minecraft line and the placeholder was redirected to Fabric instead;
see `REFACTOR-LOG.md`.

## 2026-09-21 — from the Qodana run-832 triage

### B40 — take the Qodana baseline, against a run that is both trustworthy and confirmed

**What:** add `--baseline <sarif>` to the scan so a run reports what is *new* rather than everything,
retiring the residual style noise that nobody intends to fix.

**Why it waited, and why it is NOT simply the leftover of B38.** Two independent reasons, both of which
had to be cleared first and neither of which was visible when B38 was written:

1. **A baseline suppresses every current problem at once, including the twelve the exclusions target.**
   B38's own acceptance test is the count dropping by *exactly* twelve, 28 → 16. A baseline landing in
   the same run zeroes the count either way and so destroys the only evidence that the exclusions match
   anything — and a `name:`/`paths:` pair that matches nothing fails **silently**, looking identical to
   one that works. The exclusions must be confirmed alone, first.
2. **Run 832 could not fully read `-app`.** It carried the same 35 sanity failures as run 817, so 79
   `-app` files were analysed with their inspections degraded. A baseline built from it would encode the
   finding set of a scan that could not read a fifth of the repository. The codegen step now in
   `qodana.yml` is what fixes that, and its own effect is unmeasured until a run reports back.

**Pick it up when** one run has reported **both** `Qodana problems: 16` and `Qodana sanity failures: 0`.
Those two lines are in the job summary by design — until both read as stated, the finding set is not yet
the real one and anything baselined against it is a guess. If the problem count is not exactly 16, fix
the exclusions rather than baselining over the discrepancy; if the sanity count is not 0, the codegen
step is not doing its job and B39's instruction was to revert it rather than keep a slower job.

**Expect the real `-app` finding set to be larger than today's**, possibly much larger — 79 files are
about to be inspected properly for the first time. That is the point of doing this in the right order:
those findings are worth reading before deciding what to bury.
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
