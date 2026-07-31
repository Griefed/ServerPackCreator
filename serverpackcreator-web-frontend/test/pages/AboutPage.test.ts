import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AboutPage from 'pages/AboutPage.vue'
import AboutItem from 'components/AboutItem.vue'

/**
 * Pins the About page's outbound links.
 *
 * The page is a hand-maintained list of three external destinations (source, issues, Discord) rendered through
 * `AboutItem`. There is no logic to speak of, so this deliberately covers only the part that can be wrong without
 * anyone noticing: a link that is empty, relative or not `https` still renders as a perfectly normal-looking row, and
 * the only symptom is a user clicking it and going nowhere.
 */
describe('AboutPage', () => {
  // QPage refuses to render outside a QLayout, which would leave the page's children unmounted; a passthrough stub
  // renders the slot so the AboutItem rows actually exist. Same reason the download-page tests use shallowMount.
  const mountPage = () =>
    mount(AboutPage, { global: { stubs: { QPage: { template: '<div><slot /></div>' } } } })

  it('renders one AboutItem per destination', () => {
    const wrapper = mountPage()

    const items = wrapper.findAllComponents(AboutItem)
    expect(items.length).toBeGreaterThan(0)
    expect(items.length).toBe((wrapper.vm.aboutItems as unknown[]).length)
  })

  it('gives every destination a title, an icon and an absolute https link', () => {
    const wrapper = mountPage()

    for (const item of wrapper.vm.aboutItems as Array<{ title: string; link: string; icon: string }>) {
      expect(item.title, 'an entry without a title renders as a blank row').toBeTruthy()
      expect(item.icon, `"${item.title}" has no icon`).toBeTruthy()
      expect(
        item.link,
        `"${item.title}" must link somewhere absolute — a relative or empty href leads nowhere from a SPA route`
      ).toMatch(/^https:\/\/\S+$/)
    }
  })

  it('passes each destination through to its AboutItem', () => {
    const wrapper = mountPage()
    const rendered = wrapper.findAllComponents(AboutItem).map(item => item.props())

    for (const expected of wrapper.vm.aboutItems as Array<{ title: string; link: string }>) {
      expect(
        rendered.some(props => props.title === expected.title && props.link === expected.link),
        `"${expected.title}" is in the data but was not handed to an AboutItem`
      ).toBe(true)
    }
  })
})
