// @vitest-environment node
import { afterEach, describe, expect, it, vi } from 'vitest'
import { countAllDownloads, countPlatformDownloads } from '@/services/downloads/countDownloads'
import { counterFor, DEFAULT_HOSTS } from '@/services/downloads/counterRegistry'
import { REQUEST_TIMEOUT_MS } from '@/services/downloads/releasePages'
import { UnknownPlatformError } from '@/services/downloads/types'
import { fetchStub, release } from './fetchStub'

const githubPage = (owner, repo) =>
  `https://api.github.com/repos/${owner}/${repo}/releases?per_page=100&page=1`
const giteaPage = (host, owner, repo) =>
  `${host}/api/v1/repos/${owner}/${repo}/releases?limit=50&page=1`

describe('counterFor', () => {
  it.each(['github', 'gitea', 'forgejo'])('resolves %s', (platform) => {
    expect(counterFor(platform).platform).toBe(platform)
  })

  /**
   * GitLab is rejected rather than quietly counted as zero. The data does not exist -- its release
   * `assets.links[]` carry no download count and gitlab-org/gitlab#327317 has been open since
   * 2021 -- and a zero would be indistinguishable from a repository with no downloads and from a
   * failed request.
   */
  it('rejects gitlab with an error that says why', () => {
    expect(() => counterFor('gitlab')).toThrow(UnknownPlatformError)
    expect(() => counterFor('gitlab')).toThrow(/does not expose release-asset download counts/)
  })

  it('rejects an unknown identifier', () => {
    expect(() => counterFor('bitbucket')).toThrow(UnknownPlatformError)
  })
})

describe('DEFAULT_HOSTS', () => {
  it('has no default for forgejo, which is self-hosted and has no canonical instance', () => {
    expect(DEFAULT_HOSTS.forgejo).toBeNull()
    expect(DEFAULT_HOSTS.github).toBe('https://github.com')
    expect(DEFAULT_HOSTS.gitea).toBe('https://gitea.com')
  })
})

describe('countPlatformDownloads', () => {
  it('derives owner, repository and API root from the URL it is given', async () => {
    const { fetch, requested } = fetchStub({
      [giteaPage('https://git.example.com', 'someone', 'their-repo')]: { body: [release(4, 6)] }
    })

    const counted = await countPlatformDownloads(
      'forgejo',
      'https://git.example.com/someone/their-repo',
      { fetch }
    )

    expect(requested[0]).toContain('https://git.example.com/api/v1/repos/someone/their-repo')
    expect(counted).toMatchObject({
      platform: 'forgejo',
      host: 'https://git.example.com',
      owner: 'someone',
      repository: 'their-repo',
      total: 10
    })
  })

  it('rejects gitlab', async () => {
    await expect(
      countPlatformDownloads('gitlab', 'https://gitlab.com/Griefed/ServerPackCreator')
    ).rejects.toThrow(UnknownPlatformError)
  })
})

describe('countAllDownloads credentials', () => {
  /** Records the authorization header each URL was requested with, answering every one. */
  function recordingFetch() {
    const authorization = new Map()
    const fetch = (url, init) => {
      authorization.set(String(url), init?.headers?.authorization)
      return Promise.resolve(
        new Response(JSON.stringify([release(1)]), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        })
      )
    }
    return { fetch, authorization }
  }

  const everyForge = { forgejo: 'https://git.griefed.de' }

  /**
   * A GitHub token is a credential for GitHub alone. Sent to gitea.com it discloses the token to a
   * third party, and a Gitea or Forgejo instance may answer an unknown bearer token with 401 even on
   * a public endpoint.
   */
  it('sends a GitHub token to GitHub and to no other forge', async () => {
    const { fetch, authorization } = recordingFetch()

    await countAllDownloads('Griefed', 'ServerPackCreator', {
      fetch, hosts: everyForge, tokens: { github: 'ghp_example' }
    })

    expect(authorization.size).toBe(3)
    for (const [url, header] of authorization) {
      if (url.startsWith('https://api.github.com/')) {
        expect(header, url).toBe('Bearer ghp_example')
      } else {
        expect(header, url).toBeUndefined()
      }
    }
  })

  it('sends each platform only its own token', async () => {
    const { fetch, authorization } = recordingFetch()

    await countAllDownloads('Griefed', 'ServerPackCreator', {
      fetch, hosts: everyForge, tokens: { forgejo: 'forgejo_example' }
    })

    for (const [url, header] of authorization) {
      expect(header, url).toBe(url.includes('git.griefed.de') ? 'Bearer forgejo_example' : undefined)
    }
  })

  /** The single-counter `token` option has no platform, so a sweep of every forge must ignore it. */
  it('sends no forge a token handed over without naming its platform', async () => {
    const { fetch, authorization } = recordingFetch()

    await countAllDownloads('Griefed', 'ServerPackCreator', {
      fetch, hosts: everyForge, token: 'ghp_example'
    })

    expect([...authorization.values()].filter((header) => header !== undefined)).toEqual([])
  })
})

