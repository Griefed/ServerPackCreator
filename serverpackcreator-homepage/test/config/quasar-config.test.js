// @vitest-environment node
import { describe, expect, it } from 'vitest'
import quasarConfig from '../../quasar.config.js'

/**
 * Guards the one build flag the whole site depends on and nothing else can catch.
 *
 * Every SFC in `src/` is Options API -- `data`, `methods`, `computed`, `mounted`.
 * `@quasar/app-vite` 3 defaults `build.vueOptionsAPI` to **false** and turns that into the
 * compile-time define `__VUE_OPTIONS_API__ = "false"`, which tree-shakes `applyOptions` out of Vue.
 * The result is silent: `setup()` still runs so every page still renders and nothing throws, but
 * the flashcards lose `cardImage` and `styleTop`, the feature cards lose `hoverOver`, and the
 * download counter's `mounted` hook never fires. No unit test sees it, because Vitest does not
 * apply the build's defines; only a real production build does.
 */
describe('quasar.config.js', () => {
  it('keeps the Vue Options API enabled, which every SFC in src/ relies on', () => {
    const config = quasarConfig({ prod: true, dev: false, mode: {} })

    expect(config.build.vueOptionsAPI).toBe(true)
  })
})
