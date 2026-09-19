package de.griefed.serverpackcreator.app.cli.commands

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.HomeDirectoryPreference
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Pins that an interactive prompt leaves `System.in` open.
 *
 * Every prompting command wraps `System.in` in a `Scanner` and used to close it when done. A
 * `Scanner` closes its underlying source, and `System.in` cannot be reopened — so the first prompt
 * in a `-cli` session permanently closed file descriptor 0. JLine's POSIX terminal holds that very
 * descriptor as its pty slave and calls `ioctl(TIOCGWINSZ)` on it to size the display, so the next
 * prompt died with `Error calling ioctl(TIOCGWINSZ): return code is -1` wrapped in a
 * `java.io.IOError` — an *Error*, which the shell's `catch (e: Exception)` misses, so the whole
 * session ended after a single command.
 *
 * The assertion is on the stream rather than on a terminal, because the damage is done the moment
 * the descriptor closes; what JLine then fails to do is downstream of that and needs a real tty to
 * observe. Note a `ByteArrayInputStream` would be useless here: its `close()` is a no-op, so the
 * defect would be invisible. Hence the recording wrapper.
 */
internal class InteractivePromptStdinTest {

    /**
     * An `InputStream` that remembers whether anything closed it, delegating everything else. The
     * point of the class: `System.in`'s real closure is not observable after the fact, so the test
     * has to watch the call rather than the consequence.
     */
    private class CloseRecordingInputStream(private val delegate: InputStream) : InputStream() {
        /** Whether `close()` was called on this stream. */
        var closed = false
            private set

        override fun read(): Int = delegate.read()

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate.read(buffer, offset, length)

        override fun available(): Int = delegate.available()

        override fun close() {
            closed = true
            delegate.close()
        }
    }

    /**
     * Feed [input] to [prompt] as `System.in` and report whether [prompt] closed it, restoring the
     * real `System.in` either way so one failing case cannot take the rest of the suite with it.
     */
    private fun stdinClosedBy(input: String, prompt: () -> Unit): Boolean {
        val original = System.`in`
        val recording = CloseRecordingInputStream(ByteArrayInputStream(input.toByteArray()))
        try {
            System.setIn(recording)
            prompt()
        } finally {
            System.setIn(original)
        }
        return recording.closed
    }

    /**
     * `cgen`'s modpack-directory prompt — the one in the reported session, and the only one of the
     * four whose prompt loop is reachable without going through the command's side effects.
     */
    @Test
    fun cgenPromptLeavesSystemInOpen(@TempDir tempDir: File) {
        val modpack = File(tempDir, "modpack")
        modpack.mkdirs()
        var answered: File? = null
        val closed = stdinClosedBy("${modpack.absolutePath}\n") {
            answered = ConfigGenCommand(mockk()).requestModpackDir()
        }
        Assertions.assertEquals(modpack, answered, "the prompt must still read the path it was given")
        Assertions.assertFalse(closed, "cgen's prompt must not close System.in — the shell reads from it next")
    }

    /**
     * `homeDir`'s prompt. The stored preference is saved and restored around it, because the command
     * persists what it is told and this test's answer must not outlive it.
     */
    @Test
    fun homeDirPromptLeavesSystemInOpen(@TempDir tempDir: File) {
        val previous = HomeDirectoryPreference.stored()
        try {
            val closed = stdinClosedBy("${tempDir.absolutePath}\n") {
                HomeDirCommand().run()
            }
            Assertions.assertFalse(closed, "homeDir's prompt must not close System.in")
        } finally {
            previous?.let { HomeDirectoryPreference.store(it) }
        }
    }

    /**
     * `lang`'s prompt. The answer has to be a locale the shipped `Translations` offers, or the command
     * loops until the fed input runs out; `en_GB` is the one this project is written in. It used to be
     * `en` here, because the command listed `en_GB` and accepted only `en`.
     */
    @Test
    fun langPromptLeavesSystemInOpen() {
        val closed = stdinClosedBy("en_GB\n") {
            LanguageCommand(mockk(relaxed = true)).run()
        }
        Assertions.assertFalse(closed, "lang's prompt must not close System.in")
    }

    /**
     * `run withSpecificConfig`'s prompt, which is reached by omitting the `-c` option. The config it
     * is pointed at is real but empty, so the generation behind it is a no-op against a relaxed mock
     * rather than an actual server pack build.
     */
    @Test
    fun runWithSpecificConfigPromptLeavesSystemInOpen(@TempDir tempDir: File) {
        val config = File(tempDir, "serverpackcreator.conf")
        config.writeText("modpackDir = \"\"")
        val closed = stdinClosedBy("${config.absolutePath}\n") {
            RunHeadlessCommand(mockk<ApiWrapper>(relaxed = true)).withSpecificConfig(null, null)
        }
        Assertions.assertFalse(closed, "the config-file prompt must not close System.in")
    }
}
