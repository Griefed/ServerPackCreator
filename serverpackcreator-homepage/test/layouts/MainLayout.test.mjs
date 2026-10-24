import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MainLayout from 'layouts/MainLayout.vue'

describe('MainLayout', () => {
  // The three header tiles (Help, API docs, Addons) each colour their icon panel with
  // `:style="'background-color:' + secondaryColour"`. The assertion is that the colour arrives --
  // not that any particular string was interpolated -- because an invalid declaration is dropped by
  // the DOM rather than kept, so "the bug's text is present" is not a reliable way to ask.
  it('gives every header icon tile a real background colour', () => {
    const wrapper = mount(MainLayout)

    const iconPanels = wrapper.findAll('.q-item__section--side')
    expect(iconPanels.length).toBe(3)

    for (const panel of iconPanels) {
      const colour = panel.attributes('style') ?? ''
      expect(colour).toContain('background-color')
      expect(colour).not.toContain('undefined')
    }
  })
})
