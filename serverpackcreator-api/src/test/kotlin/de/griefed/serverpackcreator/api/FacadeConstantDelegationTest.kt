package de.griefed.serverpackcreator.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guards that the two published constants which survived Phase 1c/1d as facades keep *reading* the
 * value their extracted collaborator owns, instead of holding a second copy of the same literal.
 *
 * The assertions are on **identity**, deliberately. Comparing values would pass just as happily
 * against a re-introduced duplicate that happens to agree, which is exactly the state this guards
 * against: before the rewiring, `ServerPackHandler` and `ConfigurationHandler` each declared their
 * own copy of a literal that `ModListCompiler` / `ModpackZipInspector` also declared privately, with
 * nothing linking the two. Both agreed, so nothing failed — but the copy that generation actually
 * consults carried no documentation, while the documented copy was dead, so an edit aimed at the
 * documented one would have changed nothing at all.
 */
internal class FacadeConstantDelegationTest {
    private val api = ApiWrapper.api(File("build/resources/test/serverpackcreator.properties"))

    /** `ServerPackHandler.modFileEndings` must be the very list `ModListCompiler` walks the mods dir with. */
    @Test
    fun serverPackHandlerReadsTheModFileEndingsOwnedByModListCompiler() {
        val serverPackHandler = api.serverPackHandler
        Assertions.assertSame(
            serverPackHandler.modListCompiler.modFileEndings,
            serverPackHandler.modFileEndings,
            "ServerPackHandler.modFileEndings must delegate to ModListCompiler, not hold its own copy"
        )
        Assertions.assertEquals(listOf("jar", "disabled"), serverPackHandler.modFileEndings)
    }

    /** `ConfigurationHandler.zipCheck` must be the very regex `ModpackZipInspector` matches entries with. */
    @Test
    fun configurationHandlerReadsTheZipCheckOwnedByModpackZipInspector() {
        val configurationHandler = api.configurationHandler
        Assertions.assertSame(
            configurationHandler.zipInspector.zipCheck,
            configurationHandler.zipCheck,
            "ConfigurationHandler.zipCheck must delegate to ModpackZipInspector, not hold its own copy"
        )
        Assertions.assertEquals("^\\w+[/\\\\]$", configurationHandler.zipCheck.pattern)
    }
}
