import type { CounterOptions, Platform, PlatformDownloads, RepositoryReference } from './types'

/** Counts the release-asset downloads of one repository on one forge. */
export interface ReleaseDownloadCounter {
  readonly platform: Platform
  count(reference: RepositoryReference, options?: CounterOptions): Promise<PlatformDownloads>
}
