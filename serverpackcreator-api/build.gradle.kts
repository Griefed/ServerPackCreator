
import java.util.prefs.Preferences

plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
    id("de.comahe.i18n4k") version "0.7.0"
}

repositories {
    mavenCentral()
}

dependencies {
    api("io.github.microutils:kotlin-logging:3.0.5")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    api("de.jensklingenberg.ktorfit:ktorfit-lib:1.12.0")
    api("de.comahe.i18n4k:i18n4k-core:0.7.0")
    implementation("org.jetbrains.kotlin:kotlin-bom:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation(files("${layout.buildDirectory.asFile.get()}/resources/"))
    api("de.comahe.i18n4k:i18n4k-core-jvm:0.7.0")
    api("com.electronwill.night-config:toml:3.6.7")
    api("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    api("net.lingala.zip4j:zip4j:2.11.5")
    api("org.apache.logging.log4j:log4j-api-kotlin:1.4.0")
    api("org.apache.logging.log4j:log4j-core:2.22.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("org.pf4j:pf4j:3.10.0")
    api("org.bouncycastle:bcpkix-jdk18on:1.77")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks.sourcesJar {
    dependsOn(tasks.generateI18n4kFiles)
}

tasks.processResources {

}

tasks.processResources {
    dependsOn(tasks.generateI18n4kFiles)
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
}

//Fix resources missing in multiplatform jvm inDev run https://youtrack.jetbrains.com/issue/KTIJ-16582/Consumer-Kotlin-JVM-library-cannot-access-a-Kotlin-Multiplatform-JVM-target-resources-in-multi-module-Gradle-project
tasks.register<Copy>("fixMissingResources") {
    dependsOn(tasks.processResources)
    from("${layout.buildDirectory.asFile.get()}/processedResources/jvm/main")
    into("${layout.buildDirectory.asFile.get()}/resources/")
}

tasks.dokkaHtml {
    dependsOn(tasks.generateI18n4kFiles, tasks.getByName("fixMissingResources"))
}

tasks.jar {
    dependsOn(tasks.getByName("fixMissingResources"))
}

tasks.register<Copy>("updateManifests") {
    dependsOn(tasks.test)
    from(rootDir.resolve("serverpackcreator-app/tests/manifests"))
    into(projectDir.resolve("src/main/resources/de/griefed/resources/manifests"))
}

tasks.test {
    dependsOn(tasks.getByName("fixMissingResources"))
    Preferences.userRoot().node("ServerPackCreator").clear()
    Preferences.userRoot().node("ServerPackCreator").sync()
}

tasks.build {
    doLast {
        tasks.dokkaJavadocJar
    }
}