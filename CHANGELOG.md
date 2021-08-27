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
