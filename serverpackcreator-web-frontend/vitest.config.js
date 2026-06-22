import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { quasar, transformAssetUrls } from '@quasar/vite-plugin'
import { fileURLToPath } from 'node:url'

// Resolve Quasar's path-aliases so test imports match the app's import style
// (e.g. `stores/setting-store`, `components/...`).
const srcDir = fileURLToPath(new URL('./src', import.meta.url))

export default defineConfig({
  // The Quasar plugin auto-imports Quasar components on demand (as the real build does), so SFCs
  // mounted in tests render real Quasar DOM rather than unresolved <q-*> custom elements.
  plugins: [vue({ template: { transformAssetUrls } }), quasar()],
  resolve: {
    alias: {
      src: srcDir,
      app: fileURLToPath(new URL('.', import.meta.url)),
      boot: `${srcDir}/boot`,
      stores: `${srcDir}/stores`,
      components: `${srcDir}/components`,
      layouts: `${srcDir}/layouts`,
      pages: `${srcDir}/pages`,
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['test/**/*.{test,spec}.{js,ts}'],
    setupFiles: ['./test/install-quasar.ts'],
  },
})
