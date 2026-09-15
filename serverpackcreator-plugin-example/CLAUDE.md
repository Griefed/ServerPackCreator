# serverpackcreator-plugin-example — module context

> pf4j example plugin exercising every extension point (config check, pre/post generation,
> GUI tab/panel). **Documentation-by-example: it must always reflect current API idiom.**

## Rules

- Use the **intended plugin idiom**: `ApiWrapper.api()` to register listeners is correct for plugins
  (that's the public entrypoint) — it is *not* an internal-constructor-injection smell here.
- No deprecated API calls. If an API change deprecates something this plugin uses, update the plugin
  in the same change so the example stays current.

## Testing

- Tests live in **`src/test/kotlin`** (the old empty `AddonTests.kt` was in `src/test/java` — fixed).
  `ConfigurationCheckTest` is the template: unit-test a `ConfigCheckExtension`, relaxed-mockk the
  unused `versionMeta`/`apiProperties`/`utilities`. Test dep: `io.mockk:mockk`.
- **Integration coverage lives in the API module**, where the pf4j fixtures are: `ApiPluginsTest`
  builds this plugin's jar and asserts all six extension points are discovered. Rebuild the jar
  against the API after API changes to confirm the compatibility policy held.
- Do **not** add hook-firing-during-generation tests: the example hooks only `println`, so verifying
  them means brittle stdout capture; the discovery test + the API generation tests already cover the
  meaningful integration.
- The built `…-dev.jar` consumed by API tests is a regenerated build artifact — don't commit rebuilds.
