import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ModPackCard from 'components/ModPackCard.vue'

// The card fetches its modpack by id on mount; mock the axios boot-module so it never hits the
// network and resolves with a well-shaped payload the test controls.
vi.mock('boot/axios', () => ({
  modpacks: { get: vi.fn(() => Promise.resolve({ data: modpackPayload })) }
}))

let modpackPayload: Record<string, unknown>

/**
 * Characterization tests for ModPackCard — pins the fetch-then-display wiring and, in particular,
 * the `projectID/versionID.length === 1 ? value : 'N/A'` template quirk (a Modrinth id is shown
 * only when it is a single-character string). Added retroactively for the untested card.
 */
describe('ModPackCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    modpackPayload = {
      dateCreated: 0, name: 'Cool Pack', projectID: 'p', serverPacks: [{}, {}],
      sha256: 'abc', size: 12, source: 'CURSEFORGE', status: 'DONE', versionID: 'v'
    }
  })

  async function mountCard() {
    const wrapper = mount(ModPackCard, { props: { id: '7' } })
    await flushPromises() // let mounted()'s axios promise settle (also flips `visible` to false)
    return wrapper
  }

  it('loads the modpack by id and exposes the fetched fields', async () => {
    const wrapper = await mountCard()
    expect(wrapper.vm.name).toBe('Cool Pack')
    expect(wrapper.vm.size).toBe(12)
    expect(wrapper.vm.serverPacks).toHaveLength(2)
  })

  it('shows a Modrinth id only when it is a single-character string, else N/A', async () => {
    modpackPayload.projectID = 'a' // length 1 -> shown verbatim
    modpackPayload.versionID = 'abcd' // length 4 -> N/A
    const wrapper = await mountCard()

    // Captions render in template order: [0] ModPack ID, [1] Project ID, [2] Version ID, …
    const captions = wrapper.findAll('.q-item__label--caption')
    expect(captions[1]!.text()).toBe('a')
    expect(captions[2]!.text()).toBe('N/A')
  })

  it('renders the size in MB and the server-pack count', async () => {
    const wrapper = await mountCard()
    const captions = wrapper.findAll('.q-item__label--caption')
    expect(captions[5]!.text()).toBe('12 MB')
    expect(captions[9]!.text()).toBe('2') // serverPacks.length
  })
})
