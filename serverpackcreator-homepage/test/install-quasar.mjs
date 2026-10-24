// Installs Quasar globally for Vue Test Utils, so a mounted SFC renders real Quasar DOM instead of
// unresolved <q-*> custom elements.
//
// The components are collected from Quasar's own exports rather than through @quasar/vite-plugin:
// that plugin's current major requires quasar ^2.16.0 and this module is still on 2.11.10. Quasar's
// installer takes a `components` map directly, which is all the plugin's auto-import buys a test.
import { config } from '@vue/test-utils'
import * as quasar from 'quasar'

const components = Object.fromEntries(
  Object.entries(quasar).filter(([name]) => /^Q[A-Z]/.test(name)),
)

config.global.plugins.push([quasar.Quasar, { components, plugins: { Notify: quasar.Notify } }])

// Quasar's palette reaches the app through its stylesheet, which Vitest does not load -- and
// `getCssVar` reads the custom properties off document.body. Without these a component that colours
// itself from the theme renders no colour at all in a test, which is indistinguishable from the
// defect such a test exists to catch. Values mirror src/css/quasar.variables.sass.
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
