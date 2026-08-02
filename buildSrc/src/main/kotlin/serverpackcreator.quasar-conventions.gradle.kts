import org.siouan.frontendgradleplugin.infrastructure.gradle.RunNpmTaskType

plugins {
    id("org.siouan.frontend-jdk21")
}

repositories {
    mavenCentral()
}

frontend {
    packageJsonDirectory.set(project.layout.projectDirectory.asFile)

    nodeVersion.set("24.18.1")
    nodeInstallDirectory.set(project.layout.projectDirectory.dir("node"))

    assembleScript.set("run build")

    // Print the architecture we are running on.
    println(String.format("I am running on: %s", System.getProperty("os.arch")))

    verboseModeEnabled.set(true)
}

tasks.register("installQuasar", RunNpmTaskType::class) {
    args.set("install -g @quasar/cli")
}

tasks.getByName("installNode").finalizedBy(
    tasks.getByName("installQuasar")
)

