<#
.SYNOPSIS
    Strip a Windows guest down to what a build runner needs, and tune it for throughput.

.DESCRIPTION
    Run by install.bat before provision.ps1, and safe to re-run by hand:

        powershell -NoProfile -ExecutionPolicy Bypass -File C:\OEM\optimise.ps1

    NOTHING HERE IS FATAL. A guest that is merely fatter or slower than intended is still a working
    runner, so every action is guarded and reported rather than allowed to abort provisioning -- which
    is the step that actually matters. Read the log to see what did and did not happen.

    THIS PASS IS LOAD-BEARING, because the leaner editions are not an option. docker-compose.yml
    installs `VERSION: "11"` -- Windows 11 Pro -- which ships the whole consumer app set, so every
    pattern below has something to find. The editions that would have shipped without it are all
    Evaluation media in dockur and expire after 90 days with no key able to activate them; the compose
    file has the dispatch. On a guest built from licensed LTSC media bound at /custom.iso most of the
    removal pass correctly reports matching nothing, and then earns its place as the guard that
    notices when an edition starts shipping something new.

    APPS ARE MATCHED BY PATTERN, NOT BY EXACT NAME, and every pattern reports what it matched. Microsoft
    renames these: the Xbox app became `Microsoft.GamingApp`, Teams became `MSTeams`, Media Player is
    still `Microsoft.ZuneMusic`. A hardcoded list of exact names fails SILENTLY -- it removes nothing and
    says nothing -- so a pattern that matches zero packages is logged as such, which is the difference
    between "already absent" and "the name moved and nobody noticed".
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
    New-Item 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\CloudContent' -Force | Out-Null
    Set-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\CloudContent' DisableWindowsConsumerFeatures 1 -Type DWord
}
Try-Step 'disable OneDrive by policy' {
    New-Item 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\OneDrive' -Force | Out-Null
    Set-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\OneDrive' DisableFileSyncNGSC 1 -Type DWord
}
Try-Step 'turn off Copilot by policy' {
    New-Item 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsCopilot' -Force | Out-Null
    Set-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsCopilot' TurnOffWindowsCopilot 1 -Type DWord
}

# --------------------------------------------------------------------------------------------------
# Performance
# --------------------------------------------------------------------------------------------------
Step 'Power'
# Reclaims hiberfil.sys, which is sized against RAM -- 8 GB of the disk image, for a machine that must
# never sleep anyway.
Try-Step 'disable hibernation' { powercfg.exe /hibernate off }
Try-Step 'high performance power scheme' { powercfg.exe /setactive SCHEME_MIN }
Try-Step 'never sleep or blank the display' {
    powercfg.exe /change standby-timeout-ac 0
    powercfg.exe /change monitor-timeout-ac 0
    powercfg.exe /change disk-timeout-ac 0
}

Step 'Services a headless build runner does not use'
# WSearch indexes a disk whose contents are rebuilt every job; SysMain prefetches on a VM where the
# guess is wrong; DiagTrack and WerSvc send telemetry nobody reads. Each is disabled rather than
# stopped, so it does not come back on reboot.
#
# DoSvc (Delivery Optimization) is NOT in this list, deliberately: its service key is ACL-protected and
# `Set-Service` answers `Access is denied` even to an administrator. Its policy is below, which is the
# supported lever and reaches the same end.
foreach ($service in 'WSearch', 'SysMain', 'DiagTrack', 'WerSvc') {
    Try-Step "disable $service" {
        Stop-Service $service -Force -ErrorAction SilentlyContinue
        Set-Service  $service -StartupType Disabled -ErrorAction Stop
    }
}

# `DODownloadMode = 0` is HTTP-only: downloads still work, peer-to-peer sharing does not happen. The
# policy key takes priority over the Settings UI, which is why it is set here rather than the service.
Try-Step 'stop Delivery Optimization sharing with peers' {
    New-Item 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' -Force | Out-Null
    Set-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\DeliveryOptimization' DODownloadMode 0 -Type DWord
}

Step 'Visuals and housekeeping'
Try-Step 'visual effects: best performance' {
    New-Item 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VisualEffects' -Force | Out-Null
    Set-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VisualEffects' VisualFXSetting 2 -Type DWord
}
Try-Step 'disable System Restore' { Disable-ComputerRestore -Drive "$env:SystemDrive\" }
Try-Step 'disable the scheduled defrag task' {
    Disable-ScheduledTask -TaskPath '\Microsoft\Windows\Defrag\' -TaskName 'ScheduledDefrag' -ErrorAction Stop | Out-Null
}

# The largest single build-time win, and the one most likely to be reverted by policy: Defender scans
# every file Gradle and jpackage write, and they write tens of thousands. Tamper Protection does NOT
# cover exclusions on a machine that is not Intune-enrolled, so this works here -- but the result is
# read back rather than assumed, because on an enrolled or policy-managed guest it silently would not.
Step 'Defender exclusions for the build directories'
$excluded = @('C:\forgejo-runner', "$env:SystemDrive\actions-runner", "$env:USERPROFILE\.gradle")
foreach ($path in $excluded) {
    Try-Step "exclude $path" { Add-MpPreference -ExclusionPath $path -ErrorAction Stop }
}
try {
    $applied = (Get-MpPreference).ExclusionPath
    Write-Host "  Defender now excludes: $($applied -join ', ')"
    foreach ($path in $excluded) {
        if ($applied -notcontains $path) { Write-Host "  warn  $path did NOT stick -- policy or Tamper Protection" }
    }
} catch {
    Write-Host "  warn  could not read back the exclusion list: $($_.Exception.Message)"
}

Write-Host "`n=== Optimisation finished ==="
