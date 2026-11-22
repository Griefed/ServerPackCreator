package de.griefed.common.gradle

import com.github.jk1.license.License
import com.github.jk1.license.ModuleData
import com.github.jk1.license.PomData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.time.Duration

/**
 * Pins that the licence renderer's one network read is bounded.
 *
 * `generateLicenseReport` is on the build path, and a POM licence URL whose host accepts the connection
 * and then says nothing would otherwise stall the whole build: the JDK's default connect- and
 * read-timeout is infinite. The `catch` around the call cannot help, because a hang never throws.
 *
 * `buildSrc` is a separate build and cannot reach `-api`'s `URL.timedConnection`, which is the same
 * reason that function exists — so this is its own bounded read, and this is what keeps it bounded.
 */
internal class LicenseAgreementRendererTest {

    /**
     * Both timeouts must reach the connection. Asserted on a `file:` URL so no server is needed —
     * `URLConnection` accepts and reports the values whatever the protocol.
     */
    @Test
    fun theLicenceReadCarriesBothTimeouts(@TempDir tempDir: File) {
        val licence = File(tempDir, "LICENSE.txt").apply { writeText("Apache License 2.0") }
        val connection = licence.toURI().toURL().openConnection()

        val body = LicenseAgreementRenderer().readBounded(connection)

        Assertions.assertEquals("Apache License 2.0", body, "the licence text must still be read")
        Assertions.assertTrue(
            connection.connectTimeout > 0,
            "a connect timeout of 0 is the JDK's 'wait for ever', which is what stalls the build"
        )
        Assertions.assertTrue(
            connection.readTimeout > 0,
            "a read timeout of 0 is the JDK's 'wait for ever' — the host that accepts and says nothing"
        )
    }

    /**
     * The renderer's licence fetch must go through [LicenseAgreementRenderer.readBounded]. Driven through the
     * dependency entry the report actually renders, against a loopback host that accepts the connection and
     * never answers: bounded, the entry comes back with the licence name and URL; unbounded, it never comes back.
     */
    @Test
    fun aLicenceHostThatNeverAnswersCannotStallTheReport() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { silentHost ->
            val licenceUrl = "http://127.0.0.1:${silentHost.localPort}/LICENSE.txt"
            val pom = PomData().apply {
                projectUrl = "https://example.org/silent"
                licenses = setOf(License("Silent Licence", licenceUrl))
            }
            val module = ModuleData("org.example", "silent", "1.0").apply { poms = setOf(pom) }

            val entry = Assertions.assertTimeoutPreemptively<String>(Duration.ofSeconds(30), {
                LicenseAgreementRenderer().getDependencyLicense(module)
            }, "a licence host that says nothing must not stall the report")

            Assertions.assertTrue(entry.contains("URL: $licenceUrl"), "the entry still names the licence URL")
        }
    }
}
