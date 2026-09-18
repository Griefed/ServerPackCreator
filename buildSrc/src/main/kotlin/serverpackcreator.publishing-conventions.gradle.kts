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

// LANDMINE - `withJavadocJar()` is deliberately absent. It registers Gradle's stock `javadocJar`,
// which zips the stock `javadoc` task, and this module is pure Kotlin: `javadoc` documents nothing, so
// that jar is a 261-byte manifest and nothing else. Worse, it collides -- `dokka-conventions` registers
// `dokkaJavadocJar` with the same `javadoc` classifier, so both wrote
// `build/libs/<name>-<version>-javadoc.jar` and whichever ran last won. That is why the release ASSET
// javadoc jar was empty (the `assets` job runs `build`, which ran the stock task and never Dokka's)
// while Maven Central's was 2.4 MB (the `maven` job runs `:serverpackcreator-api:dokkaJavadocJar`
// first). The real javadoc is attached by `dokka-conventions`; do not re-add this.
java {
    withSourcesJar()
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
            // git.griefed.de is FORGEJO now, not GitLab. The old URL was a GitLab path
            // (/api/v4/projects/63/packages/maven) authenticated with a `Private-Token` header, and
            // neither exists on Forgejo: its package registry is /api/packages/{owner}/maven and it
            // authenticates with ordinary HTTP Basic. The repository keeps the name `GitGriefed` so the
            // generated task name -- publishMavenJavaPublicationToGitGriefedRepository, which CI calls --
            // does not change.
            name = "GitGriefed"
            url = uri("https://git.griefed.de/api/packages/Griefed/maven")
            credentials {
                username = System.getenv("FORGEJO_ACTOR")
                password = System.getenv("FORGEJO_TOKEN")
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
            // LANDMINE - without `from(components["java"])` this publication has NO main artifact and
            // NO dependencies. Gradle then writes `<packaging>pom</packaging>` and an empty POM, and the
            // module is undeployable as a library: anyone declaring
            // `de.griefed.serverpackcreator:serverpackcreator-api` got a POM and a javadoc jar, no
            // classes. Verified on Maven Central for 7.3.0, 8.0.0, 8.1.0, 8.1.2, 9.0.0-alpha.6,
            // 9.0.0-alpha.9 and 9.0.0-beta.1 -- every one of them ships exactly `-javadoc.jar` + `.pom`.
            // The component carries the main jar, the sources jar (via `withSourcesJar()` above) and the
            // resolved dependency list; the javadoc jar is added by `dokka-conventions`, which is the
            // plugin that owns it.
            from(components["java"])
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
