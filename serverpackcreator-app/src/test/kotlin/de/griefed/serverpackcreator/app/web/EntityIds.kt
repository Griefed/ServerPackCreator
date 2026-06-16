package de.griefed.serverpackcreator.app.web

/**
 * Assigns the Mongo-ID backing-field of the given web-entity via reflection. The web-entities
 * expose their IDs with private setters (normally assigned by Spring Data's PersistenceCreator),
 * so tests use this helper to construct realistic entities without a database.
 */
fun assignEntityId(entity: Any, idValue: String) {
    val idField = entity.javaClass.getDeclaredField("id")
    idField.isAccessible = true
    idField.set(entity, idValue)
}
