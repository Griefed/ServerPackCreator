package de.griefed.serverpackcreator.app.cli

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Pins the one prompt loop the commands share: that it re-asks rather than accepting a bad answer,
 * that it says what was wrong with the answer it refused, that a choice it offers can always be
 * chosen, and that it never closes the stream it reads from.
 */
internal class ConsolePromptTest {

    /**
     * An `InputStream` that remembers whether anything closed it. A `ByteArrayInputStream` cannot pin
     * the closing defect on its own, because its `close()` is a no-op and the call leaves no trace.
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

    /** The console output a prompt produced, so what the user was shown can be asserted. */
    private val shown = StringBuilder()

    /** A prompt answering from [answers], one per line, writing to [shown]. */
    private fun promptAnswering(vararg answers: String) =
        ConsolePrompt(ByteArrayInputStream(answers.joinToString("\n", postfix = "\n").toByteArray()), shown)

    /**
     * The loop re-asks until the answer names a real directory, and names the one it refused.
     */
    @Test
    fun readExistingDirectoryReAsksUntilTheAnswerIsADirectory(@TempDir tempDir: File) {
        val directory = File(tempDir, "modpack")
        directory.mkdirs()
        val missing = File(tempDir, "nope").absolutePath

        val answered = promptAnswering(missing, directory.absolutePath)
            .readExistingDirectory("Enter the full path to the modpack-directory.")

        Assertions.assertEquals(directory, answered)
        Assertions.assertTrue(shown.contains("Enter the full path to the modpack-directory."))
        Assertions.assertTrue(shown.contains("Directory '$missing' does not exist."), "must name what it refused")
        Assertions.assertEquals(2, shown.split("Path: ").size - 1, "must have asked twice")
    }

    /**
     * A directory is not a file, so the file prompt must refuse one rather than hand it back.
     */
    @Test
    fun readExistingFileReAsksUntilTheAnswerIsAFile(@TempDir tempDir: File) {
        val directory = File(tempDir, "adirectory")
        directory.mkdirs()
        val file = File(tempDir, "server pack.conf")
        file.writeText("modpackDir = \"x\"")

        val answered = promptAnswering(directory.absolutePath, file.absolutePath)
            .readExistingFile("Enter the full path to the config file.")

        Assertions.assertEquals(file, answered)
        Assertions.assertTrue(
            shown.contains("File '${directory.absolutePath}' does not exist."),
            "a directory must not satisfy a prompt asking for a file"
        )
    }

    /**
     * The guard that makes the `lang` defect unrepresentable: whatever the prompt lists, it accepts.
     */
    @Test
    fun readChoiceAcceptsExactlyWhatItDisplayed() {
        val choices = mapOf("en_GB" to "english", "pt_BR" to "portuguese")

        val chosen = promptAnswering("en_GB").readChoice("Choose one of the above.", "Language: ", choices)

        Assertions.assertEquals("english", chosen, "the value behind the displayed key must come back")
        Assertions.assertTrue(shown.contains("en_GB"), "must list the choices")
        Assertions.assertTrue(shown.contains("pt_BR"), "must list the choices")
        Assertions.assertFalse(shown.contains("Unsupported"), "what it displayed must not be refused")
    }

    /**
     * An answer that was never offered is refused by name, and the question is put again.
     */
    @Test
    fun readChoiceReAsksOnAnAnswerItNeverOffered() {
        val choices = mapOf("en_GB" to "english", "pt_BR" to "portuguese")

        val chosen = promptAnswering("en", "pt_BR").readChoice("Choose one of the above.", "Language: ", choices)

        Assertions.assertEquals("portuguese", chosen)
        Assertions.assertTrue(shown.contains("Unsupported choice en."), "must name the answer it refused")
        Assertions.assertEquals(2, shown.split("Language: ").size - 1, "must have asked twice")
    }

    /**
     * The whole reason this class exists in one copy: closing the stream kills the shell, so nothing
     * here may close it.
     */
    @Test
    fun theStreamIsNeverClosed(@TempDir tempDir: File) {
        val directory = File(tempDir, "modpack")
        directory.mkdirs()
        val recording = CloseRecordingInputStream(ByteArrayInputStream("${directory.absolutePath}\n".toByteArray()))

        ConsolePrompt(recording, shown).readExistingDirectory("Enter the full path to the modpack-directory.")

        Assertions.assertFalse(recording.closed, "closing this stream closes System.in, which ends the shell")
    }
}
