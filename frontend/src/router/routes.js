
const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/Configuration.vue') },
      { path: '/configuration', component: () => import('pages/Configuration.vue') },
      { path: '/downloads', component: () => import('pages/Downloads.vue') },
      { path: '/logs', component: () => import('pages/Logs.vue') },
      { path: '/about', component: () => import('pages/About.vue') }
    ]
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/Error404.vue') }
    ]
  }
]

export default routes
