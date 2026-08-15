import org.siouan.frontendgradleplugin.infrastructure.gradle.RunNpmTaskType

plugins {
    id("org.siouan.frontend-jdk21")
}


frontend {
    packageJsonDirectory.set(project.layout.projectDirectory.asFile)

    nodeVersion.set("24.18.1")
    nodeInstallDirectory.set(project.layout.projectDirectory.dir("node"))

    assembleScript.set("run build")

    // Without this the plugin SKIPs checkFrontend entirely, so `./gradlew build` compiled and bundled the
    // SPA while never running its test suite — green builds that had not executed a single frontend test.
    // Maps to `npm run test` -> `vitest run` (package.json).
    checkScript.set("run test")

    verboseModeEnabled.set(true)
}

tasks.register("installQuasar", RunNpmTaskType::class) {
    args.set("install -g @quasar/cli")
}

tasks.getByName("installNode").finalizedBy(
    tasks.getByName("installQuasar")
)

