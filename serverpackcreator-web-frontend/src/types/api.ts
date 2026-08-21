/**
 * Minimal shapes of the ServerPackCreator web-API payloads the frontend actually reads. These are
 * not exhaustive mirrors of the backend DTOs — they cover the fields the components and templates
 * consume, so single-file-component templates type-check under strict mode.
 */

/** A single configuration/generation error attached to a history event, rendered by ErrorsCard. */
export interface ErrorItem {
  id: number
  error: string
}

/** A modpack record as keyed by id in SubmitModPackForm's modpack picker. */
export interface ModPack {
  id: string
  projectID: string
  versionID: string
  dateCreated: string | number
  name: string
  size: number
  status: string
  source: string
  sha256: string
  serverPacks: unknown[]
}

/** A run-configuration record as keyed by id in SubmitModPackForm's run-config picker. */
export interface RunConfiguration {
  id: string
  minecraftVersion: string
  modloader: string
  modloaderVersion: string
  startArgs: string[]
  clientMods: string[]
  whitelistedMods: string[]
}
