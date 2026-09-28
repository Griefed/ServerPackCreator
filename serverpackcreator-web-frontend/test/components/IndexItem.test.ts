import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import IndexItem from 'components/IndexItem.vue'

/**
 * Pins IndexItem's presentational contract: a landing-page entry that renders its title and caption
 * and a navigation button carrying the named icon. Like DrawerLink, the `:to` routing is left to
 * integration (no router mounted here).
 */
describe('IndexItem', () => {
  it('renders its title, caption and the navigation-button icon', () => {
    const wrapper = mount(IndexItem, {
      props: { title: 'Downloads', caption: 'Browse available packs', icon: 'download', link: '/downloads' }
    })

    expect(wrapper.text()).toContain('Downloads')
    expect(wrapper.text()).toContain('Browse available packs')
    expect(wrapper.find('.q-icon').text()).toBe('download')
  })
})
