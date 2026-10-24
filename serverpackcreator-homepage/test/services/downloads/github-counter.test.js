// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { GitHubDownloadCounter, GITHUB_PAGE_SIZE } from '@/services/downloads/GitHubDownloadCounter'
import { fetchStub, release } from './fetchStub'

const reference = {
  platform: 'github',
  host: 'https://github.com',
  owner: 'Griefed',
  repository: 'ServerPackCreator'
}

const endpoint = 'https://api.github.com/repos/Griefed/ServerPackCreator/releases'

describe('GitHubDownloadCounter', () => {
  it('pages with per_page at GitHub\'s maximum of 100', async () => {
    const { fetch, requested } = fetchStub({
      [`${endpoint}?per_page=${GITHUB_PAGE_SIZE}&page=1`]: { body: [release(3)] }
    })

    await new GitHubDownloadCounter().count(reference, { fetch })

    expect(GITHUB_PAGE_SIZE).toBe(100)
    expect(requested).toEqual([`${endpoint}?per_page=${GITHUB_PAGE_SIZE}&page=1`])
  })

  /**
   * GitHub rewrites the `Link` header's next URL to a `/repositories/{id}/` form, which cannot be
   * reconstructed from the request. Following the header rather than incrementing a page counter
   * is therefore not a stylistic choice.
   */
  it('follows the Link header rel=next rather than guessing the next URL', async () => {
    const second = 'https://api.github.com/repositories/336388951/releases?per_page=100&page=2'
    const { fetch, requested } = fetchStub({
      [`${endpoint}?per_page=${GITHUB_PAGE_SIZE}&page=1`]: {
        body: [release(10, 20)],
        link: `<${second}>; rel="next", <${second}>; rel="last"`
      },
      [second]: { body: [release(5)] }
    })

    const counted = await new GitHubDownloadCounter().count(reference, { fetch })

    expect(requested[1]).toBe(second)
    expect(counted.total).toBe(35)
    expect(counted.releases).toBe(2)
    expect(counted.assets).toBe(3)
  })

  it('stops when the Link header offers no next page', async () => {
    const { fetch, requested } = fetchStub({
      [`${endpoint}?per_page=${GITHUB_PAGE_SIZE}&page=1`]: {
        body: [release(1)],
        link: `<${endpoint}?per_page=100&page=1>; rel="last"`
      }
    })

    await new GitHubDownloadCounter().count(reference, { fetch })

    expect(requested.length).toBe(1)
  })

  it('sends a bearer token when one is supplied, since anonymous is 60 requests an hour', async () => {
    const seen = []
    const fetch = (url, init) => {
      seen.push(init?.headers ?? {})
      return Promise.resolve(
        new Response('[]', { status: 200, headers: { 'content-type': 'application/json' } })
      )
    }

    await new GitHubDownloadCounter().count(reference, { fetch, token: 'ghp_example' })

    expect(JSON.stringify(seen[0])).toContain('Bearer ghp_example')
  })

  it('rejects rather than returning a partial total when the API fails', async () => {
    const { fetch } = fetchStub({
      [`${endpoint}?per_page=${GITHUB_PAGE_SIZE}&page=1`]: { status: 403 }
    })

    await expect(new GitHubDownloadCounter().count(reference, { fetch })).rejects.toThrow(/403/)
  })
})
