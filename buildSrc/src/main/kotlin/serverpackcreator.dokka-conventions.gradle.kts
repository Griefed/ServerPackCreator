import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
}


dokka {
    moduleName = "ServerPackCreator"
    dokkaPublications.html {
        suppressInheritedMembers.set(true)
        failOnWarning.set(false)
        outputDirectory.set(layout.buildDirectory.asFile.get().resolve("dokka"))
        includes.from("README.md")
    }

    dokkaSourceSets {
        configureEach {
            //includes.from("README.md")
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl("https://git.griefed.de/Griefed/ServerPackCreator")
                remoteLineSuffix.set("#L")
            }
            documentedVisibilities.set(
                setOf(
                    VisibilityModifier.Public,
                    VisibilityModifier.Protected,
                    VisibilityModifier.Package
                )
            )
            skipDeprecated.set(false)
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            jdkVersion.set(21)
            // Generated sources (e.g. the i18n4k `Translations` object) carry no hand-written
            // KDoc, so documenting them is neither possible nor meaningful — exclude them to keep
            // the docs (and the reportUndocumented output) focused on first-party code.
            // `suppressGeneratedFiles` alone does not catch the i18n4k output under
            // `build/generated`, so we additionally suppress that directory by path.
            suppressGeneratedFiles.set(true)
            suppressedFiles.from(layout.buildDirectory.dir("generated"))
            includes.from(
                projectDir.resolve("module.md")
            )
        }
    }
}

// BOTH publications read `build/generated` — `suppressedFiles` above points at it — so both must
// declare the Java compilations that also write there, or Gradle fails the build with
// "uses this output of task ':…:compileJava' without declaring an explicit or implicit dependency".
// Only the Javadoc half was declared until 2026-08-16; the HTML half had the identical need and was
// missing it. The gap stayed invisible because `build` runs only the Javadoc publication (via
// `finalizedBy` in -api), so nothing in the normal loop ever put HTML in a graph with the compile
// tasks. Keep the two in step — fixing one and not the other is exactly how this arose.
listOf(tasks.dokkaGeneratePublicationJavadoc, tasks.dokkaGeneratePublicationHtml).forEach { publication ->
    publication.configure {
        dependsOn(tasks.named("compileJava"), tasks.named("compileTestJava"))
    }
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaGeneratePublicationJavadoc)
    archiveClassifier.set("javadoc")
    // The two publications both write an `index.html`, so the jar has a name collision whenever BOTH
    // output directories are populated -- and only then, which is why it has never failed in CI: the
    // task depends on the Javadoc publication alone, and `build/dokka` is empty in a fresh checkout.
    // Locally it is not: `:serverpackcreator-api:dokkaGenerateHtml` (which the release's `assets` job
    // runs, in a different job on a different runner) or any earlier `dokkaGeneratePublicationHtml`
    // leaves it populated, after which this task dies with "Entry index.html is a duplicate but no
    // duplicate handling strategy has been set". EXCLUDE rather than INCLUDE: the Javadoc tree is added
    // first, so its `index.html` wins and the jar keeps the entry point a `-javadoc.jar` is expected to
    // have, instead of carrying two entries under one name.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    from(dokka.dokkaPublications.html.flatMap { it.outputDirectory })
}