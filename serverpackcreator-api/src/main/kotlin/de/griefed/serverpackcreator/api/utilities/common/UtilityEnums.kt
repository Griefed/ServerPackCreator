package de.griefed.serverpackcreator.api.utilities.common

enum class Comparison {
    /**
     * Used to determine whether two given versions are the same.
     */
    EQUAL,

    /**
     * Used to determine whether a given version is newer.
     */
    NEW,

    /**
     * Used to determine whether a given version is the same or newer.
     */
    EQUAL_OR_NEW
}

/**
 * File-type to use, identify and report configured Java versions with.
 *
 * @author Griefed
 */
enum class FileType {
    /**
     * A regular file.
     */
    FILE,

    /**
     * A regular directory.
     */
    DIRECTORY,

    /**
     * A Windows link.
     */
    LINK,

    /**
     * A UNIX symlink.
     */
    SYMLINK,

    /**
     * Not a valid file.
     */
    INVALID
}

/**
 * Filter-types by which to filter entries when walking through the files in a directory.
 * @author Griefed
 */
enum class FilterType {
    /**
     * Whether the string to check contains the given filter.
     */
    CONTAINS,

    /**
     * Whether the string to check ends with the given filter.
     */
    ENDS_WITH,

    /**
     * Whether the string to check starts with the given filter.
     */
    STARTS_WITH
}