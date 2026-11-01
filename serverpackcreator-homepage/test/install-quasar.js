// Installs Quasar globally for Vue Test Utils, so a mounted SFC renders real Quasar DOM instead of
// unresolved <q-*> custom elements. @quasar/vite-plugin in vitest.config.js does the component
// auto-import; this supplies the plugin instance and what the theme would otherwise come from.
//
// A file carrying `// @vitest-environment node` has no DOM, and merely importing Quasar there
// throws on `window` before the suite starts. Those files test configuration rather than
// components, so this setup skips itself for them.
import { config } from '@vue/test-utils'

if (globalThis.window !== undefined) {
  const { Quasar, Notify } = await import('quasar')

  config.global.plugins.push([Quasar, { plugins: { Notify } }])

  // Quasar's palette reaches the app through its stylesheet, which Vitest does not load -- and
  // `getCssVar` reads the custom properties off document.body. Without these a component that
  // colours itself from the theme renders no colour at all in a test, which is indistinguishable
  // from the defect such a test exists to catch. Mirrors src/css/quasar.variables.sass.
  beforeEach(() => {
    const palette = {
      primary: '#325358',
      secondary: '#54a896',
      accent: '#6a1a78',
      dark: '#1d1d1d',
      positive: '#21BA45',
      negative: '#C10015',
      info: '#31CCEC',
      warning: '#cea32b',
    }
    for (const [name, value] of Object.entries(palette)) {
      document.body.style.setProperty(`--q-${name}`, value)
    }
  })

  // The page's router links and outlet are not what these tests are about.
  config.global.stubs = {
    'router-link': { template: '<a><slot /></a>' },
    'router-view': { template: '<div />' },
  }
}
