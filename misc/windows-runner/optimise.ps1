<#
.SYNOPSIS
    Strip a Windows guest down to what a build runner needs, and tune it for throughput.

.DESCRIPTION
    Run by hand in an Administrator PowerShell, before provision.ps1, and safe to re-run:

        powershell -NoProfile -ExecutionPolicy Bypass -File .\optimise.ps1

    NOTHING HERE IS FATAL. A guest that is merely fatter or slower than intended is still a working
    runner, so every action is guarded and reported rather than allowed to abort provisioning -- which
    is the step that actually matters. Read the log to see what did and did not happen.

    THIS PASS IS LOAD-BEARING, because the leaner editions are not an option. The README beside this
    script installs retail Windows 11 Pro, which ships the whole consumer app set, so every pattern
    below has something to find. The leaner editions are Evaluation media, which expires after 90 days
    with no key able to activate it, and a build runner that stops working on a date nobody wrote down
    is worse than a fat one. On a guest built from licensed LTSC media most of the
    removal pass correctly reports matching nothing, and then earns its place as the guard that
    notices when an edition starts shipping something new.

    APPS ARE MATCHED BY PATTERN, NOT BY EXACT NAME, and every pattern reports what it matched. Microsoft
    renames these: the Xbox app became `Microsoft.GamingApp`, Teams became `MSTeams`, Media Player is
    still `Microsoft.ZuneMusic`. A hardcoded list of exact names fails SILENTLY -- it removes nothing and
    says nothing -- so a pattern that matches zero packages is logged as such, which is the difference
    between "already absent" and "the name moved and nobody noticed".

    THE JOBS RUN AS SYSTEM, which is the fact every path in the tuning half depends on. provision.ps1
    registers the daemon as a scheduled task under the SYSTEM principal, so a job's temp directory,
    tool cache and Java user home are SYSTEM's, not the interactive account's. A Defender exclusion
    written against `$env:USERPROFILE` names the wrong profile and silently protects nothing.

    WHAT THIS CANNOT DO IS SET CPU FEATURES. Virtualisation-Based Security is disabled below, but the
    settings that do it are guest-side and an update may set them back. `domain.xml` withholds vmx/svm
    from the guest entirely, which is the half that cannot be undone from in here; this half is what
    makes the guest correct when it runs on a host whose domain definition predates that.
#>
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
# Deliberately NOT 'Stop': see the note above. Each action decides its own failure handling.
$ErrorActionPreference = 'Continue'

function Step([string] $message) { Write-Host "`n=== $message ===" }

<# Run [Action], reporting failure instead of propagating it. #>
function Try-Step {
    param([string] $What, [scriptblock] $Action)
    try { & $Action; Write-Host "  ok    $What" }
    catch { Write-Host "  FAIL  $What -- $($_.Exception.Message)" }
}

<# Run a native executable, raising when it exits non-zero. Native commands set $LASTEXITCODE
   rather than throwing, so a bare call inside Try-Step reports 'ok' however it went. #>
function Invoke-Native {
    param([string] $Executable, [string[]] $Arguments)
    & $Executable @Arguments | Write-Host
    if ($LASTEXITCODE -ne 0) { throw "$Executable $($Arguments -join ' ') exited $LASTEXITCODE" }
}

<# Create [Path] if needed and write one DWORD into it. Policy and DeviceGuard keys are frequently
   absent on a fresh guest, so creating before writing is the normal case rather than a fallback. #>
function Set-RegistryDword {
    param([string] $Path, [string] $Name, [int] $Value)
    New-Item $Path -Force | Out-Null
    Set-ItemProperty $Path -Name $Name -Value $Value -Type DWord
}

<# The filesystem path of SYSTEM's profile, read from the profile list rather than assumed.
   It is what decides where the runner daemon puts a job's files, and a wrong answer here excludes
   nothing while looking exactly like an exclusion that worked. The stored value is a REG_EXPAND_SZ
   (`%systemroot%\system32\config\systemprofile`), which PowerShell expands on read. Falls back to
   the conventional path rather than throwing, because an aborted pass is worse than an assumed one. #>
