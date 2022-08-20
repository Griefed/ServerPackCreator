## [3.12.0-beta.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.11.1...3.12.0-beta.1) (2022-08-14)


### :scissors: Refactor

* **Start:** When running as webservice, read serverpackcreator.properties, too. Re-order assignment for ARGS. ([6052855](https://git.griefed.de/Griefed/ServerPackCreator/commit/6052855a3cff5165ce3369bffa3a6e032f1b3f88))


### 💎 Improvements

* **Minecraft snapshot and pre-release versions:** Opt-in for listing Minecraft pre-release and snapshot versions via `de.griefed.serverpackcreator.minecraft.snapshots` ([9858322](https://git.griefed.de/Griefed/ServerPackCreator/commit/9858322b5030d18073e361ac51dccc29eb5fd7dd))
* **User-specified clientside-only mod exclusion filters:** Allow users to change the way SPC filters user-specified clientside-only mods by introducting an additional property de.griefed.serverpackcreator.serverpack.autodiscovery.filter ([eb024f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/eb024f45dedcad431868a12c31d690408975fc52))


### 🛠 Fixes

* **ApplicationProperties:** Remove unnecessary and conflicting declaration ([3b16aa4](https://git.griefed.de/Griefed/ServerPackCreator/commit/3b16aa4eb0044b259ff1e14c030ca67eb366777b))

## [3.11.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.11.0...3.11.1) (2022-08-12)


### 🦊 CI/CD

* **GitLab:** Move maven package upload to GitLab to last position ([7c12da6](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c12da654042908c8825aae98d289e16bd92d06d))


### 🛠 Fixes

* **Logging:** Code formatting broke the log4j2.xml. ([01b5d62](https://git.griefed.de/Griefed/ServerPackCreator/commit/01b5d625a0ed89d9149d40a0747b9f7b44e62fa1))

## [3.11.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.10.2...3.11.0) (2022-08-12)


### :scissors: Refactor

* **ConfigurationHandler:** Refactor isZip to improve readibility. Move Json util methods to own utilities class. ([a31376c](https://git.griefed.de/Griefed/ServerPackCreator/commit/a31376c114caddb1a545841d2904a4d4a5107189))


### 💎 Improvements

* **CLI Configuration Editor:** Create, load, edit, check configurations in CLI mode `-cli` or `-cgen` ([cff423c](https://git.griefed.de/Griefed/ServerPackCreator/commit/cff423c7bdfef1e88eacceed049b6b9e29377123))

## [3.10.2](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.10.1...3.10.2) (2022-08-06)


### :scissors: Refactor

* **ModScanners:** Improve readability of Json-Scanners ([d70de25](https://git.griefed.de/Griefed/ServerPackCreator/commit/d70de25c6b3ecf21bbbda14ef887dd18da4d1878))


### 🦊 CI/CD

* Add scheduled job for automated package updates in Docker container, should I ever decide to use it. ([1ac652b](https://git.griefed.de/Griefed/ServerPackCreator/commit/1ac652bf90ee4b1e0d5ab8201898e277702f5708))


### 🛠 Fixes

* **deps:** update dependency io.github.vincenzopalazzo:material-ui-swing to v1.1.3 ([1d1acec](https://git.griefed.de/Griefed/ServerPackCreator/commit/1d1acecfdd68ab03266bd29c5deef64d14a6f503))
* **deps:** update dependency org.jgroups:jgroups to v5.2.4.final ([99ebc98](https://git.griefed.de/Griefed/ServerPackCreator/commit/99ebc98ed7971520f6d0363b9be0ec4492ccde30))
* **deps:** update dependency org.xerial:sqlite-jdbc to v3.39.2.0 ([060eb8d](https://git.griefed.de/Griefed/ServerPackCreator/commit/060eb8d869e5331761a42331ac0e8beef91d90ce))
* **deps:** update junit5 monorepo ([8cd8008](https://git.griefed.de/Griefed/ServerPackCreator/commit/8cd8008773895c414ebf501ba508051ef87cd3a5))


### Other

* **Clientside Modslist:** Add Blur- to the list. ([45f3010](https://git.griefed.de/Griefed/ServerPackCreator/commit/45f3010194117b913fd1bb4cd2494b0cda75b490))
* **deps:** update dependency @quasar/extras to v1.15.1 ([99660d8](https://git.griefed.de/Griefed/ServerPackCreator/commit/99660d84d52aa0a06cfcda6d2aec7236bd8f4f8f))
* **deps:** update dependency core-js to v3.24.1 ([37a1676](https://git.griefed.de/Griefed/ServerPackCreator/commit/37a16763e53b7ae8c85789969c0c6d779d16cf16))
* **deps:** update dependency eslint to v8.21.0 ([6509275](https://git.griefed.de/Griefed/ServerPackCreator/commit/650927550f85272e71d88a2d88b8d31f78224e59))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.12 ([59b69f3](https://git.griefed.de/Griefed/ServerPackCreator/commit/59b69f3acb54a2df6da85198049936e453b819dd))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.9 ([843a97d](https://git.griefed.de/Griefed/ServerPackCreator/commit/843a97d27b8f4d9960a798e0b47e7acd55757f53))
* **deps:** update dependency gradle to v7.5 ([d4eb3c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4eb3c2705682682a318033b85f231d9cbf9c77d))
* **deps:** update dependency jetbrains/qodana-jvm-community to v2022.2 ([6b32e2d](https://git.griefed.de/Griefed/ServerPackCreator/commit/6b32e2d07117f96e0a3f61d20ae6b7455e23cd28))
* **deps:** update dependency quasar to v2.7.7 ([d377bc3](https://git.griefed.de/Griefed/ServerPackCreator/commit/d377bc3e5f0d8b9bb399075cf9b572a4536e2055))

## [3.10.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.10.0...3.10.1) (2022-07-25)


### :scissors: Refactor

* **BufferedImage initialization:** Ensure we have the buffered image, or throw ([9f0b467](https://git.griefed.de/Griefed/ServerPackCreator/commit/9f0b46788e54a4074cdb70ea53a122f04ccaefc4))
* **Clientside mod exclusion logging:** Improve logging of excluded mods and checks thereof. ([89c37b3](https://git.griefed.de/Griefed/ServerPackCreator/commit/89c37b3ae737abc2290b72ed03d09a19dbfbb5fd))
* **Mods File List:** Prevent NPE by properly initializing filesInModsDir ([75a0094](https://git.griefed.de/Griefed/ServerPackCreator/commit/75a0094565059f8c63ab11bdef07f747994f32b6))
* **ModScanning:** Remove unnecessary checks ([c72daee](https://git.griefed.de/Griefed/ServerPackCreator/commit/c72daee2a5f0c0ffc4fc6825570efe7e4fd18bfb))


### 📔 Docs

* **ConfigUtilities:** Add missing deprecated annotation ([3083521](https://git.griefed.de/Griefed/ServerPackCreator/commit/3083521712e60afc79a168c3690fc9cd49f98df1))


### 🛠 Fixes

* **File and Directory exclusions:** Fix files and/or directories not excluding files and/or directories correctly. ([ed17ad3](https://git.griefed.de/Griefed/ServerPackCreator/commit/ed17ad3bb8714fb9d94d0c0447f0b831c550b035))

## [3.10.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.9.0...3.10.0) (2022-07-24)


### :scissors: Refactor

* **Application Plugins:** Indent listing of installed addons/plugins and add example plugins for testing ([fbd0c75](https://git.griefed.de/Griefed/ServerPackCreator/commit/fbd0c7550713ba7b7c3351572a577e8dd0f5ffc7))
* **Housekeeping:** Much needed refactorings, cleanups, documentations etc. ([3237acd](https://git.griefed.de/Griefed/ServerPackCreator/commit/3237acdea9527b4a114487abcd8b195a730f722b))
* **i18n:** Always create jarResources, preventing unnecessary calls and checks ([5ae4a19](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ae4a19b782a451fa08424c74b73d78cc6f5283c))
* **Lists:** Replace Lists with TreeSets where sensible ([bc0159f](https://git.griefed.de/Griefed/ServerPackCreator/commit/bc0159f56835537a97a9451b9c07a0c0df4d8e97))


### 💎 Improvements

* **Copy-Dir Checks:** Only check copy-directory entries when the modpack-directory is valid. ([d5f0fee](https://git.griefed.de/Griefed/ServerPackCreator/commit/d5f0fee7d6d4b5280a346c6a2c3ebcabcabd2d5a))


### 🦊 CI/CD

* **Coverage Report:** Disable coverage job ([f533473](https://git.griefed.de/Griefed/ServerPackCreator/commit/f5334731a03a34c77a7f196d4edf721786009848))


### 🧪 Tests

* **Cleanup:** Remove unnecessary tests. ([02ed5f3](https://git.griefed.de/Griefed/ServerPackCreator/commit/02ed5f330d1f5476908fdb8ada2d284a75a286c4))


### 🛠 Fixes

* **Illegal Characters Check:** Remove ' from check for illegal characters. It CAN be used in filenames and paths. The more you know, eh? ([c6eaa49](https://git.griefed.de/Griefed/ServerPackCreator/commit/c6eaa4912a0d048e4e8bd53b2a9bc3a35067eaf4))
* **Toml Sideness Scanning:** Correctly scan mods.toml in newer Forge mods for their sideness, prevent false-positives. ([f85dca3](https://git.griefed.de/Griefed/ServerPackCreator/commit/f85dca3db05a0b6c6e1239c7576f84ce1117379f))
* **Toml Sideness Scanning:** Correctly scan mods.toml in newer Forge mods for their sideness, prevent false-positives. ([eb7a341](https://git.griefed.de/Griefed/ServerPackCreator/commit/eb7a34125ffb9c2c2b59ab478c8173123d8246a3))


### Other

* **Fallback Modslist:** Update fallback modslist with clientside-only mods kindly gathered by BisectHosting and @AzureDoom. ([4b766d7](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b766d71469c27b1fce8908fa23f303b727c010a))
* **i18n cleanups:** Small cleanup and logging improvement. ([0898495](https://git.griefed.de/Griefed/ServerPackCreator/commit/089849506d45edffad95e1bddf3df68ce60f54c2))
* **i18n cleanups:** Small cleanup and logging improvement. ([4775ba6](https://git.griefed.de/Griefed/ServerPackCreator/commit/4775ba6b7c40a510b1c9c050023fce7605ab736b))

## [3.9.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.8.0...3.9.0) (2022-07-16)


### 💎 Improvements

* **Compatibility checks:** Simple check whether the specified Minecraft version and Fabric/Quilt versions are compatible with each other and available. Enhance VersionMeta for Fabric, allowing for more checks in back- and frontend. The GUI will now only allow valid Minecraft and Fabric/Quilt combinations and present you with an error if you manage to pass an invalid combination. ([8c6324b](https://git.griefed.de/Griefed/ServerPackCreator/commit/8c6324b08778ce62856a184000adacb8635589b3))
* **Manifest scans:** Scan ATLauncher manifest and improve all manifest scans for better modloader and version detection. Detect Quilt where possible. ([ac6f799](https://git.griefed.de/Griefed/ServerPackCreator/commit/ac6f799376eccc3d6dc9713c3fe24cc6ed0d254f))
* **ZIP-archive checks:** Check the validity of a ZIP-archive and improve the listing of files and directories in a given ZIP-archive. ([fd0621d](https://git.griefed.de/Griefed/ServerPackCreator/commit/fd0621d4e0da121cd79d7770d5d4b05a029b56d6))

## [3.8.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.7.0...3.8.0) (2022-07-11)


### 💎 Improvements

* **Start scripts:** Ensure we stay in the batch-scripts containing directory, even when someone decides to run with administrator privileges ([621bf18](https://git.griefed.de/Griefed/ServerPackCreator/commit/621bf18f49fd079b613a51486d1b72e7dc421414))


### 🚀 Features

* **Start scripts:** Templating! Create start scripts from customizable templates ([26e2452](https://git.griefed.de/Griefed/ServerPackCreator/commit/26e245266e51234199bd88d54bbe674820357ff7))


### Other

* **deps-dev:** bump @babel/eslint-parser in /frontend ([1139c56](https://git.griefed.de/Griefed/ServerPackCreator/commit/1139c56060fcc19e492f3d157e0948143270d3a9))
* **deps-dev:** bump @types/node from 17.0.24 to 18.0.3 in /frontend ([37af17d](https://git.griefed.de/Griefed/ServerPackCreator/commit/37af17de9727614eed64c7e7b51fec7df86224a6))
* **deps-dev:** bump eslint from 8.14.0 to 8.18.0 in /frontend ([2242843](https://git.griefed.de/Griefed/ServerPackCreator/commit/2242843dd25e516222825269546b22c120feb031))
* **deps-dev:** bump eslint from 8.18.0 to 8.19.0 in /frontend ([e4a6147](https://git.griefed.de/Griefed/ServerPackCreator/commit/e4a61479c85fe9050d04299ce80def6f1eb6fbc7))
* **deps-dev:** bump eslint-plugin-vue from 8.7.1 to 9.2.0 in /frontend ([700f9b8](https://git.griefed.de/Griefed/ServerPackCreator/commit/700f9b8613ac5c49d4bf9d07ccfcbc909a1613e3))
* **deps:** bump @quasar/extras from 1.13.6 to 1.14.1 in /frontend ([a1d247e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a1d247e5cc43f07ff1a87510591206bfb0c75e47))
* **deps:** bump core-js from 3.22.8 to 3.23.4 in /frontend ([8d01f28](https://git.griefed.de/Griefed/ServerPackCreator/commit/8d01f28c92ccc6df54dae49debd3013954224ad0))
* **deps:** bump docker/build-push-action from 2 to 3 ([dfd9e29](https://git.griefed.de/Griefed/ServerPackCreator/commit/dfd9e291eac7a6a0d31e035d86ce22ed0aede14d))
* **deps:** bump docker/setup-buildx-action from 1 to 2 ([97715c1](https://git.griefed.de/Griefed/ServerPackCreator/commit/97715c159b6115090a0fdd4c5bb5d907f2c4dca1))
* **deps:** bump docker/setup-qemu-action from 1 to 2 ([0fc9735](https://git.griefed.de/Griefed/ServerPackCreator/commit/0fc97352255dc0e7385adaa5ce10950c1b200942))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.10 to 2.0.11 ([16b9fa0](https://git.griefed.de/Griefed/ServerPackCreator/commit/16b9fa054d1a7e39007008a7383b09977f025795))
* **deps:** bump io.spring.dependency-management ([6018b76](https://git.griefed.de/Griefed/ServerPackCreator/commit/6018b76c78f570b637ed566c3bea9ce0f5169945))
* **deps:** bump JamesIves/github-pages-deploy-action ([e909c69](https://git.griefed.de/Griefed/ServerPackCreator/commit/e909c69d2184c5b0f96eff4fad67a66121b5e26b))
* **deps:** bump log4j-core from 2.17.2 to 2.18.0 ([aaae449](https://git.griefed.de/Griefed/ServerPackCreator/commit/aaae449a31fe5e40f9f3d0f4c92f4bb0812dd0b9))
* **deps:** bump log4j-jul from 2.17.2 to 2.18.0 ([2b635b0](https://git.griefed.de/Griefed/ServerPackCreator/commit/2b635b0830fb5c004b53da702c9a5bf7ce1c726e))
* **deps:** bump log4j-web from 2.17.2 to 2.18.0 ([691be8c](https://git.griefed.de/Griefed/ServerPackCreator/commit/691be8c0e8a58f44813f949b900ce0c59b0ab73c))
* **deps:** bump org.springframework.boot from 2.6.7 to 2.7.1 ([3231370](https://git.griefed.de/Griefed/ServerPackCreator/commit/323137080d933fcd633cc3c48a43f9e9189596b0))
* **deps:** bump quasar from 2.6.6 to 2.7.3 in /frontend ([c88465a](https://git.griefed.de/Griefed/ServerPackCreator/commit/c88465a9480201df14f0c0e707e2282808db7665))
* **deps:** bump spring-boot-starter-artemis from 2.7.0 to 2.7.1 ([b8d39d3](https://git.griefed.de/Griefed/ServerPackCreator/commit/b8d39d3d4b87faab93efb9953f4986ef48f11cb7))
* **deps:** bump spring-boot-starter-data-jpa from 2.7.0 to 2.7.1 ([224e03a](https://git.griefed.de/Griefed/ServerPackCreator/commit/224e03a5e0833cc31a1421fe31964e59b24b2f52))
* **deps:** bump spring-boot-starter-web from 2.7.0 to 2.7.1 ([944bd33](https://git.griefed.de/Griefed/ServerPackCreator/commit/944bd339341563adb6efdb491c12db87e94c5f66))
* **deps:** bump vue from 3.2.33 to 3.2.37 in /frontend ([d589fff](https://git.griefed.de/Griefed/ServerPackCreator/commit/d589fff10e1e0a00246ba91c868bdb055d3a6935))
* **deps:** bump zip4j from 2.10.0 to 2.11.1 ([fc84982](https://git.griefed.de/Griefed/ServerPackCreator/commit/fc849821a77bd9eb7ae9ca54059a4ff25d6e8e23))
* **deps:** update dependency @quasar/extras to v1.14.2 ([12228dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/12228dcee34ec32482de385ab54247a273302b3f))
* **deps:** update dependency core-js to v3.23.3 ([6bdaf51](https://git.griefed.de/Griefed/ServerPackCreator/commit/6bdaf5140372e423a0a20501b755f3cc2e2b7451))
* **deps:** update dependency eslint to v8.19.0 ([9b9378d](https://git.griefed.de/Griefed/ServerPackCreator/commit/9b9378de759e93e68a967a9b282d5ccc227332b3))
* **deps:** update dependency eslint-plugin-vue to v9 ([f328a06](https://git.griefed.de/Griefed/ServerPackCreator/commit/f328a06de16d50b27d997d769006b83d7ee22d43))
* **deps:** update dependency eslint-webpack-plugin to v3.2.0 ([75322cf](https://git.griefed.de/Griefed/ServerPackCreator/commit/75322cf8cfe60925bfb9c57226fbed501c675f31))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.11 ([6292f3f](https://git.griefed.de/Griefed/ServerPackCreator/commit/6292f3ffaa61f0a9d96b304f6fc8405061e9ff00))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.8 ([1c40cc0](https://git.griefed.de/Griefed/ServerPackCreator/commit/1c40cc008259a5de9376582dee3d2e4cba306141))
* **deps:** update dependency org.apache.logging.log4j:log4j-api to v2.18.0 ([d1157e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/d1157e42507d451ade6cdda84e35366290f0117e))
* **deps:** update dependency org.apache.logging.log4j:log4j-core to v2.18.0 ([d2f24e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2f24e1d696e6393499a1a5962aa9f955382478c))
* **deps:** update dependency org.apache.logging.log4j:log4j-jul to v2.18.0 ([0166133](https://git.griefed.de/Griefed/ServerPackCreator/commit/0166133e9b70cdfe5b82ae2545f8915ed7686074))
* **deps:** update dependency org.apache.logging.log4j:log4j-slf4j-impl to v2.18.0 ([c46d93c](https://git.griefed.de/Griefed/ServerPackCreator/commit/c46d93c4b7c1ce9e18a2ff961ac044b844987617))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.18.0 ([455a181](https://git.griefed.de/Griefed/ServerPackCreator/commit/455a18133ead8a31eeac380a334f5dd038ff9b4a))
* **deps:** update dependency org.pf4j:pf4j to v3.7.0 ([a10228b](https://git.griefed.de/Griefed/ServerPackCreator/commit/a10228bba4455870620521b45a406e4ff0db9ddd))
* **deps:** update dependency quasar to v2.7.4 ([8040ae5](https://git.griefed.de/Griefed/ServerPackCreator/commit/8040ae5bc8ab3d7f2f4862dc8d993ea155069e7f))
* **deps:** update dependency quasar to v2.7.5 ([ae5d48f](https://git.griefed.de/Griefed/ServerPackCreator/commit/ae5d48f08ffbff5b26f4df7d743757c07e5fa8a6))
* **deps:** update dependency vue to v3.2.37 ([018c907](https://git.griefed.de/Griefed/ServerPackCreator/commit/018c9079e0a1f9e62a44114cc69e9342a5c2c5e8))
* **deps:** update plugin io.spring.dependency-management to v1.0.12.release ([540ee71](https://git.griefed.de/Griefed/ServerPackCreator/commit/540ee711e5e6acaeaf49c3af0bb54efde2ea8909))
* **deps:** update spring boot to v2.7.1 ([7f9172d](https://git.griefed.de/Griefed/ServerPackCreator/commit/7f9172d3d10a8a1808bcdd8d1642b30f0b1a2297))

## [3.7.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.6.0...3.7.0) (2022-06-29)


### 💎 Improvements

* **Start scripts:** Various improvements, additions and fixes to the start scripts ([204ac6e](https://git.griefed.de/Griefed/ServerPackCreator/commit/204ac6e32532143ccb550b1c67d020176d8a3365))


### 🦊 CI/CD

* **Maven publishing:** Only publish maven artifacts when a beta or a full release was published ([1323088](https://git.griefed.de/Griefed/ServerPackCreator/commit/1323088fff61033c427b9dc948e7a5c4ea64af1f))

## [3.6.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.1...3.6.0) (2022-06-26)


### :scissors: Refactor

* **Aikars Flags:** Move Aikars flags to property in serverpackcreator.properties ([bbd34fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbd34fa3447aa68b254b1ee3ea67d3ce53207281))
* **ApplicationProperties:** Call reloading from constructor to reduce duplicate code and increase maintainability. ([91b2ee7](https://git.griefed.de/Griefed/ServerPackCreator/commit/91b2ee789b4859077829b057c81955139df32130))
* **GUI-API separation:** Work towards separating the GUI from the API. ([3f17c7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/3f17c7e526e28f6321e96a59ad823dea2386d511))
* **Startup:** Rearrange a couple of startup methods. Get rid of the Main-class by moving/merging into ServerPackCreator.class. ([1e02b2e](https://git.griefed.de/Griefed/ServerPackCreator/commit/1e02b2e413925a1a9469416e99f03cb9f9556401))


### 👀 Reverts

* **Tabbed Pane opaque call:** Call TABBED_PANE.setOaque(..) not from the constructor, but from createAndShowGUI() to prevent transparent tab-bar. ([9e402ed](https://git.griefed.de/Griefed/ServerPackCreator/commit/9e402eda358007b37aa579925ce0037dd8409b34))


### 💎 Improvements

* **File exclusion in ZIP-archives:** Make file exclusion opt-out and allow customizing of files to exclude with some basic filter `MINECRAFT_VERSION`, `MODLOADER`, `MODLOADER_VERSION` will be replaced with their respective values. ([ef26008](https://git.griefed.de/Griefed/ServerPackCreator/commit/ef260086e75290570ab84263dc803c718c983e0e))


### 📔 Docs

* **zipBuilder params:** Add missing params for modloader and modloader version. ([c14fd45](https://git.griefed.de/Griefed/ServerPackCreator/commit/c14fd4520c0db3d65df7668b7df09dfdcad3b669))


### 🛠 Fixes

* **Config loading/saving & Application Properties:** Improve config loading and saving. Fix the directories to include in server pack setting by loading the correct settings with from a corrected property. Delete you `serverpackcreator.properties`-file to generate a new one. ([8eec4ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/8eec4ac6ec41a1383cd3798ef7b9495ad18ff5d4))

### [3.5.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0...3.5.1) (2022-06-24)


### 🛠 Fixes

* **Start Scripts:** Correctly read the users answer to whether they agree to Mojang's EULA ([f2a82b3](https://git.griefed.de/Griefed/ServerPackCreator/commit/f2a82b3a35455893e8f01539e5f1cc12cd57ac29))

## [3.5.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.4.1...3.5.0) (2022-06-21)


### :scissors: Refactor

* **Addongs log tab:** Remove not needed fields and constructor params ([fe5d633](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe5d633f5117db7e6e4874be10914f8d5c27932c))
* **Addongs log tab:** Remove not needed fields and constructor params. Display full logs without truncating. ([1504a8e](https://git.griefed.de/Griefed/ServerPackCreator/commit/1504a8e57d78f59f11da37574d01a39ee60f4e06))
* **Code Quality & Style:** Several code quality and style changes. ([486ea43](https://git.griefed.de/Griefed/ServerPackCreator/commit/486ea43f71fe404aa6e561365a67c693d1ff8f2c))
* **Code Quality & Style:** Several code quality and style changes. ([75b1ad7](https://git.griefed.de/Griefed/ServerPackCreator/commit/75b1ad77f841d83d450794bb1ad4ed75763bca14))
* **Code Quality & Style:** Several code quality and style changes. ([bd9c0f6](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd9c0f6a1cb8e6bf44ada3bcefbf4c492ccd33dc))
* **Code Quality & Style:** Several code quality and style changes. ([0def7aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/0def7aa74f73f3932dc216de0cbe00e9b0e3d0af))
* **Enums:** Change the way enums are used in the CommandlineParser and VersionMeta. Remove / use values where appropriate. ([fea95a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/fea95a27fac3ae6180d3725384a260361b1820b2))
* **File copying:** Gather a list of all files to be copied to the server pack and THEN copy them. Improves readibility of the copyFiles(...)-method. Delete your `lang/lang_en_us.properties`-file to receive the new text. ([640ba07](https://git.griefed.de/Griefed/ServerPackCreator/commit/640ba071cef5ce19ca9fde76b4ac0d3210e80f20))
* **GUI init:** Move fields to constructor ([1a1304f](https://git.griefed.de/Griefed/ServerPackCreator/commit/1a1304f2ee56b6c1b0a6041ede797f3fd8df81ab))
* **Log Tail Component:** Always display horizontal scrollbar. ([fa20df4](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa20df4e369d4e6d2a69b816f931e0de8cbb8342))
* **ServerPackModel params:** Remove unsudes params from constructor. ([a341f83](https://git.griefed.de/Griefed/ServerPackCreator/commit/a341f83859c00f6ba397eb7a6289113cf172d0b4))
* **UNIX symlinks and Windows lnks:** Sanitize links at beginning of config check and resolve any and all links before checks are run. ([2a9ea36](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a9ea364f16c56fe9ddf266eef2d71f8f846f2c7))


### ⏩ Performance

* **FileWatcher setup:** Setup FileWatcher off-thread when running GUI, to improve startup-time of ServerPackCreator. ([087bed5](https://git.griefed.de/Griefed/ServerPackCreator/commit/087bed58fb3a255bb544dbee6bab4491d05c2a62))
* **GUI:** Various little improvements towards initialization and setup of the GUI ([8112bcb](https://git.griefed.de/Griefed/ServerPackCreator/commit/8112bcbff3987526d8b1659c0548fe5e7ee1e517))


### 👀 Reverts

* **CurseForge:** Completely remove CurseForge functionality from ServerPackCreator. ([d3de549](https://git.griefed.de/Griefed/ServerPackCreator/commit/d3de549c3c0d420ccf6d47d2c477bf8672e3687a))
* **CurseForge:** Remove mentions of CurseForge from i18n ([d876360](https://git.griefed.de/Griefed/ServerPackCreator/commit/d876360860e9eb4b75ce7f856161a9aed41a0378))
* **Modrinth:** Remove Modrinth preperations from backend and tab from frontend ([eeafa78](https://git.griefed.de/Griefed/ServerPackCreator/commit/eeafa78e90b1468687e67cbdf5cfe168dd0747e4))


### 💈 Style

* **Formatting:** Let IntelliJ IDEA reformat code and optimize imports. ([a019a55](https://git.griefed.de/Griefed/ServerPackCreator/commit/a019a551b3bafb74cd4a41666c70bbcf852ccb28))
* **Formatting:** Let IntelliJ IDEA reformat code and optimize imports. ([dfdc91b](https://git.griefed.de/Griefed/ServerPackCreator/commit/dfdc91b09c35f57b780cc715388e96a8603de547))
* **Google Java Format:** Apply Google Java format to sources, reformat and optimize imports. ([dadfe85](https://git.griefed.de/Griefed/ServerPackCreator/commit/dadfe856a5781aad46a22b2b5a7eb4615cb4244b))
* **Google Java Format:** Apply Google Java format to sources, reformat and optimize imports. Add editorconfig and project config ([7eea595](https://git.griefed.de/Griefed/ServerPackCreator/commit/7eea595c108d37c7af7d9e0d9097548942a5cc5a))


### 💎 Improvements

* **About window:** Improve display of About text and improve the text itself. Delete your `lang/lang_en_us.properties`-file to receive the new text. ([0d4193d](https://git.griefed.de/Griefed/ServerPackCreator/commit/0d4193d42b0dcf30ad5dd4ae54b06f1cb9e8ab86))
* **ConfigChecks:** Check the ServerPackCreator base directory and files and directories specified in the configuration for read-permission. ([bbb70db](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbb70dbbf025967c2bf025f761348ef3011a1e35))
* **Copy directories textfield:** Turn textfield into a textarea much like clientside-only mods and JVM args. ([9dfc606](https://git.griefed.de/Griefed/ServerPackCreator/commit/9dfc6066a71af723b7862e32d84b01fc9fefa748))
* **Copy Files and Directories:** Allow specifying absolute paths to files and directories to include in the server pack. Example `C:/`foo/bar` will result in `bar` being copied to the server pack as `bar`, and `C:/`foo/bar.file` will result in `bar.file` being copied to the server pack as `bar.file`. ([5978347](https://git.griefed.de/Griefed/ServerPackCreator/commit/59783475400d73d5f13505fb0ba7c5361a942458))
* **Create Server Pack Tab:** Improve scroll amount which improves the user experience with the tab ([b11a876](https://git.griefed.de/Griefed/ServerPackCreator/commit/b11a8761535741f8c1ed6d23a7a38f9049038987))
* **Create Server Pack tab:** Place status labels in BoxLayout panel and set preferred size, preventing resizing. Allow resizing of ServerPackCreator window at all times. ([2a4bc52](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a4bc52ba16671fdb5fc8f10e6914a3562f28705))
* **Fabric start scripts:** If the improved Fabric server launcher is present, use it. Otherwise the old-fashoned way of downloading and installing a classic Fabric server is used. ([0283d34](https://git.griefed.de/Griefed/ServerPackCreator/commit/0283d34402a5f6ec1285494b7b4a65619b462972))
* **Fallback List Clientside-Mods:** Remove JEITweaker from list of fallback clientside-mods. Thanks to @NevadaActual for the report. ([c7de800](https://git.griefed.de/Griefed/ServerPackCreator/commit/c7de80090c24ae784d581cf36eed17591b293a46))
* **GUI conf and log upload:** Improve checks and error message when uploading files to HaseBin which are too large. Delete your `lang/lang_en_us.properties` to receive the new message if you're using SPC in english. ([5ac005e](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ac005e667c06af6aaffc0f3aadaecc1590f3043))
* **GUI FileChoosers and file/folder opening:** Open filechoosers in center of SPC window. Centralize file and folder opening to utilities. ([0b8b426](https://git.griefed.de/Griefed/ServerPackCreator/commit/0b8b4263c351f7d5fa6e17a9107cc1c55b671ba3))
* **GUI Ready status:** Enable generation button and turn of status bar after generation has completed, but before dialogs are shown. Looks better and makes more sense. ([d3ab985](https://git.griefed.de/Griefed/ServerPackCreator/commit/d3ab98564284eb1b3aa0098421c9b0b080d4c868))
* **GUI status display:** Display a scrolling bar during the generation of a server pack to indicate that ServerPackCreator is currently doing something. Thanks to @Kreezxil for the suggestion! ([fa331bf](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa331bf8048bb3c5fadc3d9f9ab8a0b882b37a82))
* **GUI:** Various. Prevent text in status label box from being cut off at the end. Replace crude status animation with custom made LarsonScanner. Refactor some classes to inner classes where appropriate. Update some dependencies. ([377d674](https://git.griefed.de/Griefed/ServerPackCreator/commit/377d6745e0e1f577a9e1dc7ff192910da7374252))
* **i18n:** Provide more language-keys for i18n. ([1b5c695](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b5c6955a456b93802605ae3d100f9127792f6de))
* **Java-path label and tooltip:** Improve wording to reduce confusion as to what this setting is for. Please delete your `lang/lang_en_us.properties`-file. ([f87537f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f87537f9a7539ebf456e4f4fa4f70b1b67dd20b5))
* **Menu Bar View items:** Add SPC log, modloader installer log and addons log view items to open the respective logs in your default text-editor. ([361a62e](https://git.griefed.de/Griefed/ServerPackCreator/commit/361a62edf1780c94b505fc14a44a29cd3201b8ae))
* **Modloader Installer Log tab:** Remove Modloader installer log tab. Rarely used, slows down SPC, rarely contains important information. ([8a9a793](https://git.griefed.de/Griefed/ServerPackCreator/commit/8a9a79386b527ef8a06e506e1a08bb22e0f367dc))
* **Modloader server installation:** Move the installation of the modloader server AFTER the generation of the server packs ZIP-archive. This ensures the ZIP-archive contains NO files which would result in a refection from CurseForge or other services. It ensures the ZIP-archive is as lightweight as possible. ([39eb24f](https://git.griefed.de/Griefed/ServerPackCreator/commit/39eb24fcf182b71eb773e2f90aad6aab28002829))
* **Server-icon and properties:** Improve copying of the specified server-icon.png and server.properties as well as logging in case of errors. If the specified image is already 64x64, do not scale it unnecessarily. ([d2cb195](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2cb195cbf9cdd0a1978286ed8181692061694ad))
* **SPC Window:** Open ServerPackCreator in the center of the main display. ([2020ca6](https://git.griefed.de/Griefed/ServerPackCreator/commit/2020ca6874531fab71d3ec356c825abc3a55492e))
* **UNIX symlinks and Windows lnks:** Allow users to work with links/symlinks at certain points. Modpack directory, copy directories, server icon and properties, Java path, config to load shoud now all work with UNIX symlinks or Windows lnks. ([f57686a](https://git.griefed.de/Griefed/ServerPackCreator/commit/f57686af86741126ceb77017151b91638cd984f8))
* **UNIX symlinks and Windows lnks:** Allow users to work with links/symlinks at certain points. Modpack directory, copy directories, server icon and properties, Java path, config to load shoud now all work with UNIX symlinks or Windows lnks. ([74bea1b](https://git.griefed.de/Griefed/ServerPackCreator/commit/74bea1b87f39514cea8f77c32e8e707d75b0a0e4))


### 📔 Docs

* **GenerateZip params:** Remove param in JavaDoc for GenerateZip:44, containing extra, invalid, argument. ([15319af](https://git.griefed.de/Griefed/ServerPackCreator/commit/15319aff5256e076ae7af0756960f2a14211de33))
* **params:** Remove/fix constructors and missing params ([d78e310](https://git.griefed.de/Griefed/ServerPackCreator/commit/d78e3107916cb85699dd06b017a7576eb8968bd1))


### 🦊 CI/CD

* **Workflow:** Prevent detached pipelines from running ([af486a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/af486a598d1870f94a6473fcd1e44d95713a8344))


### 🧪 Tests

* **GitLab:** Fix coverage-job artifacts so it works on GitLab.com again. Deactivate Inform About Release job, as GitHub is the main distribution platform for releases. ([13e0d3e](https://git.griefed.de/Griefed/ServerPackCreator/commit/13e0d3e3b412fb84d13449e5d0bcf32acac739c9))
* **GitLab:** SSL still broken with GitLab.com. ([bcc0ff3](https://git.griefed.de/Griefed/ServerPackCreator/commit/bcc0ff30e7f8162a8278517f06ee12ab144516cd))


### 🚀 Features

* **Quilt Modloader Support:** Fully fledged Quilt support! Including start scripts, modloader server installation, automated clientside-only mod exclusions and webservice support! Please delete your `lang/lang_en_us.properties`-file to receive language updates. ([849e0bc](https://git.griefed.de/Griefed/ServerPackCreator/commit/849e0bcc1709530e9670a29244d546d6a59b88a3))


### 🛠 Fixes

* **1.12.2 Forge sideness scanning false positives:** If a single mod JAR-file contains multiple mods, check all mods in that JAR-file for sideness and determine whether it is clientside-only correctly. ([f4d1081](https://git.griefed.de/Griefed/ServerPackCreator/commit/f4d1081a5f21adb71c07ee03e83c72a1fbadc6b8))
* **Copy-directories re-validation:** Upon changing the modpack-directory, re-validate the copy-directories field to prevent false-negatives. ([0ee983c](https://git.griefed.de/Griefed/ServerPackCreator/commit/0ee983cdf49f5dd4d31c63b72468ac089161b1dd))
* **File-ending:** If a user does not specify .conf as the file ending when SaveAs is used, append it so we always have .conf-files. ([7d87b2f](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d87b2f26885e6e4f696a1d164cc9a6a5a002da0))
* **GUI Forge versions selection box:** Update the list of Forge versions when the selected Minecraft version is changed ([02f6004](https://git.griefed.de/Griefed/ServerPackCreator/commit/02f60040a77e30d8ed2da7bfc18b30b991f2d353))
* **Symlink check:** Prevent IllegalCharacterException in FileUtilities.isLink(...) when checking the given file whether it is a symlink but said file having a Windows-path. Rename commonutilities package to common. ([a6c5c59](https://git.griefed.de/Griefed/ServerPackCreator/commit/a6c5c59ae445ea7fdd0d84d3267d7e53ab6278ac))
* **Unable to save config when clientside-mods is empty:** Prevent IndexOutOfBounds-exception when the passed list in encapsulateListElements is empty. ([bd620f8](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd620f8fdba6264110e0a9e5d5e59cc29981f729))
* **WebUI:** Correctly set modloader versions when selecting a Minecraft version ([a17e3f5](https://git.griefed.de/Griefed/ServerPackCreator/commit/a17e3f5d0910071bf9300b9f9feb49b1807caf56))


### Other

* **deps:** update dependency @quasar/extras to v1.13.6 ([d34f8fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/d34f8fd657a10731584ac0d1b2355229e7d6543b))
* **deps:** update dependency axios to v0.27.2 ([86f56f5](https://git.griefed.de/Griefed/ServerPackCreator/commit/86f56f57668d0ef7039d7b78f04207e8fa0569b8))
* **deps:** update dependency core-js to v3.22.4 ([1c4b985](https://git.griefed.de/Griefed/ServerPackCreator/commit/1c4b985529212927f0ac43195b4d2f3d37fffc44))
* **deps:** update dependency core-js to v3.22.8 ([e91d9f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/e91d9f1759e3bc5412da0b95ce80dfd750747624))
* **deps:** update dependency eslint to v8.14.0 ([dbb27fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/dbb27faa7e1d69fb4e4f869ff5f7436431bcfcf6))
* **deps:** update dependency eslint-plugin-vue to v8.7.1 ([97f86a8](https://git.griefed.de/Griefed/ServerPackCreator/commit/97f86a8809fea915b5e540e1ed8b8d200ba5cb0b))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.10 ([dd527e5](https://git.griefed.de/Griefed/ServerPackCreator/commit/dd527e59f9b6a50669ddc459097da188a8799e89))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.9 ([198aa4f](https://git.griefed.de/Griefed/ServerPackCreator/commit/198aa4f4d7a5df186af45fc3b7ce0c0e2328e17e))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.6 ([42b6f7a](https://git.griefed.de/Griefed/ServerPackCreator/commit/42b6f7a1d0f0e42cd7db98aceee58b73c98dd466))
* **deps:** update dependency gradle to v7.4.2 ([b65f938](https://git.griefed.de/Griefed/ServerPackCreator/commit/b65f9388ab4f11e10a98c4ed0e181bfbed81d129))
* **deps:** update dependency org.apache.activemq:artemis-jms-server to v2.21.0 ([ebd8add](https://git.griefed.de/Griefed/ServerPackCreator/commit/ebd8adde438f25adc933ab4a0949f9ad9c312e08))
* **deps:** update dependency tsparticles to v2.0.6 ([ebb78ba](https://git.griefed.de/Griefed/ServerPackCreator/commit/ebb78baef18c8df5d0e59cdd3e07970608338ae7))
* **deps:** update dependency vue to v3.2.33 ([3adabfc](https://git.griefed.de/Griefed/ServerPackCreator/commit/3adabfcf2a8df8bf5689d56f66e9fa770aa35bfe))
* **deps:** update jamesives/github-pages-deploy-action action to v4.3.3 ([546cec4](https://git.griefed.de/Griefed/ServerPackCreator/commit/546cec4b237dcb6663eb2981b33f8721c334b10b))
* **deps:** update spring boot to v2.6.7 ([c61906e](https://git.griefed.de/Griefed/ServerPackCreator/commit/c61906e2a306e91f3bc2a19127ea4bd34f9833c8))
* **deps:** update typescript-eslint monorepo to v5.22.0 ([f0a5512](https://git.griefed.de/Griefed/ServerPackCreator/commit/f0a5512f86421ebb33d1027bbcad8e06e93051e5))
* **Encoding:** UTF-8 ALL ZE FILES!!!11!1 ([b7d3189](https://git.griefed.de/Griefed/ServerPackCreator/commit/b7d318967d19b9e8cd3c5cdfcad8262d74c080b6))
* **gitignore:** Add some more test resources to the gitignore. ([3481345](https://git.griefed.de/Griefed/ServerPackCreator/commit/34813454de5061899ea3f929965ec3b56a485942))
* **Status labels:** Reduce max length of entries to reduce amount of component resizing. ([5eed4ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/5eed4ad4e59861b7a6f2444f3df362ad5ecf4435))

## [3.5.0-beta.4](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-beta.3...3.5.0-beta.4) (2022-06-17)


### ⏩ Performance

* **GUI:** Various little improvements towards initialization and setup of the GUI ([8112bcb](https://git.griefed.de/Griefed/ServerPackCreator/commit/8112bcbff3987526d8b1659c0548fe5e7ee1e517))


### 💎 Improvements

* **Fallback List Clientside-Mods:** Remove JEITweaker from list of fallback clientside-mods. Thanks to @NevadaActual for the report. ([c7de800](https://git.griefed.de/Griefed/ServerPackCreator/commit/c7de80090c24ae784d581cf36eed17591b293a46))
* **GUI:** Various. Prevent text in status label box from being cut off at the end. Replace crude status animation with custom made LarsonScanner. Refactor some classes to inner classes where appropriate. Update some dependencies. ([377d674](https://git.griefed.de/Griefed/ServerPackCreator/commit/377d6745e0e1f577a9e1dc7ff192910da7374252))


### 🦊 CI/CD

* **Workflow:** Prevent detached pipelines from running ([af486a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/af486a598d1870f94a6473fcd1e44d95713a8344))


### 🛠 Fixes

* **WebUI:** Correctly set modloader versions when selecting a Minecraft version ([a17e3f5](https://git.griefed.de/Griefed/ServerPackCreator/commit/a17e3f5d0910071bf9300b9f9feb49b1807caf56))

## [3.5.0-beta.3](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-beta.2...3.5.0-beta.3) (2022-06-06)


### 💎 Improvements

* **GUI FileChoosers and file/folder opening:** Open filechoosers in center of SPC window. Centralize file and folder opening to utilities. ([0b8b426](https://git.griefed.de/Griefed/ServerPackCreator/commit/0b8b4263c351f7d5fa6e17a9107cc1c55b671ba3))
* **GUI Ready status:** Enable generation button and turn of status bar after generation has completed, but before dialogs are shown. Looks better and makes more sense. ([d3ab985](https://git.griefed.de/Griefed/ServerPackCreator/commit/d3ab98564284eb1b3aa0098421c9b0b080d4c868))
* **GUI status display:** Display a scrolling bar during the generation of a server pack to indicate that ServerPackCreator is currently doing something. Thanks to @Kreezxil for the suggestion! ([fa331bf](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa331bf8048bb3c5fadc3d9f9ab8a0b882b37a82))

## [3.5.0-beta.2](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-beta.1...3.5.0-beta.2) (2022-06-05)


### :scissors: Refactor

* **Addongs log tab:** Remove not needed fields and constructor params ([fe5d633](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe5d633f5117db7e6e4874be10914f8d5c27932c))
* **Addongs log tab:** Remove not needed fields and constructor params. Display full logs without truncating. ([1504a8e](https://git.griefed.de/Griefed/ServerPackCreator/commit/1504a8e57d78f59f11da37574d01a39ee60f4e06))
* **Log Tail Component:** Always display horizontal scrollbar. ([fa20df4](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa20df4e369d4e6d2a69b816f931e0de8cbb8342))


### 💎 Improvements

* **Create Server Pack Tab:** Improve scroll amount which improves the user experience with the tab ([b11a876](https://git.griefed.de/Griefed/ServerPackCreator/commit/b11a8761535741f8c1ed6d23a7a38f9049038987))
* **Create Server Pack tab:** Place status labels in BoxLayout panel and set preferred size, preventing resizing. Allow resizing of ServerPackCreator window at all times. ([2a4bc52](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a4bc52ba16671fdb5fc8f10e6914a3562f28705))
* **Menu Bar View items:** Add SPC log, modloader installer log and addons log view items to open the respective logs in your default text-editor. ([361a62e](https://git.griefed.de/Griefed/ServerPackCreator/commit/361a62edf1780c94b505fc14a44a29cd3201b8ae))
* **Modloader Installer Log tab:** Remove Modloader installer log tab. Rarely used, slows down SPC, rarely contains important information. ([8a9a793](https://git.griefed.de/Griefed/ServerPackCreator/commit/8a9a79386b527ef8a06e506e1a08bb22e0f367dc))


### Other

* **Encoding:** UTF-8 ALL ZE FILES!!!11!1 ([b7d3189](https://git.griefed.de/Griefed/ServerPackCreator/commit/b7d318967d19b9e8cd3c5cdfcad8262d74c080b6))

## [3.5.0-beta.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.4.1...3.5.0-beta.1) (2022-06-04)


### :scissors: Refactor

* **Code Quality & Style:** Several code quality and style changes. ([486ea43](https://git.griefed.de/Griefed/ServerPackCreator/commit/486ea43f71fe404aa6e561365a67c693d1ff8f2c))
* **Code Quality & Style:** Several code quality and style changes. ([75b1ad7](https://git.griefed.de/Griefed/ServerPackCreator/commit/75b1ad77f841d83d450794bb1ad4ed75763bca14))
* **Code Quality & Style:** Several code quality and style changes. ([bd9c0f6](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd9c0f6a1cb8e6bf44ada3bcefbf4c492ccd33dc))
* **Code Quality & Style:** Several code quality and style changes. ([0def7aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/0def7aa74f73f3932dc216de0cbe00e9b0e3d0af))
* **Enums:** Change the way enums are used in the CommandlineParser and VersionMeta. Remove / use values where appropriate. ([fea95a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/fea95a27fac3ae6180d3725384a260361b1820b2))
* **File copying:** Gather a list of all files to be copied to the server pack and THEN copy them. Improves readibility of the copyFiles(...)-method. Delete your `lang/lang_en_us.properties`-file to receive the new text. ([640ba07](https://git.griefed.de/Griefed/ServerPackCreator/commit/640ba071cef5ce19ca9fde76b4ac0d3210e80f20))
* **ServerPackModel params:** Remove unsudes params from constructor. ([a341f83](https://git.griefed.de/Griefed/ServerPackCreator/commit/a341f83859c00f6ba397eb7a6289113cf172d0b4))
* **UNIX symlinks and Windows lnks:** Sanitize links at beginning of config check and resolve any and all links before checks are run. ([2a9ea36](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a9ea364f16c56fe9ddf266eef2d71f8f846f2c7))


### ⏩ Performance

* **FileWatcher setup:** Setup FileWatcher off-thread when running GUI, to improve startup-time of ServerPackCreator. ([087bed5](https://git.griefed.de/Griefed/ServerPackCreator/commit/087bed58fb3a255bb544dbee6bab4491d05c2a62))


### 👀 Reverts

* **CurseForge:** Completely remove CurseForge functionality from ServerPackCreator. ([d3de549](https://git.griefed.de/Griefed/ServerPackCreator/commit/d3de549c3c0d420ccf6d47d2c477bf8672e3687a))
* **CurseForge:** Remove mentions of CurseForge from i18n ([d876360](https://git.griefed.de/Griefed/ServerPackCreator/commit/d876360860e9eb4b75ce7f856161a9aed41a0378))
* **Modrinth:** Remove Modrinth preperations from backend and tab from frontend ([eeafa78](https://git.griefed.de/Griefed/ServerPackCreator/commit/eeafa78e90b1468687e67cbdf5cfe168dd0747e4))


### 💈 Style

* **Formatting:** Let IntelliJ IDEA reformat code and optimize imports. ([a019a55](https://git.griefed.de/Griefed/ServerPackCreator/commit/a019a551b3bafb74cd4a41666c70bbcf852ccb28))
* **Formatting:** Let IntelliJ IDEA reformat code and optimize imports. ([dfdc91b](https://git.griefed.de/Griefed/ServerPackCreator/commit/dfdc91b09c35f57b780cc715388e96a8603de547))
* **Google Java Format:** Apply Google Java format to sources, reformat and optimize imports. ([dadfe85](https://git.griefed.de/Griefed/ServerPackCreator/commit/dadfe856a5781aad46a22b2b5a7eb4615cb4244b))
* **Google Java Format:** Apply Google Java format to sources, reformat and optimize imports. Add editorconfig and project config ([7eea595](https://git.griefed.de/Griefed/ServerPackCreator/commit/7eea595c108d37c7af7d9e0d9097548942a5cc5a))


### 💎 Improvements

* **About window:** Improve display of About text and improve the text itself. Delete your `lang/lang_en_us.properties`-file to receive the new text. ([0d4193d](https://git.griefed.de/Griefed/ServerPackCreator/commit/0d4193d42b0dcf30ad5dd4ae54b06f1cb9e8ab86))
* **ConfigChecks:** Check the ServerPackCreator base directory and files and directories specified in the configuration for read-permission. ([bbb70db](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbb70dbbf025967c2bf025f761348ef3011a1e35))
* **Copy directories textfield:** Turn textfield into a textarea much like clientside-only mods and JVM args. ([9dfc606](https://git.griefed.de/Griefed/ServerPackCreator/commit/9dfc6066a71af723b7862e32d84b01fc9fefa748))
* **Copy Files and Directories:** Allow specifying absolute paths to files and directories to include in the server pack. Example `C:/`foo/bar` will result in `bar` being copied to the server pack as `bar`, and `C:/`foo/bar.file` will result in `bar.file` being copied to the server pack as `bar.file`. ([5978347](https://git.griefed.de/Griefed/ServerPackCreator/commit/59783475400d73d5f13505fb0ba7c5361a942458))
* **Fabric start scripts:** If the improved Fabric server launcher is present, use it. Otherwise the old-fashoned way of downloading and installing a classic Fabric server is used. ([0283d34](https://git.griefed.de/Griefed/ServerPackCreator/commit/0283d34402a5f6ec1285494b7b4a65619b462972))
* **GUI conf and log upload:** Improve checks and error message when uploading files to HaseBin which are too large. Delete your `lang/lang_en_us.properties` to receive the new message if you're using SPC in english. ([5ac005e](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ac005e667c06af6aaffc0f3aadaecc1590f3043))
* **i18n:** Provide more language-keys for i18n. ([1b5c695](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b5c6955a456b93802605ae3d100f9127792f6de))
* **Java-path label and tooltip:** Improve wording to reduce confusion as to what this setting is for. Please delete your `lang/lang_en_us.properties`-file. ([f87537f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f87537f9a7539ebf456e4f4fa4f70b1b67dd20b5))
* **Modloader server installation:** Move the installation of the modloader server AFTER the generation of the server packs ZIP-archive. This ensures the ZIP-archive contains NO files which would result in a refection from CurseForge or other services. It ensures the ZIP-archive is as lightweight as possible. ([39eb24f](https://git.griefed.de/Griefed/ServerPackCreator/commit/39eb24fcf182b71eb773e2f90aad6aab28002829))
* **Server-icon and properties:** Improve copying of the specified server-icon.png and server.properties as well as logging in case of errors. If the specified image is already 64x64, do not scale it unnecessarily. ([d2cb195](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2cb195cbf9cdd0a1978286ed8181692061694ad))
* **SPC Window:** Open ServerPackCreator in the center of the main display. ([2020ca6](https://git.griefed.de/Griefed/ServerPackCreator/commit/2020ca6874531fab71d3ec356c825abc3a55492e))
* **UNIX symlinks and Windows lnks:** Allow users to work with links/symlinks at certain points. Modpack directory, copy directories, server icon and properties, Java path, config to load shoud now all work with UNIX symlinks or Windows lnks. ([f57686a](https://git.griefed.de/Griefed/ServerPackCreator/commit/f57686af86741126ceb77017151b91638cd984f8))
* **UNIX symlinks and Windows lnks:** Allow users to work with links/symlinks at certain points. Modpack directory, copy directories, server icon and properties, Java path, config to load shoud now all work with UNIX symlinks or Windows lnks. ([74bea1b](https://git.griefed.de/Griefed/ServerPackCreator/commit/74bea1b87f39514cea8f77c32e8e707d75b0a0e4))


### 📔 Docs

* **GenerateZip params:** Remove param in JavaDoc for GenerateZip:44, containing extra, invalid, argument. ([15319af](https://git.griefed.de/Griefed/ServerPackCreator/commit/15319aff5256e076ae7af0756960f2a14211de33))
* **params:** Remove/fix constructors and missing params ([d78e310](https://git.griefed.de/Griefed/ServerPackCreator/commit/d78e3107916cb85699dd06b017a7576eb8968bd1))


### 🧪 Tests

* **GitLab:** Fix coverage-job artifacts so it works on GitLab.com again. Deactivate Inform About Release job, as GitHub is the main distribution platform for releases. ([13e0d3e](https://git.griefed.de/Griefed/ServerPackCreator/commit/13e0d3e3b412fb84d13449e5d0bcf32acac739c9))
* **GitLab:** SSL still broken with GitLab.com. ([bcc0ff3](https://git.griefed.de/Griefed/ServerPackCreator/commit/bcc0ff30e7f8162a8278517f06ee12ab144516cd))


### 🚀 Features

* **Quilt Modloader Support:** Fully fledged Quilt support! Including start scripts, modloader server installation, automated clientside-only mod exclusions and webservice support! Please delete your `lang/lang_en_us.properties`-file to receive language updates. ([849e0bc](https://git.griefed.de/Griefed/ServerPackCreator/commit/849e0bcc1709530e9670a29244d546d6a59b88a3))


### 🛠 Fixes

* **1.12.2 Forge sideness scanning false positives:** If a single mod JAR-file contains multiple mods, check all mods in that JAR-file for sideness and determine whether it is clientside-only correctly. ([f4d1081](https://git.griefed.de/Griefed/ServerPackCreator/commit/f4d1081a5f21adb71c07ee03e83c72a1fbadc6b8))
* **Copy-directories re-validation:** Upon changing the modpack-directory, re-validate the copy-directories field to prevent false-negatives. ([0ee983c](https://git.griefed.de/Griefed/ServerPackCreator/commit/0ee983cdf49f5dd4d31c63b72468ac089161b1dd))
* **File-ending:** If a user does not specify .conf as the file ending when SaveAs is used, append it so we always have .conf-files. ([7d87b2f](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d87b2f26885e6e4f696a1d164cc9a6a5a002da0))
* **GUI Forge versions selection box:** Update the list of Forge versions when the selected Minecraft version is changed ([02f6004](https://git.griefed.de/Griefed/ServerPackCreator/commit/02f60040a77e30d8ed2da7bfc18b30b991f2d353))
* **Symlink check:** Prevent IllegalCharacterException in FileUtilities.isLink(...) when checking the given file whether it is a symlink but said file having a Windows-path. Rename commonutilities package to common. ([a6c5c59](https://git.griefed.de/Griefed/ServerPackCreator/commit/a6c5c59ae445ea7fdd0d84d3267d7e53ab6278ac))
* **Unable to save config when clientside-mods is empty:** Prevent IndexOutOfBounds-exception when the passed list in encapsulateListElements is empty. ([bd620f8](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd620f8fdba6264110e0a9e5d5e59cc29981f729))


### Other

* **deps:** update dependency @quasar/extras to v1.13.6 ([d34f8fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/d34f8fd657a10731584ac0d1b2355229e7d6543b))
* **deps:** update dependency axios to v0.27.2 ([86f56f5](https://git.griefed.de/Griefed/ServerPackCreator/commit/86f56f57668d0ef7039d7b78f04207e8fa0569b8))
* **deps:** update dependency core-js to v3.22.4 ([1c4b985](https://git.griefed.de/Griefed/ServerPackCreator/commit/1c4b985529212927f0ac43195b4d2f3d37fffc44))
* **deps:** update dependency eslint to v8.14.0 ([dbb27fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/dbb27faa7e1d69fb4e4f869ff5f7436431bcfcf6))
* **deps:** update dependency eslint-plugin-vue to v8.7.1 ([97f86a8](https://git.griefed.de/Griefed/ServerPackCreator/commit/97f86a8809fea915b5e540e1ed8b8d200ba5cb0b))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.9 ([198aa4f](https://git.griefed.de/Griefed/ServerPackCreator/commit/198aa4f4d7a5df186af45fc3b7ce0c0e2328e17e))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.6 ([42b6f7a](https://git.griefed.de/Griefed/ServerPackCreator/commit/42b6f7a1d0f0e42cd7db98aceee58b73c98dd466))
* **deps:** update dependency gradle to v7.4.2 ([b65f938](https://git.griefed.de/Griefed/ServerPackCreator/commit/b65f9388ab4f11e10a98c4ed0e181bfbed81d129))
* **deps:** update dependency org.apache.activemq:artemis-jms-server to v2.21.0 ([ebd8add](https://git.griefed.de/Griefed/ServerPackCreator/commit/ebd8adde438f25adc933ab4a0949f9ad9c312e08))
* **deps:** update dependency tsparticles to v2.0.6 ([ebb78ba](https://git.griefed.de/Griefed/ServerPackCreator/commit/ebb78baef18c8df5d0e59cdd3e07970608338ae7))
* **deps:** update dependency vue to v3.2.33 ([3adabfc](https://git.griefed.de/Griefed/ServerPackCreator/commit/3adabfcf2a8df8bf5689d56f66e9fa770aa35bfe))
* **deps:** update jamesives/github-pages-deploy-action action to v4.3.3 ([546cec4](https://git.griefed.de/Griefed/ServerPackCreator/commit/546cec4b237dcb6663eb2981b33f8721c334b10b))
* **deps:** update spring boot to v2.6.7 ([c61906e](https://git.griefed.de/Griefed/ServerPackCreator/commit/c61906e2a306e91f3bc2a19127ea4bd34f9833c8))
* **deps:** update typescript-eslint monorepo to v5.22.0 ([f0a5512](https://git.griefed.de/Griefed/ServerPackCreator/commit/f0a5512f86421ebb33d1027bbcad8e06e93051e5))
* **gitignore:** Add some more test resources to the gitignore. ([3481345](https://git.griefed.de/Griefed/ServerPackCreator/commit/34813454de5061899ea3f929965ec3b56a485942))
* **Status labels:** Reduce max length of entries to reduce amount of component resizing. ([5eed4ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/5eed4ad4e59861b7a6f2444f3df362ad5ecf4435))

## [3.5.0-alpha.8](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-alpha.7...3.5.0-alpha.8) (2022-06-03)


### :scissors: Refactor

* **File copying:** Gather a list of all files to be copied to the server pack and THEN copy them. Improves readibility of the copyFiles(...)-method. Delete your `lang/lang_en_us.properties`-file to receive the new text. ([640ba07](https://git.griefed.de/Griefed/ServerPackCreator/commit/640ba071cef5ce19ca9fde76b4ac0d3210e80f20))


### ⏩ Performance

* **FileWatcher setup:** Setup FileWatcher off-thread when running GUI, to improve startup-time of ServerPackCreator. ([087bed5](https://git.griefed.de/Griefed/ServerPackCreator/commit/087bed58fb3a255bb544dbee6bab4491d05c2a62))


### 💎 Improvements

* **About window:** Improve display of About text and improve the text itself. Delete your `lang/lang_en_us.properties`-file to receive the new text. ([0d4193d](https://git.griefed.de/Griefed/ServerPackCreator/commit/0d4193d42b0dcf30ad5dd4ae54b06f1cb9e8ab86))
* **Copy directories textfield:** Turn textfield into a textarea much like clientside-only mods and JVM args. ([9dfc606](https://git.griefed.de/Griefed/ServerPackCreator/commit/9dfc6066a71af723b7862e32d84b01fc9fefa748))
* **Copy Files and Directories:** Allow specifying absolute paths to files and directories to include in the server pack. Example `C:/`foo/bar` will result in `bar` being copied to the server pack as `bar`, and `C:/`foo/bar.file` will result in `bar.file` being copied to the server pack as `bar.file`. ([5978347](https://git.griefed.de/Griefed/ServerPackCreator/commit/59783475400d73d5f13505fb0ba7c5361a942458))
* **i18n:** Provide more language-keys for i18n. ([1b5c695](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b5c6955a456b93802605ae3d100f9127792f6de))
* **Java-path label and tooltip:** Improve wording to reduce confusion as to what this setting is for. Please delete your `lang/lang_en_us.properties`-file. ([f87537f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f87537f9a7539ebf456e4f4fa4f70b1b67dd20b5))
* **SPC Window:** Open ServerPackCreator in the center of the main display. ([2020ca6](https://git.griefed.de/Griefed/ServerPackCreator/commit/2020ca6874531fab71d3ec356c825abc3a55492e))


### 🛠 Fixes

* **Symlink check:** Prevent IllegalCharacterException in FileUtilities.isLink(...) when checking the given file whether it is a symlink but said file having a Windows-path. Rename commonutilities package to common. ([a6c5c59](https://git.griefed.de/Griefed/ServerPackCreator/commit/a6c5c59ae445ea7fdd0d84d3267d7e53ab6278ac))

## [3.5.0-alpha.7](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-alpha.6...3.5.0-alpha.7) (2022-05-28)


### :scissors: Refactor

* **Code Quality & Style:** Several code quality and style changes. ([486ea43](https://git.griefed.de/Griefed/ServerPackCreator/commit/486ea43f71fe404aa6e561365a67c693d1ff8f2c))


### 🛠 Fixes

* **GUI Forge versions selection box:** Update the list of Forge versions when the selected Minecraft version is changed ([02f6004](https://git.griefed.de/Griefed/ServerPackCreator/commit/02f60040a77e30d8ed2da7bfc18b30b991f2d353))

## [3.5.0-alpha.6](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-alpha.5...3.5.0-alpha.6) (2022-05-28)


### :scissors: Refactor

* **Code Quality & Style:** Several code quality and style changes. ([75b1ad7](https://git.griefed.de/Griefed/ServerPackCreator/commit/75b1ad77f841d83d450794bb1ad4ed75763bca14))
* **Enums:** Change the way enums are used in the CommandlineParser and VersionMeta. Remove / use values where appropriate. ([fea95a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/fea95a27fac3ae6180d3725384a260361b1820b2))


### 💎 Improvements

* **Fabric start scripts:** If the improved Fabric server launcher is present, use it. Otherwise the old-fashoned way of downloading and installing a classic Fabric server is used. ([0283d34](https://git.griefed.de/Griefed/ServerPackCreator/commit/0283d34402a5f6ec1285494b7b4a65619b462972))
* **Modloader server installation:** Move the installation of the modloader server AFTER the generation of the server packs ZIP-archive. This ensures the ZIP-archive contains NO files which would result in a refection from CurseForge or other services. It ensures the ZIP-archive is as lightweight as possible. ([39eb24f](https://git.griefed.de/Griefed/ServerPackCreator/commit/39eb24fcf182b71eb773e2f90aad6aab28002829))


### 🚀 Features

* **Quilt Modloader Support:** Fully fledged Quilt support! Including start scripts, modloader server installation, automated clientside-only mod exclusions and webservice support! Please delete your `lang/lang_en_us.properties`-file to receive language updates. ([849e0bc](https://git.griefed.de/Griefed/ServerPackCreator/commit/849e0bcc1709530e9670a29244d546d6a59b88a3))

## [3.5.0-alpha.5](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-alpha.4...3.5.0-alpha.5) (2022-05-26)


### :scissors: Refactor

* **Code Quality & Style:** Several code quality and style changes. ([bd9c0f6](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd9c0f6a1cb8e6bf44ada3bcefbf4c492ccd33dc))
* **Code Quality & Style:** Several code quality and style changes. ([0def7aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/0def7aa74f73f3932dc216de0cbe00e9b0e3d0af))


### 👀 Reverts

* **Modrinth:** Remove Modrinth preperations from backend and tab from frontend ([eeafa78](https://git.griefed.de/Griefed/ServerPackCreator/commit/eeafa78e90b1468687e67cbdf5cfe168dd0747e4))


### 🧪 Tests

* **GitLab:** Fix coverage-job artifacts so it works on GitLab.com again. Deactivate Inform About Release job, as GitHub is the main distribution platform for releases. ([13e0d3e](https://git.griefed.de/Griefed/ServerPackCreator/commit/13e0d3e3b412fb84d13449e5d0bcf32acac739c9))
* **GitLab:** SSL still broken with GitLab.com. ([bcc0ff3](https://git.griefed.de/Griefed/ServerPackCreator/commit/bcc0ff30e7f8162a8278517f06ee12ab144516cd))

## [3.5.0-alpha.4](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-alpha.3...3.5.0-alpha.4) (2022-05-23)


### :scissors: Refactor

* **ServerPackModel params:** Remove unsudes params from constructor. ([a341f83](https://git.griefed.de/Griefed/ServerPackCreator/commit/a341f83859c00f6ba397eb7a6289113cf172d0b4))


### 👀 Reverts

* **CurseForge:** Remove mentions of CurseForge from i18n ([d876360](https://git.griefed.de/Griefed/ServerPackCreator/commit/d876360860e9eb4b75ce7f856161a9aed41a0378))


### 💎 Improvements

* **GUI conf and log upload:** Improve checks and error message when uploading files to HaseBin which are too large. Delete your `lang/lang_en_us.properties` to receive the new message if you're using SPC in english. ([5ac005e](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ac005e667c06af6aaffc0f3aadaecc1590f3043))
* **Server-icon and properties:** Improve copying of the specified server-icon.png and server.properties as well as logging in case of errors. If the specified image is already 64x64, do not scale it unnecessarily. ([d2cb195](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2cb195cbf9cdd0a1978286ed8181692061694ad))


### Other

* **gitignore:** Add some more test resources to the gitignore. ([3481345](https://git.griefed.de/Griefed/ServerPackCreator/commit/34813454de5061899ea3f929965ec3b56a485942))

## [3.5.0-alpha.3](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-alpha.2...3.5.0-alpha.3) (2022-05-19)


### 📔 Docs

* **params:** Remove/fix constructors and missing params ([d78e310](https://git.griefed.de/Griefed/ServerPackCreator/commit/d78e3107916cb85699dd06b017a7576eb8968bd1))

## [3.5.0-alpha.2](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.5.0-alpha.1...3.5.0-alpha.2) (2022-05-19)


### 📔 Docs

* **GenerateZip params:** Remove param in JavaDoc for GenerateZip:44, containing extra, invalid, argument. ([15319af](https://git.griefed.de/Griefed/ServerPackCreator/commit/15319aff5256e076ae7af0756960f2a14211de33))

## [3.5.0-alpha.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.4.1...3.5.0-alpha.1) (2022-05-18)


### :scissors: Refactor

* **UNIX symlinks and Windows lnks:** Sanitize links at beginning of config check and resolve any and all links before checks are run. ([2a9ea36](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a9ea364f16c56fe9ddf266eef2d71f8f846f2c7))


### 👀 Reverts

* **CurseForge:** Completely remove CurseForge functionality from ServerPackCreator. ([d3de549](https://git.griefed.de/Griefed/ServerPackCreator/commit/d3de549c3c0d420ccf6d47d2c477bf8672e3687a))


### 💎 Improvements

* **UNIX symlinks and Windows lnks:** Allow users to work with links/symlinks at certain points. Modpack directory, copy directories, server icon and properties, Java path, config to load shoud now all work with UNIX symlinks or Windows lnks. ([f57686a](https://git.griefed.de/Griefed/ServerPackCreator/commit/f57686af86741126ceb77017151b91638cd984f8))
* **UNIX symlinks and Windows lnks:** Allow users to work with links/symlinks at certain points. Modpack directory, copy directories, server icon and properties, Java path, config to load shoud now all work with UNIX symlinks or Windows lnks. ([74bea1b](https://git.griefed.de/Griefed/ServerPackCreator/commit/74bea1b87f39514cea8f77c32e8e707d75b0a0e4))


### 🛠 Fixes

* **1.12.2 Forge sideness scanning false positives:** If a single mod JAR-file contains multiple mods, check all mods in that JAR-file for sideness and determine whether it is clientside-only correctly. ([f4d1081](https://git.griefed.de/Griefed/ServerPackCreator/commit/f4d1081a5f21adb71c07ee03e83c72a1fbadc6b8))
* **Copy-directories re-validation:** Upon changing the modpack-directory, re-validate the copy-directories field to prevent false-negatives. ([0ee983c](https://git.griefed.de/Griefed/ServerPackCreator/commit/0ee983cdf49f5dd4d31c63b72468ac089161b1dd))
* **File-ending:** If a user does not specify .conf as the file ending when SaveAs is used, append it so we always have .conf-files. ([7d87b2f](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d87b2f26885e6e4f696a1d164cc9a6a5a002da0))
* **Unable to save config when clientside-mods is empty:** Prevent IndexOutOfBounds-exception when the passed list in encapsulateListElements is empty. ([bd620f8](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd620f8fdba6264110e0a9e5d5e59cc29981f729))


### Other

* **deps:** update dependency @quasar/extras to v1.13.6 ([d34f8fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/d34f8fd657a10731584ac0d1b2355229e7d6543b))
* **deps:** update dependency axios to v0.27.2 ([86f56f5](https://git.griefed.de/Griefed/ServerPackCreator/commit/86f56f57668d0ef7039d7b78f04207e8fa0569b8))
* **deps:** update dependency core-js to v3.22.4 ([1c4b985](https://git.griefed.de/Griefed/ServerPackCreator/commit/1c4b985529212927f0ac43195b4d2f3d37fffc44))
* **deps:** update dependency eslint to v8.14.0 ([dbb27fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/dbb27faa7e1d69fb4e4f869ff5f7436431bcfcf6))
* **deps:** update dependency eslint-plugin-vue to v8.7.1 ([97f86a8](https://git.griefed.de/Griefed/ServerPackCreator/commit/97f86a8809fea915b5e540e1ed8b8d200ba5cb0b))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.9 ([198aa4f](https://git.griefed.de/Griefed/ServerPackCreator/commit/198aa4f4d7a5df186af45fc3b7ce0c0e2328e17e))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.6 ([42b6f7a](https://git.griefed.de/Griefed/ServerPackCreator/commit/42b6f7a1d0f0e42cd7db98aceee58b73c98dd466))
* **deps:** update dependency gradle to v7.4.2 ([b65f938](https://git.griefed.de/Griefed/ServerPackCreator/commit/b65f9388ab4f11e10a98c4ed0e181bfbed81d129))
* **deps:** update dependency org.apache.activemq:artemis-jms-server to v2.21.0 ([ebd8add](https://git.griefed.de/Griefed/ServerPackCreator/commit/ebd8adde438f25adc933ab4a0949f9ad9c312e08))
* **deps:** update dependency tsparticles to v2.0.6 ([ebb78ba](https://git.griefed.de/Griefed/ServerPackCreator/commit/ebb78baef18c8df5d0e59cdd3e07970608338ae7))
* **deps:** update dependency vue to v3.2.33 ([3adabfc](https://git.griefed.de/Griefed/ServerPackCreator/commit/3adabfcf2a8df8bf5689d56f66e9fa770aa35bfe))
* **deps:** update jamesives/github-pages-deploy-action action to v4.3.3 ([546cec4](https://git.griefed.de/Griefed/ServerPackCreator/commit/546cec4b237dcb6663eb2981b33f8721c334b10b))
* **deps:** update spring boot to v2.6.7 ([c61906e](https://git.griefed.de/Griefed/ServerPackCreator/commit/c61906e2a306e91f3bc2a19127ea4bd34f9833c8))
* **deps:** update typescript-eslint monorepo to v5.22.0 ([f0a5512](https://git.griefed.de/Griefed/ServerPackCreator/commit/f0a5512f86421ebb33d1027bbcad8e06e93051e5))
* **Status labels:** Reduce max length of entries to reduce amount of component resizing. ([5eed4ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/5eed4ad4e59861b7a6f2444f3df362ad5ecf4435))

### [3.4.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.4.0...3.4.1) (2022-05-03)


### 🛠 Fixes

* **Status Label texts:** Fix the text in the status labels being cut of at random. Java is awesome. I swear. ([2f1958f](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f1958fda5b91d698c9d4d7e2006c24465787642))

## [3.4.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.3.0...3.4.0) (2022-05-03)


### 💎 Improvements

* **GUI:** Live-checks of input-fields in GUI. Allow closing of splash. Improve text colors of light and dark themes. Move Java args configuration from menu bar to Create Server Pack tab. Make Create Server Pack tab scrollable to improve usability for those with smaller screens. More improvements in version 5, milestone 5. ([3337a14](https://git.griefed.de/Griefed/ServerPackCreator/commit/3337a1451299fed199f5c21693cfdb122f7e03b2))

## [3.3.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.2.0...3.3.0) (2022-04-24)


### 💈 Style

* **Formatting:** Make LocalizationManager a bit more readable ([bf851dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/bf851dc277b31504b88d601a62f986e7674326e1))


### 💎 Improvements

* **Help text:** Make formatting of help text more easily expandable for future updates. Expand help text with new arguments. ([796328c](https://git.griefed.de/Griefed/ServerPackCreator/commit/796328c31d0552186fc87a1b05ee424850eb1a01))
* **Sideness scanning:** Scan sideness of Fabric mods and automatically excluded any detected clientside-only mods. ([4bb76aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/4bb76aaa2ab0ba012daddc32dad9b6d3cd87e206))
* **Startup Sequence:** Implement priority system in case multiple arguments have been used (see CommandlineParser). When the GUI is used, display a splashscreen whilst SPC is loading (see ServerPackCreatorSplash). CLI menu for an improved user-experience, giving the user more choice when running in commandline (see ServerPackCreator). The Main-class now only initializes ServerPackCreator and runs it with the determined mode. ([a925a76](https://git.griefed.de/Griefed/ServerPackCreator/commit/a925a763adf04cf5926f1187fc534a9f0203c71c))


### 🧪 Tests

* Try and fix tests which error in CI pipelines, but not on local ([88fb612](https://git.griefed.de/Griefed/ServerPackCreator/commit/88fb6125ea26b112fdd8f6745037fa67a1d9319d))


### 🛠 Fixes

* **ApplicationProperties:** Only load a filesystem properties if it exists. ([417c866](https://git.griefed.de/Griefed/ServerPackCreator/commit/417c8662611517229f08dc655fcb027659a3f28a))
* **File copying from JAR-files:** Replace old mentions of Main.class and other with passed class. Correctly discern whether a dev or production is being used in order to copy files and folders. Add additional method which allows replacing an already existing file when copying from a JAR-file. ([d5b17c5](https://git.griefed.de/Griefed/ServerPackCreator/commit/d5b17c5c388b69c25cd247fe43655b90f26a1be0))
* **VersionMeta double loading:** Prevent VersionMeta from running initializations twice some times by explicitly updating the separate metas. ([808379f](https://git.griefed.de/Griefed/ServerPackCreator/commit/808379fe577ba8deba7b1b7e21c7888418ba944d))


### Other

* **Help:** Information about supported Java version(s) and link to Wiki articles in `-help`-argument ([c8031ce](https://git.griefed.de/Griefed/ServerPackCreator/commit/c8031cef7938d812cd9e9ccbaf17c916b62881c4))
* **Optimize imports:** Just a couple of import optimizations. ([f3cf380](https://git.griefed.de/Griefed/ServerPackCreator/commit/f3cf380ea7ff82ccdfc3aed89c2fa24c7035a45d))

## [3.2.0-beta.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.1.0...3.2.0-beta.1) (2022-04-18)


### 💎 Improvements

* **Sideness scanning:** Scan sideness of Fabric mods and automatically excluded any detected clientside-only mods. ([4bb76aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/4bb76aaa2ab0ba012daddc32dad9b6d3cd87e206))

## [3.2.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.1.0...3.2.0) (2022-04-18)


### :scissors: Refactor

* **GitHub Release Workflow:** Change text in the release workflow changelog generation. It still mentioned pre-releases.... ([ad90d16](https://git.griefed.de/Griefed/ServerPackCreator/commit/ad90d168aa8c0f453b123807b2e92363b8510edb))
* **Manifests:** Move manifest acquisition and update checks from DefaultFiles to VersionMeta. Calling VersionMeta.update() will now check for updated manifests and refresh if needed. ([e2a0af0](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2a0af096cdc1cf243fd9dda819bcf82dbeac5f3))
* **Update Fallback List:** Always update the fallback list if it is different from the one in the repository. Should any server-mod ever make it into this list by accident, this allows remediation of this error from the users side. ([8fd2453](https://git.griefed.de/Griefed/ServerPackCreator/commit/8fd2453604a7c09c29a7b7b0dbbd3b1688541d12))
* **VersionMeta independance:** Make VersionMeta independant of ApplicationProperties by passing the manifest files to the constructor ([f5bd9c7](https://git.griefed.de/Griefed/ServerPackCreator/commit/f5bd9c7639aa44c007cd84787c20fdee45a413a9))


### ⏩ Performance

* **VersionMeta:** Improved startup speed of ServerPackCreator by not gathering all information about Minecraft servers during VersionMeta-instantiation. ([d5986f0](https://git.griefed.de/Griefed/ServerPackCreator/commit/d5986f0ff0cf6344a26063cbda470ab8e1bd6ab5))


### 💎 Improvements

* **Update checks:** Update VersionChecker to 1.1.0. Allow users to check for updates from within the GUI via Menu -> About -> Check for updates. ([0c11ed3](https://git.griefed.de/Griefed/ServerPackCreator/commit/0c11ed319cb61687e2eb993a74a552bbf249aee7))


### 🛠 Fixes

* **UpdateChecker:** Only overwrite Update if GitLab or GitGriefed actually have a newer version available ([e830cdf](https://git.griefed.de/Griefed/ServerPackCreator/commit/e830cdf0863fefa8114187f4e4929b6ef82a548b))


### Other

* **Clientside Mod:** Added mod-credits, durability-notifier, modmenu, defaultsettings-fabric, dynamic-fps, blur, rebrand, better-biome-blend and eggtab-fabric to the fallback list of clientside-only mods. ([f7e7bf4](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7e7bf49baf1a7377df6e85c0c64bb03bfc2d674))
* **Deps:** Dependency updates and ExampleAddon mod replace with versoin 3.0.7 for Unit Testing ([13addd4](https://git.griefed.de/Griefed/ServerPackCreator/commit/13addd45a03fdececa55873b8a3778e5c894d41b))

## [3.1.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.1...3.1.0) (2022-04-15)


### :scissors: Refactor

* **Plugins:** Simplify some calls to plugin information and execution. Move ApplicationPlugins to package plugins. ([8c8c0d1](https://git.griefed.de/Griefed/ServerPackCreator/commit/8c8c0d13110a0fc5602a5ef51cdf12153f420ca8))
* **Utilities:** Combine all utility-classes in one Utilities and allow access of all sub-utility-classes from there. Simplifies constructors and accesses to utilities via a centralized Utilities-class. ([60e20a8](https://git.griefed.de/Griefed/ServerPackCreator/commit/60e20a82c432fe390ebf1be1b4eca0ffeff4071a))


### 💎 Improvements

* **File and Folder exclusions:** Allow exclusions of files or folders from the mod-directory, in addition to the clientside-only mods and sideness-detection. ([0d927c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/0d927c2435a354e43e2c6d17aea483f495a9ebca))
* **Help and HowTo:** Link to my wiki Help and HowTo pages in the menu. Moves the guide on using ServerPackCreator from the README to the wiki. ([8271fa0](https://git.griefed.de/Griefed/ServerPackCreator/commit/8271fa04e24c54d237b8d56e4f50b4bc65c16d5b))
* **Help:** Open the ServerPackCreator help wiki page in the browser. ([4bc81dd](https://git.griefed.de/Griefed/ServerPackCreator/commit/4bc81dd5d3c7d963e9cae7c318d0e8d53b472c11))
* **VersionMeta and Utilities:** Rearrange Utility-classes and completely rewrite the VersionMeta, replacing VersionLister. VersionMeta now provides extensive information about available Minecraft, Fabric and Forge versions, Minecraft servers and their Java version, and more. ([57feba2](https://git.griefed.de/Griefed/ServerPackCreator/commit/57feba262348cc68ec0723a525cf42023a64c5dd))


### 🚀 Features

* **Update fallback modslist from repository:** Update property de.griefed.serverpackcreator.configuration.fallbackmodslist from repository. Refrain from manually editing this property. Use Main Menu->File->"Update Fallback Clientside Modslist" to trigger update checks for this list. ([7e4b332](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e4b3328b89618faa71f8ef41d150a951df2c869))


### 🛠 Fixes

* **Web Modloader Version Selection:** Correctly get, select and present initial modloader version depending on Minecraft version. Also disable modloader server installation which saves space and generation time. Start scripts install the server anyway. ([8bb771b](https://git.griefed.de/Griefed/ServerPackCreator/commit/8bb771bae37a50ec8f6e88f29d5ba97f9335a2a9))


### Other

* **Clientside Mod:** Add BisectHosting Server Integration Menu, BH-Menu-, to fallback list of clientside-only mods. ([ba1eed6](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba1eed6b527756bd6aa8e0c14734ac01e9bb78db))
* **Clientside Mod:** Expand fallback list for clientside-only mods with suggestions from @TheButterbrotman in https://github.com/Griefed/ServerPackCreator/issues/318 ([a7e7a8f](https://git.griefed.de/Griefed/ServerPackCreator/commit/a7e7a8fc188887e345e55c5bddeab707de9025bf))
* **Clientside Mod:** Expand fallback list for clientside-only mods with suggestions from @TheButterbrotman in https://github.com/Griefed/ServerPackCreator/issues/319 ([a5a7cc4](https://git.griefed.de/Griefed/ServerPackCreator/commit/a5a7cc4132367f9629502684c67046b7fa2f1144))
* **Clientside Mods:** Added Charmonium, Dashloader and Entity Texture Features to the fallback list of clientside-only mods. Thanks to @TheButterbrotMan for reporting these! ([2dab9ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/2dab9ade4f6f87bd2369f8fdf72121c8ec985b80))
* **deps:** update actions/checkout action to v3 ([abb1f9a](https://git.griefed.de/Griefed/ServerPackCreator/commit/abb1f9a6520b5b8ab2598507bca2bf6483290e80))
* **deps:** update actions/upload-artifact action to v3 ([be080fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/be080fad4857957f78246e1298bc95b49322705d))
* **deps:** update dependency @quasar/cli to v1.3.2 ([b9bfdc6](https://git.griefed.de/Griefed/ServerPackCreator/commit/b9bfdc66680d0b95949409f218b859380c7c9dd6))
* **deps:** update dependency @quasar/extras to v1.13.5 ([cf07eb6](https://git.griefed.de/Griefed/ServerPackCreator/commit/cf07eb6b2374cd4d707238b04347b154d30f82a4))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.8 ([d5e024b](https://git.griefed.de/Griefed/ServerPackCreator/commit/d5e024b8f09f3d9b24927cc9807e3047c5304b1c))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.5 ([37a6d54](https://git.griefed.de/Griefed/ServerPackCreator/commit/37a6d54cba26d81f7c1e2dd6ca5f2cdc93222902))
* **deps:** update dependency tsparticles to v1.42.4 ([2dfd871](https://git.griefed.de/Griefed/ServerPackCreator/commit/2dfd8718766344c27b54275534a6c12d570edbf6))
* **deps:** update typescript-eslint monorepo to v5.17.0 ([cd6c87b](https://git.griefed.de/Griefed/ServerPackCreator/commit/cd6c87b6c11f18bcea0c83344ac0c7c8c60d4388))
* **Misc:** Add configuration for running tests only ([a0b1ba4](https://git.griefed.de/Griefed/ServerPackCreator/commit/a0b1ba4b40c8b82ad8c5dd0d9b84c57aa6bfca2b))
* **Misc:** Update links in README so they do not point at no longer existing files. Rephrase release text body in release workflows and tell people about the changelog-file. ([801aef1](https://git.griefed.de/Griefed/ServerPackCreator/commit/801aef1485b64a6d5d84146d063d2fc67e6721d7))

### [3.0.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0...3.0.1) (2022-03-26)


### 🛠 Fixes

* **Tests:** Remove unnecessary tests for UpdateChecker as I already have those covered over on VersionChecker. This gets rid of the failing tests as well. ([a1a694b](https://git.griefed.de/Griefed/ServerPackCreator/commit/a1a694b978fcd0903b988c549679a8923b7b36c2))


### Other

* **log4j:** Set log level back down to INFO instead of DEBUG. If you are interested in debug logging, edit your log4j2.xml and set the`property `log-level-spc` to DEBUG ([2a31a2d](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a31a2da0751ae7febdaa6e0791d4a9e3157af15))

## [3.0.0](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.1.1...3.0.0) (2022-03-26)


### :scissors: Refactor

* Add -help argument explaining the basics of running ServerPackCreator. If -help is used, said help text is printed to the console and ServerPackCreator exited. ([4689f54](https://git.griefed.de/Griefed/ServerPackCreator/commit/4689f543359d7a5850d8cd26f2856ff88b719969))
* Add -lang argument information to -help display ([164073f](https://git.griefed.de/Griefed/ServerPackCreator/commit/164073fc8b1a461d35f94921fb2f444728672738))
* Add additional catch for NPE. Fix typo in docs. Remove unused field. ([b5f9042](https://git.griefed.de/Griefed/ServerPackCreator/commit/b5f90421002124b7a1e53f2c11581ead7fab00a2))
* Add getters/setters and allow reloading of properties. Also add some documentation to properties as well as more default values, just to be on the safe side ([049925e](https://git.griefed.de/Griefed/ServerPackCreator/commit/049925e9ddad7e89ed5f735ddb33da9325375a86))
* Allow closing of notification if status is already exists ([a25e6f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/a25e6f7b191a08e35f8b83d5911e9ac8bc9c11c8))
* Allow configuration of hastebin server in serverpackcreator.properties. ([0235378](https://git.griefed.de/Griefed/ServerPackCreator/commit/023537882243979fd7f2b66fc69113eb43477902))
* Be more specific with not found language key ([129877b](https://git.griefed.de/Griefed/ServerPackCreator/commit/129877bebe2691663cd7dc962b2bfd73f7dae796))
* Build for armv7 again thanks to [@djmaze](https://git.griefed.de/djmaze) and their dind-image-with-armhf available at https://github.com/djmaze/dind-image-with-armhf. Store and read version more efficiently by writing it to the manifest. ([d5bde7b](https://git.griefed.de/Griefed/ServerPackCreator/commit/d5bde7b7d2f0f073753b94c9f8a0e382d3280c6e))
* Change banner being displayed when running as webservice ([75899d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/75899d4d211647acf9de589007bfeaa88664cf23))
* Change groupID. Also change url for OSSRH to the correct snapshot url. ([e9ff899](https://git.griefed.de/Griefed/ServerPackCreator/commit/e9ff899023f5f2386653cf49e29dd9cea87ab99e))
* Change groupID. Also change url for OSSRH. Now everything works when ([0cf5dbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/0cf5dbccc8f40cf16e28a4011ede3264a7626076))
* Change labels for Minecraft, modloader and modloader version to better reflect new feature of selection from lists. ([84755a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/84755a185c02948050d0e534b2a5771898f13aff))
* Combine start and download scripts. Add checks for files in scripts. Removes option to generate scripts and generates them always instead. Closes issue [#81](https://git.griefed.de/Griefed/ServerPackCreator/issues/81) ([f037c34](https://git.griefed.de/Griefed/ServerPackCreator/commit/f037c34eb43b4910ea3002eba6362dd3d749261a))
* Copy log4j2.xml to basedir where JAR/EXE is executed. Improve logging-configuration and allow user to set level to DEBUG/INFO with '<Property name="log-level-spc">DEBUG</Property>' ([fcbe6cf](https://git.griefed.de/Griefed/ServerPackCreator/commit/fcbe6cfade911ee429bffd47b82cbe71b7f0d2bc))
* Create empty serverpackcreator.properties. Makes manual migrations by users more unlikely while at the same time reducing risk of users breaking SPC with misconfigurations ([98c9a70](https://git.griefed.de/Griefed/ServerPackCreator/commit/98c9a70f6cd7deed6a0705f8589cc964824d765b))
* Create modpacks downloaded from CurseForge in the work/modpacks-directory. ([3178326](https://git.griefed.de/Griefed/ServerPackCreator/commit/3178326cc960bde4482e847c5464ef4f50ed856c))
* DI serverpackcreator.properties everywhere! ([4b01d4a](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b01d4a809a08e420d399af9b9e58dca2c526002))
* Disbale whitelist for tempalte server.properties ([bc4018e](https://git.griefed.de/Griefed/ServerPackCreator/commit/bc4018edf2c33a240f4cdf7d9d1ad4378854c8ba))
* Display download button only if status is Available ([9c0edc7](https://git.griefed.de/Griefed/ServerPackCreator/commit/9c0edc71c4180725269d1a3ddcb7ca4958d89e4d))
* Display MB behind size ([1188b9f](https://git.griefed.de/Griefed/ServerPackCreator/commit/1188b9f0c687e3301e1e4d695450e0b5f1890f71))
* Do not directly access the ServerPackRepository ([ba4bf2c](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba4bf2c9e57a0b982913dce816724d6c32f78edb))
* Extract actions and events into separate methods. Improves maintainability. ([7a335da](https://git.griefed.de/Griefed/ServerPackCreator/commit/7a335dab87acbd4f136e520fb6c1af012659606d))
* Extract actions and events into separate methods. Improves maintainability. ([9268245](https://git.griefed.de/Griefed/ServerPackCreator/commit/9268245df88d96fbe358b68de488992e102d448c))
* Finish TODOs. Setup missing lang keys. Minor improvements to tests ([b884e7a](https://git.griefed.de/Griefed/ServerPackCreator/commit/b884e7a77469135a5e3eb0bf56c44fb1249d7f76))
* Gather information from CurseForge modpack from JsonNodes instead of Class-mapping. Makes maintenance and expansion easier. Reduces complexity. ([caa033b](https://git.griefed.de/Griefed/ServerPackCreator/commit/caa033bae0d54a5e7171871ea7023e99fc5c99a0))
* Generate server packs in ./server-packs in the directory where ServerPackCreator is executed in. Prevents 1. in [#55](https://git.griefed.de/Griefed/ServerPackCreator/issues/55) where the Overwolf CurseForge App filewatcher can cause installed mods to disappear due to copying mods around inside the modpack directory. ([539341d](https://git.griefed.de/Griefed/ServerPackCreator/commit/539341d68f54965b958d74e11e7e9fcc31da9ada))
* Improve automatic acquisition of java path from system environment. ([fae311e](https://git.griefed.de/Griefed/ServerPackCreator/commit/fae311ea2e5f0c38c7caec7a06d06ed43957eae5))
* Improve configuration check and tests. Add more debug logging. Add tests. ([b6da489](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6da489e08da8a20074f32ae938658649b982f3e))
* Improve debug logging for VersionLister ([29be15f](https://git.griefed.de/Griefed/ServerPackCreator/commit/29be15fa5ba18ce8bdb0f4345e989ef843a63e75))
* Improve dialog after uploading config and logs to hastebin ([da5e298](https://git.griefed.de/Griefed/ServerPackCreator/commit/da5e2981333806adf93f63bb549a48cb5d1e91b3))
* Improve dialog after uploading config and logs to hastebin ([13f4587](https://git.griefed.de/Griefed/ServerPackCreator/commit/13f4587e736743ae9217a12562077bcaeb33023b))
* Improve error handling and reporting ([77985b6](https://git.griefed.de/Griefed/ServerPackCreator/commit/77985b6f23fa95d388b349a016d090a480a869aa))
* Improve update checks by sequentially checking GitHub, GitGriefed and then GitLab ([c25eaac](https://git.griefed.de/Griefed/ServerPackCreator/commit/c25eaacd6767b721a7624847f40dd3639c7f7430))
* Initialize addons and check/create files when creating our DefaultFiles and AddonsHandler instances. ([864f10c](https://git.griefed.de/Griefed/ServerPackCreator/commit/864f10cd33e7f06693e47791ceeb7ac9a9e16974))
* Instantiate CreateGui only when GUI is actually about to be used ([d39730c](https://git.griefed.de/Griefed/ServerPackCreator/commit/d39730c86c9e8726716d2f6a4ca15bba3743ad5a))
* Just kill it. ([b6bbe54](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6bbe54ad03f89505350e9714af2d65ef6fec1fb))
* Just some renamings...nothing important. ([2c65582](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c65582691abf06558deaf4461c90265770bb6d1))
* Merge checkJavaPath and getJavaPathFromSystem ([0c982cb](https://git.griefed.de/Griefed/ServerPackCreator/commit/0c982cb5abd629e21fbc23c08b0a76240a4ea11f))
* Modloader setting as a slider to select either Forge of Fabric ([4f9eb79](https://git.griefed.de/Griefed/ServerPackCreator/commit/4f9eb79f813d3f127d89d99151163f3186dabcf9))
* More work towards allowing parallel runs of server pack generation. Split Configuration into ConfigurationModel and ConfigurationHandler ([cb3e8a7](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb3e8a79e86c023a35d5224a5f31b1539903c59e))
* Move assignemts to field declaration where applicable. Extract method for adding MouseListeners to buttons. ([b37ad30](https://git.griefed.de/Griefed/ServerPackCreator/commit/b37ad30ce88e570e4b8632760dee5cebab28f8da))
* Move helper and utility methods to separate classes. Reorganize code. More and improved unit tests. Add a little info text to start scripts for Minecraft 1.17+ as well as print of Java version ([e41e97c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e41e97c1e31dd05aba19b5b429491d013401020a))
* Move language specification from lang.properties to serverpackcreator.properties. Move FALLBACKSMODSLIST to serverpackcreator.properties. ([bb11972](https://git.griefed.de/Griefed/ServerPackCreator/commit/bb119727113ba0cb8e58977348673860bcb47851))
* Move ObjectMapper init to getter like in ConfigurationHandler ([d73ebd4](https://git.griefed.de/Griefed/ServerPackCreator/commit/d73ebd40e3a77dc512bd4f542eb5780fa9663a3a))
* Move ObjectMapper init to getter like in ConfigurationHandler ([ac955c5](https://git.griefed.de/Griefed/ServerPackCreator/commit/ac955c520f434fba1dedaf0299213f6b85489709))
* Move plugins folder creationf to DefaultFiles. Create example file for disabling plugins. Improve logging for installed plugin extensions. ([1fad8ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/1fad8ac858377c43250d4f6f644ecf7c719c7e02))
* Move script creation to separate methods and refactor write.write() to increase readability and maintainability. Also, move info regarding EULA agreement. ([65121a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/65121a2a8e7adaac47c25e2b08498b7b6cbb61d7))
* Only check for database existence when running as a webservice ([87618f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/87618f4f99d9376de0dd5ffc135265fec35cebef))
* Only provide translations for messages which actually have a need for translation. Error/debug messages mainly do not need to be translated, as those will be reported in issues, therefore I need to be able to read them. ([2132baa](https://git.griefed.de/Griefed/ServerPackCreator/commit/2132baa6a19000ffdabec555a3e3bca5c8fc0708))
* Prevent going through a list of clientside-only mods automatically gathered from modpack is property is false. ([51a3e42](https://git.griefed.de/Griefed/ServerPackCreator/commit/51a3e42ea18e37453734c5cc6c4e2e63fea8bfee))
* Print server-icon and server.properties paths. Re-organize method in CreateServerPackTab to ensure GUI becomes responsible again if the generation of a server pack fails. ([e42b3b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/e42b3b1aaac9845bbf053d49705b8cb044eb3c07))
* Provide improved Fabric Server Launcher as well as old launcher. Create SERVER_PACK_INFO.txt with information about said improved launcher. Thanks to @TheButterbrotMan for the detailed conversations in issue [#202](https://git.griefed.de/Griefed/ServerPackCreator/issues/202) ([6148a3e](https://git.griefed.de/Griefed/ServerPackCreator/commit/6148a3eca54543171d3c63f8336b4a01acc2f407))
* Rearrange some fields ([4592b70](https://git.griefed.de/Griefed/ServerPackCreator/commit/4592b7041a130204a8847e775cc077ab8c64c498))
* Refactor lang keys to better reflect where they're used. Add more lang keys for logging. Improve wording. Fix some minor typos. ([354fb2e](https://git.griefed.de/Griefed/ServerPackCreator/commit/354fb2e7003df6293ebb496c22d085493eb868c5))
* Refactor lang keys to better reflect where they're used. Add more lang keys for logging. Improve wording. Fix some minor typos. ([9553557](https://git.griefed.de/Griefed/ServerPackCreator/commit/9553557d40a129194c3b2fd478b83805f35b0805))
* Refactor tailers to run in threads. ServerPackCreator can still become unresponsive if you resize during zip-creation, after a Forge server was installed, though..... ([d4c986e](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4c986eaa2451989420fa9785fab6f86523c8755))
* Remove elements starting with ! from list instead of avoiding them with an ugly if-statement ([b8c84e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/b8c84e1294d7e8feebd34a0da202f8dc60d02d78))
* Remove preparations for 1.12 and older clientside autodetection. See https://github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692 ([3638e22](https://git.griefed.de/Griefed/ServerPackCreator/commit/3638e22dd96cea72ec86d22f7c16d335eefa9bf0)), closes [/github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692](https://git.griefed.de/Griefed//github.com/Griefed/ServerPackCreator/issues/62/issues/issuecomment-901382692)
* Remove preparations for 1.12 and older clientside autodetection. See https://github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692 ([4977ae7](https://git.griefed.de/Griefed/ServerPackCreator/commit/4977ae7f01db82b79b1af0057e505877e4307ad9)), closes [/github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692](https://git.griefed.de/Griefed//github.com/Griefed/ServerPackCreator/issues/62/issues/issuecomment-901382692)
* Remove/extract commonly used fields and methods. Make sure our database is always present. Other. ([859ede1](https://git.griefed.de/Griefed/ServerPackCreator/commit/859ede176db6ae995c72405b95c584de298300ef))
* Remove/extract commonly used fields and methods. Work towards webservice ([abf0135](https://git.griefed.de/Griefed/ServerPackCreator/commit/abf01355447f0c3a0af4af97d1cac259ddc113fd))
* Remove/extract commonly used fields/methods ([1f40517](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f405176a505bfcb5932493f94924bf45e2ade19))
* Remove/extract commonly used fields/methods ([df84569](https://git.griefed.de/Griefed/ServerPackCreator/commit/df845695059550025d0f24326d69a9f7ebf3d9f4))
* Remove/extract commonly used fields/methods ([c9cc954](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9cc9548973d7b181ff91175ac1bd5959740c81f))
* remove/extract commonly used fields/methods. Use configurationModel for everything. ([4ea254f](https://git.griefed.de/Griefed/ServerPackCreator/commit/4ea254fcf3aa6503efb8a168d54346af45f93150))
* Rename and sort classes and packages to make more sense. ([5ca227d](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ca227d79a0dfcb40effe9eb344da9575cf8e9bc))
* rename applicationProperties field ([533c850](https://git.griefed.de/Griefed/ServerPackCreator/commit/533c850300e6dfa17fa6607bc2ae738e45a22b78))
* Rename applicationProperties field ([781e1cd](https://git.griefed.de/Griefed/ServerPackCreator/commit/781e1cdedfc303f933bea618b72a404e258b5027))
* Rename fields still referencing old serverPackCreatorProperties to applicationProperties ([e1b7c62](https://git.griefed.de/Griefed/ServerPackCreator/commit/e1b7c6254a710f5f2a3436090782f079d1f433e4))
* Replace e.getStateChange() with ItemEvent.SELECTED. ([ab87c06](https://git.griefed.de/Griefed/ServerPackCreator/commit/ab87c06ea99443fa6856a152fd15d07fdd395c4e))
* Replace file-saver with call to api. Improves downloading of server packs. ([b60aeb7](https://git.griefed.de/Griefed/ServerPackCreator/commit/b60aeb7ddbb8b1f3354cae2313136c7a193fc917))
* Replace name or property-file to correct one ([ee0aab7](https://git.griefed.de/Griefed/ServerPackCreator/commit/ee0aab7a3fec9a3828e4248877bf1f968dc151c2))
* Replace slider for modloader selection with radio buttons. Looks better and cleaner. Selection fires less events than slider did. ([c36189c](https://git.griefed.de/Griefed/ServerPackCreator/commit/c36189cf5252e0fe27701e779f6e539b1d79a335))
* Require file passed to CreateServerPack.run in order to generate server pack. Create new Configuration object with said file. Should allow parallel runs in the future, but needs to be tested when I get to that. ([67c0cba](https://git.griefed.de/Griefed/ServerPackCreator/commit/67c0cba498dece33f265c376c88cbe4b3ac6e77a))
* Reverse lists of modloader versions to display in order of newest to oldest versions. Closes issue [#74](https://git.griefed.de/Griefed/ServerPackCreator/issues/74). ([4534d87](https://git.griefed.de/Griefed/ServerPackCreator/commit/4534d8774056f9de3d2063ea130c7bd85a4a6137))
* Rework checkConfiguration to provide more ways of checking a given configuration. Require checks to run before passing to run(...). ([a3ecd11](https://git.griefed.de/Griefed/ServerPackCreator/commit/a3ecd11c58cf044c58d1f39c0b62bc30a729e189))
* Rework error redirect. ([85543ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/85543ac9f6fc7385c0e634fa60c78cec4e289c01))
* Rewrite unzipping of CurseForge acquired modpack with zip4j library ([9f8c87f](https://git.griefed.de/Griefed/ServerPackCreator/commit/9f8c87fca09beb239030b4228958a0e52c0d83c1))
* Set clientMods and javaPath with fallback-list and system environemnt respectively, if the config is empty or an invalid javaPath was specified. ([ff18c5e](https://git.griefed.de/Griefed/ServerPackCreator/commit/ff18c5e56f1416316a20158f66ce9f24c1ff7cd5))
* Set logger context with log4j2.component.properties ([7038dcf](https://git.griefed.de/Griefed/ServerPackCreator/commit/7038dcf76e61ca4adf85a2d842f4cdeafbc409e7))
* Set rate at which tailers tail to 100ms, instead of 2000ms. ([ba4624f](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba4624f9116f248ac5953e90c1209b50990c4155))
* Set server-packs directory to /server-packs. Add new configuration to config. Add volume to Dockerfile. Update documentation in README ([267e3e9](https://git.griefed.de/Griefed/ServerPackCreator/commit/267e3e9f168803209e26f8038a4c14d16d30b920))
* Set status to Queued for a new instance ServerPack ([e2eb166](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2eb166e31a3a26a145283b68242c996cff65884))
* Simplify default files setup by merging methods which create our files. Instead of a separate method for each file, we have one method which gets passed different parameters depending on which file we want to setup. Makes maintenance easiert and code easier to read. ([9111e7c](https://git.griefed.de/Griefed/ServerPackCreator/commit/9111e7c58508700b31efeb617f110bae9a8b9f7f))
* Simplify log tabs to increase maintainability. Abstract classes rock! ([7fc3404](https://git.griefed.de/Griefed/ServerPackCreator/commit/7fc3404df9577c15493c6b98905792e0860c5ecd))
* Simplify server installation to increase maintainability ([7bec08a](https://git.griefed.de/Griefed/ServerPackCreator/commit/7bec08a7e774f2935d34933b95b4624677e27737))
* Sort by downloads, descending ([2f6f6d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f6f6d4578b2bf5429fd2b85291850b292766e50))
* Store Fabric installer manifest in work/*. Only refresh when SPC starts. Don't delete manifest files during runs of SPC. Rename lang keys to fit usage. Other misc changes. ([1927faa](https://git.griefed.de/Griefed/ServerPackCreator/commit/1927faa33da1063ba4eea239cabcf9c6a4335b8d))
* Store Minecraft, Fabric and Forge version validation in work/*. Only refresh them when SPC starts. Setup work, work/temp and server-packs folder for future use. ([ab080a6](https://git.griefed.de/Griefed/ServerPackCreator/commit/ab080a6024138972c0b34524c4c7a728c64b8f74))
* Switch back to old pattern format so GUI looks clean again ([483bdc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/483bdc15fedcf1db513b41169affda85a99cd0b4))
* Switch options to YES_NO to ensure users is always warned about empty javapath setting if they did not choose to select it now. ([c6f4ef8](https://git.griefed.de/Griefed/ServerPackCreator/commit/c6f4ef8cfc5e138191079acbf773ab91cef0d091))
* Throw custom exceptions on incorrect IDs ([875817c](https://git.griefed.de/Griefed/ServerPackCreator/commit/875817c7ee2ea024c631b9a37794feb690e434cd))
* Upgrade to Gradle 7.2. Remove Fabric-Installer dependecy by retrieving the Minecraft server url ourselves. ([e297f63](https://git.griefed.de/Griefed/ServerPackCreator/commit/e297f6347e393359ac71b0a70c388afd759355a8))
* Use a single ExtensionFactory as per pf4j docs ([62ed8e7](https://git.griefed.de/Griefed/ServerPackCreator/commit/62ed8e76fac1d3b28df557da89d39e1f166ca14a))
* Use FIleUtils for copying ([4529017](https://git.griefed.de/Griefed/ServerPackCreator/commit/452901776346acf5318b5629367e1e3f75b2317f))
* Use FIleUtils for copying and deleting, Files for deleting files. Replace messages with lang keys ([186d610](https://git.griefed.de/Griefed/ServerPackCreator/commit/186d6107e799fda23ea6259382d6fda261eaa253))
* Use FIleUtils for copying, Files for deleting ([4459847](https://git.griefed.de/Griefed/ServerPackCreator/commit/4459847bfc94117773605e07a6dc26e6716a8c51))
* When a requested server pack already exists, offer a download to the user. ([39dc626](https://git.griefed.de/Griefed/ServerPackCreator/commit/39dc6268e8ebd1048c0e19c0a479bd731c8d1e98))
* **webservice:** Allow user to specify mode. Test whether libatomic1 works now. ([6dfa0dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/6dfa0dcf44652910c83ce8b269929893aa04a4b3))
* **webservice:** Display status as "Generating" if server pack is being generated. Refactor regeneration to use queueing-system. ([78b88f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/78b88f22b18ba87723d3808586b496abcc3ab25e))
* **webservice:** Move ScanCurseProject and GenerateCurseProject to separate classes to eliminate statics. Closes GL[#88](https://git.griefed.de/Griefed/ServerPackCreator/issues/88) ([5815eb7](https://git.griefed.de/Griefed/ServerPackCreator/commit/5815eb7e8dd2abc7a0cdc2287e950b2f0bb2e683))
* **webservice:** Remove unnecessary logging ([a619997](https://git.griefed.de/Griefed/ServerPackCreator/commit/a6199977958c4040657976750d9093bf6922cb4f))
* **webservice:** Set download-filename to fileDiskName + _server_pack-zip ([e597dc4](https://git.griefed.de/Griefed/ServerPackCreator/commit/e597dc4804896d971951f183e09a585a8943a956))
* **webservice:** Set initial rows per page to 13 ([e45cf0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/e45cf0e21a0b535f06358aa37016b3c8d38590a6))
* **webservice:** Set logging pattern for Spring to ours ([4348f76](https://git.griefed.de/Griefed/ServerPackCreator/commit/4348f7601b5d2818b0bd343e2f0cb33cab02e2ec))
* **webservice:** Store size in MB and display size in frontend in MB ([37d4daa](https://git.griefed.de/Griefed/ServerPackCreator/commit/37d4daa3e2863ab6077174d9249478c0ea179a1a))


### ⏩ Performance

* Improve project- and filename acquisition by checking project and files directly ([f6e7b54](https://git.griefed.de/Griefed/ServerPackCreator/commit/f6e7b5454e316ad3f7acb0958d69476e3dcbf163))
* Perform version checks with lists gathered by VersionLister. ([d440e5e](https://git.griefed.de/Griefed/ServerPackCreator/commit/d440e5e2c079ac44bc040d87cacb1f29951160d9))
* Retrieve Forge versions from HashMap with Minecraft version as key instead of re-reading list and lists and arrays of data again and again and again, ([0018abc](https://git.griefed.de/Griefed/ServerPackCreator/commit/0018abc4772b7e062fc5bd131a62edcceae4aac6))


### 👀 Reverts

* Do not create the eula.txt-file automatically. Reverts feature request issue [#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83). Lots of other smaller things, too many to list. ([ae66641](https://git.griefed.de/Griefed/ServerPackCreator/commit/ae66641b4e66e4711069289c79427651d10aaf11))
* Maybe another time ([f7ea248](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7ea248f50ef2dbbdc99fa4538c9561d35e96ea7))
* Re-implement removal and change of new entries to copyDirs and clientMods ([eec45d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/eec45d5950b088625760187b070bace44940d57e))


### 💈 Style

* Declare fields above constructor. Only have methods under constructor. ([76c6b58](https://git.griefed.de/Griefed/ServerPackCreator/commit/76c6b584b05d48adf0714f4ad066c6cf0f5d775a))
* Reorder calls in Main.main to reflect importance. Makes it slightly more readable as well. ([576cbae](https://git.griefed.de/Griefed/ServerPackCreator/commit/576cbae9938563ef50dd27f174b3f340c4998f60))


### 💎 Improvements

* **Clientside Mods Help:** Expand help text for clientside only mods with a more detailed explanation of possible ways of configuration ([2ba30ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/2ba30ea8c6727e24f89e133d8dc929fcbefa2228))
* **Strings:** Strengthen configuration checks by making sure some strings do not contain backslashes. Make server pack suffix more secure by removing illegal characters. ([04b76c9](https://git.griefed.de/Griefed/ServerPackCreator/commit/04b76c93b6dd1955440b247f3542d0729d4af7b7))


### 📔 Docs

* Add author tags. Add link to GitHub issues in case anyone wants something added to fallbackModslist or directories for CurseForge automation. ([7699c64](https://git.griefed.de/Griefed/ServerPackCreator/commit/7699c64d4f7d14f3d13b86acb92489c1c0ba2a33))
* Add call to initializeAddons to main description ([ac14f99](https://git.griefed.de/Griefed/ServerPackCreator/commit/ac14f996a55402d1d5b8cc8930bbb1ead57852e7))
* Add documentation for UpdateChecker utility. ([f804589](https://git.griefed.de/Griefed/ServerPackCreator/commit/f8045896d075fc67d0befa1565e88ddd1a831ba5))
* Add javadoc for scanAnnotations ([e0a08f9](https://git.griefed.de/Griefed/ServerPackCreator/commit/e0a08f9547891a2807fd20a89927856b2a86329d))
* Add missing method to table ([d1fca12](https://git.griefed.de/Griefed/ServerPackCreator/commit/d1fca12b00b8b79cf0ede59d58295eeb61a80c5c))
* Add missing method to table ([f04b728](https://git.griefed.de/Griefed/ServerPackCreator/commit/f04b72818257e1d71b2e60dd86af8921c32e45eb))
* Add missing parameter to setJavaArgs ([761e2fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/761e2fdcc110e96db825527471c60cc427078552))
* Add missing throws ([4538f54](https://git.griefed.de/Griefed/ServerPackCreator/commit/4538f547b291d5b02619e3f366ab53fff63726e9))
* Change version dropdown to input ([c5a5893](https://git.griefed.de/Griefed/ServerPackCreator/commit/c5a589358382085c7cf416f3608150bd012998bb))
* Cleanup changelog due to some sort of tag issue I created. Yay. ([17c234b](https://git.griefed.de/Griefed/ServerPackCreator/commit/17c234bfbe56760cefd07bf98b3e7357f8167a55))
* Cleanup changelog due to some sort of tag issue I created. Yay. ([65bf366](https://git.griefed.de/Griefed/ServerPackCreator/commit/65bf366c368f13de51f2f8963d7c3ce9ecbc954b))
* Do not display the version *dev* in the title of the Java documentation ([124c19f](https://git.griefed.de/Griefed/ServerPackCreator/commit/124c19f4ac5fe2b6cd291c308890a1338ccf7d6d))
* Don't include private methods in documentation ([719b4f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/719b4f2e8aec75075fda349383a305cce8aebf1a))
* Exclude certain classes from JaCoCo test coverage ([1f4cfbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f4cfbc73bdcd9267bb9e56e8bbd95ff7a8b1866))
* Generate patch release on docs change. ([d6e65ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/d6e65eadb8e5c5071d8b8a693433ae7e38aa2582))
* List minigame example addon ([3577d33](https://git.griefed.de/Griefed/ServerPackCreator/commit/3577d33dae6cc895d3fbb97f57d9bcc4b716ecc2))
* List server-packs directory for volumes ([82b13e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/82b13e43771a2964d1d6339994dd431e94701a67))
* Name correct filename for properties according to merge of lang.properties with serverpackcreator.properties ([ed42dcd](https://git.griefed.de/Griefed/ServerPackCreator/commit/ed42dcd14479013e979f9793aae884b0c0cf1836))
* Spelling and grammar fixesas well as [@author](https://git.griefed.de/author) tag fixes. ([9d157d6](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d157d6227ac3c484b740297c012f817c169abde))
* Update CONTRIBUTING with step-by-step guide on how to contribute to ServerPackCreator ([db3b061](https://git.griefed.de/Griefed/ServerPackCreator/commit/db3b06100510d2a2e35c0ce92cbf6c04d01c6b1f))
* Update licenses ([21ae0ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/21ae0ad3f704b997ac4823a447fbeae1c9bbe1a1))
* Update README with info regarding contributions. Closes GL[#75](https://git.griefed.de/Griefed/ServerPackCreator/issues/75). ([e3d499c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3d499cf948f58084ee2afd8568bdb50ba483d3a))
* Update README with new feature information and reflect changes made to file-structure ([04ffed5](https://git.griefed.de/Griefed/ServerPackCreator/commit/04ffed5e30c450520132d984e0c2974cafc777d1))
* Update README with new feature information and reflect changes made to file-structure ([b3f211c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b3f211cf51abd589672fe3005f0cfc9ef76cec76))
* Update table of methods ([dabf028](https://git.griefed.de/Griefed/ServerPackCreator/commit/dabf02866d58a72159642452c46b3ca6f109791a))
* Update table of methods for classes ([eeb6887](https://git.griefed.de/Griefed/ServerPackCreator/commit/eeb6887e3b52f67dd431adfe997ce1c144ab28fc))
* Update templates ([9fe1101](https://git.griefed.de/Griefed/ServerPackCreator/commit/9fe11013ba346443124d5c2cadb1364e4633cef7))
* Write docs for all the REST API classes, methods etc. I've been working on for the last couple of weeks. This commit also contains some minor refactorings, but nothing major or worth a separate commit. ([26519a0](https://git.griefed.de/Griefed/ServerPackCreator/commit/26519a002538bc01de17ad6debbb45d334527694))
* Write documentation for fabric-server-launch replace method ([7ab20eb](https://git.griefed.de/Griefed/ServerPackCreator/commit/7ab20eb47a2149271cf461dba0d0f0a0b1ad40d5))
* Write missing documentation for getters and setters for javaargs and javapath settings ([f29924b](https://git.griefed.de/Griefed/ServerPackCreator/commit/f29924bd00724b53669c51829b1497810b8596fb))
* **package-info:** Provide package information for all our packages giving more information about their purpose. ([2f420eb](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f420eb99c067b68b3da2b76b74eaa8ecc30d43f))
* **webservice:** Enable debug log output for Docker build ([eaae701](https://git.griefed.de/Griefed/ServerPackCreator/commit/eaae701fb7d5666251a07f93a8bcd67fa4785b3a))


### 🦊 CI/CD

* Add signing and publishing. Will be published to GitLab, GitHub, git.griefed, OSSRH on new tag creation. ([b60a8f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/b60a8f2a63c986eb609975f8299719aa9f731e32))
* Pass host for git clone so we can always clone from the infrastructure we are running on ([faa937a](https://git.griefed.de/Griefed/ServerPackCreator/commit/faa937ae750941fce8c52b8434a82ada816de932))
* Replace Typesafe with Nightconfig, allowing for more safety measures ([b9939b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/b9939b101e906b7a578794cf79659c5035e9c692))
* Switch to GHCR images to prevent job failures due to rate limiting by DockerHub ([bbe0c0b](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbe0c0b7e7db49189e22bcb2f2b1f55d083be6fa))
* Switch VersionChecker to library implementation. Update jms-server. Minor URL refactorings in gradle publishing. ([62c438a](https://git.griefed.de/Griefed/ServerPackCreator/commit/62c438a75d5a783d741fbacfc8c0861899892f69))
* Update dependencies ([e726f31](https://git.griefed.de/Griefed/ServerPackCreator/commit/e726f316c5928856a7b911be92d910f2ea6e6d26))
* Update dependencies. Cleanup & readability. ([fe583aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe583aa0f73326b328f2c672859053fe6c6b8b67))
* Update frontend dependencies ([d953f31](https://git.griefed.de/Griefed/ServerPackCreator/commit/d953f31dbc75f0006b34445a20e074fbc698f9bc))
* Update Gradle to 7.3 ([5dafa9e](https://git.griefed.de/Griefed/ServerPackCreator/commit/5dafa9ee7e7e6ee8beb2126296fed1853eb5f978))
* Update gradle to 7.3.1 ([88c1330](https://git.griefed.de/Griefed/ServerPackCreator/commit/88c133060f88303a6e734275c01704bb8ec4f782))
* Update Gradle to 7.3.3 ([541122b](https://git.griefed.de/Griefed/ServerPackCreator/commit/541122b0dded68e62878065bea3ea47aee55d1f5))
* Update griefed/baseimage-ubuntu-jdk-8 to 2.0.1 ([d77a61f](https://git.griefed.de/Griefed/ServerPackCreator/commit/d77a61f7e1cfd874f5ec9df05c1c56737bfd30ed))
* Upgrade dependencies ([426ec44](https://git.griefed.de/Griefed/ServerPackCreator/commit/426ec440b54ff9909d202bbdfe697d1259d7773a))
* **deps-dev:** bump @babel/eslint-parser in /frontend ([a0629ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/a0629eadd4b21b204ba2caf1732c69b8c0315415))
* **deps-dev:** bump @quasar/app from 3.2.3 to 3.2.5 in /frontend ([4d2092b](https://git.griefed.de/Griefed/ServerPackCreator/commit/4d2092bb73fe18589b5e150deebf7844c01c2198))
* **deps-dev:** bump @quasar/app from 3.2.5 to 3.2.6 in /frontend ([c53aeac](https://git.griefed.de/Griefed/ServerPackCreator/commit/c53aeac47f2b3fe0621e4abce2b89b3daf58e4d8))
* **deps-dev:** bump @quasar/app from 3.3.2 to 3.3.3 in /frontend ([ff176bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/ff176bd3bc1e844be1b6e2eea0f578cd7cc3ddc4))
* **deps-dev:** bump @types/node from 16.11.10 to 16.11.11 in /frontend ([043414e](https://git.griefed.de/Griefed/ServerPackCreator/commit/043414ebed40dadf28ddb888276c1d8ca47835e5))
* **deps-dev:** bump @types/node from 16.11.10 to 16.11.12 in /frontend ([ddd4424](https://git.griefed.de/Griefed/ServerPackCreator/commit/ddd44242048537fe22b3c2c3344a82884507c5c7))
* **deps-dev:** bump @types/node from 16.11.14 to 17.0.2 in /frontend ([d8109a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/d8109a55fd012cc8e376d47e46ee768040174b28))
* **deps-dev:** bump @types/node from 17.0.10 to 17.0.17 in /frontend ([dade4db](https://git.griefed.de/Griefed/ServerPackCreator/commit/dade4db41c2dccfc6db0ebf3752cd845cea88ba3))
* **deps-dev:** bump @types/node from 17.0.17 to 17.0.19 in /frontend ([8ae217b](https://git.griefed.de/Griefed/ServerPackCreator/commit/8ae217bf2b621f060b77d18b13f696c2c770e890))
* **deps-dev:** bump @types/node from 17.0.19 to 17.0.21 in /frontend ([43834fe](https://git.griefed.de/Griefed/ServerPackCreator/commit/43834fefc1c84b63d4eba4dc3ca74898953801b0))
* **deps-dev:** bump @types/node from 17.0.2 to 17.0.5 in /frontend ([0ae1140](https://git.griefed.de/Griefed/ServerPackCreator/commit/0ae11401030687941c00f0bf5f4696c6af4ec036))
* **deps-dev:** bump @types/node from 17.0.5 to 17.0.7 in /frontend ([9d66fc3](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d66fc3c153118d8e6555b4093d58574b6729fa1))
* **deps-dev:** bump @types/node from 17.0.5 to 17.0.8 in /frontend ([ea1383c](https://git.griefed.de/Griefed/ServerPackCreator/commit/ea1383c2bcbc60b889d262778d89d75002c86cdc))
* **deps-dev:** bump @types/node from 17.0.8 to 17.0.9 in /frontend ([a642a14](https://git.griefed.de/Griefed/ServerPackCreator/commit/a642a146fa2d2956970dc9daa01671c1b02a4873))
* **deps-dev:** bump @types/node from 17.0.9 to 17.0.10 in /frontend ([96e1d62](https://git.griefed.de/Griefed/ServerPackCreator/commit/96e1d6292a35016df0ef31bb41ed0cd1940c3cfb))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([925b5d2](https://git.griefed.de/Griefed/ServerPackCreator/commit/925b5d2eb9bec460ede155840e950482c41e5c11))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([38cfdde](https://git.griefed.de/Griefed/ServerPackCreator/commit/38cfdde75095c3b2cb50676a11811965cc3ab148))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([9cccb82](https://git.griefed.de/Griefed/ServerPackCreator/commit/9cccb82e522181a5017ac1c879ebde65e1f30dfc))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([f0c49fb](https://git.griefed.de/Griefed/ServerPackCreator/commit/f0c49fb9a5c1e6e25edf562f07a16cef023e2a87))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([f7bd184](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7bd18496b56250d00442c3f8c37aa75188ab0c0))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([3a7dffc](https://git.griefed.de/Griefed/ServerPackCreator/commit/3a7dffcd05f0610bea570e7253a96510927dca63))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([579714d](https://git.griefed.de/Griefed/ServerPackCreator/commit/579714df6f96a30796293f37dec76bc04273d647))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([55b5ba5](https://git.griefed.de/Griefed/ServerPackCreator/commit/55b5ba52f6b9c9377e730fd8d3ff0b25be52eca6))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([a04e32d](https://git.griefed.de/Griefed/ServerPackCreator/commit/a04e32dcbae32613130238cfcff0428274cb45db))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([2fdeec9](https://git.griefed.de/Griefed/ServerPackCreator/commit/2fdeec99954be7b1a9af3fd9239398ad0569ad8d))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([ca93040](https://git.griefed.de/Griefed/ServerPackCreator/commit/ca93040d6b76c1f538f66a8fd8ccdb118976b744))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([3795601](https://git.griefed.de/Griefed/ServerPackCreator/commit/3795601b23fd063c2ffd05d38754725bdc24a8f2))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([29466f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/29466f2d9aa89935e20ef96184eae95b34329f84))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([94c6af4](https://git.griefed.de/Griefed/ServerPackCreator/commit/94c6af47d56f3606fdd142697ecd05527fa9adaf))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([d06b4cd](https://git.griefed.de/Griefed/ServerPackCreator/commit/d06b4cd7559dea9eefd686a189ceb22ece256320))
* **deps-dev:** bump eslint from 8.10.0 to 8.11.0 in /frontend ([66c8700](https://git.griefed.de/Griefed/ServerPackCreator/commit/66c8700a345a54d70084dadb413f6e62593a089d))
* **deps-dev:** bump eslint from 8.5.0 to 8.6.0 in /frontend ([9698f98](https://git.griefed.de/Griefed/ServerPackCreator/commit/9698f98650490b0126467cfadf0ee7320ccd180a))
* **deps-dev:** bump eslint from 8.6.0 to 8.7.0 in /frontend ([f80efe5](https://git.griefed.de/Griefed/ServerPackCreator/commit/f80efe5c4457fb35367814556774e8e363f25d92))
* **deps-dev:** bump eslint from 8.7.0 to 8.9.0 in /frontend ([9268eb9](https://git.griefed.de/Griefed/ServerPackCreator/commit/9268eb9dbacf4903d24152e72774397c71f95b1f))
* **deps-dev:** bump eslint-config-prettier in /frontend ([0692bf8](https://git.griefed.de/Griefed/ServerPackCreator/commit/0692bf815373976cc0c67812a158050a1fb1cb6d))
* **deps-dev:** bump eslint-config-prettier in /frontend ([e3f70e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3f70e1dd7cbd9433b009fc7b6ff690d111cc5f5))
* **deps-dev:** bump eslint-plugin-vue from 8.2.0 to 8.3.0 in /frontend ([f9f3e48](https://git.griefed.de/Griefed/ServerPackCreator/commit/f9f3e48ca2a775f8161bc83bb2fc380d68bdfee2))
* **deps:** bump @quasar/cli from 1.2.2 to 1.3.0 in /frontend ([3d39571](https://git.griefed.de/Griefed/ServerPackCreator/commit/3d39571341e6755707904d8b19c44b85ff37d59d))
* **deps:** bump @quasar/extras from 1.12.1 to 1.12.2 in /frontend ([bf9f871](https://git.griefed.de/Griefed/ServerPackCreator/commit/bf9f871eb39c3a18e8f4c67bd44d5a1c4dfd68a5))
* **deps:** bump @quasar/extras from 1.12.2 to 1.12.3 in /frontend ([08590a7](https://git.griefed.de/Griefed/ServerPackCreator/commit/08590a7bc96ad03837081ecc8b4779c3a1696791))
* **deps:** bump @quasar/extras from 1.12.4 to 1.12.5 in /frontend ([465f083](https://git.griefed.de/Griefed/ServerPackCreator/commit/465f0833298c78aa51808e654243aa6d376d1741))
* **deps:** bump @quasar/extras from 1.12.5 to 1.13.0 in /frontend ([a9add11](https://git.griefed.de/Griefed/ServerPackCreator/commit/a9add1111d404935ea14219ae72fbad95629018a))
* **deps:** bump @quasar/extras from 1.13.0 to 1.13.1 in /frontend ([7e2fe46](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e2fe46061145338673180bffbb6dc8f37741bd1))
* **deps:** bump axios from 0.24.0 to 0.25.0 in /frontend ([c9b0734](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9b0734f51698a7349b6782bd7423b4ef9de7a92))
* **deps:** bump axios from 0.25.0 to 0.26.0 in /frontend ([02e8739](https://git.griefed.de/Griefed/ServerPackCreator/commit/02e8739e8548c4dec5973ab45bde5d7fc1e86e61))
* **deps:** bump axios from 0.26.0 to 0.26.1 in /frontend ([42dd920](https://git.griefed.de/Griefed/ServerPackCreator/commit/42dd92070f4ee12f527e7dba19d388ae3dce3768))
* **deps:** bump com.github.ben-manes.versions from 0.39.0 to 0.40.0 ([778e5e6](https://git.griefed.de/Griefed/ServerPackCreator/commit/778e5e6ff9a25c3af7853b771dda0b940cf3013b))
* **deps:** bump com.github.ben-manes.versions from 0.40.0 to 0.41.0 ([855c6e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/855c6e0a44232119c99ad028135083d817c98698))
* **deps:** bump com.github.ben-manes.versions from 0.41.0 to 0.42.0 ([6456e3f](https://git.griefed.de/Griefed/ServerPackCreator/commit/6456e3f211af4dda8f693c5f6222950b709032bb))
* **deps:** bump core-js from 3.19.1 to 3.19.3 in /frontend ([4864c13](https://git.griefed.de/Griefed/ServerPackCreator/commit/4864c13d9b2b7a7ffc979c54483803b54d445c44))
* **deps:** bump core-js from 3.20.0 to 3.20.1 in /frontend ([bbad029](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbad02947f1ad5462c46b418bb7d2d6c55bb3038))
* **deps:** bump core-js from 3.20.1 to 3.20.2 in /frontend ([f9c1068](https://git.griefed.de/Griefed/ServerPackCreator/commit/f9c10686b424e460fd1fefaa92e8230b637bb189))
* **deps:** bump core-js from 3.20.2 to 3.20.3 in /frontend ([2a4b86f](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a4b86f9f84cdc5c5b14479a7c016b0be8694309))
* **deps:** bump core-js from 3.20.3 to 3.21.0 in /frontend ([1862a3b](https://git.griefed.de/Griefed/ServerPackCreator/commit/1862a3b9f2eb08090bd62f30f677a3792f9cd8b5))
* **deps:** bump edu.sc.seis.launch4j from 2.5.1 to 2.5.2 ([59051b9](https://git.griefed.de/Griefed/ServerPackCreator/commit/59051b927831a6e09ee3b0a491b014c4d67a6034))
* **deps:** bump follow-redirects from 1.14.7 to 1.14.8 in /frontend ([2bde3af](https://git.griefed.de/Griefed/ServerPackCreator/commit/2bde3af44e9def9c685911a6097ef41f7a5ac78a))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.0 to 2.0.2 ([003e1a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/003e1a1d404b0c835394b787acaa321063a7b891))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.3 to 2.0.4 ([b560e65](https://git.griefed.de/Griefed/ServerPackCreator/commit/b560e65133acb81c21e8fc22d61215f97d991450))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.4 to 2.0.5 ([6bbacef](https://git.griefed.de/Griefed/ServerPackCreator/commit/6bbacef14f16213d42c3d2a83e0aeacc1837cb95))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.6 to 2.0.7 ([2acd8c0](https://git.griefed.de/Griefed/ServerPackCreator/commit/2acd8c06b783136c030ea6c540bff234b9ef0023))
* **deps:** bump JamesIves/github-pages-deploy-action ([c63a20d](https://git.griefed.de/Griefed/ServerPackCreator/commit/c63a20d71daec6684ed437857b7c6920859c34dc))
* **deps:** bump JamesIves/github-pages-deploy-action ([49cd567](https://git.griefed.de/Griefed/ServerPackCreator/commit/49cd567d7b9d0a68611b5771778a97e309bc80e8))
* **deps:** bump junit-platform-commons from 1.8.1 to 1.8.2 ([d8483f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/d8483f1d5767c0ec62d7bb12cfa4d4f476d3d62f))
* **deps:** bump log4j-api from 2.17.0 to 2.17.1 ([f243a62](https://git.griefed.de/Griefed/ServerPackCreator/commit/f243a626a7f8b956703807a83d12696a84a4b898))
* **deps:** bump log4j-api from 2.17.1 to 2.17.2 ([2984f46](https://git.griefed.de/Griefed/ServerPackCreator/commit/2984f46d7ce916705c9fc537ddeb1b13a3b4355a))
* **deps:** bump log4j-core from 2.17.0 to 2.17.1 ([1e579d2](https://git.griefed.de/Griefed/ServerPackCreator/commit/1e579d2c9a4b75327cb42f44c7e9b549edae614e))
* **deps:** bump log4j-core from 2.17.1 to 2.17.2 ([06c0dd6](https://git.griefed.de/Griefed/ServerPackCreator/commit/06c0dd6f2848484b9e97e3a441b8dcd893ce144a))
* **deps:** bump log4j-jul from 2.17.0 to 2.17.1 ([7c10e41](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c10e41c2085471c78849b08f1230089d170273b))
* **deps:** bump log4j-jul from 2.17.1 to 2.17.2 ([2f38947](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f38947cde6546126a08e1716174fd1d0ea70520))
* **deps:** bump log4j-slf4j-impl from 2.17.0 to 2.17.1 ([303e2da](https://git.griefed.de/Griefed/ServerPackCreator/commit/303e2dad816660947384df1f10ea69fbba27b7f5))
* **deps:** bump log4j-slf4j-impl from 2.17.1 to 2.17.2 ([0a9099b](https://git.griefed.de/Griefed/ServerPackCreator/commit/0a9099bdd28c88f2a8e040a9cc558bb82dc84cda))
* **deps:** bump log4j-web from 2.17.0 to 2.17.1 ([7a2ba8a](https://git.griefed.de/Griefed/ServerPackCreator/commit/7a2ba8ad49e1fe16d7733b8189fb5034a1cb0fe0))
* **deps:** bump log4j-web from 2.17.1 to 2.17.2 ([e76c049](https://git.griefed.de/Griefed/ServerPackCreator/commit/e76c049e05342c0aa3ff41076db12bd6ca36df5c))
* **deps:** bump org.springframework.boot from 2.6.2 to 2.6.3 ([8e02fa7](https://git.griefed.de/Griefed/ServerPackCreator/commit/8e02fa73374e600c55ac673f3a2502a6c8e1c4eb))
* **deps:** bump org.springframework.boot from 2.6.3 to 2.6.4 ([f9e0d1a](https://git.griefed.de/Griefed/ServerPackCreator/commit/f9e0d1af4051320b368eb31872881bc79759b334))
* **deps:** bump quasar from 2.3.3 to 2.3.4 in /frontend ([373fdb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/373fdb340ca949d61f51374f7e03685e18708f82))
* **deps:** bump quasar from 2.3.4 to 2.4.2 in /frontend ([bd3051c](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd3051c18690a09609b10ece95bf0500f73036c1))
* **deps:** bump quasar from 2.4.13 to 2.5.3 in /frontend ([1d2ca7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/1d2ca7e34726667131ccc87360c2b5eb5d96efa2))
* **deps:** bump quasar from 2.4.3 to 2.4.4 in /frontend ([904db5f](https://git.griefed.de/Griefed/ServerPackCreator/commit/904db5feb51353c8054b200c32a560106ac1e6ca))
* **deps:** bump quasar from 2.4.9 to 2.4.13 in /frontend ([ef5a18d](https://git.griefed.de/Griefed/ServerPackCreator/commit/ef5a18d2fb27deaac90a28020fc9ae24382ec5d5))
* **deps:** bump quasar from 2.5.3 to 2.5.5 in /frontend ([4e303bf](https://git.griefed.de/Griefed/ServerPackCreator/commit/4e303bf4b91cd86a820c5ec9765bc87015a3daf4))
* **deps:** bump quasar from 2.5.5 to 2.6.0 in /frontend ([8b35e1f](https://git.griefed.de/Griefed/ServerPackCreator/commit/8b35e1f5e2a759c6ac3e5cfd127747cc84f1ce2e))
* **deps:** bump spring-boot-devtools from 2.5.6 to 2.6.0 ([678e175](https://git.griefed.de/Griefed/ServerPackCreator/commit/678e1750ee6a29def7d52920b5699c0b7ed89322))
* **deps:** bump spring-boot-devtools from 2.6.0 to 2.6.1 ([a51e28e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a51e28e646c115cce8f784458e08a4d95197edb4))
* **deps:** bump spring-boot-devtools from 2.6.2 to 2.6.3 ([0fe0b42](https://git.griefed.de/Griefed/ServerPackCreator/commit/0fe0b42715808954bb722f22e222a6970ed8436e))
* **deps:** bump spring-boot-devtools from 2.6.3 to 2.6.4 ([077e0b4](https://git.griefed.de/Griefed/ServerPackCreator/commit/077e0b4a387c912de8990469f54bb238fdef05d4))
* **deps:** bump spring-boot-starter-artemis from 2.6.2 to 2.6.3 ([9038c21](https://git.griefed.de/Griefed/ServerPackCreator/commit/9038c21f85a327fc2355254d6ead68490a55aaa1))
* **deps:** bump spring-boot-starter-artemis from 2.6.3 to 2.6.4 ([7b651ca](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b651ca2bf8efbaf84b14d5465b8a739c74c2743))
* **deps:** bump spring-boot-starter-data-jpa from 2.5.6 to 2.6.0 ([dc8797a](https://git.griefed.de/Griefed/ServerPackCreator/commit/dc8797af78b505599e5f8fa7916c93030324fc52))
* **deps:** bump spring-boot-starter-data-jpa from 2.6.2 to 2.6.3 ([093ab09](https://git.griefed.de/Griefed/ServerPackCreator/commit/093ab091f40aca24e1501c47aa360735240f61fb))
* **deps:** bump spring-boot-starter-data-jpa from 2.6.3 to 2.6.4 ([74a7ada](https://git.griefed.de/Griefed/ServerPackCreator/commit/74a7adab6934bd62cd55a356ebc9cde1ec86a606))
* **deps:** bump spring-boot-starter-log4j2 from 2.5.6 to 2.6.0 ([5b67e52](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b67e52fd5c7783d8a08cd892ed6ef285d336836))
* **deps:** bump spring-boot-starter-log4j2 from 2.6.2 to 2.6.3 ([a4091bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/a4091bd83e8936c97bafeceba508df6692a6421b))
* **deps:** bump spring-boot-starter-log4j2 from 2.6.3 to 2.6.4 ([536134d](https://git.griefed.de/Griefed/ServerPackCreator/commit/536134d96b352227981c37215f72ce5336f6af27))
* **deps:** bump spring-boot-starter-quartz from 2.5.6 to 2.6.0 ([0433e90](https://git.griefed.de/Griefed/ServerPackCreator/commit/0433e905151ef0a60a2f8a00f5cd5587c4bf024c))
* **deps:** bump spring-boot-starter-quartz from 2.6.2 to 2.6.3 ([6dd76e3](https://git.griefed.de/Griefed/ServerPackCreator/commit/6dd76e3e3a635e6af613bfc3d437233518bdc9d8))
* **deps:** bump spring-boot-starter-quartz from 2.6.3 to 2.6.4 ([2211d4d](https://git.griefed.de/Griefed/ServerPackCreator/commit/2211d4d67bc7bc14097859a879e78270dcb80902))
* **deps:** bump spring-boot-starter-test from 2.5.6 to 2.6.1 ([0f39852](https://git.griefed.de/Griefed/ServerPackCreator/commit/0f398524acfbb7c01b9a404430ee35eba351ee84))
* **deps:** bump spring-boot-starter-test from 2.6.2 to 2.6.3 ([577b79a](https://git.griefed.de/Griefed/ServerPackCreator/commit/577b79a0a5dfe0fb082bf820ab84846a7645bb19))
* **deps:** bump spring-boot-starter-test from 2.6.3 to 2.6.4 ([f27d94d](https://git.griefed.de/Griefed/ServerPackCreator/commit/f27d94d6942a241bdf13948940c7c454921f020c))
* **deps:** bump spring-boot-starter-validation from 2.5.6 to 2.6.1 ([1473032](https://git.griefed.de/Griefed/ServerPackCreator/commit/14730327dae5a9d81df7caf3ce0e4d1a5f4fda88))
* **deps:** bump spring-boot-starter-validation from 2.6.2 to 2.6.3 ([516db6a](https://git.griefed.de/Griefed/ServerPackCreator/commit/516db6ad4207aa079aa350f2b5d8c10323e4a67b))
* **deps:** bump spring-boot-starter-validation from 2.6.3 to 2.6.4 ([9f66c07](https://git.griefed.de/Griefed/ServerPackCreator/commit/9f66c0711de1a88bdb555dcc15fe3c009b9df8de))
* **deps:** bump spring-boot-starter-web from 2.5.6 to 2.6.1 ([9d7ab8b](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d7ab8b0f024d1cc0f6f88ea5aa68ecbffbb545f))
* **deps:** bump spring-boot-starter-web from 2.6.2 to 2.6.3 ([7c49fd3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c49fd341694ae81cccf77ef9abadc33b15fb22a))
* **deps:** bump spring-boot-starter-web from 2.6.3 to 2.6.4 ([c017e87](https://git.griefed.de/Griefed/ServerPackCreator/commit/c017e87f03cdce98511b271ddf084fba372dee85))
* **deps:** bump tsparticles from 1.37.5 to 1.37.6 in /frontend ([7ab7a69](https://git.griefed.de/Griefed/ServerPackCreator/commit/7ab7a69446f71d8dd827a3b152cc54987946a88d))
* **deps:** bump tsparticles from 1.37.6 to 1.38.0 in /frontend ([d17900b](https://git.griefed.de/Griefed/ServerPackCreator/commit/d17900b81fb766bf6984c844e3ca3bd609194767))
* **deps:** bump tsparticles from 1.38.0 to 1.39.0 in /frontend ([75a3b00](https://git.griefed.de/Griefed/ServerPackCreator/commit/75a3b00e5119863a21f57207fbb1609a261ab2ee))
* **deps:** bump tsparticles from 1.39.1 to 1.41.0 in /frontend ([fa0cabc](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa0cabc745932f327ebd46cac4f76994797b941e))
* **deps:** bump tsparticles from 1.41.0 to 1.41.1 in /frontend ([469ce7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/469ce7e15bd3e993c159357d358e1830662922e9))
* **deps:** bump tsparticles from 1.41.1 to 1.41.4 in /frontend ([dc8440e](https://git.griefed.de/Griefed/ServerPackCreator/commit/dc8440e0ad9689c2336c7e72918d1e3e6e7ceb05))
* **deps:** bump tsparticles from 1.41.5 to 1.41.6 in /frontend ([ecb4eff](https://git.griefed.de/Griefed/ServerPackCreator/commit/ecb4effc29673172fd7c74bfb57a325b6b3c5f4f))
* **deps:** bump tsparticles from 1.41.6 to 1.42.2 in /frontend ([7c8b807](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c8b807935a3a0bc9780ee6df30ded68b8c18149))
* **deps:** bump versionchecker from 1.0.4 to 1.0.5 ([57f0dd0](https://git.griefed.de/Griefed/ServerPackCreator/commit/57f0dd0f03a9ca48fe7fde7bec144c0e7136a3a8))
* **deps:** bump vue from 3.2.22 to 3.2.24 in /frontend ([62d687a](https://git.griefed.de/Griefed/ServerPackCreator/commit/62d687a0ffccc248c4ae0f89168ce18e3e47fabf))
* **deps:** bump vue from 3.2.26 to 3.2.29 in /frontend ([09dd657](https://git.griefed.de/Griefed/ServerPackCreator/commit/09dd6572ef82eef592a2ee746b826459311d6fdb))
* **deps:** bump vue from 3.2.29 to 3.2.30 in /frontend ([da542e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/da542e0f7bce7e7f30d039c206e6548d66e8f16f))
* **deps:** bump vue from 3.2.30 to 3.2.31 in /frontend ([674ff6b](https://git.griefed.de/Griefed/ServerPackCreator/commit/674ff6ba538dcc855a80b9d8c61aec4a68f22c5b))
* **deps:** Update commons-io to 2.11.0 ([b8a673a](https://git.griefed.de/Griefed/ServerPackCreator/commit/b8a673a8b744eb7653a2bbd359c0caadeac7ea72))
* **deps:** Update VersionChecker to 1.0.8 to make sure update notifications for 3.0.0 from 3.0.0-alpha or 3.0.0-beta version come through ([e178b56](https://git.griefed.de/Griefed/ServerPackCreator/commit/e178b567188692310009f71a23cb9e51324f5696))
* **fabric:** Update default Fabric Installer version if it can not be acquired from external ([b6b0bc3](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6b0bc31f1b6c3f5065e6c65b7fb4c292e8aced6))
* **fabric:** Update default Fabric Loader version if it can not be acquired from external ([aa2f9e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa2f9e16ee05e60374a6f6b33368a3fc9f928feb))
* **webservice:** Add artemis dependency for queueing system. Update dependencies. Exclude redundant slf4j. ([0954a56](https://git.griefed.de/Griefed/ServerPackCreator/commit/0954a56cf7ef8b1b8d26152a0b45aff86e3767cf))
* **webservice:** Do not run tests in Docker build. We have the Gradle Test stage for that. ([54b98fc](https://git.griefed.de/Griefed/ServerPackCreator/commit/54b98fc7eb143fd402a355118eeddef60ff03742))
* **webservice:** Ensure task are executed in correct order ([afb2f73](https://git.griefed.de/Griefed/ServerPackCreator/commit/afb2f73d0d27e4aaeaddbb4849e60a1b0a6f2b7d))
* Add Breaking section to changelog ([7165659](https://git.griefed.de/Griefed/ServerPackCreator/commit/7165659d8ccb507be63047c3b0f37d2fca2ac859))
* Add changes from main for GitHub workflows, delete no longer needed workflows. ([03ad356](https://git.griefed.de/Griefed/ServerPackCreator/commit/03ad356f762bd66d7cc887d537542fc06187cb2b))
* Add changes to github ci ([128ea30](https://git.griefed.de/Griefed/ServerPackCreator/commit/128ea30bbcd1011edb9a2fda85bfe1153863f787))
* Add improv for Improvements to list of commits which generate a release ([70d4b49](https://git.griefed.de/Griefed/ServerPackCreator/commit/70d4b4993726b5e3e464db4ea1bc6cc2a43d1dbb))
* Add readme-template and sponsors ci job ([5622dca](https://git.griefed.de/Griefed/ServerPackCreator/commit/5622dcaa0a32ecc40761056df461adc95ce08cce))
* Allow failure of dependency check and coverage jobs ([f8bb3d1](https://git.griefed.de/Griefed/ServerPackCreator/commit/f8bb3d1e82989d5639152d204c18aae642f6ff19))
* Allow running of Gradle and Docker test in parallel, to speed up pipeline completion. Move variables and services into global variable ([187a966](https://git.griefed.de/Griefed/ServerPackCreator/commit/187a9668d91fcc2ed8b809c86e6c8edc54db6f97))
* Bring in changes to CI from main ([b89125b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b89125ba34c873328f9e600f0bafd02586de1ad4))
* Build releases for alpha and beta branches ([8643327](https://git.griefed.de/Griefed/ServerPackCreator/commit/864332713be0adb15e8cebba0d679cdcebb755af))
* Build with --full-stacktrace ([cde8d08](https://git.griefed.de/Griefed/ServerPackCreator/commit/cde8d0845005f906f07f6878900ee7ab6ce99c98))
* Change branch separator in dependabot config ([3b08ff8](https://git.griefed.de/Griefed/ServerPackCreator/commit/3b08ff8e9169990d4c502a5cc1ecd86c3ca96a8d))
* Clean up and beautify ([d2ff50f](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2ff50fffc4571875724131a7b5d9cd4fbdf4521))
* Cleanup GitLab CI and Dockerfile. Remove spotbug. ([017ebed](https://git.griefed.de/Griefed/ServerPackCreator/commit/017ebed289b10b88e473ef18651c01cc7acee13b))
* Correctly write VERSION.txt ([6434be8](https://git.griefed.de/Griefed/ServerPackCreator/commit/6434be836fa19f5df05eb38980dbaf57938e4866))
* Create jacoco coverage report for coverage visualization in GitLab ([5da842f](https://git.griefed.de/Griefed/ServerPackCreator/commit/5da842f5415fbc16e43d51dd6195a4bd53ad22f3))
* Create pre-releases for alpha and beta branches ([e6729ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/e6729ea0a9f800def1c6de68c0ece7b4647ff111))
* Create pre-releases on pre-release tags mirror ([9b9e1b7](https://git.griefed.de/Griefed/ServerPackCreator/commit/9b9e1b79632a0a565f7433ac9025c1dd9d8dedee))
* Create releases for non-alpha/beta tags only. ([e2f76eb](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2f76eb26047e708f1eb286c93eb1e27eb083d59))
* Deactivate push on docker tests. Remove unnecessary file renaming in build release. Properly run publish job. ([481a048](https://git.griefed.de/Griefed/ServerPackCreator/commit/481a0488e27333ae3c7964a1fa67b8234e3ac6ac))
* Disable Docker pipelines for the time being. Docker is acting up and building Docker images of the webservice-branch is not necessary as I have yet to start actual work on the webservice itself. ([f45e25f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f45e25f681102dd991ff179a59df7c9fb85af227))
* Ensure docker jobs only run on git.griefed.de ([e633a0b](https://git.griefed.de/Griefed/ServerPackCreator/commit/e633a0b59b1d937ef7752333434cd1733c05b105))
* Exclude libraries folder from test workflow artifacts ([c796115](https://git.griefed.de/Griefed/ServerPackCreator/commit/c7961153fdb212f68360e06b4a9d04a50222b518))
* Fix artifact names for renaming ([d4f4f35](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4f4f352150a874f270e4468bcf102df10b72c68))
* Fix branch acquisition for GitHub Docker test ([063215f](https://git.griefed.de/Griefed/ServerPackCreator/commit/063215f65b7dbe9cd55b10ccac65de59b67c5cf4))
* Fix release build... ([fe2f601](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe2f6014802607e822ac0fde7facfb79a32233af))
* Further restrict jobs to specific branches. Sort jobs according to purpose ([444eede](https://git.griefed.de/Griefed/ServerPackCreator/commit/444eedec770570aab80f2183a86b147cb0a6688e))
* God damn, would you please only run when I tell you to? ([c610692](https://git.griefed.de/Griefed/ServerPackCreator/commit/c6106922a1c04fa3cee17880dfd8b931e5b9f951))
* Hopefully fix main release workflow trying to run on alpha/beta release ([9e6122e](https://git.griefed.de/Griefed/ServerPackCreator/commit/9e6122e7a5523d3b35850721062fe385f8c5d207))
* Hopefully fix main release workflow trying to run on alpha/beta release ([9742091](https://git.griefed.de/Griefed/ServerPackCreator/commit/97420912cb14057d1bc4fa92eaf1833015321eb3))
* Hopefully fix pattern for pre-releases ([efe28a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/efe28a55ef69b4195620d5f3190b16508a1121ea))
* Improve exclusion of tags/branches ([0f178fc](https://git.griefed.de/Griefed/ServerPackCreator/commit/0f178fc1dbabc5db6c4a5d47ab5f8d82d16c0d7a))
* Improve exclusion of tags/branches ([ee4dfcb](https://git.griefed.de/Griefed/ServerPackCreator/commit/ee4dfcbf6cfbc89a241df33c0176214839fcc62d))
* Merge Release and PreRelease jobs and only run on git.griefed.de ([f3115c9](https://git.griefed.de/Griefed/ServerPackCreator/commit/f3115c9c5757cb3f74ec2b15b3683ab226abe623))
* Mirror release on GitLab.com after tag mirror ([d08845d](https://git.griefed.de/Griefed/ServerPackCreator/commit/d08845dc1676e165ceb724d9ea775c37e1f3211f))
* Only create GitHub release for regular tags ([76ea670](https://git.griefed.de/Griefed/ServerPackCreator/commit/76ea6702e7ad715a62038d9746fac767a3892d74))
* Only run docker related jobs on git.griefed.de ([17339f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/17339f4d402b88ac6b358f0de6b2557d6df03122))
* Only run Gradle Test and Docker Test on main & master ([236c661](https://git.griefed.de/Griefed/ServerPackCreator/commit/236c661f6fa60a84f0290a295967186261ebce81))
* Only run tag and release generation on git.griefed.de ([8afea27](https://git.griefed.de/Griefed/ServerPackCreator/commit/8afea27163a985596c4d37102b6e7d366e640ba0))
* Post webhook message to Discord on new release ([2e3e25d](https://git.griefed.de/Griefed/ServerPackCreator/commit/2e3e25dde5ce19d8be2a2e641e9829ce1733c528))
* Prevent Generate Release job from running unnecessarily ([5be9fcd](https://git.griefed.de/Griefed/ServerPackCreator/commit/5be9fcdf2624991b9eaf845aafc3bdef8d34f04d))
* Publish maven artifact on (pre)release. Add info about new additional mirror on Gitea.com ([cfde3e2](https://git.griefed.de/Griefed/ServerPackCreator/commit/cfde3e29075254aa8e214349f29149b128e93b9d))
* Re-enable arch dependant nodedisturl ([f840e31](https://git.griefed.de/Griefed/ServerPackCreator/commit/f840e31a0e2fb95457a91d2e087ee66c756973d8))
* Reactivate docker jobs ([4b520c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b520c2f39e28633b25788300cf88e2a1c531d5f))
* Remove changelog generation of GitHub releases as I copy and paste the changelog from GitLab anyway ([25cdb26](https://git.griefed.de/Griefed/ServerPackCreator/commit/25cdb26d97fd5427e152615a9d10749d6039765f))
* Remove unnecessary environment cleaning ([67e1029](https://git.griefed.de/Griefed/ServerPackCreator/commit/67e1029e1cb12632d9cbe70c37466be84385721d))
* Remove unnecessary login to docker registry ([e5b034f](https://git.griefed.de/Griefed/ServerPackCreator/commit/e5b034f331e3b1d238da8e25254cf105d304e484))
* Remove unnecessary logins from test job ([dac135c](https://git.griefed.de/Griefed/ServerPackCreator/commit/dac135cc4e079a996e8ca45ae95d019345ea2283))
* Revert changes to Docker release jobs. Prevent running if on gitlab.com ([7e6404e](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e6404e9fc912a01674f4576a665115b67aa98e4))
* Run correct Gradle tasks on tag mirror from GitLab to GItHub ([db6dcd0](https://git.griefed.de/Griefed/ServerPackCreator/commit/db6dcd0b245b2603b7aafea0c59cba114016a291))
* Run dockerjobs differently when running on other GitLab instances. ([169733f](https://git.griefed.de/Griefed/ServerPackCreator/commit/169733f1b9aa7c6295b4074f0468dc51caa342be))
* Run dockerjobs differently when running on other GitLab instances. ([0385ba1](https://git.griefed.de/Griefed/ServerPackCreator/commit/0385ba139b783bf348dbd14b0f69bda587a0cb77))
* Run dockerjobs differently when running on other GitLab instances. ([bfcbd51](https://git.griefed.de/Griefed/ServerPackCreator/commit/bfcbd51ebd6f30331d82fbc53284a357f9d751aa))
* Run GitHubs dependabot on dependabot-branch and run tests on GitHubs infrastructure. The more the merrier ([659f0f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/659f0f4bd721befa0b3a57f4699a437390c7fbbb))
* Set loglevel in SAST to debug ([fc5341f](https://git.griefed.de/Griefed/ServerPackCreator/commit/fc5341fea92bba0e2f650644e543c53a1d8c48c4))
* Split tests in GitHub workflow into separate jobs ([58fd4b3](https://git.griefed.de/Griefed/ServerPackCreator/commit/58fd4b3758aea9fc029bf70929fef9f5d2f9cddd))
* Tag dev-images with short_sha as well. Remove some artifacts ([f3f9913](https://git.griefed.de/Griefed/ServerPackCreator/commit/f3f9913797cc55458eef5eca7554c4de877f1adf))
* Try and fix Renovate warning ([893a581](https://git.griefed.de/Griefed/ServerPackCreator/commit/893a581c9d6a2935cdd80aa9df03f1717b3a425c))
* Update siouan/frontend-gradle-plugin to 5.3.0 and remove arch dependant configuration of nodeDistributionUrlPathPattern. See https://github.com/siouan/frontend-gradle-plugin/issues/165 ([1177d05](https://git.griefed.de/Griefed/ServerPackCreator/commit/1177d056934bc2b8521f214b326c16d5e069fb7a))
* Upload artifacts of GitHub actions ([b4e41e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/b4e41e458435b591a3fee54f7d38fbe2bb66feb4))
* You have a problem, so you use regex. Now you have two problems. ([b05c007](https://git.griefed.de/Griefed/ServerPackCreator/commit/b05c0075a810f89ba79ff3a9f32939e0abbe0ca8))
* **docs:** No need to run tests ([728af78](https://git.griefed.de/Griefed/ServerPackCreator/commit/728af78dc4cb6c1f93b730e7367fcefe85483365))
* **GitHub:** Correctly execute (pre)release actions when tags are pushed. ([19c24c3](https://git.griefed.de/Griefed/ServerPackCreator/commit/19c24c3aa0f504ca3f1a7e0c726c8d08ff578b79))
* **webservice:** Add temporary job for testing webservice and fix gitignore ([350582e](https://git.griefed.de/Griefed/ServerPackCreator/commit/350582e3a829d285607a2a21d10889350cab4ee8))
* **webservice:** Ensure quasar is installed before assembling frontend ([0f414ca](https://git.griefed.de/Griefed/ServerPackCreator/commit/0f414ca06487647b964bfd3e2fa3daa4244b1ecc))
* **webservice:** Fix URL for node distribution on arm ([f24663f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f24663f1c72a88444a0cb1cfd264605f59fbb5aa))
* **webservice:** Make sure arm-builds in Docker work with the frontend plugin ([2c3793c](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c3793c0b2fa838504219f4c662723db9a928df8))
* **webservice:** Make sure no cache interferes with Docker build. Install library in hopes of fixing a failure in the pipeline. ([5841007](https://git.griefed.de/Griefed/ServerPackCreator/commit/58410078abdaf7ee2bf878edac14143d73f4866b))
* **webservice:** Scan dep updates for frontend, too (I hope this works lol) ([2994d25](https://git.griefed.de/Griefed/ServerPackCreator/commit/2994d257075deeda7817fad5990d02c2d5e7f867))


### 🧨 Breaking changes!

* Allow users to specify JVM flags/args for start-scripts via Menu->Edit->Edit Start-Scripts Java Args. Start scripts are no longer copied from server-files. New config option javaArgs automatically migrated into configs. ([929bfa6](https://git.griefed.de/Griefed/ServerPackCreator/commit/929bfa680704846e72952989f9f6f4f71e081ac7))


### 🧪 Tests

* Adapt tests ([e20f89d](https://git.griefed.de/Griefed/ServerPackCreator/commit/e20f89d34ecbcc85edea44264715ac90c47bc7af))
* Add more unit tests. ([ae06aa6](https://git.griefed.de/Griefed/ServerPackCreator/commit/ae06aa64a2463f31305efb072e7c5c49b42e5575))
* Add unit tests for UpdateChecker ([1b4b91a](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b4b91ac48c33b26aa77863eaed993171c99a372))
* Autowire jmsTemplate ([1ba6968](https://git.griefed.de/Griefed/ServerPackCreator/commit/1ba6968cb942ede7a211f58cb2aae930ad97fa66))
* Disable CurseForge related tests ([b28c97c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b28c97c9ccd3602fa266def9df1ff010cae4e68b))
* Don't delete default files after testing for them. ([b34602c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b34602c1a0ba30481c25fbb580c17d3331513ddc))
* Don't mention what is tested. Method names already tell us that. ([e32fd53](https://git.griefed.de/Griefed/ServerPackCreator/commit/e32fd534ec2498e8326d52da83759dd5d5e7bdac))
* Ensure serverpackcreator.properties is always available to prevent NPEs ([f674e13](https://git.griefed.de/Griefed/ServerPackCreator/commit/f674e137d44c3dfa3832d16c870aa865b1f6e6d6))
* Fix a test regarding AddonHandler ([b737d92](https://git.griefed.de/Griefed/ServerPackCreator/commit/b737d92db767f961151cd22ca2c0227d0020fa5a))
* Fix some paths and configs so tests don't fail because of Layer 8 ([8270c82](https://git.griefed.de/Griefed/ServerPackCreator/commit/8270c82a6cb32ed7415b680e7f38bd81462bf2c7))
* Fix some tests ([5ba12ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ba12adf856ea9a0341393e56665c0c7f873649b))
* Fix test failing due to missing, recently added, clientside-only mod ([1eaa966](https://git.griefed.de/Griefed/ServerPackCreator/commit/1eaa966468cc74f0ed2aab63cdc3dc006df082e0))
* Hopefully fix ArtemisConfigTest ([7573d99](https://git.griefed.de/Griefed/ServerPackCreator/commit/7573d99bbc009eeb987d1743dae6e55896ea7545))
* Print stacktrace in all gradle builds to allow for better debugging ([7b6e480](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b6e480c5e50f49843fadfdb6efcfbbdfeb8cc69))
* Remove addon execution from tests, as parallel running tests caused problems because the addon can only be accessed by one thread at a time. ([b963b10](https://git.griefed.de/Griefed/ServerPackCreator/commit/b963b1094e3a470213fc737f9effa305960ad31f))
* Set ddl-auto to create ([8e00f7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/8e00f7e4990ad42ceb2e7a23bbdcaf075e26a261))
* Some cleanups. Nothing interesting ([12bc506](https://git.griefed.de/Griefed/ServerPackCreator/commit/12bc50602b411589b65f5e70e2024fbc0bff53f1))
* Split test methods. Helps pin-pointing cause of error in case of failure. ([f2d723b](https://git.griefed.de/Griefed/ServerPackCreator/commit/f2d723b2e3ebf24e9bdb86c83c35a791efa082c8))
* Try and fix ArtemisConfigTest and SpringBootTests for spotbugs job ([67817a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/67817a1e1b24742f9cac1930f44a8908272330e2))
* Try and fix ArtemisConfigTest and SpringBootTests for spotbugs job ([29c870f](https://git.griefed.de/Griefed/ServerPackCreator/commit/29c870fec68e75df7da3d8dba978a6f6688642b2))
* Try and fix ArtemisConfigTest for spotbugs job ([c665bf5](https://git.griefed.de/Griefed/ServerPackCreator/commit/c665bf5fd23d4fe56c249c3d4b3f1a22ebd5c3b5))
* Try and fix error because of missing database ([81d4f80](https://git.griefed.de/Griefed/ServerPackCreator/commit/81d4f8045ed06bd83525edbb4980dde8afa2881e))
* Ye olde I RUN FINE ON YOUR MACHINE BUT NOT ON ANOTHER NU-UUUUHHUUUU.....Sigh ([4442168](https://git.griefed.de/Griefed/ServerPackCreator/commit/444216872f3df37e7e7cb9681d3752d91eb82d17))


### 🚀 Features

* Add methods to reverse the order of a String List or String Array. Allows setting of lists in GUI with newest to oldest versions. ([11d565e](https://git.griefed.de/Griefed/ServerPackCreator/commit/11d565ef61ed9ea2d324b82b4cb49ec529ffe624))
* Add tab for addons log tail. ([b84cc5b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b84cc5b12c9cd33176830d8eb413a1005a0d87a2))
* Add tooltip to SPC log panel informing users about the upload buttons in the menu bar ([08a123d](https://git.griefed.de/Griefed/ServerPackCreator/commit/08a123daae1687d8e7f929ae078b91c444aa7c9b))
* Addon functionality! This allows users to install addons to execute additional operations after a server pack was generated. See 5. in the README and the example addon at https://github.com/Griefed/ServerPackCreatorExampleAddon ([2a93e54](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a93e5476d11e84215667460997b694d30e93770))
* Allow check of configuration from an instance of ConfigurationModel, without any file involved. ([17529fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/17529fa958fbb386dfe7bdc91eaec2f9ceff39f5))
* Allow generation of a server pack by uploading it to the webservice. ([c92ddd2](https://git.griefed.de/Griefed/ServerPackCreator/commit/c92ddd2d01ec7851fed4696608a71b6c9efeea08))
* Allow generation of a server pack from an instance of ConfigurationModel ([5b54a1c](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b54a1ca9b3be3cc7d72e3c1851a636ee81a482e))
* Allow specifying custom server-icon.png and server.properties. The image will be scaled to 64x64. Implements GH[#88](https://git.griefed.de/Griefed/ServerPackCreator/issues/88) and GH[#89](https://git.griefed.de/Griefed/ServerPackCreator/issues/89). ([e3670e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3670e4ffc15505856ae9695f59f3c614e0199dd))
* Allow specifying files to add to server pack with simple `foo.bar` connotations. Closes issue [#86](https://git.griefed.de/Griefed/ServerPackCreator/issues/86) ([8a53aa6](https://git.griefed.de/Griefed/ServerPackCreator/commit/8a53aa6b9dbf148d60f4001a47e64055e8975d10))
* Allow users to disable cleanups of server packs and downloaded CurseForge modpacks. Can save bandwidth, time and disk operations, if the user is interested in that. ([3155af4](https://git.griefed.de/Griefed/ServerPackCreator/commit/3155af499006eba64751cca01e53e45480e8e936))
* Allow users to disabled server pack overwriting. If de.griefed.serverpackcreator.serverpack.overwrite.enabled=false AND the server pack for the specified modpack ALREADY EXISTS, then a new server pack will NOT be generated. Saves a LOT of time! ([00dd7aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/00dd7aa15b8cdbdce91f6d510fc2505f2f6e9d1a))
* Allow users to edit language-definitions in the lang-directory. ([e2b5cca](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2b5ccaef8834ab3a9154d7208a5e6ff90a2b14b))
* Allow users to exclude files and directories from the server pack to be generated with ! as the prefix in an entry in copyDirs ([f527d04](https://git.griefed.de/Griefed/ServerPackCreator/commit/f527d04dc67d5c2c186a460068aa84167278cafd))
* Allow users to set a suffix for the server pack to be generated. Requested in issue [#77](https://git.griefed.de/Griefed/ServerPackCreator/issues/77) ([2d32119](https://git.griefed.de/Griefed/ServerPackCreator/commit/2d321197c6123348558476b20b6f2c9aa93cc54f))
* Allow users to specify a custom directory in which server-packs will be generated and stored in. ([4a36e76](https://git.griefed.de/Griefed/ServerPackCreator/commit/4a36e76bfab5a66ce52c51e57bb16af79dddb752))
* Automatically detect clientside-only mods for Minecraft modpacks version 1.12 and older. ([e17322e](https://git.griefed.de/Griefed/ServerPackCreator/commit/e17322ed5db6bd18b4573be4a3562295317dd137))
* Automatically detect clientside-only mods for Minecraft modpacks version 1.13+. ([3811190](https://git.griefed.de/Griefed/ServerPackCreator/commit/3811190cb401c8993d84f0026618ad6e4958ed27))
* Basic filewatcher to monitor a couple of important files. Example: Delete serverpackcreator.properties to reload defaults ([d3f194a](https://git.griefed.de/Griefed/ServerPackCreator/commit/d3f194abb2ef55e168c094290263d4e78162cc91))
* Check and notify on updates in logs, console and in GUI. Also replaced and update a couple of i18n keys. VersionChecker can be found at https://git.griefed.de/Griefed/VersionChecker ([64419a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/64419a203a0d26bb001f20de2f8ab0a732156f20))
* Check setting for Javapath upon selecting "Install modloader-server?". If it is empty, the user is asked whether they would like to select their Java executable now. If not, the user is warned about the danger of not setting the Javapath ([5d474f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/5d474f1cf2763c010b6c02f969e2843de96d339f))
* Configurable schedules in webservice which clean up the database and filesystem of unwanted server packs and files. ([09ccbc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/09ccbc14921946a022634c454a013f0adb1cac63))
* Create eula.txt upon server pack generation. Closes issue [#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83) ([d48191c](https://git.griefed.de/Griefed/ServerPackCreator/commit/d48191cda634f8bb8cc4db2298a0848b8b14c2cc))
* Create server packs from zipped modpacks. Point modpackDir at a ZIP-file which contains a modpack in the ZIP-archives root. ([fbdae16](https://git.griefed.de/Griefed/ServerPackCreator/commit/fbdae16759e90cfd86786ee43ccf7a448fae0cce))
* Display version in window title and print to logs ([201a64c](https://git.griefed.de/Griefed/ServerPackCreator/commit/201a64c32868b0d26800b50c55d1e39dd5daa464))
* Enable/disable clientside-only mods autodiscovery via property de.griefed.serverpackcreator.serverpack.autodiscoverenabled=true / false. Closes [#62](https://git.griefed.de/Griefed/ServerPackCreator/issues/62). ([094a217](https://git.griefed.de/Griefed/ServerPackCreator/commit/094a217e83f2f27ba1e3746088b459a542411254))
* If given languagekey can not be found, use en_us from resources as fallback ([5802636](https://git.griefed.de/Griefed/ServerPackCreator/commit/5802636a612c4a49878f68b827e1115895062a95))
* If i18n localized string can not be found in local file, try JAR-resource. If locale is not en_us, get en_us localized string as fallback. Allow users to write their own locales, languages and translations. ([802eb0c](https://git.griefed.de/Griefed/ServerPackCreator/commit/802eb0c5a4aa06b90d71bb570864bcda613bc55c))
* Implement voting-system for server packs. Improve styling of download table. ([e49fa96](https://git.griefed.de/Griefed/ServerPackCreator/commit/e49fa96e4d2268441d67b8cd253c67e92dc33128))
* in start scripts: Ask user whether they agree to Mojang's EULA, and create `eula=true` in `eula.txt` if they specify I agree. Closes GH[#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83) ([5995f51](https://git.griefed.de/Griefed/ServerPackCreator/commit/5995f512d2731ebbd161c0ff8e34e37a437da0ac))
* New theme and cleaned up GUI. MenuBar for various things (wip). Lists for version selection. Switch between darkmode and lightmode and remember last mode used. More things, check commit. ([949fb6a](https://git.griefed.de/Griefed/ServerPackCreator/commit/949fb6aecd47518e0b91ca3a8be0516a9f2cb540))
* Pass the path where ServerPackCreator resides in to addons. Create dedicated addon-directory in work/temp, avoiding potential conflict with other addons. ([c9050b6](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9050b68ee42b4dabcde73cfb8eaf1417ab0a312))
* Provide HashMap of Key-Value pairs in MinecraftVersion-ForgeVersions format. Use a given Minecraft version as key and receive a string array for available Forge versions for said MInecraft versions. ([0a0d3b5](https://git.griefed.de/Griefed/ServerPackCreator/commit/0a0d3b50c7d7e955c41ce148bb82d4fc9abe6ac1))
* Read Minecraft, Forge and Fabric versions from their manifests into lists which can then be used in GUIs. ([c9ce1ff](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9ce1ff41f12b6eeef9dc00827d3e6a129ee8a5f))
* Replace crude self-made addon system with Pf4j and provide first entry points ([e591488](https://git.griefed.de/Griefed/ServerPackCreator/commit/e59148806a0d3550cc3a9b2b3e4318e186b71029))
* replace fabric-server-launch.jar with improved Fabric Server Launcher, if it is available for the specified Minecraft and Fabric versions.Thanks to @TheButterbrotMan for the idea! ([befdaf7](https://git.griefed.de/Griefed/ServerPackCreator/commit/befdaf7ea4265af9b3a0398f58a43bab3f19525f))
* Select Minecraft and modloader versions from lists instead of entering text into a textfield. ([5b56f18](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b56f18a90e7d3f1bfda98d5ae509a9cda29e959))
* Set copyDirs to "lazy_mode" to lazily create a server pack from the whole modpack. This will copy everything, no exceptions. Thanks to [@kreezxil](https://git.griefed.de/kreezxil) for the idea. ([2d89bec](https://git.griefed.de/Griefed/ServerPackCreator/commit/2d89bec8de7574bc14e213ce2e575558f12c9537))
* Store server pack suffix in serverpackcreator.conf.l Closes [#77](https://git.griefed.de/Griefed/ServerPackCreator/issues/77) again. ([d6c74e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/d6c74e0f62f395ea171266daca6194e39f0f634a))
* Write errors encountered during config check to logs/console. When using GUI, show a message with the encountered Errors. Helps with figuring out whats wrong with a given configuration. ([e1b0c62](https://git.griefed.de/Griefed/ServerPackCreator/commit/e1b0c6269cbd545993854786a07a949f4a379c45))
* **gui:** Add button in menubar to clear GUI. Allows starting with a fresh config without having to restart ServerPackCreator. Implements GH[#91](https://git.griefed.de/Griefed/ServerPackCreator/issues/91) ([dddee02](https://git.griefed.de/Griefed/ServerPackCreator/commit/dddee0286ca110bb25c75ff5d66756e86130b356))
* **gui:** Open server-icon.png in users default picture-viewer. From there on, users can open their favourite editing software. ([d960dd2](https://git.griefed.de/Griefed/ServerPackCreator/commit/d960dd28f7e796b8d7f84dfbcfe55273e60cfec8))
* **gui:** Open server.properties in users default text editor via Edit->Open server.properties in Editor ([1bf7533](https://git.griefed.de/Griefed/ServerPackCreator/commit/1bf75338e60b4fe0ff85eca6a55308eb4538fe7f))
* **gui:** Redesign help window. Users can choose what they need help with from a list, which then displays the help-text for the chosen item. ([7c490a3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c490a3d2a205181c61148ad4ff9b8872ff5961b))
* **gui:** Save the last loaded configuration alongside the default serverpackcreator.conf, unless a new configuration was started. Can be activated/deactivated with `de.griefed.serverpackcreator.configuration.saveloadedconfig=true` or `false` respectively ([e03b808](https://git.griefed.de/Griefed/ServerPackCreator/commit/e03b8089dca9ca40aa8d2a07948603888fbefd70))
* **gui:** Set LAF for Java Args correctly. If javaArgs is "empty", display textField as "" to not confuse users. ([462e7a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/462e7a1cef59715b08ff5f20ac03ae760a45132c))
* **gui:** Various changes. Too many to list. MenuBar entries, Theme changes. MenuItem funcitionality etc. etc. ([28c088c](https://git.griefed.de/Griefed/ServerPackCreator/commit/28c088cc5395a432ac6cbd83f2b31643922bf858))
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

* -Dlog4j2.formatMsgNoLookups=true to prevent log4j2 vulnerability, added via customziable OTHERARGS in scripts. Move java path to JAVA for customizability (is that a word?) ([ff7dc52](https://git.griefed.de/Griefed/ServerPackCreator/commit/ff7dc52f23ed5e1e2abc92f33c9964225c083dcb))
* Allow selection of bmp, jpg and jpeg as server-icons. Java correctly converts them to png for use as server-icons. If the image could not be loaded, print an error message. ([d2c1ac7](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2c1ac78fbf97c003e10f49af281437b95891865))
* Allow translating for full GUI as well as missing parts in backend. ([366cb10](https://git.griefed.de/Griefed/ServerPackCreator/commit/366cb106fddbebb1411105d466017c2f36e19a63))
* Always load classpath serverpackcreator.properties first, then loac local filesystem serverpackcreator.properties. Ensures defaults are always present and available to be overwritten and never empty. ([f91c8da](https://git.griefed.de/Griefed/ServerPackCreator/commit/f91c8da02116c5271eda0d02b4a394d2ed267ae2))
* Catch occasional error from CurseForge's API which could lead to dead entries in the database ([625a8a8](https://git.griefed.de/Griefed/ServerPackCreator/commit/625a8a83647a3fd875b80c629159c2874f667f63))
* Clear text every 1000 lines. Help with issue [#76](https://git.griefed.de/Griefed/ServerPackCreator/issues/76). ([132a3dd](https://git.griefed.de/Griefed/ServerPackCreator/commit/132a3ddd903f8693e08d9252c1f3e9c6004aad3f))
* Copy lang-files if running as .exe ([c7c1415](https://git.griefed.de/Griefed/ServerPackCreator/commit/c7c1415ecdc4e30e9743f378e70e25b3b7545977))
* Correctlry get property which decides whether autodiscovery of clientside-only mods should be enabled ([3c5deff](https://git.griefed.de/Griefed/ServerPackCreator/commit/3c5deff79acf70d5d6ea6d578cc4e73faf85d4d3))
* Correctly check source;destination-combinations no matter whether a absolute path, relative path, file or folder was specified as the source and correctly copy them to the server pack. ([ba2a2f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba2a2f11eef0771448022c8fd8f299d1c98473cd))
* Correctly compare user input with variable in start.bat, resulting in creation of eula.txt if user enters "I agree" ([224cbb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/224cbb3874830c7ff2cce83e403eb27470244aa8))
* Correctly initialize variable in start-scripts. Correctly pass OTHERARGS in batch-scripts. ([26f6dfd](https://git.griefed.de/Griefed/ServerPackCreator/commit/26f6dfdd24fb24c27755699edaa3b79bf89ae698))
* Create additional pattern for log files as ANSI colouring frakked up the formatting for log entries in files. ([f246bf8](https://git.griefed.de/Griefed/ServerPackCreator/commit/f246bf8777d72832041c16f3f1f4fe21305ef870))
* Deactivate CurseForge related code until custom implementation of CurseForgeAPI with CurseForge-provided API token is implemented and provided ([8c9bbff](https://git.griefed.de/Griefed/ServerPackCreator/commit/8c9bbff55d50a660ed0b673152a2b61c84845aae))
* Empty commit so a new alpha will be generated... ([c1b5698](https://git.griefed.de/Griefed/ServerPackCreator/commit/c1b5698a0aac863518244998c72a4f21ff4d604c))
* Ensure no empty entries make it into copyDirs or clientMods lists. Prevents accidental copying of the whole modpack into the server pack. Thanks to @Kreezxil for this improvement! ([5549930](https://git.griefed.de/Griefed/ServerPackCreator/commit/5549930966408fd219ab9f8a8e2dbaeaefcf3d57))
* Expanded fallback clientside modslist with 3dSkinLayers-,armorchroma-,Craftpresence-,medievalmusic-,MyServerIsCompatible- thanks to @TheButterbrotMan ([a2ac391](https://git.griefed.de/Griefed/ServerPackCreator/commit/a2ac391d7ca4664b8320be07671b669721dfa4b3))
* Expanded fallback modslist with yisthereautojump, ToastControl, torchoptimizer ([f1c4ba3](https://git.griefed.de/Griefed/ServerPackCreator/commit/f1c4ba31f0a6253064c990ccf9dd05dd77f47d55))
* Explicitly define log4j and force any dependency using it to use the secure version ([f0c1946](https://git.griefed.de/Griefed/ServerPackCreator/commit/f0c19465ba0daf6c6d8ce090913a24e3ab8d820c))
* Fix axios instance for api ([12508f3](https://git.griefed.de/Griefed/ServerPackCreator/commit/12508f34884ebce85d88386f35363efd34d35e1d))
* Fix building of list of fallbackmods if property contains , ([e000f25](https://git.griefed.de/Griefed/ServerPackCreator/commit/e000f2549e673b505df6b5d71a5c8455d78ddfab))
* Fix downloading of server packs by storing the path of the server pack in the DB in the path column ([8a47213](https://git.griefed.de/Griefed/ServerPackCreator/commit/8a472136554f25ac06caf1a013fd64a5dda6e79e))
* Fix downloading of server packs by updating the path of the server pack in the DB in the path column ([64dc619](https://git.griefed.de/Griefed/ServerPackCreator/commit/64dc619389442cfe5f6eddbb9ad98969dd60d987))
* Fix Forge installer log deletion. Forgot String.format with destination. ([1b44cb8](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b44cb8cc8022ffd7335e86823b98b7c31430e5f))
* Fix loading config not setting modloader specified in config ([cb50348](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb50348c6a4e4615db397948aefca5edabbbb83a))
* Fix missing serverpackcreator.properties for tests and do not run tests on GitHub releases. ([8895be8](https://git.griefed.de/Griefed/ServerPackCreator/commit/8895be80bfc76165d0347ee97e750301d6870afe))
* Fix reverseOrderList not actually reliably reversing a list ([bbfdea5](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbfdea53b9d6668f35f2635a295f042a45beade5))
* Fix scheduling to not run every second or minute ([9e87689](https://git.griefed.de/Griefed/ServerPackCreator/commit/9e87689c0dad05569bc74f7aba1bb687602c8bd4))
* Fix some mods broken dependency definitions breaking SPC funcitonality. Closes issue [#80](https://git.griefed.de/Griefed/ServerPackCreator/issues/80). ([a1c8a7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a1c8a7ef419ba7dcf90b74694c5f04480edfe807))
* Fix status message in GUI being displayed incorrectly on some Linux distros. Closes issue [#79](https://git.griefed.de/Griefed/ServerPackCreator/issues/79) ([5e7c08d](https://git.griefed.de/Griefed/ServerPackCreator/commit/5e7c08d886c9b1b7ef0640fe9cfe6f54e0d1fdc9))
* Fix user in Docker environment ([39f6bc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/39f6bc1fd6ca75e6783ae77c736983e601c550ab))
* Generate Minecraft 1.17+ Forge compatible scripts. Fixes issue [#84](https://git.griefed.de/Griefed/ServerPackCreator/issues/84). ([7d07e1d](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d07e1dad99c175b330f18c4c6cc83b00d43acac))
* Hopefully fix ServerPackCreator becoming unresponsive after generating a few server packs. Hopefully closes issue [#76](https://git.griefed.de/Griefed/ServerPackCreator/issues/76). ([aa92d9b](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa92d9b5afb3ceec2345c311ae90062aa45ce6c5))
* If no startup parameter is specified, assume -cli, else use the provided one. ([cad6e55](https://git.griefed.de/Griefed/ServerPackCreator/commit/cad6e55e73048003896fdde1f3e2b27ce69fa78a))
* Implement log4j exploit protection ([971fc4f](https://git.griefed.de/Griefed/ServerPackCreator/commit/971fc4fe7cfa362b48197d0222373a884c517f92))
* Improve configuration loading. Prevent NullPointers when reading Minecraft version, modloader, modloader version. ([0507ab7](https://git.griefed.de/Griefed/ServerPackCreator/commit/0507ab736d852415f2666937b1174429e7bac109))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([c556baa](https://git.griefed.de/Griefed/ServerPackCreator/commit/c556baaac7fe41ec0a11958e868a1da5bf26b14f))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([b108b67](https://git.griefed.de/Griefed/ServerPackCreator/commit/b108b6773d99fa8747fca016c70479521b2d6b1a))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([6afdbb4](https://git.griefed.de/Griefed/ServerPackCreator/commit/6afdbb4eb04af7b53ba64603fcc6348610edd4af))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([dd3ff6e](https://git.griefed.de/Griefed/ServerPackCreator/commit/dd3ff6ecf43a64ef29481007c700c74595b38229))
* Instead of using an external applications.properties for customizing, use our serverpackcreator.properties...which exists anyway! ([3794896](https://git.griefed.de/Griefed/ServerPackCreator/commit/3794896971e775d4f4d37aee7d340cc0510d8024))
* Last fallback in case no value can be found for a given key. ([53817d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/53817d4b8672359ff4a5b244c127afc581881436))
* Make date created and last modified in web frontend human-readable. ([2da9c29](https://git.griefed.de/Griefed/ServerPackCreator/commit/2da9c29c28aebe77365fec1426021a69e3e5ba7c))
* Make sure clientMods is set correctly with no starting [ or ending ] ([c98ef0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/c98ef0e0777673a6015d738c378b3bf30edf7eff))
* Modloader selection visually defaulted to Forge if no configuration was found in a given serverpackcreator.conf, but the value wasn't correctly set, resulting in the user having to select Forge manually anyway. ([d126447](https://git.griefed.de/Griefed/ServerPackCreator/commit/d12644714a8281e5dd7063521e28235b9204d5a3))
* More hardening against CVE-2021-44228 ([eaa4668](https://git.griefed.de/Griefed/ServerPackCreator/commit/eaa4668731ded0145f47d810d65dbf703306009c))
* Move destination acquisition into if-statement ([5d356a9](https://git.griefed.de/Griefed/ServerPackCreator/commit/5d356a95ec85cd04879a99c64538c113422f56ab))
* Move destination into if-statement ([9ae5ceb](https://git.griefed.de/Griefed/ServerPackCreator/commit/9ae5ceb8b314b5b6e065496118bc13aa6a3cab46))
* Only copy file from JAR-file if it is not found on local filesystem. ([09e271e](https://git.griefed.de/Griefed/ServerPackCreator/commit/09e271e4a8c6e0d202fd4a1db175087c8c9f9966))
* Open dialog whether the user wants to browse the generated server pack with our JFrame as parent, instead of JTabbedPane ([aa647f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa647f77429e6207927e5b1a743cb5b8f0be4887))
* Prevent dialog after server pack generation from becoming longer with each run. Removes the path to the server pack, though. Meh ([2260693](https://git.griefed.de/Griefed/ServerPackCreator/commit/226069366091155e11d9a1b7da9521f9802f168d))
* Prevent encapsulateListElements from writing duplicate entries ([1e64cd6](https://git.griefed.de/Griefed/ServerPackCreator/commit/1e64cd67dcbfcf95ccb544f84b70ee39e5123e75))
* Prevent file-lock from mod-sideness-scanning. Thanks to @Seniorendi for reporting. ([28a88dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/28a88dc3890d843677723cbdeed0847f725a4533))
* Prevent NPE for clientside-only mod property ([b188a85](https://git.griefed.de/Griefed/ServerPackCreator/commit/b188a858f637b8329447be08ed3701c43a713b00))
* Prevent NullPointerException if version or author are not defined in the modpacks manifest. ([d7336ba](https://git.griefed.de/Griefed/ServerPackCreator/commit/d7336baaae13781538d132ed62b24e25825da721))
* Prevent resizing of window during generation of server pack, to prevent freezes due to Forge installer log spamming. Seriously, that thing spams more than any bot I know of. ([89edc6f](https://git.griefed.de/Griefed/ServerPackCreator/commit/89edc6f61fbd40e1b1ed46871d70f103139200a5))
* Prevent unlikely, but possible, overwriting of properties file with wrong content from i18n initialization ([3675b09](https://git.griefed.de/Griefed/ServerPackCreator/commit/3675b0934253c5d03457cd64b6ca96825e0ee063))
* Prevent UpdateChecker from crashing SPC when any instance can not be reached ([b96cdb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/b96cdb347329e4512ecfe2b7c11e66479ee8be10))
* Print correct string for server pack suffix ([08c69e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/08c69e1be591421138d88429bc007091a13837ab))
* Re-add nogui parameter for fabric scripts. Apparently that is needed. Local tests proved it is not. My Little Fabric: Servers Are Magic ([6381c3b](https://git.griefed.de/Griefed/ServerPackCreator/commit/6381c3b1fc741ee684740db6d9fb5d7ccfb8f4d1))
* Read correct log in modloader-installer log tab ([095d05e](https://git.griefed.de/Griefed/ServerPackCreator/commit/095d05edd1235957e13b98122deba8c54c9efa12))
* remove `--` from Forge `nogui` argument. Fixes GH[#82](https://git.griefed.de/Griefed/ServerPackCreator/issues/82) ([f585891](https://git.griefed.de/Griefed/ServerPackCreator/commit/f58589114cd255a191b226c08c89f8dfeeac72dc))
* Set downloads and votes to zero upon generation of server pack ([be84232](https://git.griefed.de/Griefed/ServerPackCreator/commit/be8423251d82aea1a7639cd30bbaf9d0f06397df))
* Update frontend packages so it no longer throws some CSS minify errors around the block ([342e3c8](https://git.griefed.de/Griefed/ServerPackCreator/commit/342e3c895c6c090a09475d0d57a7c3d47e1238b7))
* Use inverted order array for Fabric version when checking for Fabric version upon config load and therefore set correct Fabric version. ([de5cdcf](https://git.griefed.de/Griefed/ServerPackCreator/commit/de5cdcf0b1bf1f81c812bd685dc41a5ef74b7f09))
* When writing configfiles, encapsulate every element of String Lists in `"` in order to avoid problems described in issue [#71](https://git.griefed.de/Griefed/ServerPackCreator/issues/71). Fixes and closes issue [#71](https://git.griefed.de/Griefed/ServerPackCreator/issues/71). ([0e029ec](https://git.griefed.de/Griefed/ServerPackCreator/commit/0e029ec477864ea765e8ad446ac2b9b93186b952))
* Whoops ([2c1841c](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c1841ca18856ba0d398641d52923f8537135c71))
* **ci:** Remove mv statement in GitHub release workflow causing the job to fail because of identical file names. ([e671001](https://git.griefed.de/Griefed/ServerPackCreator/commit/e671001dd10618ef342d948897aac21cb73c0847))
* **clientside-mods:** Add Optifine and OptiForge to list of clientside-only mods. ([eab294f](https://git.griefed.de/Griefed/ServerPackCreator/commit/eab294fd8c973512fb9c362e8a5721aec043e204))
* **Configuration:** Correctly load default properties and allow overriding of application properties from serverpackcreator properties. ([2f03d33](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f03d33f5634603d305b85a2681307a63a7ec10a))
* **CurseForge:** Remove some more mentions of generating a server pack from CurseForge as it is currently impossible to do so. ([7fbd22e](https://git.griefed.de/Griefed/ServerPackCreator/commit/7fbd22e24ed75586cfcb0bfc2227e5dc3c445421))
* **deps:** update dependency core-js to v3.19.3 ([f7a3140](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7a314067fae89105aed95cae95188c827812c2f))
* **deps:** update dependency vue to v3.2.24 ([4b44938](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b4493876f4476c6ecc90497bbc621e1aa1b545a))
* **deps:** update dependency vue to v3.2.26 ([be664e8](https://git.griefed.de/Griefed/ServerPackCreator/commit/be664e84c506155157e879f42b50426b0f8e7800))
* **Start Scripts:** Correctly call Java version print in bash scripts. Thanks to [@kreezxil](https://git.griefed.de/kreezxil) for reporting this issue! Closes [#274](https://git.griefed.de/Griefed/ServerPackCreator/issues/274). ([defb89b](https://git.griefed.de/Griefed/ServerPackCreator/commit/defb89b48cd6d217f78e3149eca68f859c53483c))
* **UpdateChecker:** Update VersionChecker to 1.0.7 and refactor UpdateChecker to DI to prevent false positives and correctly check for available updates. ([341d2d1](https://git.griefed.de/Griefed/ServerPackCreator/commit/341d2d1403922df2f685ad94d1a996eac6645645))
* **VersionChecker:** Update VersionChecker to version 1.0.6, closing issue [#256](https://git.griefed.de/Griefed/ServerPackCreator/issues/256) on GitHub. ([08c16ca](https://git.griefed.de/Griefed/ServerPackCreator/commit/08c16ca21006007263a9d903fbce9522c55ed5a5))
* **webservice:** Display correct tooltips for buttons in MainLayout ([d4530d3](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4530d35727e3b092fdb8383f546dda8dcc825d2))


### Other

* Add CraftPresence to fallbacklist of clientside-only mods (Reported by Law on Discord) ([88150ab](https://git.griefed.de/Griefed/ServerPackCreator/commit/88150ab82f654eba1d5be27566f3b74fea5d2b66))
* Add GitLab templates for Service Desk ([6be793f](https://git.griefed.de/Griefed/ServerPackCreator/commit/6be793fbe24177de6d17088f9ce0371c17fd0e77))
* Add improvement template. To be evaluated over time whether this is usefull ([218622b](https://git.griefed.de/Griefed/ServerPackCreator/commit/218622b7b091a7a90508449d1935afca3ff39a85))
* Add list of addons to README. Currently only the ExampleAddon I made is available. ([3367a8b](https://git.griefed.de/Griefed/ServerPackCreator/commit/3367a8bf839486c86efdb41f32caa85bcbd5a6bb))
* Add missing space in lang keys for copyDirs help. Closes issue [#78](https://git.griefed.de/Griefed/ServerPackCreator/issues/78) ([3539582](https://git.griefed.de/Griefed/ServerPackCreator/commit/35395827fb5a8e837ccae61925a0557aae544f29))
* Add moreoverlays- to list of fallback modlist ([e990661](https://git.griefed.de/Griefed/ServerPackCreator/commit/e9906612dd5b583c505f0eb0d4b5b5cb7fd769b2))
* Add moveoverlays- to list of fallback modslist ([64ead40](https://git.griefed.de/Griefed/ServerPackCreator/commit/64ead409e5ffb156da1d9b3ed8103f722483e3e2))
* Added debug logging when a new entry to files or directories to exclude is made ([719bb85](https://git.griefed.de/Griefed/ServerPackCreator/commit/719bb85b3c060854955b02fb225ddc171ddf5d80))
* Change order of input so users don't confuse the log-section to be related to the webservice. ([e352d12](https://git.griefed.de/Griefed/ServerPackCreator/commit/e352d120603e6810a3a3ed0b3e46b021db4ca5a0))
* Changelog from alpha branch ([c0e9383](https://git.griefed.de/Griefed/ServerPackCreator/commit/c0e93837a8751a3dcf06818953bf6e9ceea8e918))
* Clarify when I started with Java to put things into perspective. ([16f52f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/16f52f771587c94843a09eb46be7d047793b604e))
* Cleanup after build with tests. ([145e9d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/145e9d5b171e5afaaaaa5c5488437388d12ae4bf))
* Fix minor typo in language key ([9177763](https://git.griefed.de/Griefed/ServerPackCreator/commit/91777632c7ef1715f45af28ddb4f0848d5abb432))
* Fix tests, docs and add TODOs regarding lang keys ([2dac4e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/2dac4e1f0a7e53f7b04cfce982c1a6d2c99c5747))
* Include JProfiler and ej-Technologies in Awesomesauce section ([b989173](https://git.griefed.de/Griefed/ServerPackCreator/commit/b9891736d997c0c6ad81a8f4b650a1e7c0368dec))
* Inform issuer about what happens if they do not use the template ([3b89a7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/3b89a7e857fa2211c589e561ea28e56210296245))
* Inform issuer about what happens if they do not use the template ([49cffcf](https://git.griefed.de/Griefed/ServerPackCreator/commit/49cffcf6151ec9368eb233cec03aad1e97a1c2a6))
* Label issues and pull requests made by sponsors ([95591f9](https://git.griefed.de/Griefed/ServerPackCreator/commit/95591f90bb3af101ba7571230bccf7d2a19c450a))
* List addresses for Java documentation ([b90045b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b90045b05878f455947e0fcf2e38149ebdce7c05))
* List all places where ServerPackCreator is available at ([cb12edc](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb12edce4e26271d271344d90b7421c3118b3ee2))
* Mention libraries used and add third-party licenses ([8d4c715](https://git.griefed.de/Griefed/ServerPackCreator/commit/8d4c71535a46335788b3f8337d1581144c18f6bc))
* New screenshots, comparisons between different modes ([12ed5f6](https://git.griefed.de/Griefed/ServerPackCreator/commit/12ed5f6ec63cf1a04dd357955fa799c07e05780c))
* Re-add test-application.properties, which somehow vanished somewhere in the last commits. Set versioncheck.prerelease to false, in preparation for 3.0.0. Some cleanups here and there. ([a7718cc](https://git.griefed.de/Griefed/ServerPackCreator/commit/a7718ccae217adf7a37df7e461af29637dd40bf8))
* README overhaul. Include guides. Update guides. Number chapters. Cleanup ([7d0d2bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d0d2bd5b2823e64a7aa20a2239699533f9dc930))
* Remove --no-daemon from run configurations ([a76e357](https://git.griefed.de/Griefed/ServerPackCreator/commit/a76e3570de7cb7cbf75a96697f122cf02e69e693))
* Remove mention of armv7 docker images as they are no longer being supplied ([72e8308](https://git.griefed.de/Griefed/ServerPackCreator/commit/72e83089ef328494dcb07115f649682eec7edd59))
* Remove mentions of CurseForge until the custom API has been implemented. Cleanups.. ([b7c6d09](https://git.griefed.de/Griefed/ServerPackCreator/commit/b7c6d09459aba6c24eadb94c08663ef4e6062471))
* Remove no longer needed lang keys ([6435fbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/6435fbc73be7405290a48a16c2b053a0fa09e1ed))
* Remove no longer needed run configurations ([7e43ee3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e43ee3e6be65d55da98c2c06a19d69abd055880))
* Remove no longer relevant license ([64fbeeb](https://git.griefed.de/Griefed/ServerPackCreator/commit/64fbeeb9593a3696b9a53f1f436bbdf6d00e22e9))
* Remove unneeded imports ([8482d29](https://git.griefed.de/Griefed/ServerPackCreator/commit/8482d295eb1d731d1c02c654363dafe235ba9910))
* Remove unused language keys ([43fdba7](https://git.griefed.de/Griefed/ServerPackCreator/commit/43fdba70b1dfc52139c9fb2f255a065bdd92ef12))
* Rename job to better reflect what is actually happening ([4885952](https://git.griefed.de/Griefed/ServerPackCreator/commit/48859526c2c259ffb8f74f23ba83155409fe1384))
* Some cleanups and TODOs ([da02619](https://git.griefed.de/Griefed/ServerPackCreator/commit/da0261950ac780dea53055c3c41b5b0f513628b0))
* Some more logging ([d4fa143](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4fa143125b1eeb1e8e69e020906788a2224853f))
* TODO ([085c831](https://git.griefed.de/Griefed/ServerPackCreator/commit/085c83132b54693e05bab5d01eb77666ea5642ec))
* Udpate versions ([eecc90a](https://git.griefed.de/Griefed/ServerPackCreator/commit/eecc90a88dfa2d787a256e341dc422a124a22cab))
* Update git index for gradlew so execution is always allowed ([057b6c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/057b6c2e1514f5287596e4004cbbb790f34c1d12))
* Update gitignore to exclude new files generated by tests ([4147138](https://git.griefed.de/Griefed/ServerPackCreator/commit/4147138bfadee97e0671bfb1f8a3b41c657d62b3))
* Update README ([1fc9df7](https://git.griefed.de/Griefed/ServerPackCreator/commit/1fc9df72c1d1a8f5c7d82dc18a27af33e61b1307))
* Update README in resources ([4b8a3f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b8a3f4415a419e1b4acab1b86f79d83343da48f))
* Update README with information from self-hosted GitLab pipeline status. Expand on deploy and versioning info. Add more Jetbrains swag. All that good stuff. ([c36ad6c](https://git.griefed.de/Griefed/ServerPackCreator/commit/c36ad6cd313c83b4b321ae768922bfd16c751f07))
* Update README with new addon example ([bcca1ce](https://git.griefed.de/Griefed/ServerPackCreator/commit/bcca1ce72aff02ad28cdd3408286bfa8c64311da))
* Update third party-licenses ([b41a15f](https://git.griefed.de/Griefed/ServerPackCreator/commit/b41a15f94768f52069f3a969d511de9c387d0634))
* WHITESPACE! ([de9ebcc](https://git.griefed.de/Griefed/ServerPackCreator/commit/de9ebcc2147e6b205789d4f1c82720daed0a6ddd))
* **deps:** pin dependencies ([f6d8822](https://git.griefed.de/Griefed/ServerPackCreator/commit/f6d88221cb966c739365f352b2a9c6bb660eeb17))
* **deps:** pin dependencies ([358275b](https://git.griefed.de/Griefed/ServerPackCreator/commit/358275b16134c3953250e0dbcc763005a7a6b344))
* **deps:** update actions/setup-java action to v3 ([90a6baf](https://git.griefed.de/Griefed/ServerPackCreator/commit/90a6baf68ab7a4f73e8da5c74dfa92eb686a79b9))
* **deps:** update dependency @babel/eslint-parser to v7.16.5 ([d90ef33](https://git.griefed.de/Griefed/ServerPackCreator/commit/d90ef333df1d80fde46189faebe288f53f211427))
* **deps:** update dependency @quasar/app to v3.2.4 ([e33df47](https://git.griefed.de/Griefed/ServerPackCreator/commit/e33df47cb0182788a995f55a7a1852f3d75919d4))
* **deps:** update dependency @quasar/app to v3.2.6 ([292d4f5](https://git.griefed.de/Griefed/ServerPackCreator/commit/292d4f5d8b2c048aa6ed28b18e0bdf0eaa4de79c))
* **deps:** update dependency @quasar/app to v3.2.9 ([d61a461](https://git.griefed.de/Griefed/ServerPackCreator/commit/d61a4618eb3246b9bc96f19fbf0833f075af32a7))
* **deps:** update dependency @quasar/app to v3.3.2 ([e43122d](https://git.griefed.de/Griefed/ServerPackCreator/commit/e43122d85cb34e81d884feffe87023669f62ee3b))
* **deps:** update dependency @quasar/extras to v1.12.4 ([10b76e6](https://git.griefed.de/Griefed/ServerPackCreator/commit/10b76e68202bc00f55660f356b0471f018714b76))
* **deps:** update dependency @types/node to v16.11.10 ([e38cd23](https://git.griefed.de/Griefed/ServerPackCreator/commit/e38cd23fdda88247f678e718831dcb7f1dba7580))
* **deps:** update dependency @types/node to v16.11.14 ([76baa87](https://git.griefed.de/Griefed/ServerPackCreator/commit/76baa87cb160827729922b4cd11a407cf523fb9c))
* **deps:** update dependency @types/node to v16.11.15 ([7b8dd46](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b8dd46df3819ab64778b033403ee30b59ee0a7b))
* **deps:** update dependency @typescript-eslint/eslint-plugin to v5.12.1 ([c58b2a0](https://git.griefed.de/Griefed/ServerPackCreator/commit/c58b2a0722e9a80322c9a88170dd7d20246ff6b4))
* **deps:** update dependency axios to v0.25.0 ([3008f24](https://git.griefed.de/Griefed/ServerPackCreator/commit/3008f24ac04a5e50cf9cc94af7fffd70b85621f3))
* **deps:** update dependency com.fasterxml.jackson.core:jackson-databind to v2.13.0 ([9216f2e](https://git.griefed.de/Griefed/ServerPackCreator/commit/9216f2efb599ae971824818dfa038216d4f0c3da))
* **deps:** update dependency core-js to v3.20.0 ([809855a](https://git.griefed.de/Griefed/ServerPackCreator/commit/809855a1defa480ee9869c3bf3124474e0a8c34f))
* **deps:** update dependency core-js to v3.20.1 ([cde9246](https://git.griefed.de/Griefed/ServerPackCreator/commit/cde9246b792470bfc4e9308bb32bea2ae3bb8ada))
* **deps:** update dependency core-js to v3.20.2 ([b4bd45e](https://git.griefed.de/Griefed/ServerPackCreator/commit/b4bd45e7ef3b140f4941fb9e93f6fce8ac390394))
* **deps:** update dependency core-js to v3.21.1 ([9c612f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/9c612f2d94cf8001789ba2ee3327d0836fe5e40d))
* **deps:** update dependency eslint to v8.10.0 ([2e5f498](https://git.griefed.de/Griefed/ServerPackCreator/commit/2e5f4985b73024908e94acb9d1f4a75d3e10dc94))
* **deps:** update dependency eslint to v8.4.1 ([2db3a36](https://git.griefed.de/Griefed/ServerPackCreator/commit/2db3a36ae3f5f12e1263fbb91d5a7984804c58a8))
* **deps:** update dependency eslint to v8.5.0 ([6f7c5c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/6f7c5c24b8cb8a68427836331b1b2e758fdfeaa8))
* **deps:** update dependency eslint to v8.6.0 ([2e6ab21](https://git.griefed.de/Griefed/ServerPackCreator/commit/2e6ab21ee3ba1ff0649b4442e9edd3d8a1cb9b02))
* **deps:** update dependency eslint to v8.9.0 ([462f3d3](https://git.griefed.de/Griefed/ServerPackCreator/commit/462f3d36f47f90312ffa97caec9da6d4cd15ee6a))
* **deps:** update dependency eslint-plugin-vue to v8.2.0 ([e2df4dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2df4dc25fae418fdf495d7c2d4acbf1cae68567))
* **deps:** update dependency eslint-plugin-vue to v8.3.0 ([61e2eb4](https://git.griefed.de/Griefed/ServerPackCreator/commit/61e2eb47a22615bc23ef5040546ababeb8ca7a22))
* **deps:** update dependency eslint-plugin-vue to v8.4.1 ([0b16371](https://git.griefed.de/Griefed/ServerPackCreator/commit/0b16371881d6a5069744fc6b05a5fd05353b7dc6))
* **deps:** update dependency eslint-plugin-vue to v8.5.0 ([b30b829](https://git.griefed.de/Griefed/ServerPackCreator/commit/b30b829d56525606d1f365abdbbc8f3e70f31699))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.6 ([5941f91](https://git.griefed.de/Griefed/ServerPackCreator/commit/5941f9133f29623800d46da9fedd419c2618645a))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.7 ([11a5684](https://git.griefed.de/Griefed/ServerPackCreator/commit/11a568470256ccff757fecff38329f6516b6832c))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.3 ([bad28e8](https://git.griefed.de/Griefed/ServerPackCreator/commit/bad28e82029e6e4e429a0e8468551d99265095c0))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.4 ([22fe616](https://git.griefed.de/Griefed/ServerPackCreator/commit/22fe616e492d36c8107f2993cce2fcdceb10665a))
* **deps:** update dependency gradle to v7.2 ([268955f](https://git.griefed.de/Griefed/ServerPackCreator/commit/268955f0b67f2180ce7b8de467a911103f6d15af))
* **deps:** update dependency gradle to v7.3.1 ([6964401](https://git.griefed.de/Griefed/ServerPackCreator/commit/6964401eddbfadb265bb15fbd8a1aacfc5e6ea50))
* **deps:** update dependency gradle to v7.3.2 ([69019b9](https://git.griefed.de/Griefed/ServerPackCreator/commit/69019b97c3e2f4c38ae1a6eb4b8913a095986714))
* **deps:** update dependency gradle to v7.4 ([a636cbe](https://git.griefed.de/Griefed/ServerPackCreator/commit/a636cbe79aa4b0f1a14298c053804775c5cd8158))
* **deps:** update dependency org.apache.activemq:artemis-jms-server to v2.19.0 ([3245976](https://git.griefed.de/Griefed/ServerPackCreator/commit/3245976c0f88eef1e0e2b25da88d6eefed7e9dd3))
* **deps:** update dependency org.apache.logging.log4j:log4j-api to v2.17.1 ([01c8a80](https://git.griefed.de/Griefed/ServerPackCreator/commit/01c8a80de9499ea377bf03eff6eaac1b73f8efb9))
* **deps:** update dependency org.apache.logging.log4j:log4j-core to v2.17.1 ([7cbd208](https://git.griefed.de/Griefed/ServerPackCreator/commit/7cbd208142e559d57c37f12ccc5a738a2f682bc1))
* **deps:** update dependency org.apache.logging.log4j:log4j-jul to v2.17.1 ([48cf50d](https://git.griefed.de/Griefed/ServerPackCreator/commit/48cf50df5230f399c93f8abf25d7aff5f3500697))
* **deps:** update dependency org.apache.logging.log4j:log4j-slf4j-impl to v2.17.1 ([de850ff](https://git.griefed.de/Griefed/ServerPackCreator/commit/de850ff6bb2c9600be0a06b06f84fe594c190427))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.15.0 ([1018e10](https://git.griefed.de/Griefed/ServerPackCreator/commit/1018e106aeffa8439e0f5dd2aeaa2d1e6bf68639))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.16.0 ([5632772](https://git.griefed.de/Griefed/ServerPackCreator/commit/5632772a0785567f1ed0142c845120aac98a30bb))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.17.0 ([9ab5fc7](https://git.griefed.de/Griefed/ServerPackCreator/commit/9ab5fc7e189765d9a42dabb66274870e06ecd409))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.17.1 ([32af395](https://git.griefed.de/Griefed/ServerPackCreator/commit/32af395878dfe45ebfed0e0dbbcd77f104418558))
* **deps:** update dependency org.mockito:mockito-core to v3.12.1 ([ea12b3b](https://git.griefed.de/Griefed/ServerPackCreator/commit/ea12b3b5c277289b9389d8d41226edd4a2c5e210))
* **deps:** update dependency org.mockito:mockito-core to v4 ([0a8fbc9](https://git.griefed.de/Griefed/ServerPackCreator/commit/0a8fbc9cf95211ae234f0c2227f8c5bb6c190a5e))
* **deps:** update dependency org.slf4j:slf4j-log4j12 to v2.0.0-alpha3 ([6d79885](https://git.griefed.de/Griefed/ServerPackCreator/commit/6d79885da3136748c9c5c5da12dcc4368f3a07ef))
* **deps:** update dependency quasar to v2.4.12 ([8c3ab82](https://git.griefed.de/Griefed/ServerPackCreator/commit/8c3ab82e8889276595ce89d7b1b4b64d1a37a0c8))
* **deps:** update dependency quasar to v2.4.2 ([28ec385](https://git.griefed.de/Griefed/ServerPackCreator/commit/28ec3853f08d5e16110a1d95e1a2f95add7fc164))
* **deps:** update dependency quasar to v2.4.3 ([c3ff9b2](https://git.griefed.de/Griefed/ServerPackCreator/commit/c3ff9b2e55f4cedf6346d53a4395fcea633f2967))
* **deps:** update dependency quasar to v2.4.9 ([467b615](https://git.griefed.de/Griefed/ServerPackCreator/commit/467b6153cd2284a17815f8eee025dd88caed3c13))
* **deps:** update dependency tsparticles to v1.37.6 ([e69e81a](https://git.griefed.de/Griefed/ServerPackCreator/commit/e69e81a4263706ed969f4f7f1454dc550ee6659c))
* **deps:** update dependency tsparticles to v1.38.0 ([fa498bc](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa498bc7fd1df3067a2d12e6c227c35635848a46))
* **deps:** update dependency tsparticles to v1.39.1 ([d231885](https://git.griefed.de/Griefed/ServerPackCreator/commit/d231885bb4b569f1eba3eed492c22a653f9f72ae))
* **deps:** update dependency tsparticles to v1.41.2 ([b777818](https://git.griefed.de/Griefed/ServerPackCreator/commit/b777818b5f455b60e742f0bcd0d7fe93821472bc))
* **deps:** update dependency tsparticles to v1.41.5 ([71fd7cb](https://git.griefed.de/Griefed/ServerPackCreator/commit/71fd7cb1d1b6a4589f44ae201cd42c78c0aefccd))
* **deps:** update dependency vue to v3.2.28 ([c2fb183](https://git.griefed.de/Griefed/ServerPackCreator/commit/c2fb1836712dd415ea61ba252d69f307f1924b63))
* **deps:** update dependency vue to v3.2.29 ([57246dd](https://git.griefed.de/Griefed/ServerPackCreator/commit/57246dda971532cd7eae1d09b904e47631fe250e))
* **deps:** update ghcr.io/griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.3 ([a849b0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a849b0eed192bee1e0cf175930375beffc97f226))
* **deps:** update ghcr.io/griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.4 ([029c810](https://git.griefed.de/Griefed/ServerPackCreator/commit/029c810751db62f42ba1c8b08dfde3735e87fc40))
* **deps:** update ghcr.io/griefed/gitlab-ci-cd docker tag to v2.0.1 ([bf76d58](https://git.griefed.de/Griefed/ServerPackCreator/commit/bf76d58525bc75e65fb0dfdc3f1ae8541d1def6c))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2 ([e3d9f7c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3d9f7c907c39619fe0c36504472722140a03cec))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.1 ([18a75a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/18a75a55d5782e4823fda59915bfedc7111d35af))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.2 ([65f7d15](https://git.griefed.de/Griefed/ServerPackCreator/commit/65f7d1594cd6f9827b3c42cf59653623ee791b2e))
* **deps:** update jamesives/github-pages-deploy-action action to v4.1.8 ([1d4a7f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/1d4a7f7e3c389abdc1513050327b6018848441ff))
* **deps:** update jamesives/github-pages-deploy-action action to v4.2.0 ([20a6b82](https://git.griefed.de/Griefed/ServerPackCreator/commit/20a6b828e163b949dc29f534241bb3dc98ccb923))
* **deps:** update jamesives/github-pages-deploy-action action to v4.2.3 ([a3706fc](https://git.griefed.de/Griefed/ServerPackCreator/commit/a3706fca5b4164ce8c2aeb569dfa452272197593))
* **deps:** update npm to v8 ([f446f11](https://git.griefed.de/Griefed/ServerPackCreator/commit/f446f1167dc950ea509c4117743a380957c0502e))
* **deps:** update plugin com.github.ben-manes.versions to v0.40.0 ([55d37b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/55d37b1f93623c823c788a9ee970a00a4cd961a2))
* **deps:** update plugin com.github.ben-manes.versions to v0.41.0 ([28989fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/28989fdbd7aa57b6b036d91082694b047d266e4e))
* **deps:** update plugin edu.sc.seis.launch4j to v2.5.2 ([4e515f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/4e515f41687b5c13fa1a431ee5f664dc9f7748c3))
* **deps:** update registry.gitlab.com/haynes/jacoco2cobertura docker tag to v1.0.8 ([8df16d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/8df16d58cbd755361e7b1354841cbc5a4d43e3eb))
* **deps:** update spring boot to v2.6.1 ([d0d9f03](https://git.griefed.de/Griefed/ServerPackCreator/commit/d0d9f03b447443fb08da3b4ee517ee85cf08e29d))
* **deps:** update spring boot to v2.6.2 ([b6e4850](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6e4850ff8ebe5f18e5472563bb3782cfd1ea0a9))
* **deps:** update spring boot to v2.6.3 ([6a12a17](https://git.griefed.de/Griefed/ServerPackCreator/commit/6a12a17c95763abf0bd8f85b32b6dedea82f9df9))
* **deps:** update spring boot to v2.6.4 ([7ceabfc](https://git.griefed.de/Griefed/ServerPackCreator/commit/7ceabfc1acf20f1f388209108255958bc74e6101))
* **deps:** update typescript-eslint monorepo to v5.10.0 ([6cec6a6](https://git.griefed.de/Griefed/ServerPackCreator/commit/6cec6a6d662930906c608b00e85e84dfe262c12a))
* **deps:** update typescript-eslint monorepo to v5.6.0 ([c27b3b0](https://git.griefed.de/Griefed/ServerPackCreator/commit/c27b3b04ddb8219fd0c80f5e850c243bcb540634))
* **deps:** update typescript-eslint monorepo to v5.7.0 ([e6b01d8](https://git.griefed.de/Griefed/ServerPackCreator/commit/e6b01d858d2b9e25656fdbe07904b840242d2003))
* **deps:** update typescript-eslint monorepo to v5.8.0 ([1f29f23](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f29f236d19653487b791576c76cfee8c58e1e88))
* **deps:** update typescript-eslint monorepo to v5.8.1 ([ded0c7b](https://git.griefed.de/Griefed/ServerPackCreator/commit/ded0c7b39e9d48a06b7b6fc87537670e0a430f69))
* **deps:** update typescript-eslint monorepo to v5.9.0 ([7b705a4](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b705a4f8dab2c8055629078208b89ea4c264b46))
* **deps:** update typescript-eslint monorepo to v5.9.1 ([a766e2a](https://git.griefed.de/Griefed/ServerPackCreator/commit/a766e2a3cc33e4002f1bc38c97c997a6f24be9d2))
* **Icon:** Update icon template with layers for Addons Overview and Example Addon ([3df0c10](https://git.griefed.de/Griefed/ServerPackCreator/commit/3df0c101e98ee8e403a6dd2770ecb8b8e6e0577f))
* **README:** Fix markdown formatting. Thanks GitHub/IDEA, for showing different renderings of the same markdown file. *le sigh* ([bc780b5](https://git.griefed.de/Griefed/ServerPackCreator/commit/bc780b5f11dc8c0c3999f9fb992e5fbc93e053e7))
* **README:** Rephrase addons section and include link to new addons overview website ([a12ce06](https://git.griefed.de/Griefed/ServerPackCreator/commit/a12ce06c2c898bd14588688d517e696432eae69f))
* **webservice:** Add instructions on how to build SPC locally ([6e873ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/6e873ac174109b6d837de2c237d587128f5763a3))
* **webservice:** Expand readme with webservice related information ([fe5d440](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe5d440cc71a6445d211b7c3ca8ebfb0268eda6e))
* **webservice:** Properly setup manifest. Include up-to-date copies of license, readme, contributing, code of conduct, changelog in the jar. Exclude said files in backend/main/resources with gitignore. ([4812918](https://git.griefed.de/Griefed/ServerPackCreator/commit/4812918a72bf9dfdec89d4f052b1d7f173ae688c))

## [3.0.0-beta.11](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.10...3.0.0-beta.11) (2022-03-25)


### 🦊 CI/CD

* **deps:** Update VersionChecker to 1.0.8 to make sure update notifications for 3.0.0 from 3.0.0-alpha or 3.0.0-beta version come through ([e178b56](https://git.griefed.de/Griefed/ServerPackCreator/commit/e178b567188692310009f71a23cb9e51324f5696))


### Other

* Re-add test-application.properties, which somehow vanished somewhere in the last commits. Set versioncheck.prerelease to false, in preparation for 3.0.0. Some cleanups here and there. ([a7718cc](https://git.griefed.de/Griefed/ServerPackCreator/commit/a7718ccae217adf7a37df7e461af29637dd40bf8))

## [3.0.0-beta.10](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.9...3.0.0-beta.10) (2022-03-20)


### 🦊 CI/CD

* **deps-dev:** bump @types/node from 17.0.19 to 17.0.21 in /frontend ([43834fe](https://git.griefed.de/Griefed/ServerPackCreator/commit/43834fefc1c84b63d4eba4dc3ca74898953801b0))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([925b5d2](https://git.griefed.de/Griefed/ServerPackCreator/commit/925b5d2eb9bec460ede155840e950482c41e5c11))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([38cfdde](https://git.griefed.de/Griefed/ServerPackCreator/commit/38cfdde75095c3b2cb50676a11811965cc3ab148))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([a04e32d](https://git.griefed.de/Griefed/ServerPackCreator/commit/a04e32dcbae32613130238cfcff0428274cb45db))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([2fdeec9](https://git.griefed.de/Griefed/ServerPackCreator/commit/2fdeec99954be7b1a9af3fd9239398ad0569ad8d))
* **deps-dev:** bump eslint from 8.10.0 to 8.11.0 in /frontend ([66c8700](https://git.griefed.de/Griefed/ServerPackCreator/commit/66c8700a345a54d70084dadb413f6e62593a089d))
* **deps-dev:** bump eslint-config-prettier in /frontend ([0692bf8](https://git.griefed.de/Griefed/ServerPackCreator/commit/0692bf815373976cc0c67812a158050a1fb1cb6d))
* **deps:** bump @quasar/cli from 1.2.2 to 1.3.0 in /frontend ([3d39571](https://git.griefed.de/Griefed/ServerPackCreator/commit/3d39571341e6755707904d8b19c44b85ff37d59d))
* **deps:** bump @quasar/extras from 1.12.5 to 1.13.0 in /frontend ([a9add11](https://git.griefed.de/Griefed/ServerPackCreator/commit/a9add1111d404935ea14219ae72fbad95629018a))
* **deps:** bump @quasar/extras from 1.13.0 to 1.13.1 in /frontend ([7e2fe46](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e2fe46061145338673180bffbb6dc8f37741bd1))
* **deps:** bump axios from 0.26.0 to 0.26.1 in /frontend ([42dd920](https://git.griefed.de/Griefed/ServerPackCreator/commit/42dd92070f4ee12f527e7dba19d388ae3dce3768))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.6 to 2.0.7 ([2acd8c0](https://git.griefed.de/Griefed/ServerPackCreator/commit/2acd8c06b783136c030ea6c540bff234b9ef0023))
* **deps:** bump quasar from 2.5.5 to 2.6.0 in /frontend ([8b35e1f](https://git.griefed.de/Griefed/ServerPackCreator/commit/8b35e1f5e2a759c6ac3e5cfd127747cc84f1ce2e))
* **deps:** bump tsparticles from 1.41.5 to 1.41.6 in /frontend ([ecb4eff](https://git.griefed.de/Griefed/ServerPackCreator/commit/ecb4effc29673172fd7c74bfb57a325b6b3c5f4f))
* **deps:** bump tsparticles from 1.41.6 to 1.42.2 in /frontend ([7c8b807](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c8b807935a3a0bc9780ee6df30ded68b8c18149))


### 🛠 Fixes

* Correctly check source;destination-combinations no matter whether a absolute path, relative path, file or folder was specified as the source and correctly copy them to the server pack. ([ba2a2f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba2a2f11eef0771448022c8fd8f299d1c98473cd))


### Other

* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.7 ([11a5684](https://git.griefed.de/Griefed/ServerPackCreator/commit/11a568470256ccff757fecff38329f6516b6832c))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.4 ([22fe616](https://git.griefed.de/Griefed/ServerPackCreator/commit/22fe616e492d36c8107f2993cce2fcdceb10665a))

## [3.0.0-beta.9](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.8...3.0.0-beta.9) (2022-03-02)


### 🛠 Fixes

* **Configuration:** Correctly load default properties and allow overriding of application properties from serverpackcreator properties. ([2f03d33](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f03d33f5634603d305b85a2681307a63a7ec10a))

## [3.0.0-beta.8](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.7...3.0.0-beta.8) (2022-03-01)


### 🛠 Fixes

* **Start Scripts:** Correctly call Java version print in bash scripts. Thanks to [@kreezxil](https://git.griefed.de/kreezxil) for reporting this issue! Closes [#274](https://git.griefed.de/Griefed/ServerPackCreator/issues/274). ([defb89b](https://git.griefed.de/Griefed/ServerPackCreator/commit/defb89b48cd6d217f78e3149eca68f859c53483c))

## [3.0.0-beta.7](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.6...3.0.0-beta.7) (2022-02-27)


### 💎 Improvements

* **Strings:** Strengthen configuration checks by making sure some strings do not contain backslashes. Make server pack suffix more secure by removing illegal characters. ([04b76c9](https://git.griefed.de/Griefed/ServerPackCreator/commit/04b76c93b6dd1955440b247f3542d0729d4af7b7))


### 🦊 CI/CD

* **deps:** bump edu.sc.seis.launch4j from 2.5.1 to 2.5.2 ([59051b9](https://git.griefed.de/Griefed/ServerPackCreator/commit/59051b927831a6e09ee3b0a491b014c4d67a6034))
* **deps:** bump log4j-api from 2.17.1 to 2.17.2 ([2984f46](https://git.griefed.de/Griefed/ServerPackCreator/commit/2984f46d7ce916705c9fc537ddeb1b13a3b4355a))
* **deps:** bump log4j-core from 2.17.1 to 2.17.2 ([06c0dd6](https://git.griefed.de/Griefed/ServerPackCreator/commit/06c0dd6f2848484b9e97e3a441b8dcd893ce144a))
* **deps:** bump log4j-jul from 2.17.1 to 2.17.2 ([2f38947](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f38947cde6546126a08e1716174fd1d0ea70520))
* **deps:** bump log4j-slf4j-impl from 2.17.1 to 2.17.2 ([0a9099b](https://git.griefed.de/Griefed/ServerPackCreator/commit/0a9099bdd28c88f2a8e040a9cc558bb82dc84cda))
* **deps:** bump log4j-web from 2.17.1 to 2.17.2 ([e76c049](https://git.griefed.de/Griefed/ServerPackCreator/commit/e76c049e05342c0aa3ff41076db12bd6ca36df5c))
* **deps:** bump org.springframework.boot from 2.6.3 to 2.6.4 ([f9e0d1a](https://git.griefed.de/Griefed/ServerPackCreator/commit/f9e0d1af4051320b368eb31872881bc79759b334))
* **deps:** bump spring-boot-devtools from 2.6.3 to 2.6.4 ([077e0b4](https://git.griefed.de/Griefed/ServerPackCreator/commit/077e0b4a387c912de8990469f54bb238fdef05d4))
* **deps:** bump spring-boot-starter-artemis from 2.6.3 to 2.6.4 ([7b651ca](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b651ca2bf8efbaf84b14d5465b8a739c74c2743))
* **deps:** bump spring-boot-starter-data-jpa from 2.6.3 to 2.6.4 ([74a7ada](https://git.griefed.de/Griefed/ServerPackCreator/commit/74a7adab6934bd62cd55a356ebc9cde1ec86a606))
* **deps:** bump spring-boot-starter-log4j2 from 2.6.3 to 2.6.4 ([536134d](https://git.griefed.de/Griefed/ServerPackCreator/commit/536134d96b352227981c37215f72ce5336f6af27))
* **deps:** bump spring-boot-starter-quartz from 2.6.3 to 2.6.4 ([2211d4d](https://git.griefed.de/Griefed/ServerPackCreator/commit/2211d4d67bc7bc14097859a879e78270dcb80902))
* **deps:** bump spring-boot-starter-test from 2.6.3 to 2.6.4 ([f27d94d](https://git.griefed.de/Griefed/ServerPackCreator/commit/f27d94d6942a241bdf13948940c7c454921f020c))
* **deps:** bump spring-boot-starter-validation from 2.6.3 to 2.6.4 ([9f66c07](https://git.griefed.de/Griefed/ServerPackCreator/commit/9f66c0711de1a88bdb555dcc15fe3c009b9df8de))
* **deps:** bump spring-boot-starter-web from 2.6.3 to 2.6.4 ([c017e87](https://git.griefed.de/Griefed/ServerPackCreator/commit/c017e87f03cdce98511b271ddf084fba372dee85))


### 🛠 Fixes

* **UpdateChecker:** Update VersionChecker to 1.0.7 and refactor UpdateChecker to DI to prevent false positives and correctly check for available updates. ([341d2d1](https://git.griefed.de/Griefed/ServerPackCreator/commit/341d2d1403922df2f685ad94d1a996eac6645645))


### Other

* **deps:** update actions/setup-java action to v3 ([90a6baf](https://git.griefed.de/Griefed/ServerPackCreator/commit/90a6baf68ab7a4f73e8da5c74dfa92eb686a79b9))
* **deps:** update dependency @typescript-eslint/eslint-plugin to v5.12.1 ([c58b2a0](https://git.griefed.de/Griefed/ServerPackCreator/commit/c58b2a0722e9a80322c9a88170dd7d20246ff6b4))
* **deps:** update dependency eslint to v8.10.0 ([2e5f498](https://git.griefed.de/Griefed/ServerPackCreator/commit/2e5f4985b73024908e94acb9d1f4a75d3e10dc94))
* **deps:** update dependency eslint-plugin-vue to v8.5.0 ([b30b829](https://git.griefed.de/Griefed/ServerPackCreator/commit/b30b829d56525606d1f365abdbbc8f3e70f31699))
* **deps:** update dependency ghcr.io/griefed/baseimage-ubuntu-jdk-8 to v2.0.6 ([5941f91](https://git.griefed.de/Griefed/ServerPackCreator/commit/5941f9133f29623800d46da9fedd419c2618645a))
* **deps:** update dependency ghcr.io/griefed/gitlab-ci-cd to v2.0.3 ([bad28e8](https://git.griefed.de/Griefed/ServerPackCreator/commit/bad28e82029e6e4e429a0e8468551d99265095c0))
* **deps:** update dependency tsparticles to v1.41.5 ([71fd7cb](https://git.griefed.de/Griefed/ServerPackCreator/commit/71fd7cb1d1b6a4589f44ae201cd42c78c0aefccd))
* **deps:** update plugin edu.sc.seis.launch4j to v2.5.2 ([4e515f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/4e515f41687b5c13fa1a431ee5f664dc9f7748c3))
* **deps:** update spring boot to v2.6.4 ([7ceabfc](https://git.griefed.de/Griefed/ServerPackCreator/commit/7ceabfc1acf20f1f388209108255958bc74e6101))

## [3.0.0-beta.6](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.5...3.0.0-beta.6) (2022-02-25)


### 💎 Improvements

* **Clientside Mods Help:** Expand help text for clientside only mods with a more detailed explanation of possible ways of configuration ([2ba30ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/2ba30ea8c6727e24f89e133d8dc929fcbefa2228))


### Other

* **README:** Fix markdown formatting. Thanks GitHub/IDEA, for showing different renderings of the same markdown file. *le sigh* ([bc780b5](https://git.griefed.de/Griefed/ServerPackCreator/commit/bc780b5f11dc8c0c3999f9fb992e5fbc93e053e7))

## [3.0.0-beta.5](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.4...3.0.0-beta.5) (2022-02-25)


### 🛠 Fixes

* **VersionChecker:** Update VersionChecker to version 1.0.6, closing issue [#256](https://git.griefed.de/Griefed/ServerPackCreator/issues/256) on GitHub. ([08c16ca](https://git.griefed.de/Griefed/ServerPackCreator/commit/08c16ca21006007263a9d903fbce9522c55ed5a5))


### Other

* **Icon:** Update icon template with layers for Addons Overview and Example Addon ([3df0c10](https://git.griefed.de/Griefed/ServerPackCreator/commit/3df0c101e98ee8e403a6dd2770ecb8b8e6e0577f))

## [3.0.0-beta.4](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.3...3.0.0-beta.4) (2022-02-21)


### 🦊 CI/CD

* **deps-dev:** bump @types/node from 17.0.17 to 17.0.19 in /frontend ([8ae217b](https://git.griefed.de/Griefed/ServerPackCreator/commit/8ae217bf2b621f060b77d18b13f696c2c770e890))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([9cccb82](https://git.griefed.de/Griefed/ServerPackCreator/commit/9cccb82e522181a5017ac1c879ebde65e1f30dfc))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([ca93040](https://git.griefed.de/Griefed/ServerPackCreator/commit/ca93040d6b76c1f538f66a8fd8ccdb118976b744))
* **deps-dev:** bump eslint-config-prettier in /frontend ([e3f70e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3f70e1dd7cbd9433b009fc7b6ff690d111cc5f5))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.4 to 2.0.5 ([6bbacef](https://git.griefed.de/Griefed/ServerPackCreator/commit/6bbacef14f16213d42c3d2a83e0aeacc1837cb95))
* **deps:** bump JamesIves/github-pages-deploy-action ([c63a20d](https://git.griefed.de/Griefed/ServerPackCreator/commit/c63a20d71daec6684ed437857b7c6920859c34dc))
* **deps:** bump tsparticles from 1.41.1 to 1.41.4 in /frontend ([dc8440e](https://git.griefed.de/Griefed/ServerPackCreator/commit/dc8440e0ad9689c2336c7e72918d1e3e6e7ceb05))
* **GitHub:** Correctly execute (pre)release actions when tags are pushed. ([19c24c3](https://git.griefed.de/Griefed/ServerPackCreator/commit/19c24c3aa0f504ca3f1a7e0c726c8d08ff578b79))


### Other

* **deps:** update dependency core-js to v3.21.1 ([9c612f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/9c612f2d94cf8001789ba2ee3327d0836fe5e40d))
* **deps:** update dependency tsparticles to v1.41.2 ([b777818](https://git.griefed.de/Griefed/ServerPackCreator/commit/b777818b5f455b60e742f0bcd0d7fe93821472bc))

## [3.0.0-beta.3](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.2...3.0.0-beta.3) (2022-02-18)


### 📔 Docs

* Do not display the version *dev* in the title of the Java documentation ([124c19f](https://git.griefed.de/Griefed/ServerPackCreator/commit/124c19f4ac5fe2b6cd291c308890a1338ccf7d6d))


### 🛠 Fixes

* Prevent file-lock from mod-sideness-scanning. Thanks to @Seniorendi for reporting. ([28a88dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/28a88dc3890d843677723cbdeed0847f725a4533))

## [3.0.0-beta.2](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-beta.1...3.0.0-beta.2) (2022-02-14)


### 📔 Docs

* **package-info:** Provide package information for all our packages giving more information about their purpose. ([2f420eb](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f420eb99c067b68b3da2b76b74eaa8ecc30d43f))


### 🦊 CI/CD

* **deps-dev:** bump eslint from 8.7.0 to 8.9.0 in /frontend ([9268eb9](https://git.griefed.de/Griefed/ServerPackCreator/commit/9268eb9dbacf4903d24152e72774397c71f95b1f))
* **deps:** bump axios from 0.25.0 to 0.26.0 in /frontend ([02e8739](https://git.griefed.de/Griefed/ServerPackCreator/commit/02e8739e8548c4dec5973ab45bde5d7fc1e86e61))
* **deps:** bump follow-redirects from 1.14.7 to 1.14.8 in /frontend ([2bde3af](https://git.griefed.de/Griefed/ServerPackCreator/commit/2bde3af44e9def9c685911a6097ef41f7a5ac78a))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.3 to 2.0.4 ([b560e65](https://git.griefed.de/Griefed/ServerPackCreator/commit/b560e65133acb81c21e8fc22d61215f97d991450))
* **deps:** bump tsparticles from 1.41.0 to 1.41.1 in /frontend ([469ce7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/469ce7e15bd3e993c159357d358e1830662922e9))
* **deps:** bump vue from 3.2.30 to 3.2.31 in /frontend ([674ff6b](https://git.griefed.de/Griefed/ServerPackCreator/commit/674ff6ba538dcc855a80b9d8c61aec4a68f22c5b))


### 🛠 Fixes

* **ci:** Remove mv statement in GitHub release workflow causing the job to fail because of identical file names. ([e671001](https://git.griefed.de/Griefed/ServerPackCreator/commit/e671001dd10618ef342d948897aac21cb73c0847))
* **clientside-mods:** Add Optifine and OptiForge to list of clientside-only mods. ([eab294f](https://git.griefed.de/Griefed/ServerPackCreator/commit/eab294fd8c973512fb9c362e8a5721aec043e204))
* **CurseForge:** Remove some more mentions of generating a server pack from CurseForge as it is currently impossible to do so. ([7fbd22e](https://git.griefed.de/Griefed/ServerPackCreator/commit/7fbd22e24ed75586cfcb0bfc2227e5dc3c445421))


### Other

* **deps:** update dependency eslint to v8.9.0 ([462f3d3](https://git.griefed.de/Griefed/ServerPackCreator/commit/462f3d36f47f90312ffa97caec9da6d4cd15ee6a))
* **deps:** update dependency gradle to v7.4 ([a636cbe](https://git.griefed.de/Griefed/ServerPackCreator/commit/a636cbe79aa4b0f1a14298c053804775c5cd8158))
* **deps:** update ghcr.io/griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.4 ([029c810](https://git.griefed.de/Griefed/ServerPackCreator/commit/029c810751db62f42ba1c8b08dfde3735e87fc40))
* **deps:** update ghcr.io/griefed/gitlab-ci-cd docker tag to v2.0.1 ([bf76d58](https://git.griefed.de/Griefed/ServerPackCreator/commit/bf76d58525bc75e65fb0dfdc3f1ae8541d1def6c))
* **README:** Rephrase addons section and include link to new addons overview website ([a12ce06](https://git.griefed.de/Griefed/ServerPackCreator/commit/a12ce06c2c898bd14588688d517e696432eae69f))

## [3.0.0-beta.1](https://git.griefed.de/Griefed/ServerPackCreator/compare/2.1.1...3.0.0-beta.1) (2022-02-11)


### :scissors: Refactor

* Add -help argument explaining the basics of running ServerPackCreator. If -help is used, said help text is printed to the console and ServerPackCreator exited. ([4689f54](https://git.griefed.de/Griefed/ServerPackCreator/commit/4689f543359d7a5850d8cd26f2856ff88b719969))
* Add -lang argument information to -help display ([164073f](https://git.griefed.de/Griefed/ServerPackCreator/commit/164073fc8b1a461d35f94921fb2f444728672738))
* Add additional catch for NPE. Fix typo in docs. Remove unused field. ([b5f9042](https://git.griefed.de/Griefed/ServerPackCreator/commit/b5f90421002124b7a1e53f2c11581ead7fab00a2))
* Add getters/setters and allow reloading of properties. Also add some documentation to properties as well as more default values, just to be on the safe side ([049925e](https://git.griefed.de/Griefed/ServerPackCreator/commit/049925e9ddad7e89ed5f735ddb33da9325375a86))
* Allow closing of notification if status is already exists ([a25e6f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/a25e6f7b191a08e35f8b83d5911e9ac8bc9c11c8))
* Allow configuration of hastebin server in serverpackcreator.properties. ([0235378](https://git.griefed.de/Griefed/ServerPackCreator/commit/023537882243979fd7f2b66fc69113eb43477902))
* Be more specific with not found language key ([129877b](https://git.griefed.de/Griefed/ServerPackCreator/commit/129877bebe2691663cd7dc962b2bfd73f7dae796))
* Build for armv7 again thanks to [@djmaze](https://git.griefed.de/djmaze) and their dind-image-with-armhf available at https://github.com/djmaze/dind-image-with-armhf. Store and read version more efficiently by writing it to the manifest. ([d5bde7b](https://git.griefed.de/Griefed/ServerPackCreator/commit/d5bde7b7d2f0f073753b94c9f8a0e382d3280c6e))
* Change banner being displayed when running as webservice ([75899d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/75899d4d211647acf9de589007bfeaa88664cf23))
* Change groupID. Also change url for OSSRH to the correct snapshot url. ([e9ff899](https://git.griefed.de/Griefed/ServerPackCreator/commit/e9ff899023f5f2386653cf49e29dd9cea87ab99e))
* Change groupID. Also change url for OSSRH. Now everything works when ([0cf5dbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/0cf5dbccc8f40cf16e28a4011ede3264a7626076))
* Change labels for Minecraft, modloader and modloader version to better reflect new feature of selection from lists. ([84755a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/84755a185c02948050d0e534b2a5771898f13aff))
* Combine start and download scripts. Add checks for files in scripts. Removes option to generate scripts and generates them always instead. Closes issue [#81](https://git.griefed.de/Griefed/ServerPackCreator/issues/81) ([f037c34](https://git.griefed.de/Griefed/ServerPackCreator/commit/f037c34eb43b4910ea3002eba6362dd3d749261a))
* Copy log4j2.xml to basedir where JAR/EXE is executed. Improve logging-configuration and allow user to set level to DEBUG/INFO with '<Property name="log-level-spc">DEBUG</Property>' ([fcbe6cf](https://git.griefed.de/Griefed/ServerPackCreator/commit/fcbe6cfade911ee429bffd47b82cbe71b7f0d2bc))
* Create empty serverpackcreator.properties. Makes manual migrations by users more unlikely while at the same time reducing risk of users breaking SPC with misconfigurations ([98c9a70](https://git.griefed.de/Griefed/ServerPackCreator/commit/98c9a70f6cd7deed6a0705f8589cc964824d765b))
* Create modpacks downloaded from CurseForge in the work/modpacks-directory. ([3178326](https://git.griefed.de/Griefed/ServerPackCreator/commit/3178326cc960bde4482e847c5464ef4f50ed856c))
* DI serverpackcreator.properties everywhere! ([4b01d4a](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b01d4a809a08e420d399af9b9e58dca2c526002))
* Disbale whitelist for tempalte server.properties ([bc4018e](https://git.griefed.de/Griefed/ServerPackCreator/commit/bc4018edf2c33a240f4cdf7d9d1ad4378854c8ba))
* Display download button only if status is Available ([9c0edc7](https://git.griefed.de/Griefed/ServerPackCreator/commit/9c0edc71c4180725269d1a3ddcb7ca4958d89e4d))
* Display MB behind size ([1188b9f](https://git.griefed.de/Griefed/ServerPackCreator/commit/1188b9f0c687e3301e1e4d695450e0b5f1890f71))
* Do not directly access the ServerPackRepository ([ba4bf2c](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba4bf2c9e57a0b982913dce816724d6c32f78edb))
* Extract actions and events into separate methods. Improves maintainability. ([7a335da](https://git.griefed.de/Griefed/ServerPackCreator/commit/7a335dab87acbd4f136e520fb6c1af012659606d))
* Extract actions and events into separate methods. Improves maintainability. ([9268245](https://git.griefed.de/Griefed/ServerPackCreator/commit/9268245df88d96fbe358b68de488992e102d448c))
* Finish TODOs. Setup missing lang keys. Minor improvements to tests ([b884e7a](https://git.griefed.de/Griefed/ServerPackCreator/commit/b884e7a77469135a5e3eb0bf56c44fb1249d7f76))
* Gather information from CurseForge modpack from JsonNodes instead of Class-mapping. Makes maintenance and expansion easier. Reduces complexity. ([caa033b](https://git.griefed.de/Griefed/ServerPackCreator/commit/caa033bae0d54a5e7171871ea7023e99fc5c99a0))
* Generate server packs in ./server-packs in the directory where ServerPackCreator is executed in. Prevents 1. in [#55](https://git.griefed.de/Griefed/ServerPackCreator/issues/55) where the Overwolf CurseForge App filewatcher can cause installed mods to disappear due to copying mods around inside the modpack directory. ([539341d](https://git.griefed.de/Griefed/ServerPackCreator/commit/539341d68f54965b958d74e11e7e9fcc31da9ada))
* Improve automatic acquisition of java path from system environment. ([fae311e](https://git.griefed.de/Griefed/ServerPackCreator/commit/fae311ea2e5f0c38c7caec7a06d06ed43957eae5))
* Improve configuration check and tests. Add more debug logging. Add tests. ([b6da489](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6da489e08da8a20074f32ae938658649b982f3e))
* Improve debug logging for VersionLister ([29be15f](https://git.griefed.de/Griefed/ServerPackCreator/commit/29be15fa5ba18ce8bdb0f4345e989ef843a63e75))
* Improve dialog after uploading config and logs to hastebin ([da5e298](https://git.griefed.de/Griefed/ServerPackCreator/commit/da5e2981333806adf93f63bb549a48cb5d1e91b3))
* Improve dialog after uploading config and logs to hastebin ([13f4587](https://git.griefed.de/Griefed/ServerPackCreator/commit/13f4587e736743ae9217a12562077bcaeb33023b))
* Improve error handling and reporting ([77985b6](https://git.griefed.de/Griefed/ServerPackCreator/commit/77985b6f23fa95d388b349a016d090a480a869aa))
* Improve update checks by sequentially checking GitHub, GitGriefed and then GitLab ([c25eaac](https://git.griefed.de/Griefed/ServerPackCreator/commit/c25eaacd6767b721a7624847f40dd3639c7f7430))
* Initialize addons and check/create files when creating our DefaultFiles and AddonsHandler instances. ([864f10c](https://git.griefed.de/Griefed/ServerPackCreator/commit/864f10cd33e7f06693e47791ceeb7ac9a9e16974))
* Instantiate CreateGui only when GUI is actually about to be used ([d39730c](https://git.griefed.de/Griefed/ServerPackCreator/commit/d39730c86c9e8726716d2f6a4ca15bba3743ad5a))
* Just kill it. ([b6bbe54](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6bbe54ad03f89505350e9714af2d65ef6fec1fb))
* Just some renamings...nothing important. ([2c65582](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c65582691abf06558deaf4461c90265770bb6d1))
* Merge checkJavaPath and getJavaPathFromSystem ([0c982cb](https://git.griefed.de/Griefed/ServerPackCreator/commit/0c982cb5abd629e21fbc23c08b0a76240a4ea11f))
* Modloader setting as a slider to select either Forge of Fabric ([4f9eb79](https://git.griefed.de/Griefed/ServerPackCreator/commit/4f9eb79f813d3f127d89d99151163f3186dabcf9))
* More work towards allowing parallel runs of server pack generation. Split Configuration into ConfigurationModel and ConfigurationHandler ([cb3e8a7](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb3e8a79e86c023a35d5224a5f31b1539903c59e))
* Move assignemts to field declaration where applicable. Extract method for adding MouseListeners to buttons. ([b37ad30](https://git.griefed.de/Griefed/ServerPackCreator/commit/b37ad30ce88e570e4b8632760dee5cebab28f8da))
* Move helper and utility methods to separate classes. Reorganize code. More and improved unit tests. Add a little info text to start scripts for Minecraft 1.17+ as well as print of Java version ([e41e97c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e41e97c1e31dd05aba19b5b429491d013401020a))
* Move language specification from lang.properties to serverpackcreator.properties. Move FALLBACKSMODSLIST to serverpackcreator.properties. ([bb11972](https://git.griefed.de/Griefed/ServerPackCreator/commit/bb119727113ba0cb8e58977348673860bcb47851))
* Move ObjectMapper init to getter like in ConfigurationHandler ([d73ebd4](https://git.griefed.de/Griefed/ServerPackCreator/commit/d73ebd40e3a77dc512bd4f542eb5780fa9663a3a))
* Move ObjectMapper init to getter like in ConfigurationHandler ([ac955c5](https://git.griefed.de/Griefed/ServerPackCreator/commit/ac955c520f434fba1dedaf0299213f6b85489709))
* Move plugins folder creationf to DefaultFiles. Create example file for disabling plugins. Improve logging for installed plugin extensions. ([1fad8ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/1fad8ac858377c43250d4f6f644ecf7c719c7e02))
* Move script creation to separate methods and refactor write.write() to increase readability and maintainability. Also, move info regarding EULA agreement. ([65121a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/65121a2a8e7adaac47c25e2b08498b7b6cbb61d7))
* Only check for database existence when running as a webservice ([87618f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/87618f4f99d9376de0dd5ffc135265fec35cebef))
* Only provide translations for messages which actually have a need for translation. Error/debug messages mainly do not need to be translated, as those will be reported in issues, therefore I need to be able to read them. ([2132baa](https://git.griefed.de/Griefed/ServerPackCreator/commit/2132baa6a19000ffdabec555a3e3bca5c8fc0708))
* Prevent going through a list of clientside-only mods automatically gathered from modpack is property is false. ([51a3e42](https://git.griefed.de/Griefed/ServerPackCreator/commit/51a3e42ea18e37453734c5cc6c4e2e63fea8bfee))
* Print server-icon and server.properties paths. Re-organize method in CreateServerPackTab to ensure GUI becomes responsible again if the generation of a server pack fails. ([e42b3b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/e42b3b1aaac9845bbf053d49705b8cb044eb3c07))
* Provide improved Fabric Server Launcher as well as old launcher. Create SERVER_PACK_INFO.txt with information about said improved launcher. Thanks to @TheButterbrotMan for the detailed conversations in issue [#202](https://git.griefed.de/Griefed/ServerPackCreator/issues/202) ([6148a3e](https://git.griefed.de/Griefed/ServerPackCreator/commit/6148a3eca54543171d3c63f8336b4a01acc2f407))
* Rearrange some fields ([4592b70](https://git.griefed.de/Griefed/ServerPackCreator/commit/4592b7041a130204a8847e775cc077ab8c64c498))
* Refactor lang keys to better reflect where they're used. Add more lang keys for logging. Improve wording. Fix some minor typos. ([354fb2e](https://git.griefed.de/Griefed/ServerPackCreator/commit/354fb2e7003df6293ebb496c22d085493eb868c5))
* Refactor lang keys to better reflect where they're used. Add more lang keys for logging. Improve wording. Fix some minor typos. ([9553557](https://git.griefed.de/Griefed/ServerPackCreator/commit/9553557d40a129194c3b2fd478b83805f35b0805))
* Refactor tailers to run in threads. ServerPackCreator can still become unresponsive if you resize during zip-creation, after a Forge server was installed, though..... ([d4c986e](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4c986eaa2451989420fa9785fab6f86523c8755))
* Remove elements starting with ! from list instead of avoiding them with an ugly if-statement ([b8c84e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/b8c84e1294d7e8feebd34a0da202f8dc60d02d78))
* Remove preparations for 1.12 and older clientside autodetection. See https://github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692 ([3638e22](https://git.griefed.de/Griefed/ServerPackCreator/commit/3638e22dd96cea72ec86d22f7c16d335eefa9bf0)), closes [/github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692](https://git.griefed.de/Griefed//github.com/Griefed/ServerPackCreator/issues/62/issues/issuecomment-901382692)
* Remove preparations for 1.12 and older clientside autodetection. See https://github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692 ([4977ae7](https://git.griefed.de/Griefed/ServerPackCreator/commit/4977ae7f01db82b79b1af0057e505877e4307ad9)), closes [/github.com/Griefed/ServerPackCreator/issues/62#issuecomment-901382692](https://git.griefed.de/Griefed//github.com/Griefed/ServerPackCreator/issues/62/issues/issuecomment-901382692)
* Remove/extract commonly used fields and methods. Make sure our database is always present. Other. ([859ede1](https://git.griefed.de/Griefed/ServerPackCreator/commit/859ede176db6ae995c72405b95c584de298300ef))
* Remove/extract commonly used fields and methods. Work towards webservice ([abf0135](https://git.griefed.de/Griefed/ServerPackCreator/commit/abf01355447f0c3a0af4af97d1cac259ddc113fd))
* Remove/extract commonly used fields/methods ([1f40517](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f405176a505bfcb5932493f94924bf45e2ade19))
* Remove/extract commonly used fields/methods ([df84569](https://git.griefed.de/Griefed/ServerPackCreator/commit/df845695059550025d0f24326d69a9f7ebf3d9f4))
* Remove/extract commonly used fields/methods ([c9cc954](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9cc9548973d7b181ff91175ac1bd5959740c81f))
* remove/extract commonly used fields/methods. Use configurationModel for everything. ([4ea254f](https://git.griefed.de/Griefed/ServerPackCreator/commit/4ea254fcf3aa6503efb8a168d54346af45f93150))
* Rename and sort classes and packages to make more sense. ([5ca227d](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ca227d79a0dfcb40effe9eb344da9575cf8e9bc))
* rename applicationProperties field ([533c850](https://git.griefed.de/Griefed/ServerPackCreator/commit/533c850300e6dfa17fa6607bc2ae738e45a22b78))
* Rename applicationProperties field ([781e1cd](https://git.griefed.de/Griefed/ServerPackCreator/commit/781e1cdedfc303f933bea618b72a404e258b5027))
* Rename fields still referencing old serverPackCreatorProperties to applicationProperties ([e1b7c62](https://git.griefed.de/Griefed/ServerPackCreator/commit/e1b7c6254a710f5f2a3436090782f079d1f433e4))
* Replace e.getStateChange() with ItemEvent.SELECTED. ([ab87c06](https://git.griefed.de/Griefed/ServerPackCreator/commit/ab87c06ea99443fa6856a152fd15d07fdd395c4e))
* Replace file-saver with call to api. Improves downloading of server packs. ([b60aeb7](https://git.griefed.de/Griefed/ServerPackCreator/commit/b60aeb7ddbb8b1f3354cae2313136c7a193fc917))
* Replace name or property-file to correct one ([ee0aab7](https://git.griefed.de/Griefed/ServerPackCreator/commit/ee0aab7a3fec9a3828e4248877bf1f968dc151c2))
* Replace slider for modloader selection with radio buttons. Looks better and cleaner. Selection fires less events than slider did. ([c36189c](https://git.griefed.de/Griefed/ServerPackCreator/commit/c36189cf5252e0fe27701e779f6e539b1d79a335))
* Require file passed to CreateServerPack.run in order to generate server pack. Create new Configuration object with said file. Should allow parallel runs in the future, but needs to be tested when I get to that. ([67c0cba](https://git.griefed.de/Griefed/ServerPackCreator/commit/67c0cba498dece33f265c376c88cbe4b3ac6e77a))
* Reverse lists of modloader versions to display in order of newest to oldest versions. Closes issue [#74](https://git.griefed.de/Griefed/ServerPackCreator/issues/74). ([4534d87](https://git.griefed.de/Griefed/ServerPackCreator/commit/4534d8774056f9de3d2063ea130c7bd85a4a6137))
* Rework checkConfiguration to provide more ways of checking a given configuration. Require checks to run before passing to run(...). ([a3ecd11](https://git.griefed.de/Griefed/ServerPackCreator/commit/a3ecd11c58cf044c58d1f39c0b62bc30a729e189))
* Rework error redirect. ([85543ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/85543ac9f6fc7385c0e634fa60c78cec4e289c01))
* Rewrite unzipping of CurseForge acquired modpack with zip4j library ([9f8c87f](https://git.griefed.de/Griefed/ServerPackCreator/commit/9f8c87fca09beb239030b4228958a0e52c0d83c1))
* Set clientMods and javaPath with fallback-list and system environemnt respectively, if the config is empty or an invalid javaPath was specified. ([ff18c5e](https://git.griefed.de/Griefed/ServerPackCreator/commit/ff18c5e56f1416316a20158f66ce9f24c1ff7cd5))
* Set logger context with log4j2.component.properties ([7038dcf](https://git.griefed.de/Griefed/ServerPackCreator/commit/7038dcf76e61ca4adf85a2d842f4cdeafbc409e7))
* Set rate at which tailers tail to 100ms, instead of 2000ms. ([ba4624f](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba4624f9116f248ac5953e90c1209b50990c4155))
* Set server-packs directory to /server-packs. Add new configuration to config. Add volume to Dockerfile. Update documentation in README ([267e3e9](https://git.griefed.de/Griefed/ServerPackCreator/commit/267e3e9f168803209e26f8038a4c14d16d30b920))
* Set status to Queued for a new instance ServerPack ([e2eb166](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2eb166e31a3a26a145283b68242c996cff65884))
* Simplify default files setup by merging methods which create our files. Instead of a separate method for each file, we have one method which gets passed different parameters depending on which file we want to setup. Makes maintenance easiert and code easier to read. ([9111e7c](https://git.griefed.de/Griefed/ServerPackCreator/commit/9111e7c58508700b31efeb617f110bae9a8b9f7f))
* Simplify log tabs to increase maintainability. Abstract classes rock! ([7fc3404](https://git.griefed.de/Griefed/ServerPackCreator/commit/7fc3404df9577c15493c6b98905792e0860c5ecd))
* Simplify server installation to increase maintainability ([7bec08a](https://git.griefed.de/Griefed/ServerPackCreator/commit/7bec08a7e774f2935d34933b95b4624677e27737))
* Sort by downloads, descending ([2f6f6d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f6f6d4578b2bf5429fd2b85291850b292766e50))
* Store Fabric installer manifest in work/*. Only refresh when SPC starts. Don't delete manifest files during runs of SPC. Rename lang keys to fit usage. Other misc changes. ([1927faa](https://git.griefed.de/Griefed/ServerPackCreator/commit/1927faa33da1063ba4eea239cabcf9c6a4335b8d))
* Store Minecraft, Fabric and Forge version validation in work/*. Only refresh them when SPC starts. Setup work, work/temp and server-packs folder for future use. ([ab080a6](https://git.griefed.de/Griefed/ServerPackCreator/commit/ab080a6024138972c0b34524c4c7a728c64b8f74))
* Switch back to old pattern format so GUI looks clean again ([483bdc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/483bdc15fedcf1db513b41169affda85a99cd0b4))
* Switch options to YES_NO to ensure users is always warned about empty javapath setting if they did not choose to select it now. ([c6f4ef8](https://git.griefed.de/Griefed/ServerPackCreator/commit/c6f4ef8cfc5e138191079acbf773ab91cef0d091))
* Throw custom exceptions on incorrect IDs ([875817c](https://git.griefed.de/Griefed/ServerPackCreator/commit/875817c7ee2ea024c631b9a37794feb690e434cd))
* Upgrade to Gradle 7.2. Remove Fabric-Installer dependecy by retrieving the Minecraft server url ourselves. ([e297f63](https://git.griefed.de/Griefed/ServerPackCreator/commit/e297f6347e393359ac71b0a70c388afd759355a8))
* Use a single ExtensionFactory as per pf4j docs ([62ed8e7](https://git.griefed.de/Griefed/ServerPackCreator/commit/62ed8e76fac1d3b28df557da89d39e1f166ca14a))
* Use FIleUtils for copying ([4529017](https://git.griefed.de/Griefed/ServerPackCreator/commit/452901776346acf5318b5629367e1e3f75b2317f))
* Use FIleUtils for copying and deleting, Files for deleting files. Replace messages with lang keys ([186d610](https://git.griefed.de/Griefed/ServerPackCreator/commit/186d6107e799fda23ea6259382d6fda261eaa253))
* Use FIleUtils for copying, Files for deleting ([4459847](https://git.griefed.de/Griefed/ServerPackCreator/commit/4459847bfc94117773605e07a6dc26e6716a8c51))
* When a requested server pack already exists, offer a download to the user. ([39dc626](https://git.griefed.de/Griefed/ServerPackCreator/commit/39dc6268e8ebd1048c0e19c0a479bd731c8d1e98))
* **webservice:** Allow user to specify mode. Test whether libatomic1 works now. ([6dfa0dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/6dfa0dcf44652910c83ce8b269929893aa04a4b3))
* **webservice:** Display status as "Generating" if server pack is being generated. Refactor regeneration to use queueing-system. ([78b88f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/78b88f22b18ba87723d3808586b496abcc3ab25e))
* **webservice:** Move ScanCurseProject and GenerateCurseProject to separate classes to eliminate statics. Closes GL[#88](https://git.griefed.de/Griefed/ServerPackCreator/issues/88) ([5815eb7](https://git.griefed.de/Griefed/ServerPackCreator/commit/5815eb7e8dd2abc7a0cdc2287e950b2f0bb2e683))
* **webservice:** Remove unnecessary logging ([a619997](https://git.griefed.de/Griefed/ServerPackCreator/commit/a6199977958c4040657976750d9093bf6922cb4f))
* **webservice:** Set download-filename to fileDiskName + _server_pack-zip ([e597dc4](https://git.griefed.de/Griefed/ServerPackCreator/commit/e597dc4804896d971951f183e09a585a8943a956))
* **webservice:** Set initial rows per page to 13 ([e45cf0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/e45cf0e21a0b535f06358aa37016b3c8d38590a6))
* **webservice:** Set logging pattern for Spring to ours ([4348f76](https://git.griefed.de/Griefed/ServerPackCreator/commit/4348f7601b5d2818b0bd343e2f0cb33cab02e2ec))
* **webservice:** Store size in MB and display size in frontend in MB ([37d4daa](https://git.griefed.de/Griefed/ServerPackCreator/commit/37d4daa3e2863ab6077174d9249478c0ea179a1a))


### ⏩ Performance

* Improve project- and filename acquisition by checking project and files directly ([f6e7b54](https://git.griefed.de/Griefed/ServerPackCreator/commit/f6e7b5454e316ad3f7acb0958d69476e3dcbf163))
* Perform version checks with lists gathered by VersionLister. ([d440e5e](https://git.griefed.de/Griefed/ServerPackCreator/commit/d440e5e2c079ac44bc040d87cacb1f29951160d9))
* Retrieve Forge versions from HashMap with Minecraft version as key instead of re-reading list and lists and arrays of data again and again and again, ([0018abc](https://git.griefed.de/Griefed/ServerPackCreator/commit/0018abc4772b7e062fc5bd131a62edcceae4aac6))


### 👀 Reverts

* Do not create the eula.txt-file automatically. Reverts feature request issue [#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83). Lots of other smaller things, too many to list. ([ae66641](https://git.griefed.de/Griefed/ServerPackCreator/commit/ae66641b4e66e4711069289c79427651d10aaf11))
* Maybe another time ([f7ea248](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7ea248f50ef2dbbdc99fa4538c9561d35e96ea7))
* Re-implement removal and change of new entries to copyDirs and clientMods ([eec45d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/eec45d5950b088625760187b070bace44940d57e))


### 💈 Style

* Declare fields above constructor. Only have methods under constructor. ([76c6b58](https://git.griefed.de/Griefed/ServerPackCreator/commit/76c6b584b05d48adf0714f4ad066c6cf0f5d775a))
* Reorder calls in Main.main to reflect importance. Makes it slightly more readable as well. ([576cbae](https://git.griefed.de/Griefed/ServerPackCreator/commit/576cbae9938563ef50dd27f174b3f340c4998f60))


### 📔 Docs

* Add author tags. Add link to GitHub issues in case anyone wants something added to fallbackModslist or directories for CurseForge automation. ([7699c64](https://git.griefed.de/Griefed/ServerPackCreator/commit/7699c64d4f7d14f3d13b86acb92489c1c0ba2a33))
* Add call to initializeAddons to main description ([ac14f99](https://git.griefed.de/Griefed/ServerPackCreator/commit/ac14f996a55402d1d5b8cc8930bbb1ead57852e7))
* Add documentation for UpdateChecker utility. ([f804589](https://git.griefed.de/Griefed/ServerPackCreator/commit/f8045896d075fc67d0befa1565e88ddd1a831ba5))
* Add javadoc for scanAnnotations ([e0a08f9](https://git.griefed.de/Griefed/ServerPackCreator/commit/e0a08f9547891a2807fd20a89927856b2a86329d))
* Add missing method to table ([d1fca12](https://git.griefed.de/Griefed/ServerPackCreator/commit/d1fca12b00b8b79cf0ede59d58295eeb61a80c5c))
* Add missing method to table ([f04b728](https://git.griefed.de/Griefed/ServerPackCreator/commit/f04b72818257e1d71b2e60dd86af8921c32e45eb))
* Add missing parameter to setJavaArgs ([761e2fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/761e2fdcc110e96db825527471c60cc427078552))
* Add missing throws ([4538f54](https://git.griefed.de/Griefed/ServerPackCreator/commit/4538f547b291d5b02619e3f366ab53fff63726e9))
* Change version dropdown to input ([c5a5893](https://git.griefed.de/Griefed/ServerPackCreator/commit/c5a589358382085c7cf416f3608150bd012998bb))
* Cleanup changelog due to some sort of tag issue I created. Yay. ([17c234b](https://git.griefed.de/Griefed/ServerPackCreator/commit/17c234bfbe56760cefd07bf98b3e7357f8167a55))
* Cleanup changelog due to some sort of tag issue I created. Yay. ([65bf366](https://git.griefed.de/Griefed/ServerPackCreator/commit/65bf366c368f13de51f2f8963d7c3ce9ecbc954b))
* Don't include private methods in documentation ([719b4f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/719b4f2e8aec75075fda349383a305cce8aebf1a))
* Exclude certain classes from JaCoCo test coverage ([1f4cfbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f4cfbc73bdcd9267bb9e56e8bbd95ff7a8b1866))
* Generate patch release on docs change. ([d6e65ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/d6e65eadb8e5c5071d8b8a693433ae7e38aa2582))
* List minigame example addon ([3577d33](https://git.griefed.de/Griefed/ServerPackCreator/commit/3577d33dae6cc895d3fbb97f57d9bcc4b716ecc2))
* List server-packs directory for volumes ([82b13e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/82b13e43771a2964d1d6339994dd431e94701a67))
* Name correct filename for properties according to merge of lang.properties with serverpackcreator.properties ([ed42dcd](https://git.griefed.de/Griefed/ServerPackCreator/commit/ed42dcd14479013e979f9793aae884b0c0cf1836))
* Spelling and grammar fixesas well as [@author](https://git.griefed.de/author) tag fixes. ([9d157d6](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d157d6227ac3c484b740297c012f817c169abde))
* Update CONTRIBUTING with step-by-step guide on how to contribute to ServerPackCreator ([db3b061](https://git.griefed.de/Griefed/ServerPackCreator/commit/db3b06100510d2a2e35c0ce92cbf6c04d01c6b1f))
* Update licenses ([21ae0ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/21ae0ad3f704b997ac4823a447fbeae1c9bbe1a1))
* Update README with info regarding contributions. Closes GL[#75](https://git.griefed.de/Griefed/ServerPackCreator/issues/75). ([e3d499c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3d499cf948f58084ee2afd8568bdb50ba483d3a))
* Update README with new feature information and reflect changes made to file-structure ([04ffed5](https://git.griefed.de/Griefed/ServerPackCreator/commit/04ffed5e30c450520132d984e0c2974cafc777d1))
* Update README with new feature information and reflect changes made to file-structure ([b3f211c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b3f211cf51abd589672fe3005f0cfc9ef76cec76))
* Update table of methods ([dabf028](https://git.griefed.de/Griefed/ServerPackCreator/commit/dabf02866d58a72159642452c46b3ca6f109791a))
* Update table of methods for classes ([eeb6887](https://git.griefed.de/Griefed/ServerPackCreator/commit/eeb6887e3b52f67dd431adfe997ce1c144ab28fc))
* Update templates ([9fe1101](https://git.griefed.de/Griefed/ServerPackCreator/commit/9fe11013ba346443124d5c2cadb1364e4633cef7))
* Write docs for all the REST API classes, methods etc. I've been working on for the last couple of weeks. This commit also contains some minor refactorings, but nothing major or worth a separate commit. ([26519a0](https://git.griefed.de/Griefed/ServerPackCreator/commit/26519a002538bc01de17ad6debbb45d334527694))
* Write documentation for fabric-server-launch replace method ([7ab20eb](https://git.griefed.de/Griefed/ServerPackCreator/commit/7ab20eb47a2149271cf461dba0d0f0a0b1ad40d5))
* Write missing documentation for getters and setters for javaargs and javapath settings ([f29924b](https://git.griefed.de/Griefed/ServerPackCreator/commit/f29924bd00724b53669c51829b1497810b8596fb))
* **webservice:** Enable debug log output for Docker build ([eaae701](https://git.griefed.de/Griefed/ServerPackCreator/commit/eaae701fb7d5666251a07f93a8bcd67fa4785b3a))


### 🦊 CI/CD

* Add signing and publishing. Will be published to GitLab, GitHub, git.griefed, OSSRH on new tag creation. ([b60a8f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/b60a8f2a63c986eb609975f8299719aa9f731e32))
* Pass host for git clone so we can always clone from the infrastructure we are running on ([faa937a](https://git.griefed.de/Griefed/ServerPackCreator/commit/faa937ae750941fce8c52b8434a82ada816de932))
* Replace Typesafe with Nightconfig, allowing for more safety measures ([b9939b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/b9939b101e906b7a578794cf79659c5035e9c692))
* Switch to GHCR images to prevent job failures due to rate limiting by DockerHub ([bbe0c0b](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbe0c0b7e7db49189e22bcb2f2b1f55d083be6fa))
* Switch VersionChecker to library implementation. Update jms-server. Minor URL refactorings in gradle publishing. ([62c438a](https://git.griefed.de/Griefed/ServerPackCreator/commit/62c438a75d5a783d741fbacfc8c0861899892f69))
* Update dependencies ([e726f31](https://git.griefed.de/Griefed/ServerPackCreator/commit/e726f316c5928856a7b911be92d910f2ea6e6d26))
* Update dependencies. Cleanup & readability. ([fe583aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe583aa0f73326b328f2c672859053fe6c6b8b67))
* Update frontend dependencies ([d953f31](https://git.griefed.de/Griefed/ServerPackCreator/commit/d953f31dbc75f0006b34445a20e074fbc698f9bc))
* Update Gradle to 7.3 ([5dafa9e](https://git.griefed.de/Griefed/ServerPackCreator/commit/5dafa9ee7e7e6ee8beb2126296fed1853eb5f978))
* Update gradle to 7.3.1 ([88c1330](https://git.griefed.de/Griefed/ServerPackCreator/commit/88c133060f88303a6e734275c01704bb8ec4f782))
* Update Gradle to 7.3.3 ([541122b](https://git.griefed.de/Griefed/ServerPackCreator/commit/541122b0dded68e62878065bea3ea47aee55d1f5))
* Update griefed/baseimage-ubuntu-jdk-8 to 2.0.1 ([d77a61f](https://git.griefed.de/Griefed/ServerPackCreator/commit/d77a61f7e1cfd874f5ec9df05c1c56737bfd30ed))
* Upgrade dependencies ([426ec44](https://git.griefed.de/Griefed/ServerPackCreator/commit/426ec440b54ff9909d202bbdfe697d1259d7773a))
* **deps-dev:** bump @babel/eslint-parser in /frontend ([a0629ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/a0629eadd4b21b204ba2caf1732c69b8c0315415))
* **deps-dev:** bump @quasar/app from 3.2.3 to 3.2.5 in /frontend ([4d2092b](https://git.griefed.de/Griefed/ServerPackCreator/commit/4d2092bb73fe18589b5e150deebf7844c01c2198))
* **deps-dev:** bump @quasar/app from 3.2.5 to 3.2.6 in /frontend ([c53aeac](https://git.griefed.de/Griefed/ServerPackCreator/commit/c53aeac47f2b3fe0621e4abce2b89b3daf58e4d8))
* **deps-dev:** bump @quasar/app from 3.3.2 to 3.3.3 in /frontend ([ff176bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/ff176bd3bc1e844be1b6e2eea0f578cd7cc3ddc4))
* **deps-dev:** bump @types/node from 16.11.10 to 16.11.11 in /frontend ([043414e](https://git.griefed.de/Griefed/ServerPackCreator/commit/043414ebed40dadf28ddb888276c1d8ca47835e5))
* **deps-dev:** bump @types/node from 16.11.10 to 16.11.12 in /frontend ([ddd4424](https://git.griefed.de/Griefed/ServerPackCreator/commit/ddd44242048537fe22b3c2c3344a82884507c5c7))
* **deps-dev:** bump @types/node from 16.11.14 to 17.0.2 in /frontend ([d8109a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/d8109a55fd012cc8e376d47e46ee768040174b28))
* **deps-dev:** bump @types/node from 17.0.10 to 17.0.17 in /frontend ([dade4db](https://git.griefed.de/Griefed/ServerPackCreator/commit/dade4db41c2dccfc6db0ebf3752cd845cea88ba3))
* **deps-dev:** bump @types/node from 17.0.2 to 17.0.5 in /frontend ([0ae1140](https://git.griefed.de/Griefed/ServerPackCreator/commit/0ae11401030687941c00f0bf5f4696c6af4ec036))
* **deps-dev:** bump @types/node from 17.0.5 to 17.0.7 in /frontend ([9d66fc3](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d66fc3c153118d8e6555b4093d58574b6729fa1))
* **deps-dev:** bump @types/node from 17.0.5 to 17.0.8 in /frontend ([ea1383c](https://git.griefed.de/Griefed/ServerPackCreator/commit/ea1383c2bcbc60b889d262778d89d75002c86cdc))
* **deps-dev:** bump @types/node from 17.0.8 to 17.0.9 in /frontend ([a642a14](https://git.griefed.de/Griefed/ServerPackCreator/commit/a642a146fa2d2956970dc9daa01671c1b02a4873))
* **deps-dev:** bump @types/node from 17.0.9 to 17.0.10 in /frontend ([96e1d62](https://git.griefed.de/Griefed/ServerPackCreator/commit/96e1d6292a35016df0ef31bb41ed0cd1940c3cfb))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([f0c49fb](https://git.griefed.de/Griefed/ServerPackCreator/commit/f0c49fb9a5c1e6e25edf562f07a16cef023e2a87))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([f7bd184](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7bd18496b56250d00442c3f8c37aa75188ab0c0))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([3a7dffc](https://git.griefed.de/Griefed/ServerPackCreator/commit/3a7dffcd05f0610bea570e7253a96510927dca63))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([579714d](https://git.griefed.de/Griefed/ServerPackCreator/commit/579714df6f96a30796293f37dec76bc04273d647))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([55b5ba5](https://git.griefed.de/Griefed/ServerPackCreator/commit/55b5ba52f6b9c9377e730fd8d3ff0b25be52eca6))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([3795601](https://git.griefed.de/Griefed/ServerPackCreator/commit/3795601b23fd063c2ffd05d38754725bdc24a8f2))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([29466f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/29466f2d9aa89935e20ef96184eae95b34329f84))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([94c6af4](https://git.griefed.de/Griefed/ServerPackCreator/commit/94c6af47d56f3606fdd142697ecd05527fa9adaf))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([d06b4cd](https://git.griefed.de/Griefed/ServerPackCreator/commit/d06b4cd7559dea9eefd686a189ceb22ece256320))
* **deps-dev:** bump eslint from 8.5.0 to 8.6.0 in /frontend ([9698f98](https://git.griefed.de/Griefed/ServerPackCreator/commit/9698f98650490b0126467cfadf0ee7320ccd180a))
* **deps-dev:** bump eslint from 8.6.0 to 8.7.0 in /frontend ([f80efe5](https://git.griefed.de/Griefed/ServerPackCreator/commit/f80efe5c4457fb35367814556774e8e363f25d92))
* **deps-dev:** bump eslint-plugin-vue from 8.2.0 to 8.3.0 in /frontend ([f9f3e48](https://git.griefed.de/Griefed/ServerPackCreator/commit/f9f3e48ca2a775f8161bc83bb2fc380d68bdfee2))
* **deps:** bump @quasar/extras from 1.12.1 to 1.12.2 in /frontend ([bf9f871](https://git.griefed.de/Griefed/ServerPackCreator/commit/bf9f871eb39c3a18e8f4c67bd44d5a1c4dfd68a5))
* **deps:** bump @quasar/extras from 1.12.2 to 1.12.3 in /frontend ([08590a7](https://git.griefed.de/Griefed/ServerPackCreator/commit/08590a7bc96ad03837081ecc8b4779c3a1696791))
* **deps:** bump @quasar/extras from 1.12.4 to 1.12.5 in /frontend ([465f083](https://git.griefed.de/Griefed/ServerPackCreator/commit/465f0833298c78aa51808e654243aa6d376d1741))
* **deps:** bump axios from 0.24.0 to 0.25.0 in /frontend ([c9b0734](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9b0734f51698a7349b6782bd7423b4ef9de7a92))
* **deps:** bump com.github.ben-manes.versions from 0.39.0 to 0.40.0 ([778e5e6](https://git.griefed.de/Griefed/ServerPackCreator/commit/778e5e6ff9a25c3af7853b771dda0b940cf3013b))
* **deps:** bump com.github.ben-manes.versions from 0.40.0 to 0.41.0 ([855c6e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/855c6e0a44232119c99ad028135083d817c98698))
* **deps:** bump com.github.ben-manes.versions from 0.41.0 to 0.42.0 ([6456e3f](https://git.griefed.de/Griefed/ServerPackCreator/commit/6456e3f211af4dda8f693c5f6222950b709032bb))
* **deps:** bump core-js from 3.19.1 to 3.19.3 in /frontend ([4864c13](https://git.griefed.de/Griefed/ServerPackCreator/commit/4864c13d9b2b7a7ffc979c54483803b54d445c44))
* **deps:** bump core-js from 3.20.0 to 3.20.1 in /frontend ([bbad029](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbad02947f1ad5462c46b418bb7d2d6c55bb3038))
* **deps:** bump core-js from 3.20.1 to 3.20.2 in /frontend ([f9c1068](https://git.griefed.de/Griefed/ServerPackCreator/commit/f9c10686b424e460fd1fefaa92e8230b637bb189))
* **deps:** bump core-js from 3.20.2 to 3.20.3 in /frontend ([2a4b86f](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a4b86f9f84cdc5c5b14479a7c016b0be8694309))
* **deps:** bump core-js from 3.20.3 to 3.21.0 in /frontend ([1862a3b](https://git.griefed.de/Griefed/ServerPackCreator/commit/1862a3b9f2eb08090bd62f30f677a3792f9cd8b5))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.0 to 2.0.2 ([003e1a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/003e1a1d404b0c835394b787acaa321063a7b891))
* **deps:** bump JamesIves/github-pages-deploy-action ([49cd567](https://git.griefed.de/Griefed/ServerPackCreator/commit/49cd567d7b9d0a68611b5771778a97e309bc80e8))
* **deps:** bump junit-platform-commons from 1.8.1 to 1.8.2 ([d8483f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/d8483f1d5767c0ec62d7bb12cfa4d4f476d3d62f))
* **deps:** bump log4j-api from 2.17.0 to 2.17.1 ([f243a62](https://git.griefed.de/Griefed/ServerPackCreator/commit/f243a626a7f8b956703807a83d12696a84a4b898))
* **deps:** bump log4j-core from 2.17.0 to 2.17.1 ([1e579d2](https://git.griefed.de/Griefed/ServerPackCreator/commit/1e579d2c9a4b75327cb42f44c7e9b549edae614e))
* **deps:** bump log4j-jul from 2.17.0 to 2.17.1 ([7c10e41](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c10e41c2085471c78849b08f1230089d170273b))
* **deps:** bump log4j-slf4j-impl from 2.17.0 to 2.17.1 ([303e2da](https://git.griefed.de/Griefed/ServerPackCreator/commit/303e2dad816660947384df1f10ea69fbba27b7f5))
* **deps:** bump log4j-web from 2.17.0 to 2.17.1 ([7a2ba8a](https://git.griefed.de/Griefed/ServerPackCreator/commit/7a2ba8ad49e1fe16d7733b8189fb5034a1cb0fe0))
* **deps:** bump org.springframework.boot from 2.6.2 to 2.6.3 ([8e02fa7](https://git.griefed.de/Griefed/ServerPackCreator/commit/8e02fa73374e600c55ac673f3a2502a6c8e1c4eb))
* **deps:** bump quasar from 2.3.3 to 2.3.4 in /frontend ([373fdb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/373fdb340ca949d61f51374f7e03685e18708f82))
* **deps:** bump quasar from 2.3.4 to 2.4.2 in /frontend ([bd3051c](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd3051c18690a09609b10ece95bf0500f73036c1))
* **deps:** bump quasar from 2.4.13 to 2.5.3 in /frontend ([1d2ca7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/1d2ca7e34726667131ccc87360c2b5eb5d96efa2))
* **deps:** bump quasar from 2.4.3 to 2.4.4 in /frontend ([904db5f](https://git.griefed.de/Griefed/ServerPackCreator/commit/904db5feb51353c8054b200c32a560106ac1e6ca))
* **deps:** bump quasar from 2.4.9 to 2.4.13 in /frontend ([ef5a18d](https://git.griefed.de/Griefed/ServerPackCreator/commit/ef5a18d2fb27deaac90a28020fc9ae24382ec5d5))
* **deps:** bump quasar from 2.5.3 to 2.5.5 in /frontend ([4e303bf](https://git.griefed.de/Griefed/ServerPackCreator/commit/4e303bf4b91cd86a820c5ec9765bc87015a3daf4))
* **deps:** bump spring-boot-devtools from 2.5.6 to 2.6.0 ([678e175](https://git.griefed.de/Griefed/ServerPackCreator/commit/678e1750ee6a29def7d52920b5699c0b7ed89322))
* **deps:** bump spring-boot-devtools from 2.6.0 to 2.6.1 ([a51e28e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a51e28e646c115cce8f784458e08a4d95197edb4))
* **deps:** bump spring-boot-devtools from 2.6.2 to 2.6.3 ([0fe0b42](https://git.griefed.de/Griefed/ServerPackCreator/commit/0fe0b42715808954bb722f22e222a6970ed8436e))
* **deps:** bump spring-boot-starter-artemis from 2.6.2 to 2.6.3 ([9038c21](https://git.griefed.de/Griefed/ServerPackCreator/commit/9038c21f85a327fc2355254d6ead68490a55aaa1))
* **deps:** bump spring-boot-starter-data-jpa from 2.5.6 to 2.6.0 ([dc8797a](https://git.griefed.de/Griefed/ServerPackCreator/commit/dc8797af78b505599e5f8fa7916c93030324fc52))
* **deps:** bump spring-boot-starter-data-jpa from 2.6.2 to 2.6.3 ([093ab09](https://git.griefed.de/Griefed/ServerPackCreator/commit/093ab091f40aca24e1501c47aa360735240f61fb))
* **deps:** bump spring-boot-starter-log4j2 from 2.5.6 to 2.6.0 ([5b67e52](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b67e52fd5c7783d8a08cd892ed6ef285d336836))
* **deps:** bump spring-boot-starter-log4j2 from 2.6.2 to 2.6.3 ([a4091bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/a4091bd83e8936c97bafeceba508df6692a6421b))
* **deps:** bump spring-boot-starter-quartz from 2.5.6 to 2.6.0 ([0433e90](https://git.griefed.de/Griefed/ServerPackCreator/commit/0433e905151ef0a60a2f8a00f5cd5587c4bf024c))
* **deps:** bump spring-boot-starter-quartz from 2.6.2 to 2.6.3 ([6dd76e3](https://git.griefed.de/Griefed/ServerPackCreator/commit/6dd76e3e3a635e6af613bfc3d437233518bdc9d8))
* **deps:** bump spring-boot-starter-test from 2.5.6 to 2.6.1 ([0f39852](https://git.griefed.de/Griefed/ServerPackCreator/commit/0f398524acfbb7c01b9a404430ee35eba351ee84))
* **deps:** bump spring-boot-starter-test from 2.6.2 to 2.6.3 ([577b79a](https://git.griefed.de/Griefed/ServerPackCreator/commit/577b79a0a5dfe0fb082bf820ab84846a7645bb19))
* **deps:** bump spring-boot-starter-validation from 2.5.6 to 2.6.1 ([1473032](https://git.griefed.de/Griefed/ServerPackCreator/commit/14730327dae5a9d81df7caf3ce0e4d1a5f4fda88))
* **deps:** bump spring-boot-starter-validation from 2.6.2 to 2.6.3 ([516db6a](https://git.griefed.de/Griefed/ServerPackCreator/commit/516db6ad4207aa079aa350f2b5d8c10323e4a67b))
* **deps:** bump spring-boot-starter-web from 2.5.6 to 2.6.1 ([9d7ab8b](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d7ab8b0f024d1cc0f6f88ea5aa68ecbffbb545f))
* **deps:** bump spring-boot-starter-web from 2.6.2 to 2.6.3 ([7c49fd3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c49fd341694ae81cccf77ef9abadc33b15fb22a))
* **deps:** bump tsparticles from 1.37.5 to 1.37.6 in /frontend ([7ab7a69](https://git.griefed.de/Griefed/ServerPackCreator/commit/7ab7a69446f71d8dd827a3b152cc54987946a88d))
* **deps:** bump tsparticles from 1.37.6 to 1.38.0 in /frontend ([d17900b](https://git.griefed.de/Griefed/ServerPackCreator/commit/d17900b81fb766bf6984c844e3ca3bd609194767))
* **deps:** bump tsparticles from 1.38.0 to 1.39.0 in /frontend ([75a3b00](https://git.griefed.de/Griefed/ServerPackCreator/commit/75a3b00e5119863a21f57207fbb1609a261ab2ee))
* **deps:** bump tsparticles from 1.39.1 to 1.41.0 in /frontend ([fa0cabc](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa0cabc745932f327ebd46cac4f76994797b941e))
* **deps:** bump versionchecker from 1.0.4 to 1.0.5 ([57f0dd0](https://git.griefed.de/Griefed/ServerPackCreator/commit/57f0dd0f03a9ca48fe7fde7bec144c0e7136a3a8))
* **deps:** bump vue from 3.2.22 to 3.2.24 in /frontend ([62d687a](https://git.griefed.de/Griefed/ServerPackCreator/commit/62d687a0ffccc248c4ae0f89168ce18e3e47fabf))
* **deps:** bump vue from 3.2.26 to 3.2.29 in /frontend ([09dd657](https://git.griefed.de/Griefed/ServerPackCreator/commit/09dd6572ef82eef592a2ee746b826459311d6fdb))
* **deps:** bump vue from 3.2.29 to 3.2.30 in /frontend ([da542e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/da542e0f7bce7e7f30d039c206e6548d66e8f16f))
* **deps:** Update commons-io to 2.11.0 ([b8a673a](https://git.griefed.de/Griefed/ServerPackCreator/commit/b8a673a8b744eb7653a2bbd359c0caadeac7ea72))
* **fabric:** Update default Fabric Installer version if it can not be acquired from external ([b6b0bc3](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6b0bc31f1b6c3f5065e6c65b7fb4c292e8aced6))
* **fabric:** Update default Fabric Loader version if it can not be acquired from external ([aa2f9e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa2f9e16ee05e60374a6f6b33368a3fc9f928feb))
* **webservice:** Add artemis dependency for queueing system. Update dependencies. Exclude redundant slf4j. ([0954a56](https://git.griefed.de/Griefed/ServerPackCreator/commit/0954a56cf7ef8b1b8d26152a0b45aff86e3767cf))
* **webservice:** Do not run tests in Docker build. We have the Gradle Test stage for that. ([54b98fc](https://git.griefed.de/Griefed/ServerPackCreator/commit/54b98fc7eb143fd402a355118eeddef60ff03742))
* **webservice:** Ensure task are executed in correct order ([afb2f73](https://git.griefed.de/Griefed/ServerPackCreator/commit/afb2f73d0d27e4aaeaddbb4849e60a1b0a6f2b7d))
* Add Breaking section to changelog ([7165659](https://git.griefed.de/Griefed/ServerPackCreator/commit/7165659d8ccb507be63047c3b0f37d2fca2ac859))
* Add changes from main for GitHub workflows, delete no longer needed workflows. ([03ad356](https://git.griefed.de/Griefed/ServerPackCreator/commit/03ad356f762bd66d7cc887d537542fc06187cb2b))
* Add changes to github ci ([128ea30](https://git.griefed.de/Griefed/ServerPackCreator/commit/128ea30bbcd1011edb9a2fda85bfe1153863f787))
* Add improv for Improvements to list of commits which generate a release ([70d4b49](https://git.griefed.de/Griefed/ServerPackCreator/commit/70d4b4993726b5e3e464db4ea1bc6cc2a43d1dbb))
* Add readme-template and sponsors ci job ([5622dca](https://git.griefed.de/Griefed/ServerPackCreator/commit/5622dcaa0a32ecc40761056df461adc95ce08cce))
* Allow failure of dependency check and coverage jobs ([f8bb3d1](https://git.griefed.de/Griefed/ServerPackCreator/commit/f8bb3d1e82989d5639152d204c18aae642f6ff19))
* Allow running of Gradle and Docker test in parallel, to speed up pipeline completion. Move variables and services into global variable ([187a966](https://git.griefed.de/Griefed/ServerPackCreator/commit/187a9668d91fcc2ed8b809c86e6c8edc54db6f97))
* Bring in changes to CI from main ([b89125b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b89125ba34c873328f9e600f0bafd02586de1ad4))
* Build releases for alpha and beta branches ([8643327](https://git.griefed.de/Griefed/ServerPackCreator/commit/864332713be0adb15e8cebba0d679cdcebb755af))
* Build with --full-stacktrace ([cde8d08](https://git.griefed.de/Griefed/ServerPackCreator/commit/cde8d0845005f906f07f6878900ee7ab6ce99c98))
* Change branch separator in dependabot config ([3b08ff8](https://git.griefed.de/Griefed/ServerPackCreator/commit/3b08ff8e9169990d4c502a5cc1ecd86c3ca96a8d))
* Clean up and beautify ([d2ff50f](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2ff50fffc4571875724131a7b5d9cd4fbdf4521))
* Cleanup GitLab CI and Dockerfile. Remove spotbug. ([017ebed](https://git.griefed.de/Griefed/ServerPackCreator/commit/017ebed289b10b88e473ef18651c01cc7acee13b))
* Correctly write VERSION.txt ([6434be8](https://git.griefed.de/Griefed/ServerPackCreator/commit/6434be836fa19f5df05eb38980dbaf57938e4866))
* Create jacoco coverage report for coverage visualization in GitLab ([5da842f](https://git.griefed.de/Griefed/ServerPackCreator/commit/5da842f5415fbc16e43d51dd6195a4bd53ad22f3))
* Create pre-releases for alpha and beta branches ([e6729ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/e6729ea0a9f800def1c6de68c0ece7b4647ff111))
* Deactivate push on docker tests. Remove unnecessary file renaming in build release. Properly run publish job. ([481a048](https://git.griefed.de/Griefed/ServerPackCreator/commit/481a0488e27333ae3c7964a1fa67b8234e3ac6ac))
* Disable Docker pipelines for the time being. Docker is acting up and building Docker images of the webservice-branch is not necessary as I have yet to start actual work on the webservice itself. ([f45e25f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f45e25f681102dd991ff179a59df7c9fb85af227))
* Ensure docker jobs only run on git.griefed.de ([e633a0b](https://git.griefed.de/Griefed/ServerPackCreator/commit/e633a0b59b1d937ef7752333434cd1733c05b105))
* Exclude libraries folder from test workflow artifacts ([c796115](https://git.griefed.de/Griefed/ServerPackCreator/commit/c7961153fdb212f68360e06b4a9d04a50222b518))
* Fix artifact names for renaming ([d4f4f35](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4f4f352150a874f270e4468bcf102df10b72c68))
* Fix branch acquisition for GitHub Docker test ([063215f](https://git.griefed.de/Griefed/ServerPackCreator/commit/063215f65b7dbe9cd55b10ccac65de59b67c5cf4))
* Fix release build... ([fe2f601](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe2f6014802607e822ac0fde7facfb79a32233af))
* Further restrict jobs to specific branches. Sort jobs according to purpose ([444eede](https://git.griefed.de/Griefed/ServerPackCreator/commit/444eedec770570aab80f2183a86b147cb0a6688e))
* God damn, would you please only run when I tell you to? ([c610692](https://git.griefed.de/Griefed/ServerPackCreator/commit/c6106922a1c04fa3cee17880dfd8b931e5b9f951))
* Hopefully fix main release workflow trying to run on alpha/beta release ([9e6122e](https://git.griefed.de/Griefed/ServerPackCreator/commit/9e6122e7a5523d3b35850721062fe385f8c5d207))
* Merge Release and PreRelease jobs and only run on git.griefed.de ([f3115c9](https://git.griefed.de/Griefed/ServerPackCreator/commit/f3115c9c5757cb3f74ec2b15b3683ab226abe623))
* Mirror release on GitLab.com after tag mirror ([d08845d](https://git.griefed.de/Griefed/ServerPackCreator/commit/d08845dc1676e165ceb724d9ea775c37e1f3211f))
* Only run docker related jobs on git.griefed.de ([17339f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/17339f4d402b88ac6b358f0de6b2557d6df03122))
* Only run Gradle Test and Docker Test on main & master ([236c661](https://git.griefed.de/Griefed/ServerPackCreator/commit/236c661f6fa60a84f0290a295967186261ebce81))
* Only run tag and release generation on git.griefed.de ([8afea27](https://git.griefed.de/Griefed/ServerPackCreator/commit/8afea27163a985596c4d37102b6e7d366e640ba0))
* Post webhook message to Discord on new release ([2e3e25d](https://git.griefed.de/Griefed/ServerPackCreator/commit/2e3e25dde5ce19d8be2a2e641e9829ce1733c528))
* Prevent Generate Release job from running unnecessarily ([5be9fcd](https://git.griefed.de/Griefed/ServerPackCreator/commit/5be9fcdf2624991b9eaf845aafc3bdef8d34f04d))
* Publish maven artifact on (pre)release. Add info about new additional mirror on Gitea.com ([cfde3e2](https://git.griefed.de/Griefed/ServerPackCreator/commit/cfde3e29075254aa8e214349f29149b128e93b9d))
* Re-enable arch dependant nodedisturl ([f840e31](https://git.griefed.de/Griefed/ServerPackCreator/commit/f840e31a0e2fb95457a91d2e087ee66c756973d8))
* Reactivate docker jobs ([4b520c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b520c2f39e28633b25788300cf88e2a1c531d5f))
* Remove changelog generation of GitHub releases as I copy and paste the changelog from GitLab anyway ([25cdb26](https://git.griefed.de/Griefed/ServerPackCreator/commit/25cdb26d97fd5427e152615a9d10749d6039765f))
* Remove unnecessary environment cleaning ([67e1029](https://git.griefed.de/Griefed/ServerPackCreator/commit/67e1029e1cb12632d9cbe70c37466be84385721d))
* Remove unnecessary login to docker registry ([e5b034f](https://git.griefed.de/Griefed/ServerPackCreator/commit/e5b034f331e3b1d238da8e25254cf105d304e484))
* Remove unnecessary logins from test job ([dac135c](https://git.griefed.de/Griefed/ServerPackCreator/commit/dac135cc4e079a996e8ca45ae95d019345ea2283))
* Revert changes to Docker release jobs. Prevent running if on gitlab.com ([7e6404e](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e6404e9fc912a01674f4576a665115b67aa98e4))
* Run correct Gradle tasks on tag mirror from GitLab to GItHub ([db6dcd0](https://git.griefed.de/Griefed/ServerPackCreator/commit/db6dcd0b245b2603b7aafea0c59cba114016a291))
* Run dockerjobs differently when running on other GitLab instances. ([169733f](https://git.griefed.de/Griefed/ServerPackCreator/commit/169733f1b9aa7c6295b4074f0468dc51caa342be))
* Run dockerjobs differently when running on other GitLab instances. ([0385ba1](https://git.griefed.de/Griefed/ServerPackCreator/commit/0385ba139b783bf348dbd14b0f69bda587a0cb77))
* Run dockerjobs differently when running on other GitLab instances. ([bfcbd51](https://git.griefed.de/Griefed/ServerPackCreator/commit/bfcbd51ebd6f30331d82fbc53284a357f9d751aa))
* Run GitHubs dependabot on dependabot-branch and run tests on GitHubs infrastructure. The more the merrier ([659f0f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/659f0f4bd721befa0b3a57f4699a437390c7fbbb))
* Set loglevel in SAST to debug ([fc5341f](https://git.griefed.de/Griefed/ServerPackCreator/commit/fc5341fea92bba0e2f650644e543c53a1d8c48c4))
* Split tests in GitHub workflow into separate jobs ([58fd4b3](https://git.griefed.de/Griefed/ServerPackCreator/commit/58fd4b3758aea9fc029bf70929fef9f5d2f9cddd))
* Tag dev-images with short_sha as well. Remove some artifacts ([f3f9913](https://git.griefed.de/Griefed/ServerPackCreator/commit/f3f9913797cc55458eef5eca7554c4de877f1adf))
* Try and fix Renovate warning ([893a581](https://git.griefed.de/Griefed/ServerPackCreator/commit/893a581c9d6a2935cdd80aa9df03f1717b3a425c))
* Update siouan/frontend-gradle-plugin to 5.3.0 and remove arch dependant configuration of nodeDistributionUrlPathPattern. See https://github.com/siouan/frontend-gradle-plugin/issues/165 ([1177d05](https://git.griefed.de/Griefed/ServerPackCreator/commit/1177d056934bc2b8521f214b326c16d5e069fb7a))
* Upload artifacts of GitHub actions ([b4e41e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/b4e41e458435b591a3fee54f7d38fbe2bb66feb4))
* You have a problem, so you use regex. Now you have two problems. ([b05c007](https://git.griefed.de/Griefed/ServerPackCreator/commit/b05c0075a810f89ba79ff3a9f32939e0abbe0ca8))
* **docs:** No need to run tests ([728af78](https://git.griefed.de/Griefed/ServerPackCreator/commit/728af78dc4cb6c1f93b730e7367fcefe85483365))
* **webservice:** Add temporary job for testing webservice and fix gitignore ([350582e](https://git.griefed.de/Griefed/ServerPackCreator/commit/350582e3a829d285607a2a21d10889350cab4ee8))
* **webservice:** Ensure quasar is installed before assembling frontend ([0f414ca](https://git.griefed.de/Griefed/ServerPackCreator/commit/0f414ca06487647b964bfd3e2fa3daa4244b1ecc))
* **webservice:** Fix URL for node distribution on arm ([f24663f](https://git.griefed.de/Griefed/ServerPackCreator/commit/f24663f1c72a88444a0cb1cfd264605f59fbb5aa))
* **webservice:** Make sure arm-builds in Docker work with the frontend plugin ([2c3793c](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c3793c0b2fa838504219f4c662723db9a928df8))
* **webservice:** Make sure no cache interferes with Docker build. Install library in hopes of fixing a failure in the pipeline. ([5841007](https://git.griefed.de/Griefed/ServerPackCreator/commit/58410078abdaf7ee2bf878edac14143d73f4866b))
* **webservice:** Scan dep updates for frontend, too (I hope this works lol) ([2994d25](https://git.griefed.de/Griefed/ServerPackCreator/commit/2994d257075deeda7817fad5990d02c2d5e7f867))


### 🧨 Breaking changes!

* Allow users to specify JVM flags/args for start-scripts via Menu->Edit->Edit Start-Scripts Java Args. Start scripts are no longer copied from server-files. New config option javaArgs automatically migrated into configs. ([929bfa6](https://git.griefed.de/Griefed/ServerPackCreator/commit/929bfa680704846e72952989f9f6f4f71e081ac7))


### 🧪 Tests

* Adapt tests ([e20f89d](https://git.griefed.de/Griefed/ServerPackCreator/commit/e20f89d34ecbcc85edea44264715ac90c47bc7af))
* Add more unit tests. ([ae06aa6](https://git.griefed.de/Griefed/ServerPackCreator/commit/ae06aa64a2463f31305efb072e7c5c49b42e5575))
* Add unit tests for UpdateChecker ([1b4b91a](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b4b91ac48c33b26aa77863eaed993171c99a372))
* Autowire jmsTemplate ([1ba6968](https://git.griefed.de/Griefed/ServerPackCreator/commit/1ba6968cb942ede7a211f58cb2aae930ad97fa66))
* Disable CurseForge related tests ([b28c97c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b28c97c9ccd3602fa266def9df1ff010cae4e68b))
* Don't delete default files after testing for them. ([b34602c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b34602c1a0ba30481c25fbb580c17d3331513ddc))
* Don't mention what is tested. Method names already tell us that. ([e32fd53](https://git.griefed.de/Griefed/ServerPackCreator/commit/e32fd534ec2498e8326d52da83759dd5d5e7bdac))
* Ensure serverpackcreator.properties is always available to prevent NPEs ([f674e13](https://git.griefed.de/Griefed/ServerPackCreator/commit/f674e137d44c3dfa3832d16c870aa865b1f6e6d6))
* Fix a test regarding AddonHandler ([b737d92](https://git.griefed.de/Griefed/ServerPackCreator/commit/b737d92db767f961151cd22ca2c0227d0020fa5a))
* Fix some paths and configs so tests don't fail because of Layer 8 ([8270c82](https://git.griefed.de/Griefed/ServerPackCreator/commit/8270c82a6cb32ed7415b680e7f38bd81462bf2c7))
* Fix some tests ([5ba12ad](https://git.griefed.de/Griefed/ServerPackCreator/commit/5ba12adf856ea9a0341393e56665c0c7f873649b))
* Fix test failing due to missing, recently added, clientside-only mod ([1eaa966](https://git.griefed.de/Griefed/ServerPackCreator/commit/1eaa966468cc74f0ed2aab63cdc3dc006df082e0))
* Hopefully fix ArtemisConfigTest ([7573d99](https://git.griefed.de/Griefed/ServerPackCreator/commit/7573d99bbc009eeb987d1743dae6e55896ea7545))
* Print stacktrace in all gradle builds to allow for better debugging ([7b6e480](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b6e480c5e50f49843fadfdb6efcfbbdfeb8cc69))
* Remove addon execution from tests, as parallel running tests caused problems because the addon can only be accessed by one thread at a time. ([b963b10](https://git.griefed.de/Griefed/ServerPackCreator/commit/b963b1094e3a470213fc737f9effa305960ad31f))
* Set ddl-auto to create ([8e00f7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/8e00f7e4990ad42ceb2e7a23bbdcaf075e26a261))
* Some cleanups. Nothing interesting ([12bc506](https://git.griefed.de/Griefed/ServerPackCreator/commit/12bc50602b411589b65f5e70e2024fbc0bff53f1))
* Split test methods. Helps pin-pointing cause of error in case of failure. ([f2d723b](https://git.griefed.de/Griefed/ServerPackCreator/commit/f2d723b2e3ebf24e9bdb86c83c35a791efa082c8))
* Try and fix ArtemisConfigTest and SpringBootTests for spotbugs job ([67817a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/67817a1e1b24742f9cac1930f44a8908272330e2))
* Try and fix ArtemisConfigTest and SpringBootTests for spotbugs job ([29c870f](https://git.griefed.de/Griefed/ServerPackCreator/commit/29c870fec68e75df7da3d8dba978a6f6688642b2))
* Try and fix ArtemisConfigTest for spotbugs job ([c665bf5](https://git.griefed.de/Griefed/ServerPackCreator/commit/c665bf5fd23d4fe56c249c3d4b3f1a22ebd5c3b5))
* Try and fix error because of missing database ([81d4f80](https://git.griefed.de/Griefed/ServerPackCreator/commit/81d4f8045ed06bd83525edbb4980dde8afa2881e))
* Ye olde I RUN FINE ON YOUR MACHINE BUT NOT ON ANOTHER NU-UUUUHHUUUU.....Sigh ([4442168](https://git.griefed.de/Griefed/ServerPackCreator/commit/444216872f3df37e7e7cb9681d3752d91eb82d17))


### 🚀 Features

* Add methods to reverse the order of a String List or String Array. Allows setting of lists in GUI with newest to oldest versions. ([11d565e](https://git.griefed.de/Griefed/ServerPackCreator/commit/11d565ef61ed9ea2d324b82b4cb49ec529ffe624))
* Add tab for addons log tail. ([b84cc5b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b84cc5b12c9cd33176830d8eb413a1005a0d87a2))
* Add tooltip to SPC log panel informing users about the upload buttons in the menu bar ([08a123d](https://git.griefed.de/Griefed/ServerPackCreator/commit/08a123daae1687d8e7f929ae078b91c444aa7c9b))
* Addon functionality! This allows users to install addons to execute additional operations after a server pack was generated. See 5. in the README and the example addon at https://github.com/Griefed/ServerPackCreatorExampleAddon ([2a93e54](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a93e5476d11e84215667460997b694d30e93770))
* Allow check of configuration from an instance of ConfigurationModel, without any file involved. ([17529fa](https://git.griefed.de/Griefed/ServerPackCreator/commit/17529fa958fbb386dfe7bdc91eaec2f9ceff39f5))
* Allow generation of a server pack by uploading it to the webservice. ([c92ddd2](https://git.griefed.de/Griefed/ServerPackCreator/commit/c92ddd2d01ec7851fed4696608a71b6c9efeea08))
* Allow generation of a server pack from an instance of ConfigurationModel ([5b54a1c](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b54a1ca9b3be3cc7d72e3c1851a636ee81a482e))
* Allow specifying custom server-icon.png and server.properties. The image will be scaled to 64x64. Implements GH[#88](https://git.griefed.de/Griefed/ServerPackCreator/issues/88) and GH[#89](https://git.griefed.de/Griefed/ServerPackCreator/issues/89). ([e3670e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3670e4ffc15505856ae9695f59f3c614e0199dd))
* Allow specifying files to add to server pack with simple `foo.bar` connotations. Closes issue [#86](https://git.griefed.de/Griefed/ServerPackCreator/issues/86) ([8a53aa6](https://git.griefed.de/Griefed/ServerPackCreator/commit/8a53aa6b9dbf148d60f4001a47e64055e8975d10))
* Allow users to disable cleanups of server packs and downloaded CurseForge modpacks. Can save bandwidth, time and disk operations, if the user is interested in that. ([3155af4](https://git.griefed.de/Griefed/ServerPackCreator/commit/3155af499006eba64751cca01e53e45480e8e936))
* Allow users to disabled server pack overwriting. If de.griefed.serverpackcreator.serverpack.overwrite.enabled=false AND the server pack for the specified modpack ALREADY EXISTS, then a new server pack will NOT be generated. Saves a LOT of time! ([00dd7aa](https://git.griefed.de/Griefed/ServerPackCreator/commit/00dd7aa15b8cdbdce91f6d510fc2505f2f6e9d1a))
* Allow users to edit language-definitions in the lang-directory. ([e2b5cca](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2b5ccaef8834ab3a9154d7208a5e6ff90a2b14b))
* Allow users to exclude files and directories from the server pack to be generated with ! as the prefix in an entry in copyDirs ([f527d04](https://git.griefed.de/Griefed/ServerPackCreator/commit/f527d04dc67d5c2c186a460068aa84167278cafd))
* Allow users to set a suffix for the server pack to be generated. Requested in issue [#77](https://git.griefed.de/Griefed/ServerPackCreator/issues/77) ([2d32119](https://git.griefed.de/Griefed/ServerPackCreator/commit/2d321197c6123348558476b20b6f2c9aa93cc54f))
* Allow users to specify a custom directory in which server-packs will be generated and stored in. ([4a36e76](https://git.griefed.de/Griefed/ServerPackCreator/commit/4a36e76bfab5a66ce52c51e57bb16af79dddb752))
* Automatically detect clientside-only mods for Minecraft modpacks version 1.12 and older. ([e17322e](https://git.griefed.de/Griefed/ServerPackCreator/commit/e17322ed5db6bd18b4573be4a3562295317dd137))
* Automatically detect clientside-only mods for Minecraft modpacks version 1.13+. ([3811190](https://git.griefed.de/Griefed/ServerPackCreator/commit/3811190cb401c8993d84f0026618ad6e4958ed27))
* Basic filewatcher to monitor a couple of important files. Example: Delete serverpackcreator.properties to reload defaults ([d3f194a](https://git.griefed.de/Griefed/ServerPackCreator/commit/d3f194abb2ef55e168c094290263d4e78162cc91))
* Check and notify on updates in logs, console and in GUI. Also replaced and update a couple of i18n keys. VersionChecker can be found at https://git.griefed.de/Griefed/VersionChecker ([64419a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/64419a203a0d26bb001f20de2f8ab0a732156f20))
* Check setting for Javapath upon selecting "Install modloader-server?". If it is empty, the user is asked whether they would like to select their Java executable now. If not, the user is warned about the danger of not setting the Javapath ([5d474f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/5d474f1cf2763c010b6c02f969e2843de96d339f))
* Configurable schedules in webservice which clean up the database and filesystem of unwanted server packs and files. ([09ccbc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/09ccbc14921946a022634c454a013f0adb1cac63))
* Create eula.txt upon server pack generation. Closes issue [#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83) ([d48191c](https://git.griefed.de/Griefed/ServerPackCreator/commit/d48191cda634f8bb8cc4db2298a0848b8b14c2cc))
* Create server packs from zipped modpacks. Point modpackDir at a ZIP-file which contains a modpack in the ZIP-archives root. ([fbdae16](https://git.griefed.de/Griefed/ServerPackCreator/commit/fbdae16759e90cfd86786ee43ccf7a448fae0cce))
* Display version in window title and print to logs ([201a64c](https://git.griefed.de/Griefed/ServerPackCreator/commit/201a64c32868b0d26800b50c55d1e39dd5daa464))
* Enable/disable clientside-only mods autodiscovery via property de.griefed.serverpackcreator.serverpack.autodiscoverenabled=true / false. Closes [#62](https://git.griefed.de/Griefed/ServerPackCreator/issues/62). ([094a217](https://git.griefed.de/Griefed/ServerPackCreator/commit/094a217e83f2f27ba1e3746088b459a542411254))
* If given languagekey can not be found, use en_us from resources as fallback ([5802636](https://git.griefed.de/Griefed/ServerPackCreator/commit/5802636a612c4a49878f68b827e1115895062a95))
* If i18n localized string can not be found in local file, try JAR-resource. If locale is not en_us, get en_us localized string as fallback. Allow users to write their own locales, languages and translations. ([802eb0c](https://git.griefed.de/Griefed/ServerPackCreator/commit/802eb0c5a4aa06b90d71bb570864bcda613bc55c))
* Implement voting-system for server packs. Improve styling of download table. ([e49fa96](https://git.griefed.de/Griefed/ServerPackCreator/commit/e49fa96e4d2268441d67b8cd253c67e92dc33128))
* in start scripts: Ask user whether they agree to Mojang's EULA, and create `eula=true` in `eula.txt` if they specify I agree. Closes GH[#83](https://git.griefed.de/Griefed/ServerPackCreator/issues/83) ([5995f51](https://git.griefed.de/Griefed/ServerPackCreator/commit/5995f512d2731ebbd161c0ff8e34e37a437da0ac))
* New theme and cleaned up GUI. MenuBar for various things (wip). Lists for version selection. Switch between darkmode and lightmode and remember last mode used. More things, check commit. ([949fb6a](https://git.griefed.de/Griefed/ServerPackCreator/commit/949fb6aecd47518e0b91ca3a8be0516a9f2cb540))
* Pass the path where ServerPackCreator resides in to addons. Create dedicated addon-directory in work/temp, avoiding potential conflict with other addons. ([c9050b6](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9050b68ee42b4dabcde73cfb8eaf1417ab0a312))
* Provide HashMap of Key-Value pairs in MinecraftVersion-ForgeVersions format. Use a given Minecraft version as key and receive a string array for available Forge versions for said MInecraft versions. ([0a0d3b5](https://git.griefed.de/Griefed/ServerPackCreator/commit/0a0d3b50c7d7e955c41ce148bb82d4fc9abe6ac1))
* Read Minecraft, Forge and Fabric versions from their manifests into lists which can then be used in GUIs. ([c9ce1ff](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9ce1ff41f12b6eeef9dc00827d3e6a129ee8a5f))
* Replace crude self-made addon system with Pf4j and provide first entry points ([e591488](https://git.griefed.de/Griefed/ServerPackCreator/commit/e59148806a0d3550cc3a9b2b3e4318e186b71029))
* replace fabric-server-launch.jar with improved Fabric Server Launcher, if it is available for the specified Minecraft and Fabric versions.Thanks to @TheButterbrotMan for the idea! ([befdaf7](https://git.griefed.de/Griefed/ServerPackCreator/commit/befdaf7ea4265af9b3a0398f58a43bab3f19525f))
* Select Minecraft and modloader versions from lists instead of entering text into a textfield. ([5b56f18](https://git.griefed.de/Griefed/ServerPackCreator/commit/5b56f18a90e7d3f1bfda98d5ae509a9cda29e959))
* Set copyDirs to "lazy_mode" to lazily create a server pack from the whole modpack. This will copy everything, no exceptions. Thanks to [@kreezxil](https://git.griefed.de/kreezxil) for the idea. ([2d89bec](https://git.griefed.de/Griefed/ServerPackCreator/commit/2d89bec8de7574bc14e213ce2e575558f12c9537))
* Store server pack suffix in serverpackcreator.conf.l Closes [#77](https://git.griefed.de/Griefed/ServerPackCreator/issues/77) again. ([d6c74e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/d6c74e0f62f395ea171266daca6194e39f0f634a))
* Write errors encountered during config check to logs/console. When using GUI, show a message with the encountered Errors. Helps with figuring out whats wrong with a given configuration. ([e1b0c62](https://git.griefed.de/Griefed/ServerPackCreator/commit/e1b0c6269cbd545993854786a07a949f4a379c45))
* **gui:** Add button in menubar to clear GUI. Allows starting with a fresh config without having to restart ServerPackCreator. Implements GH[#91](https://git.griefed.de/Griefed/ServerPackCreator/issues/91) ([dddee02](https://git.griefed.de/Griefed/ServerPackCreator/commit/dddee0286ca110bb25c75ff5d66756e86130b356))
* **gui:** Open server-icon.png in users default picture-viewer. From there on, users can open their favourite editing software. ([d960dd2](https://git.griefed.de/Griefed/ServerPackCreator/commit/d960dd28f7e796b8d7f84dfbcfe55273e60cfec8))
* **gui:** Open server.properties in users default text editor via Edit->Open server.properties in Editor ([1bf7533](https://git.griefed.de/Griefed/ServerPackCreator/commit/1bf75338e60b4fe0ff85eca6a55308eb4538fe7f))
* **gui:** Redesign help window. Users can choose what they need help with from a list, which then displays the help-text for the chosen item. ([7c490a3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c490a3d2a205181c61148ad4ff9b8872ff5961b))
* **gui:** Save the last loaded configuration alongside the default serverpackcreator.conf, unless a new configuration was started. Can be activated/deactivated with `de.griefed.serverpackcreator.configuration.saveloadedconfig=true` or `false` respectively ([e03b808](https://git.griefed.de/Griefed/ServerPackCreator/commit/e03b8089dca9ca40aa8d2a07948603888fbefd70))
* **gui:** Set LAF for Java Args correctly. If javaArgs is "empty", display textField as "" to not confuse users. ([462e7a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/462e7a1cef59715b08ff5f20ac03ae760a45132c))
* **gui:** Various changes. Too many to list. MenuBar entries, Theme changes. MenuItem funcitionality etc. etc. ([28c088c](https://git.griefed.de/Griefed/ServerPackCreator/commit/28c088cc5395a432ac6cbd83f2b31643922bf858))
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

* -Dlog4j2.formatMsgNoLookups=true to prevent log4j2 vulnerability, added via customziable OTHERARGS in scripts. Move java path to JAVA for customizability (is that a word?) ([ff7dc52](https://git.griefed.de/Griefed/ServerPackCreator/commit/ff7dc52f23ed5e1e2abc92f33c9964225c083dcb))
* Allow selection of bmp, jpg and jpeg as server-icons. Java correctly converts them to png for use as server-icons. If the image could not be loaded, print an error message. ([d2c1ac7](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2c1ac78fbf97c003e10f49af281437b95891865))
* Allow translating for full GUI as well as missing parts in backend. ([366cb10](https://git.griefed.de/Griefed/ServerPackCreator/commit/366cb106fddbebb1411105d466017c2f36e19a63))
* Always load classpath serverpackcreator.properties first, then loac local filesystem serverpackcreator.properties. Ensures defaults are always present and available to be overwritten and never empty. ([f91c8da](https://git.griefed.de/Griefed/ServerPackCreator/commit/f91c8da02116c5271eda0d02b4a394d2ed267ae2))
* Catch occasional error from CurseForge's API which could lead to dead entries in the database ([625a8a8](https://git.griefed.de/Griefed/ServerPackCreator/commit/625a8a83647a3fd875b80c629159c2874f667f63))
* Clear text every 1000 lines. Help with issue [#76](https://git.griefed.de/Griefed/ServerPackCreator/issues/76). ([132a3dd](https://git.griefed.de/Griefed/ServerPackCreator/commit/132a3ddd903f8693e08d9252c1f3e9c6004aad3f))
* Copy lang-files if running as .exe ([c7c1415](https://git.griefed.de/Griefed/ServerPackCreator/commit/c7c1415ecdc4e30e9743f378e70e25b3b7545977))
* Correctlry get property which decides whether autodiscovery of clientside-only mods should be enabled ([3c5deff](https://git.griefed.de/Griefed/ServerPackCreator/commit/3c5deff79acf70d5d6ea6d578cc4e73faf85d4d3))
* Correctly compare user input with variable in start.bat, resulting in creation of eula.txt if user enters "I agree" ([224cbb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/224cbb3874830c7ff2cce83e403eb27470244aa8))
* Correctly initialize variable in start-scripts. Correctly pass OTHERARGS in batch-scripts. ([26f6dfd](https://git.griefed.de/Griefed/ServerPackCreator/commit/26f6dfdd24fb24c27755699edaa3b79bf89ae698))
* Create additional pattern for log files as ANSI colouring frakked up the formatting for log entries in files. ([f246bf8](https://git.griefed.de/Griefed/ServerPackCreator/commit/f246bf8777d72832041c16f3f1f4fe21305ef870))
* Deactivate CurseForge related code until custom implementation of CurseForgeAPI with CurseForge-provided API token is implemented and provided ([8c9bbff](https://git.griefed.de/Griefed/ServerPackCreator/commit/8c9bbff55d50a660ed0b673152a2b61c84845aae))
* Empty commit so a new alpha will be generated... ([c1b5698](https://git.griefed.de/Griefed/ServerPackCreator/commit/c1b5698a0aac863518244998c72a4f21ff4d604c))
* Ensure no empty entries make it into copyDirs or clientMods lists. Prevents accidental copying of the whole modpack into the server pack. Thanks to @Kreezxil for this improvement! ([5549930](https://git.griefed.de/Griefed/ServerPackCreator/commit/5549930966408fd219ab9f8a8e2dbaeaefcf3d57))
* Expanded fallback clientside modslist with 3dSkinLayers-,armorchroma-,Craftpresence-,medievalmusic-,MyServerIsCompatible- thanks to @TheButterbrotMan ([a2ac391](https://git.griefed.de/Griefed/ServerPackCreator/commit/a2ac391d7ca4664b8320be07671b669721dfa4b3))
* Expanded fallback modslist with yisthereautojump, ToastControl, torchoptimizer ([f1c4ba3](https://git.griefed.de/Griefed/ServerPackCreator/commit/f1c4ba31f0a6253064c990ccf9dd05dd77f47d55))
* Explicitly define log4j and force any dependency using it to use the secure version ([f0c1946](https://git.griefed.de/Griefed/ServerPackCreator/commit/f0c19465ba0daf6c6d8ce090913a24e3ab8d820c))
* Fix axios instance for api ([12508f3](https://git.griefed.de/Griefed/ServerPackCreator/commit/12508f34884ebce85d88386f35363efd34d35e1d))
* Fix building of list of fallbackmods if property contains , ([e000f25](https://git.griefed.de/Griefed/ServerPackCreator/commit/e000f2549e673b505df6b5d71a5c8455d78ddfab))
* Fix downloading of server packs by storing the path of the server pack in the DB in the path column ([8a47213](https://git.griefed.de/Griefed/ServerPackCreator/commit/8a472136554f25ac06caf1a013fd64a5dda6e79e))
* Fix downloading of server packs by updating the path of the server pack in the DB in the path column ([64dc619](https://git.griefed.de/Griefed/ServerPackCreator/commit/64dc619389442cfe5f6eddbb9ad98969dd60d987))
* Fix Forge installer log deletion. Forgot String.format with destination. ([1b44cb8](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b44cb8cc8022ffd7335e86823b98b7c31430e5f))
* Fix loading config not setting modloader specified in config ([cb50348](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb50348c6a4e4615db397948aefca5edabbbb83a))
* Fix missing serverpackcreator.properties for tests and do not run tests on GitHub releases. ([8895be8](https://git.griefed.de/Griefed/ServerPackCreator/commit/8895be80bfc76165d0347ee97e750301d6870afe))
* Fix reverseOrderList not actually reliably reversing a list ([bbfdea5](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbfdea53b9d6668f35f2635a295f042a45beade5))
* Fix scheduling to not run every second or minute ([9e87689](https://git.griefed.de/Griefed/ServerPackCreator/commit/9e87689c0dad05569bc74f7aba1bb687602c8bd4))
* Fix some mods broken dependency definitions breaking SPC funcitonality. Closes issue [#80](https://git.griefed.de/Griefed/ServerPackCreator/issues/80). ([a1c8a7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a1c8a7ef419ba7dcf90b74694c5f04480edfe807))
* Fix status message in GUI being displayed incorrectly on some Linux distros. Closes issue [#79](https://git.griefed.de/Griefed/ServerPackCreator/issues/79) ([5e7c08d](https://git.griefed.de/Griefed/ServerPackCreator/commit/5e7c08d886c9b1b7ef0640fe9cfe6f54e0d1fdc9))
* Fix user in Docker environment ([39f6bc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/39f6bc1fd6ca75e6783ae77c736983e601c550ab))
* Generate Minecraft 1.17+ Forge compatible scripts. Fixes issue [#84](https://git.griefed.de/Griefed/ServerPackCreator/issues/84). ([7d07e1d](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d07e1dad99c175b330f18c4c6cc83b00d43acac))
* Hopefully fix ServerPackCreator becoming unresponsive after generating a few server packs. Hopefully closes issue [#76](https://git.griefed.de/Griefed/ServerPackCreator/issues/76). ([aa92d9b](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa92d9b5afb3ceec2345c311ae90062aa45ce6c5))
* If no startup parameter is specified, assume -cli, else use the provided one. ([cad6e55](https://git.griefed.de/Griefed/ServerPackCreator/commit/cad6e55e73048003896fdde1f3e2b27ce69fa78a))
* Implement log4j exploit protection ([971fc4f](https://git.griefed.de/Griefed/ServerPackCreator/commit/971fc4fe7cfa362b48197d0222373a884c517f92))
* Improve configuration loading. Prevent NullPointers when reading Minecraft version, modloader, modloader version. ([0507ab7](https://git.griefed.de/Griefed/ServerPackCreator/commit/0507ab736d852415f2666937b1174429e7bac109))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([c556baa](https://git.griefed.de/Griefed/ServerPackCreator/commit/c556baaac7fe41ec0a11958e868a1da5bf26b14f))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([b108b67](https://git.griefed.de/Griefed/ServerPackCreator/commit/b108b6773d99fa8747fca016c70479521b2d6b1a))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([6afdbb4](https://git.griefed.de/Griefed/ServerPackCreator/commit/6afdbb4eb04af7b53ba64603fcc6348610edd4af))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([dd3ff6e](https://git.griefed.de/Griefed/ServerPackCreator/commit/dd3ff6ecf43a64ef29481007c700c74595b38229))
* Instead of using an external applications.properties for customizing, use our serverpackcreator.properties...which exists anyway! ([3794896](https://git.griefed.de/Griefed/ServerPackCreator/commit/3794896971e775d4f4d37aee7d340cc0510d8024))
* Last fallback in case no value can be found for a given key. ([53817d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/53817d4b8672359ff4a5b244c127afc581881436))
* Make date created and last modified in web frontend human-readable. ([2da9c29](https://git.griefed.de/Griefed/ServerPackCreator/commit/2da9c29c28aebe77365fec1426021a69e3e5ba7c))
* Make sure clientMods is set correctly with no starting [ or ending ] ([c98ef0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/c98ef0e0777673a6015d738c378b3bf30edf7eff))
* Modloader selection visually defaulted to Forge if no configuration was found in a given serverpackcreator.conf, but the value wasn't correctly set, resulting in the user having to select Forge manually anyway. ([d126447](https://git.griefed.de/Griefed/ServerPackCreator/commit/d12644714a8281e5dd7063521e28235b9204d5a3))
* More hardening against CVE-2021-44228 ([eaa4668](https://git.griefed.de/Griefed/ServerPackCreator/commit/eaa4668731ded0145f47d810d65dbf703306009c))
* Move destination acquisition into if-statement ([5d356a9](https://git.griefed.de/Griefed/ServerPackCreator/commit/5d356a95ec85cd04879a99c64538c113422f56ab))
* Move destination into if-statement ([9ae5ceb](https://git.griefed.de/Griefed/ServerPackCreator/commit/9ae5ceb8b314b5b6e065496118bc13aa6a3cab46))
* Only copy file from JAR-file if it is not found on local filesystem. ([09e271e](https://git.griefed.de/Griefed/ServerPackCreator/commit/09e271e4a8c6e0d202fd4a1db175087c8c9f9966))
* Open dialog whether the user wants to browse the generated server pack with our JFrame as parent, instead of JTabbedPane ([aa647f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/aa647f77429e6207927e5b1a743cb5b8f0be4887))
* Prevent dialog after server pack generation from becoming longer with each run. Removes the path to the server pack, though. Meh ([2260693](https://git.griefed.de/Griefed/ServerPackCreator/commit/226069366091155e11d9a1b7da9521f9802f168d))
* Prevent encapsulateListElements from writing duplicate entries ([1e64cd6](https://git.griefed.de/Griefed/ServerPackCreator/commit/1e64cd67dcbfcf95ccb544f84b70ee39e5123e75))
* Prevent NPE for clientside-only mod property ([b188a85](https://git.griefed.de/Griefed/ServerPackCreator/commit/b188a858f637b8329447be08ed3701c43a713b00))
* Prevent NullPointerException if version or author are not defined in the modpacks manifest. ([d7336ba](https://git.griefed.de/Griefed/ServerPackCreator/commit/d7336baaae13781538d132ed62b24e25825da721))
* Prevent resizing of window during generation of server pack, to prevent freezes due to Forge installer log spamming. Seriously, that thing spams more than any bot I know of. ([89edc6f](https://git.griefed.de/Griefed/ServerPackCreator/commit/89edc6f61fbd40e1b1ed46871d70f103139200a5))
* Prevent unlikely, but possible, overwriting of properties file with wrong content from i18n initialization ([3675b09](https://git.griefed.de/Griefed/ServerPackCreator/commit/3675b0934253c5d03457cd64b6ca96825e0ee063))
* Prevent UpdateChecker from crashing SPC when any instance can not be reached ([b96cdb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/b96cdb347329e4512ecfe2b7c11e66479ee8be10))
* Print correct string for server pack suffix ([08c69e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/08c69e1be591421138d88429bc007091a13837ab))
* Re-add nogui parameter for fabric scripts. Apparently that is needed. Local tests proved it is not. My Little Fabric: Servers Are Magic ([6381c3b](https://git.griefed.de/Griefed/ServerPackCreator/commit/6381c3b1fc741ee684740db6d9fb5d7ccfb8f4d1))
* Read correct log in modloader-installer log tab ([095d05e](https://git.griefed.de/Griefed/ServerPackCreator/commit/095d05edd1235957e13b98122deba8c54c9efa12))
* remove `--` from Forge `nogui` argument. Fixes GH[#82](https://git.griefed.de/Griefed/ServerPackCreator/issues/82) ([f585891](https://git.griefed.de/Griefed/ServerPackCreator/commit/f58589114cd255a191b226c08c89f8dfeeac72dc))
* Set downloads and votes to zero upon generation of server pack ([be84232](https://git.griefed.de/Griefed/ServerPackCreator/commit/be8423251d82aea1a7639cd30bbaf9d0f06397df))
* Update frontend packages so it no longer throws some CSS minify errors around the block ([342e3c8](https://git.griefed.de/Griefed/ServerPackCreator/commit/342e3c895c6c090a09475d0d57a7c3d47e1238b7))
* Use inverted order array for Fabric version when checking for Fabric version upon config load and therefore set correct Fabric version. ([de5cdcf](https://git.griefed.de/Griefed/ServerPackCreator/commit/de5cdcf0b1bf1f81c812bd685dc41a5ef74b7f09))
* When writing configfiles, encapsulate every element of String Lists in `"` in order to avoid problems described in issue [#71](https://git.griefed.de/Griefed/ServerPackCreator/issues/71). Fixes and closes issue [#71](https://git.griefed.de/Griefed/ServerPackCreator/issues/71). ([0e029ec](https://git.griefed.de/Griefed/ServerPackCreator/commit/0e029ec477864ea765e8ad446ac2b9b93186b952))
* Whoops ([2c1841c](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c1841ca18856ba0d398641d52923f8537135c71))
* **deps:** update dependency core-js to v3.19.3 ([f7a3140](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7a314067fae89105aed95cae95188c827812c2f))
* **deps:** update dependency vue to v3.2.24 ([4b44938](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b4493876f4476c6ecc90497bbc621e1aa1b545a))
* **deps:** update dependency vue to v3.2.26 ([be664e8](https://git.griefed.de/Griefed/ServerPackCreator/commit/be664e84c506155157e879f42b50426b0f8e7800))
* **webservice:** Display correct tooltips for buttons in MainLayout ([d4530d3](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4530d35727e3b092fdb8383f546dda8dcc825d2))


### Other

* Add CraftPresence to fallbacklist of clientside-only mods (Reported by Law on Discord) ([88150ab](https://git.griefed.de/Griefed/ServerPackCreator/commit/88150ab82f654eba1d5be27566f3b74fea5d2b66))
* Add GitLab templates for Service Desk ([6be793f](https://git.griefed.de/Griefed/ServerPackCreator/commit/6be793fbe24177de6d17088f9ce0371c17fd0e77))
* Add improvement template. To be evaluated over time whether this is usefull ([218622b](https://git.griefed.de/Griefed/ServerPackCreator/commit/218622b7b091a7a90508449d1935afca3ff39a85))
* Add list of addons to README. Currently only the ExampleAddon I made is available. ([3367a8b](https://git.griefed.de/Griefed/ServerPackCreator/commit/3367a8bf839486c86efdb41f32caa85bcbd5a6bb))
* Add missing space in lang keys for copyDirs help. Closes issue [#78](https://git.griefed.de/Griefed/ServerPackCreator/issues/78) ([3539582](https://git.griefed.de/Griefed/ServerPackCreator/commit/35395827fb5a8e837ccae61925a0557aae544f29))
* Add moreoverlays- to list of fallback modlist ([e990661](https://git.griefed.de/Griefed/ServerPackCreator/commit/e9906612dd5b583c505f0eb0d4b5b5cb7fd769b2))
* Add moveoverlays- to list of fallback modslist ([64ead40](https://git.griefed.de/Griefed/ServerPackCreator/commit/64ead409e5ffb156da1d9b3ed8103f722483e3e2))
* Added debug logging when a new entry to files or directories to exclude is made ([719bb85](https://git.griefed.de/Griefed/ServerPackCreator/commit/719bb85b3c060854955b02fb225ddc171ddf5d80))
* Change order of input so users don't confuse the log-section to be related to the webservice. ([e352d12](https://git.griefed.de/Griefed/ServerPackCreator/commit/e352d120603e6810a3a3ed0b3e46b021db4ca5a0))
* Changelog from alpha branch ([c0e9383](https://git.griefed.de/Griefed/ServerPackCreator/commit/c0e93837a8751a3dcf06818953bf6e9ceea8e918))
* Clarify when I started with Java to put things into perspective. ([16f52f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/16f52f771587c94843a09eb46be7d047793b604e))
* Cleanup after build with tests. ([145e9d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/145e9d5b171e5afaaaaa5c5488437388d12ae4bf))
* Fix minor typo in language key ([9177763](https://git.griefed.de/Griefed/ServerPackCreator/commit/91777632c7ef1715f45af28ddb4f0848d5abb432))
* Fix tests, docs and add TODOs regarding lang keys ([2dac4e1](https://git.griefed.de/Griefed/ServerPackCreator/commit/2dac4e1f0a7e53f7b04cfce982c1a6d2c99c5747))
* Include JProfiler and ej-Technologies in Awesomesauce section ([b989173](https://git.griefed.de/Griefed/ServerPackCreator/commit/b9891736d997c0c6ad81a8f4b650a1e7c0368dec))
* Label issues and pull requests made by sponsors ([95591f9](https://git.griefed.de/Griefed/ServerPackCreator/commit/95591f90bb3af101ba7571230bccf7d2a19c450a))
* List addresses for Java documentation ([b90045b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b90045b05878f455947e0fcf2e38149ebdce7c05))
* List all places where ServerPackCreator is available at ([cb12edc](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb12edce4e26271d271344d90b7421c3118b3ee2))
* Mention libraries used and add third-party licenses ([8d4c715](https://git.griefed.de/Griefed/ServerPackCreator/commit/8d4c71535a46335788b3f8337d1581144c18f6bc))
* New screenshots, comparisons between different modes ([12ed5f6](https://git.griefed.de/Griefed/ServerPackCreator/commit/12ed5f6ec63cf1a04dd357955fa799c07e05780c))
* README overhaul. Include guides. Update guides. Number chapters. Cleanup ([7d0d2bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/7d0d2bd5b2823e64a7aa20a2239699533f9dc930))
* Remove --no-daemon from run configurations ([a76e357](https://git.griefed.de/Griefed/ServerPackCreator/commit/a76e3570de7cb7cbf75a96697f122cf02e69e693))
* Remove mention of armv7 docker images as they are no longer being supplied ([72e8308](https://git.griefed.de/Griefed/ServerPackCreator/commit/72e83089ef328494dcb07115f649682eec7edd59))
* Remove mentions of CurseForge until the custom API has been implemented. Cleanups.. ([b7c6d09](https://git.griefed.de/Griefed/ServerPackCreator/commit/b7c6d09459aba6c24eadb94c08663ef4e6062471))
* Remove no longer needed lang keys ([6435fbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/6435fbc73be7405290a48a16c2b053a0fa09e1ed))
* Remove no longer needed run configurations ([7e43ee3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e43ee3e6be65d55da98c2c06a19d69abd055880))
* Remove no longer relevant license ([64fbeeb](https://git.griefed.de/Griefed/ServerPackCreator/commit/64fbeeb9593a3696b9a53f1f436bbdf6d00e22e9))
* Remove unneeded imports ([8482d29](https://git.griefed.de/Griefed/ServerPackCreator/commit/8482d295eb1d731d1c02c654363dafe235ba9910))
* Remove unused language keys ([43fdba7](https://git.griefed.de/Griefed/ServerPackCreator/commit/43fdba70b1dfc52139c9fb2f255a065bdd92ef12))
* Rename job to better reflect what is actually happening ([4885952](https://git.griefed.de/Griefed/ServerPackCreator/commit/48859526c2c259ffb8f74f23ba83155409fe1384))
* Some cleanups and TODOs ([da02619](https://git.griefed.de/Griefed/ServerPackCreator/commit/da0261950ac780dea53055c3c41b5b0f513628b0))
* Some more logging ([d4fa143](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4fa143125b1eeb1e8e69e020906788a2224853f))
* TODO ([085c831](https://git.griefed.de/Griefed/ServerPackCreator/commit/085c83132b54693e05bab5d01eb77666ea5642ec))
* Udpate versions ([eecc90a](https://git.griefed.de/Griefed/ServerPackCreator/commit/eecc90a88dfa2d787a256e341dc422a124a22cab))
* Update git index for gradlew so execution is always allowed ([057b6c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/057b6c2e1514f5287596e4004cbbb790f34c1d12))
* Update gitignore to exclude new files generated by tests ([4147138](https://git.griefed.de/Griefed/ServerPackCreator/commit/4147138bfadee97e0671bfb1f8a3b41c657d62b3))
* Update README ([1fc9df7](https://git.griefed.de/Griefed/ServerPackCreator/commit/1fc9df72c1d1a8f5c7d82dc18a27af33e61b1307))
* Update README in resources ([4b8a3f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b8a3f4415a419e1b4acab1b86f79d83343da48f))
* Update README with information from self-hosted GitLab pipeline status. Expand on deploy and versioning info. Add more Jetbrains swag. All that good stuff. ([c36ad6c](https://git.griefed.de/Griefed/ServerPackCreator/commit/c36ad6cd313c83b4b321ae768922bfd16c751f07))
* Update README with new addon example ([bcca1ce](https://git.griefed.de/Griefed/ServerPackCreator/commit/bcca1ce72aff02ad28cdd3408286bfa8c64311da))
* Update third party-licenses ([b41a15f](https://git.griefed.de/Griefed/ServerPackCreator/commit/b41a15f94768f52069f3a969d511de9c387d0634))
* WHITESPACE! ([de9ebcc](https://git.griefed.de/Griefed/ServerPackCreator/commit/de9ebcc2147e6b205789d4f1c82720daed0a6ddd))
* **deps:** pin dependencies ([f6d8822](https://git.griefed.de/Griefed/ServerPackCreator/commit/f6d88221cb966c739365f352b2a9c6bb660eeb17))
* **deps:** pin dependencies ([358275b](https://git.griefed.de/Griefed/ServerPackCreator/commit/358275b16134c3953250e0dbcc763005a7a6b344))
* **deps:** update dependency @babel/eslint-parser to v7.16.5 ([d90ef33](https://git.griefed.de/Griefed/ServerPackCreator/commit/d90ef333df1d80fde46189faebe288f53f211427))
* **deps:** update dependency @quasar/app to v3.2.4 ([e33df47](https://git.griefed.de/Griefed/ServerPackCreator/commit/e33df47cb0182788a995f55a7a1852f3d75919d4))
* **deps:** update dependency @quasar/app to v3.2.6 ([292d4f5](https://git.griefed.de/Griefed/ServerPackCreator/commit/292d4f5d8b2c048aa6ed28b18e0bdf0eaa4de79c))
* **deps:** update dependency @quasar/app to v3.2.9 ([d61a461](https://git.griefed.de/Griefed/ServerPackCreator/commit/d61a4618eb3246b9bc96f19fbf0833f075af32a7))
* **deps:** update dependency @quasar/app to v3.3.2 ([e43122d](https://git.griefed.de/Griefed/ServerPackCreator/commit/e43122d85cb34e81d884feffe87023669f62ee3b))
* **deps:** update dependency @quasar/extras to v1.12.4 ([10b76e6](https://git.griefed.de/Griefed/ServerPackCreator/commit/10b76e68202bc00f55660f356b0471f018714b76))
* **deps:** update dependency @types/node to v16.11.10 ([e38cd23](https://git.griefed.de/Griefed/ServerPackCreator/commit/e38cd23fdda88247f678e718831dcb7f1dba7580))
* **deps:** update dependency @types/node to v16.11.14 ([76baa87](https://git.griefed.de/Griefed/ServerPackCreator/commit/76baa87cb160827729922b4cd11a407cf523fb9c))
* **deps:** update dependency @types/node to v16.11.15 ([7b8dd46](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b8dd46df3819ab64778b033403ee30b59ee0a7b))
* **deps:** update dependency axios to v0.25.0 ([3008f24](https://git.griefed.de/Griefed/ServerPackCreator/commit/3008f24ac04a5e50cf9cc94af7fffd70b85621f3))
* **deps:** update dependency core-js to v3.20.0 ([809855a](https://git.griefed.de/Griefed/ServerPackCreator/commit/809855a1defa480ee9869c3bf3124474e0a8c34f))
* **deps:** update dependency core-js to v3.20.1 ([cde9246](https://git.griefed.de/Griefed/ServerPackCreator/commit/cde9246b792470bfc4e9308bb32bea2ae3bb8ada))
* **deps:** update dependency core-js to v3.20.2 ([b4bd45e](https://git.griefed.de/Griefed/ServerPackCreator/commit/b4bd45e7ef3b140f4941fb9e93f6fce8ac390394))
* **deps:** update dependency eslint to v8.4.1 ([2db3a36](https://git.griefed.de/Griefed/ServerPackCreator/commit/2db3a36ae3f5f12e1263fbb91d5a7984804c58a8))
* **deps:** update dependency eslint to v8.5.0 ([6f7c5c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/6f7c5c24b8cb8a68427836331b1b2e758fdfeaa8))
* **deps:** update dependency eslint to v8.6.0 ([2e6ab21](https://git.griefed.de/Griefed/ServerPackCreator/commit/2e6ab21ee3ba1ff0649b4442e9edd3d8a1cb9b02))
* **deps:** update dependency eslint-plugin-vue to v8.2.0 ([e2df4dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2df4dc25fae418fdf495d7c2d4acbf1cae68567))
* **deps:** update dependency eslint-plugin-vue to v8.3.0 ([61e2eb4](https://git.griefed.de/Griefed/ServerPackCreator/commit/61e2eb47a22615bc23ef5040546ababeb8ca7a22))
* **deps:** update dependency eslint-plugin-vue to v8.4.1 ([0b16371](https://git.griefed.de/Griefed/ServerPackCreator/commit/0b16371881d6a5069744fc6b05a5fd05353b7dc6))
* **deps:** update dependency gradle to v7.3.1 ([6964401](https://git.griefed.de/Griefed/ServerPackCreator/commit/6964401eddbfadb265bb15fbd8a1aacfc5e6ea50))
* **deps:** update dependency gradle to v7.3.2 ([69019b9](https://git.griefed.de/Griefed/ServerPackCreator/commit/69019b97c3e2f4c38ae1a6eb4b8913a095986714))
* **deps:** update dependency org.apache.activemq:artemis-jms-server to v2.19.0 ([3245976](https://git.griefed.de/Griefed/ServerPackCreator/commit/3245976c0f88eef1e0e2b25da88d6eefed7e9dd3))
* **deps:** update dependency org.apache.logging.log4j:log4j-api to v2.17.1 ([01c8a80](https://git.griefed.de/Griefed/ServerPackCreator/commit/01c8a80de9499ea377bf03eff6eaac1b73f8efb9))
* **deps:** update dependency org.apache.logging.log4j:log4j-core to v2.17.1 ([7cbd208](https://git.griefed.de/Griefed/ServerPackCreator/commit/7cbd208142e559d57c37f12ccc5a738a2f682bc1))
* **deps:** update dependency org.apache.logging.log4j:log4j-jul to v2.17.1 ([48cf50d](https://git.griefed.de/Griefed/ServerPackCreator/commit/48cf50df5230f399c93f8abf25d7aff5f3500697))
* **deps:** update dependency org.apache.logging.log4j:log4j-slf4j-impl to v2.17.1 ([de850ff](https://git.griefed.de/Griefed/ServerPackCreator/commit/de850ff6bb2c9600be0a06b06f84fe594c190427))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.15.0 ([1018e10](https://git.griefed.de/Griefed/ServerPackCreator/commit/1018e106aeffa8439e0f5dd2aeaa2d1e6bf68639))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.16.0 ([5632772](https://git.griefed.de/Griefed/ServerPackCreator/commit/5632772a0785567f1ed0142c845120aac98a30bb))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.17.0 ([9ab5fc7](https://git.griefed.de/Griefed/ServerPackCreator/commit/9ab5fc7e189765d9a42dabb66274870e06ecd409))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.17.1 ([32af395](https://git.griefed.de/Griefed/ServerPackCreator/commit/32af395878dfe45ebfed0e0dbbcd77f104418558))
* **deps:** update dependency quasar to v2.4.12 ([8c3ab82](https://git.griefed.de/Griefed/ServerPackCreator/commit/8c3ab82e8889276595ce89d7b1b4b64d1a37a0c8))
* **deps:** update dependency quasar to v2.4.2 ([28ec385](https://git.griefed.de/Griefed/ServerPackCreator/commit/28ec3853f08d5e16110a1d95e1a2f95add7fc164))
* **deps:** update dependency quasar to v2.4.3 ([c3ff9b2](https://git.griefed.de/Griefed/ServerPackCreator/commit/c3ff9b2e55f4cedf6346d53a4395fcea633f2967))
* **deps:** update dependency quasar to v2.4.9 ([467b615](https://git.griefed.de/Griefed/ServerPackCreator/commit/467b6153cd2284a17815f8eee025dd88caed3c13))
* **deps:** update dependency tsparticles to v1.37.6 ([e69e81a](https://git.griefed.de/Griefed/ServerPackCreator/commit/e69e81a4263706ed969f4f7f1454dc550ee6659c))
* **deps:** update dependency tsparticles to v1.38.0 ([fa498bc](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa498bc7fd1df3067a2d12e6c227c35635848a46))
* **deps:** update dependency tsparticles to v1.39.1 ([d231885](https://git.griefed.de/Griefed/ServerPackCreator/commit/d231885bb4b569f1eba3eed492c22a653f9f72ae))
* **deps:** update dependency vue to v3.2.28 ([c2fb183](https://git.griefed.de/Griefed/ServerPackCreator/commit/c2fb1836712dd415ea61ba252d69f307f1924b63))
* **deps:** update dependency vue to v3.2.29 ([57246dd](https://git.griefed.de/Griefed/ServerPackCreator/commit/57246dda971532cd7eae1d09b904e47631fe250e))
* **deps:** update ghcr.io/griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.3 ([a849b0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a849b0eed192bee1e0cf175930375beffc97f226))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2 ([e3d9f7c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3d9f7c907c39619fe0c36504472722140a03cec))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.1 ([18a75a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/18a75a55d5782e4823fda59915bfedc7111d35af))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.2 ([65f7d15](https://git.griefed.de/Griefed/ServerPackCreator/commit/65f7d1594cd6f9827b3c42cf59653623ee791b2e))
* **deps:** update jamesives/github-pages-deploy-action action to v4.1.8 ([1d4a7f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/1d4a7f7e3c389abdc1513050327b6018848441ff))
* **deps:** update jamesives/github-pages-deploy-action action to v4.2.0 ([20a6b82](https://git.griefed.de/Griefed/ServerPackCreator/commit/20a6b828e163b949dc29f534241bb3dc98ccb923))
* **deps:** update jamesives/github-pages-deploy-action action to v4.2.3 ([a3706fc](https://git.griefed.de/Griefed/ServerPackCreator/commit/a3706fca5b4164ce8c2aeb569dfa452272197593))
* **deps:** update npm to v8 ([f446f11](https://git.griefed.de/Griefed/ServerPackCreator/commit/f446f1167dc950ea509c4117743a380957c0502e))
* **deps:** update plugin com.github.ben-manes.versions to v0.40.0 ([55d37b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/55d37b1f93623c823c788a9ee970a00a4cd961a2))
* **deps:** update plugin com.github.ben-manes.versions to v0.41.0 ([28989fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/28989fdbd7aa57b6b036d91082694b047d266e4e))
* **deps:** update registry.gitlab.com/haynes/jacoco2cobertura docker tag to v1.0.8 ([8df16d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/8df16d58cbd755361e7b1354841cbc5a4d43e3eb))
* **deps:** update spring boot to v2.6.1 ([d0d9f03](https://git.griefed.de/Griefed/ServerPackCreator/commit/d0d9f03b447443fb08da3b4ee517ee85cf08e29d))
* **deps:** update spring boot to v2.6.2 ([b6e4850](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6e4850ff8ebe5f18e5472563bb3782cfd1ea0a9))
* **deps:** update spring boot to v2.6.3 ([6a12a17](https://git.griefed.de/Griefed/ServerPackCreator/commit/6a12a17c95763abf0bd8f85b32b6dedea82f9df9))
* **deps:** update typescript-eslint monorepo to v5.10.0 ([6cec6a6](https://git.griefed.de/Griefed/ServerPackCreator/commit/6cec6a6d662930906c608b00e85e84dfe262c12a))
* **deps:** update typescript-eslint monorepo to v5.6.0 ([c27b3b0](https://git.griefed.de/Griefed/ServerPackCreator/commit/c27b3b04ddb8219fd0c80f5e850c243bcb540634))
* **deps:** update typescript-eslint monorepo to v5.7.0 ([e6b01d8](https://git.griefed.de/Griefed/ServerPackCreator/commit/e6b01d858d2b9e25656fdbe07904b840242d2003))
* **deps:** update typescript-eslint monorepo to v5.8.0 ([1f29f23](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f29f236d19653487b791576c76cfee8c58e1e88))
* **deps:** update typescript-eslint monorepo to v5.8.1 ([ded0c7b](https://git.griefed.de/Griefed/ServerPackCreator/commit/ded0c7b39e9d48a06b7b6fc87537670e0a430f69))
* **deps:** update typescript-eslint monorepo to v5.9.0 ([7b705a4](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b705a4f8dab2c8055629078208b89ea4c264b46))
* **deps:** update typescript-eslint monorepo to v5.9.1 ([a766e2a](https://git.griefed.de/Griefed/ServerPackCreator/commit/a766e2a3cc33e4002f1bc38c97c997a6f24be9d2))
* **webservice:** Add instructions on how to build SPC locally ([6e873ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/6e873ac174109b6d837de2c237d587128f5763a3))
* **webservice:** Expand readme with webservice related information ([fe5d440](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe5d440cc71a6445d211b7c3ca8ebfb0268eda6e))
* **webservice:** Properly setup manifest. Include up-to-date copies of license, readme, contributing, code of conduct, changelog in the jar. Exclude said files in backend/main/resources with gitignore. ([4812918](https://git.griefed.de/Griefed/ServerPackCreator/commit/4812918a72bf9dfdec89d4f052b1d7f173ae688c))

## [3.0.0-alpha.19](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.18...3.0.0-alpha.19) (2022-02-10)


### :scissors: Refactor

* Change groupID. Also change url for OSSRH to the correct snapshot url. ([e9ff899](https://git.griefed.de/Griefed/ServerPackCreator/commit/e9ff899023f5f2386653cf49e29dd9cea87ab99e))
* Change groupID. Also change url for OSSRH. Now everything works when ([0cf5dbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/0cf5dbccc8f40cf16e28a4011ede3264a7626076))
* Move plugins folder creationf to DefaultFiles. Create example file for disabling plugins. Improve logging for installed plugin extensions. ([1fad8ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/1fad8ac858377c43250d4f6f644ecf7c719c7e02))


### 📔 Docs

* Add documentation for UpdateChecker utility. ([f804589](https://git.griefed.de/Griefed/ServerPackCreator/commit/f8045896d075fc67d0befa1565e88ddd1a831ba5))
* Cleanup changelog due to some sort of tag issue I created. Yay. ([17c234b](https://git.griefed.de/Griefed/ServerPackCreator/commit/17c234bfbe56760cefd07bf98b3e7357f8167a55))
* Cleanup changelog due to some sort of tag issue I created. Yay. ([65bf366](https://git.griefed.de/Griefed/ServerPackCreator/commit/65bf366c368f13de51f2f8963d7c3ce9ecbc954b))
* Generate patch release on docs change. ([d6e65ea](https://git.griefed.de/Griefed/ServerPackCreator/commit/d6e65eadb8e5c5071d8b8a693433ae7e38aa2582))


### 🦊 CI/CD

* Add signing and publishing. Will be published to GitLab, GitHub, git.griefed, OSSRH on new tag creation. ([b60a8f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/b60a8f2a63c986eb609975f8299719aa9f731e32))
* Switch VersionChecker to library implementation. Update jms-server. Minor URL refactorings in gradle publishing. ([62c438a](https://git.griefed.de/Griefed/ServerPackCreator/commit/62c438a75d5a783d741fbacfc8c0861899892f69))
* **deps:** bump com.github.ben-manes.versions from 0.41.0 to 0.42.0 ([6456e3f](https://git.griefed.de/Griefed/ServerPackCreator/commit/6456e3f211af4dda8f693c5f6222950b709032bb))
* **deps:** bump versionchecker from 1.0.4 to 1.0.5 ([57f0dd0](https://git.griefed.de/Griefed/ServerPackCreator/commit/57f0dd0f03a9ca48fe7fde7bec144c0e7136a3a8))
* Deactivate push on docker tests. Remove unnecessary file renaming in build release. Properly run publish job. ([481a048](https://git.griefed.de/Griefed/ServerPackCreator/commit/481a0488e27333ae3c7964a1fa67b8234e3ac6ac))
* Fix artifact names for renaming ([d4f4f35](https://git.griefed.de/Griefed/ServerPackCreator/commit/d4f4f352150a874f270e4468bcf102df10b72c68))
* Prevent Generate Release job from running unnecessarily ([5be9fcd](https://git.griefed.de/Griefed/ServerPackCreator/commit/5be9fcdf2624991b9eaf845aafc3bdef8d34f04d))
* Remove unnecessary logins from test job ([dac135c](https://git.griefed.de/Griefed/ServerPackCreator/commit/dac135cc4e079a996e8ca45ae95d019345ea2283))
* You have a problem, so you use regex. Now you have two problems. ([b05c007](https://git.griefed.de/Griefed/ServerPackCreator/commit/b05c0075a810f89ba79ff3a9f32939e0abbe0ca8))


### 🧪 Tests

* Add more unit tests. ([ae06aa6](https://git.griefed.de/Griefed/ServerPackCreator/commit/ae06aa64a2463f31305efb072e7c5c49b42e5575))


### 🚀 Features

* Allow generation of a server pack by uploading it to the webservice. ([c92ddd2](https://git.griefed.de/Griefed/ServerPackCreator/commit/c92ddd2d01ec7851fed4696608a71b6c9efeea08))
* Create server packs from zipped modpacks. Point modpackDir at a ZIP-file which contains a modpack in the ZIP-archives root. ([fbdae16](https://git.griefed.de/Griefed/ServerPackCreator/commit/fbdae16759e90cfd86786ee43ccf7a448fae0cce))


### 🛠 Fixes

* Prevent UpdateChecker from crashing SPC when any instance can not be reached ([b96cdb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/b96cdb347329e4512ecfe2b7c11e66479ee8be10))


### Other

* Cleanup after build with tests. ([145e9d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/145e9d5b171e5afaaaaa5c5488437388d12ae4bf))
* Update README with new addon example ([bcca1ce](https://git.griefed.de/Griefed/ServerPackCreator/commit/bcca1ce72aff02ad28cdd3408286bfa8c64311da))
* **deps:** update dependency @quasar/app to v3.3.2 ([e43122d](https://git.griefed.de/Griefed/ServerPackCreator/commit/e43122d85cb34e81d884feffe87023669f62ee3b))
* **deps:** update dependency eslint-plugin-vue to v8.4.1 ([0b16371](https://git.griefed.de/Griefed/ServerPackCreator/commit/0b16371881d6a5069744fc6b05a5fd05353b7dc6))
* **deps:** update ghcr.io/griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.3 ([a849b0e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a849b0eed192bee1e0cf175930375beffc97f226))
* **deps:** update jamesives/github-pages-deploy-action action to v4.2.3 ([a3706fc](https://git.griefed.de/Griefed/ServerPackCreator/commit/a3706fca5b4164ce8c2aeb569dfa452272197593))

## [3.0.0-alpha.18](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.17...3.0.0-alpha.18) (2022-01-30)


### :scissors: Refactor

* Build for armv7 again thanks to [@djmaze](https://git.griefed.de/djmaze) and their dind-image-with-armhf available at https://github.com/djmaze/dind-image-with-armhf. Store and read version more efficiently by writing it to the manifest. ([d5bde7b](https://git.griefed.de/Griefed/ServerPackCreator/commit/d5bde7b7d2f0f073753b94c9f8a0e382d3280c6e))
* Improve update checks by sequentially checking GitHub, GitGriefed and then GitLab ([c25eaac](https://git.griefed.de/Griefed/ServerPackCreator/commit/c25eaacd6767b721a7624847f40dd3639c7f7430))
* Provide improved Fabric Server Launcher as well as old launcher. Create SERVER_PACK_INFO.txt with information about said improved launcher. Thanks to @TheButterbrotMan for the detailed conversations in issue [#202](https://git.griefed.de/Griefed/ServerPackCreator/issues/202) ([6148a3e](https://git.griefed.de/Griefed/ServerPackCreator/commit/6148a3eca54543171d3c63f8336b4a01acc2f407))
* Use a single ExtensionFactory as per pf4j docs ([62ed8e7](https://git.griefed.de/Griefed/ServerPackCreator/commit/62ed8e76fac1d3b28df557da89d39e1f166ca14a))


### 👀 Reverts

* Re-implement removal and change of new entries to copyDirs and clientMods ([eec45d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/eec45d5950b088625760187b070bace44940d57e))


### 📔 Docs

* Change version dropdown to input ([c5a5893](https://git.griefed.de/Griefed/ServerPackCreator/commit/c5a589358382085c7cf416f3608150bd012998bb))
* Exclude certain classes from JaCoCo test coverage ([1f4cfbc](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f4cfbc73bdcd9267bb9e56e8bbd95ff7a8b1866))
* List minigame example addon ([3577d33](https://git.griefed.de/Griefed/ServerPackCreator/commit/3577d33dae6cc895d3fbb97f57d9bcc4b716ecc2))


### 🦊 CI/CD

* Pass host for git clone so we can always clone from the infrastructure we are running on ([faa937a](https://git.griefed.de/Griefed/ServerPackCreator/commit/faa937ae750941fce8c52b8434a82ada816de932))
* Switch to GHCR images to prevent job failures due to rate limiting by DockerHub ([bbe0c0b](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbe0c0b7e7db49189e22bcb2f2b1f55d083be6fa))
* Update griefed/baseimage-ubuntu-jdk-8 to 2.0.1 ([d77a61f](https://git.griefed.de/Griefed/ServerPackCreator/commit/d77a61f7e1cfd874f5ec9df05c1c56737bfd30ed))
* **deps-dev:** bump @types/node from 17.0.8 to 17.0.9 in /frontend ([a642a14](https://git.griefed.de/Griefed/ServerPackCreator/commit/a642a146fa2d2956970dc9daa01671c1b02a4873))
* **deps-dev:** bump @types/node from 17.0.9 to 17.0.10 in /frontend ([96e1d62](https://git.griefed.de/Griefed/ServerPackCreator/commit/96e1d6292a35016df0ef31bb41ed0cd1940c3cfb))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([3a7dffc](https://git.griefed.de/Griefed/ServerPackCreator/commit/3a7dffcd05f0610bea570e7253a96510927dca63))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([29466f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/29466f2d9aa89935e20ef96184eae95b34329f84))
* **deps-dev:** bump eslint from 8.6.0 to 8.7.0 in /frontend ([f80efe5](https://git.griefed.de/Griefed/ServerPackCreator/commit/f80efe5c4457fb35367814556774e8e363f25d92))
* **deps-dev:** bump eslint-plugin-vue from 8.2.0 to 8.3.0 in /frontend ([f9f3e48](https://git.griefed.de/Griefed/ServerPackCreator/commit/f9f3e48ca2a775f8161bc83bb2fc380d68bdfee2))
* **deps:** bump axios from 0.24.0 to 0.25.0 in /frontend ([c9b0734](https://git.griefed.de/Griefed/ServerPackCreator/commit/c9b0734f51698a7349b6782bd7423b4ef9de7a92))
* **deps:** bump core-js from 3.20.2 to 3.20.3 in /frontend ([2a4b86f](https://git.griefed.de/Griefed/ServerPackCreator/commit/2a4b86f9f84cdc5c5b14479a7c016b0be8694309))
* **deps:** bump griefed/baseimage-ubuntu-jdk-8 from 2.0.0 to 2.0.2 ([003e1a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/003e1a1d404b0c835394b787acaa321063a7b891))
* **deps:** bump org.springframework.boot from 2.6.2 to 2.6.3 ([8e02fa7](https://git.griefed.de/Griefed/ServerPackCreator/commit/8e02fa73374e600c55ac673f3a2502a6c8e1c4eb))
* **deps:** bump quasar from 2.4.9 to 2.4.13 in /frontend ([ef5a18d](https://git.griefed.de/Griefed/ServerPackCreator/commit/ef5a18d2fb27deaac90a28020fc9ae24382ec5d5))
* **deps:** bump spring-boot-devtools from 2.6.2 to 2.6.3 ([0fe0b42](https://git.griefed.de/Griefed/ServerPackCreator/commit/0fe0b42715808954bb722f22e222a6970ed8436e))
* **deps:** bump spring-boot-starter-artemis from 2.6.2 to 2.6.3 ([9038c21](https://git.griefed.de/Griefed/ServerPackCreator/commit/9038c21f85a327fc2355254d6ead68490a55aaa1))
* **deps:** bump spring-boot-starter-data-jpa from 2.6.2 to 2.6.3 ([093ab09](https://git.griefed.de/Griefed/ServerPackCreator/commit/093ab091f40aca24e1501c47aa360735240f61fb))
* **deps:** bump spring-boot-starter-log4j2 from 2.6.2 to 2.6.3 ([a4091bd](https://git.griefed.de/Griefed/ServerPackCreator/commit/a4091bd83e8936c97bafeceba508df6692a6421b))
* **deps:** bump spring-boot-starter-quartz from 2.6.2 to 2.6.3 ([6dd76e3](https://git.griefed.de/Griefed/ServerPackCreator/commit/6dd76e3e3a635e6af613bfc3d437233518bdc9d8))
* **deps:** bump spring-boot-starter-test from 2.6.2 to 2.6.3 ([577b79a](https://git.griefed.de/Griefed/ServerPackCreator/commit/577b79a0a5dfe0fb082bf820ab84846a7645bb19))
* **deps:** bump spring-boot-starter-validation from 2.6.2 to 2.6.3 ([516db6a](https://git.griefed.de/Griefed/ServerPackCreator/commit/516db6ad4207aa079aa350f2b5d8c10323e4a67b))
* **deps:** bump spring-boot-starter-web from 2.6.2 to 2.6.3 ([7c49fd3](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c49fd341694ae81cccf77ef9abadc33b15fb22a))
* **deps:** bump vue from 3.2.26 to 3.2.29 in /frontend ([09dd657](https://git.griefed.de/Griefed/ServerPackCreator/commit/09dd6572ef82eef592a2ee746b826459311d6fdb))
* Build with --full-stacktrace ([cde8d08](https://git.griefed.de/Griefed/ServerPackCreator/commit/cde8d0845005f906f07f6878900ee7ab6ce99c98))
* Merge Release and PreRelease jobs and only run on git.griefed.de ([f3115c9](https://git.griefed.de/Griefed/ServerPackCreator/commit/f3115c9c5757cb3f74ec2b15b3683ab226abe623))
* Mirror release on GitLab.com after tag mirror ([d08845d](https://git.griefed.de/Griefed/ServerPackCreator/commit/d08845dc1676e165ceb724d9ea775c37e1f3211f))
* Post webhook message to Discord on new release ([2e3e25d](https://git.griefed.de/Griefed/ServerPackCreator/commit/2e3e25dde5ce19d8be2a2e641e9829ce1733c528))
* Publish maven artifact on (pre)release. Add info about new additional mirror on Gitea.com ([cfde3e2](https://git.griefed.de/Griefed/ServerPackCreator/commit/cfde3e29075254aa8e214349f29149b128e93b9d))
* Revert changes to Docker release jobs. Prevent running if on gitlab.com ([7e6404e](https://git.griefed.de/Griefed/ServerPackCreator/commit/7e6404e9fc912a01674f4576a665115b67aa98e4))


### 🧪 Tests

* Add unit tests for UpdateChecker ([1b4b91a](https://git.griefed.de/Griefed/ServerPackCreator/commit/1b4b91ac48c33b26aa77863eaed993171c99a372))
* Print stacktrace in all gradle builds to allow for better debugging ([7b6e480](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b6e480c5e50f49843fadfdb6efcfbbdfeb8cc69))


### 🚀 Features

* Add tooltip to SPC log panel informing users about the upload buttons in the menu bar ([08a123d](https://git.griefed.de/Griefed/ServerPackCreator/commit/08a123daae1687d8e7f929ae078b91c444aa7c9b))
* Configurable schedules in webservice which clean up the database and filesystem of unwanted server packs and files. ([09ccbc1](https://git.griefed.de/Griefed/ServerPackCreator/commit/09ccbc14921946a022634c454a013f0adb1cac63))
* Replace crude self-made addon system with Pf4j and provide first entry points ([e591488](https://git.griefed.de/Griefed/ServerPackCreator/commit/e59148806a0d3550cc3a9b2b3e4318e186b71029))
* Set copyDirs to "lazy_mode" to lazily create a server pack from the whole modpack. This will copy everything, no exceptions. Thanks to [@kreezxil](https://git.griefed.de/kreezxil) for the idea. ([2d89bec](https://git.griefed.de/Griefed/ServerPackCreator/commit/2d89bec8de7574bc14e213ce2e575558f12c9537))


### 🛠 Fixes

* Ensure no empty entries make it into copyDirs or clientMods lists. Prevents accidental copying of the whole modpack into the server pack. Thanks to @Kreezxil for this improvement! ([5549930](https://git.griefed.de/Griefed/ServerPackCreator/commit/5549930966408fd219ab9f8a8e2dbaeaefcf3d57))
* Expanded fallback clientside modslist with 3dSkinLayers-,armorchroma-,Craftpresence-,medievalmusic-,MyServerIsCompatible- thanks to @TheButterbrotMan ([a2ac391](https://git.griefed.de/Griefed/ServerPackCreator/commit/a2ac391d7ca4664b8320be07671b669721dfa4b3))
* Expanded fallback modslist with yisthereautojump, ToastControl, torchoptimizer ([f1c4ba3](https://git.griefed.de/Griefed/ServerPackCreator/commit/f1c4ba31f0a6253064c990ccf9dd05dd77f47d55))


### Other

* Add improvement template. To be evaluated over time whether this is usefull ([218622b](https://git.griefed.de/Griefed/ServerPackCreator/commit/218622b7b091a7a90508449d1935afca3ff39a85))
* Added debug logging when a new entry to files or directories to exclude is made ([719bb85](https://git.griefed.de/Griefed/ServerPackCreator/commit/719bb85b3c060854955b02fb225ddc171ddf5d80))
* List addresses for Java documentation ([b90045b](https://git.griefed.de/Griefed/ServerPackCreator/commit/b90045b05878f455947e0fcf2e38149ebdce7c05))
* Some cleanups and TODOs ([da02619](https://git.griefed.de/Griefed/ServerPackCreator/commit/da0261950ac780dea53055c3c41b5b0f513628b0))
* **deps:** update dependency @quasar/app to v3.2.9 ([d61a461](https://git.griefed.de/Griefed/ServerPackCreator/commit/d61a4618eb3246b9bc96f19fbf0833f075af32a7))
* **deps:** update dependency @quasar/extras to v1.12.4 ([10b76e6](https://git.griefed.de/Griefed/ServerPackCreator/commit/10b76e68202bc00f55660f356b0471f018714b76))
* **deps:** update dependency axios to v0.25.0 ([3008f24](https://git.griefed.de/Griefed/ServerPackCreator/commit/3008f24ac04a5e50cf9cc94af7fffd70b85621f3))
* **deps:** update dependency quasar to v2.4.12 ([8c3ab82](https://git.griefed.de/Griefed/ServerPackCreator/commit/8c3ab82e8889276595ce89d7b1b4b64d1a37a0c8))
* **deps:** update dependency tsparticles to v1.39.1 ([d231885](https://git.griefed.de/Griefed/ServerPackCreator/commit/d231885bb4b569f1eba3eed492c22a653f9f72ae))
* **deps:** update dependency vue to v3.2.28 ([c2fb183](https://git.griefed.de/Griefed/ServerPackCreator/commit/c2fb1836712dd415ea61ba252d69f307f1924b63))
* **deps:** update dependency vue to v3.2.29 ([57246dd](https://git.griefed.de/Griefed/ServerPackCreator/commit/57246dda971532cd7eae1d09b904e47631fe250e))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.1 ([18a75a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/18a75a55d5782e4823fda59915bfedc7111d35af))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2.0.2 ([65f7d15](https://git.griefed.de/Griefed/ServerPackCreator/commit/65f7d1594cd6f9827b3c42cf59653623ee791b2e))
* **deps:** update spring boot to v2.6.3 ([6a12a17](https://git.griefed.de/Griefed/ServerPackCreator/commit/6a12a17c95763abf0bd8f85b32b6dedea82f9df9))
* **deps:** update typescript-eslint monorepo to v5.10.0 ([6cec6a6](https://git.griefed.de/Griefed/ServerPackCreator/commit/6cec6a6d662930906c608b00e85e84dfe262c12a))

## [3.0.0-alpha.17](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.16...3.0.0-alpha.17) (2022-01-18)


### 🦊 CI/CD

* Correctly write VERSION.txt ([6434be8](https://git.griefed.de/Griefed/ServerPackCreator/commit/6434be836fa19f5df05eb38980dbaf57938e4866))
* Fix release build... ([fe2f601](https://git.griefed.de/Griefed/ServerPackCreator/commit/fe2f6014802607e822ac0fde7facfb79a32233af))
* Only run tag and release generation on git.griefed.de ([8afea27](https://git.griefed.de/Griefed/ServerPackCreator/commit/8afea27163a985596c4d37102b6e7d366e640ba0))


### 🛠 Fixes

* Empty commit so a new alpha will be generated... ([c1b5698](https://git.griefed.de/Griefed/ServerPackCreator/commit/c1b5698a0aac863518244998c72a4f21ff4d604c))

## [3.0.0-alpha.16](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.15...3.0.0-alpha.16) (2022-01-18)


### 👀 Reverts

* Maybe another time ([f7ea248](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7ea248f50ef2dbbdc99fa4538c9561d35e96ea7))


### 🦊 CI/CD

* Ensure docker jobs only run on git.griefed.de ([e633a0b](https://git.griefed.de/Griefed/ServerPackCreator/commit/e633a0b59b1d937ef7752333434cd1733c05b105))
* God damn, would you please only run when I tell you to? ([c610692](https://git.griefed.de/Griefed/ServerPackCreator/commit/c6106922a1c04fa3cee17880dfd8b931e5b9f951))
* Only run docker related jobs on git.griefed.de ([17339f4](https://git.griefed.de/Griefed/ServerPackCreator/commit/17339f4d402b88ac6b358f0de6b2557d6df03122))
* Run dockerjobs differently when running on other GitLab instances. ([169733f](https://git.griefed.de/Griefed/ServerPackCreator/commit/169733f1b9aa7c6295b4074f0468dc51caa342be))
* Run dockerjobs differently when running on other GitLab instances. ([0385ba1](https://git.griefed.de/Griefed/ServerPackCreator/commit/0385ba139b783bf348dbd14b0f69bda587a0cb77))
* Run dockerjobs differently when running on other GitLab instances. ([bfcbd51](https://git.griefed.de/Griefed/ServerPackCreator/commit/bfcbd51ebd6f30331d82fbc53284a357f9d751aa))


### 🛠 Fixes

* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([c556baa](https://git.griefed.de/Griefed/ServerPackCreator/commit/c556baaac7fe41ec0a11958e868a1da5bf26b14f))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([b108b67](https://git.griefed.de/Griefed/ServerPackCreator/commit/b108b6773d99fa8747fca016c70479521b2d6b1a))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([6afdbb4](https://git.griefed.de/Griefed/ServerPackCreator/commit/6afdbb4eb04af7b53ba64603fcc6348610edd4af))
* Improve VersionChecker by correctly throwing NumberFormatExceptions which can then be caught in checkForUpdate. Return updates.log.info.none on throw. Read version from property instead. ([dd3ff6e](https://git.griefed.de/Griefed/ServerPackCreator/commit/dd3ff6ecf43a64ef29481007c700c74595b38229))

## [3.0.0-alpha.15](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.14...3.0.0-alpha.15) (2022-01-16)


### :scissors: Refactor

* Simplify log tabs to increase maintainability. Abstract classes rock! ([7fc3404](https://git.griefed.de/Griefed/ServerPackCreator/commit/7fc3404df9577c15493c6b98905792e0860c5ecd))
* Simplify server installation to increase maintainability ([7bec08a](https://git.griefed.de/Griefed/ServerPackCreator/commit/7bec08a7e774f2935d34933b95b4624677e27737))


### 📔 Docs

* Add missing throws ([4538f54](https://git.griefed.de/Griefed/ServerPackCreator/commit/4538f547b291d5b02619e3f366ab53fff63726e9))
* Don't include private methods in documentation ([719b4f2](https://git.griefed.de/Griefed/ServerPackCreator/commit/719b4f2e8aec75075fda349383a305cce8aebf1a))
* Write documentation for fabric-server-launch replace method ([7ab20eb](https://git.griefed.de/Griefed/ServerPackCreator/commit/7ab20eb47a2149271cf461dba0d0f0a0b1ad40d5))


### 🦊 CI/CD

* **deps-dev:** bump @types/node from 17.0.5 to 17.0.8 in /frontend ([ea1383c](https://git.griefed.de/Griefed/ServerPackCreator/commit/ea1383c2bcbc60b889d262778d89d75002c86cdc))
* **deps:** bump @quasar/extras from 1.12.2 to 1.12.3 in /frontend ([08590a7](https://git.griefed.de/Griefed/ServerPackCreator/commit/08590a7bc96ad03837081ecc8b4779c3a1696791))
* **deps:** bump JamesIves/github-pages-deploy-action ([49cd567](https://git.griefed.de/Griefed/ServerPackCreator/commit/49cd567d7b9d0a68611b5771778a97e309bc80e8))
* **deps:** bump quasar from 2.4.3 to 2.4.4 in /frontend ([904db5f](https://git.griefed.de/Griefed/ServerPackCreator/commit/904db5feb51353c8054b200c32a560106ac1e6ca))
* **deps:** bump tsparticles from 1.38.0 to 1.39.0 in /frontend ([75a3b00](https://git.griefed.de/Griefed/ServerPackCreator/commit/75a3b00e5119863a21f57207fbb1609a261ab2ee))


### 🚀 Features

* Check and notify on updates in logs, console and in GUI. Also replaced and update a couple of i18n keys. VersionChecker can be found at https://git.griefed.de/Griefed/VersionChecker ([64419a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/64419a203a0d26bb001f20de2f8ab0a732156f20))
* Display version in window title and print to logs ([201a64c](https://git.griefed.de/Griefed/ServerPackCreator/commit/201a64c32868b0d26800b50c55d1e39dd5daa464))
* If i18n localized string can not be found in local file, try JAR-resource. If locale is not en_us, get en_us localized string as fallback. Allow users to write their own locales, languages and translations. ([802eb0c](https://git.griefed.de/Griefed/ServerPackCreator/commit/802eb0c5a4aa06b90d71bb570864bcda613bc55c))
* replace fabric-server-launch.jar with improved Fabric Server Launcher, if it is available for the specified Minecraft and Fabric versions.Thanks to @TheButterbrotMan for the idea! ([befdaf7](https://git.griefed.de/Griefed/ServerPackCreator/commit/befdaf7ea4265af9b3a0398f58a43bab3f19525f))


### 🛠 Fixes

* Last fallback in case no value can be found for a given key. ([53817d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/53817d4b8672359ff4a5b244c127afc581881436))
* Prevent unlikely, but possible, overwriting of properties file with wrong content from i18n initialization ([3675b09](https://git.griefed.de/Griefed/ServerPackCreator/commit/3675b0934253c5d03457cd64b6ca96825e0ee063))
* Whoops ([2c1841c](https://git.griefed.de/Griefed/ServerPackCreator/commit/2c1841ca18856ba0d398641d52923f8537135c71))


### Other

* Changelog from alpha branch ([c0e9383](https://git.griefed.de/Griefed/ServerPackCreator/commit/c0e93837a8751a3dcf06818953bf6e9ceea8e918))
* List all places where ServerPackCreator is available at ([cb12edc](https://git.griefed.de/Griefed/ServerPackCreator/commit/cb12edce4e26271d271344d90b7421c3118b3ee2))
* TODO ([085c831](https://git.griefed.de/Griefed/ServerPackCreator/commit/085c83132b54693e05bab5d01eb77666ea5642ec))
* **deps:** update dependency eslint-plugin-vue to v8.3.0 ([61e2eb4](https://git.griefed.de/Griefed/ServerPackCreator/commit/61e2eb47a22615bc23ef5040546ababeb8ca7a22))
* **deps:** update dependency quasar to v2.4.9 ([467b615](https://git.griefed.de/Griefed/ServerPackCreator/commit/467b6153cd2284a17815f8eee025dd88caed3c13))
* **deps:** update typescript-eslint monorepo to v5.9.1 ([a766e2a](https://git.griefed.de/Griefed/ServerPackCreator/commit/a766e2a3cc33e4002f1bc38c97c997a6f24be9d2))

## [3.0.0-alpha.14](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.13...3.0.0-alpha.14) (2022-01-09)


### :scissors: Refactor

* Be more specific with not found language key ([129877b](https://git.griefed.de/Griefed/ServerPackCreator/commit/129877bebe2691663cd7dc962b2bfd73f7dae796))
* Create empty serverpackcreator.properties. Makes manual migrations by users more unlikely while at the same time reducing risk of users breaking SPC with misconfigurations ([98c9a70](https://git.griefed.de/Griefed/ServerPackCreator/commit/98c9a70f6cd7deed6a0705f8589cc964824d765b))
* Move helper and utility methods to separate classes. Reorganize code. More and improved unit tests. Add a little info text to start scripts for Minecraft 1.17+ as well as print of Java version ([e41e97c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e41e97c1e31dd05aba19b5b429491d013401020a))
* Move script creation to separate methods and refactor write.write() to increase readability and maintainability. Also, move info regarding EULA agreement. ([65121a2](https://git.griefed.de/Griefed/ServerPackCreator/commit/65121a2a8e7adaac47c25e2b08498b7b6cbb61d7))
* Rearrange some fields ([4592b70](https://git.griefed.de/Griefed/ServerPackCreator/commit/4592b7041a130204a8847e775cc077ab8c64c498))
* rename applicationProperties field ([533c850](https://git.griefed.de/Griefed/ServerPackCreator/commit/533c850300e6dfa17fa6607bc2ae738e45a22b78))
* Rename applicationProperties field ([781e1cd](https://git.griefed.de/Griefed/ServerPackCreator/commit/781e1cdedfc303f933bea618b72a404e258b5027))
* Rework checkConfiguration to provide more ways of checking a given configuration. Require checks to run before passing to run(...). ([a3ecd11](https://git.griefed.de/Griefed/ServerPackCreator/commit/a3ecd11c58cf044c58d1f39c0b62bc30a729e189))


### 🦊 CI/CD

* **deps-dev:** bump @types/node from 17.0.5 to 17.0.7 in /frontend ([9d66fc3](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d66fc3c153118d8e6555b4093d58574b6729fa1))
* **deps-dev:** bump eslint from 8.5.0 to 8.6.0 in /frontend ([9698f98](https://git.griefed.de/Griefed/ServerPackCreator/commit/9698f98650490b0126467cfadf0ee7320ccd180a))
* **deps:** bump com.github.ben-manes.versions from 0.39.0 to 0.40.0 ([778e5e6](https://git.griefed.de/Griefed/ServerPackCreator/commit/778e5e6ff9a25c3af7853b771dda0b940cf3013b))
* **deps:** bump com.github.ben-manes.versions from 0.40.0 to 0.41.0 ([855c6e0](https://git.griefed.de/Griefed/ServerPackCreator/commit/855c6e0a44232119c99ad028135083d817c98698))
* **deps:** bump core-js from 3.20.1 to 3.20.2 in /frontend ([f9c1068](https://git.griefed.de/Griefed/ServerPackCreator/commit/f9c10686b424e460fd1fefaa92e8230b637bb189))
* **deps:** bump log4j-api from 2.17.0 to 2.17.1 ([f243a62](https://git.griefed.de/Griefed/ServerPackCreator/commit/f243a626a7f8b956703807a83d12696a84a4b898))
* **deps:** bump log4j-core from 2.17.0 to 2.17.1 ([1e579d2](https://git.griefed.de/Griefed/ServerPackCreator/commit/1e579d2c9a4b75327cb42f44c7e9b549edae614e))
* **deps:** bump log4j-jul from 2.17.0 to 2.17.1 ([7c10e41](https://git.griefed.de/Griefed/ServerPackCreator/commit/7c10e41c2085471c78849b08f1230089d170273b))
* **deps:** bump log4j-slf4j-impl from 2.17.0 to 2.17.1 ([303e2da](https://git.griefed.de/Griefed/ServerPackCreator/commit/303e2dad816660947384df1f10ea69fbba27b7f5))
* **deps:** bump log4j-web from 2.17.0 to 2.17.1 ([7a2ba8a](https://git.griefed.de/Griefed/ServerPackCreator/commit/7a2ba8ad49e1fe16d7733b8189fb5034a1cb0fe0))
* **deps:** bump tsparticles from 1.37.6 to 1.38.0 in /frontend ([d17900b](https://git.griefed.de/Griefed/ServerPackCreator/commit/d17900b81fb766bf6984c844e3ca3bd609194767))


### 🧪 Tests

* Fix some paths and configs so tests don't fail because of Layer 8 ([8270c82](https://git.griefed.de/Griefed/ServerPackCreator/commit/8270c82a6cb32ed7415b680e7f38bd81462bf2c7))


### 🚀 Features

* If given languagekey can not be found, use en_us from resources as fallback ([5802636](https://git.griefed.de/Griefed/ServerPackCreator/commit/5802636a612c4a49878f68b827e1115895062a95))


### 🛠 Fixes

* Copy lang-files if running as .exe ([c7c1415](https://git.griefed.de/Griefed/ServerPackCreator/commit/c7c1415ecdc4e30e9743f378e70e25b3b7545977))
* Deactivate CurseForge related code until custom implementation of CurseForgeAPI with CurseForge-provided API token is implemented and provided ([8c9bbff](https://git.griefed.de/Griefed/ServerPackCreator/commit/8c9bbff55d50a660ed0b673152a2b61c84845aae))
* Fix reverseOrderList not actually reliably reversing a list ([bbfdea5](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbfdea53b9d6668f35f2635a295f042a45beade5))
* Fix scheduling to not run every second or minute ([9e87689](https://git.griefed.de/Griefed/ServerPackCreator/commit/9e87689c0dad05569bc74f7aba1bb687602c8bd4))
* Only copy file from JAR-file if it is not found on local filesystem. ([09e271e](https://git.griefed.de/Griefed/ServerPackCreator/commit/09e271e4a8c6e0d202fd4a1db175087c8c9f9966))


### Other

* Add moreoverlays- to list of fallback modlist ([e990661](https://git.griefed.de/Griefed/ServerPackCreator/commit/e9906612dd5b583c505f0eb0d4b5b5cb7fd769b2))
* Add moveoverlays- to list of fallback modslist ([64ead40](https://git.griefed.de/Griefed/ServerPackCreator/commit/64ead409e5ffb156da1d9b3ed8103f722483e3e2))
* **deps:** update dependency core-js to v3.20.2 ([b4bd45e](https://git.griefed.de/Griefed/ServerPackCreator/commit/b4bd45e7ef3b140f4941fb9e93f6fce8ac390394))
* **deps:** update dependency eslint to v8.6.0 ([2e6ab21](https://git.griefed.de/Griefed/ServerPackCreator/commit/2e6ab21ee3ba1ff0649b4442e9edd3d8a1cb9b02))
* **deps:** update dependency quasar to v2.4.3 ([c3ff9b2](https://git.griefed.de/Griefed/ServerPackCreator/commit/c3ff9b2e55f4cedf6346d53a4395fcea633f2967))
* **deps:** update jamesives/github-pages-deploy-action action to v4.2.0 ([20a6b82](https://git.griefed.de/Griefed/ServerPackCreator/commit/20a6b828e163b949dc29f534241bb3dc98ccb923))
* **deps:** update plugin com.github.ben-manes.versions to v0.40.0 ([55d37b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/55d37b1f93623c823c788a9ee970a00a4cd961a2))
* **deps:** update plugin com.github.ben-manes.versions to v0.41.0 ([28989fd](https://git.griefed.de/Griefed/ServerPackCreator/commit/28989fdbd7aa57b6b036d91082694b047d266e4e))
* **deps:** update registry.gitlab.com/haynes/jacoco2cobertura docker tag to v1.0.8 ([8df16d5](https://git.griefed.de/Griefed/ServerPackCreator/commit/8df16d58cbd755361e7b1354841cbc5a4d43e3eb))
* **deps:** update typescript-eslint monorepo to v5.9.0 ([7b705a4](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b705a4f8dab2c8055629078208b89ea4c264b46))

## [3.0.0-alpha.13](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.12...3.0.0-alpha.13) (2021-12-30)


### 🦊 CI/CD

* Update Gradle to 7.3.3 ([541122b](https://git.griefed.de/Griefed/ServerPackCreator/commit/541122b0dded68e62878065bea3ea47aee55d1f5))
* **deps-dev:** bump @quasar/app from 3.2.5 to 3.2.6 in /frontend ([c53aeac](https://git.griefed.de/Griefed/ServerPackCreator/commit/c53aeac47f2b3fe0621e4abce2b89b3daf58e4d8))
* **deps-dev:** bump @types/node from 16.11.14 to 17.0.2 in /frontend ([d8109a5](https://git.griefed.de/Griefed/ServerPackCreator/commit/d8109a55fd012cc8e376d47e46ee768040174b28))
* **deps-dev:** bump @types/node from 17.0.2 to 17.0.5 in /frontend ([0ae1140](https://git.griefed.de/Griefed/ServerPackCreator/commit/0ae11401030687941c00f0bf5f4696c6af4ec036))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([579714d](https://git.griefed.de/Griefed/ServerPackCreator/commit/579714df6f96a30796293f37dec76bc04273d647))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([94c6af4](https://git.griefed.de/Griefed/ServerPackCreator/commit/94c6af47d56f3606fdd142697ecd05527fa9adaf))
* **deps:** bump core-js from 3.20.0 to 3.20.1 in /frontend ([bbad029](https://git.griefed.de/Griefed/ServerPackCreator/commit/bbad02947f1ad5462c46b418bb7d2d6c55bb3038))
* **deps:** bump quasar from 2.3.4 to 2.4.2 in /frontend ([bd3051c](https://git.griefed.de/Griefed/ServerPackCreator/commit/bd3051c18690a09609b10ece95bf0500f73036c1))
* **deps:** bump tsparticles from 1.37.5 to 1.37.6 in /frontend ([7ab7a69](https://git.griefed.de/Griefed/ServerPackCreator/commit/7ab7a69446f71d8dd827a3b152cc54987946a88d))
* Allow failure of dependency check and coverage jobs ([f8bb3d1](https://git.griefed.de/Griefed/ServerPackCreator/commit/f8bb3d1e82989d5639152d204c18aae642f6ff19))
* Allow running of Gradle and Docker test in parallel, to speed up pipeline completion. Move variables and services into global variable ([187a966](https://git.griefed.de/Griefed/ServerPackCreator/commit/187a9668d91fcc2ed8b809c86e6c8edc54db6f97))
* Change branch separator in dependabot config ([3b08ff8](https://git.griefed.de/Griefed/ServerPackCreator/commit/3b08ff8e9169990d4c502a5cc1ecd86c3ca96a8d))
* Cleanup GitLab CI and Dockerfile. Remove spotbug. ([017ebed](https://git.griefed.de/Griefed/ServerPackCreator/commit/017ebed289b10b88e473ef18651c01cc7acee13b))
* Create jacoco coverage report for coverage visualization in GitLab ([5da842f](https://git.griefed.de/Griefed/ServerPackCreator/commit/5da842f5415fbc16e43d51dd6195a4bd53ad22f3))
* Exclude libraries folder from test workflow artifacts ([c796115](https://git.griefed.de/Griefed/ServerPackCreator/commit/c7961153fdb212f68360e06b4a9d04a50222b518))
* Fix branch acquisition for GitHub Docker test ([063215f](https://git.griefed.de/Griefed/ServerPackCreator/commit/063215f65b7dbe9cd55b10ccac65de59b67c5cf4))
* Remove unnecessary environment cleaning ([67e1029](https://git.griefed.de/Griefed/ServerPackCreator/commit/67e1029e1cb12632d9cbe70c37466be84385721d))
* Split tests in GitHub workflow into separate jobs ([58fd4b3](https://git.griefed.de/Griefed/ServerPackCreator/commit/58fd4b3758aea9fc029bf70929fef9f5d2f9cddd))
* Upload artifacts of GitHub actions ([b4e41e4](https://git.griefed.de/Griefed/ServerPackCreator/commit/b4e41e458435b591a3fee54f7d38fbe2bb66feb4))


### 🧪 Tests

* Disable CurseForge related tests ([b28c97c](https://git.griefed.de/Griefed/ServerPackCreator/commit/b28c97c9ccd3602fa266def9df1ff010cae4e68b))
* Fix test failing due to missing, recently added, clientside-only mod ([1eaa966](https://git.griefed.de/Griefed/ServerPackCreator/commit/1eaa966468cc74f0ed2aab63cdc3dc006df082e0))
* Try and fix ArtemisConfigTest and SpringBootTests for spotbugs job ([67817a1](https://git.griefed.de/Griefed/ServerPackCreator/commit/67817a1e1b24742f9cac1930f44a8908272330e2))
* Try and fix ArtemisConfigTest and SpringBootTests for spotbugs job ([29c870f](https://git.griefed.de/Griefed/ServerPackCreator/commit/29c870fec68e75df7da3d8dba978a6f6688642b2))
* Try and fix ArtemisConfigTest for spotbugs job ([c665bf5](https://git.griefed.de/Griefed/ServerPackCreator/commit/c665bf5fd23d4fe56c249c3d4b3f1a22ebd5c3b5))


### 🚀 Features

* Allow users to edit language-definitions in the lang-directory. ([e2b5cca](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2b5ccaef8834ab3a9154d7208a5e6ff90a2b14b))


### 🛠 Fixes

* Always load classpath serverpackcreator.properties first, then loac local filesystem serverpackcreator.properties. Ensures defaults are always present and available to be overwritten and never empty. ([f91c8da](https://git.griefed.de/Griefed/ServerPackCreator/commit/f91c8da02116c5271eda0d02b4a394d2ed267ae2))
* Correctly initialize variable in start-scripts. Correctly pass OTHERARGS in batch-scripts. ([26f6dfd](https://git.griefed.de/Griefed/ServerPackCreator/commit/26f6dfdd24fb24c27755699edaa3b79bf89ae698))
* Explicitly define log4j and force any dependency using it to use the secure version ([f0c1946](https://git.griefed.de/Griefed/ServerPackCreator/commit/f0c19465ba0daf6c6d8ce090913a24e3ab8d820c))
* Re-add nogui parameter for fabric scripts. Apparently that is needed. Local tests proved it is not. My Little Fabric: Servers Are Magic ([6381c3b](https://git.griefed.de/Griefed/ServerPackCreator/commit/6381c3b1fc741ee684740db6d9fb5d7ccfb8f4d1))


### Other

* Add CraftPresence to fallbacklist of clientside-only mods (Reported by Law on Discord) ([88150ab](https://git.griefed.de/Griefed/ServerPackCreator/commit/88150ab82f654eba1d5be27566f3b74fea5d2b66))
* Change order of input so users don't confuse the log-section to be related to the webservice. ([e352d12](https://git.griefed.de/Griefed/ServerPackCreator/commit/e352d120603e6810a3a3ed0b3e46b021db4ca5a0))
* **deps:** update dependency @babel/eslint-parser to v7.16.5 ([d90ef33](https://git.griefed.de/Griefed/ServerPackCreator/commit/d90ef333df1d80fde46189faebe288f53f211427))
* **deps:** update dependency @quasar/app to v3.2.6 ([292d4f5](https://git.griefed.de/Griefed/ServerPackCreator/commit/292d4f5d8b2c048aa6ed28b18e0bdf0eaa4de79c))
* **deps:** update dependency @types/node to v16.11.14 ([76baa87](https://git.griefed.de/Griefed/ServerPackCreator/commit/76baa87cb160827729922b4cd11a407cf523fb9c))
* **deps:** update dependency @types/node to v16.11.15 ([7b8dd46](https://git.griefed.de/Griefed/ServerPackCreator/commit/7b8dd46df3819ab64778b033403ee30b59ee0a7b))
* **deps:** update dependency core-js to v3.20.0 ([809855a](https://git.griefed.de/Griefed/ServerPackCreator/commit/809855a1defa480ee9869c3bf3124474e0a8c34f))
* **deps:** update dependency core-js to v3.20.1 ([cde9246](https://git.griefed.de/Griefed/ServerPackCreator/commit/cde9246b792470bfc4e9308bb32bea2ae3bb8ada))
* **deps:** update dependency eslint to v8.5.0 ([6f7c5c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/6f7c5c24b8cb8a68427836331b1b2e758fdfeaa8))
* **deps:** update dependency gradle to v7.3.2 ([69019b9](https://git.griefed.de/Griefed/ServerPackCreator/commit/69019b97c3e2f4c38ae1a6eb4b8913a095986714))
* **deps:** update dependency org.apache.logging.log4j:log4j-api to v2.17.1 ([01c8a80](https://git.griefed.de/Griefed/ServerPackCreator/commit/01c8a80de9499ea377bf03eff6eaac1b73f8efb9))
* **deps:** update dependency org.apache.logging.log4j:log4j-core to v2.17.1 ([7cbd208](https://git.griefed.de/Griefed/ServerPackCreator/commit/7cbd208142e559d57c37f12ccc5a738a2f682bc1))
* **deps:** update dependency org.apache.logging.log4j:log4j-jul to v2.17.1 ([48cf50d](https://git.griefed.de/Griefed/ServerPackCreator/commit/48cf50df5230f399c93f8abf25d7aff5f3500697))
* **deps:** update dependency org.apache.logging.log4j:log4j-slf4j-impl to v2.17.1 ([de850ff](https://git.griefed.de/Griefed/ServerPackCreator/commit/de850ff6bb2c9600be0a06b06f84fe594c190427))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.17.0 ([9ab5fc7](https://git.griefed.de/Griefed/ServerPackCreator/commit/9ab5fc7e189765d9a42dabb66274870e06ecd409))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.17.1 ([32af395](https://git.griefed.de/Griefed/ServerPackCreator/commit/32af395878dfe45ebfed0e0dbbcd77f104418558))
* **deps:** update dependency quasar to v2.4.2 ([28ec385](https://git.griefed.de/Griefed/ServerPackCreator/commit/28ec3853f08d5e16110a1d95e1a2f95add7fc164))
* **deps:** update dependency tsparticles to v1.37.6 ([e69e81a](https://git.griefed.de/Griefed/ServerPackCreator/commit/e69e81a4263706ed969f4f7f1454dc550ee6659c))
* **deps:** update dependency tsparticles to v1.38.0 ([fa498bc](https://git.griefed.de/Griefed/ServerPackCreator/commit/fa498bc7fd1df3067a2d12e6c227c35635848a46))
* **deps:** update jamesives/github-pages-deploy-action action to v4.1.8 ([1d4a7f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/1d4a7f7e3c389abdc1513050327b6018848441ff))
* **deps:** update spring boot to v2.6.2 ([b6e4850](https://git.griefed.de/Griefed/ServerPackCreator/commit/b6e4850ff8ebe5f18e5472563bb3782cfd1ea0a9))
* **deps:** update typescript-eslint monorepo to v5.7.0 ([e6b01d8](https://git.griefed.de/Griefed/ServerPackCreator/commit/e6b01d858d2b9e25656fdbe07904b840242d2003))
* **deps:** update typescript-eslint monorepo to v5.8.0 ([1f29f23](https://git.griefed.de/Griefed/ServerPackCreator/commit/1f29f236d19653487b791576c76cfee8c58e1e88))
* **deps:** update typescript-eslint monorepo to v5.8.1 ([ded0c7b](https://git.griefed.de/Griefed/ServerPackCreator/commit/ded0c7b39e9d48a06b7b6fc87537670e0a430f69))

## [3.0.0-alpha.12](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.11...3.0.0-alpha.12) (2021-12-13)


### :scissors: Refactor

* Allow closing of notification if status is already exists ([a25e6f7](https://git.griefed.de/Griefed/ServerPackCreator/commit/a25e6f7b191a08e35f8b83d5911e9ac8bc9c11c8))
* Change banner being displayed when running as webservice ([75899d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/75899d4d211647acf9de589007bfeaa88664cf23))


### 🦊 CI/CD

* Update gradle to 7.3.1 ([88c1330](https://git.griefed.de/Griefed/ServerPackCreator/commit/88c133060f88303a6e734275c01704bb8ec4f782))
* **deps-dev:** bump @quasar/app from 3.2.3 to 3.2.5 in /frontend ([4d2092b](https://git.griefed.de/Griefed/ServerPackCreator/commit/4d2092bb73fe18589b5e150deebf7844c01c2198))
* **deps-dev:** bump @types/node from 16.11.10 to 16.11.11 in /frontend ([043414e](https://git.griefed.de/Griefed/ServerPackCreator/commit/043414ebed40dadf28ddb888276c1d8ca47835e5))
* **deps-dev:** bump @types/node from 16.11.10 to 16.11.12 in /frontend ([ddd4424](https://git.griefed.de/Griefed/ServerPackCreator/commit/ddd44242048537fe22b3c2c3344a82884507c5c7))
* **deps-dev:** bump @typescript-eslint/eslint-plugin in /frontend ([55b5ba5](https://git.griefed.de/Griefed/ServerPackCreator/commit/55b5ba52f6b9c9377e730fd8d3ff0b25be52eca6))
* **deps-dev:** bump @typescript-eslint/parser in /frontend ([d06b4cd](https://git.griefed.de/Griefed/ServerPackCreator/commit/d06b4cd7559dea9eefd686a189ceb22ece256320))
* **deps:** bump @quasar/extras from 1.12.1 to 1.12.2 in /frontend ([bf9f871](https://git.griefed.de/Griefed/ServerPackCreator/commit/bf9f871eb39c3a18e8f4c67bd44d5a1c4dfd68a5))
* **deps:** bump core-js from 3.19.1 to 3.19.3 in /frontend ([4864c13](https://git.griefed.de/Griefed/ServerPackCreator/commit/4864c13d9b2b7a7ffc979c54483803b54d445c44))
* **deps:** bump junit-platform-commons from 1.8.1 to 1.8.2 ([d8483f1](https://git.griefed.de/Griefed/ServerPackCreator/commit/d8483f1d5767c0ec62d7bb12cfa4d4f476d3d62f))
* **deps:** bump quasar from 2.3.3 to 2.3.4 in /frontend ([373fdb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/373fdb340ca949d61f51374f7e03685e18708f82))
* **deps:** bump spring-boot-devtools from 2.6.0 to 2.6.1 ([a51e28e](https://git.griefed.de/Griefed/ServerPackCreator/commit/a51e28e646c115cce8f784458e08a4d95197edb4))
* **deps:** bump spring-boot-starter-test from 2.5.6 to 2.6.1 ([0f39852](https://git.griefed.de/Griefed/ServerPackCreator/commit/0f398524acfbb7c01b9a404430ee35eba351ee84))
* **deps:** bump spring-boot-starter-validation from 2.5.6 to 2.6.1 ([1473032](https://git.griefed.de/Griefed/ServerPackCreator/commit/14730327dae5a9d81df7caf3ce0e4d1a5f4fda88))
* **deps:** bump spring-boot-starter-web from 2.5.6 to 2.6.1 ([9d7ab8b](https://git.griefed.de/Griefed/ServerPackCreator/commit/9d7ab8b0f024d1cc0f6f88ea5aa68ecbffbb545f))
* **deps:** bump vue from 3.2.22 to 3.2.24 in /frontend ([62d687a](https://git.griefed.de/Griefed/ServerPackCreator/commit/62d687a0ffccc248c4ae0f89168ce18e3e47fabf))
* Set loglevel in SAST to debug ([fc5341f](https://git.griefed.de/Griefed/ServerPackCreator/commit/fc5341fea92bba0e2f650644e543c53a1d8c48c4))
* Try and fix Renovate warning ([893a581](https://git.griefed.de/Griefed/ServerPackCreator/commit/893a581c9d6a2935cdd80aa9df03f1717b3a425c))


### 🧪 Tests

* Set ddl-auto to create ([8e00f7e](https://git.griefed.de/Griefed/ServerPackCreator/commit/8e00f7e4990ad42ceb2e7a23bbdcaf075e26a261))
* Try and fix error because of missing database ([81d4f80](https://git.griefed.de/Griefed/ServerPackCreator/commit/81d4f8045ed06bd83525edbb4980dde8afa2881e))


### 🛠 Fixes

* -Dlog4j2.formatMsgNoLookups=true to prevent log4j2 vulnerability, added via customziable OTHERARGS in scripts. Move java path to JAVA for customizability (is that a word?) ([ff7dc52](https://git.griefed.de/Griefed/ServerPackCreator/commit/ff7dc52f23ed5e1e2abc92f33c9964225c083dcb))
* Correctly compare user input with variable in start.bat, resulting in creation of eula.txt if user enters "I agree" ([224cbb3](https://git.griefed.de/Griefed/ServerPackCreator/commit/224cbb3874830c7ff2cce83e403eb27470244aa8))
* Implement log4j exploit protection ([971fc4f](https://git.griefed.de/Griefed/ServerPackCreator/commit/971fc4fe7cfa362b48197d0222373a884c517f92))
* More hardening against CVE-2021-44228 ([eaa4668](https://git.griefed.de/Griefed/ServerPackCreator/commit/eaa4668731ded0145f47d810d65dbf703306009c))
* Use inverted order array for Fabric version when checking for Fabric version upon config load and therefore set correct Fabric version. ([de5cdcf](https://git.griefed.de/Griefed/ServerPackCreator/commit/de5cdcf0b1bf1f81c812bd685dc41a5ef74b7f09))
* **deps:** update dependency core-js to v3.19.3 ([f7a3140](https://git.griefed.de/Griefed/ServerPackCreator/commit/f7a314067fae89105aed95cae95188c827812c2f))
* **deps:** update dependency vue to v3.2.24 ([4b44938](https://git.griefed.de/Griefed/ServerPackCreator/commit/4b4493876f4476c6ecc90497bbc621e1aa1b545a))
* **deps:** update dependency vue to v3.2.26 ([be664e8](https://git.griefed.de/Griefed/ServerPackCreator/commit/be664e84c506155157e879f42b50426b0f8e7800))


### Other

* Update git index for gradlew so execution is always allowed ([057b6c2](https://git.griefed.de/Griefed/ServerPackCreator/commit/057b6c2e1514f5287596e4004cbbb790f34c1d12))
* **deps:** pin dependencies ([f6d8822](https://git.griefed.de/Griefed/ServerPackCreator/commit/f6d88221cb966c739365f352b2a9c6bb660eeb17))
* **deps:** update dependency @quasar/app to v3.2.4 ([e33df47](https://git.griefed.de/Griefed/ServerPackCreator/commit/e33df47cb0182788a995f55a7a1852f3d75919d4))
* **deps:** update dependency eslint to v8.4.1 ([2db3a36](https://git.griefed.de/Griefed/ServerPackCreator/commit/2db3a36ae3f5f12e1263fbb91d5a7984804c58a8))
* **deps:** update dependency eslint-plugin-vue to v8.2.0 ([e2df4dc](https://git.griefed.de/Griefed/ServerPackCreator/commit/e2df4dc25fae418fdf495d7c2d4acbf1cae68567))
* **deps:** update dependency gradle to v7.3.1 ([6964401](https://git.griefed.de/Griefed/ServerPackCreator/commit/6964401eddbfadb265bb15fbd8a1aacfc5e6ea50))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.15.0 ([1018e10](https://git.griefed.de/Griefed/ServerPackCreator/commit/1018e106aeffa8439e0f5dd2aeaa2d1e6bf68639))
* **deps:** update dependency org.apache.logging.log4j:log4j-web to v2.16.0 ([5632772](https://git.griefed.de/Griefed/ServerPackCreator/commit/5632772a0785567f1ed0142c845120aac98a30bb))
* **deps:** update griefed/baseimage-ubuntu-jdk-8 docker tag to v2 ([e3d9f7c](https://git.griefed.de/Griefed/ServerPackCreator/commit/e3d9f7c907c39619fe0c36504472722140a03cec))
* **deps:** update npm to v8 ([f446f11](https://git.griefed.de/Griefed/ServerPackCreator/commit/f446f1167dc950ea509c4117743a380957c0502e))
* **deps:** update spring boot to v2.6.1 ([d0d9f03](https://git.griefed.de/Griefed/ServerPackCreator/commit/d0d9f03b447443fb08da3b4ee517ee85cf08e29d))
* **deps:** update typescript-eslint monorepo to v5.6.0 ([c27b3b0](https://git.griefed.de/Griefed/ServerPackCreator/commit/c27b3b04ddb8219fd0c80f5e850c243bcb540634))

## [3.0.0-alpha.11](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.10...3.0.0-alpha.11) (2021-11-30)


### :scissors: Refactor

* Improve error handling and reporting ([77985b6](https://git.griefed.de/Griefed/ServerPackCreator/commit/77985b6f23fa95d388b349a016d090a480a869aa))
* Print server-icon and server.properties paths. Re-organize method in CreateServerPackTab to ensure GUI becomes responsible again if the generation of a server pack fails. ([e42b3b1](https://git.griefed.de/Griefed/ServerPackCreator/commit/e42b3b1aaac9845bbf053d49705b8cb044eb3c07))
* Rename fields still referencing old serverPackCreatorProperties to applicationProperties ([e1b7c62](https://git.griefed.de/Griefed/ServerPackCreator/commit/e1b7c6254a710f5f2a3436090782f079d1f433e4))
* Rework error redirect. ([85543ac](https://git.griefed.de/Griefed/ServerPackCreator/commit/85543ac9f6fc7385c0e634fa60c78cec4e289c01))
* Set rate at which tailers tail to 100ms, instead of 2000ms. ([ba4624f](https://git.griefed.de/Griefed/ServerPackCreator/commit/ba4624f9116f248ac5953e90c1209b50990c4155))
* Sort by downloads, descending ([2f6f6d4](https://git.griefed.de/Griefed/ServerPackCreator/commit/2f6f6d4578b2bf5429fd2b85291850b292766e50))
* When a requested server pack already exists, offer a download to the user. ([39dc626](https://git.griefed.de/Griefed/ServerPackCreator/commit/39dc6268e8ebd1048c0e19c0a479bd731c8d1e98))


### 🚀 Features

* Write errors encountered during config check to logs/console. When using GUI, show a message with the encountered Errors. Helps with figuring out whats wrong with a given configuration. ([e1b0c62](https://git.griefed.de/Griefed/ServerPackCreator/commit/e1b0c6269cbd545993854786a07a949f4a379c45))


### 🛠 Fixes

* Allow selection of bmp, jpg and jpeg as server-icons. Java correctly converts them to png for use as server-icons. If the image could not be loaded, print an error message. ([d2c1ac7](https://git.griefed.de/Griefed/ServerPackCreator/commit/d2c1ac78fbf97c003e10f49af281437b95891865))
* Set downloads and votes to zero upon generation of server pack ([be84232](https://git.griefed.de/Griefed/ServerPackCreator/commit/be8423251d82aea1a7639cd30bbaf9d0f06397df))


### Other

* Udpate versions ([eecc90a](https://git.griefed.de/Griefed/ServerPackCreator/commit/eecc90a88dfa2d787a256e341dc422a124a22cab))

## [3.0.0-alpha.10](https://git.griefed.de/Griefed/ServerPackCreator/compare/3.0.0-alpha.9...3.0.0-alpha.10) (2021-11-27)


### 🦊 CI/CD

* Remove changelog generation of GitHub releases as I copy and paste the changelog from GitLab anyway ([25cdb26](https://git.griefed.de/Griefed/ServerPackCreator/commit/25cdb26d97fd5427e152615a9d10749d6039765f))


### 🛠 Fixes

* Instead of using an external applications.properties for customizing, use our serverpackcreator.properties...which exists anyway! ([3794896](https://git.griefed.de/Griefed/ServerPackCreator/commit/3794896971e775d4f4d37aee7d340cc0510d8024))

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
