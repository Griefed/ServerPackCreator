# Contributing

## Building ServerPackCreator locally

Clone a branch of the repository:

`git clone -b $BRANCH https://github.com/Griefed/ServerPackCreator.git`

Where `$BRANCH` represents the branch you want to clone.

If you are on linux, run `chmod +x gradlew` first.

Build with:

`build --info --full-stacktrace`

The `Build All` task is configured to do everything automatically, from installing frontend dependencies, assembling the web-frontend, copying some files around, build and testing.

## Contributing via GitHub Forks

If you want to contribute to ServerPackCreator, then the following procedure **must** be adhered to:

1. Fork ServerPackCreator on GitHub
2. Switch to the develop branch. If the develop branch does not exist, create it from main.
3. Make your changes to the develop branch:
   1. Follow conventional commit messages. See **Commits**-section for more details. See `.releaserc.yml` for details. Example:
      - feat: Allow upload of modpack-export zip-archive to web-frontend
      - refactor: Use apache commons-io for copying, instead of Files
4. Open a PR on GitHub:
   1. PR title: Your Username - Branch type (e.g. feat) - Short description of your changes. Example:
      - Griefed - feat - Allow upload of modpack-export zip-archive to web-frontend
   2. PR description: A short but concise description of your PRs goal and/or purpose.
5. Done!

# Code

- **Config file:** If you want to contribute to SPC, please make sure you do not change the `serverpackcreator.conf`-file. Ideally, any version of SPC will work with any config file, as they all have the same content. Changing what's inside the `serverpackcreator.conf`-file may make versions incompatible to each other, but I want users to be able to simply download the newest version **without** having to migrate their config file or even worrying about such a thing.
Therefore, I ask that you do not touch the `serverpackcreator.conf`-file.

- **Variable names:** Please keep variable names verbose i.e. `thisStoresSomething` or `checkForStuff` or some such. Variables like `a` and `tmpA` make code harder to read.

- **Translating:** If you wish to contribute to translating ServerPackCreator, see `Adding a translation` in the [HELP](HELP.md)