// @vitest-environment node
import { describe, expect, it } from 'vitest'
import {
  GENERATED_DOWNLOADS, NO_GENERATED_DOWNLOADS, selectGenerated
} from '@/services/downloads/bakedDownloads'

describe('the baked download counts', () => {
  const collected = {
    generatedAt: '2026-10-24T12:00:00.000Z',
    total: 189691,
    platforms: []
  }

  it('uses the collector output when the build produced one', () => {
    expect(selectGenerated({ '../../generated/download-counts.json': collected })).toBe(collected)
  })

  /**
   * `src/generated/` is gitignored and only `npm run collect-downloads` writes it, so a fresh
   * checkout has no such file. That must leave `quasar dev` and the type-check working.
   */
  it('falls back to an unknown total, counted at the epoch, when the collector never ran', () => {
    const fallback = selectGenerated({})

    expect(fallback).toBe(NO_GENERATED_DOWNLOADS)
    expect(fallback.total).toBeNull()
    expect(fallback.platforms).toEqual([])
    expect(Date.parse(fallback.generatedAt)).toBe(0)
  })

  it('loads whether or not the generated file exists', () => {
    expect(typeof GENERATED_DOWNLOADS.generatedAt).toBe('string')
    expect(Array.isArray(GENERATED_DOWNLOADS.platforms)).toBe(true)
  })
})
