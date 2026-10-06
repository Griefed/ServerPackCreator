# Runner host — a Windows runner, for the self-contained Windows artifact

**Host configuration, not a repository change.** The workflow jobs that need this are useless without
it, and a job whose label no runner offers does not fail — it sits queued. `devbuild.yml` already
documents the symptom for the arch label it deliberately avoids: *"No matching online runner with
label"*.

Same arrangement as [`RUNNER-REGISTRY-CACHE.md`](RUNNER-REGISTRY-CACHE.md): **the host half goes first.**

## Why a Windows machine is required at all

`jpackage` **cannot cross-compile**. [JEP 392](https://openjdk.org/jeps/392) lists it as an explicit
non-goal — *"Native packages are generated using tools available on the host platform"* — so a Windows
app-image has to be produced on Windows.

This is the difference from the AppImages, and it is worth being precise about, because at first glance
they look like the same problem already solved. They are not: an AppImage is a tarball of files plus a
prebuilt runtime stub, so **nothing architecture-specific has to execute** and `build-appimage.sh`
cross-packages the aarch64 one from an amd64 runner. A jpackage app-image contains a native launcher
that jpackage builds with the host's own toolchain.

The install4j Windows **installer** likewise cross-builds from Linux today and keeps doing so. Nothing
here replaces it; this adds a second Windows artifact for people who do not want an installer.

## What to register

One runner, executing **on the host** rather than in a container:

| Label | Host |
|---|---|
| `windows-latest:host` | Windows on x86_64 |

**x86_64 only, and that is a decision rather than an omission.** A Windows ARM64 artifact would need a
native ARM64 Windows runner, and the only way to host one here is an emulated guest — there is no
acceleration for an ARM64 guest on an x86_64 CPU, so `qemu-system-aarch64` falls back to TCG and
translates every guest instruction in software. The throughput that produces is not something to gate a
release on. Windows on ARM64 runs x86_64 binaries, so that audience is served by the x86_64 artifact.

**The `:host` suffix is not optional.** Read out of the runner's own
`internal/pkg/labels/labels.go`: a label is split on `:` into `name[:scheme[:arg]]` and the scheme
**defaults to `docker`** when absent. There is no Linux container to execute a Windows job in, so the
label has to say `host` or the runner tries to start one.

### There is no published Forgejo runner binary for Windows

Checked against the last five releases of `forgejo/runner` (v12.13.1 … v13.2.0): every asset is
`linux-amd64` or `linux-arm64`, twelve per release, no Windows among them. The Makefile carries
`WINDOWS_ARCHS ?= windows/amd64`, so the build system knows the target — nothing is released for it.

It is Go, so building it is one command:

```sh
git clone https://code.forgejo.org/forgejo/runner && cd runner
GOOS=windows GOARCH=amd64 go build -o forgejo-runner.exe
```

### If the Windows machine is a container on a Linux host

[`misc/windows-runner/`](../misc/windows-runner/) has a `docker-compose.yml` for the guest on an
x86_64 Linux host, with the unattended provisioning that registers it. Its
[README](../misc/windows-runner/README.md) is the operating manual.

## What to check on the first run, because it fails the same way the container landmine does

**Forgejo Actions runs every JavaScript action by exec'ing `node`.** In `docker` scheme the runner
injects one into the job container; in `host` scheme it has to find one on the host. If it cannot,
`actions/checkout` dies with `exec: "node": executable file not found in $PATH`, exit 127, and every
later step is skipped — byte-identical to the symptom
[`.claude/rules/ci-workflows.md`](../.claude/rules/ci-workflows.md) documents for giving a job a
`container:` of a tool image, from a completely different cause.

So: install Node on the host and confirm `node --version` answers from the account the runner service
runs as, not just from an interactive shell.

**Do not reach for `winget` to install it from an unattended or service context.** It ships inside the
App Installer MSIX, which is registered per *user*, so anything running as SYSTEM — a `SetupComplete`
script, an OEM provisioning step — gets `The term 'winget' is not recognized`, which reads like a PATH
problem and is not one. `misc/windows-runner/oem/provision.ps1` downloads the official installers
instead.

**PowerShell 7 is NOT required, and the workflows must not ask for it.** Windows ships `powershell.exe`
(Windows PowerShell 5.1) and nothing else; `pwsh` is a separate install. A step declaring `shell: pwsh`
fails before it runs a line, with `Cannot find: pwsh in PATH`, and act skips the rest of the job — which
reads as the build failing rather than as a missing interpreter. The Windows jobs use
`shell: powershell`, and `misc/build-winimage.ps1` is written in the 5.1 dialect for the same reason.

Three more worth confirming before blaming a workflow:

- **`jpackage` is on `PATH`.** It ships with the JDK, and the jobs install one with `setup-java`, so
  this normally takes care of itself — but a host JDK ahead of it on `PATH` would be used instead, and
  jpackage embeds **the running JDK's** runtime into the artifact.
- **`git` is on `PATH`**, for `actions/checkout`'s fallback path.
- **Long paths are enabled** (`git config --system core.longpaths true`). The staged jlink runtime is
  deep, and Windows' 260-character default truncates it in ways that read as a corrupt artifact.

## The JDK distribution

The job uses **`distribution: 'zulu'`**, like every other `setup-java` in this repository, and here
that is consistency rather than a constraint: for `windows/x64`, Adoptium, Zulu and Microsoft all
publish JDK 21 and JDK 25 GA, so an LTS bump has a free choice. Measured 2026-10-03 against
`api.adoptium.net` and `api.azul.com`.

What jpackage embeds in the image is the runtime of the JDK the job is **running on**, so this picks
the artifact's bundled Java as well as the build's compiler.

## When the host half is done

The repository half is `build-winimage-x86_64` in
[`../.forgejo/workflows/devbuild.yml`](../.forgejo/workflows/devbuild.yml), and the `winimage` job in
[`release-build.yml`](../.forgejo/workflows/release-build.yml). Both call
[`../misc/build-winimage.ps1`](../misc/build-winimage.ps1). Run a `workflow_dispatch` dev build first:
it produces the same artifact as a release without touching one.

**The release job runs AFTER `assets`, on purpose**, and takes the jar that `assets` already built
rather than building a second one — so the jar inside the image is byte-identical to the jar the
release ships, and the frontend's npm build never has to run on Windows. The cost is that
`checksum.txt` is written before the images exist, so each one carries its own `.sha256` beside it,
the same way the devbuild jobs publish theirs. `release` and `mirror` merge the images into their
asset set; `virustotal` deliberately does not, because its loop scans `*.jar *.sh *.dmg *.exe` and
already excludes the AppImages for the same reason.

Then, on a real machine of each architecture, extract the zip and start all three launchers — the GUI,
`ServerPackCreator-CLI.exe` and `ServerPackCreator-WebService.exe`. The script already fails the build
if jpackage did not emit all three plus `runtime\bin\java.exe`, but "the files exist" and "the
application starts" are different claims and only the second one is the point of the artifact.
