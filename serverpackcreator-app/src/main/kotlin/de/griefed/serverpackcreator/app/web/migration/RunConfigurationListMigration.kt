/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.app.web.migration

import com.mongodb.DBRef
import org.bson.Document
import org.springframework.stereotype.Component

/**
 * Rewrites a stored run-configuration from the `@DBRef` list shape to embedded strings.
 *
 * `RunConfiguration.startArgs`, `clientMods` and `whitelistedMods` used to be `@DBRef` arrays pointing
 * at collections whose documents held nothing but their own `@MongoId`. So the rewrite is **join-free**:
 * a `DBRef`'s `$id` *is* the value, and `{$ref: "clientMod", $id: "OptiFine"}` becomes `"OptiFine"`
 * without the referenced collection being read at all. That is what makes this migration cheap, and
 * what makes it safe to run against a database whose `clientMod` collection has already been dropped.
 *
 * The transformation is deliberately separate from the component that applies it
 * ([RunConfigurationListMigrationRunner]), so it can be tested without a database.
 */
@Component
class RunConfigurationListMigration {

    companion object {
        /** The three fields that changed shape. */
        val MIGRATED_FIELDS = listOf("startArgs", "clientMods", "whitelistedMods")
    }

    /**
     * Whether [storedConfig] still holds any list element as a `DBRef`.
     *
     * False for a fresh install, for an already-migrated document, and for empty or absent lists — so
     * the runner can skip the write entirely rather than rewriting untouched documents on every startup.
     */
    fun needsRewrite(storedConfig: Document): Boolean =
        MIGRATED_FIELDS.any { field ->
            (storedConfig[field] as? List<*>)?.any { element -> element is DBRef } == true
        }

    /**
     * [storedConfig] with every `DBRef` list element replaced by its id, and everything else untouched.
     *
     * Element-wise rather than list-wise, so a half-migrated document — what an interrupted run leaves
     * behind — is completed instead of corrupted. Absent lists stay absent; an empty list stays empty.
     */
    fun rewrite(storedConfig: Document): Document {
        val rewritten = Document(storedConfig)
        for (field in MIGRATED_FIELDS) {
            val stored = rewritten[field] as? List<*> ?: continue
            rewritten[field] = stored.map { element ->
                when (element) {
                    is DBRef -> element.id.toString()
                    else -> element
                }
            }
        }
        return rewritten
    }
}
