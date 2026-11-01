// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { GiteaDownloadCounter, GITEA_PAGE_SIZE } from '@/services/downloads/GiteaDownloadCounter'
import { fetchStub, release } from './fetchStub'

const reference = (platform = 'forgejo') => ({
  platform,
  host: 'https://git.griefed.de',
  owner: 'Griefed',
  repository: 'ServerPackCreator'
})

const endpoint = 'https://git.griefed.de/api/v1/repos/Griefed/ServerPackCreator/releases'

describe('GiteaDownloadCounter', () => {
  /**
   * The highest-value guard in this library, and the reason it is written first.
   *
   * Gitea and Forgejo page with `limit`, capped at 50. They do NOT reject `per_page` -- they
   * ignore it and fall back to the instance's DEFAULT_PAGING_NUM, measured at 10 on gitea.com and
   * 30 on git.griefed.de. So a counter that sends GitHub's parameter name gets a short page, stops
   * early, and returns a confident number that is a fraction of the real one. There is no error
   * and no symptom; only this assertion can tell the two apart.
   */
  it('pages with limit and never with per_page', async () => {
    const { fetch, requested } = fetchStub({
      [`${endpoint}?limit=${GITEA_PAGE_SIZE}&page=1`]: { body: [release(1, 2)] }
    })

    await new GiteaDownloadCounter('forgejo').count(reference(), { fetch })

    expect(requested).not.toEqual([])
    for (const url of requested) {
      expect(url).toContain(`limit=${GITEA_PAGE_SIZE}`)
      expect(url).not.toContain('per_page')
    }
  })

  it('asks for the maximum page size the API allows, which is 50', () => {
    expect(GITEA_PAGE_SIZE).toBe(50)
  })

  it('follows pages until one comes back short, and sums every asset', async () => {
    const full = Array.from({ length: GITEA_PAGE_SIZE }, () => release(2))
    const { fetch, requested } = fetchStub({
      [`${endpoint}?limit=${GITEA_PAGE_SIZE}&page=1`]: { body: full },
      [`${endpoint}?limit=${GITEA_PAGE_SIZE}&page=2`]: { body: [release(5, 5)] }
    })

    const counted = await new GiteaDownloadCounter('forgejo').count(reference(), { fetch })

    expect(requested.length).toBe(2)
    expect(counted.total).toBe(GITEA_PAGE_SIZE * 2 + 10)
    expect(counted.releases).toBe(GITEA_PAGE_SIZE + 1)
    expect(counted.assets).toBe(GITEA_PAGE_SIZE + 2)
  })

  it('stops after a first page that is already short, asking for nothing more', async () => {
    const { fetch, requested } = fetchStub({
      [`${endpoint}?limit=${GITEA_PAGE_SIZE}&page=1`]: { body: [release(7)] }
    })

    const counted = await new GiteaDownloadCounter('forgejo').count(reference(), { fetch })

    expect(requested.length).toBe(1)
    expect(counted.total).toBe(7)
  })

  it('reports the platform it was built for, so gitea and forgejo stay distinguishable', async () => {
    const { fetch } = fetchStub({
      [`${endpoint}?limit=${GITEA_PAGE_SIZE}&page=1`]: { body: [] }
    })

    const counted = await new GiteaDownloadCounter('gitea').count(reference('gitea'), { fetch })

    expect(counted.platform).toBe('gitea')
    expect(counted.total).toBe(0)
  })

  it('rejects rather than returning a partial total when the API fails', async () => {
    const { fetch } = fetchStub({
      [`${endpoint}?limit=${GITEA_PAGE_SIZE}&page=1`]: { status: 503 }
    })

    await expect(new GiteaDownloadCounter('forgejo').count(reference(), { fetch })).rejects.toThrow(
      /503/
    )
  })
})
