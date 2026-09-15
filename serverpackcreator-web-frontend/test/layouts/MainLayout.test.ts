import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MainLayout from 'layouts/MainLayout.vue'
import routes from 'src/router/routes'

// `routes` statically imports the two download pages, which reach `boot/axios` and its Quasar-only
// `#q-app/wrappers`. Mocking the boot module keeps that chain out of the test, as the data-card tests do.
vi.mock('boot/axios', () => ({
  modpacks: { get: vi.fn(() => Promise.resolve({ data: {} })) },
  serverpacks: { get: vi.fn(() => Promise.resolve({ data: {} })) },
  settings: { get: vi.fn(() => Promise.resolve({ data: {} })) }
}))

/**
 * Pins the two things in MainLayout that can break without anyone noticing.
 *
 * The drawer's `linksList` is a hand-maintained array that has to stay in step with the router: add a page and forget
 * the entry and the page is simply unreachable from the UI, with nothing failing. And `drawerClick` calls
 * `stopPropagation` for a reason — the click that toggles the mini-state also bubbles to the drawer itself, which would
 * immediately toggle it back, so dropping that call breaks the mini-drawer while leaving every other test green.
 *
 * The nav list is compared against the router rather than against a copy of itself; asserting the array equals a
 * literal would pass forever regardless of what the app actually routes.
 */
describe('MainLayout', () => {
  /** Mount with `router-view` stubbed — the layout's outlet is meaningless without a router. */
  const mountLayout = () => mount(MainLayout, { global: { stubs: { 'router-view': true } } })

  /** The child paths of the layout route — the pages a drawer link can point at. */
  const layoutChildPaths = (): string[] => {
    const layoutRoute = routes.find(route => route.path === '/')
    return (layoutRoute?.children ?? [])
      .map(child => child.path)
      .filter(path => path !== '' && !path.includes(':'))
  }

  it('offers a drawer link for every routable page', () => {
    const wrapper = mountLayout()
    const linked: string[] = wrapper.vm.linksList.map((entry: { link: string }) => entry.link)

    for (const path of layoutChildPaths()) {
      expect(
        linked,
        `no drawer link points at /${path} — the page would be unreachable from the UI`
      ).toContain(`/${path}`)
    }
  })

  it('points every drawer link at a route that exists', () => {
    const wrapper = mountLayout()
    const routable = ['/', ...layoutChildPaths().map(path => `/${path}`)]

    for (const entry of wrapper.vm.linksList as Array<{ title: string; link: string }>) {
      expect(routable, `the "${entry.title}" drawer link points at ${entry.link}, which is not routed`)
        .toContain(entry.link)
    }
  })

  it('gives every drawer link a title, caption and icon', () => {
    const wrapper = mountLayout()

    for (const entry of wrapper.vm.linksList as Array<{ title: string; caption: string; icon: string }>) {
      expect(entry.title, 'a drawer link without a title renders as a blank row').toBeTruthy()
      expect(entry.caption, `"${entry.title}" has no caption`).toBeTruthy()
      expect(entry.icon, `"${entry.title}" has no icon`).toBeTruthy()
    }
  })

  it('toggles the mini-state and stops the click from bubbling', () => {
    const wrapper = mountLayout()
    let propagationStopped = false
    const event = { stopPropagation: () => { propagationStopped = true } } as unknown as Event

    expect(wrapper.vm.miniState).toBe(true)

    wrapper.vm.drawerClick(event)

    expect(wrapper.vm.miniState).toBe(false)
    expect(
      propagationStopped,
      'without stopPropagation the click also reaches the drawer, which toggles the mini-state straight back'
    ).toBe(true)

    // And it is a toggle, not a one-way latch.
    wrapper.vm.drawerClick(event)
    expect(wrapper.vm.miniState).toBe(true)
  })

  it('starts with the drawer open', () => {
    const wrapper = mountLayout()
    expect(wrapper.vm.drawer).toBe(true)
  })
})
