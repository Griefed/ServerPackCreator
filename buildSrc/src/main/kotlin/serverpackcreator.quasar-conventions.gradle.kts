import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.version
import org.siouan.frontendgradleplugin.infrastructure.gradle.RunNpm
import java.io.File

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
    //yarnVersion = "1.22.11"
    //yarnInstallDirectory = file("${projectDir}/frontend/yarn")

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

tasks.register("copyDist", Copy::class) {
    val serverPackCreator = projectDir.absoluteFile.parentFile
    val spa = File(File(projectDir.absoluteFile,"dist"),"spa").absoluteFile
    from(spa)
    into(serverPackCreator.path + File.separator + "serverpackcreator-app" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static")
}

tasks.getByName("build").finalizedBy(
    tasks.getByName("copyDist")
)