import { config } from '@vue/test-utils'
import { Quasar, Notify } from 'quasar'

// Component-test harness: install Quasar globally for Vue Test Utils so SFC tests can mount
// components that rely on Quasar's components/directives and the `$q` instance, without booting
// the full app. Loaded via vitest.config.js `setupFiles`. Notify is registered because several
// components call `this.$q.notify(...)`; tests typically `vi.spyOn(wrapper.vm.$q, 'notify')`.
config.global.plugins = [...(config.global.plugins ?? []), [Quasar, { plugins: { Notify } }]]