function Get-SystemProfilePath {
    $fallback = "$env:SystemRoot\System32\config\systemprofile"
    try {
        $key = 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\ProfileList\S-1-5-18'
        $path = (Get-ItemProperty -Path $key -Name ProfileImagePath -ErrorAction Stop).ProfileImagePath
        if ([string]::IsNullOrWhiteSpace($path)) { throw 'ProfileImagePath is empty' }
        return $path
    } catch {
        Write-Host "  warn  could not read SYSTEM's profile path ($($_.Exception.Message)); assuming $fallback"
        return $fallback
    }
}

# --------------------------------------------------------------------------------------------------
# Apps
# --------------------------------------------------------------------------------------------------
# Patterns are deliberately narrow. A pattern that caught a FRAMEWORK package -- Microsoft.VCLibs,
# Microsoft.UI.Xaml, Microsoft.NET.Native -- would break applications that depend on it, including ones
# installed later, so nothing here uses a bare `Microsoft.*`-style wildcard.
$unwanted = [ordered]@{
    'OneDrive (app package)'  = 'Microsoft.OneDrive*'
    'Copilot'                 = '*Copilot*'
    'Feedback Hub'            = '*WindowsFeedbackHub*'
    'Clipchamp'               = '*Clipchamp*'
    'News'                    = '*BingNews*'
    'Teams'                   = '*Teams*'
    'To Do'                   = '*Todos*'
    'Outlook (new)'           = '*OutlookForWindows*'
    'Power Automate'          = '*PowerAutomateDesktop*'
    'Quick Assist'            = '*QuickAssist*'
    'Solitaire & casual games'= '*SolitaireCollection*'
    'Sound Recorder'          = '*WindowsSoundRecorder*'
    'Sticky Notes'            = '*StickyNotes*'
    'Weather'                 = '*BingWeather*'
    'Media Player'            = '*ZuneMusic*'
    'Movies & TV'             = '*ZuneVideo*'
    'Paint'                   = '*Microsoft.Paint*'
    'Xbox app'                = '*GamingApp*'
    'Xbox game overlay'       = '*XboxGam*Overlay*'
    'Xbox identity provider'  = '*XboxIdentityProvider*'
    'Xbox speech overlay'     = '*XboxSpeechToTextOverlay*'
    'Xbox TCUI'               = '*Xbox.TCUI*'
    'Xbox Live'               = '*XboxLive*'
}

# Never remove these even if a pattern above somehow reaches them. The runner installs tooling later,
# and a missing framework package is a failure that surfaces much further away than its cause.
$protected = @('*VCLibs*', '*UI.Xaml*', '*NET.Native*', '*DesktopAppInstaller*', '*WindowsStore*',
               '*StorePurchaseApp*', '*SecHealthUI*', '*Win32WebViewHost*')

function Test-Protected([string] $Name) {
    foreach ($pattern in $protected) { if ($Name -like $pattern) { return $true } }
    return $false
}

Step 'Removing apps a build runner does not need'
foreach ($entry in $unwanted.GetEnumerator()) {
    $label = $entry.Key; $pattern = $entry.Value
    $matched = 0

    # Installed copies, for every user that already exists.
    foreach ($package in @(Get-AppxPackage -AllUsers -Name $pattern -ErrorAction SilentlyContinue)) {
        if (Test-Protected $package.Name) { Write-Host "  skip  $($package.Name) is protected"; continue }
        $matched++
        try { Remove-AppxPackage -Package $package.PackageFullName -AllUsers -ErrorAction Stop }
        catch { Write-Host "  warn  could not remove $($package.Name): $($_.Exception.Message)" }
    }

    # The PROVISIONED copy is the one that matters here: it is what gets installed into every user
    # created later, which on a fresh guest is all of them.
    foreach ($package in @(Get-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue |
                           Where-Object { $_.DisplayName -like $pattern })) {
        if (Test-Protected $package.DisplayName) { Write-Host "  skip  $($package.DisplayName) is protected"; continue }
        $matched++
        try { Remove-AppxProvisionedPackage -Online -PackageName $package.PackageName -ErrorAction Stop | Out-Null }
        catch { Write-Host "  warn  could not deprovision $($package.DisplayName): $($_.Exception.Message)" }
    }

    if ($matched -gt 0) { Write-Host "  ok    $label -- removed $matched" }
    else { Write-Host "  none  $label -- nothing matched '$pattern' (already absent, or the name moved)" }
}

