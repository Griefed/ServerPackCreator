import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { quasar, transformAssetUrls } from '@quasar/vite-plugin'
import { fileURLToPath } from 'node:url'

// One alias, matching what @quasar/app-vite 3 generates into .quasar/tsconfig.json, so a test
// imports a component exactly as the app does.
const src = fileURLToPath(new URL('./src', import.meta.url))

export default defineConfig({
  // The Quasar plugin auto-imports Quasar components on demand (as the real build does) and
  // resolves Quasar's CLIENT build -- its `main` is the SSR-server one, which throws under Node.
  plugins: [vue({ template: { transformAssetUrls } }), quasar()],
  resolve: {
    alias: { '@': src },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['test/**/*.{test,spec}.{js,mjs,ts}'],
    setupFiles: ['./test/install-quasar.js'],
  },
})
