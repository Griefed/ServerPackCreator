package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.GrindVerdict
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

internal class StoreWriteBenchTest {
    private fun verdict(i: Int) = GrindVerdict(
        "Modrinth", "mod$i", "https://modrinth.com/mod/mod$i", "Forge", "mod$i-",
        Confidence.LOW, "Forge 47.2.0 / Minecraft 1.20.1 -> SURVIVED (exit 137)", Instant.parse("2026-08-29T00:00:00Z")
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
                out.write(""""confidence":"${v.confidence}","detail":"${v.detail}","verifiedAt":"2026-08-29T00:00:00Z"}""")
            }
            out.write("]")
        }
    }

    @Test
    fun bench(@TempDir dir: File) {
        Assumptions.assumeTrue(
            System.getenv("SPC_GRINDER_BENCH") != null,
            "set SPC_GRINDER_BENCH=1 to run the store write benchmark"
        )
        for (size in listOf(1_000, 10_000, 100_000)) {
            val file = File(dir, "verdicts-$size.json")
            seed(file, size)
            val store = JsonVerdictStore(file)
            check(store.all().size == size) { "seeded $size but loaded ${store.all().size}" }
            repeat(3) { store.record(verdict(size + it)) }
            val started = System.nanoTime()
            repeat(10) { store.record(verdict(size + 100 + it)) }
            val perRecord = (System.nanoTime() - started) / 10 / 1_000_000.0
            println("[bench] %6d rows -> %7.1f ms per record(), file %6.1f MiB".format(size, perRecord, file.length() / 1024.0 / 1024.0))
        }
    }

    /**
     * Splits `persist()`'s cost three ways, to answer B35's actual question: is dropping the pretty-printer
     * enough on its own, or does the store need an append-log?
     */
    @Test
    fun benchComponents(@TempDir dir: File) {
        Assumptions.assumeTrue(
            System.getenv("SPC_GRINDER_BENCH") != null,
            "set SPC_GRINDER_BENCH=1 to run the store write benchmark"
        )
        val mapper = jacksonObjectMapper().findAndRegisterModules()
        for (size in listOf(10_000, 100_000)) {
            val rows = (0 until size).map { verdict(it) }
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
