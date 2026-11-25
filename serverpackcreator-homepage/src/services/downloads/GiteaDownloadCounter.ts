import { releasesEndpoint } from './apiEndpoints'
import {
  assertWithinPageBound, emptyTally, fetchReleasePage, tallyPage, toDownloads
} from './releasePages'
import type { ReleaseDownloadCounter } from './ReleaseDownloadCounter'
import type {
  CounterOptions, Platform, PlatformDownloads, RepositoryReference
} from './types'

/**
 * Gitea's page-size parameter and its ceiling.
 *
 * NOT `per_page`, and the difference is silent: Gitea and Forgejo ignore an unknown paging
 * parameter and fall back to the instance's own DEFAULT_PAGING_NUM -- measured at 10 on gitea.com
 * and 30 on git.griefed.de -- with no error. A counter that sends GitHub's parameter name here
 * reads a short page, believes it has reached the end, and returns a plausible number that is a
 * fraction of the truth.
 */
export const GITEA_PAGE_PARAMETER = 'limit'
/** The default `MAX_RESPONSE_ITEMS`; a larger `limit` is clamped to the instance's setting. */
export const GITEA_PAGE_SIZE = 50

const ACCEPT = 'application/json'

function isLastPage(pageLength: number, releasesRead: number, response: Response): boolean {
  if (pageLength === 0) {
    return true
  }
  const total = Number.parseInt(response.headers.get('x-total-count') ?? '', 10)
  return Number.isNaN(total) ? pageLength < GITEA_PAGE_SIZE : releasesRead >= total
}

/**
 * Counts through the Gitea `/api/v1` releases API. Serves both Gitea and Forgejo, which is one
 * implementation rather than two because Forgejo is a Gitea fork and the endpoint, the asset shape
 * and the paging are identical; only the platform label differs.
 *
 * Paging ends on an empty page, or once `X-Total-Count` releases have been read. Only without that
 * header does a page shorter than the requested size end it, because `MAX_RESPONSE_ITEMS` is
 * per-instance: on an instance capped below 50, every page is short.
 */
export class GiteaDownloadCounter implements ReleaseDownloadCounter {
  constructor(readonly platform: Platform) {}

  async count(
    reference: RepositoryReference,
    options?: CounterOptions
  ): Promise<PlatformDownloads> {
    const endpoint = releasesEndpoint(reference)
    const tally = emptyTally()

    for (let page = 1; ; page += 1) {
      const url = `${endpoint}?${GITEA_PAGE_PARAMETER}=${GITEA_PAGE_SIZE}&page=${page}`
      assertWithinPageBound(page - 1, url)
      const { releases, response } = await fetchReleasePage(url, options, ACCEPT)
      tallyPage(tally, releases)
      if (isLastPage(releases.length, tally.releases, response)) {
        break
      }
    }

    return toDownloads(reference, tally)
  }
}