describe('countAllDownloads', () => {
  it('sums the platforms that have a host and skips forgejo when none is supplied', async () => {
    const { fetch, requested } = fetchStub({
      [githubPage('Griefed', 'ServerPackCreator')]: { body: [release(100)] },
      [giteaPage('https://gitea.com', 'Griefed', 'ServerPackCreator')]: { body: [release(25)] }
    })

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch })

    expect(report.total).toBe(125)
    expect(report.platforms.map((p) => p.platform).sort()).toEqual(['gitea', 'github'])
    expect(report.failures).toEqual([])
    expect(requested.some((url) => url.includes('git.griefed.de'))).toBe(false)
    expect(Date.parse(report.countedAt)).not.toBeNaN()
  })

  it('counts a forgejo instance once its host is supplied', async () => {
    const { fetch } = fetchStub({
      [githubPage('Griefed', 'ServerPackCreator')]: { body: [release(100)] },
      [giteaPage('https://gitea.com', 'Griefed', 'ServerPackCreator')]: { body: [release(25)] },
      [giteaPage('https://git.griefed.de', 'Griefed', 'ServerPackCreator')]: { body: [release(8)] }
    })

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', {
      fetch,
      hosts: { forgejo: 'https://git.griefed.de' }
    })

    expect(report.total).toBe(133)
    expect(report.platforms.length).toBe(3)
  })

  /**
   * One forge being unreachable must not cost the others. git.griefed.de sends no CORS header by
   * default, so in a browser this is the ordinary case rather than an exotic one.
   */
  it('records a failing platform and still returns the others total', async () => {
    const { fetch } = fetchStub({
      [githubPage('Griefed', 'ServerPackCreator')]: { body: [release(100)] },
      [giteaPage('https://gitea.com', 'Griefed', 'ServerPackCreator')]: { status: 502 }
    })

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch, retryDelayMs: 0 })

    expect(report.total).toBe(100)
    expect(report.platforms.map((p) => p.platform)).toEqual(['github'])
    expect(report.failures.length).toBe(1)
    expect(report.failures[0].platform).toBe('gitea')
    expect(report.failures[0].reason).toMatch(/502/)
  })

  /**
   * The failure direction here is quiet: a forge that blips simply lowers the total, and nothing
   * distinguishes that from a forge with fewer downloads.
   */
  it('retries a failed platform rather than baking a silently low total', async () => {
    let giteaCalls = 0
    const fetch = (url) => {
      const asked = String(url)
      if (asked.includes('gitea.com')) {
        giteaCalls += 1
        if (giteaCalls === 1) {
          return Promise.reject(new Error('fetch failed'))
        }
      }
      return Promise.resolve(
        new Response(JSON.stringify([{ assets: [{ download_count: 11 }] }]), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        })
      )
    }

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch, retryDelayMs: 0 })

    expect(giteaCalls).toBe(2)
    expect(report.failures).toEqual([])
    expect(report.platforms.map((p) => p.platform).sort()).toEqual(['gitea', 'github'])
    expect(report.total).toBe(22)
  })

  it('gives up after the retry and records the failure', async () => {
    let giteaCalls = 0
    const fetch = (url) => {
      if (String(url).includes('gitea.com')) {
        giteaCalls += 1
        return Promise.reject(new Error('fetch failed'))
      }
      return Promise.resolve(
        new Response(JSON.stringify([{ assets: [{ download_count: 4 }] }]), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        })
      )
    }

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch, retryDelayMs: 0 })

    expect(giteaCalls).toBe(2)
    expect(report.total).toBe(4)
    expect(report.failures.map((f) => f.platform)).toEqual(['gitea'])
  })

  it('honours attempts: 1 for a caller that does not want the retry', async () => {
    let calls = 0
    const fetch = () => {
      calls += 1
      return Promise.reject(new Error('fetch failed'))
    }

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch, attempts: 1 })

    expect(calls).toBe(2) // github and gitea, once each
    expect(report.failures.length).toBe(2)
  })

  it('returns a zero total with every platform recorded as failed when all are down', async () => {
    const { fetch } = fetchStub({})

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch })

    expect(report.total).toBe(0)
    expect(report.platforms).toEqual([])
    expect(report.failures.length).toBe(2)
  })
})

