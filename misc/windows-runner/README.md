# A Windows Forgejo runner on a Linux x86_64 host

One Windows guest as a container, because `jpackage` cannot cross-compile and so ServerPackCreator's
Windows app-image has to be built on Windows. The operational context — why this exists, what the job
does, and what to check on the first run — is in
[`../../claude-docs/WINDOWS-RUNNER.md`](../../claude-docs/WINDOWS-RUNNER.md).

| Service | Guest | Acceleration | Label | Web viewer | RDP |
|---|---|---|---|---|---|
| `windows-x64` | Windows 11 x86_64 | KVM | `windows-latest:host` | `8006` | `3389` |

**Both ports are bound to `127.0.0.1` on the Docker host and are not reachable from anywhere else.**
See *Connecting from your workstation* below — that is deliberate, not an oversight to work around.

## x86_64 only, deliberately

There is no ARM64 guest here, and that is a decision rather than something missing. **An ARM64 guest on
an x86_64 CPU gets no acceleration at all:** KVM virtualises, it does not translate instruction sets,
so `qemu-system-aarch64` falls back to TCG — `-accel tcg,thread=multi` with `-cpu cortex-a76`, read out
of `qemus/qemu-arm`'s `proc.sh` — and every guest instruction is translated in software. That is a
difference in kind rather than degree: hours for the unattended Windows install alone, and a jpackage
run close enough to `release-build.yml`'s job timeout that a release would be gated on emulation
throughput.

Windows on ARM64 runs x86_64 binaries, so that audience is served by the x86_64 artifact.

## There is no published Forgejo runner for Windows — cross-compile it

Checked against the last five releases of `forgejo/runner` (v12.13.1 … v13.2.0): every asset is
`linux-amd64` or `linux-arm64`. The Makefile knows `WINDOWS_ARCHS ?= windows/amd64`, but nothing is
released for it.

It is Go, so one command on the Linux host produces it:

```sh
git clone https://code.forgejo.org/forgejo/runner && cd runner
GOOS=windows GOARCH=amd64 go build -o forgejo-runner.exe
```

The JDK `provision.ps1` installs is what matters for the artifact, not this binary: that JDK's runtime
is what `jpackage` embeds into the app-image.

## Connecting from your workstation

The host is normally headless and remote, and the ports are published on `127.0.0.1` only, so nothing
here is reachable across the network by design. **Port 8006 is an unauthenticated console to a Windows
VM** — anyone who can open it is sitting at the machine, before Windows has a login screen and after.
Publishing it on `0.0.0.0` on a remote host hands that out to whatever can route to it.

So the route in is an SSH tunnel, which needs nothing installed on the host:

```sh
ssh -N -L 8006:127.0.0.1:8006 -L 3389:127.0.0.1:3389 you@runner-host
```

Leave it running, and the guest is at `localhost` on your own machine. Identical on macOS and Linux;
`-N` means "forward only, no shell".

| What | Where, once the tunnel is up |
|---|---|
| Web viewer | <http://localhost:8006> |
| RDP | `localhost:3389` |

The **web viewer is the only thing that works during installation** — there is no RDP service until
Windows is up — so that is what you watch an install with, and what you use if a guest stops booting.

For RDP afterwards, user `runner` and the password from `.env`:

