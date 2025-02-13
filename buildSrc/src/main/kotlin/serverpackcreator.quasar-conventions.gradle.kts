import org.siouan.frontendgradleplugin.infrastructure.gradle.RunNpmTaskType

plugins {
    id("org.siouan.frontend-jdk21")
}

repositories {
    mavenCentral()
}

frontend {
    packageJsonDirectory.set(project.layout.projectDirectory.asFile)

    nodeVersion.set("20.18.3")
    nodeInstallDirectory.set(project.layout.projectDirectory.dir("node"))

    assembleScript.set("run build")

    // Print the architecture we are running on.
    println(String.format("I am running on: %s", System.getProperty("os.arch")))

    verboseModeEnabled.set(true)
}

tasks.register("installQuasar", RunNpmTaskType::class) {
    dependsOn("installCorepackLatest")
    args.set("install -g @quasar/cli")
}

//Temporary intermediate task to prevent https://github.com/nodejs/corepack/issues/612#issuecomment-2631491212
//TODO Remove once the error, which caused this task to exist in the first place, is fixed in NodeJS/Corepack
tasks.register("installCorepackLatest", RunNpmTaskType::class) {
    args.set("install --global corepack@latest")
}

tasks.getByName("installNode").finalizedBy(
    tasks.getByName("installQuasar")
)