# OneDrive is not an app package on most images; it is a per-user setup stub run from the system
# directory, so the package pattern above will usually report "none" for it and this is what works.
Step 'Removing OneDrive'
foreach ($setup in @("$env:SystemRoot\SysWOW64\OneDriveSetup.exe", "$env:SystemRoot\System32\OneDriveSetup.exe")) {
    if (Test-Path $setup) {
        Try-Step "uninstall via $setup" { Start-Process $setup -ArgumentList '/uninstall' -Wait -NoNewWindow }
    }
}

# Removing an app does not stop Windows fetching it again. These two policies are what make the
# removal stick, and without them a guest quietly re-acquires the consumer apps on first sign-in.
Step 'Preventing the removed apps from coming back'
Try-Step 'disable consumer features (suggested apps, auto-installed games)' {
    Set-RegistryDword 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\CloudContent' DisableWindowsConsumerFeatures 1
}
Try-Step 'disable OneDrive by policy' {
    Set-RegistryDword 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\OneDrive' DisableFileSyncNGSC 1
}
Try-Step 'turn off Copilot by policy' {
    Set-RegistryDword 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsCopilot' TurnOffWindowsCopilot 1
}

# --------------------------------------------------------------------------------------------------
# Virtualisation-Based Security
# --------------------------------------------------------------------------------------------------
# THE LARGEST SINGLE GUEST-SIDE WIN, and the one whose cost is invisible from inside Windows. VBS and
# HVCI ("Memory integrity") run the Windows kernel as a guest of a Hyper-V root partition. On real
# hardware that costs single-digit percent; in a KVM guest it makes this machine a NESTED hypervisor,
# and every exit is paid twice. Microsoft enables Memory integrity through quality updates, so this
# runs on every pass rather than once at install time.
#
# The CPU-feature half is in `domain.xml` and is the durable one: a guest with no vmx/svm cannot start
# VBS whatever these values say. Both halves exist because the domain may predate the change.
Step 'Virtualisation-Based Security'
function Get-VbsStatus {
    $guard = Get-CimInstance -ClassName Win32_DeviceGuard -Namespace root\Microsoft\Windows\DeviceGuard -ErrorAction Stop
    # VirtualizationBasedSecurityStatus: 0 off, 1 configured but not running, 2 running.
    switch ($guard.VirtualizationBasedSecurityStatus) {
        0 { 'not enabled' }
        1 { 'enabled but not running' }
        2 { 'RUNNING' }
        default { "unknown ($($guard.VirtualizationBasedSecurityStatus))" }
    }
}
try { Write-Host "  before: VBS is $(Get-VbsStatus)" }
catch { Write-Host "  warn  could not read Win32_DeviceGuard: $($_.Exception.Message)" }

Try-Step 'disable VBS and HVCI by policy' {
    Set-RegistryDword 'HKLM:\SYSTEM\CurrentControlSet\Control\DeviceGuard' EnableVirtualizationBasedSecurity 0
    Set-RegistryDword 'HKLM:\SYSTEM\CurrentControlSet\Control\DeviceGuard\Scenarios\HypervisorEnforcedCodeIntegrity' Enabled 0
    Set-RegistryDword 'HKLM:\SYSTEM\CurrentControlSet\Control\Lsa' LsaCfgFlags 0
}
# The registry values decide whether Windows CONFIGURES VBS; this decides whether the hypervisor that
# would host it is started at all. Both are needed, and neither takes effect before a reboot.
Try-Step 'stop the Windows hypervisor launching at boot' { Invoke-Native bcdedit.exe '/set','hypervisorlaunchtype','off' }
Write-Host '  note  VBS state does not change until the guest reboots; re-run Get-VbsStatus after that.'

