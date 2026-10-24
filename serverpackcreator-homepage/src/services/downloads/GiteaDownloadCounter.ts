import { releasesEndpoint } from './apiEndpoints'
import {
  emptyTally, fetchReleasePage, tallyPage, toDownloads
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
/** `MAX_RESPONSE_ITEMS`; a larger `limit` is clamped to this. */
export const GITEA_PAGE_SIZE = 50

const ACCEPT = 'application/json'

/**
 * Counts through the Gitea `/api/v1` releases API. Serves both Gitea and Forgejo, which is one
 * implementation rather than two because Forgejo is a Gitea fork and the endpoint, the asset shape
 * and the paging are identical; only the platform label differs.
 *
 * There is no `Link` header to follow, so paging ends on the first page shorter than the page size.
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
      const { releases } = await fetchReleasePage(url, options, ACCEPT)
      tallyPage(tally, releases)
      if (releases.length < GITEA_PAGE_SIZE) {
        break
      }
    }

    return toDownloads(reference, tally)
  }
}
