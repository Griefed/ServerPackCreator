@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.text.SimpleDateFormat
import java.util.*

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

/**
 * Escape a filesystem path for a `.properties` value: backslashes and colons are separators there, so a Windows
 * path written verbatim would be read back mangled (`C:\dir` becomes `C` + a value starting at `dir`).
 */
private fun escapeForProperties(path: String): String = path.replace("\\", "\\\\").replace(":", "\\:")

// The suite boots an ApiWrapper from build/resources/test/serverpackcreator.properties in dozens of places, and two
// of its values are inherently per-machine: the JDK path SPC writes into generated packs, and the tomcat basedir.
// Committing resolved values means committing one developer's filesystem, so the committed file leaves them blank and
// the build fills them in on the way to build/resources/test. Pinned by `TestPropertiesTest`.
tasks.processTestResources {
    val moduleTestHome = layout.projectDirectory.dir("tests").asFile.absolutePath
    val testJavaExecutable = javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile.absolutePath
    // Declared as inputs so a changed toolchain or module path re-runs the copy instead of serving a stale one.
    inputs.property("spcTestJavaExecutable", testJavaExecutable)
    inputs.property("spcTestModuleHome", moduleTestHome)
    filesMatching("serverpackcreator.properties") {
        filter { line: String ->
            when {
                line.startsWith("de.griefed.serverpackcreator.java=") ->
                    "de.griefed.serverpackcreator.java=${escapeForProperties(testJavaExecutable)}"
                line.startsWith("server.tomcat.basedir=") ->
                    "server.tomcat.basedir=${escapeForProperties(moduleTestHome)}"
                else -> line
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // Keep test runs off the shared Preferences node. SPC's home directory lives in a per-user, machine-wide node
    // that PathsConfig re-reads on every access and writes back to, so a suite booting an ApiWrapper would relocate
    // the home of every other SPC process on the account — it moved a live grinder daemon's home into a test
    // scratch dir (which the suite then deleted), and equally moves a developer's own GUI home. One node per
    // module, so the suites cannot collide with each other either. Pinned by `PreferencesNodeTest`.
    systemProperty("de.griefed.serverpackcreator.preferences.node", "ServerPackCreator-test-${project.name}")
    // And an isolated home to go with it: `<module>/tests`, the directory the project already reserves for exactly
    // this (gitignored bar its .gitkeep, and what `server.tomcat.basedir` has always pointed at). ApiWrapper.setup()
    // *writes* into the home directory -- README.md, CHANGELOG.md, the server_files templates, manifests, logs -- and
    // with no stored home a dev build falls back to the working directory, which for a test JVM is the module's own
    // source tree; that is how a suite once overwrote serverpackcreator-clientside/README.md's CLI guide with the
    // bundled root README. Pinned by `PathsConfigTest`.
    systemProperty("de.griefed.serverpackcreator.home", layout.projectDirectory.dir("tests").asFile.absolutePath)
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
    // Everything in the test home is disposable *except* the version manifests. Those are a cache of immutable
    // upstream data -- SPC seeds them from the jar and fetches a per-version `mcserver/<version>.json` on demand --
    // so deleting them makes every run re-download, which contradicts the module's documented "no live network
    // needed" and quietly eats any newly-fetched version. Measured 2026-07-31: a single test task took the cache
    // from 643 files to 0, and that is what kept deleting the hand-seeded Minecraft 26.2 metadata during the Forge
    // work, and what left the newest versions resolving as "required Java unknown" in the template matrix.
    // `updateManifests` benefits too: it copies this directory into the shipped resources, so preserving it lets
    // the snapshot accumulate versions released since the last refresh instead of being capped at the seeded set.
    projectDir.resolve("tests")
        .listFiles()
        .filter { !it.name.endsWith("gitkeep") && it.name != "manifests" }
        .forEach {
            it.deleteRecursively()
        }
    // Deliberately does NOT touch the Preferences store any more. This used to `removeNode()` the shared,
    // machine-wide `ServerPackCreator` node and write the module's test directory into it as the home -- so every
    // `test` or `clean` invocation relocated the home of the developer's own GUI, and of any running daemon, into
    // the repository. The isolated per-module node and `-Dde.griefed.serverpackcreator.home` injected on the test
    // task above replace it completely; SPC prefers that property over the stored preference, so nothing needs a
    // stored value. Verified: the shared node held a repo test path from this mechanism.
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
