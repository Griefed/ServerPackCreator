// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { parseRepositoryUrl } from '@/services/downloads/repositoryReference'
import { RepositoryUrlError } from '@/services/downloads/types'

describe('parseRepositoryUrl', () => {
  it('takes the owner and repository from the first two path segments', () => {
    expect(parseRepositoryUrl('github', 'https://github.com/Griefed/ServerPackCreator')).toEqual({
      platform: 'github',
      host: 'https://github.com',
      owner: 'Griefed',
      repository: 'ServerPackCreator'
    })
  })

  it('keeps the origin of a self-hosted instance, which is the whole point of taking a URL', () => {
    expect(
      parseRepositoryUrl('forgejo', 'https://git.griefed.de/Griefed/ServerPackCreator').host
    ).toBe('https://git.griefed.de')
  })

  it('preserves a non-default port', () => {
    expect(parseRepositoryUrl('gitea', 'http://localhost:3000/me/thing').host).toBe(
      'http://localhost:3000'
    )
  })

  // Each of these is a URL somebody really does copy out of a browser or a clone command.
  it.each([
    ['a trailing slash', 'https://github.com/Griefed/ServerPackCreator/'],
    ['a .git suffix', 'https://github.com/Griefed/ServerPackCreator.git'],
    ['a deeper path', 'https://github.com/Griefed/ServerPackCreator/tree/main/docs'],
    ['a query string', 'https://github.com/Griefed/ServerPackCreator?tab=readme']
  ])('tolerates %s', (_label, url) => {
    const reference = parseRepositoryUrl('github', url)
    expect(reference.owner).toBe('Griefed')
    expect(reference.repository).toBe('ServerPackCreator')
  })

  it.each([
    ['no repository segment', 'https://github.com/Griefed'],
    ['no path at all', 'https://github.com'],
    ['not a URL', 'Griefed/ServerPackCreator']
  ])('rejects %s', (_label, url) => {
    expect(() => parseRepositoryUrl('github', url)).toThrow(RepositoryUrlError)
  })
})
