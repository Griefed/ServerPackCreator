import { RepositoryUrlError, type Platform, type RepositoryReference } from './types'

/**
 * Resolve a repository URL into the pieces a counter addresses an API with. Accepts anything of the
 * shape `<origin>/<owner>/<repository>`, tolerating a trailing slash, a `.git` suffix and deeper
 * paths such as `/tree/main`, so a URL copied out of a browser works.
 *
 * The first two path segments are always owner and repository, so an instance served under a path
 * prefix (`https://host/git/owner/repo`) is not supported: it is indistinguishable from a deeper path.
 */
export function parseRepositoryUrl(platform: Platform, url: string): RepositoryReference {
  let parsed: URL
  try {
    parsed = new URL(url)
  } catch {
    throw new RepositoryUrlError(url, 'it is not an absolute URL')
  }

  const segments = parsed.pathname
    .split('/')
    .filter((segment) => segment.length > 0)
    .map((segment) => decodeURIComponent(segment))
  if (segments.length < 2) {
    throw new RepositoryUrlError(url, 'its path names no owner and repository')
  }

  const [owner, repository] = segments as [string, string]
  return {
    platform,
    host: parsed.origin,
    owner,
    repository: repository.replace(/\.git$/, '')
  }
}
