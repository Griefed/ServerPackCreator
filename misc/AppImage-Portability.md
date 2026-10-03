# AppImage — what the artifact requires, and how it updates

What systems a ServerPackCreator AppImage runs on, how that was measured, how to re-measure it after a
JDK bump, and how an installed one updates itself. Written after two reports: that the AppImages are
"not self-contained, references glibc 2.15" — accurate, and the bundled JDK's floor rather than a
packaging mistake — and that they carried no update information.

The build script is [`build-appimage.sh`](build-appimage.sh); the jobs that run it are
[`devbuild.yml`](../.forgejo/workflows/devbuild.yml) and
[`release-build.yml`](../.forgejo/workflows/release-build.yml).

## The requirement, measured

Measured 2026-10-03 against Temurin `jdk-21.0.12.1+1`, the build `JDK_RELEASE` pins — every ELF in the
JDK scanned for versioned symbol references and for `DT_NEEDED` entries.

| | |
|---|---|
| **glibc floor** | **`GLIBC_2.15`**, raised by `lib/libjava.so` and `lib/server/libjvm.so` |
| other glibc versions referenced | 2.2.5, 2.3, 2.3.2, 2.3.3, 2.3.4, 2.4, 2.6, 2.7, 2.9, 2.14 |
| glibc libraries needed | `ld-linux-x86-64.so.2`, `libc.so.6`, `libdl.so.2`, `libm.so.6`, `libpthread.so.0`, `librt.so.1` |
| X11, for the Swing GUI | `libX11.so.6`, `libXext.so.6`, `libXi.so.6`, `libXrender.so.1`, `libXtst.so.6` |
| sound, for `libjsound.so` | `libasound.so.2` |
| `libstdc++` / `libgcc_s` | **not needed** — Temurin links them statically |
| C library shipped inside the AppImage | **none** |

glibc 2.15 was released in March 2012, so in practice every glibc distribution still in use satisfies
it. What does *not* satisfy it is a **musl** system (Alpine, Void-musl) or a non-FHS one (NixOS, GUIX)
without an FHS shim, because there is no glibc there at all.

**The AppImage runtime contributes nothing to this.** It is AppImage `type2-runtime`, static-pie, with
libfuse linked in and no versioned glibc references of its own. It needs `fusermount` on `PATH`, or
`--appimage-extract-and-run`. So the bundled JDK alone decides the floor, which is why
[`build-appimage.sh`](build-appimage.sh) pins `JDK_RELEASE` rather than fetching `latest/<major>/ga`.

## The guard

`build-appimage.sh` asserts the floor against the JDK **as copied into the AppDir**, so it measures the
shipped bytes rather than the download. It fails the build when the highest `GLIBC_` symbol exceeds
`JDK_MAX_GLIBC`, naming the files that raised it, and it fails rather than passes when it can read no
symbol at all — a check that cannot answer must not answer "fine".

Bumping `JDK_RELEASE` therefore shows up as a red build rather than as a user report, and
`JDK_MAX_GLIBC` is raised deliberately, in a commit that says what the new floor costs.

## Re-measuring it by hand

```sh
# The floor, and what raises it
grep -rhao 'GLIBC_[0-9][0-9.]*' <jdk-dir> | sed 's/^GLIBC_//' | sort -V -u | tail -n 1
grep -rlao 'GLIBC_2.15'         <jdk-dir>

# Everything the JDK expects from the host, bundled or not
find <jdk-dir> -name '*.so' -exec sh -c 'readelf -d "$1" | grep NEEDED' _ {} \;
```

`readelf` is Linux-side; the DT_NEEDED table above was produced by parsing the ELF dynamic sections
directly, which works from any host.

## What is deliberately NOT done

