/** Binary size units, smallest first. Indexed by how many times the value has been divided by 1024. */
const BINARY_UNITS = ['B', 'KiB', 'MiB', 'GiB', 'TiB'] as const

/**
 * Render a byte count the way a person reads it.
 *
 * The API reports archive sizes in bytes, so the unit belongs here rather than in a template. It
 * previously reported truncated mebibytes as an integer, which made every pack under 1 MiB arrive as
 * `0` — and both pack tables gated their download button on `size > 0`, so those packs could not be
 * downloaded at all. Anything that is not a finite number renders as `unknown` rather than
 * `NaN undefined`, because rows written before the correction, and rows with no size at all, are both
 * still out there.
 */
export function formatBytes(bytes: number | null | undefined): string {
  if (typeof bytes !== 'number' || !Number.isFinite(bytes) || bytes < 0) {
    return 'unknown'
  }
  let value = bytes
  let unitIndex = 0
  while (value >= 1024 && unitIndex < BINARY_UNITS.length - 1) {
    value /= 1024
    unitIndex++
  }
  // One decimal, but never a bare ".0" — "1 KiB" reads better than "1.0 KiB".
  const rendered = unitIndex === 0 ? String(value) : value.toFixed(1).replace(/\.0$/, '')
  return `${rendered} ${BINARY_UNITS[unitIndex]}`
}
