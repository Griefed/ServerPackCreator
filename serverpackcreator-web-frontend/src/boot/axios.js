import { defineBoot } from '#q-app/wrappers'
import axios from 'axios'

// Be careful when using SSR for cross-request state pollution
// due to creating a Singleton instance here;
// If any client changes this (global) instance, it might be a
// good idea to move this instance creation inside the
// "export default () => {}" function below (which runs individually
// for each client)
const baseUrl = '/api/v2/';

const versions = axios.create({
  baseURL: baseUrl + 'versions',
  method: 'get'
});
const settings = axios.create({
  baseURL: baseUrl + 'settings/',
  method: 'get'
});
const events = axios.create({
  baseURL: baseUrl + 'events/',
  method: 'get'
});
const serverpacks = axios.create({
  baseURL: baseUrl + 'serverpacks/',
  method: 'get'
});
const modpacks = axios.create({
  baseURL: baseUrl + 'modpacks/'
});
const runConfigs = axios.create({
  baseURL: baseUrl + 'runconfigs/',
  method: 'get'
})
const stats = axios.create({
  baseURL: baseUrl + '/stats',
  method: 'get'
})

export default defineBoot(({ app }) => {
  // for use inside Vue files (Options API) through this.$axios and this.$api

  app.config.globalProperties.$axios = axios;
  // ^ ^ ^ this will allow you to use this.$axios (for Vue Options API form)
  //       so you won't necessarily have to import axios in each vue file

  app.config.globalProperties.$versions = versions;
  app.config.globalProperties.$settings = settings;
  app.config.globalProperties.$events = events;
  app.config.globalProperties.$serverpacks = serverpacks;
  app.config.globalProperties.$modpacks = modpacks;
  app.config.globalProperties.$runConfigs = runConfigs;
  app.config.globalProperties.$stats = stats;
})

export { versions, settings, events, serverpacks, modpacks, runConfigs, stats };
