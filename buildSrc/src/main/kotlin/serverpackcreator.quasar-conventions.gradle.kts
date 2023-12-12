import org.siouan.frontendgradleplugin.infrastructure.gradle.RunNpm

plugins {
    id("org.siouan.frontend-jdk11")
}

repositories {
    mavenCentral()
}

frontend {
    packageJsonDirectory.set(project.layout.projectDirectory.asFile)

    nodeVersion.set("16.9.1")
    nodeInstallDirectory.set(project.layout.projectDirectory.dir("node"))

    yarnEnabled.set(false)

    cleanScript.set("run clean")
    assembleScript.set("run build")

    // Print the architecture we are running on.
    println(String.format("I am running on: %s", System.getProperty("os.arch")))

    // If we are running on arm, specify Node path pattern so arm-builds succeed.
    if (System.getProperty("os.arch").equals("arm")) {
        nodeDistributionUrlPathPattern.set("vVERSION/node-vVERSION-linux-armv7l.TYPE")
    } else if (System.getProperty("os.arch").equals("aarch64")) {
        nodeDistributionUrlPathPattern.set("vVERSION/node-vVERSION-linux-arm64.TYPE")
    }
}

tasks.register("installQuasar", RunNpm::class) {
    script.set("install -g @quasar/cli")
}

tasks.getByName("installNode").finalizedBy(
    tasks.getByName("installQuasar")
)

