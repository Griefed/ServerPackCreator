/* Copyright (C) 2025 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
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
         * Check a list of versions following the semantic-scheme and find the latest one.
         *
         * @author Griefed
         *
         * @throws NumberFormatException If any of the versions in the list can not be parsed correctly, possibly due to
         * not being in the expected semantic-format, or because they contain letters.
         */
        @Suppress("unused")
        fun findNewestVersion(versions: List<String>): String {
            var newest = versions[0]
            for (version in versions) {
                if (!versions.all { compareSemantics(newest, version) }) {
                    newest = version
                }
            }
            return newest
        }

        /**
         * Check a list of versions following the semantic-scheme and find the oldest one.
         *
         * @author Griefed
         *
         * @throws NumberFormatException If any of the versions in the list can not be parsed correctly, possibly due to
         * not being in the expected semantic-format, or because they contain letters.
         */
        @Suppress("unused")
        fun findOldestVersion(versions: List<String>): String {
            var oldest = versions[0]
            for (version in versions) {
                if (versions.all { compareSemantics(oldest, version, Comparison.EQUAL_OR_NEW) }) {
                    oldest = version
                }
            }
            return oldest
        }

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