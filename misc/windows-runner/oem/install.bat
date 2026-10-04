@echo off
REM Runs once, during the final step of the unattended Windows install. dockur mounts the compose
REM file's ./oem here and executes this automatically; everything beside it is available at %~dp0.
REM
REM Two scripts, two logs, in this order and deliberately not combined:
REM
REM   optimise.ps1  strips the apps a build runner does not need and tunes the guest. Nothing in it
REM                 is fatal -- a fatter or slower guest is still a working runner -- so its failure
REM                 must not prevent the step below, which is the one that matters.
REM   provision.ps1 installs node and git and registers the runner. This is what makes the guest
REM                 useful at all.
REM
REM Both logs land on the public desktop so a failure is visible over RDP without reading container
REM logs. Neither script needs the other to have run.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0optimise.ps1" ^
  > "%PUBLIC%\Desktop\runner-optimise.log" 2>&1

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0provision.ps1" ^
  > "%PUBLIC%\Desktop\runner-provisioning.log" 2>&1
exit /b 0
