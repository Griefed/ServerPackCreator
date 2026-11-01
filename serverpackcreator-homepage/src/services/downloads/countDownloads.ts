import { counterFor, DEFAULT_HOSTS, isPlatform, SUPPORTED_PLATFORMS } from './counterRegistry'
import { parseRepositoryUrl } from './repositoryReference'
import {
  UnknownPlatformError,
  type CounterOptions, type DownloadReport, type Platform,
  type PlatformDownloads, type PlatformFailure
} from './types'

/** Extra hosts, or replacements for the defaults, when counting across every platform. */
export interface CountAllOptions extends CounterOptions {
  readonly hosts?: Partial<Record<Platform, string | null>>
  /** Total tries per platform, retries included. Defaults to 2; 1 disables the retry. */
  readonly attempts?: number
}

/**
 * Count one platform, deriving the owner, the repository and the API root from the repository URL
 * itself -- so pointing this at a self-hosted instance needs no configuration beyond the URL.
 */
export function countPlatformDownloads(
  platform: string,
  repositoryUrl: string,
  options?: CounterOptions
): Promise<PlatformDownloads> {
  if (!isPlatform(platform)) {
    return Promise.reject(new UnknownPlatformError(platform))
  }
  return counterFor(platform).count(parseRepositoryUrl(platform, repositoryUrl), options)
}

function describe(reason: unknown): string {
  return reason instanceof Error ? reason.message : String(reason)
}

interface Attempt {
  readonly platform: Platform
  readonly host: string
}

type Settled = { counted: PlatformDownloads } | { failure: PlatformFailure }

async function countOnce(
  attempts: readonly Attempt[],
  owner: string,
  repository: string,
  options?: CountAllOptions
): Promise<Settled[]> {
  return Promise.all(
    attempts.map(async ({ platform, host }): Promise<Settled> => {
      try {
        return {
          counted: await counterFor(platform).count(
            { platform, host, owner, repository },
            options
          )
        }
      } catch (reason) {
        return { failure: { platform, host, reason: describe(reason) } }
      }
    })
  )
}

const DEFAULT_ATTEMPTS = 2

/**
 * Count every platform that has a host, and sum them. A platform that fails is recorded in
 * `failures` rather than thrown, so one forge being down still yields the others' totals -- which
 * is the ordinary case in a browser, where an instance with CORS disabled is simply unreachable.
 *
 * A failed platform is retried, because the failure direction here is quiet: the total simply comes
 * out lower, with nothing to distinguish a forge that blipped from a forge with fewer downloads.
 * Observed in a real image build, where one forge dropped out and the total was baked 6,385 short.
 * A forge that is genuinely unreachable -- CORS, or no such host -- costs one extra request.
 */
export async function countAllDownloads(
  owner: string,
  repository: string,
  options?: CountAllOptions
): Promise<DownloadReport> {
  const hosts = { ...DEFAULT_HOSTS, ...options?.hosts }

  const attempts: Attempt[] = SUPPORTED_PLATFORMS.flatMap((platform) => {
    const host = hosts[platform]
    return host === null || host === undefined ? [] : [{ platform, host }]
  })

  const platforms: PlatformDownloads[] = []
  let outstanding = attempts
  let failures: PlatformFailure[] = []

  for (let attempt = 0; attempt < (options?.attempts ?? DEFAULT_ATTEMPTS); attempt += 1) {
    if (outstanding.length === 0) {
      break
    }
    const settled = await countOnce(outstanding, owner, repository, options)
    for (const result of settled) {
      if ('counted' in result) {
        platforms.push(result.counted)
      }
    }
    failures = settled.flatMap((result) => ('failure' in result ? [result.failure] : []))
    outstanding = failures.map(({ platform, host }) => ({ platform, host }))
  }

  return {
    total: platforms.reduce((sum, platform) => sum + platform.total, 0),
    platforms,
    failures,
    countedAt: new Date().toISOString()
  }
}
