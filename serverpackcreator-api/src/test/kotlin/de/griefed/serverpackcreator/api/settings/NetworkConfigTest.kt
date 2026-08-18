package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Tests for [NetworkConfig], the HTTP-timeout settings-group. Pins the shipped defaults, that
 * store-entries override them, and that a negative value falls back rather than reaching
 * `setConnectTimeout`, which throws on negatives.
 */
internal class NetworkConfigTest {

    /**
     * Pins the shipped defaults, so a bump is a deliberate edit rather than a silent drift. The
     * download read-timeout must stay the largest: it is the one applied to installers and mod jars.
     */
    @Test
    fun defaultsAreTheShippedFallbacks() {
        val networkConfig = NetworkConfig(PropertyStore())
        Assertions.assertEquals(5_000, networkConfig.connectTimeout)
        Assertions.assertEquals(15_000, networkConfig.readTimeout)
        Assertions.assertEquals(60_000, networkConfig.downloadReadTimeout)
        Assertions.assertTrue(
            networkConfig.downloadReadTimeout > networkConfig.readTimeout,
            "Downloads must tolerate longer stalls than metadata calls"
        )
    }

    /**
     * Pins that a stored value wins over the fallback, which is what makes these settings operator-
     * and embedder-tunable at all.
     */
    @Test
    fun storedValuesOverrideTheDefaults() {
        val store = PropertyStore()
        val networkConfig = NetworkConfig(store)
        store.define(NetworkConfig.CONNECT_TIMEOUT_KEY, "1500")
        store.define(NetworkConfig.READ_TIMEOUT_KEY, "2500")
        store.define(NetworkConfig.DOWNLOAD_READ_TIMEOUT_KEY, "3500")
        Assertions.assertEquals(1500, networkConfig.connectTimeout)
        Assertions.assertEquals(2500, networkConfig.readTimeout)
        Assertions.assertEquals(3500, networkConfig.downloadReadTimeout)
    }

    /**
     * Pins that zero is passed through rather than sanitised away. It is the JDK's "wait forever",
     * i.e. the behaviour SPC had before these settings existed, and is kept as the documented
     * escape hatch for anyone who needs it back.
     */
    @Test
    fun zeroIsHonouredAsTheNoTimeoutEscapeHatch() {
        val store = PropertyStore()
        val networkConfig = NetworkConfig(store)
        store.define(NetworkConfig.CONNECT_TIMEOUT_KEY, "0")
        store.define(NetworkConfig.READ_TIMEOUT_KEY, "0")
        Assertions.assertEquals(0, networkConfig.connectTimeout)
        Assertions.assertEquals(0, networkConfig.readTimeout)
    }

    /**
     * Pins that a negative value falls back instead of being handed to the connection.
     * `HttpURLConnection.setConnectTimeout` throws `IllegalArgumentException` on negatives, so
     * passing one through would turn a properties-file typo into a crash on every network call.
     */
    @Test
    fun negativeValuesFallBackRatherThanReachingTheConnection() {
        val store = PropertyStore()
        val networkConfig = NetworkConfig(store)
        store.define(NetworkConfig.CONNECT_TIMEOUT_KEY, "-1")
        store.define(NetworkConfig.READ_TIMEOUT_KEY, "-9000")
        store.define(NetworkConfig.DOWNLOAD_READ_TIMEOUT_KEY, "-1")
        Assertions.assertEquals(networkConfig.fallbackConnectTimeout, networkConfig.connectTimeout)
        Assertions.assertEquals(networkConfig.fallbackReadTimeout, networkConfig.readTimeout)
        Assertions.assertEquals(networkConfig.fallbackDownloadReadTimeout, networkConfig.downloadReadTimeout)
    }

    /**
     * Pins that an unparseable value falls back, too — [PropertyStore.getInt] resets the key, and the
     * group must not surface the garbage in between.
     */
    @Test
    fun unparseableValuesFallBack() {
        val store = PropertyStore()
        val networkConfig = NetworkConfig(store)
        store.define(NetworkConfig.READ_TIMEOUT_KEY, "not-a-number")
        Assertions.assertEquals(networkConfig.fallbackReadTimeout, networkConfig.readTimeout)
    }

    /**
     * Pins that assigning a timeout writes it back to the store, so it survives a save/load cycle.
     */
    @Test
    fun settersWriteBackToTheStore() {
        val store = PropertyStore()
        val networkConfig = NetworkConfig(store)
        networkConfig.connectTimeout = 4321
        Assertions.assertEquals("4321", store.properties.getProperty(NetworkConfig.CONNECT_TIMEOUT_KEY))
    }
}
