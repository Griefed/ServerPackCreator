<#
.SYNOPSIS
    Build a self-contained ServerPackCreator app-image for Windows -- the AppImage's counterpart.

.DESCRIPTION
    `jpackage --type app-image` emits a directory holding a native launcher, a jlinked Java runtime and
    the application jar. No installer, no system Java: the user unpacks the zip and runs the exe.

    It mirrors the three launchers `spc.install4j` defines -- the GUI, `-cli` and `-web` -- so the
    artifact offers the same entry points as the installer does.

    THIS SCRIPT OWNS THE OUTPUT FILENAME, including `_experimental`, exactly as `build-appimage.sh`
    owns the AppImages'. The workflows resolve it by glob. A copy of the name in a workflow is a copy
    that goes stale the next time this changes.

    WINDOWS POWERSHELL 5.1, not PowerShell 7. Windows ships no `pwsh`, and the runner guest installs
    none, so nothing here may use a 7-only construct -- no ternary, no `??`, no `&&`/`||` chains, and
    every `Get-ChildItem` whose result is indexed gets wrapped in `@()`, because 5.1 unwraps a
    single-element result to a bare object.

    LANDMINE -- jpackage CANNOT CROSS-COMPILE. JEP 392 makes it an explicit non-goal, so a Windows
    app-image has to be produced on Windows. That is why this is a PowerShell script rather than
    another arm of build-appimage.sh, and why the job calling it needs a runner that
    `claude-docs/WINDOWS-RUNNER.md` describes how to register.

.PARAMETER Version
    The ServerPackCreator version, e.g. `9.1.0` or `9.2.0-alpha.3`. Used verbatim in the artifact name.

.PARAMETER Arch
    `x86_64`, the only Windows architecture this repository ships. Names the artifact only -- jpackage
    always builds for the host, so this must match the runner it is invoked on, and is checked against
    the host rather than trusted. Windows on ARM64 runs the x86_64 artifact.

.PARAMETER JarPath
    The Spring Boot fat jar. Defaults to the one `./gradlew build` leaves in the app module.

.EXAMPLE
    powershell -File misc/build-winimage.ps1 -Version 9.1.0 -Arch x86_64
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string] $Version,
    [Parameter(Mandatory = $true)][ValidateSet('x86_64')][string] $Arch,
    [string] $JarPath
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$AppName   = 'ServerPackCreator'
$MainClass = 'org.springframework.boot.loader.launch.JarLauncher'
# Identical to build-appimage.sh's APP_ARGS, so the two artifacts start the same application.
$JavaOptions = @(
    '-Dfile.encoding=UTF-8'
    '-Dlog4j2.formatMsgNoLookups=true'
    '-DServerPackCreator'
    '-Dname=ServerPackCreator'
    '-Dspring.application.name=ServerPackCreator'
)

# --- the host must be the architecture being claimed ------------------------------------------------
# jpackage builds for the machine it runs on and says nothing about it, so a mislabelled artifact is
# silent: an image built on one architecture and named for another unpacks fine and refuses to start, on
# the user's machine. An ARM64 host is rejected here rather than quietly producing an artifact nothing
# publishes.
$hostArch = switch ($env:PROCESSOR_ARCHITECTURE) {
    'AMD64' { 'x86_64' }
    'ARM64' { 'aarch64' }
    default { $env:PROCESSOR_ARCHITECTURE }
}
if ($hostArch -ne $Arch) {
    throw "Asked for $Arch but this host is $hostArch. jpackage cannot cross-compile (JEP 392), so this would produce a mislabelled artifact."
}

