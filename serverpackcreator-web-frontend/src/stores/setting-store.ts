import { defineStore, acceptHMRUpdate } from 'pinia'
import { settings } from '../boot/axios'

/**
 * Shape of the settings the backend serves at `GET /settings/current`, mirrored into the store's
 * reactive state. Declared explicitly so the empty-array defaults are typed as `string[]` rather
 * than inferred as `never[]` under strict mode.
 */
interface SettingsState {
  clientsideMods: string[]
  whitelistMods: string[]
  supportedModloaders: string[]
  version: string
  devBuild: boolean
  directoriesToInclude: string[]
  directoriesToExclude: string[]
  zipArchiveExclusions: string[]
  exclusionFilter: string
  isZipFileExclusionEnabled: boolean
  isAutoExcludingModsEnabled: boolean
  isMinecraftPreReleasesAvailabilityEnabled: boolean
  aikarsFlags: string
  language: string
}

export const settingsStore = defineStore('settings', {
  state: (): SettingsState => ({
    clientsideMods: [],
    whitelistMods: [],
    supportedModloaders: [],
    version: '',
    devBuild: false,
    directoriesToInclude: [],
    directoriesToExclude: [],
    zipArchiveExclusions: [],
    exclusionFilter: '',
    isZipFileExclusionEnabled: true,
    isAutoExcludingModsEnabled: true,
    isMinecraftPreReleasesAvailabilityEnabled: false,
    aikarsFlags: '',
    language: ''
  }),

  actions: {
    /**
     * Fetches the current settings from the backend and maps the response into this store's state.
     * Pure data-fetching: the returned promise rejects on failure so the caller (a component, which
     * has access to Quasar's `$q`) can surface the error to the user — the store stays UI-agnostic.
     */
    async refresh() {
      return settings.get('current').then(response => {
        this.clientsideMods = response.data.clientsideMods
        this.whitelistMods = response.data.whitelistMods
        this.supportedModloaders = response.data.supportedModloaders
        this.version = response.data.version
        this.devBuild = response.data.devBuild
        this.directoriesToInclude = response.data.directoriesToInclude
        this.directoriesToExclude = response.data.directoriesToExclude
        this.zipArchiveExclusions = response.data.zipArchiveExclusions
        this.exclusionFilter = response.data.exclusionFilter
        this.isZipFileExclusionEnabled = response.data.isZipFileExclusionEnabled
        this.isAutoExcludingModsEnabled = response.data.isAutoExcludingModsEnabled
        this.isMinecraftPreReleasesAvailabilityEnabled = response.data.isMinecraftPreReleasesAvailabilityEnabled
        this.aikarsFlags = response.data.aikarsFlags
        this.language = response.data.language
      })
    }
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(settingsStore, import.meta.hot))
}
