package de.griefed.serverpackcreator.api.utilities.common

/**
 * Utility-class to compare to versions of semantic-format against each other and determine whether one is newer than
 * the other.
 *
 * @author Griefed
 */
class SemanticVersionComparator {
    companion object {
        /**
         * Compare the given new version against the given current version, depending on comparison type `EQUAL`,
         * `NEW`, or `EQUAL_OR_NEW`.
         * Checks are performed with the semantic release-formatting, e.g. 1.2.3, 2.3.4, 6.6.6
         * @author Griefed
         * @param currentVersion Current version to check against `newVersion`.
         * @param newVersion New version to check against `currentVersion`.
         * @param comparison Comparison level. Either `EQUAL`, `NEW`, or `EQUAL_OR_NEW`.
         * @return Boolean. Returns `true` if the new version is indeed newer than the current version. Otherwise
         * `false`.
         * @throws NumberFormatException Thrown if the passed `currentVersion` or `newVersion` can not be
         * parsed into integers.
         */
        fun compareSemantics(currentVersion: String, newVersion: String, comparison: Comparison = Comparison.NEW): Boolean {
            if (newVersion == "no_release") {
                return false
            }
            val newMajor: Int
            val newMinor: Int
            val newPatch: Int
            val currentMajor: Int
            val currentMinor: Int
            val currentPatch: Int
            val currentSemantics = getSemantics(currentVersion)
            val newSemantics = getSemantics(newVersion)
            currentMajor = currentSemantics[0]
            currentMinor = currentSemantics[1]
            currentPatch = currentSemantics[2]
            newMajor = newSemantics[0]
            newMinor = newSemantics[1]
            newPatch = newSemantics[2]
            return when (comparison) {
                Comparison.EQUAL -> checkEqual(
                    newMajor,
                    newMinor,
                    newPatch,
                    currentMajor,
                    currentMinor,
                    currentPatch
                )

                Comparison.NEW -> checkNew(
                    newMajor,
                    newMinor,
                    newPatch,
                    currentMajor,
                    currentMinor,
                    currentPatch
                )

                Comparison.EQUAL_OR_NEW -> checkNewOrEqual(
                    newMajor,
                    newMinor,
                    newPatch,
                    currentMajor,
                    currentMinor,
                    currentPatch
                )
            }
        }

        /**
         * Acquire the version numbers of a semantic-formatted version.
         * @author Griefed
         * @param version The version from which to acquire the version numbers.
         * @return List of version numbers. Major index 0, minor index 1, patch index 2.
         */
        private fun getSemantics(version: String): List<Int> {
            val semantics: MutableList<Int> = ArrayList(3)
            val versionNumbers = version.split("\\.".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            semantics.add(0, versionNumbers[0].toInt())
            semantics.add(1, versionNumbers[1].toInt())

            if (versionNumbers.size >= 3) {
                if (versionNumbers[2].contains("-")) {
                    semantics.add(
                        2,
                        versionNumbers[2].split("-".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0].toInt()
                    )
                } else {
                    semantics.add(2, versionNumbers[2].toInt())
                }
            } else {
                // Versions like 1.21, 2.34 etc. don't have patch. Treat patch as 0
                semantics.add(2, 0)
            }
            return semantics
        }

        /**
         * Compare two versions against each other and determine whether the new version is a newer semantic release version.
         * @author Griefed
         * @param newMajor The new versions major number.
         * @param newMinor The new versions minor number.
         * @param newPatch The new versions patch number.
         * @param currentMajor The old versions major number.
         * @param currentMinor The old versions minor number.
         * @param currentPatch The old versions patch number.
         * @return True if a new version was determined.
         */
        private fun checkNew(
            newMajor: Int,
            newMinor: Int,
            newPatch: Int,
            currentMajor: Int,
            currentMinor: Int,
            currentPatch: Int
        ): Boolean {
            return if (newMajor > currentMajor) {
                // new major update
                true
            } else if (newMajor == currentMajor && newMinor > currentMinor) {
                // new minor update
                true

                // new patch update if true
            } else newMajor == currentMajor && newMinor == currentMinor && newPatch > currentPatch
        }

        /**
         * Compare two versions against each other and determine whether the new version is a newer or the same semantic
         * release version.
         * @author Griefed
         * @param newMajor The new versions major number.
         * @param newMinor The new versions minor number.
         * @param newPatch The new versions patch number.
         * @param currentMajor The old versions major number.
         * @param currentMinor The old versions minor number.
         * @param currentPatch The old versions patch number.
         * @return True if a new or equal version was determined.
         */
        private fun checkNewOrEqual(
            newMajor: Int,
            newMinor: Int,
            newPatch: Int,
            currentMajor: Int,
            currentMinor: Int,
            currentPatch: Int
        ): Boolean {
            return if (newMajor == currentMajor && newMinor == currentMinor && newPatch == currentPatch) {
                // equal version
                true
            } else if (newMajor > currentMajor) {
                // new major update
                true
            } else if (newMajor == currentMajor && newMinor > currentMinor) {
                // new minor update
                true
                // new patch update if true
            } else newMajor == currentMajor && newMinor == currentMinor && newPatch > currentPatch
        }

        /**
         * Compare two versions against each other and determine whether they are the same semantic versions.
         * @author Griefed
         * @param newMajor The new versions major number.
         * @param newMinor The new versions minor number.
         * @param newPatch The new versions patch number.
         * @param currentMajor The old versions major number.
         * @param currentMinor The old versions minor number.
         * @param currentPatch The old versions patch number.
         * @return True if the versions are the same.
         */
        private fun checkEqual(
            newMajor: Int,
            newMinor: Int,
            newPatch: Int,
            currentMajor: Int,
            currentMinor: Int,
            currentPatch: Int
        ): Boolean {
            return newMajor == currentMajor && newMinor == currentMinor && newPatch == currentPatch
        }
    }
}