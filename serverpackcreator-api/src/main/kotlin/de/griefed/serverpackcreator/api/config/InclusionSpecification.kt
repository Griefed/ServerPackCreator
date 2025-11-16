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
package de.griefed.serverpackcreator.api.config

/**
 * A FileInclusionSpecification is at minimum a source-declaration from which files and directories will be included
 * in the server pack to be generated.
 *
 * Optionally, the following values can be specified:
 * * Destination
 * * Inclusion-filter (regex)
 * * Exclusion-filter (regex)
 *
 * * When a destination is specified, then every file and directory acquired from the source will be copied to the
 * specified destination in the server pack, whereas otherwise the source would be copied to a destination in the server
 * pack matching the name of the source.
 * * Inclusion filters help to further limit the files and directories to be included from the specified source. Any file
 * and directory matching this filter will be included.
 * * Exclusion filters help to further limit the files and directories to be included from the specified source. Any file
 * and directory matching this filter will NOT be included.
 *
 * @author Griefed
 */
class InclusionSpecification(
    var source: String,
    var destination: String? = null,
    var inclusionFilter: String? = null,
    var exclusionFilter: String? = null
) {

    /**
     * Whether this inclusion-spec has an Inclusion-Filter present.
     */
    fun hasInclusionFilter(): Boolean {
        return inclusionFilter != null && inclusionFilter!!.isNotBlank()
    }

    /**
     * Whether this inclusion-spec has an Exclusion-Filter present.
     */
    fun hasExclusionFilter(): Boolean {
        return exclusionFilter != null && exclusionFilter!!.isNotBlank()
    }

    /**
     * Whether this inclusion-spec has a destination in the server-pack-to-be set.
     */
    fun hasDestination(): Boolean {
        return !destination.isNullOrBlank()
    }

    /**
     * Whether this inclusion-spec is a global filter to be applied to every inclusion.
     * A global filter requires the source to be empty and/or blank and an inclusion- or exclusion-filter to be set.
     */
    fun isGlobalFilter(): Boolean {
        return (source.isBlank() && hasInclusionFilter()) || (source.isBlank() && hasExclusionFilter())
    }

    /**
     * Get this inclusion-spec as a hashmap.
     */
    fun asHashMap(): HashMap<String,String> {
        val map = HashMap<String,String>()
        map["source"] = source
        map["destination"] = destination ?: ""
        map["inclusionFilter"] = inclusionFilter ?: ""
        map["exclusionFilter"] = exclusionFilter ?: ""
        return map
    }
}