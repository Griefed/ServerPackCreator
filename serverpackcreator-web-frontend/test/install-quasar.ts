import { config } from '@vue/test-utils'
import { Quasar } from 'quasar'

// Component-test harness: install Quasar globally for Vue Test Utils so SFC tests can mount
// components that rely on Quasar's components/directives and the `$q` instance, without booting
// the full app. Loaded via vitest.config.js `setupFiles`. Pass plugins (e.g. Notify) per-test
// through `mount(..., { global: { plugins: [[Quasar, { plugins: { Notify } }]] } })` when needed.
config.global.plugins = [...(config.global.plugins ?? []), [Quasar, {}]]
