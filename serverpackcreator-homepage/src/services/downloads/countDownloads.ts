import { counterFor, DEFAULT_HOSTS, isPlatform, SUPPORTED_PLATFORMS } from './counterRegistry'
import { HttpStatusError } from './releasePages'
import { parseRepositoryUrl } from './repositoryReference'
import {
  UnknownPlatformError,
  type CounterOptions, type DownloadReport, type Platform,
  type PlatformDownloads, type PlatformFailure
} from './types'

/**
 * Options for counting across every platform. There is no single `token`: a credential belongs to
 * one forge, so each is keyed by the platform it is sent to and reaches no other.
 */
export interface CountAllOptions extends Omit<CounterOptions, 'token'> {
  /** Extra hosts, or replacements for the defaults. */
  readonly hosts?: Partial<Record<Platform, string | null>>
  /** A bearer token per platform; a platform without one is asked anonymously. */
  readonly tokens?: Partial<Record<Platform, string>>
  /** Total tries per platform, retries included. Defaults to 2; 1 disables the retry. */
  readonly attempts?: number
  /** The wait before each retry. Defaults to DEFAULT_RETRY_DELAY_MS. */
  readonly retryDelayMs?: number
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

/** The options one platform's counter gets: the shared ones, plus that platform's token only. */
function optionsFor(platform: Platform, options?: CountAllOptions): CounterOptions {
  const token = options?.tokens?.[platform]
  return {
    ...(options?.fetch === undefined ? {} : { fetch: options.fetch }),
    ...(options?.signal === undefined ? {} : { signal: options.signal }),
    ...(options?.timeoutMs === undefined ? {} : { timeoutMs: options.timeoutMs }),
    ...(token === undefined ? {} : { token })
  }
}

function describe(reason: unknown): string {
  return reason instanceof Error ? reason.message : String(reason)
}

interface Attempt {
  readonly platform: Platform
  readonly host: string
}

type Settled =
  | { counted: PlatformDownloads }
  | { failure: PlatformFailure; retryable: boolean }

/**
 * Whether asking again can succeed. A 4xx other than 408 is an answer, not an outage: a rate limit
 * (403/429) does not lift within a retry's delay and a missing repository stays missing.
 */
function isRetryable(reason: unknown): boolean {
  if (!(reason instanceof HttpStatusError)) {
    return true
  }
  return reason.status === 408 || reason.status < 400 || reason.status >= 500
}

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
            optionsFor(platform, options)
          )
        }
      } catch (reason) {
        return {
          failure: { platform, host, reason: describe(reason) },
          retryable: isRetryable(reason)
        }
      }
    })
  )
}

const DEFAULT_ATTEMPTS = 2
export const DEFAULT_RETRY_DELAY_MS = 2000

function sleep(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}

/**
 * Count every platform that has a host, and sum them. A platform that fails is recorded in
 * `failures` rather than thrown, so one forge being down still yields the others' totals -- which
 * is the ordinary case in a browser, where an instance with CORS disabled is simply unreachable.
 *
 * A failed platform is retried, because the failure direction here is quiet: the total simply comes
 * out lower, with nothing to distinguish a forge that blipped from a forge with fewer downloads.
 * Only a failure that can be transient is retried, after `retryDelayMs`; a 4xx other than 408 is
 * final. A forge that is genuinely unreachable -- CORS, or no such host -- costs one extra request.
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

  const final: PlatformFailure[] = []

  for (let attempt = 0; attempt < (options?.attempts ?? DEFAULT_ATTEMPTS); attempt += 1) {
    if (outstanding.length === 0) {
      break
    }
    if (attempt > 0) {
      await sleep(options?.retryDelayMs ?? DEFAULT_RETRY_DELAY_MS)
    }
    const settled = await countOnce(outstanding, owner, repository, options)
    const retryable: PlatformFailure[] = []
    for (const result of settled) {
      if ('counted' in result) {
        platforms.push(result.counted)
      } else if (result.retryable) {
        retryable.push(result.failure)
      } else {
        final.push(result.failure)
      }
    }
    failures = [...final, ...retryable]
    outstanding = retryable.map(({ platform, host }) => ({ platform, host }))
  }

  return {
    total: platforms.reduce((sum, platform) => sum + platform.total, 0),
    platforms,
    failures,
    countedAt: new Date().toISOString()
  }
}
