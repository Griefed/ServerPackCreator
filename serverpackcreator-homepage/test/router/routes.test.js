import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import routes from '@/router/routes'
import MainLayout from '@/layouts/MainLayout.vue'
import Index from '@/pages/Index.vue'
import AddonIndex from '@/pages/AddonIndex.vue'
import Error404 from '@/pages/Error404.vue'

/** The components a path renders, outermost first, with every lazy import resolved. */
async function rendered(path) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  const matched = router.resolve(path).matched
  return Promise.all(matched.map(async (record) => {
    const component = record.components.default
    return typeof component === 'function' ? (await component()).default : component
  }))
}

describe('the route table', () => {
  it.each([
    ['/', Index],
    ['/addons', AddonIndex],
    ['/nonsense', Error404],
    ['/addons/nested/nonsense', Error404]
  ])('renders %s inside the one MainLayout', async (path, page) => {
    expect(await rendered(path)).toEqual([MainLayout, page])
  })
})
