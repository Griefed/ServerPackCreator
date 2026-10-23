import { config } from '@vue/test-utils'

// Component-test harness: install Quasar globally for Vue Test Utils so SFC tests can mount
// components that rely on Quasar's components/directives and the `$q` instance, without booting
// the full app. Loaded via vitest.config.js `setupFiles`. Notify is registered because several
// components call `this.$q.notify(...)`; tests typically `vi.spyOn(wrapper.vm.$q, 'notify')`.
//
// Skipped without a DOM: Quasar's client bundle touches `window` on import, so a test declaring
// `@vitest-environment node` (the build-config guards, which must not boot a browser) would die
// here on an import it has no use for.
if (typeof window !== 'undefined') {
  const { Quasar, Notify } = await import('quasar')
  config.global.plugins = [...(config.global.plugins ?? []), [Quasar, { plugins: { Notify } }]]
}
