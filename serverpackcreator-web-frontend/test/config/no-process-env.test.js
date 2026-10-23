// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const srcDir = fileURLToPath(new URL('../../src', import.meta.url))

function sourceFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) return sourceFiles(full)
    return /\.(ts|js|vue)$/.test(entry.name) ? [full] : []
  })
}

/**
 * Comments are stripped before matching, so the rule is about code rather than prose -- a doc
 * comment naming the old API is not a defect. The line-comment pattern deliberately skips `//`
 * preceded by `:`, so a URL does not swallow the rest of its line.
 */
function code(source) {
  return source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/(^|[^:])\/\/.*$/gm, '$1')
}

/**
 * `@quasar/app-vite` 3 defines **no** `process.env` replacements at all — only
 * `import.meta.env.QUASAR_*` (see its `quasar-config-file.js`). Browser code reading `process.env`
 * therefore gets a `ReferenceError: process is not defined` under `quasar dev`, and in a production
 * build the reads collapse to `undefined` instead of the configured value — silently, because
 * `undefined === 'history'` is simply false and the fallback branch looks like a working default.
 *
 * That is how `vueRouterMode` came to be ignored in production while the app still worked: `'hash'`
 * happened to be the fallback. Measured by building with `vueRouterMode: 'history'` and observing
 * the rendered RouterLink hrefs stay `#/`, `#/downloads`.
 */
describe('src/', () => {
  it('reads build-time configuration from import.meta.env, never process.env', () => {
    const offenders = sourceFiles(srcDir)
      .map((file) => ({ file, hits: (code(readFileSync(file, 'utf8')).match(/process\.env/g) || []).length }))
      .filter(({ hits }) => hits > 0)
      .map(({ file, hits }) => `${file.slice(srcDir.length + 1)} (${hits})`)

    expect(offenders).toEqual([])
  })
})