- **macOS** — [Windows App](https://apps.apple.com/us/app/windows-app/id1295203466), which replaced
  Microsoft Remote Desktop in May 2025. Add a PC with hostname `localhost:3389`.
- **Linux** — Remmina, or FreeRDP directly. The binary is `xfreerdp` or `xfreerdp3` depending on the
  distribution's packaging:

  ```sh
  xfreerdp3 /v:localhost:3389 /u:runner /p:"$WINDOWS_PASSWORD" /dynamic-resolution
  ```

**RDP's UDP transport does not survive the tunnel**, because `ssh -L` forwards TCP only. The client
negotiates TCP and works; it is only slightly less smooth over a slow link.

Worth a one-line `~/.ssh/config` entry, since this is a thing you do repeatedly:

```
Host runner-host
    HostName <ip or name>
    User <you>
    LocalForward 8006 127.0.0.1:8006
    LocalForward 3389 127.0.0.1:3389
```

Then `ssh -N runner-host` is the whole command.

If the host is already on a WireGuard or Tailscale network you share, binding the ports to that
interface instead of `127.0.0.1` is the other reasonable answer — the point is that *something*
authenticates before port 8006 does, because port 8006 never will.

## Setup

```sh
cd misc/windows-runner
mkdir -p shared storage-x64
echo "WINDOWS_PASSWORD=$(head -c 18 /dev/urandom | base64)" > .env   # compose refuses to start without it

cp /path/to/forgejo-runner.exe shared/
printf '%s' 'windows-latest:host' > shared/runner-labels.txt

# Settings -> Actions -> Runners -> "Create new runner" shows a UUID and a Token. Both, verbatim:
printf '%s' '<uuid>'  > shared/runner-uuid.txt
printf '%s' '<token>' > shared/runner-token.txt

docker compose up -d windows-x64
```

Watch the install at <http://localhost:8006> through the tunnel from the previous section. It takes a
while even with KVM, and the web viewer is the only way to see it: there is no RDP service until
Windows is up.

`oem/install.bat` runs at the end of the unattended install and calls two scripts, each writing its own
log to the public desktop so a failure is visible over RDP without reading container logs:

| Script | Does | Log |
|---|---|---|
| `oem/optimise.ps1` | removes the apps a runner does not need, tunes the guest | `runner-optimise.log` |
| `oem/provision.ps1` | installs Node and Git, writes the runner config, starts it at boot | `runner-provisioning.log` |

Nothing in `optimise.ps1` is fatal — a fatter guest is still a working runner — so it cannot stop the
step that matters.

**Do not also set a `COMMAND` in the compose file.** dockur does not run it separately: it *appends*
the variable to the `install.bat` it finds in `/oem`, so anything there lands after that file's
`exit /b 0` and can never run. dockur's linter reports it on every boot as
*"Unreachable code after EXIT or GOTO (E008)"*. `oem/install.bat` is the entry point; `COMMAND` is the
same mechanism reached a worse way. **No JDK is installed**: every job that needs one uses `actions/setup-java`, so one
baked in here would be a second, unused copy that drifts from the one the build actually uses.

### What gets removed, and the bigger lever

`optimise.ps1` removes OneDrive, Copilot, Feedback Hub, Clipchamp, News, Teams, To Do, Outlook, Power
Automate, Quick Assist, Solitaire, Sound Recorder, Sticky Notes, Weather, Media Player, Movies & TV,
Paint and the Xbox stack — both the installed copies and the *provisioned* ones, which is what stops
them reappearing for the next user created. It also sets the policies that prevent Windows
re-acquiring them, disables Search indexing, SysMain, telemetry and hibernation, puts the guest on the
high-performance power scheme, and adds Defender exclusions for the build directories.

Apps are matched **by pattern, and every pattern reports what it matched**, because Microsoft renames
these — the Xbox app became `Microsoft.GamingApp`, Teams became `MSTeams`. A list of exact names fails
silently; a pattern matching nothing is logged as such, which distinguishes "already absent" from "the
name moved". Framework packages are protected by name and can never be caught.

**Why this pass is needed at all, when a leaner edition exists.** Windows 11 LTSC ships without the
Microsoft Store, Game Bar, Feedback Hub and the rest of the consumer set, which would make most of
`optimise.ps1` redundant, and it is 4.7 GB against Pro's 7.9 GB. **It is also Evaluation media, and
expires.**

Every "lean" alias dockur offers — `11l` (LTSC), `11e` (Enterprise), `11i` (IoT) — resolves to an id
ending `-eval`. `define.sh` rewrites `11l` to `win11x64-enterprise-ltsc-eval`; `mido.sh` routes any
`-eval` id through `downloadWindowsEval`, which fetches from
`microsoft.com/en-us/evalcenter/download-…`. Evaluation Windows stops after 90 days and **no product
key activates it** — the edition itself is not licensable. Only `VERSION: "11"` takes the other
branch, `downloadWindows` → `microsoft.com/en-us/software-download/windows11`, which is retail media.

So the guest installs **Windows 11 Pro**, and `optimise.ps1` is the first line of defence rather than
the second. Left unactivated, retail Windows 11 runs indefinitely: a desktop watermark and a locked
*Settings → Personalization*, no shutdowns and no deadline. Neither reaches a headless build runner.

Two ways to do better, if you want them:

- **A licence.** Set `KEY` to a 25-character product key in the service's `environment`. dockur runs
  `slmgr /ipk` with it during setup (`answer.sh`, `updateProductKey`), which installs the key without
  activating; activate afterwards as you would on any machine.
- **Your own media.** Bind a volume-licensed LTSC or Enterprise ISO at `/custom.iso` — see *Reusing
  the ISO* below. That bypasses `VERSION` entirely and is the only route to a **non-evaluation** LTSC
  guest, which is the configuration this section would otherwise recommend.

### How registration works

**Registration here is a configuration file, not a command.** *Settings → Actions → Runners →
"Create new runner"* shows a **UUID** and a **Token**, and Forgejo's documentation says to copy them
into the runner configuration's `server` section. `provision.ps1` does exactly that and nothing else:

```yaml
server:
  connections:
    forgejo:
      url: https://git.griefed.de
      uuid: <from the dialog>
      token: <from the dialog>
      labels:
        - windows-latest:host
```

That is what `forgejo-runner register`'s own deprecation warning means by *"declare connections in the
runner configuration instead"*. The older `register` route, which consumes a separate single-use
registration token, is deliberately not supported — it is a second mechanism for no gain, and the one
it replaces hands you both values at once.

Labels are declared **on the connection**: a connection with none falls back to `runner.labels`, and
being explicit keeps one guest's labels from ever being read as another's.

The guest reads those three files from `./shared` at provisioning time, so write them before starting
it.

### Two things that will go wrong

**A runner listed as OFFLINE with a "last online" time a few minutes old** means it connected once and
then stopped. `forgejo-runner daemon` is an ordinary console program — the runner implements no Windows
service support — so registering it with `sc.exe create` makes the Service Control Manager start it,
wait for a status report that never comes, and kill it after about 30 seconds. That is why it runs as a
**scheduled task at startup** here instead, under SYSTEM, via `run-runner.cmd`. `provision.ps1` checks
the process is still alive ten seconds after starting it, so this fails loudly rather than looking like
a successful provisioning.

Its output is in `C:\forgejo-runner\daemon.log`, and `C:\forgejo-runner\run-runner.cmd` can be run by
hand to see the daemon without the scheduler in the way.

**A runner that is online and never takes a job** is usually a label nobody is asking for, or a UUID
and Token that are not the pair the dialog showed together. `provision.ps1` reports the length and a
masked form of each value it read, so compare those against the dialog before suspecting the network.

**Git or Node "not found" immediately after being installed.** An installer writes the machine `PATH`
in the registry and cannot update a process that is already running, so re-reading the registry is
necessary and was not sufficient. The script now also asserts the install directories itself and calls
`git` by resolved full path. On an older copy of this script, relaunching the shell and re-running was
the workaround.


## Re-running the provisioning

`oem/provision.ps1` is safe to run again, and that is the fix for a guest that came up but never
appeared as a runner. Over RDP or the web viewer, as Administrator:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:\OEM\provision.ps1
```

Read `C:\Users\Public\Desktop\runner-provisioning.log` first — it says which step failed.

## Reusing the ISO instead of re-downloading it

The installation media is 7.9 GB, and by default it is downloaded **again after every reinstall** — dockur sets `REMOVE: "Y"`, deleting the ISO once the install finishes, so there is
nothing left to reuse. The compose file overrides that to `"N"`, which keeps it; the two steps below
then move it somewhere `storage-<arch>` being cleared cannot reach.

**Binding an ISO onto a guest that is already installed REINSTALLS it.** dockur decides whether to
install by comparing the media it recorded against the media it has now (`needsInstall`), and a path
change counts: you get *"Detected that your custom .iso file was changed, a backup of your previous
installation will be saved"* and the whole unattended install again. The only carve-out in that
comparison is *removing* custom media after a completed install, which is ignored — adding or changing
it is not.

So the order that costs nothing:

- **If you already have an ISO**, bind it before the guest is ever started. Nothing is downloaded and
  nothing is reinstalled.
- **If you do not**, let the first guest download one, copy it out, and bind it on the *next* guest or
  at the next deliberate rebuild. Switching a working guest over buys the saving at the price of one
  reinstall.

Copying it out, per architecture, after a guest has finished installing:

```sh
mkdir -p iso
cp storage-x64/win11x64.iso iso/win11-x64.iso        # see the naming note below
```

**The file is named after the RESOLVED version, not the alias you set.** `install.sh` builds it as
`"${VERSION//\//}.iso"` *after* `define.sh` has rewritten the alias, so `VERSION: "11"` leaves
`win11x64.iso` rather than `11.iso`. `ls storage-x64/*.iso` settles it without guessing.

Then uncomment the matching line in `docker-compose.yml`:

```yaml
- ./iso/win11-x64.iso:/custom.iso
```

From then on nothing is downloaded: a bound `/custom.iso` is used in place of any download and
**`VERSION` is ignored entirely**. Clearing `storage-x64` to reinstall then costs no bandwidth.

**Uncomment it only once the file exists.** Docker creates a *directory* at a bind source that is
missing, and a directory at `/custom.iso` fails confusingly rather than clearly — which is why the
line ships commented rather than as something to delete later.

`iso/` is gitignored — gigabytes, and Microsoft's to distribute rather than ours.

Supplying your own ISO works the same way and skips step one entirely. It is also how you pin an
exact build rather than taking whatever `VERSION` currently resolves to, and — per *What gets removed*
above — the only way to run a **licensed** LTSC guest, since every LTSC alias dockur resolves is
Evaluation media.

## What survives a `docker compose down`

Everything. `./storage-<arch>` holds the guest's installed disk image, the ISO, `setup.img` and
`windows.base`, bind-mounted, so an image update, a host reboot or a `down && up` recreates only the
*container* — the Windows install, the runner and its registration are all still there.

To deliberately start over — a Windows version bump, or a guest you have broken past repairing —
comment the `/storage` line out of the service in `docker-compose.yml` and bring it up again. Expect
the whole unattended install, and a fresh registration: **the old runner stays listed on Forgejo as
offline until you delete it there**, which is easy to miss and looks like the new one failing to
register. If you have done the ISO step above, that reinstall costs no download.

## Afterwards

Confirm both appear under *Settings → Actions → Runners* with the right labels **before** relying on
them. A label no runner offers does not fail a job, it queues it — which in this repository stalls the
dev build's continuous release and the tagged release, by design, because a release quietly short of
two artifacts is worse.
