package de.griefed.common.gradle

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

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
}
