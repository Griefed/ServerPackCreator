import type { Platform } from '@/services/downloads'

/**
 * Where ServerPackCreator is published. Site configuration, not library content -- the counter
 * library itself hard-codes no URL, which is what lets anyone else point it at their own forges.
 *
 * GitLab is absent because it reports no download counts at all, not because the mirror does not
 * exist. See src/services/downloads/types.ts.
 */
export const SERVERPACKCREATOR_HOSTS: Readonly<Partial<Record<Platform, string>>> = {
  github: 'https://github.com',
  gitea: 'https://gitea.com',
  forgejo: 'https://git.griefed.de'
}

export const SERVERPACKCREATOR_OWNER = 'Griefed'
export const SERVERPACKCREATOR_REPOSITORY = 'ServerPackCreator'
