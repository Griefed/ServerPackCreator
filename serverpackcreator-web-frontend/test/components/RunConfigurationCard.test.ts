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
 * Characterization test for RunConfigurationCard — pins how the card consumes a run-configuration's
 * three mod-lists and renders them.
 *
 * Those lists are **plain string arrays**: the web module embedded them, replacing the `{argument}` /
 * `{mod}` documents they used to be `@DBRef`s to. Every list therefore needs an assertion on the
 * *rendered text*, not on the prop: the card assigns `this.clientMods = runConfig.clientMods`
 * unchanged, so comparing `vm.clientMods` to the payload holds for any element type — verified, a card
 * reverted to `clientMods.map(m => m.mod).join(', ')` renders `undefined, undefined` and passed all 31
 * frontend tests.
 */
describe('RunConfigurationCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    runConfigPayload = {
      minecraftVersion: '1.20.1', modloader: 'Forge', modloaderVersion: '47.2.0',
      startArgs: ['-Xmx4G', '-Xms2G'],
      clientMods: ['optifine'],
      whitelistedMods: ['jei']
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

  // The rendered text, deliberately, and for every list: the assertions above compare the prop with the
  // payload it came from, which cannot tell a string array from an array of objects. Rendering can —
  // consuming the wrong element shape puts `undefined` on the card.
  it('renders the client mods and the whitelist as their own values', async () => {
    const wrapper = await mountCard()
    expect(wrapper.text()).toContain('optifine')
    expect(wrapper.text()).toContain('jei')
    expect(wrapper.text()).not.toContain('undefined')
    expect(wrapper.text()).not.toContain('[object Object]')
  })
})
