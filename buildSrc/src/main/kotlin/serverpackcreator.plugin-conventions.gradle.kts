import org.gradle.api.tasks.bundling.Jar

// Everything that is true of EVERY ServerPackCreator plugin module and is a decision rather than data:
// how its jar is named, and how the root build gets hold of that jar. The per-plugin manifest stays in
// each module's own build script, because a plugin's id, class and description are data, not a shared
// choice.

// A consumable view of just this module's plugin jar, for the root build's copy tasks. Explicit rather
// than the legacy `archives` configuration, which Gradle 9 removes, and it carries the task dependency
// so the jar is built on demand. Lived in all four plugin modules verbatim until it moved here.
val pluginArtifact: Configuration = configurations.create("pluginArtifact") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(pluginArtifact.name, tasks.named<Jar>("jar"))
}

// `_experimental` is part of the name on purpose, and this is the ONE place that decides it -- the same
// arrangement `misc/build-appimage.sh` has for the AppImages, and for the same reason: a user picking a
// download should be able to see from the filename alone that these are newer and less travelled than
// the application they plug into. Nothing else hardcodes it; `release-build.yml` and `devbuild.yml` glob
// `*_experimental.jar` precisely so this stays a single decision.
//
// Computed eagerly rather than through a provider that reads `project`: this build runs with the
// configuration cache, which cannot serialize a captured Project. `version` comes from
// `gradle.properties` (`dev`) or `-Pversion=<release>`, so it is never blank and the name never
// degenerates.
val experimentalJarName = "${project.name}-${project.version}_experimental.jar"

tasks.named<Jar>("jar") {
    archiveFileName.set(experimentalJarName)
}
