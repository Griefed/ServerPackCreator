# serverpackcreator-api — module context

> Core library, published to Maven Central. **Public surface is a plugin-compatibility
> constraint** — see the API compatibility policy in the root `CLAUDE.md`. Domain core: must not
> depend on Swing, Spring web, or the frontend, and must be unit-testable without a Spring context.

## Layout & composition

- Packages: `config` (validation, `PackConfig`), `serverpack` (generation, `ServerPackHandler`),
  `modscanning` (clientside-mod detection per loader), `versionmeta` (Minecraft/loader manifests),
  `plugins` (pf4j plugin API), `settings` (extracted config groups), `utilities`.
- `ApiWrapper` is the composition root — a thin, lazy, constructor-injected collaborator graph.
  Leave it thin.
- Tests: JUnit 5; real fixture modpacks under `tests/` and `src/test/resources/testresources/`.
  "Tested" = unit tests per class **plus** generation end-to-end. No live network (version
  manifests are cached).

## Established patterns

- **Settings-group extraction** (used to break up `ApiProperties`): (1) write group tests first
  against `PropertyStore`; (2) move get/set logic verbatim into the group class, keys as companion
  constants; (3) `ApiProperties` keeps thin facade properties delegating to the group; (4) run API
  + app suites. Large data blocks (e.g. fallback mod-lists) are moved by script, not retyped.
- `ApiProperties` now delegates to `PropertyStore` + 8 `settings.*Config` groups and retains only
  orchestration, jar/OS info, version/firstRun, preferences, hasteBin and the log4j factory.

## Landmines & verified quirks (durable — do not relearn)

- **Property-group declaration order:** group declarations in `ApiProperties` must come **after**
  the groups they depend on — Kotlin initializes properties in declaration order. Reordering can
  NPE at construction.
- **`ApiProperties` IS log4j's `ConfigurationFactory`** (via `@Plugin`). The log4j-XML machinery
  stays there; moving it risks breaking log4j plugin-discovery.
- **Loader regexes have a single source of truth:** `config.SupportedModloaders` (5 exact-match
  regexes + canonical `names`). Do **not** reintroduce `"^forge$"`-style literals anywhere else.
- **LANDMINE — Minecraft has two versioning schemes; never read a component in isolation.** Releases are
  either `1.x[.y]` or the newer `YY.x[.y]` (`26.1.2`, `26.2`). Any test on the *minor* component alone is
  therefore wrong: `26.2`'s minor is `2`, which reads as the 1.2 era. Two live instances were found and fixed
  on 2026-07-31, both in the start-script templates and both silent — a wrong branch produces a pack that dies
  before loading a mod, not an error:
  - the Forge launcher era (`SEMANTICS[1] -le 16`) sent every Forge boot on Minecraft 26.x down the legacy
    `forge.jar` path → `Error: Unable to access jarfile forge.jar`. **24 grinder boot logs, all Forge, never
    started the server.**
  - the NeoForge 1.20/1.20.1 installer coordinate (`SEMANTICS[1] -eq 20`) would send a future `26.20` at a
    1.20-era URL. Latent, fixed anyway.

  Both now require major `1` as well, pinned by `ScriptTemplateContentTest`, which **executes** the extracted
  shell functions across both schemes. **The Kotlin side was surveyed and is clean by construction — keep it
  that way:** `BootCandidateSelector.minecraftComparator` compares component-wise, `ImageJavaRuntimes` takes
  required-Java from `MinecraftMeta.requiredJavaVersion` (Mojang's own declaration), and
  `LoaderVersionResolver` delegates to the manifests. Derive from metadata or compare all components; never
  hand-roll an era heuristic.
- **LANDMINE — `-Djava.security.manager=allow` is fatal from Java 24 on.** JEP 486 removed Security Manager
  support, so the VM *refuses to start* rather than ignoring the flag. `PackConfig.spcSSJArgsKeyDefaultValue`
  still defaults `SSJ_FORGE_ARGS` to it, because Forge's ServerStarterJar needs it on older Java — the
  templates therefore pass it **only below Java 24**. Minecraft 26.x requires Java 25, so before that guard
  every modern Forge pack died before Forge loaded; NeoForge/Fabric/Quilt never pass the flag, which is why
  only Forge was affected. Pinned by `ScriptTemplateContentTest`. Do not "simplify" by dropping the default —
  old packs still need it — and do not pass it unconditionally.
- **`PackConfig.modloader` setter silently ignores unrecognized values**; unknown loaders default
  to **Forge**. Most-specific loader names must be matched first (LegacyFabric before Fabric, etc.).
- **`PackConfig.save(destination, apiProperties)`** is the primary (injection-required) overload;
  `save(destination)` is a `@Deprecated` facade resolving `ApiProperties` via the singleton — don't
  build new call-sites on the deprecated one.
- **`InclusionSpecification`** has a **manual** `equals`/`hashCode` over its four fields (source,
  destination, inclusion/exclusion filter) — intentionally *not* a `data class`, to keep the public
  API stable for plugins. Verified safe: no hash-based collections of inclusions exist.
- **`ReticulatingSplines`** (SimCity-style splash texts) is an intentional just-for-fun API
  endpoint per Griefed — it **stays** in the API; do not move or deprecate it.
- **`java.awt.Desktop` convenience methods stay in `-api`** (`WebUtilities.openLinkInBrowser`,
  `FileUtilities.openFolder`/`openFile`). They are **deliberately** here, not a boundary violation:
  only `-api` is published to Maven, and a plugin may run under the GUI **or** the web backend, so
  keeping these in the API lets plugin authors open browsers/files/folders regardless of host. Do
  **not** invert them behind an app-side adapter — that would remove the capability from plugins.
  (`java.awt` is core JDK; the inward-dep rule only forbids Swing / Spring-web / frontend.)
- **Plugin test jar:** `serverpackcreator-plugin-example-dev.jar` under the API test-resources is a
  build artifact regenerated by `copyPluginsApiUnitTests` — **don't commit rebuilds.** `ApiPluginsTest`
  loads it via pf4j and asserts all six extension points are discovered.

## When extracting/refactoring here

Follow root **Refactor discipline**: characterization tests first, verbatim move, thin deprecated
facade, suites green. The Phase 1a characterization tests (ConfigurationHandler manifest parsing;
ServerPackHandler file-gathering/cleanup/icon/properties/placeholders) pin behavior through the
facades — keep them green.
