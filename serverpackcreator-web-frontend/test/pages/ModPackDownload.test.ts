import { beforeEach, describe, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import ModPackDownload from 'pages/ModPackDownload.vue'

// Mock the axios boot-module: the page fetches the modpack by id on mount.
vi.mock('boot/axios', () => ({
  modpacks: { get: vi.fn(() => Promise.resolve({ data: modpackPayload })) }
}))

let modpackPayload: Record<string, unknown>

/**
 * Characterization test for ModPackDownload — pins the download-filename derivation (spaces in the
 * modpack name become underscores), the behavior the page exists for. Added retroactively for the
 * untested page (audit M1).
 */
describe('ModPackDownload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    modpackPayload = {
      name: 'My Cool Pack', dateCreated: 0, projectID: '', versionID: '', serverPacks: [],
      sha256: '', size: 0, source: '', status: ''
    }
  })

  it('derives the download name from the modpack name with spaces underscored', async () => {
    const wrapper = shallowMount(ModPackDownload, {
      global: { mocks: { $route: { params: { id: '42' } }, $router: { resolve: () => ({ href: 'http://x/' }) } } }
    })
    await flushPromises()

    expect(wrapper.vm.name).toBe('My_Cool_Pack')
  })
})
