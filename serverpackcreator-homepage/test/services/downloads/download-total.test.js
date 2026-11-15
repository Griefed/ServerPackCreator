// @vitest-environment node
import { describe, expect, it, vi } from 'vitest'

/** A forge's line in the baked report, as `scripts/collect-downloads.ts` writes it. */
const counted = (platform, host, total) => ({
  platform, host, owner: 'Griefed', repository: 'ServerPackCreator', total, releases: 1, assets: 1
})

const BAKED = {
  generatedAt: '2026-10-24T12:00:00.000Z',
  total: 189691,
  platforms: [
    counted('github', 'https://github.com', 170000),
    counted('gitea', 'https://gitea.com', 6000),
    counted('forgejo', 'https://git.griefed.de', 13691)
  ]
}

vi.mock('@/services/downloads/bakedDownloads', () => ({ GENERATED_DOWNLOADS: BAKED }))

const { readDownloadTotal, isStale, refreshDownloadTotal, CACHE_KEY, MAX_AGE_MS } = await import(
  '@/services/downloads/downloadTotal'
)

/** A localStorage stand-in; `failing` makes every access throw, as private browsing can. */
function storageStub(initial = null, failing = false) {
  let value = initial
  return {
    getItem: () => {
      if (failing) throw new Error('site data is blocked')
      return value
    },
    setItem: (_key, written) => {
      if (failing) throw new Error('site data is blocked')
      value = written
    },
    read: () => value
  }
}

describe('readDownloadTotal', () => {
  it('returns the baked figure when nothing is cached, with no request', () => {
    expect(readDownloadTotal({ storage: storageStub() })).toEqual({
      total: 189691,
      generatedAt: BAKED.generatedAt
    })
  })

  it('prefers a cached figure counted after the build', () => {
    const storage = storageStub(
      JSON.stringify({ total: 190000, generatedAt: '2026-10-25T12:00:00.000Z' })
    )
    expect(readDownloadTotal({ storage }).total).toBe(190000)
  })

  it('ignores a cached figure older than the build, which a redeploy makes stale', () => {
    const storage = storageStub(
      JSON.stringify({ total: 1, generatedAt: '2026-01-01T00:00:00.000Z' })
    )
    expect(readDownloadTotal({ storage }).total).toBe(189691)
  })

  it.each([
    ['corrupt JSON', 'not json at all'],
    ['the wrong shape', JSON.stringify({ total: 'lots' })]
  ])('falls back to the baked figure on %s', (_label, raw) => {
    expect(readDownloadTotal({ storage: storageStub(raw) }).total).toBe(189691)
  })

  /** Private browsing throws on access rather than returning null, which is easy to forget. */
  it('survives a storage that throws', () => {
    expect(readDownloadTotal({ storage: storageStub(null, true) }).total).toBe(189691)
  })

  it('works with caching disabled entirely', () => {
    expect(readDownloadTotal({ storage: null }).total).toBe(189691)
  })
})

describe('isStale', () => {
  const at = (iso) => Date.parse(iso)

  it('is false within the window', () => {
    const snapshot = { total: 1, generatedAt: '2026-10-24T12:00:00.000Z' }
    expect(isStale(snapshot, { now: () => at('2026-10-24T12:00:00.000Z') + MAX_AGE_MS - 1 }))
      .toBe(false)
  })

  it('is true past it', () => {
    const snapshot = { total: 1, generatedAt: '2026-10-24T12:00:00.000Z' }
    expect(isStale(snapshot, { now: () => at('2026-10-24T12:00:00.000Z') + MAX_AGE_MS + 1 }))
      .toBe(true)
  })

  it('treats an unparseable timestamp as stale', () => {
    expect(isStale({ total: 1, generatedAt: 'whenever' })).toBe(true)
  })
})

describe('refreshDownloadTotal', () => {
  const fresh = { now: () => Date.parse(BAKED.generatedAt) + 1000 }
  const stale = { now: () => Date.parse(BAKED.generatedAt) + MAX_AGE_MS + 1 }

  it('issues no request at all while the figure is fresh', async () => {
    const fetch = vi.fn()

    const snapshot = await refreshDownloadTotal({ ...fresh, fetch, storage: storageStub() })

    expect(fetch).not.toHaveBeenCalled()
    expect(snapshot.total).toBe(189691)
  })

  it('counts and caches once the figure is stale', async () => {
    const storage = storageStub()
    const fetch = vi.fn(() =>
      Promise.resolve(
        new Response(JSON.stringify([{ assets: [{ download_count: 7 }] }]), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        })
      )
    )

    const snapshot = await refreshDownloadTotal({ ...stale, fetch, storage })

    expect(fetch).toHaveBeenCalled()
    expect(snapshot.total).toBe(21) // three forges, seven downloads each
    expect(JSON.parse(storage.read())).toMatchObject({ total: 21 })
    expect(storage.read()).toContain('generatedAt')
  })

  it('keeps the figure on screen when every forge fails', async () => {
    const fetch = vi.fn(() => Promise.resolve(new Response('', { status: 503 })))

    const snapshot = await refreshDownloadTotal({
      ...stale, fetch, storage: storageStub(), retryDelayMs: 0
    })

    expect(snapshot.total).toBe(189691)
  })

  it('still refreshes when the cache cannot be written', async () => {
    const fetch = vi.fn(() =>
      Promise.resolve(
        new Response(JSON.stringify([{ assets: [{ download_count: 2 }] }]), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        })
      )
    )

    const snapshot = await refreshDownloadTotal({
      ...stale,
      fetch,
      storage: storageStub(null, true)
    })

    expect(snapshot.total).toBe(6)
  })

  /**
   * git.griefed.de sends no CORS header, so a browser never counts Forgejo. The refresh must keep
   * the build's Forgejo figure rather than cache a total that silently lacks it.
   */
  it('keeps the baked figure of a forge the browser cannot reach', async () => {
    const storage = storageStub()
    const fetch = vi.fn((url) =>
      String(url).includes('git.griefed.de')
        ? Promise.reject(new TypeError('Failed to fetch'))
        : Promise.resolve(
          new Response(JSON.stringify([{ assets: [{ download_count: 7 }] }]), {
            status: 200,
            headers: { 'content-type': 'application/json' }
          })
        )
    )

    const snapshot = await refreshDownloadTotal({ ...stale, fetch, storage, retryDelayMs: 0 })

    expect(snapshot.total).toBe(7 + 7 + 13691)
    expect(JSON.parse(storage.read()).total).toBe(7 + 7 + 13691)
    expect(readDownloadTotal({ storage }).total).toBe(7 + 7 + 13691)
  })

  it('counts a forge the build had no figure for once the browser reaches it', async () => {
    const fetch = vi.fn(() =>
      Promise.resolve(
        new Response(JSON.stringify([{ assets: [{ download_count: 7 }] }]), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        })
      )
    )

    const snapshot = await refreshDownloadTotal({
      ...stale,
      fetch,
      storage: storageStub(),
      hosts: { forgejo: 'https://forgejo.example' }
    })

    // github and gitea refreshed, forgejo.example counted fresh; git.griefed.de is not a host of
    // this sweep, so its baked figure is not carried over.
    expect(snapshot.total).toBe(21)
  })

  it('names its cache key, which is what a visitor clears', () => {
    expect(CACHE_KEY).toBe('serverpackcreator.downloads')
  })
})
