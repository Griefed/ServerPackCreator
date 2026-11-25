import type { CounterOptions, HttpFetch, PlatformDownloads, RepositoryReference } from './types'

/** The shape both forges' releases endpoints return, reduced to the one field that is counted. */
export interface ReleasePayload {
  assets?: { download_count?: number }[] | null
}

/** Running totals over however many pages a counter has read. */
export interface Tally {
  total: number
  releases: number
  assets: number
}

/**
 * The most pages one count may read: 10,000 releases on GitHub, 5,000 on a default Gitea. Past it a
 * counter fails rather than looping, which is what a server that ignores `page` or a `Link` header
 * pointing back at itself would otherwise make it do.
 */
export const MAX_PAGES = 100

/** Raise once a counter is about to request more than MAX_PAGES pages. */
export function assertWithinPageBound(pagesRead: number, url: string): void {
  if (pagesRead >= MAX_PAGES) {
    throw new Error(`${url}: more than ${MAX_PAGES} pages of releases; refusing to page further`)
  }
}

function resolveFetch(options?: CounterOptions): HttpFetch {
  const supplied = options?.fetch ?? globalThis.fetch
  if (supplied === undefined) {
    throw new Error('No fetch implementation available; pass one via CounterOptions.fetch')
  }
  return supplied
}

/** A forge answered with a non-2xx status. */
export class HttpStatusError extends Error {
  constructor(readonly url: string, readonly status: number) {
    super(`${url} answered ${status}`)
    this.name = 'HttpStatusError'
  }
}

/** The default for `CounterOptions.timeoutMs`. */
export const REQUEST_TIMEOUT_MS = 30_000

/**
 * Run `request` with a signal that aborts after `timeoutMs` or when `outer` aborts, and reject at
 * the deadline even if the fetch implementation ignores its signal.
 */
async function withTimeout<T>(
  url: string,
  timeoutMs: number,
  outer: AbortSignal | undefined,
  request: (signal: AbortSignal) => Promise<T>
): Promise<T> {
  const controller = new AbortController()
  const forwardAbort = () => controller.abort(outer?.reason)
  if (outer?.aborted) {
    forwardAbort()
  }
  outer?.addEventListener('abort', forwardAbort, { once: true })

  let timer: ReturnType<typeof setTimeout> | undefined
  const deadline = new Promise<never>((_resolve, reject) => {
    timer = setTimeout(() => {
      const timeout = new Error(`${url} did not answer within ${timeoutMs} ms`)
      controller.abort(timeout)
      reject(timeout)
    }, timeoutMs)
  })

  try {
    return await Promise.race([request(controller.signal), deadline])
  } finally {
    clearTimeout(timer)
    outer?.removeEventListener('abort', forwardAbort)
  }
}

/** Fetch one page of releases, failing loudly rather than contributing a partial total. */
export async function fetchReleasePage(
  url: string,
  options: CounterOptions | undefined,
  accept: string
): Promise<{ releases: ReleasePayload[]; response: Response }> {
  const headers: Record<string, string> = { accept }
  if (options?.token !== undefined) {
    headers.authorization = `Bearer ${options.token}`
  }
  const fetch = resolveFetch(options)

  return withTimeout(
    url,
    options?.timeoutMs ?? REQUEST_TIMEOUT_MS,
    options?.signal,
    async (signal) => {
      const response = await fetch(url, { headers, signal })
      if (!response.ok) {
        throw new HttpStatusError(url, response.status)
      }
      return { releases: (await response.json()) as ReleasePayload[], response }
    }
  )
}

/** Add one page's assets to the running totals. */
export function tallyPage(tally: Tally, releases: ReleasePayload[]): void {
  for (const release of releases) {
    tally.releases += 1
    for (const asset of release.assets ?? []) {
      tally.assets += 1
      tally.total += asset.download_count ?? 0
    }
  }
}

export function emptyTally(): Tally {
  return { total: 0, releases: 0, assets: 0 }
}

export function toDownloads(reference: RepositoryReference, tally: Tally): PlatformDownloads {
  return {
    platform: reference.platform,
    host: reference.host,
    owner: reference.owner,
    repository: reference.repository,
    total: tally.total,
    releases: tally.releases,
    assets: tally.assets
  }
}
