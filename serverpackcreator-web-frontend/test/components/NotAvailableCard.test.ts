import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import NotAvailableCard from 'components/NotAvailableCard.vue'

/**
 * NotAvailableCard is a static placeholder shown in card-grids where no entity exists. The whole of
 * its behavior is rendering the "N/A" marker, which this pins.
 */
describe('NotAvailableCard', () => {
  it('renders the N/A placeholder', () => {
    const wrapper = mount(NotAvailableCard)
    expect(wrapper.text()).toContain('N/A')
  })
})
