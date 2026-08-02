import { beforeEach, describe, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import ServerPackDownload from 'pages/ServerPackDownload.vue'

// Mock the axios boot-module: the page fetches the server pack and its owning modpack on mount.
vi.mock('boot/axios', () => ({
  serverpacks: { get: vi.fn(() => Promise.resolve({ data: { size: 0, downloads: 0, confirmedWorking: 0, dateCreated: 0, sha256: '' } })) },
  modpacks: { get: vi.fn(() => Promise.resolve({ data: { name: modpackName } })) }
}))

let modpackName: string

/**
 * Characterization test for ServerPackDownload — pins the download-filename derivation: the owning
 * modpack's name has spaces underscored and its `.zip` suffix rewritten to `_server_pack.zip`.
 * Added retroactively for the untested page (audit M1).
 */
describe('ServerPackDownload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    modpackName = 'My Cool Pack.zip'
  })

  it('derives the server-pack filename from the owning modpack name', async () => {
    const wrapper = shallowMount(ServerPackDownload, {
      global: { mocks: { $route: { params: { id: '7' } }, $router: { resolve: () => ({ href: 'http://x/' }) } } }
    })
    await flushPromises()

    expect(wrapper.vm.name).toBe('My_Cool_Pack_server_pack.zip')
  })
})
