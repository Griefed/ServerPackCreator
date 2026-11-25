import type { GeneratedDownloads } from './types'

/**
 * Stands in for the collector's output in a checkout that never ran it: no figure, counted at the
 * epoch, so the page treats it as stale and counts in the browser instead.
 */
export const NO_GENERATED_DOWNLOADS: GeneratedDownloads = {
  generatedAt: new Date(0).toISOString(),
  total: null,
  platforms: []
}

/** The collector's output among the glob's matches, or the stand-in when there is none. */
export function selectGenerated(
  modules: Record<string, GeneratedDownloads>
): GeneratedDownloads {
  return Object.values(modules)[0] ?? NO_GENERATED_DOWNLOADS
}

/**
 * The figure baked in at build time. A glob rather than an `import` because the file is gitignored
 * and only `npm run collect-downloads` writes it: a glob that matches nothing is an empty record,
 * where a static import of a missing file fails `quasar dev`, the type-check and the tests.
 */
export const GENERATED_DOWNLOADS: GeneratedDownloads = selectGenerated(
  import.meta.glob<GeneratedDownloads>('../../generated/download-counts.json', {
    eager: true,
    import: 'default'
  })
)
