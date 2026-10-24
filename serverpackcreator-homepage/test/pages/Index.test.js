import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

// The page refreshes its download total on mount. That is not what these tests are about, and
// letting it run would put a real request in a unit test.
vi.mock('@/services/downloads/downloadTotal', () => ({
  readDownloadTotal: () => ({ total: 189691, generatedAt: '2026-10-24T12:00:00.000Z' }),
  refreshDownloadTotal: () => Promise.resolve({
    total: 189691,
    generatedAt: '2026-10-24T12:00:00.000Z',
  }),
}))

const { default: Index } = await import('@/pages/Index.vue')

// The flashcards resolve their images with webpack's `require()` inside the template. Vitest injects
// a real CJS require that resolves through Node rather than through Vite's aliases, so rendering
// those slots throws "Cannot find module". Stubbing the two components without a <slot /> keeps the
// call sites unevaluated; the cards under test here are plain q-cards elsewhere on the page. The
// Vite migration replaces `require()` with import.meta.glob and these stubs become unnecessary.
const flashcardStubs = {
  QFlashcard: { template: '<div />' },
  QFlashcardSection: { template: '<div />' },
}

describe('Index — the download chip', () => {
  // Asserts the RENDERED text, not the value handed to the component: a chip that printed the raw
  // number, or 'undefined', or a spinner that never stops, would all satisfy a prop-level check.
  it('shows the download total, thousands-separated, with no spinner', () => {
    const wrapper = mount(Index, { global: { stubs: flashcardStubs } })

    const chip = wrapper.findAll('.q-chip').find((c) => c.text().includes('Downloads'))
    expect(chip).toBeDefined()
    expect(chip.text()).toContain(new Intl.NumberFormat().format(189691))
    expect(chip.text()).not.toContain('undefined')
    expect(chip.text()).not.toContain('NaN')
    // The old chip conflated loading, failure and a genuine zero into one permanent spinner.
    expect(chip.find('.q-circular-progress').exists()).toBe(false)
  })

  it('no longer claims the figure is GitHub-only', () => {
    const wrapper = mount(Index, { global: { stubs: flashcardStubs } })

    const chip = wrapper.findAll('.q-chip').find((c) => c.text().includes('Downloads'))
    expect(chip.text()).not.toContain('GitHub')
  })
})

describe('Index — the Features Excerpt cards', () => {
  // `hoverOutTimeout` clears the animation a second after the pointer leaves, so the clock has to
  // be driven for a card to finish animating before the next one is hovered.
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  // Each card animates its OWN heading. Asserting per-card is the whole point: a single shared
  // animation target looks identical to a working one if you only ever hover one card.
  it('animates the hovered card and only the hovered card', async () => {
    const wrapper = mount(Index, { global: { stubs: flashcardStubs } })

    const cards = wrapper.findAll('.my-other-card')
    expect(cards.length).toBe(4)

    for (const [position, card] of cards.entries()) {
      await card.trigger('mouseover')

      const animated = wrapper.findAll('.bounceIn')
      expect(
        animated.length,
        `hovering card ${position + 1} should animate exactly one heading`,
      ).toBe(1)
      expect(
        card.element.contains(animated[0].element),
        `hovering card ${position + 1} should animate a heading inside that card`,
      ).toBe(true)

      await card.trigger('mouseout')
      vi.advanceTimersByTime(1000)
      await wrapper.vm.$nextTick()
      expect(
        wrapper.findAll('.bounceIn').length,
        `card ${position + 1} should stop animating a second after the pointer leaves`,
      ).toBe(0)
    }
  })
})
