// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { releasesEndpoint } from '@/services/downloads/apiEndpoints'

const reference = (platform, host) => ({
  platform,
  host,
  owner: 'Griefed',
  repository: 'ServerPackCreator'
})

describe('releasesEndpoint', () => {
  it('sends github.com to the api.github.com host, which is a different origin', () => {
    expect(releasesEndpoint(reference('github', 'https://github.com'))).toBe(
      'https://api.github.com/repos/Griefed/ServerPackCreator/releases'
    )
  })

  it('uses the /api/v3 prefix for GitHub Enterprise, which serves the API on its own origin', () => {
    expect(releasesEndpoint(reference('github', 'https://github.example.com'))).toBe(
      'https://github.example.com/api/v3/repos/Griefed/ServerPackCreator/releases'
    )
  })

  it.each(['gitea', 'forgejo'])('uses /api/v1 on the instance itself for %s', (platform) => {
    expect(releasesEndpoint(reference(platform, 'https://git.griefed.de'))).toBe(
      'https://git.griefed.de/api/v1/repos/Griefed/ServerPackCreator/releases'
    )
  })
})
