plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
}


dependencies {
    // The domain core: VersionMeta, ModScanner, ServerPackHandler, PackConfig, utilities.
    api(project(":serverpackcreator-api"))

    // The API ships jackson-databind, but not the Kotlin module the report-renderer needs for
    // jacksonObjectMapper(); log4j-api-kotlin comes transitively from the API.
    api(libs.jacksonModuleKotlin)

    // Headless-browser download of distribution-locked CurseForge files (locked = no direct URL).

    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.test {
    // MetadataScannerTest boots an offline ApiWrapper whose ModScanner relies on the API's cached
    // version-manifests, exactly as the API's own ModScannerTest does.
    dependsOn(":serverpackcreator-api:processTestResources")

    // `ClientsideReportRendererTest` reads the GitHub workflows' script, which lives outside this project and
    // is therefore not an input Gradle knows about: without this, editing the script leaves the task
    // UP-TO-DATE, or restores it from the build cache, and the marker guard never runs on that edit.
    inputs.file(file("../.github/scripts/clientside-verification.js"))
        .withPropertyName("clientsideWorkflowScript")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
