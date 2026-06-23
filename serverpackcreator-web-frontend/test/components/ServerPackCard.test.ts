import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ServerPackCard from 'components/ServerPackCard.vue'

// The card fetches its server pack by id on mount; mock the axios boot-module so it resolves with
// a controlled payload and never touches the network.
vi.mock('boot/axios', () => ({
  serverpacks: { get: vi.fn(() => Promise.resolve({ data: serverPackPayload })) }
}))

let serverPackPayload: Record<string, unknown>

/**
 * Characterization test for ServerPackCard — pins the fetch-then-display wiring (the fields the
 * card copies off the response) and the "size in MB" rendering. Added retroactively for the
 * untested card.
 */
describe('ServerPackCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    serverPackPayload = { dateCreated: 0, sha256: 'deadbeef', size: 34, downloads: 5, confirmedWorking: -1 }
  })

  async function mountCard() {
    const wrapper = mount(ServerPackCard, { props: { id: '3' } })
    await flushPromises()
    return wrapper
  }

  it('loads the server pack by id and exposes the fetched fields', async () => {
    const wrapper = await mountCard()
    expect(wrapper.vm.size).toBe(34)
    expect(wrapper.vm.downloads).toBe(5)
    expect(wrapper.vm.confirmedWorking).toBe(-1)
    expect(wrapper.vm.sha256).toBe('deadbeef')
  })

  it('renders the size in MB', async () => {
    const wrapper = await mountCard()
    const captions = wrapper.findAll('.q-item__label--caption')
    expect(captions[1]!.text()).toBe('34 MB') // [0] ServerPack ID, [1] Size
  })
})
