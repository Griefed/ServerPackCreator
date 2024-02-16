import { RouteRecordRaw } from 'vue-router';
import ModPackDownload from 'pages/ModPackDownload.vue';
import ServerPackDownload from 'pages/ServerPackDownload.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      { path: 'submissions', component: () => import('pages/SubmissionPage.vue') },
      { path: 'downloads', component: () => import('pages/DownloadsPage.vue') },
      { path: 'history', component: () => import('pages/HistoryPage.vue') },
      { path: 'about', component: () => import('pages/AboutPage.vue') },
      { path: 'download/modpack/:id', component: ModPackDownload },
      { path: 'download/serverpack/:id', component: ServerPackDownload }
    ],
  },
  // {
  //   path: '/download',
  //   component: () => import('layouts/MainLayout.vue'),
  //   children: [
  //     { path: 'modpack/:id', component: ModPackDownload },
  //     { path: 'serverpack/:id', component: ServerPackDownload }
  //   ]
  // },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorPage.vue'),
  },
];

export default routes;
