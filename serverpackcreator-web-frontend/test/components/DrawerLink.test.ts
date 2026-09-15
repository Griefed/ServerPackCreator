import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DrawerLink from 'components/DrawerLink.vue'

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

  it('defaults caption to empty and still renders the title', () => {
    const wrapper = mount(DrawerLink, { props: { title: 'About' } })
    expect(wrapper.text()).toContain('About')
  })
})
