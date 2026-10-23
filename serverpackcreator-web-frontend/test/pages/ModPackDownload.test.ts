import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, shallowMount, flushPromises } from '@vue/test-utils'
import ModPackDownload from '@/pages/ModPackDownload.vue'
import { modpacks } from '@/boot/axios'

// Mock the axios boot-module: the page fetches the modpack by id on mount. `baseURL` is part of the
// mock because the page builds its download link from it rather than restating the API path -- see
// the boot module, which is the one place that path is written.
vi.mock('@/boot/axios', () => ({
  modpacks: {
    defaults: { baseURL: '/api/v2/modpacks/' },
    get: vi.fn(() => Promise.resolve({ data: modpackPayload }))
  }
}))

let modpackPayload: Record<string, unknown>

/** QPage refuses to render outside a QLayout, so page tests pass its slot straight through. */
const pageStub = { QPage: { template: '<div><slot /></div>' } }

const mountPage = () =>
  mount(ModPackDownload, {
    global: {
      stubs: pageStub,
      mocks: { $route: { params: { id: '42' } }, $router: { resolve: () => ({ href: 'http://x/' }) } }
    }
  })

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

  /**
   * The defect this page was rewritten for: it fetched the whole archive over XHR as an
   * `arraybuffer`, wrapped it in a Blob and handed the browser an object URL. The modpack was
   * therefore buffered in the tab's memory in full before the save dialog appeared -- which is why a
   * download reported itself finished moments after it started -- and the shipped upload limit is
   * 5000MB, so the archives this serves can exceed what a tab can hold. The archive must never
   * travel through JavaScript.
   */
  it('hands the archive to the browser instead of buffering it in the page', async () => {
    const createObjectURL = vi.fn(() => 'blob:nope')
    vi.stubGlobal('URL', { ...window.URL, createObjectURL })

    const wrapper = mountPage()
    await flushPromises()
    await wrapper.get('a.q-btn[download]').trigger('click')
    await flushPromises()

    expect(createObjectURL).not.toHaveBeenCalled()
    const requested = vi.mocked(modpacks.get).mock.calls.map((call) => String(call[0]))
    expect(requested.some((url) => url.includes('download/'))).toBe(false)

    vi.unstubAllGlobals()
  })

  /**
   * The link is a real anchor, so the browser streams it straight to disk and the usual affordances
   * -- right-click, middle-click, copy link address, a download manager that can resume -- work.
   * Its href is derived from the boot module's `baseURL` rather than restating the API path.
   */
  it('points the download link at the modpack download endpoint', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.get('a.q-btn[download]').attributes('href')).toBe('/api/v2/modpacks/download/42')
  })
})
