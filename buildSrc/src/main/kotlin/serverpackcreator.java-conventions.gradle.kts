@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.prefs.Preferences

repositories {
    mavenCentral()
}

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    idea
}

java {
    // Auto JDK setup
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
}

tasks.compileJava {
    // See: https://docs.oracle.com/en/java/javase/12/tools/javac.html
    @Suppress("SpellCheckingInspection")
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all", // Enables all recommended warnings.
        )
    )
    options.encoding = "UTF-8"
}

tasks.getByName("sourcesJar",Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.clean {
    doFirst {
        cleanup()
    }
    doLast {
        delete {
            fileTree(projectDir.resolve("src/main/resources/static")) {
                exclude(".gitkeep")
            }
        }
    }
}

tasks.test {
    doFirst {
        cleanup()
    }
}

fun cleanup() {
    val tests = File(projectDir,"tests").absoluteFile
    mkdir(tests.absolutePath)
    val gitkeep = File(tests,".gitkeep").absoluteFile
    if (!gitkeep.exists()) {
        File(tests,".gitkeep").writeText("Hi")
    }
    projectDir.resolve("tests")
        .listFiles()
        .filter { !it.name.endsWith("gitkeep") }
        .forEach {
            it.deleteRecursively()
        }
    Preferences.userRoot().node("ServerPackCreator").removeNode()
    Preferences.userRoot().node("ServerPackCreator").put(
        "de.griefed.serverpackcreator.home",
        projectDir.resolve("tests").absolutePath
    )
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes(
            mapOf(
                "Built-By" to System.getProperty("user.name"),
                "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date()),
                "Created-By" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${
                    System.getProperty("java.vm.version")
                })",
                "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${
                    System.getProperty("os.version")
                }",
                "Implementation-Vendor" to "Griefed",
                "Implementation-Version" to project.version,
                "Implementation-Title" to project.name
            )
        )
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Griefed/serverpackcreator")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "GitGriefed"
            url = uri("https://git.griefed.de/api/v4/projects/63/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Private-Token"
                value = System.getenv("GITLAB_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
        maven {
            name = "GitLab"
            url = uri("https://gitlab.com/api/v4/projects/32677538/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Private-Token"
                value = System.getenv("GITLABCOM_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }

    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(tasks["javadocJar"])
            pom {
                name.set("ServerPackCreator")
                description.set("ServerPackCreators API, to create server packs from Forge, Fabric, Quilt, LegacyFabric and NeoForge modpacks.")
                url.set("https://git.griefed.de/Griefed/ServerPackCreator")

                licenses {
                    license {
                        name.set("GNU Lesser General Public License v2.1")
                        url.set("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html")
                    }
                }

                developers {
                    developer {
                        id.set("griefed")
                        name.set("Griefed")
                        email.set("griefed@griefed.de")
                    }
                }

                scm {
                    connection.set("scm:git:git:git.griefed.de/Griefed/ServerPackCreator.git")
                    developerConnection.set("scm:git:ssh://git.griefed.de/Griefed/ServerPackCreator.git")
                    url.set("https://git.griefed.de/Griefed/ServerPackCreator")
                }
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingKey").toString()
    val signingPassword = findProperty("signingPassword").toString()
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}