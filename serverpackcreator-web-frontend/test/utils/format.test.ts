import { describe, expect, it } from 'vitest'
import { formatBytes } from 'src/utils/format'

/**
 * The API reports archive sizes in bytes. It used to report truncated mebibytes as an integer, so
 * anything under 1 MiB arrived as 0 — and both tables hid their download button on `size > 0`,
 * which made small packs undownloadable. Formatting now happens here, where the unit is known.
 */
describe('formatBytes', () => {
  it('keeps small packs visible instead of rounding them to nothing', () => {
    expect(formatBytes(0)).toBe('0 B')
    expect(formatBytes(512)).toBe('512 B')
    expect(formatBytes(1048575)).toBe('1024 KiB')
  })

  it('scales through the binary units', () => {
    expect(formatBytes(1024)).toBe('1 KiB')
    expect(formatBytes(1048576)).toBe('1 MiB')
    expect(formatBytes(2200000)).toBe('2.1 MiB')
    expect(formatBytes(5 * 1024 * 1024 * 1024)).toBe('5 GiB')
  })

  it('survives the values a stale document can still hold', () => {
    // Rows written before the unit was corrected hold small mebibyte counts, and a null or absent
    // size must not render as "NaN undefined" in a table.
    expect(formatBytes(undefined)).toBe('unknown')
    expect(formatBytes(null)).toBe('unknown')
  })
})
