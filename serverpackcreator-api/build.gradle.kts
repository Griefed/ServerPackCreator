import java.util.prefs.Preferences

plugins {
    id("serverpackcreator.kotlin-multiplatform-conventions")
    id("serverpackcreator.dokka-conventions")
    id("de.comahe.i18n4k") version "0.7.0"
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.github.microutils:kotlin-logging:3.0.5")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                api("de.jensklingenberg.ktorfit:ktorfit-lib:1.9.0")
                api("de.comahe.i18n4k:i18n4k-core:0.6.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-bom")
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
                implementation(files("${layout.buildDirectory.asFile.get()}/resources/"))
                api("de.comahe.i18n4k:i18n4k-core-jvm:0.6.2")
                api("com.electronwill.night-config:toml:3.6.7")
                api("com.fasterxml.jackson.core:jackson-databind:2.15.3")
                api("net.lingala.zip4j:zip4j:2.11.5")
                api("org.apache.logging.log4j:log4j-api-kotlin:1.3.0")
                api("org.apache.logging.log4j:log4j-core:2.21.0")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                api("org.pf4j:pf4j:3.10.0")
                api("org.bouncycastle:bcpkix-jdk18on:1.77")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlin:kotlin-test:1.9.10")
                implementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
                implementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
            }
        }
// Uncomment if you wish to start developing the JS component
//        val jsMain by getting {
//            dependsOn(commonMain)
//            dependencies {
//                api(kotlin("stdlib-js"))
//                api("io.github.microutils:kotlin-logging-js:3.0.4")
//                api("de.comahe.i18n4k:i18n4k-core-js:0.5.0")
//            }
//        }
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
    }
}

tasks.signJvmPublication {
    dependsOn(tasks.dokkaJavadocJar)
}

tasks.signKotlinMultiplatformPublication {
    dependsOn(tasks.dokkaJavadocJar)
}

tasks.jvmSourcesJar {
    dependsOn(tasks.generateI18n4kFiles)
}

tasks.jvmProcessResources {
    dependsOn(tasks.generateI18n4kFiles)
}

//Fix resources missing in multiplatform jvm inDev run https://youtrack.jetbrains.com/issue/KTIJ-16582/Consumer-Kotlin-JVM-library-cannot-access-a-Kotlin-Multiplatform-JVM-target-resources-in-multi-module-Gradle-project
tasks.register<Copy>("fixMissingResources") {
    dependsOn(tasks.jvmProcessResources)
    from("${layout.buildDirectory.asFile.get()}/processedResources/jvm/main")
    into("${layout.buildDirectory.asFile.get()}/resources/")
}

tasks.dokkaHtml {
    dependsOn(tasks.generateI18n4kFiles, tasks.getByName("fixMissingResources"))
}

tasks.jvmJar {
    dependsOn(tasks.getByName("fixMissingResources"))
}

tasks.register<Copy>("updateManifests") {
    dependsOn(tasks.test)
    from(rootDir.resolve("serverpackcreator-app/tests/manifests"))
    into(projectDir.resolve("src/jvmMain/resources/de/griefed/resources/manifests"))
}

tasks.jvmTest {
    dependsOn(tasks.getByName("fixMissingResources"))
    Preferences.userRoot().node("ServerPackCreator").clear()
    Preferences.userRoot().node("ServerPackCreator").sync()
}

tasks.build {
    doLast {
        tasks.dokkaJavadocJar
    }
}

tasks.withType(PublishToMavenRepository::class) {
    dependsOn(tasks.signKotlinMultiplatformPublication,tasks.signJvmPublication)
}
