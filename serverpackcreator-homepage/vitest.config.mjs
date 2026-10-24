// `.mjs` rather than `.js` on purpose: package.json cannot carry `"type": "module"` while
// quasar.conf.js, babel.config.js, .postcssrc.js and .eslintrc.js are still CommonJS. The Vite
// migration flips all five together.
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'

const src = fileURLToPath(new URL('./src', import.meta.url))
const assets = fileURLToPath(new URL('./src/assets', import.meta.url))

export default defineConfig({
  plugins: [vue()],
  resolve: {
    // The webpack CLI's aliases, mirrored so a test imports a component exactly as the app does.
    // `~assets` is the form @vue/compiler-sfc leaves in an `src` attribute.
    alias: {
      // Quasar's `main` is dist/quasar.cjs.prod.js, which is the SSR-SERVER build: its installQuasar
      // takes an ssrContext third argument and Object.assign()s into it, so mounting anything under
      // Node dies with "Cannot convert undefined or null to object" before a single assertion runs.
      // The ESM build is the client one. @quasar/vite-plugin would pick it, but its current major
      // requires quasar ^2.16.0 and this module is still on 2.11.10.
      quasar: fileURLToPath(new URL('./node_modules/quasar/dist/quasar.esm.prod.js', import.meta.url)),
      '~assets': assets,
      assets,
      layouts: `${src}/layouts`,
      pages: `${src}/pages`,
      components: `${src}/components`,
      boot: `${src}/boot`,
      src,
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['test/**/*.{test,spec}.{js,mjs,ts}'],
    setupFiles: ['./test/install-quasar.mjs'],
  },
})
