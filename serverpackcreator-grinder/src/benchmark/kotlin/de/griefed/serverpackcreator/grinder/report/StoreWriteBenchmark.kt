package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.GrindVerdict
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

/**
 * Measures what [JsonVerdictStore] costs per `record()`, and **asserts nothing about the timings** it
 * prints.
 *
 * That is deliberate, not an omission. This project pins I/O by request, read and open *counts* and
 * never by wall-clock, so a benchmark has no honest assertion to make about the numbers it produces —
 * and the behaviour these measurements motivated is already pinned, properly, by
 * [CoalescedVerdictWritesTest]: that a coalesced store does not rewrite per `record()`, that `flush()`
 * and `close()` write what is buffered, that the scheduled flusher fires, and that a write-through
 * store still persists immediately. Adding timing assertions here would duplicate that guard with a
 * flakier one.
 *
 * What it *does* assert is its own fixture. A benchmark measuring the wrong thing is worse than none,
 * so every run checks that the store really loaded the rows it was seeded with before timing anything.
 *
 * It is a **program, not a test** — a plain `main` in the `benchmark` source set, run by a `JavaExec`
 * task: `./gradlew :serverpackcreator-grinder:benchmark`. That is not stylistic. Measured on this build,
 * **every task of type `Test` is pulled into `check`**, by type and regardless of name, group, or whether
 * anything declares the dependency — so a JUnit benchmark ran on every `./gradlew build` no matter how it
 * was gated. A `JavaExec` is not, and needs no environment variable to stay out of the way.
 *
 * A bad fixture still stops it: the seeding checks throw rather than assert, because there is no
 * framework here to assert with, and a benchmark measuring the wrong thing is worse than none.
 *
 * @author Griefed
 */
internal object StoreWriteBenchmark {
    private fun verdict(i: Int) = GrindVerdict(
        "Modrinth", "mod$i", "https://modrinth.com/mod/mod$i", "Forge", "mod$i-",
        "Forge 47.2.0 / Minecraft 1.20.1 -> SURVIVED (exit 137)", Instant.parse("2026-08-29T00:00:00Z")
    )

    /** Seed the file directly: seeding through record() is itself O(n^2), which is the thing under test. */
    private fun seed(file: File, size: Int) {
        file.bufferedWriter().use { out ->
            out.write("[")
            (0 until size).forEach { i ->
                if (i > 0) out.write(",")
                val v = verdict(i)
                out.write("""{"platform":"${v.platform}","slug":"${v.slug}","projectUrl":"${v.projectUrl}",""")
                out.write(""""loader":"${v.loader}","suggestedEntry":"${v.suggestedEntry}",""")
                out.write(""""verdict":"${v.verdict}","detail":"${v.detail}","verifiedAt":"2026-08-29T00:00:00Z"}""")
            }
            out.write("]")
        }
    }

    /** Write-through cost: milliseconds per `record()` as the file grows, the number B35 started from. */
    fun measureWriteThroughCostPerRecord(dir: File) {
        for (size in listOf(1_000, 10_000, 100_000)) {
            val file = File(dir, "verdicts-$size.json")
            seed(file, size)
            val store = JsonVerdictStore(file)
            check(store.all().size == size) { "seeded $size rows, the store loaded ${store.all().size}" }
            repeat(3) { store.record(verdict(size + it)) }
            val started = System.nanoTime()
            repeat(10) { store.record(verdict(size + 100 + it)) }
            val perRecord = (System.nanoTime() - started) / 10 / 1_000_000.0
            println("[bench] %6d rows -> %7.1f ms per record(), file %6.1f MiB".format(size, perRecord, file.length() / 1024.0 / 1024.0))
        }
    }

    /** The same measurement with writes coalesced — B35's fix, against the write-through number above. */
    fun measureCoalescedCostPerRecord(dir: File) {
        for (size in listOf(1_000, 10_000, 100_000)) {
            val file = File(dir, "coalesced-$size.json")
            seed(file, size)
            JsonVerdictStore(file, flushInterval = Duration.ofSeconds(30)).use { store ->
                check(store.all().size == size) { "seeded $size rows, the store loaded ${store.all().size}" }
                repeat(3) { store.record(verdict(size + it)) }
                val started = System.nanoTime()
                repeat(10) { store.record(verdict(size + 100 + it)) }
                val perRecord = (System.nanoTime() - started) / 10 / 1_000.0
                println("[coalesced] %6d rows -> %8.1f us per record()".format(size, perRecord))
            }
        }
    }

    /**
     * Splits `persist()`'s cost three ways, to answer B35's actual question: is dropping the pretty-printer
     * enough on its own, or does the store need an append-log?
     */
    fun measurePersistComponents(dir: File) {
        val mapper = jacksonObjectMapper().findAndRegisterModules()
        for (size in listOf(10_000, 100_000)) {
            val rows = (0 until size).map { verdict(it) }
            check(rows.size == size) { "built ${rows.size} rows, about to time $size" }
            fun time(label: String, body: () -> Unit) {
                repeat(2) { body() }
                val started = System.nanoTime()
                repeat(5) { body() }
                println("[components] %6d rows  %-22s %7.1f ms".format(size, label, (System.nanoTime() - started) / 5 / 1_000_000.0))
            }
            val out = File(dir, "c-$size.json")
            time("sort only") { rows.sortedWith(compareBy({ it.slug }, { it.loader })) }
            time("pretty write") { mapper.writerWithDefaultPrettyPrinter().writeValue(out, rows) }
            println("      pretty file: %.1f MiB".format(out.length() / 1024.0 / 1024.0))
            time("compact write") { mapper.writeValue(out, rows) }
            println("      compact file: %.1f MiB".format(out.length() / 1024.0 / 1024.0))
            time("sort + pretty write") {
                mapper.writerWithDefaultPrettyPrinter().writeValue(out, rows.sortedWith(compareBy({ it.slug }, { it.loader })))
            }
        }
    }
}

/**
 * Run all three measurements against one throwaway directory, then delete it.
 *
 * The entry point the `benchmark` JavaExec task names. Anything thrown -- a mis-seeded fixture -- exits
 * non-zero and fails the task, which is the only way this can go red and the only way it should.
 */
fun main() {
    val dir = Files.createTempDirectory("spc-store-benchmark").toFile()
    try {
        StoreWriteBenchmark.measureWriteThroughCostPerRecord(dir)
        StoreWriteBenchmark.measureCoalescedCostPerRecord(dir)
        StoreWriteBenchmark.measurePersistComponents(dir)
    } finally {
        dir.deleteRecursively()
    }
}
