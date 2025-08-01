
import java.util.prefs.Preferences

plugins {
    id("serverpackcreator.kotlin-conventions")
    id("serverpackcreator.dokka-conventions")
    id("de.comahe.i18n4k") version "0.10.0"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api("io.github.microutils:kotlin-logging:3.0.5")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    api("de.jensklingenberg.ktorfit:ktorfit-lib:2.5.1")
    api("de.comahe.i18n4k:i18n4k-core:0.10.0")
    api("de.comahe.i18n4k:i18n4k-core-jvm:0.10.0")
    implementation("org.jetbrains.kotlin:kotlin-bom:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
    implementation(files("${layout.buildDirectory.asFile.get()}/resources/main"))
    api("com.electronwill.night-config:toml:3.8.1")
    api("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    api("net.lingala.zip4j:zip4j:2.11.5")
    api("org.apache.logging.log4j:log4j-api-kotlin:1.5.0")
    api("org.apache.logging.log4j:log4j-core:2.25.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    api("org.pf4j:pf4j:3.13.0")
    api("org.bouncycastle:bcpkix-jdk18on:1.81")

    api("com.github.MCRcortex:nekodetector:Version-1.1-pre")
    //api("dev.kosmx.needle:jneedle:1.0.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
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

tasks.dokkaHtml {
    dependsOn(tasks.generateI18n4kFiles, tasks.getByName("fixMissingResources"))
}

tasks.dokkaJavadoc {
    dependsOn(tasks.getByName("fixMissingResources"))
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
    finalizedBy(tasks.dokkaJavadocJar)
}

tasks.signMavenJavaPublication {
    dependsOn(tasks.dokkaJavadocJar)
}