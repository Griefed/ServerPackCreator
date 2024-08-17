rootProject.name = "serverpackcreator"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

include(":serverpackcreator-api")
include(":serverpackcreator-app")
include(":serverpackcreator-web-frontend")
include(":serverpackcreator-plugin-example")