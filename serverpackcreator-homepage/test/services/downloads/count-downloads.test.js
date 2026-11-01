// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { countAllDownloads, countPlatformDownloads } from '@/services/downloads/countDownloads'
import { counterFor, DEFAULT_HOSTS } from '@/services/downloads/counterRegistry'
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

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch })

    expect(report.total).toBe(100)
    expect(report.platforms.map((p) => p.platform)).toEqual(['github'])
    expect(report.failures.length).toBe(1)
    expect(report.failures[0].platform).toBe('gitea')
    expect(report.failures[0].reason).toMatch(/502/)
  })

  /**
   * The failure direction here is quiet: a forge that blips simply lowers the total, and nothing
   * distinguishes that from a forge with fewer downloads. Observed in a real image build, where one
   * forge dropped out and the figure was baked 6,385 short.
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

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch })

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

    const report = await countAllDownloads('Griefed', 'ServerPackCreator', { fetch })

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
