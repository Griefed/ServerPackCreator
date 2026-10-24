import type { RepositoryReference } from './types'

/** github.com serves its API from a different origin; every other GitHub does not. */
const GITHUB_DOT_COM = 'https://github.com'
const GITHUB_DOT_COM_API = 'https://api.github.com'

/**
 * The releases endpoint for a repository. This is the only place in the library where an API path
 * is written, which is what lets a caller point it at any instance of a supported forge.
 */
export function releasesEndpoint(reference: RepositoryReference): string {
  const { platform, host, owner, repository } = reference
  const path = `repos/${owner}/${repository}/releases`

  if (platform === 'github') {
    // GitHub Enterprise serves the same API from `/api/v3` on the instance's own origin.
    return host === GITHUB_DOT_COM
      ? `${GITHUB_DOT_COM_API}/${path}`
      : `${host}/api/v3/${path}`
  }

  return `${host}/api/v1/${path}`
}
