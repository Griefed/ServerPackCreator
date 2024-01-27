import { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      { path: 'submissions', component: () => import('pages/SubmissionPage.vue') },
      { path: 'downloads', component: () => import('pages/DownloadsPage.vue') },
      { path: 'history', component: () => import('pages/HistoryPage.vue') },
      { path: 'about', component: () => import('pages/AboutPage.vue') }
    ],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorPage.vue'),
  },
];

export default routes;