Step 'Clock source'
# `domain.xml` gives this guest the Hyper-V reference TSC page (<timer name='hypervclock'/> plus
# <frequencies state='on'/>) and no HPET at all, so Windows can read the time without leaving the VM.
# `useplatformclock` and `useplatformtick` override that and send every time read to an emulated timer
# -- one VM exit each, on a kernel that reads the clock constantly, and with hpet absent the fallback
# is worse still. Neither is set by default, so this normally confirms rather than changes; it exists
# because the two are the most commonly copied "VM tuning" tip and are backwards here.
foreach ($bcdValue in @('useplatformclock', 'useplatformtick')) {
    # bcdedit's own output is the only way to ask whether a value is present; `/deletevalue` on an
    # absent one exits non-zero, which is why this looks before it deletes.
    $currentEntry = & bcdedit.exe /enum '{current}' 2>&1
    if ($currentEntry -match $bcdValue) {
        Try-Step "remove the $bcdValue override" { Invoke-Native bcdedit.exe '/deletevalue',$bcdValue }
    } else {
        Write-Host "  ok    $bcdValue is not set (the reference TSC is used)"
    }
}

# --------------------------------------------------------------------------------------------------
# Performance
# --------------------------------------------------------------------------------------------------
Step 'Power'
# Reclaims hiberfil.sys, which is sized against RAM -- 8 GB of the disk image, for a machine that must
# never sleep anyway.
Try-Step 'disable hibernation' { Invoke-Native powercfg.exe '/hibernate','off' }
Try-Step 'high performance power scheme' { Invoke-Native powercfg.exe '/setactive','SCHEME_MIN' }
Try-Step 'never sleep or blank the display' {
    Invoke-Native powercfg.exe '/change','standby-timeout-ac','0'
    Invoke-Native powercfg.exe '/change','monitor-timeout-ac','0'
    Invoke-Native powercfg.exe '/change','disk-timeout-ac','0'
}
# DO NOT set IDLEDISABLE here. It is the usual next step in a bare-metal tuning guide and it is
# backwards in a guest: an idle vcpu that halts is a vcpu the host can give to something else, and a
# vcpu kept out of idle states spins against the host scheduler for nothing. `virsh cpu-stats $VM
# --total` over a quiet minute is what shows the difference.

Step 'Services a headless build runner does not use'
# Each name costs something while it runs: WSearch indexes a disk whose contents are rebuilt every job,
# SysMain prefetches on a VM where the guess is wrong, DPS and PcaSvc do work on every process launch,
# TrkWks does I/O on every file move, and the rest service hardware a libvirt guest does not have.
# Disabled rather than stopped, so none comes back on reboot. A name absent from this edition is
# reported as such rather than silently skipped -- the same reason the app patterns above report a
# miss.
#
# DoSvc (Delivery Optimization) is NOT in this list, deliberately: its service key is ACL-protected and
# `Set-Service` answers `Access is denied` even to an administrator. Its policy is below, which is the
# supported lever and reaches the same end.
$unusedServices = [ordered]@{
    'WSearch'        = 'Windows Search indexing'
    'SysMain'        = 'SysMain / Superfetch'
    'DiagTrack'      = 'connected user experiences and telemetry'
    'WerSvc'         = 'Windows Error Reporting'
    'DPS'            = 'diagnostic policy service'
    'PcaSvc'         = 'program compatibility assistant'
    'TrkWks'         = 'distributed link tracking'
    'Spooler'        = 'print spooler'
    'MapsBroker'     = 'downloaded maps manager'
    'lfsvc'          = 'geolocation'
    'WbioSrvc'       = 'biometrics'
    'bthserv'        = 'bluetooth support'
    'BTAGService'    = 'bluetooth audio gateway'
    'BthAvctpSvc'    = 'bluetooth AVCTP'
    'XblAuthManager' = 'Xbox Live auth manager'
    'XblGameSave'    = 'Xbox Live game save'
    'XboxGipSvc'     = 'Xbox accessory management'
    'XboxNetApiSvc'  = 'Xbox Live networking'
    'SSDPSRV'        = 'SSDP discovery'
    'upnphost'       = 'UPnP device host'
    'RetailDemo'     = 'retail demo'
    'Fax'            = 'fax'
    'WpcMonSvc'      = 'parental controls'
    'SEMgrSvc'       = 'payments and NFC'
    'AJRouter'       = 'AllJoyn router'
    'PhoneSvc'       = 'phone service'
    'wisvc'          = 'Windows Insider'
    'stisvc'         = 'Windows image acquisition'
    'icssvc'         = 'mobile hotspot'
    'DusmSvc'        = 'data usage metering'
    'diagnosticshub.standardcollector.service' = 'diagnostics hub collector'
}

