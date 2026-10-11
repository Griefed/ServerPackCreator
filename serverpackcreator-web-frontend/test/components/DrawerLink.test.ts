import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DrawerLink from '@/components/DrawerLink.vue'

/**
 * Pins DrawerLink's presentational contract: a navigation list-item that renders its title and
 * caption text and the named icon. Routing (the `:to` binding) is left to integration — without a
 * router the QItem renders a plain item, so this asserts only what reliably renders.
 */
describe('DrawerLink', () => {
  it('renders its title, caption and icon', () => {
    const wrapper = mount(DrawerLink, {
      props: { title: 'Submit', caption: 'Create a server pack', link: '/submit', icon: 'send' }
    })

    expect(wrapper.text()).toContain('Submit')
    expect(wrapper.text()).toContain('Create a server pack')
    // A material-icon QIcon renders the icon name as its ligature text content.
    expect(wrapper.find('.q-icon').text()).toBe('send')
  })

  it('tints the icon with the accent colour', () => {
    const wrapper = mount(DrawerLink, {
      props: { title: 'Submit', caption: 'Create a server pack', link: '/submit', icon: 'send' }
    })

    // Quasar turns `color="accent"` into the `text-accent` class. A misspelled prop - this component
    // shipped `colour=` once - is silently dropped by Vue and leaves the icon in the default colour,
    // which renders perfectly and looks like nobody's bug. Asserting the class is what catches it.
    expect(wrapper.find('.q-icon').classes()).toContain('text-accent')
  })

  it('defaults caption to empty and still renders the title', () => {
    const wrapper = mount(DrawerLink, { props: { title: 'About' } })
    expect(wrapper.text()).toContain('About')
  })
})
