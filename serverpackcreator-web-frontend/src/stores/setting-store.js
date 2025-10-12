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

  getters: {
    doubleCount: (state) => state.counter * 2
  },

  actions: {
    async refresh() {
      settings.get('current').then(response => {
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
      }).catch(error => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'Could not retrieve settings: ' + error
        });
      });
    }
  }
})

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(settingsStore, import.meta.hot))
}
