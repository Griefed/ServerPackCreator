rootProject.name = "serverpackcreator"

dependencyResolutionManagement {
    // The single source of repositories for every module. Declaring them here rather than per-project
    // is what keeps a new module from silently resolving against a different set; RepositoriesMode
    // makes a stray project-level `repositories { }` a build failure rather than a quiet override.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        // jitpack serves com.github.MCRcortex:nekodetector, which is published nowhere else.
        maven("https://jitpack.io")
        // Spring milestones and the install4j runtime jar, both needed by -app.
        maven("https://repo.spring.io/milestone")
        maven("https://maven.ej-technologies.com/repository")
    }

    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

include(":serverpackcreator-api")
include(":serverpackcreator-clientside")
include(":serverpackcreator-grinder")
include(":serverpackcreator-app")
include(":serverpackcreator-web-frontend")
include(":serverpackcreator-plugin-example")