
plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.publishing-conventions")
    id("serverpackcreator.dokka-conventions")
    alias(libs.plugins.i18n4k)
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
    api(libs.nekodetector)
    //api("dev.kosmx.needle:jneedle:1.0.1")

    testImplementation(libs.kotlinTestJunit5)
    // MockK lets the unit tests stub network-bound collaborators (WebUtilities, VersionMeta) so
    // provisioner/manifest branches can be exercised offline. Single-versioned across the build via the
    // catalog's `mockk` — springmockk drags in an older one transitively, so `-app` declares this same
    // dependency explicitly to out-rank it. Bumping `mockk` without that would silently split the two.
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.sourcesJar {
    // shipRootDocuments writes into src/main/resources, which this task packages. Gradle can only see
    // that coupling now that the copies are real tasks; as configuration-time copy{} calls the ordering
    // was pure luck.
    dependsOn(tasks.generateI18n4kFiles, tasks.named("shipRootDocuments"))
}

tasks.processResources {
    dependsOn(tasks.generateI18n4kFiles, tasks.named("shipRootDocuments"))
}

// The root-level documents SPC ships inside its own jar and reads back at runtime (ApiWrapper.setup()
// writes them into the user's home), plus the copies Writerside builds the help site from.
//
// These used to be fifteen bare `copy { }` calls inside the `processResources` CONFIGURATION block, so
// they ran whenever that task was configured -- including on runs where processResources itself was
// UP-TO-DATE and did nothing. Measured before this change: a second, fully up-to-date
// `:serverpackcreator-api:processResources` still rewrote both destinations. As real Copy tasks they
// have declared inputs and outputs, so they are up-to-date checked, cacheable, and do not write into
// two source trees on every build that happens to touch this project.
val shippedDocuments = listOf(
    "CHANGELOG.md", "CODE_OF_CONDUCT.md", "CONTRIBUTING.md", "HELP.md", "LICENSE", "README.md", "SECURITY.md"
)

tasks.register<Copy>("shipRootDocuments") {
    description = "Copies the root-level documents SPC ships in its jar into this module's resources."
    from(rootProject.layout.projectDirectory) {
        include(shippedDocuments)
    }
    into(layout.projectDirectory.dir("src/main/resources"))
}

tasks.register<Copy>("shipWritersideDocuments") {
    description = "Mirrors the root-level documents and images into the Writerside help sources."
    // LICENSE has no extension; Writerside needs it as Markdown to render it as a topic.
    from(rootProject.layout.projectDirectory) {
        include(shippedDocuments)
        rename("LICENSE", "LICENSE.md")
    }
    into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics"))
}

tasks.register<Copy>("shipWritersideImages") {
    description = "Mirrors the root img directory into the Writerside help sources."
    from(rootProject.layout.projectDirectory.dir("img"))
    into(rootProject.layout.projectDirectory.dir("serverpackcreator-help/Writerside/topics/img"))
}

// The help sources are a documentation artifact, not an input to any jar, so they refresh with the
// build rather than blocking resource processing on them.
tasks.named("build") {
    dependsOn(tasks.named("shipWritersideDocuments"), tasks.named("shipWritersideImages"))
}

//Fix resources missing in multiplatform jvm inDev run https://youtrack.jetbrains.com/issue/KTIJ-16582/Consumer-Kotlin-JVM-library-cannot-access-a-Kotlin-Multiplatform-JVM-target-resources-in-multi-module-Gradle-project
tasks.register<Copy>("fixMissingResources") {
    dependsOn(tasks.processResources)
    from("${layout.buildDirectory.asFile.get()}/processedResources/jvm/main")
    into("${layout.buildDirectory.asFile.get()}/resources/")
}

tasks.dokkaGeneratePublicationHtml {
    dependsOn(tasks.generateI18n4kFiles, tasks.named("fixMissingResources"))
}

tasks.dokkaGeneratePublicationJavadoc {
    dependsOn(tasks.generateI18n4kFiles, tasks.named("fixMissingResources"), tasks.processResources)
}

tasks.jar {
    dependsOn(tasks.named("fixMissingResources"))
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
    dependsOn(tasks.named("fixMissingResources"))
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
        tasks.named("fixMissingResources"),
        tasks.processResources)
}

tasks.signMavenJavaPublication {
    dependsOn(tasks.dokkaJavadocJar)
}