# --- the jar ----------------------------------------------------------------------------------------
# A caller that binds -JarPath to an EMPTY string is a broken caller, not a caller asking for the
# default, and the two are indistinguishable from `$JarPath` alone -- so `$PSBoundParameters` separates
# them. Falling through to the build directory answers a bad argument with `Run ./gradlew build first`,
# which is advice for a local build and misdirects everywhere the jar arrives as a CI artifact instead.
if ($PSBoundParameters.ContainsKey('JarPath') -and -not $JarPath) {
    throw '-JarPath was passed but is empty. Whatever supplies it resolved to nothing -- in CI that is the step output, not a missing jar.'
}
if (-not $JarPath) {
    # @() so a single match is still an array: unwrapped, 5.1 hands back a bare FileInfo.
    $candidates = @(Get-ChildItem 'serverpackcreator-app/build/libs' -Filter 'serverpackcreator-app-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'plain|javadoc|sources' })
    if (-not $candidates) { throw 'No ServerPackCreator app jar found. Run `./gradlew build` first, or pass -JarPath.' }
    $JarPath = $candidates[0].FullName
}
if (-not (Test-Path $JarPath)) { throw "No such jar: $JarPath" }

# --- jpackage's --app-version takes NUMBERS ONLY on Windows -------------------------------------------
# It becomes the exe's file-version resource, which jpackage requires to be `major[.minor[.build]]`, so
# `9.2.0-alpha.3` is rejected outright. Only this resource is reduced -- the ARTIFACT NAME keeps the
# real version, which is why the two are separate variables.
#
# It must never REFUSE, and that is not a nicety: devbuild.yml builds branch pushes as
# `<branch>-<short sha>` (`develop-a1b2c3d`), which contains no version at all. Refusing there would
# fail every dev build while looking like a version-parsing rule nobody questioned. A fallback costs
# a wrong number in one file-properties dialog; refusing costs the artifact.
$versionMatch = [regex]::Match($Version, '^\d+(\.\d+){0,2}')
if ($versionMatch.Success) {
    $numericVersion = $versionMatch.Value
} else {
    $numericVersion = '1.0.0'
    Write-Warning "No numeric version in '$Version'; the Windows file-version resource will say $numericVersion. The artifact name keeps '$Version'."
}

# `$inputDir` rather than `$input`: `$input` is PowerShell's automatic pipeline-input enumerator, and a
# name that is only safe while nothing here reads a pipeline is a name that breaks the day something does.
$staging  = Join-Path ([System.IO.Path]::GetTempPath()) "spc-winimage-$([guid]::NewGuid())"
$inputDir = Join-Path $staging 'input'
$dest     = Join-Path $staging 'image'
New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
Copy-Item $JarPath (Join-Path $inputDir "$AppName.jar")

# --- the two extra launchers, mirroring spc.install4j's -cli and -web --------------------------------
# `win-console=true` on both: the CLI is interactive and the web service logs to stdout, and without a
# console a jpackage launcher is a GUI binary whose output goes nowhere -- the same way the AppImage's
# warning used to vanish under a .desktop launch.
$cliProps = Join-Path $staging 'cli.properties'
$webProps = Join-Path $staging 'web.properties'
Set-Content -LiteralPath $cliProps -Encoding ASCII -Value @('arguments=-cli', 'win-console=true')
Set-Content -LiteralPath $webProps -Encoding ASCII -Value @('arguments=-web', 'win-console=true')

$jpackageArgs = @(
    '--type', 'app-image'
    '--name', $AppName
    '--app-version', $numericVersion
    '--input', $inputDir
    '--main-jar', "$AppName.jar"
    '--main-class', $MainClass
    '--dest', $dest
    '--vendor', 'Griefed'
    '--description', 'Create server packs from Minecraft Forge, NeoForge, Fabric, Quilt or LegacyFabric modpacks.'
    '--add-launcher', "$AppName-CLI=$cliProps"
    '--add-launcher', "$AppName-WebService=$webProps"
    # --- THE BUNDLED RUNTIME IS A COMPLETE ONE ------------------------------------------------------
    # This REPLACES jpackage's default jlink options wholesale -- they are
    # `--strip-native-commands --strip-debug --no-man-pages --no-header-files` -- so naming any of them
    # means naming all the ones that are still wanted. Two are deliberately left out, both so that this
    # artifact diagnoses a user's problem as well as the AppImage and the install4j build do, each of
    # which bundles a whole Adoptium JDK:
    #
    #   --strip-native-commands  deletes every launcher from the runtime. SPC reads its own
    #                            `java.home\bin\java` to fill the Java-for-modloader-server setting
    #                            (`SystemUtilities.acquireJavaPathFromSystem`), and a stripped runtime
    #                            answers that with a path that does not exist.
    #   --strip-debug            drops LineNumberTable and friends from the JDK's own classes, so every
    #                            JDK frame in a stack trace reads `(Unknown Source)` instead of a file
    #                            and line. SPC's own frames are unaffected -- its classes live in
    #                            `app\`, not in the jimage -- but a bug report's trace is mostly JDK.
    #
    # The two that stay exclude man pages and the JNI/JVMTI C headers: build-time material for
    # compiling native code against a JVM, which the JVM itself never opens.
    '--jlink-options', '--no-man-pages --no-header-files'
)
foreach ($option in $JavaOptions) { $jpackageArgs += @('--java-options', $option) }
if (Test-Path 'img/icon.ico') { $jpackageArgs += @('--icon', (Resolve-Path 'img/icon.ico').Path) }

Write-Host "Building $AppName $Version ($Arch) with jpackage..." -ForegroundColor Yellow
& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage exited $LASTEXITCODE" }

# --- the image has to be self-contained -------------------------------------------------------------
# `runtime\bin\java.exe` is here only because `--jlink-options` above leaves `--strip-native-commands`
# out; under jpackage's defaults jlink's StripNativeCommandsPlugin drops every NATIVE_CMD entry and no
# app-image has a `java` launcher at all. It is checked so that losing that option is a failed build
# rather than a setting that silently points at nothing.
#
# The rest is what the launchers load. jlink routes every `.dll` into `bin` on Windows
# (DefaultImageBuilder.nativeDir), so the VM is `runtime\bin\server\jvm.dll`, while the jimage is
# `runtime\lib\modules` on every platform. jpackage writes one `.cfg` per launcher beside the jar,
# and a launcher without its cfg cannot find the main class, so each is checked.
$imageDir = Join-Path $dest $AppName
$requiredEntries = @(
    "$AppName.exe"
    "$AppName-CLI.exe"
    "$AppName-WebService.exe"
    "app\$AppName.jar"
    "app\$AppName.cfg"
    "app\$AppName-CLI.cfg"
    "app\$AppName-WebService.cfg"
    'runtime\bin\java.exe'
    'runtime\bin\server\jvm.dll'
    'runtime\lib\modules'
)
foreach ($required in $requiredEntries) {
    $path = Join-Path $imageDir $required
    if (-not (Test-Path $path)) { throw "jpackage reported success but produced no $required -- the image is not self-contained." }
}

# --- the artifact -----------------------------------------------------------------------------------
# `_experimental` is decided HERE and nowhere else; the workflows glob for `ServerPackCreator-*.zip`.
$output = "$AppName-$Version-Windows-${Arch}_experimental.zip"
if (Test-Path $output) { Remove-Item $output -Force }
Compress-Archive -Path $imageDir -DestinationPath $output -CompressionLevel Optimal
Remove-Item $staging -Recurse -Force

$sizeMb = [math]::Round((Get-Item $output).Length / 1MB, 1)
Write-Host "Created $output ($sizeMb MB)" -ForegroundColor Green
