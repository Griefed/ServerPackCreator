// @vitest-environment node
import { describe, expect, it } from 'vitest'
import quasarConfig from '../../quasar.config.js'

/**
 * Guards the one build flag the whole application depends on and nothing else can catch.
 *
 * Every SFC in `src/` is Options API — `methods`, `computed`, `mounted`. `@quasar/app-vite` 3
 * defaults `build.vueOptionsAPI` to **false** (its own defaults block, beside `vueRouterMode`),
 * and turns that into the compile-time define `__VUE_OPTIONS_API__ = "false"`, which tree-shakes
 * `applyOptions` out of Vue. The result is silent and total: `setup()` still runs so every page
 * still *renders*, but no `mounted` hook fires and no `methods`/`computed` exist, so the SPA never
 * issues a single API request. No unit test sees this, because Vitest does not apply the build's
 * defines; only a real production build does.
 */
describe('quasar.config.js', () => {
  it('keeps the Vue Options API enabled, which every SFC in src/ relies on', () => {
    const config = quasarConfig({ prod: true, mode: {}, dev: false })

    expect(config.build.vueOptionsAPI).toBe(true)
  })
})
