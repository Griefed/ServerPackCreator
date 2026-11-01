import { releasesEndpoint } from './apiEndpoints'
import {
  emptyTally, fetchReleasePage, tallyPage, toDownloads
} from './releasePages'
import type { ReleaseDownloadCounter } from './ReleaseDownloadCounter'
import type {
  CounterOptions, Platform, PlatformDownloads, RepositoryReference
} from './types'

/** GitHub's maximum, and its parameter name. Asking for more is clamped to this. */
export const GITHUB_PAGE_SIZE = 100

const ACCEPT = 'application/vnd.github+json'

/**
 * The `rel="next"` URL from a `Link` header, or undefined on the last page.
 *
 * GitHub rewrites that URL to a `/repositories/{id}/` form, so following the header is the only
 * way to page correctly -- a reconstructed `?page=N+1` addresses a different path.
 */
function nextPage(response: Response): string | undefined {
  const link = response.headers.get('link')
  if (link === null) {
    return undefined
  }
  const next = link.split(',').find((part) => /;\s*rel="next"/.test(part))
  return next?.match(/<([^>]+)>/)?.[1]
}

/**
 * Counts through GitHub's releases API, which embeds the assets in each release, so one request
 * per page of releases and no per-release follow-up.
 */
export class GitHubDownloadCounter implements ReleaseDownloadCounter {
  readonly platform: Platform = 'github'

  async count(
    reference: RepositoryReference,
    options?: CounterOptions
  ): Promise<PlatformDownloads> {
    const tally = emptyTally()
    let url: string | undefined =
      `${releasesEndpoint(reference)}?per_page=${GITHUB_PAGE_SIZE}&page=1`

    while (url !== undefined) {
      const { releases, response } = await fetchReleasePage(url, options, ACCEPT)
      tallyPage(tally, releases)
      url = nextPage(response)
    }

    return toDownloads(reference, tally)
  }
}
