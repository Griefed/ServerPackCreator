import js from '@eslint/js'
import globals from 'globals'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import pluginQuasar from '@quasar/app-vite/eslint'

export default tseslint.config([
  {
    // pluginQuasar.configs.recommended() already ignores node_modules, dist, .quasar and the rest
    // of the Quasar-relevant folders. ESLint requires "ignores" to be the only key in its object.
    ignores: ['docs-build/**']
  },

  ...pluginQuasar.configs.recommended(),
  js.configs.recommended,

  // TypeScript parser and recommended rules, for `.ts` files and `<script lang="ts">`.
  ...tseslint.configs.recommended,

  ...pluginVue.configs['flat/essential'],

  // eslint-plugin-vue parses `.vue` with vue-eslint-parser; this delegates the script body to the
  // TypeScript parser so a `<script lang="ts">` block is understood.
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser
      }
    }
  },

  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node, // config files
        ga: 'readonly' // Google Analytics
      }
    },

    rules: {
      'prefer-promise-reject-errors': 'off'
    }
  },

  // Vitest's globals (describe/it/expect/beforeEach) come from `test.globals` in vitest.config.js.
  {
    files: ['test/**/*.{js,ts}'],
    languageOptions: {
      globals: globals.vitest
    }
  }
])
