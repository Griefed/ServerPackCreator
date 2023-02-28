# serverpackcreator-api

The heart and soul of ServerPackCreator. This is what's responsible for turning your modpacks into server packs.

## Building

`serverpackcreator-api:build --full-stacktrace --info`

## Testing

`:serverpackcreator-api:clean :serverpackcreator-app:test --stacktrace --info`

## Implementing

### Dependency

#### Gradle Groovy DSL

```groovy
// https://mvnrepository.com/artifact/de.griefed.serverpackcreator/serverpackcreator-api
implementation group: 'de.griefed.serverpackcreator', name: 'serverpackcreator-api', version: '$VERSION'
```

#### Gradle Kotlin DSL

```kotlin
// https://mvnrepository.com/artifact/de.griefed.serverpackcreator/serverpackcreator-api
implementation("de.griefed.serverpackcreator:serverpackcreator-api:$VERSION")
```

#### Maven

```xml
<!-- https://mvnrepository.com/artifact/de.griefed.serverpackcreator/serverpackcreator-api -->
<dependency>
    <groupId>de.griefed.serverpackcreator</groupId>
    <artifactId>serverpackcreator-api</artifactId>
    <version>$VERSION</version>
</dependency>
```

### Usage

Initialize a new instance of the API:

#### Default

```kotlin
//Will create an instance of the API with default values and all files and folders residing in the users home-directory.
val api = ApiWrapper.api() 
```

#### Custom

```kotlin
//File path to a serverpackcreator.properties file with custom configurations
val propertiesFile = File("/path/to/a/custom/properties.file") 

val language = "en_us"

//false if you don't want the API to run the much needed file and folder setups
val runSetup = true

val api = ApiWrapper.api(propertiesFile, language, runSetup)
```

For concrete usage examples, browser any of the other modules in this repository:

- CLI: [serverpackcreator-cli](../serverpackcreator-cli)
- GUI: [serverpackcreator-gui](../serverpackcreator-gui)
- WEB: [serverpackcreator-web](../serverpackcreator-web)