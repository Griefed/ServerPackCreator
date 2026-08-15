/**
 * Publishing for the ONE module that is published: `serverpackcreator-api`.
 *
 * This used to live in `java-conventions`, so all six modules got `maven-publish`, `signing`, a
 * `mavenJava` publication, three remote repositories, a sources jar and a javadoc jar — while CI
 * publishes `:serverpackcreator-api` and nothing else (`.gitlab-ci.yml`: four
 * `:serverpackcreator-api:publish...` invocations). Applying it only where it is used removes the
 * apparent complexity from the other five and makes the published surface obvious.
 */

plugins {
    id("serverpackcreator.java-conventions")
    `maven-publish`
    signing
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
