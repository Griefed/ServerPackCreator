import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ErrorsCard from 'components/ErrorsCard.vue'
import type { ErrorItem } from 'src/types/api'

/**
 * Pins that ErrorsCard renders one row per error and shows each error's id and message — the
 * component's whole job (a scrollable list of generation errors for a history event).
 */
describe('ErrorsCard', () => {
  const errors: ErrorItem[] = [
    { id: 1, error: 'mods/foo.jar is clientside' },
    { id: 2, error: 'config/bar.toml missing' }
  ]

  it('renders one item per error with its id and message', () => {
    const wrapper = mount(ErrorsCard, { props: { errors } })

    expect(wrapper.findAll('.q-item')).toHaveLength(errors.length)

    const text = wrapper.text()
    expect(text).toContain('mods/foo.jar is clientside')
    expect(text).toContain('config/bar.toml missing')
    expect(text).toContain('1')
    expect(text).toContain('2')
  })

  it('renders an empty list when there are no errors', () => {
    const wrapper = mount(ErrorsCard, { props: { errors: [] } })
    expect(wrapper.findAll('.q-item')).toHaveLength(0)
  })
})
