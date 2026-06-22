/**
 * Minimal shapes of the ServerPackCreator web-API payloads the frontend actually reads. These are
 * not exhaustive mirrors of the backend DTOs — they cover the fields the components and templates
 * consume, so single-file-component templates type-check under strict mode.
 */

/** A single configuration/generation error attached to a history event, rendered by ErrorsCard. */
export interface ErrorItem {
  id: number
  error: string
}
