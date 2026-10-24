import { GiteaDownloadCounter } from './GiteaDownloadCounter'
import { GitHubDownloadCounter } from './GitHubDownloadCounter'
import { UnknownPlatformError, type Platform } from './types'
import type { ReleaseDownloadCounter } from './ReleaseDownloadCounter'

/**
 * Where each platform lives when the caller names no host.
 *
 * `forgejo` is null on purpose rather than lazily: Forgejo is self-hosted and has no canonical
 * instance, so there is nothing honest to default it to. A caller that wants a Forgejo instance
 * counted supplies its host.
 */
export const DEFAULT_HOSTS: Readonly<Record<Platform, string | null>> = {
  github: 'https://github.com',
  gitea: 'https://gitea.com',
  forgejo: null
}

/** Gitea and Forgejo serve the same API, so one implementation answers to both labels. */
const COUNTERS: Readonly<Record<Platform, () => ReleaseDownloadCounter>> = {
  github: () => new GitHubDownloadCounter(),
  gitea: () => new GiteaDownloadCounter('gitea'),
  forgejo: () => new GiteaDownloadCounter('forgejo')
}

export const SUPPORTED_PLATFORMS = Object.keys(COUNTERS) as Platform[]

export function isPlatform(candidate: string): candidate is Platform {
  return Object.prototype.hasOwnProperty.call(COUNTERS, candidate)
}

/** The counter for a platform identifier. Raises UnknownPlatformError for anything unsupported. */
export function counterFor(platform: string): ReleaseDownloadCounter {
  if (!isPlatform(platform)) {
    throw new UnknownPlatformError(platform)
  }
  return COUNTERS[platform]()
}
