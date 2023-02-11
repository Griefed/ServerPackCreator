pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "serverpackcreator"
include(":serverpackcreator-api")
include(":serverpackcreator-updater")
include(":serverpackcreator-cli")
include(":serverpackcreator-gui")
include(":serverpackcreator-web-frontend")
include(":serverpackcreator-web")
include(":serverpackcreator-app")
include(":serverpackcreator-plugin-example")