<#
.SYNOPSIS
    Turn a fresh Windows guest into a registered Forgejo Actions runner.

.DESCRIPTION
    Run once by install.bat at the end of the unattended install, and safe to re-run by hand
    afterwards:

        powershell -NoProfile -ExecutionPolicy Bypass -File C:\OEM\provision.ps1

    NO WINGET. It is tempting and it does not work here: winget ships inside the App Installer MSIX,
    which is registered PER USER, and this runs as SYSTEM during the unattended install before any
    user profile exists. The symptom is exactly `The term 'winget' is not recognized`. Installers are
    downloaded and run directly instead, which also works on a guest that has never had the Store.

    WHAT IT INSTALLS, AND WHAT IT DELIBERATELY DOES NOT:

      node  REQUIRED. Forgejo Actions runs every JavaScript action by exec'ing `node`, and under the
            `host` scheme it resolves it with a plain PATH lookup -- read from the runner's own
            `act/container/host_environment.go`, which calls `LookPath` and supplies nothing. Without
            it `actions/checkout` dies with exit 127 and every later step is skipped.
      git   For `actions/checkout`. Its API fallback exists, but a real git is what the workflows
            here assume.
      JDK   NOT installed. Every workflow job that needs one uses `actions/setup-java`, which
            installs and caches it per run -- so a JDK baked in here would be a second, unused copy
            that drifts from the one the build actually uses.

    From the host, on drive Z: (the compose file's ./shared):

      Z:\forgejo-runner.exe   cross-compiled on the Linux host, see the README beside the compose file
      Z:\runner-uuid.txt      the UUID the web UI showed after "Create new runner"
      Z:\runner-token.txt     the Token it showed beside that UUID
      Z:\runner-labels.txt    one label, e.g. `windows-latest:host`

    REGISTRATION IS A CONFIG FILE, NOT A COMMAND. Forgejo's web UI hands out a UUID and a Token and
    its own documentation says to copy them into the runner configuration's `server` section, so that
    is all this does -- `forgejo-runner register` is deprecated in favour of exactly that, and
    consuming a separate registration token as well would be a second mechanism for no gain.

#>
[CmdletBinding()]
param(
    [string] $SharedDrive = 'Z:',
    [string] $InstallDir  = 'C:\forgejo-runner',
    [string] $Instance    = 'https://git.griefed.de'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# Windows PowerShell 5.1 can still negotiate TLS 1.0 by default, which nodejs.org and GitHub refuse.
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Step([string] $message) { Write-Host "=== $message ===" }

# x86_64 only: the repository ships no Windows ARM64 artifact, so a guest of any other architecture is
# a misconfiguration rather than a case to handle. Named rather than hardcoded because the Node
# installer's filename carries it.
$arch = switch ($env:PROCESSOR_ARCHITECTURE) {
    'AMD64' { 'x64' }
    default { throw "This runner is x86_64 only; got guest architecture '$($env:PROCESSOR_ARCHITECTURE)'." }
}
Step "Guest architecture: $env:PROCESSOR_ARCHITECTURE (installers: $arch)"

<#
.SYNOPSIS
    Read a credential out of [Path], trimmed, and fail with its shape if it is not [Pattern].
.DESCRIPTION
    `-Raw` then Trim, because a value written with `echo` carries a newline and one written on Windows
    may carry a BOM; both are invisible and both make the instance reject it.

    [Pattern] is deliberately loose for the token. Its length has been hard-coded here twice and been
    wrong twice -- the format is the server's business and has changed between versions -- so this
    catches an empty file, a leftover placeholder and pasted prose, and lets Forgejo judge the rest.
#>
function Read-Credential {
    param([string] $Path, [string] $What, [string] $Pattern)

    if (-not (Test-Path $Path)) {
        throw "$Path is missing. Put the $What from the web UI's new-runner dialog there."
    }
    $value = (Get-Content $Path -Raw).Trim([char]0xFEFF, [char]0x20, [char]0x09, [char]0x0D, [char]0x0A)
    $masked = if ($value.Length -ge 12) {
        $value.Substring(0, 4) + ('*' * ($value.Length - 8)) + $value.Substring($value.Length - 4)
    } else { $value }

    if ($value -notmatch $Pattern) {
        throw "$Path does not hold a $What (read $($value.Length) characters: $masked)."
    }
    Write-Host "  $What : $($value.Length) characters, $masked"
    return $value
}

<#
.SYNOPSIS
    Download [Url] and run it silently with [Arguments], failing on anything but a clean exit.
.DESCRIPTION
    Exit code 3010 is "success, reboot required" and is treated as success: the runner service is
    created after everything is installed, so nothing here needs the reboot to have happened.
#>
function Install-Package {
    param([string] $Name, [string] $Url, [string] $FileName, [string[]] $Arguments)

    $installer = Join-Path $env:TEMP $FileName
    Write-Host "  downloading $Name -- $Url"
    Invoke-WebRequest -Uri $Url -OutFile $installer -UseBasicParsing
    Write-Host "  installing $Name"
    $process = Start-Process -FilePath $installer -ArgumentList $Arguments -Wait -PassThru
    if ($process.ExitCode -ne 0 -and $process.ExitCode -ne 3010) {
        throw "$Name installer exited $($process.ExitCode)"
    }
    Remove-Item $installer -Force -ErrorAction SilentlyContinue
}

# --- node ---------------------------------------------------------------------------------------
# Resolved from nodejs.org's own index rather than pinned, so this does not go stale unattended.
#
# NOTHING HERE RELIES ON PIPELINE UNROLLING, and that is not style. The pipeline form
# (`Invoke-RestMethod ... | Where-Object { $_.lts } | Select-Object -First 1`) resolves correctly under
# PowerShell 7. Under Windows PowerShell 5.1, which is what this guest runs, it returned EVERY version
# at once: `$lts` became an 800-entry string, the URL built from it was nonsense, and the failure
# surfaced as `400 Bad Request` from nodejs.org rather than as "the version could not be resolved".
# `foreach` behaves the same either way, and the shape check below is what turns a bad resolution into
# an error that names itself.
Step 'Installing Node.js (LTS)'
$releases = Invoke-RestMethod 'https://nodejs.org/dist/index.json' -UseBasicParsing
$lts = $null
foreach ($release in @($releases)) {
    # `lts` is the codename STRING on an LTS release and the boolean false otherwise, so the test is
    # for a string rather than for truthiness -- an array of mixed values is truthy too.
    if ($release.lts -is [string] -and $release.lts) { $lts = $release.version; break }
}
if ($lts -notmatch '^v\d+\.\d+\.\d+$') {
    throw "Could not resolve a Node LTS version from nodejs.org's index; got '$lts'."
}
Write-Host "  resolved Node LTS: $lts"
Install-Package -Name "Node.js $lts" `
    -Url "https://nodejs.org/dist/$lts/node-$lts-$arch.msi" `
    -FileName "node-$lts-$arch.msi" `
    -Arguments @('/quiet', '/norestart')

# --- git ----------------------------------------------------------------------------------------
Step 'Installing Git for Windows'
# Same shape, same reason as Node above: iterate, then check what came out.
$assetPattern = 'Git-*-64-bit.exe'
$latest = Invoke-RestMethod 'https://api.github.com/repos/git-for-windows/git/releases/latest' `
    -Headers @{ 'User-Agent' = 'serverpackcreator-runner-provisioning' }
$gitAsset = $null
foreach ($asset in @($latest.assets)) {
    # `PortableGit-*` also ends in the same suffix; the `Git-` prefix is what excludes it.
    if ($asset.name -like $assetPattern) { $gitAsset = $asset; break }
}
if (-not $gitAsset -or $gitAsset.name -notmatch '^Git-[0-9.]+-64-bit\.exe$') {
    throw "Could not find a Git for Windows $arch installer matching '$assetPattern'; got '$($gitAsset.name)'."
}
Write-Host "  resolved Git installer: $($gitAsset.name)"
# Inno Setup. /VERYSILENT shows nothing, /NORESTART because the service is created below, and
# /NOCANCEL /SP- stop it waiting for a human that is not there.
Install-Package -Name $gitAsset.name -Url $gitAsset.browser_download_url -FileName $gitAsset.name `
    -Arguments @('/VERYSILENT', '/NORESTART', '/NOCANCEL', '/SP-')

# --- make the installs visible to this process and to the service -------------------------------
# An installer updates the MACHINE PATH in the registry; it cannot update the environment of a
# process that is already running. Re-reading it is necessary and NOT sufficient -- re-reading alone
# was tried and still produced `git not found`, with a new terminal then working, which is the
# signature of this process never seeing the update. So the install locations are also added
# explicitly, which does not depend on what the installer did or on when it did it.
Step 'Refreshing PATH'
$machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')

# Git for Windows defaults to PathOption=Cmd and does add itself; Node's MSI likewise. Both are
# re-asserted anyway, because a PATH that is merely usually right produces a failure that moves.
$knownBinDirs = @(
    (Join-Path $env:ProgramFiles 'Git\cmd'),
    (Join-Path $env:ProgramFiles 'nodejs'),
    (Join-Path ${env:ProgramFiles(x86)} 'Git\cmd')
) | Where-Object { $_ -and (Test-Path $_) }

foreach ($dir in $knownBinDirs) {
    if (($machinePath -split ';') -notcontains $dir) {
        Write-Host "  adding $dir to the machine PATH"
        $machinePath = "$machinePath;$dir"
        [Environment]::SetEnvironmentVariable('Path', $machinePath, 'Machine')
    }
}
$env:Path = "$machinePath;" + [Environment]::GetEnvironmentVariable('Path', 'User')

# Resolved to FULL PATHS and used as such below. `git config --system` later in this script failed
# once already for exactly this reason; calling a resolved path cannot fail that way.
$tools = @{}
foreach ($tool in @('node', 'git')) {
    $resolved = Get-Command $tool -ErrorAction SilentlyContinue
    if (-not $resolved) {
        $fallback = $knownBinDirs | ForEach-Object { Join-Path $_ "$tool.exe" } | Where-Object { Test-Path $_ } | Select-Object -First 1
        if (-not $fallback) {
            throw "$tool is not on PATH and was not found in $($knownBinDirs -join ', ') after installing it."
        }
        $tools[$tool] = $fallback
    } else {
        $tools[$tool] = $resolved.Source
    }
    Write-Host "  $tool -> $($tools[$tool])"
}

# --- long paths ---------------------------------------------------------------------------------
# The staged jlink runtime is deep, and Windows' 260-character default truncates it in ways that read
# as a corrupt artifact rather than as a path limit.
Step 'Enabling long paths'
Set-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem' -Name LongPathsEnabled -Value 1
& $tools['git'] config --system core.longpaths true

# --- the runner ---------------------------------------------------------------------------------
Step 'Installing the Forgejo runner'
$binary   = Join-Path $SharedDrive 'forgejo-runner.exe'
$uuidFile = Join-Path $SharedDrive 'runner-uuid.txt'
$token    = Join-Path $SharedDrive 'runner-token.txt'
$labels   = Join-Path $SharedDrive 'runner-labels.txt'
foreach ($required in @($binary, $uuidFile, $token, $labels)) {
    if (-not (Test-Path $required)) {
        throw "$required is missing. Put it in the compose file's ./shared and re-run this script."
    }
}

New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
Copy-Item $binary (Join-Path $InstallDir 'forgejo-runner.exe') -Force
Push-Location $InstallDir
try {
    # The web UI's "Create new runner" dialog displays a UUID and a Token and says to copy them into
    # the runner configuration's `server` section. That is the only route this script supports.
    # `forgejo-runner register` is deprecated in favour of exactly this, and consuming a separate
    # registration token would be a second mechanism for no gain.
    $runnerUuid  = Read-Credential $uuidFile  'UUID'  '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
    $runnerToken = Read-Credential $token     'token' '^[A-Za-z0-9_-]{16,}$'
    $runnerLabels = (Get-Content $labels -Raw).Trim()

    Step "Writing the runner configuration (labels '$runnerLabels')"
    # Labels are declared ON the connection: per the runner's config reference, a connection with none
    # falls back to `runner.labels`, and being explicit keeps one guest's labels from being read as
    # another's.
    $configFile = Join-Path $InstallDir 'config.yml'
    @(
        'server:'
        '  connections:'
        '    forgejo:'
        "      url: $Instance"
        "      uuid: $runnerUuid"
        "      token: $runnerToken"
        '      labels:'
        "        - $runnerLabels"
    ) | Set-Content -LiteralPath $configFile -Encoding ASCII
    Write-Host "  wrote $configFile for $Instance"

    Step 'Starting the runner at boot'
    # A SCHEDULED TASK, NOT A WINDOWS SERVICE, and the distinction is the whole reason this works.
    # `forgejo-runner daemon` is an ordinary long-running console program: the runner implements no
    # Windows service support at all, so a `sc.exe create` registration starts it, waits for the status
    # report that a service is required to send, never gets one, and kills it after about 30 seconds.
    # The runner connects inside that window and goes away -- which Forgejo shows as a runner that is
    # OFFLINE with a "last online" timestamp a few minutes old, rather than as a failure.
    #
    # Clean up such a registration if an earlier run of this script left one behind.
    if (Get-Service ForgejoRunner -ErrorAction SilentlyContinue) {
        Write-Host '  removing the ForgejoRunner service left by an earlier run'
        sc.exe stop ForgejoRunner   | Out-Null
        sc.exe delete ForgejoRunner | Out-Null
    }

    # A wrapper rather than invoking the exe directly, for two reasons worth the extra file: it fixes
    # PATH where a scheduled task gives no equivalent of a service's `Environment` registry value --
    # and PATH is exactly what bit this guest once already -- and it keeps a log, since a daemon
    # started by the scheduler has nowhere to write otherwise.
    $launcher = Join-Path $InstallDir 'run-runner.cmd'
    $daemonLog = Join-Path $InstallDir 'daemon.log'
    @(
        '@echo off'
        'REM Generated by provision.ps1. Run it by hand to see what the scheduled task sees.'
        "set `"PATH=%PATH%;$($knownBinDirs -join ';')`""
        "cd /d `"$InstallDir`""
        "`"$InstallDir\forgejo-runner.exe`" daemon --config `"$configFile`" >> `"$daemonLog`" 2>&1"
    ) | Set-Content -LiteralPath $launcher -Encoding ASCII
    Write-Host "  wrote $launcher"

    Unregister-ScheduledTask -TaskName 'ForgejoRunner' -Confirm:$false -ErrorAction SilentlyContinue
    Register-ScheduledTask -TaskName 'ForgejoRunner' -Force `
        -Action (New-ScheduledTaskAction -Execute $launcher) `
        -Trigger (New-ScheduledTaskTrigger -AtStartup) `
        -Principal (New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest) `
        -Settings (New-ScheduledTaskSettingsSet `
            -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable `
            -MultipleInstances IgnoreNew `
            -RestartInterval (New-TimeSpan -Minutes 1) -RestartCount 999 `
            -ExecutionTimeLimit ([TimeSpan]::Zero)) | Out-Null
    # `ExecutionTimeLimit` of zero is "no limit". The default is three days, after which the scheduler
    # would stop the daemon and the runner would go offline for no reason anybody could see.
    Start-ScheduledTask -TaskName 'ForgejoRunner'

    # VERIFY IT IS ACTUALLY RUNNING. The service registration this replaces was never checked, which
    # is why a runner that had been killed by the SCM looked like a successful provisioning.
    Start-Sleep -Seconds 10
    $daemonProcess = Get-Process forgejo-runner -ErrorAction SilentlyContinue
    if (-not $daemonProcess) {
        $tail = if (Test-Path $daemonLog) { Get-Content $daemonLog -Tail 20 -ErrorAction SilentlyContinue } else { '(no log yet)' }
        throw @"
The runner was started but is not running ten seconds later.

Last lines of ${daemonLog}:
$($tail -join "`n")

Run $launcher by hand to see it without the scheduler in the way.
"@
    }
    Write-Host "  running as PID $($daemonProcess.Id)"

} finally {
    Pop-Location
}

Step 'Done'
Write-Host "Runner installed in $InstallDir, started, and set to start at boot (task 'ForgejoRunner')."
Write-Host 'Check it shows ONLINE under Settings -> Actions -> Runners before relying on it; a runner'
Write-Host 'that registered but is not running appears there with an old "last online" time instead.'