# Named so that an edit to the list above cannot take one of them out by accident. The first four are
# this guest specifically: Schedule runs the runner daemon, TermService is the RDP the README hands
# over to, and the two virtio services are what `domain.xml` depends on from inside -- `qemu-ga` (shown
# as "QEMU Guest Agent"; the name is the one qemu's own `QGA_SERVICE_NAME` registers) answers
# `virsh shutdown --mode agent` and `virsh domifaddr --source agent` when Windows itself will not, and
# `BalloonService` is what makes <memballoon freePageReporting='on'/> mean anything. The rest are
# what a networked Windows needs in order to boot, get an address and resolve a name.
$requiredServices = @('Schedule', 'TermService', 'qemu-ga', 'BalloonService',
                      'Dhcp', 'Dnscache', 'W32Time', 'EventLog', 'mpssvc', 'BFE',
                      'ProfSvc', 'RpcSs', 'DcomLaunch', 'LanmanWorkstation', 'LanmanServer')

foreach ($entry in $unusedServices.GetEnumerator()) {
    $service = $entry.Key
    if ($requiredServices -contains $service) { Write-Host "  skip  $service is required by this guest"; continue }
    if (-not (Get-Service $service -ErrorAction SilentlyContinue)) {
        Write-Host "  none  $service -- not present on this edition"
        continue
    }
    Try-Step "disable $service ($($entry.Value))" {
        Stop-Service $service -Force -ErrorAction SilentlyContinue
        Set-Service  $service -StartupType Disabled -ErrorAction Stop
    }
}

# `DODownloadMode = 0` is HTTP-only: downloads still work, peer-to-peer sharing does not happen. The
# policy key takes priority over the Settings UI, which is why it is set here rather than the service.
Try-Step 'stop Delivery Optimization sharing with peers' {
    Set-RegistryDword 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' DODownloadMode 0
}

# The Store Manager's own background work, which SysMain being disabled does not cover. Page combining
# scans the guest's RAM for identical pages, which is the guest spending host CPU to do what the host's
# own KSM would do -- and `domain.xml` turns that off for this domain too, for the same reason.
# Memory compression trades CPU for RAM and only pays when RAM is short; 16 GiB against a job that
# never runs Gradle is not short. Re-enable it with `Enable-MMAgent -MemoryCompression` if the guest
# ever starts paging.
Step 'Memory manager background work'
Try-Step 'disable prefetch, prelaunch, page combining and memory compression' {
    Disable-MMAgent -ApplicationLaunchPrefetching -ApplicationPreLaunch -PageCombining -MemoryCompression -ErrorAction Stop
}

