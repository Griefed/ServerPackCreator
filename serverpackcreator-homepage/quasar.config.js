// Configuration for the Quasar CLI (Vite).
// https://v2.quasar.dev/quasar-cli-vite/quasar-config-file
import { defineConfig } from '@quasar/app-vite'

export default defineConfig(() => {
  return {
    // https://v2.quasar.dev/quasar-cli-vite/boot-files
    boot: [],

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#css
    css: [
      'app.sass'
    ],

    // https://github.com/quasarframework/quasar/tree/dev/extras
    extras: [
      'mdi-v5',
      'fontawesome-v5',
      'roboto-font',
      'material-icons',
      'material-icons-outlined'
    ],

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#build
    build: {
      // TypeScript build settings. `strict` turns on Quasar's full strict preset, regenerated into
      // .quasar/tsconfig.json by `quasar prepare`; `vueShim` emits the `*.vue` module declaration
      // so plain tooling can resolve single-file-component imports.
      typescript: {
        strict: true,
        vueShim: true
      },

      // REQUIRED, not decorative. Every SFC in src/ is Options API, and @quasar/app-vite 3 defaults
      // this to false -- which compiles __VUE_OPTIONS_API__ = "false" and tree-shakes applyOptions
      // out of Vue. setup() still runs, so every page renders and nothing throws; the page simply
      // loses `data`, `methods`, `computed` and every lifecycle hook. Nothing in a unit suite can
      // see it, because Vitest does not apply the build's defines.
      vueOptionsAPI: true,

      vueRouterMode: 'hash', // available values: 'hash', 'history'
      publicPath: '',

      vitePlugins: [
        // Fails `quasar build` on a type or lint error and surfaces both in the dev-server overlay,
        // so the checks are part of the build rather than a step somebody has to remember.
        ['vite-plugin-checker', {
          vueTsc: true,
          eslint: {
            lintCommand: 'eslint -c ./eslint.config.js "./src/**/*.{js,ts,vue}"',
            useFlatConfig: true
          }
        }, { server: false }]
      ]
    },

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#devserver
    devServer: {
      https: false,
      port: 3000,
      open: true
    },

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#framework
    framework: {
      config: {},
      iconSet: 'material-icons',
      plugins: [
        'Notify',
        'AppFullscreen'
      ]
    },

    animations: []
  }
})
