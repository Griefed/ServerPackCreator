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

It is Go, so building it is one command — and `misc/windows-runner/provision.ps1` runs that command
**in the guest**, rather than cross-compiling it on the Linux host and handing the binary over. The
cross-compiled route worked and is not what is wrong with it: the binary arrived on a shared drive with
no record of which commit produced it, so "which runner is this" had no answer. Built in the guest, the
provisioning log names the commit.

It reports its version as `dev` either way, because the Makefile stamps that in and a plain `go build`
does not — which is why the Windows runner's behaviour has to be **measured** rather than compared
against the v12.1.2 floor the reusable-workflow design depends on. See
[`../.claude/rules/ci-workflows.md`](../.claude/rules/ci-workflows.md).

### The guest is a libvirt domain, not a container

[`misc/windows-runner/`](../misc/windows-runner/) is the operating manual: host packages,
[`domain.xml`](../misc/windows-runner/domain.xml) and what in it is load-bearing, the one manual step
Windows setup needs, and the provisioning script.

**It was a `dockurr/windows` container until 2026-10-09.** What ended that is worth recording, because
it is not a preference: during run 1005 the app-image job sat in `git checkout` for 38 minutes with no
output, **and the container's own web console on port 8006 was unresponsive at the same time**. A stuck
console means the stall is below Windows, so nothing inside the guest could have been the fix, and the
container offered no way to ask the question — where a libvirt domain answers it with
`virsh domstate --reason`, `domjobinfo` and `domblkstat` in seconds.

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
problem and is not one. `misc/windows-runner/provision.ps1` downloads the official installers
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

The repository half is the **`winimage` job of
[`../.forgejo/workflows/artifacts-reusable.yml`](../.forgejo/workflows/artifacts-reusable.yml)**, which
both `devbuild.yml` and `release-build.yml` reach by calling that workflow — there is one definition
now, not one per pipeline. It calls [`../misc/build-winimage.ps1`](../misc/build-winimage.ps1).
Rehearse it by dispatching the artifact workflow, which publishes nothing.

Its checkout is **sparse** — `misc`, `.forgejo` and `img`, 182 files of 2570 — because this job needs
fifteen of them and a full checkout is what hung on the container.

**It no longer takes a `-JarPath`.** The release job used to run after `assets` and be handed the
released jar's path through a step output, which a PowerShell encoding bug silently emptied; building
the jars once upstream gives the same byte-identity with the jobs running in parallel and nothing to
hand over. The paragraph below describes the arrangement that replaced:

<details><summary>How it worked before the shared artifact workflow</summary>

The release job ran AFTER `assets` and took the jar that `assets` already built
rather than building a second one — so the jar inside the image is byte-identical to the jar the
release ships, and the frontend's npm build never has to run on Windows. The cost is that
`checksum.txt` is written before the images exist, so each one carries its own `.sha256` beside it,
the same way the devbuild jobs publish theirs. `release` and `mirror` merge the images into their
asset set; `virustotal` deliberately does not, because its loop scans `*.jar *.sh *.dmg *.exe` and
already excludes the AppImages for the same reason.

</details>

Two of those statements have since stopped being true, which is why they are folded away rather than
deleted: `stage` runs after `winimage` now, so `checksum.txt` **does** cover the Windows app-image, and
`release` and `mirror` no longer merge the images in separately because `release-assets` already holds
them.

Then, on a real machine of each architecture, extract the zip and start all three launchers — the GUI,
`ServerPackCreator-CLI.exe` and `ServerPackCreator-WebService.exe`. The script already fails the build
if jpackage did not emit all three, their `.cfg` files, the jar, the VM at
`runtime\bin\server\jvm.dll`, the module image at `runtime\lib\modules` and the runtime's own
`runtime\bin\java.exe`. But "the files exist" and "the application starts" are different claims and
only the second one is the point of the artifact.

**`runtime\bin\java.exe` exists only because the script passes its own `--jlink-options`.** That
argument replaces jpackage's four defaults wholesale, and the script leaves two of them out so this
artifact diagnoses a user's problem as well as the AppImage and the install4j build do — both of
those bundle a whole Adoptium JDK. `--strip-native-commands` goes because it deletes every launcher
from the runtime, and SPC fills its Java-for-modloader-server setting from `java.home\bin\java`
(`SystemUtilities.acquireJavaPathFromSystem`), which a stripped runtime answers with a file that does
not exist. `--strip-debug` goes because it costs every JDK frame in every stack trace its file and
line — `(Unknown Source)` — and a bug report's trace is mostly JDK frames.

Measured on Temurin 21.0.5, one jpackage run per variant: the launchers are 1,108,928 bytes across 22
stubs, the debug attributes 16.4 MiB, and `lib/modules` is byte-identical whichever way the two
exclusion flags fall. `--no-man-pages` and `--no-header-files` stay: the 8 JNI/JVMTI headers they
exclude total 211,615 bytes of build-time material that the JVM never opens.
