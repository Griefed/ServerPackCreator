import { defineStore, acceptHMRUpdate } from 'pinia'
import {settings} from "../boot/axios.js";

export const settingsStore = defineStore('settings', {
  state: () => ({
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
