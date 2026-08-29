package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.GrindVerdict
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
}
