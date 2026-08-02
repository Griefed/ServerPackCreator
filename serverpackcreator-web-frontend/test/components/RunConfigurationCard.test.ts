import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import RunConfigurationCard from 'components/RunConfigurationCard.vue'

// The card fetches its run-configuration by id on mount; mock the axios boot-module. RunConfiguration
// imports `runConfigs` via a relative path, which resolves to the same module the alias points at,
// so mocking 'boot/axios' intercepts it.
vi.mock('boot/axios', () => ({
  runConfigs: { get: vi.fn(() => Promise.resolve({ data: runConfigPayload })) }
}))

let runConfigPayload: Record<string, unknown>

/**
 * Characterization test for RunConfigurationCard — pins the array-flattening derivations the card
 * performs on mount: the nested `{argument}` / `{mod}` objects from the backend are mapped to flat
 * string arrays and joined for display. Added retroactively for the untested card.
 */
describe('RunConfigurationCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    runConfigPayload = {
      minecraftVersion: '1.20.1', modloader: 'Forge', modloaderVersion: '47.2.0',
      startArgs: [{ argument: '-Xmx4G' }, { argument: '-Xms2G' }],
      clientMods: [{ mod: 'optifine' }],
      whitelistedMods: [{ mod: 'jei' }]
    }
  })

  async function mountCard() {
    const wrapper = mount(RunConfigurationCard, { props: { id: '9' } })
    await flushPromises()
    return wrapper
  }

  it('flattens the nested start-args / client-mods / whitelist arrays to plain strings', async () => {
    const wrapper = await mountCard()
    expect(wrapper.vm.startArgs).toEqual(['-Xmx4G', '-Xms2G'])
    expect(wrapper.vm.clientMods).toEqual(['optifine'])
    expect(wrapper.vm.whitelistedMods).toEqual(['jei'])
  })

  it('joins the start arguments with spaces for display', async () => {
    const wrapper = await mountCard()
    expect(wrapper.text()).toContain('-Xmx4G -Xms2G')
  })
})
