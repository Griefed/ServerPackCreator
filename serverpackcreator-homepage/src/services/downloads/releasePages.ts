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

function resolveFetch(options?: CounterOptions): HttpFetch {
  const supplied = options?.fetch ?? globalThis.fetch
  if (supplied === undefined) {
    throw new Error('No fetch implementation available; pass one via CounterOptions.fetch')
  }
  return supplied
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

  const init: RequestInit = { headers }
  if (options?.signal !== undefined) {
    init.signal = options.signal
  }

  const response = await resolveFetch(options)(url, init)
  if (!response.ok) {
    throw new Error(`${url} answered ${response.status}`)
  }

  return { releases: (await response.json()) as ReleasePayload[], response }
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
