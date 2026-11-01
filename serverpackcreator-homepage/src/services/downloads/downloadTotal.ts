import generated from '@/generated/download-counts.json'
import {
  SERVERPACKCREATOR_HOSTS, SERVERPACKCREATOR_OWNER, SERVERPACKCREATOR_REPOSITORY
} from '@/config/repositories'
import { countAllDownloads, type CountAllOptions } from './countDownloads'

/** A download total and when it was counted. `null` means no forge could be reached. */
export interface DownloadSnapshot {
  readonly total: number | null
  readonly generatedAt: string
}

/** Where a live refresh is remembered between visits. */
export const CACHE_KEY = 'serverpackcreator.downloads'
/** How stale the remembered figure may get before a visitor refreshes it. */
export const MAX_AGE_MS = 24 * 60 * 60 * 1000

export interface DownloadTotalOptions extends CountAllOptions {
  /** Defaults to `localStorage`; pass null to disable caching entirely. */
  readonly storage?: Pick<Storage, 'getItem' | 'setItem'> | null
  readonly now?: () => number
  readonly maxAgeMs?: number
}

/** The figure baked in at build time, counted in Node where no forge is behind CORS. */
const baked: DownloadSnapshot = {
  total: generated.total,
  generatedAt: generated.generatedAt
}

function defaultStorage(): Pick<Storage, 'getItem' | 'setItem'> | null {
  // Absent under SSR and in a unit test; throws outright when site data is blocked.
  try {
    return globalThis.localStorage ?? null
  } catch {
    return null
  }
}

function resolveStorage(options?: DownloadTotalOptions) {
  return options?.storage === undefined ? defaultStorage() : options.storage
}

function readCache(options?: DownloadTotalOptions): DownloadSnapshot | null {
  const storage = resolveStorage(options)
  if (storage === null) {
    return null
  }

  try {
    const raw = storage.getItem(CACHE_KEY)
    if (raw === null) {
      return null
    }
    const parsed = JSON.parse(raw) as Partial<DownloadSnapshot>
    if (typeof parsed.total !== 'number' || typeof parsed.generatedAt !== 'string') {
      return null
    }
    return { total: parsed.total, generatedAt: parsed.generatedAt }
  } catch {
    // Unreadable or corrupt storage is a reason to fall back, never to fail.
    return null
  }
}

function writeCache(snapshot: DownloadSnapshot, options?: DownloadTotalOptions): void {
  const storage = resolveStorage(options)
  try {
    storage?.setItem(CACHE_KEY, JSON.stringify(snapshot))
  } catch {
    // Private browsing and a full quota both throw here; neither is worth a broken page.
  }
}

/**
 * The best figure available without issuing a request: whichever of the baked-in count and the
 * visitor's cached refresh was counted later. Synchronous, so the page never shows a spinner.
 */
export function readDownloadTotal(options?: DownloadTotalOptions): DownloadSnapshot {
  const cached = readCache(options)
  if (cached === null) {
    return baked
  }
  return Date.parse(cached.generatedAt) > Date.parse(baked.generatedAt) ? cached : baked
}

/** Whether the figure on hand is old enough to be worth a sweep of the forges. */
export function isStale(snapshot: DownloadSnapshot, options?: DownloadTotalOptions): boolean {
  const now = options?.now?.() ?? Date.now()
  const maxAge = options?.maxAgeMs ?? MAX_AGE_MS
  const countedAt = Date.parse(snapshot.generatedAt)
  return Number.isNaN(countedAt) || now - countedAt > maxAge
}

/**
 * Return the current figure, refreshing it from the forges first if it has gone stale.
 *
 * A refresh costs the visitor roughly twenty requests, so it happens at most once a day. It is
 * also allowed to fail: a forge with CORS disabled -- git.griefed.de, by Forgejo's default -- is
 * simply unreachable from a browser, and the baked figure already counted it.
 */
export async function refreshDownloadTotal(
  options?: DownloadTotalOptions
): Promise<DownloadSnapshot> {
  const current = readDownloadTotal(options)
  if (!isStale(current, options)) {
    return current
  }

  const report = await countAllDownloads(
    SERVERPACKCREATOR_OWNER,
    SERVERPACKCREATOR_REPOSITORY,
    { hosts: SERVERPACKCREATOR_HOSTS, ...options }
  )

  // Every forge failing says nothing about the total, so keep what we already had.
  if (report.platforms.length === 0) {
    return current
  }

  const refreshed = { total: report.total, generatedAt: report.countedAt }
  writeCache(refreshed, options)
  return refreshed
}
