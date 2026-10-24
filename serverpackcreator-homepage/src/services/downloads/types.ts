/**
 * Shared vocabulary for counting release-asset downloads across git forges.
 *
 * GitLab is deliberately absent: it does not track release-asset downloads at all. Its
 * `assets.links[]` entries carry only id, name, url, direct_asset_url and link_type, and the
 * request for counts (gitlab-org/gitlab#327317) has been open since 2021.
 */

/** A forge whose API reports a per-asset download count. */
export type Platform = 'github' | 'gitea' | 'forgejo'

/** A repository on one forge, resolved to everything a counter needs to address its API. */
export interface RepositoryReference {
  readonly platform: Platform
  /** Origin of the forge, without a trailing slash, e.g. `https://git.griefed.de`. */
  readonly host: string
  readonly owner: string
  readonly repository: string
}

/** What one forge reports for one repository. */
export interface PlatformDownloads {
  readonly platform: Platform
  readonly host: string
  readonly owner: string
  readonly repository: string
  /** Downloads summed over every asset of every release. */
  readonly total: number
  readonly releases: number
  readonly assets: number
}

/** Why one forge could not be counted. Collected rather than thrown, so one outage is not total. */
export interface PlatformFailure {
  readonly platform: Platform
  readonly host: string
  readonly reason: string
}

/** The result of counting across several forges. */
export interface DownloadReport {
  readonly total: number
  readonly platforms: readonly PlatformDownloads[]
  readonly failures: readonly PlatformFailure[]
  /** ISO-8601 instant the count was taken. */
  readonly countedAt: string
}

/**
 * The subset of `fetch` the counters use. Injecting it is what keeps this isomorphic -- the same
 * code runs in a browser and under Node -- and what lets the tests run without a network.
 */
export type HttpFetch = (url: string, init?: RequestInit) => Promise<Response>

export interface CounterOptions {
  /** Defaults to `globalThis.fetch`. */
  readonly fetch?: HttpFetch
  /** Sent as a bearer token. Raises GitHub's anonymous 60-requests-per-hour-per-IP ceiling. */
  readonly token?: string
  readonly signal?: AbortSignal
}

/** Raised for a platform identifier this library does not implement, `'gitlab'` among them. */
export class UnknownPlatformError extends Error {
  constructor(platform: string) {
    super(
      `Unknown platform '${platform}'. Supported: github, gitea, forgejo. ` +
      'GitLab is not supported because it does not expose release-asset download counts.'
    )
    this.name = 'UnknownPlatformError'
  }
}

/** Raised for a URL that does not name a repository. */
export class RepositoryUrlError extends Error {
  constructor(url: string, detail: string) {
    super(`'${url}' is not a repository URL: ${detail}`)
    this.name = 'RepositoryUrlError'
  }
}
