## [3.0.0-alpha.9](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.8...3.0.0-alpha.9) (2021-11-27)


### :scissors: Refactor

* Add getters/setters and allow reloading of properties. Also add some documentation to properties as well as more default values, just to be on the safe side ([049925e](https://git.griefed.de/Griefed/ServerPackCreator/commit/049925e9ddad7e89ed5f735ddb33da9325375a86))
* Display download button only if status is Available ([9c0edc7](https://git.griefed.de/Griefed/ServerPackCreator/commit/9c0edc71c4180725269d1a3ddcb7ca4958d89e4d))
* Display MB behind size ([1188b9f](https://git.griefed.de/Griefed/ServerPackCreator/commit/1188b9f0c687e3301e1e4d695450e0b5f1890f71))
* Do not directly access the ServerPackRepository ([ba4bf2c](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba4bf2c9e57a0b982913dce816724d6c32f78edb))
* Just kill it. ([b6bbe54](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6bbe54ad03f89505350e9714af2d65ef6fec1fb))
* Only check for database existence when running as a webservice ([87618f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/87618f4f99d9376de0dd5ffc135265fec35cebef))
* Remove/extract commonly used fields and methods. Make sure our database is always present. Other. ([859ede1](https://git.griefed.de/Griefed/ServerPackCreator/commit/859ede176db6ae995c72405b95c584de298300ef))
* Remove/extract commonly used fields and methods. Work towards webservice ([abf0135](https://git.griefed.de/Griefed/ServerPackCreator/commit/abf01355447f0c3a0af4af97d1cac259ddc113fd))
* Remove/extract commonly used fields/methods ([1f40517](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f405176a505bfcb5932493f94924bf45e2ade19))
* Remove/extract commonly used fields/methods ([df84569](https://git.griefed.de/Griefed/ServerPackCreator/commit/df845695059550025d0f24326d69a9f7ebf3d9f4))
* Remove/extract commonly used fields/methods ([c9cc954](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9cc9548973d7b181ff91175ac1bd5959740c81f))
* remove/extract commonly used fields/methods. Use configurationModel for everything. ([4ea254f](https://git.griefed.de/Griefed/ServerPackCreator/commit/4ea254fcf3aa6503efb8a168d54346af45f93150))
* Replace file-saver with call to api. Improves downloading of server packs. ([b60aeb7](https://git.griefed.de/Griefed/ServerPackCreator/commit/b60aeb7ddbb8b1f3354cae2313136c7a193fc917))
* Set status to Queued for a new instance ServerPack ([e2eb166](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2eb166e31a3a26a145283b68242c996cff65884))
* Throw custom exceptions on incorrect IDs ([875817c](https://git.griefed.de/Griefed/ServerPackCreator/commit/875817c7ee2ea024c631b9a37794feb690e434cd))
* Use FIleUtils for copying ([4529017](https://git.griefed.de/Griefed/ServerPackCreator/commit/452901776346acf5318b5629367e1e3f75b2317f))
* Use FIleUtils for copying and deleting, Files for deleting files. Replace messages with lang keys ([186d610](https://git.griefed.de/Griefed/ServerPackCreator/commit/186d6107e799fda23ea6259382d6fda261eaa253))
* Use FIleUtils for copying, Files for deleting ([4459847](https://git.griefed.de/Griefed/ServerPackCreator/commit/4459847bfc94117773605e07a6dc26e6716a8c51))
* **webservice:** Display status as "Generating" if server pack is being generated. Refactor regeneration to use queueing-system. ([78b88f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/78b88f22b18ba87723d3808586b496abcc3ab25e))
* **webservice:** Move ScanCurseProject and GenerateCurseProject to separate classes to eliminate statics. Closes GL[#88](https://git.griefed.de/Griefed/ServerPackCreator/issues/88) ([5815eb7](https://git.griefed.de/Griefed/ServerPackCreator/commit/5815eb7e8dd2abc7a0cdc2287e950b2f0bb2e683))
* **webservice:** Remove unnecessary logging ([a619997](https://git.griefed.de/Griefed/ServerPackCreator/commit/a6199977958c4040657976750d9093bf6922cb4f))
* **webservice:** Set download-filename to fileDiskName + _server_pack-zip ([e597dc4](https://git.griefed.de/Griefed/ServerPackCreator/commit/e597dc4804896d971951f183e09a585a8943a956))
* **webservice:** Set initial rows per page to 13 ([e45cf0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/e45cf0e21a0b535f06358aa37016b3c8d38590a6))
* **webservice:** Store size in MB and display size in frontend in MB ([37d4daa](https://git.griefed.de/Griefed/ServerPackCreator/commit/37d4daa3e2863ab6077174d9249478c0ea179a1a))


### ⏩ Performance

* Improve project- and filename acquisition by checking project and files directly ([f6e7b54](https://git.griefed.de/Griefed/ServerPackCreator/commit/f6e7b5454e316ad3f7acb0958d69476e3dcbf163))


### 📔 Docs

* Update CONTRIBUTING with step-by-step guide on how to contribute to ServerPackCreator ([db3b061](https://git.griefed.de/Griefed/ServerPackCreator/commit/db3b06100510d2a2e35c0ce92cbf6c04d01c6b1f))
* Update licenses ([21ae0ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/21ae0ad3f704b997ac4823a447fbeae1c9bbe1a1))
* Update README with info regarding contributions. Closes GL[#75](https://git.griefed.de/Griefed/ServerPackCreator/issues/75). ([e3d499c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3d499cf948f58084ee2afd8568bdb50ba483d3a))
* Update templates ([9fe1101](https://git.griefed.de/Griefed/ServerPackCreator/commit/9fe11013ba346443124d5c2cadb1364e4633cef7))
* Write docs for all the REST API classes, methods etc. I've been working on for the last couple of weeks. This commit also contains some minor refactorings, but nothing major or worth a separate commit. ([26519a0](https://git.griefed.de/Griefed/ServerPackCreator/commit/26519a002538bc01de17ad6debbb45d334527694))


### 🦊 CI/CD

* Update Gradle to 7.3 ([5dafa9e](https://git.griefed.de/Griefed/ServerPackCreator/commit/5dafa9ee7e7e6ee8beb2126296fed1853eb5f978))
* **deps:** bump spring-boot-devtools from 2.5.6 to 2.6.0 ([678e175](https://git.griefed.de/Griefed/ServerPackCreator/commit/678e1750ee6a29def7d52920b5699c0b7ed89322))
* **deps:** bump spring-boot-starter-data-jpa from 2.5.6 to 2.6.0 ([dc8797a](https://git.griefed.de/Griefed/ServerPackCreator/commit/dc8797af78b505599e5f8fa7916c93030324fc52))
* **deps:** bump spring-boot-starter-log4j2 from 2.5.6 to 2.6.0 ([5b67e52](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b67e52fd5c7783d8a08cd892ed6ef285d336836))
* **deps:** bump spring-boot-starter-quartz from 2.5.6 to 2.6.0 ([0433e90](https://git.griefed.de/Griefed/ServerPackCreator/commit/0433e905151ef0a60a2f8a00f5cd5587c4bf024c))
* **webservice:** Add artemis dependency for queueing system. Update dependencies. Exclude redundant slf4j. ([0954a56](https://git.griefed.de/Griefed/ServerPackCreator/commit/0954a56cf7ef8b1b8d26152a0b45aff86e3767cf))
* Reactivate docker jobs ([4b520c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b520c2f39e28633b25788300cf88e2a1c531d5f))
* Remove unnecessary login to docker registry ([e5b034f](https://git.griefed.de/Griefed/ServerPackCreator/commit/e5b034f331e3b1d238da8e25254cf105d304e484))
* Run GitHubs dependabot on dependabot-branch and run tests on GitHubs infrastructure. The more the merrier ([659f0f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/659f0f4bd721befa0b3a57f4699a437390c7fbbb))
* Tag dev-images with short_sha as well. Remove some artifacts ([f3f9913](https://git.griefed.de/Griefed/ServerPackCreator/commit/f3f9913797cc55458eef5eca7554c4de877f1adf))


### 🧪 Tests

* Adapt tests ([e20f89d](https://git.griefed.de/Griefed/ServerPackCreator/commit/e20f89d34ecbcc85edea44264715ac90c47bc7af))
* Autowire jmsTemplate ([1ba6968](https://git.griefed.de/Griefed/ServerPackCreator/commit/1ba6968cb942ede7a211f58cb2aae930ad97fa66))
* Don't delete default files after testing for them. ([b34602c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b34602c1a0ba30481c25fbb580c17d3331513ddc))
* Fix some tests ([5ba12ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ba12adf856ea9a0341393e56665c0c7f873649b))
* Hopefully fix ArtemisConfigTest ([7573d99](https://git.griefed.de/Griefed/ServerPackCreator/commit/7573d99bbc009eeb987d1743dae6e55896ea7545))


### 🚀 Features

* Allow specifying custom server-icon.png and server.properties. The image will be scaled to 64x64. Implements GH[#88](https://git.griefed.de/Griefed/ServerPackCreator/issues/88) and GH[#89](https://git.griefed.de/Griefed/ServerPackCreator/issues/89). ([e3670e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3670e4ffc15505856ae9695f59f3c614e0199dd))
* Basic filewatcher to monitor a couple of important files. Example: Delete serverpackcreator.properties to reload defaults ([d3f194a](https://git.griefed.de/Griefed/ServerPackCreator/commit/d3f194abb2ef55e168c094290263d4e78162cc91))
* Implement voting-system for server packs. Improve styling of download table. ([e49fa96](https://git.griefed.de/Griefed/ServerPackCreator/commit/e49fa96e4d2268441d67b8cd253c67e92dc33128))
* in start scripts: Ask user whether they agree to Mojang's EULA, and create `eula=true` in `eula.txt` if they specify I agree. Closes GH[#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83) ([5995f51](https://git.griefed.de/Griefed/ServerPackCreator/commit/5995f512d2731ebbd161c0ff8e34e37a437da0ac))
* **gui:** Add button in menubar to clear GUI. Allows starting with a fresh config without having to restart ServerPackCreator. Implements GH[#91](https://git.griefed.de/Griefed/ServerPackCreator/issues/91) ([dddee02](https://git.griefed.de/Griefed/ServerPackCreator/commit/dddee0286ca110bb25c75ff5d66756e86130b356))
* **gui:** Save the last loaded configuration alongside the default serverpackcreator.conf, unless a new configuration was started. Can be activated/deactivated with `de.griefed.serverpackcreator.configuration.saveloadedconfig=true` or `false` respectively ([e03b808](https://git.griefed.de/Griefed/ServerPackCreator/commit/e03b8089dca9ca40aa8d2a07948603888fbefd70))
* **webservice:** Add fields and methods required by CurseForge modpack creation ([f3d8da9](https://git.griefed.de/Griefed/ServerPackCreator/commit/f3d8da93a1ea988cab7e5d4f2dbd495c947ffc11))
* **webservice:** Allow downloads of available server packs. Expand checks for queueing system of newly submitted generation request. ([7b75064](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b75064495786ec1f5165f8a275f4ebb57cdb53c))
* **webservice:** Allow overriding of Spring Boot properties with an external application.properties in the directory where SPC is being executed in. ([8b03f59](https://git.griefed.de/Griefed/ServerPackCreator/commit/8b03f59da6b8ae9e167a6d05dad2bdae99fe8afa))
* **webservice:** Allow the creation of a server pack from a CurseForge projectID and fileID as well as regeneration (enable in properties) of said server pack. ([fd7f6d3](https://git.griefed.de/Griefed/ServerPackCreator/commit/fd7f6d3f2aa9e39da3f1385c1527d917bb4fe6f2))
* **webservice:** Check for -web arg. Explicitly warn user about running web on Windows machines. Move DI to better accommodate mode chosen. ([1585130](https://git.griefed.de/Griefed/ServerPackCreator/commit/15851303fb7f96406d8dd2e3abe36f335ab55527))
* **webservice:** Expand properties for webservice and extract default dirs to exclude ([bbfc0fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbfc0fa2a3ca6d63415c8e29f868e56c2cf50e00))
* **webservice:** Increment download counter when downloading a server pack ([2ba7adf](https://git.griefed.de/Griefed/ServerPackCreator/commit/2ba7adfc9b2685e8ee0cbb0f765a0b0317f07aa2))
* **webservice:** More work towards the frontend. Create a server pack from a CurseForge projectID and fileID. Dark mode switch. Interactive background. Mobile compatibility changes. ([11ab90f](https://git.griefed.de/Griefed/ServerPackCreator/commit/11ab90f6b52d7dba1311cdd6a04974736d6f25ba))
* **webservice:** Move commonly used fields to custom properties. Allow reload via reload() ([b60e723](https://git.griefed.de/Griefed/ServerPackCreator/commit/b60e7231bfb6a9c4228990264de783ca609470fc))
* **webservice:** Populate Downloads-section with server packs from database and allow download if available. Add search-function and allow users to select visible columns. ([80f4b8d](https://git.griefed.de/Griefed/ServerPackCreator/commit/80f4b8d3d9ae4980df9257d812e736d989c9d26a))
* **webservice:** Queueing-system for generating server packs one-by-one. ([93a666a](https://git.griefed.de/Griefed/ServerPackCreator/commit/93a666a54a89c74d63993d0c42de833338f93cbe))
* **webservice:** Remove/extract commonly used fields/methods. Couple of refactorings of run() for webservice. ([6d986f9](https://git.griefed.de/Griefed/ServerPackCreator/commit/6d986f9bd221fbdfc408db99e6d8a7ff5e694382))
* **webservice:** Update database entry for newly created server pack with availability, filesize and cleanup no longer needed files. ([a3c5694](https://git.griefed.de/Griefed/ServerPackCreator/commit/a3c56948be7f0dd1a4532c2dbc0e163c03d9f8df))


### 🛠 Fixes

* Catch occasional error from CurseForge's API which could lead to dead entries in the database ([625a8a8](https://git.griefed.de/Griefed/ServerPackCreator/commit/625a8a83647a3fd875b80c629159c2874f667f63))
* Fix axios instance for api ([12508f3](https://git.griefed.de/Griefed/ServerPackCreator/commit/12508f34884ebce85d88386f35363efd34d35e1d))
* Fix building of list of fallbackmods if property contains , ([e000f25](https://git.griefed.de/Griefed/ServerPackCreator/commit/e000f2549e673b505df6b5d71a5c8455d78ddfab))
* Fix downloading of server packs by storing the path of the server pack in the DB in the path column ([8a47213](https://git.griefed.de/Griefed/ServerPackCreator/commit/8a472136554f25ac06caf1a013fd64a5dda6e79e))
* Fix downloading of server packs by updating the path of the server pack in the DB in the path column ([64dc619](https://git.griefed.de/Griefed/ServerPackCreator/commit/64dc619389442cfe5f6eddbb9ad98969dd60d987))
* Fix user in Docker environment ([39f6bc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/39f6bc1fd6ca75e6783ae77c736983e601c550ab))
* Move destination acquisition into if-statement ([5d356a9](https://git.griefed.de/Griefed/ServerPackCreator/commit/5d356a95ec85cd04879a99c64538c113422f56ab))
* Move destination into if-statement ([9ae5ceb](https://git.griefed.de/Griefed/ServerPackCreator/commit/9ae5ceb8b314b5b6e065496118bc13aa6a3cab46))
* Prevent NullPointerException if version or author are not defined in the modpacks manifest. ([d7336ba](https://git.griefed.de/Griefed/ServerPackCreator/commit/d7336baaae13781538d132ed62b24e25825da721))
* remove `--` from Forge `nogui` argument. Fixes GH[#82](https://git.griefed.de/Griefed/ServerPackCreator/issues/82) ([f585891](https://git.griefed.de/Griefed/ServerPackCreator/commit/f58589114cd255a191b226c08c89f8dfeeac72dc))
* **webservice:** Display correct tooltips for buttons in MainLayout ([d4530d3](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4530d35727e3b092fdb8383f546dda8dcc825d2))


### Other

* Include JProfiler and ej-Technologies in Awesomesauce section ([b989173](https://git.griefed.de/Griefed/ServerPackCreator/commit/b9891736d997c0c6ad81a8f4b650a1e7c0368dec))
* New screenshots, comparisons between different modes ([12ed5f6](https://git.griefed.de/Griefed/ServerPackCreator/commit/12ed5f6ec63cf1a04dd357955fa799c07e05780c))
* Remove --no-daemon from run configurations ([a76e357](https://git.griefed.de/Griefed/ServerPackCreator/commit/a76e3570de7cb7cbf75a96697f122cf02e69e693))
* Remove mention of armv7 docker images as they are no longer being supplied ([72e8308](https://git.griefed.de/Griefed/ServerPackCreator/commit/72e83089ef328494dcb07115f649682eec7edd59))
* **deps:** pin dependencies ([358275b](https://git.griefed.de/Griefed/ServerPackCreator/commit/358275b16134c3953250e0dbcc763005a7a6b344))
* **deps:** update dependency @types/node to v16.11.10 ([e38cd23](https://git.griefed.de/Griefed/ServerPackCreator/commit/e38cd23fdda88247f678e718831dcb7f1dba7580))
* **deps:** update dependency org.apache.activemq:artemis-jms-server to v2.19.0 ([3245976](https://git.griefed.de/Griefed/ServerPackCreator/commit/3245976c0f88eef1e0e2b25da88d6eefed7e9dd3))

## [3.0.0-alpha.8](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.7...3.0.0-alpha.8) (2021-10-15)


### 👀 Reverts

* Do not create the eula.txt-file automatically. Reverts feature request issue [#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83). Lots of other smaller things, too many to list. ([ae66641](https://git.griefed.de/Griefed/ServerPackCreator/commit/ae66641b4e66e4711069289c79427651d10aaf11))

## [3.0.0-alpha.7](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.6...3.0.0-alpha.7) (2021-10-11)


### :scissors: Refactor

* Combine start and download scripts. Add checks for files in scripts. Removes option to generate scripts and generates them always instead. Closes issue [#81](https://git.griefed.de/Griefed/ServerPackCreator/issues/81) ([f037c34](https://git.griefed.de/Griefed/ServerPackCreator/commit/f037c34eb43b4910ea3002eba6362dd3d749261a))


### 🚀 Features

* Allow specifying files to add to server pack with simple `foo.bar` connotations. Closes issue [#86](https://git.griefed.de/Griefed/ServerPackCreator/issues/86) ([8a53aa6](https://git.griefed.de/Griefed/ServerPackCreator/commit/8a53aa6b9dbf148d60f4001a47e64055e8975d10))
* Create eula.txt upon server pack generation. Closes issue [#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83) ([d48191c](https://git.griefed.de/Griefed/ServerPackCreator/commit/d48191cda634f8bb8cc4db2298a0848b8b14c2cc))


### 🛠 Fixes

* Generate Minecraft 1.17+ Forge compatible scripts. Fixes issue [#84](https://git.griefed.de/Griefed/ServerPackCreator/issues/84). ([7d07e1d](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d07e1dad99c175b330f18c4c6cc83b00d43acac))

## [3.0.0-alpha.6](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.5...3.0.0-alpha.6) (2021-10-01)


### :scissors: Refactor

* Remove elements starting with ! from list instead of avoiding them with an ugly if-statement ([b8c84e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/b8c84e1294d7e8feebd34a0da202f8dc60d02d78))


### 🦊 CI/CD

* Replace Typesafe with Nightconfig, allowing for more safety measures ([b9939b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/b9939b101e906b7a578794cf79659c5035e9c692))


### 🚀 Features

* Store server pack suffix in serverpackcreator.conf.l Closes [#77](https://git.griefed.de/Griefed/ServerPackCreator/issues/77) again. ([d6c74e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/d6c74e0f62f395ea171266daca6194e39f0f634a))


### 🛠 Fixes

* Fix some mods broken dependency definitions breaking SPC funcitonality. Closes issue [#80](https://git.griefed.de/Griefed/ServerPackCreator/issues/80). ([a1c8a7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a1c8a7ef419ba7dcf90b74694c5f04480edfe807))
* Fix status message in GUI being displayed incorrectly on some Linux distros. Closes issue [#79](https://git.griefed.de/Griefed/ServerPackCreator/issues/79) ([5e7c08d](https://git.griefed.de/Griefed/ServerPackCreator/commit/5e7c08d886c9b1b7ef0640fe9cfe6f54e0d1fdc9))
* Print correct string for server pack suffix ([08c69e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/08c69e1be591421138d88429bc007091a13837ab))

## [3.0.0-alpha.5](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.4...3.0.0-alpha.5) (2021-09-24)


### 🛠 Fixes

* Fix missing serverpackcreator.properties for tests and do not run tests on GitHub releases. ([8895be8](https://git.griefed.de/Griefed/ServerPackCreator/commit/8895be80bfc76165d0347ee97e750301d6870afe))

## [3.0.0-alpha.4](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.3...3.0.0-alpha.4) (2021-09-24)


### :scissors: Refactor

* Gather information from CurseForge modpack from JsonNodes instead of Class-mapping. Makes maintenance and expansion easier. Reduces complexity. ([caa033b](https://git.griefed.de/Griefed/ServerPackCreator/commit/caa033bae0d54a5e7171871ea7023e99fc5c99a0))


### 🦊 CI/CD

* Update frontend dependencies ([d953f31](https://git.griefed.de/Griefed/ServerPackCreator/commit/d953f31dbc75f0006b34445a20e074fbc698f9bc))


### 🚀 Features

* Allow users to exclude files and directories from the server pack to be generated with ! as the prefix in an entry in copyDirs ([f527d04](https://git.griefed.de/Griefed/ServerPackCreator/commit/f527d04dc67d5c2c186a460068aa84167278cafd))
* Allow users to set a suffix for the server pack to be generated. Requested in issue [#77](https://git.griefed.de/Griefed/ServerPackCreator/issues/77) ([2d32119](https://git.griefed.de/Griefed/ServerPackCreator/commit/2d321197c6123348558476b20b6f2c9aa93cc54f))


### Other

* Add missing space in lang keys for copyDirs help. Closes issue [#78](https://git.griefed.de/Griefed/ServerPackCreator/issues/78) ([3539582](https://git.griefed.de/Griefed/ServerPackCreator/commit/35395827fb5a8e837ccae61925a0557aae544f29))

## [3.0.0-alpha.3](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.2...3.0.0-alpha.3) (2021-09-04)


### :scissors: Refactor

* Create modpacks downloaded from CurseForge in the work/modpacks-directory. ([3178326](https://git.griefed.de/Griefed/ServerPackCreator/commit/3178326cc960bde4482e847c5464ef4f50ed856c))
* DI serverpackcreator.properties everywhere! ([4b01d4a](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b01d4a809a08e420d399af9b9e58dca2c526002))
* Initialize addons and check/create files when creating our DefaultFiles and AddonsHandler instances. ([864f10c](https://git.griefed.de/Griefed/ServerPackCreator/commit/864f10cd33e7f06693e47791ceeb7ac9a9e16974))
* Refactor tailers to run in threads. ServerPackCreator can still become unresponsive if you resize during zip-creation, after a Forge server was installed, though..... ([d4c986e](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4c986eaa2451989420fa9785fab6f86523c8755))
* Set server-packs directory to /server-packs. Add new configuration to config. Add volume to Dockerfile. Update documentation in README ([267e3e9](https://git.griefed.de/Griefed/ServerPackCreator/commit/267e3e9f168803209e26f8038a4c14d16d30b920))
* Switch options to YES_NO to ensure users is always warned about empty javapath setting if they did not choose to select it now. ([c6f4ef8](https://git.griefed.de/Griefed/ServerPackCreator/commit/c6f4ef8cfc5e138191079acbf773ab91cef0d091))


### ⏩ Performance

* Perform version checks with lists gathered by VersionLister. ([d440e5e](https://git.griefed.de/Griefed/ServerPackCreator/commit/d440e5e2c079ac44bc040d87cacb1f29951160d9))
* Retrieve Forge versions from HashMap with Minecraft version as key instead of re-reading list and lists and arrays of data again and again and again, ([0018abc](https://git.griefed.de/Griefed/ServerPackCreator/commit/0018abc4772b7e062fc5bd131a62edcceae4aac6))


### 💈 Style

* Declare fields above constructor. Only have methods under constructor. ([76c6b58](https://git.griefed.de/Griefed/ServerPackCreator/commit/76c6b584b05d48adf0714f4ad066c6cf0f5d775a))
* Reorder calls in Main.main to reflect importance. Makes it slightly more readable as well. ([576cbae](https://git.griefed.de/Griefed/ServerPackCreator/commit/576cbae9938563ef50dd27f174b3f340c4998f60))


### 📔 Docs

* Add missing parameter to setJavaArgs ([761e2fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/761e2fdcc110e96db825527471c60cc427078552))
* List server-packs directory for volumes ([82b13e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/82b13e43771a2964d1d6339994dd431e94701a67))
* Update table of methods for classes ([eeb6887](https://git.griefed.de/Griefed/ServerPackCreator/commit/eeb6887e3b52f67dd431adfe997ce1c144ab28fc))
* Write missing documentation for getters and setters for javaargs and javapath settings ([f29924b](https://git.griefed.de/Griefed/ServerPackCreator/commit/f29924bd00724b53669c51829b1497810b8596fb))


### 🦊 CI/CD

* Update dependencies. Cleanup & readability. ([fe583aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe583aa0f73326b328f2c672859053fe6c6b8b67))
* Disable Docker pipelines for the time being. Docker is acting up and building Docker images of the webservice-branch is not necessary as I have yet to start actual work on the webservice itself. ([f45e25f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f45e25f681102dd991ff179a59df7c9fb85af227))


### 🚀 Features

* Allow users to disable cleanups of server packs and downloaded CurseForge modpacks. Can save bandwidth, time and disk operations, if the user is interested in that. ([3155af4](https://git.griefed.de/Griefed/ServerPackCreator/commit/3155af499006eba64751cca01e53e45480e8e936))
* Allow users to disabled server pack overwriting. If de.griefed.serverpackcreator.serverpack.overwrite.enabled=false AND the server pack for the specified modpack ALREADY EXISTS, then a new server pack will NOT be generated. Saves a LOT of time! ([00dd7aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/00dd7aa15b8cdbdce91f6d510fc2505f2f6e9d1a))
* Allow users to specify a custom directory in which server-packs will be generated and stored in. ([4a36e76](https://git.griefed.de/Griefed/ServerPackCreator/commit/4a36e76bfab5a66ce52c51e57bb16af79dddb752))
* Check setting for Javapath upon selecting "Install modloader-server?". If it is empty, the user is asked whether they would like to select their Java executable now. If not, the user is warned about the danger of not setting the Javapath ([5d474f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/5d474f1cf2763c010b6c02f969e2843de96d339f))
* Provide HashMap of Key-Value pairs in MinecraftVersion-ForgeVersions format. Use a given Minecraft version as key and receive a string array for available Forge versions for said MInecraft versions. ([0a0d3b5](https://git.griefed.de/Griefed/ServerPackCreator/commit/0a0d3b50c7d7e955c41ce148bb82d4fc9abe6ac1))


### 🛠 Fixes

* Clear text every 1000 lines. Help with issue [#76](https://git.griefed.de/Griefed/ServerPackCreator/issues/76). ([132a3dd](https://git.griefed.de/Griefed/ServerPackCreator/commit/132a3ddd903f8693e08d9252c1f3e9c6004aad3f))
* Hopefully fix ServerPackCreator becoming unresponsive after generating a few server packs. Hopefully closes issue [#76](https://git.griefed.de/Griefed/ServerPackCreator/issues/76). ([aa92d9b](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa92d9b5afb3ceec2345c311ae90062aa45ce6c5))
* Improve configuration loading. Prevent NullPointers when reading Minecraft version, modloader, modloader version. ([0507ab7](https://git.griefed.de/Griefed/ServerPackCreator/commit/0507ab736d852415f2666937b1174429e7bac109))
* Open dialog whether the user wants to browse the generated server pack with our JFrame as parent, instead of JTabbedPane ([aa647f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa647f77429e6207927e5b1a743cb5b8f0be4887))
* Prevent dialog after server pack generation from becoming longer with each run. Removes the path to the server pack, though. Meh ([2260693](https://git.griefed.de/Griefed/ServerPackCreator/commit/226069366091155e11d9a1b7da9521f9802f168d))
* Prevent resizing of window during generation of server pack, to prevent freezes due to Forge installer log spamming. Seriously, that thing spams more than any bot I know of. ([89edc6f](https://git.griefed.de/Griefed/ServerPackCreator/commit/89edc6f61fbd40e1b1ed46871d70f103139200a5))
* Read correct log in modloader-installer log tab ([095d05e](https://git.griefed.de/Griefed/ServerPackCreator/commit/095d05edd1235957e13b98122deba8c54c9efa12))


### Other

* Remove unused language keys ([43fdba7](https://git.griefed.de/Griefed/ServerPackCreator/commit/43fdba70b1dfc52139c9fb2f255a065bdd92ef12))

## [3.0.0-alpha.2](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.1...3.0.0-alpha.2) (2021-08-29)


### :scissors: Refactor

* Add additional catch for NPE. Fix typo in docs. Remove unused field. ([b5f9042](https://git.griefed.de/Griefed/ServerPackCreator/commit/b5f90421002124b7a1e53f2c11581ead7fab00a2))
* Just some renamings...nothing important. ([2c65582](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c65582691abf06558deaf4461c90265770bb6d1))
* Only provide translations for messages which actually have a need for translation. Error/debug messages mainly do not need to be translated, as those will be reported in issues, therefore I need to be able to read them. ([2132baa](https://git.griefed.de/Griefed/ServerPackCreator/commit/2132baa6a19000ffdabec555a3e3bca5c8fc0708))
* Reverse lists of modloader versions to display in order of newest to oldest versions. Closes issue [#74](https://git.griefed.de/Griefed/ServerPackCreator/issues/74). ([4534d87](https://git.griefed.de/Griefed/ServerPackCreator/commit/4534d8774056f9de3d2063ea130c7bd85a4a6137))


### 📔 Docs

* Update table of methods ([dabf028](https://git.griefed.de/Griefed/ServerPackCreator/commit/dabf02866d58a72159642452c46b3ca6f109791a))


### 🦊 CI/CD

* Add Breaking section to changelog ([7165659](https://git.griefed.de/Griefed/ServerPackCreator/commit/7165659d8ccb507be63047c3b0f37d2fca2ac859))


### 🚀 Features

* Add methods to reverse the order of a String List or String Array. Allows setting of lists in GUI with newest to oldest versions. ([11d565e](https://git.griefed.de/Griefed/ServerPackCreator/commit/11d565ef61ed9ea2d324b82b4cb49ec529ffe624))
* **gui:** Open server-icon.png in users default picture-viewer. From there on, users can open their favourite editing software. ([d960dd2](https://git.griefed.de/Griefed/ServerPackCreator/commit/d960dd28f7e796b8d7f84dfbcfe55273e60cfec8))
* **gui:** Open server.properties in users default text editor via Edit->Open server.properties in Editor ([1bf7533](https://git.griefed.de/Griefed/ServerPackCreator/commit/1bf75338e60b4fe0ff85eca6a55308eb4538fe7f))
* **gui:** Redesign help window. Users can choose what they need help with from a list, which then displays the help-text for the chosen item. ([7c490a3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c490a3d2a205181c61148ad4ff9b8872ff5961b))
* **gui:** Set LAF for Java Args correctly. If javaArgs is "empty", display textField as "" to not confuse users. ([462e7a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/462e7a1cef59715b08ff5f20ac03ae760a45132c))


### 🛠 Fixes

* Allow translating for full GUI as well as missing parts in backend. ([366cb10](https://git.griefed.de/Griefed/ServerPackCreator/commit/366cb106fddbebb1411105d466017c2f36e19a63))
* Prevent encapsulateListElements from writing duplicate entries ([1e64cd6](https://git.griefed.de/Griefed/ServerPackCreator/commit/1e64cd67dcbfcf95ccb544f84b70ee39e5123e75))
* When writing configfiles, encapsulate every element of String Lists in `"` in order to avoid problems described in issue [#71](https://git.griefed.de/Griefed/ServerPackCreator/issues/71). Fixes and closes issue [#71](https://git.griefed.de/Griefed/ServerPackCreator/issues/71). ([0e029ec](https://git.griefed.de/Griefed/ServerPackCreator/commit/0e029ec477864ea765e8ad446ac2b9b93186b952))


### Other

* Fix minor typo in language key ([9177763](https://git.griefed.de/Griefed/ServerPackCreator/commit/91777632c7ef1715f45af28ddb4f0848d5abb432))

## [3.0.0-alpha.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.2.0-alpha.5...3.0.0-alpha.1) (2021-08-27)


### :scissors: Refactor

* Upgrade to Gradle 7.2. Remove Fabric-Installer dependecy by retrieving the Minecraft server url ourselves. ([e297f63](https://git.griefed.de/Griefed/ServerPackCreator/commit/e297f6347e393359ac71b0a70c388afd759355a8))


### 📔 Docs

* Update README with new feature information and reflect changes made to file-structure ([04ffed5](https://git.griefed.de/Griefed/ServerPackCreator/commit/04ffed5e30c450520132d984e0c2974cafc777d1))
* Update README with new feature information and reflect changes made to file-structure ([b3f211c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b3f211cf51abd589672fe3005f0cfc9ef76cec76))


### 🚀 Features

* **gui:** Various changes. Too many to list. MenuBar entries, Theme changes. MenuItem funcitionality etc. etc. ([28c088c](https://git.griefed.de/Griefed/ServerPackCreator/commit/28c088cc5395a432ac6cbd83f2b31643922bf858))


### Other

* Fix tests, docs and add TODOs regarding lang keys ([2dac4e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/2dac4e1f0a7e53f7b04cfce982c1a6d2c99c5747))
* Remove no longer relevant license ([64fbeeb](https://git.griefed.de/Griefed/ServerPackCreator/commit/64fbeeb9593a3696b9a53f1f436bbdf6d00e22e9))

## [2.2.0-alpha.5](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.2.0-alpha.4...2.2.0-alpha.5) (2021-08-25)


### :scissors: Refactor

* Change labels for Minecraft, modloader and modloader version to better reflect new feature of selection from lists. ([84755a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/84755a185c02948050d0e534b2a5771898f13aff))
* Extract actions and events into separate methods. Improves maintainability. ([7a335da](https://git.griefed.de/Griefed/ServerPackCreator/commit/7a335dab87acbd4f136e520fb6c1af012659606d))
* Extract actions and events into separate methods. Improves maintainability. ([9268245](https://git.griefed.de/Griefed/ServerPackCreator/commit/9268245df88d96fbe358b68de488992e102d448c))
* Improve debug logging for VersionLister ([29be15f](https://git.griefed.de/Griefed/ServerPackCreator/commit/29be15fa5ba18ce8bdb0f4345e989ef843a63e75))
* Move assignemts to field declaration where applicable. Extract method for adding MouseListeners to buttons. ([b37ad30](https://git.griefed.de/Griefed/ServerPackCreator/commit/b37ad30ce88e570e4b8632760dee5cebab28f8da))
* Prevent going through a list of clientside-only mods automatically gathered from modpack is property is false. ([51a3e42](https://git.griefed.de/Griefed/ServerPackCreator/commit/51a3e42ea18e37453734c5cc6c4e2e63fea8bfee))


### 📔 Docs

* Name correct filename for properties according to merge of lang.properties with serverpackcreator.properties ([ed42dcd](https://git.griefed.de/Griefed/ServerPackCreator/commit/ed42dcd14479013e979f9793aae884b0c0cf1836))


### 🧪 Tests

* Remove addon execution from tests, as parallel running tests caused problems because the addon can only be accessed by one thread at a time. ([b963b10](https://git.griefed.de/Griefed/ServerPackCreator/commit/b963b1094e3a470213fc737f9effa305960ad31f))


### 🚀 Features

* Allow check of configuration from an instance of ConfigurationModel, without any file involved. ([17529fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/17529fa958fbb386dfe7bdc91eaec2f9ceff39f5))
* Allow generation of a server pack from an instance of ConfigurationModel ([5b54a1c](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b54a1ca9b3be3cc7d72e3c1851a636ee81a482e))
* New theme and cleaned up GUI. MenuBar for various things (wip). Lists for version selection. Switch between darkmode and lightmode and remember last mode used. More things, check commit. ([949fb6a](https://git.griefed.de/Griefed/ServerPackCreator/commit/949fb6aecd47518e0b91ca3a8be0516a9f2cb540))
* Read Minecraft, Forge and Fabric versions from their manifests into lists which can then be used in GUIs. ([c9ce1ff](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9ce1ff41f12b6eeef9dc00827d3e6a129ee8a5f))
* Select Minecraft and modloader versions from lists instead of entering text into a textfield. ([5b56f18](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b56f18a90e7d3f1bfda98d5ae509a9cda29e959))


### 🛠 Fixes

* Correctlry get property which decides whether autodiscovery of clientside-only mods should be enabled ([3c5deff](https://git.griefed.de/Griefed/ServerPackCreator/commit/3c5deff79acf70d5d6ea6d578cc4e73faf85d4d3))


### Other

* Remove no longer needed lang keys ([6435fbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/6435fbc73be7405290a48a16c2b053a0fa09e1ed))
* Remove unneeded imports ([8482d29](https://git.griefed.de/Griefed/ServerPackCreator/commit/8482d295eb1d731d1c02c654363dafe235ba9910))

## [2.2.0-alpha.4](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.2.0-alpha.3...2.2.0-alpha.4) (2021-08-20)


### 📔 Docs

* Add javadoc for scanAnnotations ([e0a08f9](https://git.griefed.de/Griefed/ServerPackCreator/commit/e0a08f9547891a2807fd20a89927856b2a86329d))


### 🦊 CI/CD

* Hopefully fix main release workflow trying to run on alpha/beta release ([9e6122e](https://git.griefed.de/Griefed/ServerPackCreator/commit/9e6122e7a5523d3b35850721062fe385f8c5d207))


### 🛠 Fixes

* Modloader selection visually defaulted to Forge if no configuration was found in a given serverpackcreator.conf, but the value wasn't correctly set, resulting in the user having to select Forge manually anyway. ([d126447](https://git.griefed.de/Griefed/ServerPackCreator/commit/d12644714a8281e5dd7063521e28235b9204d5a3))

## [2.2.0-alpha.3](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.2.0-alpha.2...2.2.0-alpha.3) (2021-08-19)


### :scissors: Refactor

* Move ObjectMapper init to getter like in ConfigurationHandler ([d73ebd4](https://git.griefed.de/Griefed/ServerPackCreator/commit/d73ebd40e3a77dc512bd4f542eb5780fa9663a3a))
* Move ObjectMapper init to getter like in ConfigurationHandler ([ac955c5](https://git.griefed.de/Griefed/ServerPackCreator/commit/ac955c520f434fba1dedaf0299213f6b85489709))
* Remove preparations for 1.12 and older clientside autodetection. See https://github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692 ([3638e22](https://git.griefed.de/Griefed/ServerPackCreator/commit/3638e22dd96cea72ec86d22f7c16d335eefa9bf0)), closes [/github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692](https://git.griefed.de/Griefed//github.com/Griefed/ServerPackCreator/issues/62/issues/issuecomment-901382692)
* Remove preparations for 1.12 and older clientside autodetection. See https://github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692 ([4977ae7](https://git.griefed.de/Griefed/ServerPackCreator/commit/4977ae7f01db82b79b1af0057e505877e4307ad9)), closes [/github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692](https://git.griefed.de/Griefed//github.com/Griefed/ServerPackCreator/issues/62/issues/issuecomment-901382692)


### 📔 Docs

* Add missing method to table ([d1fca12](https://git.griefed.de/Griefed/ServerPackCreator/commit/d1fca12b00b8b79cf0ede59d58295eeb61a80c5c))
* Add missing method to table ([f04b728](https://git.griefed.de/Griefed/ServerPackCreator/commit/f04b72818257e1d71b2e60dd86af8921c32e45eb))


### 🦊 CI/CD

* Bring in changes to CI from main ([b89125b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b89125ba34c873328f9e600f0bafd02586de1ad4))


### 🚀 Features

* Automatically detect clientside-only mods for Minecraft modpacks version 1.12 and older. ([e17322e](https://git.griefed.de/Griefed/ServerPackCreator/commit/e17322ed5db6bd18b4573be4a3562295317dd137))
* Enable/disable clientside-only mods autodiscovery via property de.griefed.serverpackcreator.serverpack.autodiscoverenabled=true / false. Closes [#62](https://git.griefed.de/Griefed/ServerPackCreator/issues/62). ([094a217](https://git.griefed.de/Griefed/ServerPackCreator/commit/094a217e83f2f27ba1e3746088b459a542411254))

## [2.2.0-alpha.2](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.2.0-alpha.1...2.2.0-alpha.2) (2021-08-17)


### :scissors: Refactor

* Allow configuration of hastebin server in serverpackcreator.properties. ([0235378](https://git.griefed.de/Griefed/ServerPackCreator/commit/023537882243979fd7f2b66fc69113eb43477902))
* Merge checkJavaPath and getJavaPathFromSystem ([0c982cb](https://git.griefed.de/Griefed/ServerPackCreator/commit/0c982cb5abd629e21fbc23c08b0a76240a4ea11f))
* More work towards allowing parallel runs of server pack generation. Split Configuration into ConfigurationModel and ConfigurationHandler ([cb3e8a7](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb3e8a79e86c023a35d5224a5f31b1539903c59e))
* Move language specification from lang.properties to serverpackcreator.properties. Move FALLBACKSMODSLIST to serverpackcreator.properties. ([bb11972](https://git.griefed.de/Griefed/ServerPackCreator/commit/bb119727113ba0cb8e58977348673860bcb47851))
* Rename and sort classes and packages to make more sense. ([5ca227d](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ca227d79a0dfcb40effe9eb344da9575cf8e9bc))
* Replace name or property-file to correct one ([ee0aab7](https://git.griefed.de/Griefed/ServerPackCreator/commit/ee0aab7a3fec9a3828e4248877bf1f968dc151c2))


### 🦊 CI/CD

* Add changes to github ci ([128ea30](https://git.griefed.de/Griefed/ServerPackCreator/commit/128ea30bbcd1011edb9a2fda85bfe1153863f787))


### 🧪 Tests

* Ensure serverpackcreator.properties is always available to prevent NPEs ([f674e13](https://git.griefed.de/Griefed/ServerPackCreator/commit/f674e137d44c3dfa3832d16c870aa865b1f6e6d6))
* Some cleanups. Nothing interesting ([12bc506](https://git.griefed.de/Griefed/ServerPackCreator/commit/12bc50602b411589b65f5e70e2024fbc0bff53f1))


### 🚀 Features

* Add tab for addons log tail. ([b84cc5b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b84cc5b12c9cd33176830d8eb413a1005a0d87a2))
* Automatically detect clientside-only mods for Minecraft modpacks version 1.13+. ([3811190](https://git.griefed.de/Griefed/ServerPackCreator/commit/3811190cb401c8993d84f0026618ad6e4958ed27))


### 🛠 Fixes

* Prevent NPE for clientside-only mod property ([b188a85](https://git.griefed.de/Griefed/ServerPackCreator/commit/b188a858f637b8329447be08ed3701c43a713b00))


### Other

* Clarify when I started with Java to put things into perspective. ([16f52f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/16f52f771587c94843a09eb46be7d047793b604e))
* Mention libraries used and add third-party licenses ([8d4c715](https://git.griefed.de/Griefed/ServerPackCreator/commit/8d4c71535a46335788b3f8337d1581144c18f6bc))
* Update gitignore to exclude new files generated by tests ([4147138](https://git.griefed.de/Griefed/ServerPackCreator/commit/4147138bfadee97e0671bfb1f8a3b41c657d62b3))
* Update README in resources ([4b8a3f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b8a3f4415a419e1b4acab1b86f79d83343da48f))
* Update third party-licenses ([b41a15f](https://git.griefed.de/Griefed/ServerPackCreator/commit/b41a15f94768f52069f3a969d511de9c387d0634))
* WHITESPACE! ([de9ebcc](https://git.griefed.de/Griefed/ServerPackCreator/commit/de9ebcc2147e6b205789d4f1c82720daed0a6ddd))

## [2.2.0-alpha.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.1.1...2.2.0-alpha.1) (2021-08-14)


### :scissors: Refactor

* Add -help argument explaining the basics of running ServerPackCreator. If -help is used, said help text is printed to the console and ServerPackCreator exited. ([4689f54](https://git.griefed.de/Griefed/ServerPackCreator/commit/4689f543359d7a5850d8cd26f2856ff88b719969))
* Add -lang argument information to -help display ([164073f](https://git.griefed.de/Griefed/ServerPackCreator/commit/164073fc8b1a461d35f94921fb2f444728672738))
* Copy log4j2.xml to basedir where JAR/EXE is executed. Improve logging-configuration and allow user to set level to DEBUG/INFO with '<Property name="log-level-spc">DEBUG</Property>' ([fcbe6cf](https://git.griefed.de/Griefed/ServerPackCreator/commit/fcbe6cfade911ee429bffd47b82cbe71b7f0d2bc))
* Disbale whitelist for tempalte server.properties ([bc4018e](https://git.griefed.de/Griefed/ServerPackCreator/commit/bc4018edf2c33a240f4cdf7d9d1ad4378854c8ba))
* Finish TODOs. Setup missing lang keys. Minor improvements to tests ([b884e7a](https://git.griefed.de/Griefed/ServerPackCreator/commit/b884e7a77469135a5e3eb0bf56c44fb1249d7f76))
* Generate server packs in ./server-packs in the directory where ServerPackCreator is executed in. Prevents 1. in [#55](https://git.griefed.de/Griefed/ServerPackCreator/issues/55) where the Overwolf CurseForge App filewatcher can cause installed mods to disappear due to copying mods around inside the modpack directory. ([539341d](https://git.griefed.de/Griefed/ServerPackCreator/commit/539341d68f54965b958d74e11e7e9fcc31da9ada))
* Improve automatic acquisition of java path from system environment. ([fae311e](https://git.griefed.de/Griefed/ServerPackCreator/commit/fae311ea2e5f0c38c7caec7a06d06ed43957eae5))
* Improve configuration check and tests. Add more debug logging. Add tests. ([b6da489](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6da489e08da8a20074f32ae938658649b982f3e))
* Improve dialog after uploading config and logs to hastebin ([da5e298](https://git.griefed.de/Griefed/ServerPackCreator/commit/da5e2981333806adf93f63bb549a48cb5d1e91b3))
* Improve dialog after uploading config and logs to hastebin ([13f4587](https://git.griefed.de/Griefed/ServerPackCreator/commit/13f4587e736743ae9217a12562077bcaeb33023b))
* Instantiate CreateGui only when GUI is actually about to be used ([d39730c](https://git.griefed.de/Griefed/ServerPackCreator/commit/d39730c86c9e8726716d2f6a4ca15bba3743ad5a))
* Modloader setting as a slider to select either Forge of Fabric ([4f9eb79](https://git.griefed.de/Griefed/ServerPackCreator/commit/4f9eb79f813d3f127d89d99151163f3186dabcf9))
* Refactor lang keys to better reflect where they're used. Add more lang keys for logging. Improve wording. Fix some minor typos. ([354fb2e](https://git.griefed.de/Griefed/ServerPackCreator/commit/354fb2e7003df6293ebb496c22d085493eb868c5))
* Refactor lang keys to better reflect where they're used. Add more lang keys for logging. Improve wording. Fix some minor typos. ([9553557](https://git.griefed.de/Griefed/ServerPackCreator/commit/9553557d40a129194c3b2fd478b83805f35b0805))
* Replace e.getStateChange() with ItemEvent.SELECTED. ([ab87c06](https://git.griefed.de/Griefed/ServerPackCreator/commit/ab87c06ea99443fa6856a152fd15d07fdd395c4e))
* Replace slider for modloader selection with radio buttons. Looks better and cleaner. Selection fires less events than slider did. ([c36189c](https://git.griefed.de/Griefed/ServerPackCreator/commit/c36189cf5252e0fe27701e779f6e539b1d79a335))
* Require file passed to CreateServerPack.run in order to generate server pack. Create new Configuration object with said file. Should allow parallel runs in the future, but needs to be tested when I get to that. ([67c0cba](https://git.griefed.de/Griefed/ServerPackCreator/commit/67c0cba498dece33f265c376c88cbe4b3ac6e77a))
* Rewrite unzipping of CurseForge acquired modpack with zip4j library ([9f8c87f](https://git.griefed.de/Griefed/ServerPackCreator/commit/9f8c87fca09beb239030b4228958a0e52c0d83c1))
* Set clientMods and javaPath with fallback-list and system environemnt respectively, if the config is empty or an invalid javaPath was specified. ([ff18c5e](https://git.griefed.de/Griefed/ServerPackCreator/commit/ff18c5e56f1416316a20158f66ce9f24c1ff7cd5))
* Set logger context with log4j2.component.properties ([7038dcf](https://git.griefed.de/Griefed/ServerPackCreator/commit/7038dcf76e61ca4adf85a2d842f4cdeafbc409e7))
* Simplify default files setup by merging methods which create our files. Instead of a separate method for each file, we have one method which gets passed different parameters depending on which file we want to setup. Makes maintenance easiert and code easier to read. ([9111e7c](https://git.griefed.de/Griefed/ServerPackCreator/commit/9111e7c58508700b31efeb617f110bae9a8b9f7f))
* Store Fabric installer manifest in work/*. Only refresh when SPC starts. Don't delete manifest files during runs of SPC. Rename lang keys to fit usage. Other misc changes. ([1927faa](https://git.griefed.de/Griefed/ServerPackCreator/commit/1927faa33da1063ba4eea239cabcf9c6a4335b8d))
* Store Minecraft, Fabric and Forge version validation in work/*. Only refresh them when SPC starts. Setup work, work/temp and server-packs folder for future use. ([ab080a6](https://git.griefed.de/Griefed/ServerPackCreator/commit/ab080a6024138972c0b34524c4c7a728c64b8f74))
* Switch back to old pattern format so GUI looks clean again ([483bdc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/483bdc15fedcf1db513b41169affda85a99cd0b4))
* **webservice:** Allow user to specify mode. Test whether libatomic1 works now. ([6dfa0dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/6dfa0dcf44652910c83ce8b269929893aa04a4b3))
* **webservice:** Set logging pattern for Spring to ours ([4348f76](https://git.griefed.de/Griefed/ServerPackCreator/commit/4348f7601b5d2818b0bd343e2f0cb33cab02e2ec))


### 📔 Docs

* Add author tags. Add link to GitHub issues in case anyone wants something added to fallbackModslist or directories for CurseForge automation. ([7699c64](https://git.griefed.de/Griefed/ServerPackCreator/commit/7699c64d4f7d14f3d13b86acb92489c1c0ba2a33))
* Add call to initializeAddons to main description ([ac14f99](https://git.griefed.de/Griefed/ServerPackCreator/commit/ac14f996a55402d1d5b8cc8930bbb1ead57852e7))
* Spelling and grammar fixesas well as [@author](https://git.griefed.de/author) tag fixes. ([9d157d6](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d157d6227ac3c484b740297c012f817c169abde))
* **webservice:** Enable debug log output for Docker build ([eaae701](https://git.griefed.de/Griefed/ServerPackCreator/commit/eaae701fb7d5666251a07f93a8bcd67fa4785b3a))


### 🦊 CI/CD

* Update dependencies ([e726f31](https://git.griefed.de/Griefed/ServerPackCreator/commit/e726f316c5928856a7b911be92d910f2ea6e6d26))
* Upgrade dependencies ([426ec44](https://git.griefed.de/Griefed/ServerPackCreator/commit/426ec440b54ff9909d202bbdfe697d1259d7773a))
* **deps:** Update commons-io to 2.11.0 ([b8a673a](https://git.griefed.de/Griefed/ServerPackCreator/commit/b8a673a8b744eb7653a2bbd359c0caadeac7ea72))
* **fabric:** Update default Fabric Installer version if it can not be acquired from external ([b6b0bc3](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6b0bc31f1b6c3f5065e6c65b7fb4c292e8aced6))
* **fabric:** Update default Fabric Loader version if it can not be acquired from external ([aa2f9e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa2f9e16ee05e60374a6f6b33368a3fc9f928feb))
* **webservice:** Do not run tests in Docker build. We have the Gradle Test stage for that. ([54b98fc](https://git.griefed.de/Griefed/ServerPackCreator/commit/54b98fc7eb143fd402a355118eeddef60ff03742))
* **webservice:** Ensure task are executed in correct order ([afb2f73](https://git.griefed.de/Griefed/ServerPackCreator/commit/afb2f73d0d27e4aaeaddbb4849e60a1b0a6f2b7d))
* Add changes from main for GitHub workflows, delete no longer needed workflows. ([03ad356](https://git.griefed.de/Griefed/ServerPackCreator/commit/03ad356f762bd66d7cc887d537542fc06187cb2b))
* Add readme-template and sponsors ci job ([5622dca](https://git.griefed.de/Griefed/ServerPackCreator/commit/5622dcaa0a32ecc40761056df461adc95ce08cce))
* Build releases for alpha and beta branches ([8643327](https://git.griefed.de/Griefed/ServerPackCreator/commit/864332713be0adb15e8cebba0d679cdcebb755af))
* Clean up and beautify ([d2ff50f](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2ff50fffc4571875724131a7b5d9cd4fbdf4521))
* Create pre-releases for alpha and beta branches ([e6729ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/e6729ea0a9f800def1c6de68c0ece7b4647ff111))
* Further restrict jobs to specific branches. Sort jobs according to purpose ([444eede](https://git.griefed.de/Griefed/ServerPackCreator/commit/444eedec770570aab80f2183a86b147cb0a6688e))
* Only run Gradle Test and Docker Test on main & master ([236c661](https://git.griefed.de/Griefed/ServerPackCreator/commit/236c661f6fa60a84f0290a295967186261ebce81))
* Re-enable arch dependant nodedisturl ([f840e31](https://git.griefed.de/Griefed/ServerPackCreator/commit/f840e31a0e2fb95457a91d2e087ee66c756973d8))
* Run correct Gradle tasks on tag mirror from GitLab to GItHub ([db6dcd0](https://git.griefed.de/Griefed/ServerPackCreator/commit/db6dcd0b245b2603b7aafea0c59cba114016a291))
* Update siouan/frontend-gradle-plugin to 5.3.0 and remove arch dependant configuration of nodeDistributionUrlPathPattern. See https://github.com/siouan/frontend-gradle-plugin/issues/165 ([1177d05](https://git.griefed.de/Griefed/ServerPackCreator/commit/1177d056934bc2b8521f214b326c16d5e069fb7a))
* **docs:** No need to run tests ([728af78](https://git.griefed.de/Griefed/ServerPackCreator/commit/728af78dc4cb6c1f93b730e7367fcefe85483365))
* **webservice:** Add temporary job for testing webservice and fix gitignore ([350582e](https://git.griefed.de/Griefed/ServerPackCreator/commit/350582e3a829d285607a2a21d10889350cab4ee8))
* **webservice:** Ensure quasar is installed before assembling frontend ([0f414ca](https://git.griefed.de/Griefed/ServerPackCreator/commit/0f414ca06487647b964bfd3e2fa3daa4244b1ecc))
* **webservice:** Fix URL for node distribution on arm ([f24663f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f24663f1c72a88444a0cb1cfd264605f59fbb5aa))
* **webservice:** Make sure arm-builds in Docker work with the frontend plugin ([2c3793c](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c3793c0b2fa838504219f4c662723db9a928df8))
* **webservice:** Make sure no cache interferes with Docker build. Install library in hopes of fixing a failure in the pipeline. ([5841007](https://git.griefed.de/Griefed/ServerPackCreator/commit/58410078abdaf7ee2bf878edac14143d73f4866b))
* **webservice:** Scan dep updates for frontend, too (I hope this works lol) ([2994d25](https://git.griefed.de/Griefed/ServerPackCreator/commit/2994d257075deeda7817fad5990d02c2d5e7f867))


### 🧪 Tests

* Don't mention what is tested. Method names already tell us that. ([e32fd53](https://git.griefed.de/Griefed/ServerPackCreator/commit/e32fd534ec2498e8326d52da83759dd5d5e7bdac))
* Fix a test regarding AddonHandler ([b737d92](https://git.griefed.de/Griefed/ServerPackCreator/commit/b737d92db767f961151cd22ca2c0227d0020fa5a))
* Split test methods. Helps pin-pointing cause of error in case of failure. ([f2d723b](https://git.griefed.de/Griefed/ServerPackCreator/commit/f2d723b2e3ebf24e9bdb86c83c35a791efa082c8))
* Ye olde I RUN FINE ON YOUR MACHINE BUT NOT ON ANOTHER NU-UUUUHHUUUU.....Sigh ([4442168](https://git.griefed.de/Griefed/ServerPackCreator/commit/444216872f3df37e7e7cb9681d3752d91eb82d17))


### 🚀 Features

* Addon functionality! This allows users to install addons to execute additional operations after a server pack was generated. See 5. in the README and the example addon at https://github.com/Griefed/ServerPackCreatorExampleAddon ([2a93e54](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a93e5476d11e84215667460997b694d30e93770))
* Pass the path where ServerPackCreator resides in to addons. Create dedicated addon-directory in work/temp, avoiding potential conflict with other addons. ([c9050b6](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9050b68ee42b4dabcde73cfb8eaf1417ab0a312))


### 🛠 Fixes

* Create additional pattern for log files as ANSI colouring frakked up the formatting for log entries in files. ([f246bf8](https://git.griefed.de/Griefed/ServerPackCreator/commit/f246bf8777d72832041c16f3f1f4fe21305ef870))
* Fix Forge installer log deletion. Forgot String.format with destination. ([1b44cb8](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b44cb8cc8022ffd7335e86823b98b7c31430e5f))
* Fix loading config not setting modloader specified in config ([cb50348](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb50348c6a4e4615db397948aefca5edabbbb83a))
* If no startup parameter is specified, assume -cli, else use the provided one. ([cad6e55](https://git.griefed.de/Griefed/ServerPackCreator/commit/cad6e55e73048003896fdde1f3e2b27ce69fa78a))
* Make sure clientMods is set correctly with no starting [ or ending ] ([c98ef0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/c98ef0e0777673a6015d738c378b3bf30edf7eff))
* Update frontend packages so it no longer throws some CSS minify errors around the block ([342e3c8](https://git.griefed.de/Griefed/ServerPackCreator/commit/342e3c895c6c090a09475d0d57a7c3d47e1238b7))


### Other

* Add GitLab templates for Service Desk ([6be793f](https://git.griefed.de/Griefed/ServerPackCreator/commit/6be793fbe24177de6d17088f9ce0371c17fd0e77))
* Add list of addons to README. Currently only the ExampleAddon I made is available. ([3367a8b](https://git.griefed.de/Griefed/ServerPackCreator/commit/3367a8bf839486c86efdb41f32caa85bcbd5a6bb))
* Label issues and pull requests made by sponsors ([95591f9](https://git.griefed.de/Griefed/ServerPackCreator/commit/95591f90bb3af101ba7571230bccf7d2a19c450a))
* README overhaul. Include guides. Update guides. Number chapters. Cleanup ([7d0d2bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d0d2bd5b2823e64a7aa20a2239699533f9dc930))
* Remove no longer needed run configurations ([7e43ee3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e43ee3e6be65d55da98c2c06a19d69abd055880))
* Rename job to better reflect what is actually happening ([4885952](https://git.griefed.de/Griefed/ServerPackCreator/commit/48859526c2c259ffb8f74f23ba83155409fe1384))
* Some more logging ([d4fa143](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4fa143125b1eeb1e8e69e020906788a2224853f))
* Update README with information from self-hosted GitLab pipeline status. Expand on deploy and versioning info. Add more Jetbrains swag. All that good stuff. ([c36ad6c](https://git.griefed.de/Griefed/ServerPackCreator/commit/c36ad6cd313c83b4b321ae768922bfd16c751f07))
* **webservice:** Add instructions on how to build SPC locally ([6e873ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/6e873ac174109b6d837de2c237d587128f5763a3))
* **webservice:** Expand readme with webservice related information ([fe5d440](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe5d440cc71a6445d211b7c3ca8ebfb0268eda6e))
* **webservice:** Properly setup manifest. Include up-to-date copies of license, readme, contributing, code of conduct, changelog in the jar. Exclude said files in backend/main/resources with gitignore. ([4812918](https://git.griefed.de/Griefed/ServerPackCreator/commit/4812918a72bf9dfdec89d4f052b1d7f173ae688c))

### [2.1.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.1.0...2.1.1) (2021-07-21)


### :scissors: Refactor

* **Modloader installer:** Cleanup, beautification, etc. ([5944e8e](https://git.griefed.de/Griefed/ServerPackCreator/commit/5944e8e4564eda5837732c0a2dd480444d4f19df))


### 🧪 Tests

* **Modloader installer:** Set Fabric loader version in test to correct version ([9920d77](https://git.griefed.de/Griefed/ServerPackCreator/commit/9920d776a749cae1044f0ee598d3c9ac2f00c0ca))


### 🛠 Fixes

* **Modloader installer:** Collect Fabric installation command into list and pass to ProcessBuilder so our command is build correctly and all parameters are passed on correctly. ([bb9d7fc](https://git.griefed.de/Griefed/ServerPackCreator/commit/bb9d7fc70bdee36cc33e54665fe7092645d2ff99))


### Other

* Remove PayPal link. I'd rather use the GitHub way. ([06635fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/06635fa5f3be38c9c9ab1789f8ae72f45f50fad0))
* **Modloader installer:** Add debug logging of the command about to be executed. ([9deb8b0](https://git.griefed.de/Griefed/ServerPackCreator/commit/9deb8b08ec1938506f60a473e678f7206db85c6f))

## [2.1.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.0.7...2.1.0) (2021-07-18)


### :scissors: Refactor

* Change copyDirs related lang keys to tell the user about file specifications. ([52df3fe](https://git.griefed.de/Griefed/ServerPackCreator/commit/52df3febc079e6fb9f4c5a1539d39b9220c93a9b))


### 🦊 CI/CD

* lolwhoops ([0ffa433](https://git.griefed.de/Griefed/ServerPackCreator/commit/0ffa433718143de67d8e44204bea45689cc73374))
* Only build javaDoc on pages workflow ([edb3ef5](https://git.griefed.de/Griefed/ServerPackCreator/commit/edb3ef527bb9e2a6eab71bcc5cd03402ccb3bcf2))
* Or not... ([7d8c644](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d8c6441382ad8b860cb5344ac4639c01f763965))
* Switch image to safe space but most importanlty to make things a little easier for me ([3616478](https://git.griefed.de/Griefed/ServerPackCreator/commit/361647840c5bda246f150760ce7d06ec72bb9b45))


### 🧪 Tests

* Fix paths ([6485051](https://git.griefed.de/Griefed/ServerPackCreator/commit/6485051360e81af9b75a70c1cbc6795edb409537))
* Fix tests...again..... ([99c092f](https://git.griefed.de/Griefed/ServerPackCreator/commit/99c092f38b58dbafb6d29f4f871132e24b8d392b))
* Revert ci image partially ([af61ef0](https://git.griefed.de/Griefed/ServerPackCreator/commit/af61ef03e208484fd9532c8395b58424a139061d))


### 🚀 Features

* Allow users to specify explicit source/file;destination/file-combinations to include in generated server pack ([2843b4c](https://git.griefed.de/Griefed/ServerPackCreator/commit/2843b4ce5bf30b2d6951ce9902e6c6e0f86434ef))


### 🛠 Fixes

* Ensure no backslashes make it into our arrays and strings. Make file specifications relative to modpack directory. ([53db427](https://git.griefed.de/Griefed/ServerPackCreator/commit/53db42779aeea429724105554960562d74198b19))


### Other

* **deps:** update dependency commons-io:commons-io to v2.11.0 ([aa1baf9](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa1baf9c8040db19533947946ab118babcba1225))

### [2.0.7](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.0.6...2.0.7) (2021-07-10)


### 🦊 CI/CD

* Explicitly set baseimage version ([674f702](https://git.griefed.de/Griefed/ServerPackCreator/commit/674f702afebfc961caff408a0af75b2de46c8c95))
* Deploy pages after every commit ([f279abb](https://git.griefed.de/Griefed/ServerPackCreator/commit/f279abb123cdb1089639782ff8905cf928ecc69a))
* Fix "breaking" type ([f80c7ba](https://git.griefed.de/Griefed/ServerPackCreator/commit/f80c7ba6aae7801b27f45bd30785a888827d4761))
* Remove branches from gh-pages workflow ([b84edb2](https://git.griefed.de/Griefed/ServerPackCreator/commit/b84edb2d765c301ba10e7114c787a4a36069eb3d))
* Try and fix pages deploy on GitLab ([8f445d9](https://git.griefed.de/Griefed/ServerPackCreator/commit/8f445d99308d811fefd7a88986924e0c2dc71b55))


### 🛠 Fixes

* Remove unnecessary installation of JDK8 ([b4896ec](https://git.griefed.de/Griefed/ServerPackCreator/commit/b4896ec7ae6dec103156701fadef1a18e3df952d))


### Other

* **deps:** update dependency com.fasterxml.jackson.core:jackson-databind to v2.12.4 ([53272cc](https://git.griefed.de/Griefed/ServerPackCreator/commit/53272cca5d88894b6682397c89c263c249d55d64))

### [2.0.6](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.0.5...2.0.6) (2021-07-03)


### 🦊 CI/CD

* Deactivate test jobs on tag creation ([e354501](https://git.griefed.de/Griefed/ServerPackCreator/commit/e354501b7d918ee52e726c995884a7f00ba75934))


### 🛠 Fixes

* Do not push to GitHub packages from GitLab CI. We've got a separate GitHub workflow for mirroring and creating releases on GitHub. ([5e0a819](https://git.griefed.de/Griefed/ServerPackCreator/commit/5e0a819ef9359ddfa284401469366a3e263b54bb))

### [2.0.5](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.0.4...2.0.5) (2021-07-03)


### 🦊 CI/CD

* Artifacts aren't attached to the lreease this way anyway. ([640e0ed](https://git.griefed.de/Griefed/ServerPackCreator/commit/640e0ed154a28d5853255be9102ade9154963b27))


### 🛠 Fixes

* Fix releases on GitHub and GitLab. Release packages on GitLab to Package Registry and attach package to release. ([66b0bb8](https://git.griefed.de/Griefed/ServerPackCreator/commit/66b0bb8bab7938fc62400764912f2933bb703f16))
* Move back to de.griefed. Last time, promise. ([80919a4](https://git.griefed.de/Griefed/ServerPackCreator/commit/80919a44fea77a685ae355c6aa2dbb3a7042430c))

### [2.0.4](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.0.3...2.0.4) (2021-07-03)


### 🦊 CI/CD

* **deps:** Bump commons-io from 2.9.0 to 2.10.0. Bump mockito-core from 3.11.0 to 3.11.1. ([915f6bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/915f6bdf54bd3d5eb2c08cd1f6b77ff55ea3bfc9))
* Add major release workflow ([9e79000](https://git.griefed.de/Griefed/ServerPackCreator/commit/9e79000fa75f0a5eef7b66eb6a7a56c1cdb9b31a))
* Add minor release workflow ([3386cf4](https://git.griefed.de/Griefed/ServerPackCreator/commit/3386cf49a2a18e21e2f4fb8e206a67e55681d496))
* Add patch workflow ([aabbd64](https://git.griefed.de/Griefed/ServerPackCreator/commit/aabbd64865879c19b04b02da4727c3155aa884b3))
* Add Pre-Release major workflow ([4ee8394](https://git.griefed.de/Griefed/ServerPackCreator/commit/4ee839460b76cb272d68442e0192a4a37ed16bad))
* Add Pre-Release minor workflow ([7be2694](https://git.griefed.de/Griefed/ServerPackCreator/commit/7be269466e594c91d377319226adfc1491e1cbd1))
* Add Pre-Release patch workflow ([6a2aafb](https://git.griefed.de/Griefed/ServerPackCreator/commit/6a2aafb00e71f7c3e58007c8b26d89019c55e5b8))
* Add semver release config ([e940117](https://git.griefed.de/Griefed/ServerPackCreator/commit/e940117ddbb9f99856d04d1c3bd9da72d1c2d9ef))
* Add upload to generic packages and create asset links for release ([c62de70](https://git.griefed.de/Griefed/ServerPackCreator/commit/c62de70845c9d4a5b1182a68de6e74368c92ec3d))
* Create release on GitHub after tag mirror from GitLab ([b38de38](https://git.griefed.de/Griefed/ServerPackCreator/commit/b38de38a9a04804da3d4e0c6977649280b3999c3))
* Ensure GitHub and GitLab have same tags ([fc14baa](https://git.griefed.de/Griefed/ServerPackCreator/commit/fc14baa89b8575a9e6fe1e0bbf70f7cffbbd2381))
* Fix branches release is supposed to run on ([132f480](https://git.griefed.de/Griefed/ServerPackCreator/commit/132f480f03a5c4f36ad91b5260fa15a0fab8a80a))
* Fix links and build jobs ([e7beb7f](https://git.griefed.de/Griefed/ServerPackCreator/commit/e7beb7f554088cba6de3c7f05510f1326c71ec47))
* Move Docker build for GitHub container registry to GitLab. Automatically update javadocs on GitHub pages. ([fba6fee](https://git.griefed.de/Griefed/ServerPackCreator/commit/fba6feea7d60de14a64076b58fb6e137f390d1b8))
* Move to serverpackcreator group ([5e87b7d](https://git.griefed.de/Griefed/ServerPackCreator/commit/5e87b7d07fba9a6157eb7ffba0e941ea6150dbe6))
* Move to serverpackcreator group ([19d6db8](https://git.griefed.de/Griefed/ServerPackCreator/commit/19d6db8ddcd77577a70d0e31fd16c4699d7d46b4))
* print some extra information ([b685a4a](https://git.griefed.de/Griefed/ServerPackCreator/commit/b685a4afbefe586391fd43133dd95aff327600f1))
* print some extra information ([3c83676](https://git.griefed.de/Griefed/ServerPackCreator/commit/3c83676efc02e464caa6854355bf7d31e5ab20c6))
* Remove no longer needed folder as we now have docs automatically being updated on GitLab and GitHub pages ([492aca2](https://git.griefed.de/Griefed/ServerPackCreator/commit/492aca2f7b15fd5a3c69023128069c7a0c14201b))
* Remove no longer needed workflow ([92b18a3](https://git.griefed.de/Griefed/ServerPackCreator/commit/92b18a33e910cd3e93d7c65e0dd3ea3395abc5ae))
* Replaced by RenovateBot in GitLab ([5d951ae](https://git.griefed.de/Griefed/ServerPackCreator/commit/5d951aecd3ecbe80e210f6ef9c7f7dbc6fb780f2))
* Set type to package. Set tag related to asset link. ([3d2e26d](https://git.griefed.de/Griefed/ServerPackCreator/commit/3d2e26d84e8a4029037c9c1e670c71f9fddbc3d9))


### 🧪 Tests

* CI/CD for ServerPackCreator on GitLab. Fingers crossed. ([1f5cab6](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f5cab659a39d890235e998927519c06a2f758b2))
* Test own JDK8 baseimage for CI/CD ([df403b6](https://git.griefed.de/Griefed/ServerPackCreator/commit/df403b6d37460a9d0a9f710bb8b3884b19716cb0))


### 🛠 Fixes

* Fix typo in FALLBACKMODSLIST ([9119259](https://git.griefed.de/Griefed/ServerPackCreator/commit/91192596c6ce0d939087f62d1ca3d88d6909e0c0))


### Other

* Enable gradle-lite in RenovateBot ([836d83d](https://git.griefed.de/Griefed/ServerPackCreator/commit/836d83da757a6ff426821bc699b81d2014009e3a))
* Expand gitignore with some test-generated files ([3a05392](https://git.griefed.de/Griefed/ServerPackCreator/commit/3a053929c499bd4bf967252a44649a1bf7d9d395))
* Fix JDK path after moving to AdoptJDK ([8d6ae2b](https://git.griefed.de/Griefed/ServerPackCreator/commit/8d6ae2b328f12cb908cc4c3016cedc5c49d07c9e))
* Fix local JDK ([a5f9370](https://git.griefed.de/Griefed/ServerPackCreator/commit/a5f9370ac96532d304bb70aa22430b2ba86e0ee7))
* Inform users/visitors about move to GitLab ([94d657a](https://git.griefed.de/Griefed/ServerPackCreator/commit/94d657a3411797e61630195cd1baa83f431b52f8))
* Issue templates for GitLab ([495a537](https://git.griefed.de/Griefed/ServerPackCreator/commit/495a5379b0af5967480a9f02d507c00930a16186))
* Switch dependency formatting in hopes of RenovateBot detecting them then ([7891c94](https://git.griefed.de/Griefed/ServerPackCreator/commit/7891c94a197e91444ac2d0c32ba0a6a1e00e5be4))
* Update blog url ([efbe33c](https://git.griefed.de/Griefed/ServerPackCreator/commit/efbe33c1b44d1e3ff179fe1a10b48c087f2e2756))
* Update README badges ([9cc6d55](https://git.griefed.de/Griefed/ServerPackCreator/commit/9cc6d5557568ea8415e30be3033115c3ec7cfb6e))
* WSL and Docker is a hassle. ([4fb6378](https://git.griefed.de/Griefed/ServerPackCreator/commit/4fb63783826bb6a597d800d56951305d6f189138))
* **deps:** Bump commons-io from 2.8.0 to 2.9.0 ([26d481e](https://git.griefed.de/Griefed/ServerPackCreator/commit/26d481ed67d3a6162c8e659d2c813089f8b5c95e))
* **deps:** Bump commons-io from 2.8.0 to 2.9.0 ([7855cdc](https://git.griefed.de/Griefed/ServerPackCreator/commit/7855cdc1d9b425fd39490cbd363831e87a213e30))
* **deps:** Bump mockito-core from 3.10.0 to 3.11.0 ([6f62139](https://git.griefed.de/Griefed/ServerPackCreator/commit/6f62139a261ea06ab8ae584d1e5fd3004a71a891))
* **deps:** Bump zip4j from 2.7.0 to 2.8.0 ([0352be8](https://git.griefed.de/Griefed/ServerPackCreator/commit/0352be80a3928060592127930bfe513654b1e6b7))
* **deps:** Bump zip4j from 2.7.0 to 2.8.0 ([2626bb7](https://git.griefed.de/Griefed/ServerPackCreator/commit/2626bb776283370f64eb0132fdabacf6f41e6c44))
* **deps:** update dependency gradle to v7.1.0 ([784f90f](https://git.griefed.de/Griefed/ServerPackCreator/commit/784f90fcde123cb047cc11cad8e6bbc8d68beb4b))
* **deps:** update dependency gradle to v7.1.1 ([5bf6f94](https://git.griefed.de/Griefed/ServerPackCreator/commit/5bf6f9467783ac11b6af1b0ff8edff22319fb893))
* **deps:** update dependency net.lingala.zip4j:zip4j to v2.9.0 ([d398ddb](https://git.griefed.de/Griefed/ServerPackCreator/commit/d398ddbf74db870a416926303d1f0e100a5789b6))
* **deps:** update dependency org.mockito:mockito-core to v3.11.2 ([177e0e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/177e0e08456caee50fb4c56e96a8efbb3a683149))
* **deps:** update dependency org.slf4j:slf4j-log4j12 to v2.0.0-alpha2 ([5fa1789](https://git.griefed.de/Griefed/ServerPackCreator/commit/5fa1789f78dab6628a8a0da6bbee8082265be6a1))
* **deps:** update lsiobase/alpine docker tag to v3.14 ([b948a93](https://git.griefed.de/Griefed/ServerPackCreator/commit/b948a9310eb0ba22aaaac642961d5e378332f319))
* **deps:** update openjdk docker tag to v8 ([47eb9da](https://git.griefed.de/Griefed/ServerPackCreator/commit/47eb9dafd26226349c6e1ffda566e205b7c40d4a))
