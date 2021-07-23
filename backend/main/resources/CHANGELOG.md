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
