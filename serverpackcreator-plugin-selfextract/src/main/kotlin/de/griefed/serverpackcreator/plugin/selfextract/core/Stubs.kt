/* Copyright (C) 2026 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.plugin.selfextract.core

/**
 * The two scripts that are written in front of the archive, and everything about them that is load
 * bearing.
 *
 * They are the chapter in `HELP.md` under *Fun Stuff → Self-extracting, self-contained script*, so a
 * user handed one of these artifacts can read that chapter to learn what they were handed. Keep the two
 * in step: a change here that is not reflected there leaves the documentation describing an artifact
 * nobody produces any more.
 *
 * @author Griefed
 */
internal object Stubs {

    /** Substituted with the byte at which the payload begins; see `SelfExtractingArchive.settleOffset`. */
    const val OFFSET_PLACEHOLDER = "SPC_ARCHIVE_OFFSET"

    /** Substituted with the pack's name, which is also the directory the server is installed into. */
    const val NAME_PLACEHOLDER = "SPC_PACK_NAME"

    /**
     * The Linux and macOS stub, in POSIX `sh`.
     *
     * `tail -c +N` rather than the `awk`-and-line-count the original recipe used: a byte offset is one
     * seek, where counting lines means scanning the whole payload, and gzip output contains newline
     * bytes so a payload could in principle carry the marker line and be found instead of it. It also
     * extracts straight into the destination rather than staging in `/tmp` and copying, which halves
     * the I/O and cannot run a small `/tmp` out of space on a multi-gigabyte pack.
     *
     * The `chmod` is not belt-and-braces: the archive carries the modes `TarGzWriter` decided, and this
     * repeats them at the one place a user would notice them missing. `exec` hands the terminal and the
     * signals to the server, so Ctrl+C reaches the JVM rather than a wrapper nobody is interested in.
     */
    fun shell(name: String): String = listOf(
        "#!/bin/sh",
        "# Self-extracting ServerPackCreator server pack.",
        "# Everything below the marker at the end of this file is a gzipped tar archive.",
        "set -eu",
        "OFFSET=$OFFSET_PLACEHOLDER",
        "NAME=$NAME_PLACEHOLDER",
        "DEST=\"\${SPC_TARGET:-\$HOME/mc-servers/\$NAME}\"",
        "if [ -e \"\$DEST\" ]; then",
        "  echo \"\$DEST already exists - refusing to overwrite it.\"",
        "  echo \"Set SPC_TARGET to install somewhere else, or move the old server pack away.\"",
        "  exit 1",
        "fi",
        "echo \"Extracting \$NAME to \$DEST\"",
        "mkdir -p \"\$DEST\"",
        "tail -c +\$OFFSET \"\$0\" | tar -xzf - -C \"\$DEST\"",
        "chmod 0755 \"\$DEST\"/start.* \"\$DEST\"/install_java.* 2>/dev/null || true",
        "cd \"\$DEST\"",
        "exec ./start.sh",
        "exit 0",
        "# __ARCHIVE_BELOW__",
        ""
    ).joinToString("\n")

    /**
     * The Windows stub: a batch file that hands the work to PowerShell, because cmd.exe cannot seek.
     *
     * The same trick the generated `start.bat` already uses — `-ExecutionPolicy Bypass` — because a
     * downloaded `.ps1` is blocked outright and double-clicking one opens Notepad. Four rules keep it
     * working, and `SelfExtractingArchiveTest` pins every one of them:
     *
     * - **No `goto`, no labels.** cmd.exe resolves a label by scanning the file, and it cannot scan
     *   across the runs of null bytes gzip output is made of. Everything runs forward, once.
     * - **`EXIT /B` before the payload**, so the parser stops before it ever reaches the archive.
     * - **No `"`, `%` or `!` inside the `-Command` argument.** A double quote ends cmd's argument and
     *   turns the rest of the line into cmd operators; a `%` is substituted before PowerShell sees it;
     *   a `!` is eaten when delayed expansion is on. Every string in there is single-quoted.
     * - **State travels in environment variables.** `%~f0` may contain spaces, ampersands and
     *   parentheses, and pasting it into the PowerShell text is what breaks first.
     *
     * `FileShare.ReadWrite` on the self-open is not optional: cmd.exe is still holding this very file
     * open while the batch part runs, and the read-only share `OpenRead` asks for would be denied.
     */
    fun batch(name: String): String {
        val powershell = listOf(
            "\$ErrorActionPreference = 'Stop';",
            "\$sfx = \$env:SPC_SFX;",
            "\$offset = [int64]::Parse(\$env:SPC_OFFSET);",
            "\$dest = Join-Path \$env:USERPROFILE (Join-Path 'mc-servers' \$env:SPC_NAME);",
            "if (Test-Path -LiteralPath \$dest) { Write-Host (\$dest + ' already exists - refusing to overwrite it.'); exit 1 };",
            "\$tar = Get-Command tar.exe -ErrorAction SilentlyContinue;",
            "if (\$null -eq \$tar) { Write-Host 'tar.exe was not found - Windows 10 1803 or newer is required.'; exit 1 };",
            "Write-Host ('Extracting ' + \$env:SPC_NAME + ' to ' + \$dest);",
            "New-Item -ItemType Directory -Path \$dest -Force | Out-Null;",
            "\$tmp = Join-Path \$env:TEMP (\$env:SPC_NAME + '.tar.gz');",
            "\$in = [System.IO.File]::Open(\$sfx, 'Open', 'Read', 'ReadWrite');",
            "try { [void]\$in.Seek(\$offset - 1, 'Begin');",
            "\$out = [System.IO.File]::Create(\$tmp);",
            "try { \$in.CopyTo(\$out, 1048576) } finally { \$out.Dispose() } } finally { \$in.Dispose() };",
            "& \$tar.Source -x -z -f \$tmp -C \$dest;",
            "\$code = \$LASTEXITCODE;",
            "Remove-Item -LiteralPath \$tmp -Force -ErrorAction SilentlyContinue;",
            "if (\$code -ne 0) { Write-Host ('Extraction failed, exit code ' + \$code + '. Incomplete files are in ' + \$dest); exit \$code };",
            "Write-Host ('Starting the server in ' + \$dest);",
            "Set-Location -LiteralPath \$dest;",
            "& (Join-Path \$dest 'start.bat');",
            "exit \$LASTEXITCODE"
        ).joinToString(" ")
        return listOf(
            "@ECHO OFF",
            ":: Self-extracting ServerPackCreator server pack.",
            ":: Everything below the marker at the end of this file is a gzipped tar archive.",
            "SETLOCAL",
            "SET \"SPC_SFX=%~f0\"",
            "SET \"SPC_OFFSET=$OFFSET_PLACEHOLDER\"",
            "SET \"SPC_NAME=$NAME_PLACEHOLDER\"",
            "PowerShell -NoProfile -ExecutionPolicy Bypass -Command \"$powershell\"",
            // One line, because %SPC_EXIT% is substituted before ENDLOCAL discards it. Split in two
            // and the batch always reports success, whatever happened.
            "SET \"SPC_EXIT=%ERRORLEVEL%\"",
            "ENDLOCAL & EXIT /B %SPC_EXIT%",
            ":: __ARCHIVE_BELOW__",
            ""
        ).joinToString("\r\n")
    }
}
