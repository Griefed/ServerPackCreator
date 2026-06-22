# serverpackcreator-web-frontend — module context

> Quasar 2 / Vue 3 SPA, JavaScript (TypeScript migration planned), Pinia stores. Built into the
> app's web backend via the org.siouan frontend Gradle plugin.

## Commands

- `npm install && npx quasar dev` — dev server.
- `npx quasar build` — production build (consumed by the app web backend).
- `npm test` — Vitest (`vitest run`).

## Testing pattern (Vitest)

- Stack: `vitest@3`, `@vue/test-utils@2`, `@pinia/testing@1`, `happy-dom`. `vitest.config.js` maps
  the Quasar path aliases (`src/boot`, `stores`, `components`, …) and uses happy-dom.
- **Mock the axios boot-module, not the network:** `vi.mock('boot/axios.js')` avoids the Quasar-only
  `#q-app/wrappers` import chain. `test/stores/setting-store.test.js` is the template (pins
  `refresh()` data-fetching against the mocked boot module).
- A Quasar-plugin test harness for **component** tests is not built yet — add it before writing
  Vue Test Utils component tests.

## OPEN ISSUES — flagged, NOT yet fixed

- **`jsconfig.json` extends a non-existent `./tsconfig.json`** — resolve as part of the TS migration.

## Migration plan (Phase 4)

- 4a (done): Vitest infra stood up. Store cleanup: removed dead `doubleCount` getter (template
  leftover referencing a non-existent `counter`); `refresh()` now returns its promise (sole caller
  `SubmitModPackForm` doesn't await — behavior unchanged).
- 4b (done): decoupled the settings store from `$q`. `refresh()` is now pure data-fetching — it
  returns/rejects its promise; the sole caller `SubmitModPackForm.setup()` uses `useQuasar()` and a
  `.catch()` to notify. Fixes the latently-broken `this.$q.notify` (the store has no `$q`) and makes
  the error path unit-testable (new test pins that `refresh()` rejects on a failed request).
- 4c (next): **TypeScript migration** — add `tsconfig.json` (also resolves the dangling
  `jsconfig.json` extends), convert `stores`/`boot` first, then components; build/lint stays
  JS+ESLint9 until then. Then add the component test harness.
