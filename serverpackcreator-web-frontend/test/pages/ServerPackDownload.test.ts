import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, shallowMount, flushPromises } from '@vue/test-utils'
import ServerPackDownload from '@/pages/ServerPackDownload.vue'
import { serverpacks } from '@/boot/axios'

// Mock the axios boot-module: the page fetches the server pack and its owning modpack on mount.
// `baseURL` is part of the mock because the page builds its download link from it rather than
// restating the API path -- see the boot module, which is the one place that path is written.
vi.mock('@/boot/axios', () => ({
  serverpacks: {
    defaults: { baseURL: '/api/v2/serverpacks/' },
    get: vi.fn(() => Promise.resolve({ data: { size: 0, downloads: 0, confirmedWorking: 0, dateCreated: 0, sha256: '' } }))
  },
  modpacks: { get: vi.fn(() => Promise.resolve({ data: { name: modpackName } })) }
}))

let modpackName: string

/** QPage refuses to render outside a QLayout, so page tests pass its slot straight through. */
const pageStub = { QPage: { template: '<div><slot /></div>' } }

const mountPage = () =>
  mount(ServerPackDownload, {
    global: {
      stubs: pageStub,
      mocks: { $route: { params: { id: '7' } }, $router: { resolve: () => ({ href: 'http://x/' }) } }
    }
  })

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

  /**
   * The defect this page was rewritten for: it fetched the whole archive over XHR as an
   * `arraybuffer`, wrapped it in a Blob and handed the browser an object URL. A multi-gigabyte
   * server pack was therefore buffered in the tab's memory in full before the save dialog appeared,
   * which is why a download reported itself finished moments after it started -- and why a pack
   * larger than the tab's heap could not be downloaded at all. The archive must never travel
   * through JavaScript.
   */
  it('hands the archive to the browser instead of buffering it in the page', async () => {
    const createObjectURL = vi.fn(() => 'blob:nope')
    vi.stubGlobal('URL', { ...window.URL, createObjectURL })

    const wrapper = mountPage()
    await flushPromises()
    await wrapper.get('a.q-btn[download]').trigger('click')
    await flushPromises()

    expect(createObjectURL).not.toHaveBeenCalled()
    const requested = vi.mocked(serverpacks.get).mock.calls.map((call) => String(call[0]))
    expect(requested.some((url) => url.includes('download/'))).toBe(false)

    vi.unstubAllGlobals()
  })

  /**
   * The link is a real anchor, so the browser streams it straight to disk and the usual affordances
   * -- right-click, middle-click, copy link address, a download manager that can resume -- work.
   * Its href is derived from the boot module's `baseURL` rather than restating the API path.
   */
  it('points the download link at the server-pack download endpoint', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.get('a.q-btn[download]').attributes('href')).toBe('/api/v2/serverpacks/download/7')
  })
})
