
plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.publishing-conventions")
    id("serverpackcreator.dokka-conventions")
    id("de.comahe.i18n4k") version "0.11.2"
}


dependencies {
    api(libs.kotlinLogging)
    api(libs.kotlinxDatetime)
    implementation(libs.kotlinBom)
    implementation(libs.kotlinStdlib)
    api(libs.ktorfit)
    api(libs.i18n4kCore)
    api(libs.i18n4kCoreJvm)
    implementation(files("${layout.buildDirectory.asFile.get()}/resources/main"))
    api(libs.nightConfigToml)
    api(libs.jacksonDatabind)
    api(libs.zip4j)
    api(libs.log4jApiKotlin)
    api(libs.log4jCore)
    api(libs.kotlinxCoroutinesCore)
    api(libs.pf4j)
    api(libs.bouncycastle)

    api(libs.mslinks)
    api("com.github.MCRcortex:nekodetector:Version-1.1-pre")
    //api("dev.kosmx.needle:jneedle:1.0.1")

    testImplementation(libs.kotlinTestJunit5)
    // MockK lets the unit tests stub network-bound collaborators (WebUtilities, VersionMeta) so
    // provisioner/manifest branches can be exercised offline. Version pinned to the same 1.14.6 the
    // app module already resolves transitively via springmockk, keeping the build's mockk single-versioned.
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.sourcesJar {
    dependsOn(tasks.generateI18n4kFiles)
}

tasks.processResources {
    dependsOn(tasks.generateI18n4kFiles)
    //API
    copy {
        from(rootProject.layout.projectDirectory.file("CHANGELOG.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("CODE_OF_CONDUCT.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("CONTRIBUTING.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("HELP.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("LICENSE"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("README.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("SECURITY.md"))
        into(layout.projectDirectory.dir("src/main/resources"))
    }

    // Writerside
    copy {
        from(rootProject.layout.projectDirectory.file("CHANGELOG.md"))
        into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("CODE_OF_CONDUCT.md"))
        into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("CONTRIBUTING.md"))
        into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("HELP.md"))
        into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("LICENSE"))
        into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics"))
        rename("LICENSE","LICENSE.md")
    }
    copy {
        from(rootProject.layout.projectDirectory.file("README.md"))
        into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics"))
    }
    copy {
        from(rootProject.layout.projectDirectory.file("SECURITY.md"))
        into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics"))
    }
    copy {
        from(rootProject.layout.projectDirectory.dir("img"))
        into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics/img"))
    }
}

//Fix resources missing in multiplatform jvm inDev run https://youtrack.jetbrains.com/issue/KTIJ-16582/Consumer-Kotlin-JVM-library-cannot-access-a-Kotlin-Multiplatform-JVM-target-resources-in-multi-module-Gradle-project
tasks.register<Copy>("fixMissingResources") {
    dependsOn(tasks.processResources)
    from("${layout.buildDirectory.asFile.get()}/processedResources/jvm/main")
    into("${layout.buildDirectory.asFile.get()}/resources/")
}

tasks.dokkaGeneratePublicationHtml {
    dependsOn(tasks.generateI18n4kFiles, tasks.getByName("fixMissingResources"))
}

tasks.dokkaGeneratePublicationJavadoc {
    dependsOn(tasks.generateI18n4kFiles, tasks.getByName("fixMissingResources"), tasks.processResources)
}

tasks.jar {
    dependsOn(tasks.getByName("fixMissingResources"))
}

// Refreshes the shipped manifest snapshot from a test home that has just been populated. Sources *this* module's
// test home, not the app's: the api suite is what exercises `MinecraftMeta`, so it is the one that fetches the
// per-version `mcserver/<id>.json` files -- pointing at `serverpackcreator-app/tests` made this a no-op for them
// (measured: app 643 files, api 659, the difference being exactly the 16 releases that were missing from the
// shipped set). Since `cleanup()` stopped wiping `manifests/`, that home accumulates rather than resetting each
// run, so a plain `test` followed by this task genuinely advances the snapshot.
// Pinned by `ShippedManifestSnapshotTest`, which fails when the shipped set falls behind its own parent manifest.
tasks.register<Copy>("updateManifests") {
    dependsOn(tasks.test)
    from(projectDir.resolve("tests/manifests"))
    into(projectDir.resolve("src/main/resources/de/griefed/resources/manifests"))
}

tasks.test {
    dependsOn(tasks.getByName("fixMissingResources"))
    // `ShippedResourceTrackingTest` asserts on the repository's ignore rules, which are not otherwise an input to
    // anything -- without this the task reports UP-TO-DATE after a .gitignore change and the guard silently does
    // not run, which is exactly how its own first teeth-check appeared to pass.
    inputs.file(rootProject.file(".gitignore")).withPropertyName("rootGitignore")
}

tasks.build {
    finalizedBy(tasks.dokkaGeneratePublicationJavadoc)
}

tasks.generatePomFileForMavenJavaPublication {
    dependsOn(
        tasks.getByName("fixMissingResources"),
        tasks.processResources)
}

tasks.signMavenJavaPublication {
    dependsOn(tasks.dokkaJavadocJar)
}