**The AppImage does not ship its own glibc, and that is a decision rather than an omission.** Bundling
one is possible — `sharun` + `uruntime` (pkgforge-dev's *Anylinux-AppImages*) is a maintained toolchain
that bundles the loader, glibc, NSS and the dlopened libraries, and uses DwarFS rather than squashfs.
It was weighed and declined for now; see `claude-docs/BACKLOG.md` for the entry and the numbers.

The short version: the floor is already 2012, glibc alone would not make the artifact self-contained
because X11 and ALSA are still host libraries, and `libnet.so` calls `getaddrinfo`, whose NSS modules
are where bundled-glibc builds characteristically break — in an application whose entire job involves
downloading modpacks.

## Updating

Both pipelines build with `--update-channel`, which embeds update information and writes the matching
`.zsync` beside the AppImage. `AppImageUpdate` reads it, fetches the `.zsync` and transfers only the
blocks that changed — which is what makes a ~256 MB artifact updatable at all.

| Built by | Channel | Resolves |
|---|---|---|
| [`release-build.yml`](../.forgejo/workflows/release-build.yml) | `latest` | the newest stable GitHub release; prereleases are skipped |
| [`devbuild.yml`](../.forgejo/workflows/devbuild.yml) | `continuous` | the rolling dev release |

A local `build-appimage.sh` run embeds nothing, deliberately: an AppImage claiming it can update itself
from a file nobody published is claiming something false.

### Updating an installed AppImage

The AppImage does **not** update itself — the runtime exposes no such flag. Updating is
[`AppImageUpdate`](https://github.com/AppImage/AppImageUpdate), a separate tool:

```sh
AppImageUpdate ServerPackCreator-9.1.0-x86_64_experimental.AppImage
```

It reads the update information out of the file, resolves the `.zsync` from the release the channel
names, and rewrites the AppImage **in place** — so the name on disk keeps saying the old version after
a successful update. That is zsync's behaviour, not a bug, and it is why the version a user reports
should come from the application rather than from the filename.

What the file itself claims can be read back without any tool but the AppImage:

```sh
./ServerPackCreator-9.1.0-x86_64_experimental.AppImage --appimage-updateinformation
# gh-releases-zsync|Griefed|ServerPackCreator|latest|ServerPackCreator-*-x86_64_experimental.AppImage.zsync
```

That is the first thing to ask for in a report about updating: an empty answer means the AppImage was
built without a channel, and a `continuous` one means the user is on the dev build and will never be
offered a stable release.

### What the `.zsync` is

A small plain-text-then-binary control file, generated at build time and published as a release asset
beside the AppImage. Measured against one this build produced — a 940 KB test AppImage, so the header
is real and the sizes are illustrative:

```
zsync: 0.6.2
Filename: ServerPackCreator-9.9.9-aarch64_experimental.AppImage
MTime: Sat, 03 Oct 2026 19:39:42 +0000
Blocksize: 2048
Length: 940552
Hash-Lengths: 2,2,4
URL: ServerPackCreator-9.9.9-aarch64_experimental.AppImage
SHA-1: 4ee3442e01c0b78f6dbab33829e13e2dcf7efdaf
```

After the header come rolling and strong checksums, one pair per block — about six bytes per 2 KiB
block, which is what lets a client work out which blocks it already has and request only the rest.
That is the whole point for an artifact this size: the bundled JDK is the bulk of it and barely changes
between releases, so a version bump transfers a fraction of 256 MB rather than all of it.

**Two fields decide whether it works, and both are checked by `build-appimage.sh` after every build:**

- **`URL:`** is *relative*, naming the AppImage next to the `.zsync`. That is what makes one release
  asset resolve the other, and it is why the two must be published together, in the same release.
- **`Length:`** and the checksums describe one exact build. A `.zsync` republished against a different
  AppImage silently fails every block match and degrades to a full download at best.

### Why the transport is GitHub's, when releases are made on Forgejo

The [AppImage spec](https://github.com/AppImage/AppImageSpec/blob/master/draft.md#update-information)
offers `zsync|<url>`, `gh-releases-zsync|<owner>|<repo>|<release>|<glob>` and `pling-v1-zsync`. The
first needs a URL that **stays stable across versions**, and Forgejo serves none — probed against the
live instance:

```
/Griefed/ServerPackCreator/releases/latest/download/checksum.txt   404
/Griefed/ServerPackCreator/releases/latest                        303 -> the tag page, not a download
```

`gh-releases-zsync` takes a release and a filename glob instead, so the version moving is fine. The
GitHub mirror carries every release asset — that is what `release-build.yml`'s `mirror` job is for —
and `devbuild.yml` publishes the `continuous` release there itself, so both channels are reachable
without new infrastructure.

**The glob is architecture-specific** (`ServerPackCreator-*-x86_64_experimental.AppImage.zsync`). A
bare `ServerPackCreator-*.AppImage.zsync` would let an x86_64 install update itself to the aarch64
build.

### Two things that will bite

**appimagetool bundles its own `zsyncmake`** (`usr/bin/zsyncmake` inside the tool's own AppImage), and
uses it. Measured in a container with no `zsync` package installed: it reported *"zsyncmake is
available"* and wrote the file. **Do not add an `apt install zsync` to the workflows** — it was tried,
on the strength of a source read, and it is a prerequisite that is not one.

**`-u` embeds the update information whether or not a `zsyncmake` can be reached, and only skips the
`.zsync`** — on stderr, exit 0. That ships an AppImage advertising an update route whose `.zsync` was
never written, and it fails on a user's machine rather than in CI. `build-appimage.sh` therefore reads
back *both* halves after the build: the update information out of the AppImage, the `.zsync` being
non-empty, and its `URL:` header naming the AppImage. Reproduced with a wrapper that exits 0 and
deletes the `.zsync`: the script exits 1 and names the missing file.

## If a user reports it anyway

Since 2026-10-03 the launcher **fails** instead of quietly using the system's Java, and prints what the
bundled JDK reported. A report should carry those lines; they distinguish a genuine floor problem
(`version GLIBC_2.x not found` from the loader) from everything else.

`SPC_ALLOW_SYSTEM_JAVA=1` restores the old fallback for anyone who needs to run it regardless.
