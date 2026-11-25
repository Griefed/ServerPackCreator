import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ModPackDownload from '@/pages/ModPackDownload.vue'
import { modpacks } from '@/boot/axios'

// Mock the axios boot-module: the page fetches the modpack by id on mount. `baseURL` is part of the
// mock because the page builds its download link from it rather than restating the API path -- see
// the boot module, which is the one place that path is written.
vi.mock('@/boot/axios', () => ({
  modpacks: {
    defaults: { baseURL: '/api/v2/modpacks/' },
    get: vi.fn(() => Promise.resolve({ data: modpackPayload })),
    head: vi.fn(() => Promise.resolve({ status: 200 }))
  }
}))

let modpackPayload: Record<string, unknown>

/** Clicks the anchor as a user would, but stops happy-dom from navigating to its href. */
const clickWithoutNavigating = async (anchor: Element) => {
  anchor.addEventListener('click', (event) => event.preventDefault(), { once: true })
  anchor.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
  await flushPromises()
}

/** QPage refuses to render outside a QLayout, so page tests pass its slot straight through. */
const pageStub = { QPage: { template: '<div><slot /></div>' } }

const mountPage = () =>
  mount(ModPackDownload, {
    global: {
      stubs: pageStub,
      mocks: { $route: { params: { id: '42' } }, $router: { resolve: () => ({ href: 'http://x/' }) } }
    }
  })

/** ModPackDownload: the download link, the name it shows, and that the archive never passes through the page. */
describe('ModPackDownload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    modpackPayload = {
      name: 'My Cool Pack.zip', dateCreated: 0, projectID: '', versionID: '', serverPacks: [],
      sha256: '', size: 0, source: '', status: ''
    }
  })

  // In afterEach, not the test body, so a failed assertion cannot leak a stubbed global into later tests.
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  /**
   * The server names the saved file through `Content-Disposition`, from the modpack's name kept verbatim.
   * The page shows that same name and leaves the `download` attribute without a value, so no second,
   * client-side name exists to disagree with it.
   */
  it('shows the name the server saves the archive under and does not rename it', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('My Cool Pack.zip')
    expect(wrapper.get('a.q-btn[download]').attributes('download')).toBe('')
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
    await clickWithoutNavigating(wrapper.get('a.q-btn[download]').element)
    await flushPromises()

    expect(createObjectURL).not.toHaveBeenCalled()
    const requested = vi.mocked(modpacks.get).mock.calls.map((call) => String(call[0]))
    expect(requested.some((url) => url.includes('download/'))).toBe(false)
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

/**
 * The anchor hands the archive to the browser, which reports a 404 or 500 only in its own download list. The
 * page therefore asks the route with `HEAD` first, and claims "Downloading" only once the server said the
 * archive is there; a failure is shown on the page instead.
 */
describe('ModPackDownload download feedback', () => {
  let click: MockInstance<() => void>

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout'] })
    click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
  })

  afterEach(() => {
    click.mockRestore()
    vi.useRealTimers()
  })

  /** Mounts the page and lets it load its record, leaving the five-second countdown running. */
  const mountLoaded = async () => {
    const wrapper = mountPage()
    await flushPromises()
    return wrapper
  }

  it('clicks the link when the countdown ends and the archive is available', async () => {
    const wrapper = await mountLoaded()

    await vi.advanceTimersByTimeAsync(5000)
    await flushPromises()

    expect(vi.mocked(modpacks.head)).toHaveBeenCalledWith('download/42')
    expect(click).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Downloading')
  })

  it('shows a failure instead of claiming a download when the archive is not available', async () => {
    vi.mocked(modpacks.head).mockRejectedValueOnce(new Error('Request failed with status code 404'))
    const wrapper = await mountLoaded()

    await vi.advanceTimersByTimeAsync(5000)
    await flushPromises()

    expect(click).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Download failed')
    expect(wrapper.text()).not.toContain('Downloading')
  })

  it('does not download once the countdown is cancelled', async () => {
    const wrapper = await mountLoaded()

    await wrapper.get('button.bg-negative').trigger('click')
    await vi.advanceTimersByTimeAsync(5000)
    await flushPromises()

    expect(click).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Download aborted')
  })

  it('stops counting down once the page is unmounted', async () => {
    const wrapper = await mountLoaded()
    const vm = wrapper.vm as unknown as { counter: number }
    const before = vm.counter
    wrapper.unmount()

    await vi.advanceTimersByTimeAsync(5000)
    await flushPromises()

    expect(vm.counter).toBe(before)
    expect(click).not.toHaveBeenCalled()
  })

  it('claims nothing when there is no link to click', async () => {
    const wrapper = mount(ModPackDownload, {
      global: {
        stubs: { ...pageStub, QBtn: { render: () => null } },
        mocks: { $route: { params: { id: '42' } }, $router: { resolve: () => ({ href: 'http://x/' }) } }
      }
    })
    await flushPromises()

    await vi.advanceTimersByTimeAsync(5000)
    await flushPromises()

    expect(click).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('Downloading')
  })

  it('takes back the claim when a clicked link turns out to be unavailable', async () => {
    vi.mocked(modpacks.head).mockRejectedValueOnce(new Error('Request failed with status code 500'))
    const wrapper = await mountLoaded()

    await clickWithoutNavigating(wrapper.get('a.q-btn[download]').element)
    await flushPromises()

    expect(wrapper.text()).toContain('Download failed')
    expect(wrapper.text()).not.toContain('Downloading')
  })
})
