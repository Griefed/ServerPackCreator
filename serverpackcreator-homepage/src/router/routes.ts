import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('@/pages/Index.vue') },
      // The plugin listing, formerly its own Quasar project at
      // griefed.github.io/ServerPackCreator-Addons-Overview. A child of '/' so it shares the one
      // MainLayout instance rather than mounting a second copy of the header and footer.
      { path: 'addons', component: () => import('@/pages/AddonIndex.vue') }
    ]
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('@/pages/Error404.vue') }
    ]
  }
]

export default routes
