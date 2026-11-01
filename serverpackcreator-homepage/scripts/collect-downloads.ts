/**
 * Collect the download totals at BUILD time and write them into the bundle.
 *
 *   npm run collect-downloads
 *
 * Counting in Node rather than in the browser is what makes the figure complete and free: there is
 * no CORS here, so git.griefed.de is reachable (it sends no Access-Control-Allow-Origin, Forgejo's
 * default); no visitor pays the ~20 requests and several MB a full sweep costs; and nothing is
 * charged against GitHub's 60-requests-per-hour-per-IP anonymous ceiling. The page renders this
 * number immediately and refreshes it in the background at most once a day.
 *
 * FAIL-SOFT ON PURPOSE: a forge being unreachable must not fail a site build, so this always writes
 * a file and always exits 0. A null total is the page's signal to fall back to a live count.
 */
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { countAllDownloads } from '../src/services/downloads/index'
import {
  SERVERPACKCREATOR_HOSTS, SERVERPACKCREATOR_OWNER, SERVERPACKCREATOR_REPOSITORY
} from '../src/config/repositories'

const OUTPUT = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../src/generated/download-counts.json'
)

async function main(): Promise<void> {
  let payload

  try {
    const report = await countAllDownloads(SERVERPACKCREATOR_OWNER, SERVERPACKCREATOR_REPOSITORY, {
      hosts: SERVERPACKCREATOR_HOSTS,
      // Raises GitHub's anonymous ceiling when CI provides one; absent is fine.
      ...(process.env.GITHUB_TOKEN ? { token: process.env.GITHUB_TOKEN } : {})
    })

    for (const platform of report.platforms) {
      console.log(
        `collect-downloads: ${platform.platform.padEnd(8)} ${String(platform.total).padStart(9)}` +
        `  (${platform.releases} releases, ${platform.assets} assets)`
      )
    }
    for (const failure of report.failures) {
      console.warn(`collect-downloads: ${failure.platform} unavailable -- ${failure.reason}`)
    }

    // Every forge failing is indistinguishable from a repository nobody downloads, so say "unknown"
    // rather than claim zero.
    const total = report.platforms.length > 0 ? report.total : null
    payload = { generatedAt: report.countedAt, total, platforms: report.platforms }
    console.log(`collect-downloads: total ${total ?? 'unknown'}`)
  } catch (reason) {
    console.warn(`collect-downloads: counting failed entirely -- ${String(reason)}`)
    payload = { generatedAt: new Date().toISOString(), total: null, platforms: [] }
  }

  await mkdir(dirname(OUTPUT), { recursive: true })
  await writeFile(OUTPUT, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  console.log(`collect-downloads: wrote ${OUTPUT}`)
}

await main()
