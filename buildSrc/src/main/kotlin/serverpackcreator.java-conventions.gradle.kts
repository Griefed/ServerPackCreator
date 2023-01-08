@file:Suppress("UnstableApiUsage")

import de.griefed.common.gradle.constant.JDK_VERSION
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.text.SimpleDateFormat
import java.util.*

repositories {
    mavenCentral()
}

plugins {
    `java`
    `java-library`
    `maven-publish`
    signing
}

java {
    // Auto JDK setup
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JDK_VERSION))
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

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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


tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Built-By" to System.getProperty("user.name"),
                "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date()),
                "Created-By" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${
                    System.getProperty("java.vm.version")
                })",
                "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${
                    System.getProperty("os.version")
                }",
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
}

signing {
    val signingKey = findProperty("signingKey").toString()
    val signingPassword = findProperty("signingPassword").toString()
    useInMemoryPgpKeys(signingKey, signingPassword)
}