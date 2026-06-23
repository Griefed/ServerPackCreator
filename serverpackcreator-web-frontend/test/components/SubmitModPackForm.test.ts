import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import SubmitModPackForm from 'components/SubmitModPackForm.vue'

// Mock the axios boot-module so mounting the form never touches the network. The store imports
// `settings`; the form imports `modpacks`/`runConfigs`/`versions`. All resolve with empty-ish,
// well-shaped payloads so setup()/mounted() complete without firing error notifications.
vi.mock('boot/axios', () => ({
  settings: {
    get: vi.fn(() =>
      Promise.resolve({
        data: {
          clientsideMods: [], whitelistMods: [], supportedModloaders: [], version: '',
          devBuild: false, directoriesToInclude: [], directoriesToExclude: [],
          zipArchiveExclusions: [], exclusionFilter: '', isZipFileExclusionEnabled: true,
          isAutoExcludingModsEnabled: true, isMinecraftPreReleasesAvailabilityEnabled: false,
          aikarsFlags: '', language: ''
        }
      })
    )
  },
  modpacks: { get: vi.fn(() => Promise.resolve({ data: [] })), postForm: vi.fn(() => Promise.resolve({ data: {} })) },
  runConfigs: { get: vi.fn(() => Promise.resolve({ data: [] })) },
  versions: {
    get: vi.fn(() =>
      Promise.resolve({ data: { minecraft: [], fabric: [], legacyFabric: [], quilt: [], forge: {}, neoForge: {} } })
    )
  }
}))

/**
 * Behavior tests for SubmitModPackForm — added retroactively as the characterization coverage the
 * component lacked when it was migrated to TypeScript. Mounts the real SFC against a mocked axios
 * boot-module and exercises its methods through the component instance.
 */
describe('SubmitModPackForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  async function mountForm() {
    const wrapper = mount(SubmitModPackForm)
    await flushPromises() // let setup()/mounted() axios promises settle before assertions
    return wrapper
  }

  /**
   * Regression test for the bug fixed during the TS migration: QFile's `@rejected` hands back an
   * ARRAY of rejected entries, so the previous `rejectedEntry.name` was always undefined and the
   * toast read "undefined is not a ZIP-file". It must now name the offending file.
   */
  it('onRejected names the rejected file in the notification', async () => {
    const wrapper = await mountForm()
    const notify = vi.spyOn(wrapper.vm.$q, 'notify')

    wrapper.vm.onRejected([
      { failedPropValidation: 'accept', file: new File([], 'totally-not-a-zip.exe') }
    ])

    expect(notify).toHaveBeenCalledTimes(1)
    expect(notify).toHaveBeenCalledWith(
      expect.objectContaining({ message: 'totally-not-a-zip.exe is not a ZIP-file', type: 'negative' })
    )
  })

  /** Selecting a modloader points the version dropdown at that loader's versions and picks the first. */
  it('modloaderSelected wires the version list to the chosen loader', async () => {
    const wrapper = await mountForm()
    wrapper.vm.fabricVersions = ['0.16.0', '0.15.11']

    wrapper.vm.modloaderSelected('Fabric')

    expect(wrapper.vm.modloader).toBe('Fabric')
    expect(wrapper.vm.modloaderVersions).toEqual(['0.16.0', '0.15.11'])
    expect(wrapper.vm.modloaderVersion).toBe('0.16.0')
  })

  /** Picking a run configuration copies its fields into the form, flattening the arg/mod arrays. */
  it('selectedRunConfiguration populates the form from the stored config', async () => {
    const wrapper = await mountForm()
    wrapper.vm.runConfigurations = {
      rc1: {
        id: 'rc1',
        minecraftVersion: '1.20.1',
        modloader: 'Forge',
        modloaderVersion: '47.1.0',
        startArgs: [{ argument: '-Xmx4G' }, { argument: '-Xms2G' }],
        clientMods: [{ mod: 'optifine' }],
        whitelistedMods: [{ mod: 'jei' }, { mod: 'jade' }]
      }
    }

    wrapper.vm.selectedRunConfiguration('rc1')

    expect(wrapper.vm.minecraftVersion).toBe('1.20.1')
    expect(wrapper.vm.modloader).toBe('Forge')
    expect(wrapper.vm.modloaderVersion).toBe('47.1.0')
    expect(wrapper.vm.startArgs).toBe('-Xmx4G, -Xms2G')
    expect(wrapper.vm.clientMods).toBe('optifine')
    expect(wrapper.vm.whiteListMods).toBe('jei, jade')
  })

  /** An unknown run-config id is a no-op (guards the strict-mode undefined lookup). */
  it('selectedRunConfiguration ignores an unknown id', async () => {
    const wrapper = await mountForm()
    wrapper.vm.minecraftVersion = 'untouched'

    wrapper.vm.selectedRunConfiguration('does-not-exist')

    expect(wrapper.vm.runConfigID).toBe('does-not-exist')
    expect(wrapper.vm.minecraftVersion).toBe('untouched')
  })
})