Step 'Visuals and housekeeping'
Try-Step 'visual effects: best performance' {
    Set-RegistryDword 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VisualEffects' VisualFXSetting 2
}
Try-Step 'disable System Restore' { Disable-ComputerRestore -Drive "$env:SystemDrive\" }
# Automatic Maintenance is what starts a defrag, a Defender scan or a .NET NGEN pass at an idle moment,
# which on a runner is the moment a job arrives. The scheduled defrag task goes with it; the retrim it
# used to carry is re-added below as its own task, because that part is worth keeping.
Try-Step 'disable Automatic Maintenance' {
    Set-RegistryDword 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Schedule\Maintenance' MaintenanceDisabled 1
}
Step 'Scheduled tasks that do work this guest has no use for'
# Grouped by folder, because that is how Disable-ScheduledTask addresses them and how the task
# scheduler shows them. Everything here either profiles the machine for telemetry, measures hardware
# this guest does not have, or runs a long clean-up at an idle moment -- which on a runner is the
# moment a job arrives. ScheduledDefrag is among them: the retrim it used to carry is re-registered
# below as a task of its own, because that part is worth keeping and the defrag is not.
$unusedTasks = [ordered]@{
    '\Microsoft\Windows\Application Experience\'                = @('Microsoft Compatibility Appraiser', 'ProgramDataUpdater', 'StartupAppTask', 'PcaPatchDbTask')
    '\Microsoft\Windows\Customer Experience Improvement Program\' = @('Consolidator', 'UsbCeip')
    '\Microsoft\Windows\Defrag\'                                = @('ScheduledDefrag')
    '\Microsoft\Windows\Maintenance\'                           = @('WinSAT')
    '\Microsoft\Windows\Windows Error Reporting\'               = @('QueueReporting')
    '\Microsoft\Windows\Registry\'                              = @('RegIdleBackup')
    '\Microsoft\Windows\DiskFootprint\'                         = @('Diagnostics')
    '\Microsoft\Windows\Chkdsk\'                                = @('ProactiveScan')
    '\Microsoft\Windows\Servicing\'                             = @('StartComponentCleanup')
    '\Microsoft\Windows\RetailDemo\'                            = @('CleanupOfflineContent')
    '\Microsoft\Windows\Feedback\Siuf\'                        = @('DmClient', 'DmClientOnScenarioDownload')
    # Tamper Protection owns these even on an unmanaged guest, so expect FAIL rather than absence. They
    # are listed anyway: a FAIL line naming them is worth more than their omission, and a Windows build
    # that lets them go is one where the periodic full scan stops costing the host an hour of CPU.
    '\Microsoft\Windows\Windows Defender\'                      = @('Windows Defender Cache Maintenance', 'Windows Defender Cleanup', 'Windows Defender Scheduled Scan', 'Windows Defender Verification')
}
foreach ($folder in $unusedTasks.GetEnumerator()) {
    foreach ($task in $folder.Value) {
        if (-not (Get-ScheduledTask -TaskPath $folder.Key -TaskName $task -ErrorAction SilentlyContinue)) {
            Write-Host "  none  $($folder.Key)$task -- not present"
            continue
        }
        Try-Step "disable $task" {
            Disable-ScheduledTask -TaskPath $folder.Key -TaskName $task -ErrorAction Stop | Out-Null
        }
    }
}

Step 'Crash dumps'
# A bugcheck on a 16 GiB guest writes a 16 GiB dump through the host's disk before the domain restarts.
# 3 is a minidump: a few hundred KiB, still enough for `!analyze -v` to name the faulting driver.
Try-Step 'write minidumps rather than full memory dumps' {
    Set-RegistryDword 'HKLM:\SYSTEM\CurrentControlSet\Control\CrashControl' CrashDumpEnabled 3
}

# --------------------------------------------------------------------------------------------------
# Filesystem
# --------------------------------------------------------------------------------------------------
# 8.3 name generation makes NTFS create a SECOND directory entry per file and look up the short names
# already in the directory before it can pick one, which Microsoft's own fsutil reference calls out as
# the cost of leaving it on. This job creates tens of thousands of files in one JDK unpack and one
# jlink runtime, so it is the directory size that makes it worth turning off.
#
# Last-access updates are a metadata write per directory listing. Windows manages the setting itself by
# default and may decide either way; 1 pins it to "disabled" so that decision cannot come back.
#
# Neither setting is retroactive, and both need a reboot. They change what happens to files created
# and listed from the next boot on, which is why this runs before provisioning rather than after.
Step 'NTFS'
Try-Step 'stop generating 8.3 short names' { Invoke-Native fsutil.exe 'behavior','set','disable8dot3','1' }
Try-Step 'stop updating last-access timestamps' { Invoke-Native fsutil.exe 'behavior','set','disablelastaccess','1' }
# memoryusage 2 raises NTFS's paged- and non-paged-pool lookaside limits. The fsutil reference names
# exactly one workload it helps -- "opening and closing many files in the same file set" -- and exactly
# one condition that makes it backfire, a machine already short of system memory for other apps or
# cache. A 16 GiB guest that runs one build and nothing else is the first and not the second.
Try-Step 'give NTFS the larger metadata cache' { Invoke-Native fsutil.exe 'behavior','set','memoryusage','2' }

# TRIM is how a thin disk image shrinks. `discard='unmap'` in domain.xml is the host half; this is the
# guest half, and without it the image only ever grows, because nothing tells the host which blocks the
# guest has freed. NTFS enables delete notification by default, so this READS rather than sets: the
# only interesting outcome is the surprising one, where an administrator or an image has turned it off.
Step 'TRIM'
try {
    $deleteNotify = (fsutil.exe behavior query DisableDeleteNotify) -join ' '
    Write-Host "  $deleteNotify"
    if ($deleteNotify -match 'DisableDeleteNotify\s*(\(\w+\))?\s*=\s*1') {
        Write-Host '  warn  TRIM is OFF; the disk image will only ever grow. Enable it with: fsutil behavior set DisableDeleteNotify 0'
    }
} catch {
    Write-Host "  warn  could not query DisableDeleteNotify: $($_.Exception.Message)"
}

# Retrim only, never the full defrag Automatic Maintenance runs: the image is thin and fragmentation
# inside it is the host's business, while the freed-block list is the guest's. It is cheap ONLY if
# domain.xml's discard_granularity override is in place -- at the default 4 KiB granularity a retrim
# takes minutes to hours, which is why it must stay disabled without that override.
Try-Step 'weekly retrim, Sunday 03:00' {
    $retrim = New-ScheduledTaskAction -Execute 'powershell.exe' `
        -Argument '-NoProfile -ExecutionPolicy Bypass -Command "Optimize-Volume -DriveLetter C -ReTrim"'
    Register-ScheduledTask -TaskName 'RunnerRetrim' -Force `
        -Action $retrim `
        -Trigger (New-ScheduledTaskTrigger -Weekly -DaysOfWeek Sunday -At ([datetime]'03:00')) `
        -Principal (New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest) `
        -Settings (New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew) `
        -ErrorAction Stop | Out-Null
}

# --------------------------------------------------------------------------------------------------
# Defender
# --------------------------------------------------------------------------------------------------
# THE LARGEST BUILD-TIME WIN, and the one most likely to be reverted by policy. The app-image job is
# almost entirely small-file I/O -- setup-java unpacks a JDK, jpackage's jlink assembles a runtime,
# Compress-Archive reads all of it back -- and Defender scans every one of those files as it is
# written and again as it is read.
#
# PATHS MUST BE SYSTEM'S, NOT THE INTERACTIVE ACCOUNT'S. provision.ps1 runs the daemon as SYSTEM, so a
# job's %TEMP% is C:\Windows\Temp and its user profile is under system32\config\systemprofile. An
# exclusion naming `$env:USERPROFILE` describes whoever ran this script, which is nobody at build time.
#
# Tamper Protection does NOT cover exclusions on a machine that is not Intune-enrolled, so this works
# here -- but the result is read back rather than assumed, because on an enrolled or policy-managed
# guest it silently would not.
Step 'Defender exclusions for the build directories'

$systemProfile = Get-SystemProfilePath
Write-Host "  SYSTEM's profile: $systemProfile"

$excludedPaths = @(
    # provision.ps1's -InstallDir default, with its -WorkDir beneath it. Change both together.
    # It does NOT necessarily cover the runner's tool cache, which is where setup-java unpacks a
    # whole JDK: nothing here fixes that location. The README's section 7 says how to find it.
    'C:\forgejo-runner'
    "$env:SystemRoot\Temp"                    # SYSTEM's %TEMP%: where jpackage stages and installers unpack
    "$systemProfile\.gradle"                  # SYSTEM's Gradle home, for a job that does run Gradle
    "$systemProfile\.m2"
    "$systemProfile\AppData\Local\Temp"
    "$systemProfile\AppData\Local\go-build"   # Go's build cache, used while provisioning builds the runner
    "$systemProfile\AppData\Roaming\npm-cache"
)

# ExclusionProcess excludes the files a process OPENS, not the process itself, which is exactly what is
# wanted: whatever path a toolchain decides to write to is covered without this list having to predict
# it. A bare image name is matched, so no full path is needed or wanted.
#
# `powershell.exe` is the broadest entry and it is deliberate: `Compress-Archive` runs in-process, so
# the zip this job exists to produce is both read and written by PowerShell itself. It does mean a job
# step can write anything unscanned. The trade is made knowing the guest is NAT'd, unattended, runs no
# interactive user and is rebuilt from this script; drop the entry if that stops being true.
$excludedProcesses = @('java.exe', 'javaw.exe', 'javac.exe', 'jar.exe', 'jlink.exe', 'jpackage.exe',
                       'git.exe', 'node.exe', 'go.exe',
                       'forgejo-runner.exe', 'powershell.exe')

foreach ($path in $excludedPaths) {
    Try-Step "exclude path $path" { Add-MpPreference -ExclusionPath $path -ErrorAction Stop }
}
foreach ($process in $excludedProcesses) {
    Try-Step "exclude files opened by $process" { Add-MpPreference -ExclusionProcess $process -ErrorAction Stop }
}

# Archive scanning unpacks and scans the inside of every zip. This job downloads a JDK archive and
# writes a ~200 MB one, and the catch-up scans are what run a full scan the moment a machine that was
# off comes back -- which for a runner is the moment it is wanted.
Try-Step 'stop scanning inside archives and running catch-up scans' {
    Set-MpPreference -DisableArchiveScanning $true -DisableCatchupFullScan $true -DisableCatchupQuickScan $true -ErrorAction Stop
}

# READ IT BACK. Comparison is case-insensitive and ignores a trailing separator, because Defender
# normalises what it stores and an exact-string check reports a false miss.
function Test-Excluded {
    param([string] $Wanted, [string[]] $Applied)
    $normalised = $Wanted.TrimEnd('\')
    foreach ($entry in @($Applied)) { if ($entry.TrimEnd('\') -ieq $normalised) { return $true } }
    return $false
}
try {
    $preference = Get-MpPreference
    Write-Host "  Defender now excludes paths: $($preference.ExclusionPath -join ', ')"
    Write-Host "  Defender now excludes processes: $($preference.ExclusionProcess -join ', ')"
    foreach ($path in $excludedPaths) {
        if (-not (Test-Excluded $path $preference.ExclusionPath)) { Write-Host "  warn  $path did NOT stick -- policy or Tamper Protection" }
    }
    foreach ($process in $excludedProcesses) {
        if (-not (Test-Excluded $process $preference.ExclusionProcess)) { Write-Host "  warn  $process did NOT stick -- policy or Tamper Protection" }
    }
} catch {
    Write-Host "  warn  could not read back the exclusion list: $($_.Exception.Message)"
}

Write-Host "`n=== Optimisation finished ==="
Write-Host 'Reboot before provisioning: the VBS and hypervisor settings do not take effect until then.'
