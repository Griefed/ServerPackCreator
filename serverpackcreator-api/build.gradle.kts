
plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
    id("de.comahe.i18n4k") version "0.11.2"
}


dependencies {
    api("io.github.microutils:kotlin-logging:3.0.5")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")
    implementation("org.jetbrains.kotlin:kotlin-bom:2.3.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    api("de.jensklingenberg.ktorfit:ktorfit-lib:2.7.3")
    api("de.comahe.i18n4k:i18n4k-core:0.11.2")
    api("de.comahe.i18n4k:i18n4k-core-jvm:0.11.2")
    implementation(files("${layout.buildDirectory.asFile.get()}/resources/main"))
    api("com.electronwill.night-config:toml:3.8.4")
    api("com.fasterxml.jackson.core:jackson-databind:2.21.1")
    api("net.lingala.zip4j:zip4j:2.11.6")
    api("org.apache.logging.log4j:log4j-api-kotlin:1.5.0")
    api("org.apache.logging.log4j:log4j-core:2.26.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    api("org.pf4j:pf4j:3.15.0")
    api("org.bouncycastle:bcpkix-jdk18on:1.84")

    api("org.jabref:mslinks:1.2")
    api("com.github.MCRcortex:nekodetector:Version-1.1-pre")
    //api("dev.kosmx.needle:jneedle:1.0.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.21")
    // MockK lets the unit tests stub network-bound collaborators (WebUtilities, VersionMeta) so
    // provisioner/manifest branches can be exercised offline. Version pinned to the same 1.14.6 the
    // app module already resolves transitively via springmockk, keeping the build's mockk single-versioned.
    testImplementation("io.mockk:mockk:1.14.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
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
    doLast {
        tasks.dokkaGeneratePublicationJavadoc
    }
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