describe('countAllDownloads timeouts', () => {
  afterEach(() => vi.useRealTimers())

  /**
   * Fail-soft covers errors, not hangs: a forge that accepts the connection and never answers would
   * otherwise stall the CI step and the image build until the job timeout.
   */
  it('records a forge that never answers as failed, within the per-request budget', async () => {
    vi.useFakeTimers()
    const fetch = (url, init) => {
      if (String(url).includes('git.griefed.de')) {
        return new Promise(() => {}) // accepts, never answers, ignores the signal
      }
      void init
      return Promise.resolve(
        new Response(JSON.stringify([release(3)]), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        })
      )
    }

    let settled = false
    const counting = countAllDownloads('Griefed', 'ServerPackCreator', {
      fetch, hosts: { forgejo: 'https://git.griefed.de' }, retryDelayMs: 0
    }).finally(() => { settled = true })

    await vi.advanceTimersByTimeAsync(2 * REQUEST_TIMEOUT_MS + 1) // two attempts
    expect(settled).toBe(true)

    const report = await counting
    expect(report.total).toBe(6)
    expect(report.failures.map((failure) => failure.platform)).toEqual(['forgejo'])
    expect(report.failures[0].reason).toMatch(/did not answer within/)
  })

  it('aborts the request it gives up on, so the connection is released', async () => {
    vi.useFakeTimers()
    const signals = []
    const fetch = (_url, init) => {
      signals.push(init.signal)
      return new Promise(() => {})
    }

    const counting = countAllDownloads('Griefed', 'ServerPackCreator', {
      fetch, attempts: 1, timeoutMs: 1000
    })
    await vi.advanceTimersByTimeAsync(1001)
    await counting

    expect(signals.length).toBe(2)
    expect(signals.every((signal) => signal.aborted)).toBe(true)
  })
})

describe('countAllDownloads retries', () => {
  afterEach(() => vi.useRealTimers())

  /** Counts gitea.com requests, answering them with `giteaStatus`, and answers GitHub with 4. */
  function giteaAnswering(giteaStatus) {
    const calls = { gitea: 0 }
    const fetch = (url) => {
      if (String(url).includes('gitea.com')) {
        calls.gitea += 1
        return Promise.resolve(new Response('', { status: giteaStatus }))
      }
      return Promise.resolve(
        new Response(JSON.stringify([release(4)]), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        })
      )
    }
    return { fetch, calls }
  }

  /** A rate limit or a missing repository answers the same a second later; asking again only spends quota. */
  it.each([403, 404, 429])('does not retry a %i, which cannot succeed on a second try', async (status) => {
    const { fetch, calls } = giteaAnswering(status)

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch, retryDelayMs: 0 })

    expect(calls.gitea).toBe(1)
    expect(report.failures.map((failure) => failure.platform)).toEqual(['gitea'])
  })

  it.each([408, 500, 503])('retries a %i, which can be transient', async (status) => {
    const { fetch, calls } = giteaAnswering(status)

    await countAllDownloads('Griefed', 'ServerPackCreator', { fetch, retryDelayMs: 0 })

    expect(calls.gitea).toBe(2)
  })

  it('waits before retrying, rather than asking a struggling forge again at once', async () => {
    vi.useFakeTimers()
    const { fetch, calls } = giteaAnswering(503)

    const counting = countAllDownloads('Griefed', 'ServerPackCreator', { fetch, retryDelayMs: 2000 })
    await vi.advanceTimersByTimeAsync(1999)
    expect(calls.gitea).toBe(1)

    await vi.advanceTimersByTimeAsync(1)
    await counting
    expect(calls.gitea).toBe(2)
  })
})
