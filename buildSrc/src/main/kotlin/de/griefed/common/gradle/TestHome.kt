package de.griefed.common.gradle

import java.io.File

/**
 * The per-module test home ServerPackCreator's suites run against — `<module>/tests`.
 *
 * Lives in `buildSrc`'s Kotlin sources rather than in the convention script on purpose. A task action
 * that calls a function declared *in* a build script closes over the script object, which the
 * configuration cache cannot serialize; calling into a compiled class here closes over nothing.
 */
object TestHome {

    /**
     * Empty [testHome] for a fresh run and make sure it exists with its `.gitkeep`, sparing the version
     * manifests.
     *
     * Everything in the test home is disposable **except** `manifests/`. Those are a cache of immutable
     * upstream data — SPC seeds them from the jar and fetches a per-version `mcserver/<version>.json` on
     * demand — so deleting them makes every run re-download, which contradicts the API module's
     * documented "no live network needed" and discards any version fetched since the last seed.
     * `updateManifests` benefits too: it copies this directory into the shipped resources, so preserving
     * it lets the snapshot accumulate releases instead of being capped at the seeded set.
     *
     * Deliberately does **not** touch the Preferences store. Removing the shared, machine-wide
     * `ServerPackCreator` node would relocate the home of the developer's own GUI — and of any running
     * grinder daemon — into the repository on every `test` or `clean`. Test isolation comes from the
     * per-module Preferences node and the injected `-Dde.griefed.serverpackcreator.home` instead.
     *
     * @param testHome The module's `tests` directory.
     */
    fun prepare(testHome: File) {
        val tests = testHome.absoluteFile
        tests.mkdirs()
        val gitkeep = File(tests, ".gitkeep")
        if (!gitkeep.exists()) {
            gitkeep.writeText("Hi")
        }
        tests.listFiles()
            ?.filter { !it.name.endsWith("gitkeep") && it.name != "manifests" }
            ?.forEach { it.deleteRecursively() }
    }
}
