import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { settings } from 'boot/axios.js'
import { settingsStore } from 'stores/setting-store'

// Mock the axios boot-module so tests never touch the network and never pull in the
// Quasar-only `#q-app/wrappers` import chain. The store imports `../boot/axios.js`, which
// resolves to the same module id as the `boot/axios.js` alias mocked here.
vi.mock('boot/axios.js', () => ({
  settings: { get: vi.fn() },
}))

/**
 * Tests for the settings Pinia-store — the first frontend tests (Vitest), establishing the
 * pattern of testing a store's data-fetching action against a mocked axios instance.
 */
describe('settingsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('refresh() requests /settings/current and maps the response into state', async () => {
    settings.get.mockResolvedValue({
      data: {
        clientsideMods: ['SomeClientMod-'],
        whitelistMods: ['SomeWhitelisted-'],
        supportedModloaders: ['Forge', 'Fabric', 'Quilt', 'LegacyFabric', 'NeoForge'],
        version: '6.0.0',
        devBuild: false,
        directoriesToInclude: ['mods', 'config'],
        directoriesToExclude: ['logs'],
        zipArchiveExclusions: ['server.jar'],
        exclusionFilter: 'START',
        isZipFileExclusionEnabled: true,
        isAutoExcludingModsEnabled: true,
        isMinecraftPreReleasesAvailabilityEnabled: false,
        aikarsFlags: '-Xmx4G',
        language: 'en_GB',
      },
    })

    const store = settingsStore()
    await store.refresh()

    expect(settings.get).toHaveBeenCalledWith('current')
    expect(store.clientsideMods).toEqual(['SomeClientMod-'])
    expect(store.whitelistMods).toEqual(['SomeWhitelisted-'])
    expect(store.supportedModloaders).toEqual(['Forge', 'Fabric', 'Quilt', 'LegacyFabric', 'NeoForge'])
    expect(store.version).toBe('6.0.0')
    expect(store.directoriesToInclude).toEqual(['mods', 'config'])
    expect(store.isZipFileExclusionEnabled).toBe(true)
    expect(store.isMinecraftPreReleasesAvailabilityEnabled).toBe(false)
    expect(store.aikarsFlags).toBe('-Xmx4G')
    expect(store.language).toBe('en_GB')
  })

  it('refresh() rejects when the request fails, leaving notification to the caller', async () => {
    const failure = new Error('network down')
    settings.get.mockRejectedValue(failure)

    const store = settingsStore()

    // The store no longer swallows errors with a `$q.notify` — it propagates so the
    // component (which has `$q`) can surface them. State stays at its defaults.
    await expect(store.refresh()).rejects.toBe(failure)
    expect(store.version).toBe('')
  })

  it('starts with empty defaults before any refresh', () => {
    const store = settingsStore()
    expect(store.clientsideMods).toEqual([])
    expect(store.supportedModloaders).toEqual([])
    expect(store.version).toBe('')
    expect(store.isZipFileExclusionEnabled).toBe(true)
  })
})
