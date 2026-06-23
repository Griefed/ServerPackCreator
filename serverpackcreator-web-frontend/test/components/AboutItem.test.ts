import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AboutItem from 'components/AboutItem.vue'

/**
 * First component test — exercises the Quasar test harness (`test/install-quasar.ts`) by mounting a
 * real Quasar-backed SFC and asserting it renders its props. AboutItem is a pure presentational
 * component (no axios/store), so it is the natural smoke-test for the harness itself.
 */
describe('AboutItem', () => {
  it('renders the link text and a button that links to it', () => {
    const wrapper = mount(AboutItem, {
      props: {
        title: 'Source',
        link: 'https://github.com/Griefed/ServerPackCreator',
        icon: 'account_tree'
      }
    })

    expect(wrapper.text()).toContain('https://github.com/Griefed/ServerPackCreator')

    const anchor = wrapper.get('a')
    expect(anchor.attributes('href')).toBe('https://github.com/Griefed/ServerPackCreator')
    expect(anchor.attributes('target')).toBe('_blank')
  })
})
