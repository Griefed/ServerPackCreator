<!-- Loads when Claude works with files in this package. Module-wide context (package map, the
cross-cutting landmines, remaining work) lives in serverpackcreator-grinder/CLAUDE.md. -->

# grinder.container — the isolated-container runtime

The boot seam: the grinder implements clientside's `ServerRunner` for containers, so `BootVerifier` and
`BootLogClassifier` are reused verbatim and only the *execution* is swapped.

- **`ContainerServerRunner`** implements `clientside.ServerRunner` — boots a prepared pack in a
  container instead of a host process, so it slots into `BootVerifier` unchanged and feeds the same
  `BootLogClassifier` via `BootVerifier.outcomeFor`. Host-side staging (start-script check, write
  `eula.txt`, assemble the `ContainerSpec`) + mapping the engine's raw output onto `RunResult` live
  here, so it is unit-tested with a fake engine.
- **`ContainerEngine`** is the thin docker boundary (`ContainerSpec` → `ContainerRunOutput`), the same
  injectable-seam pattern as clientside's `HttpFetcher`. **`ContainerSpec` carries the untrusted-mod
  hardening as defaults**: `networkMode=none`, `readonlyRootfs`, `dropAllCapabilities`,
  `noNewPrivileges`, non-root `user`, tmpfs for `/tmp`, plus memory/cpu/pids caps. **Never mount the
  Docker socket into a boot container.**
- **`ContainerResources` is set in cores, via `forCpus`** — `SPC_GRINDER_CPUS` (default `2`) is read in
  `GrinderApplication` and reaches both the mod boot and the loader install; `CpuLimitWiringTest` pins that
  join, because `main` boots Docker and no test can execute it. **Send the quota and the period together.**
  A quota is a fraction of a period, so `withCpuQuota` alone leaves the real cap at whatever the daemon's
  default period makes it — measured against Docker 29.7.2 with the period dropped, a requested 1.5 cores
  arrived in the container's cgroup as `75000 100000`, i.e. 0.75 cores, with nothing reporting a problem.
  `theCpuCapReachesTheKernelWithItsPeriod` reads it back *from inside* the container for that reason (docker
  echoing a `HostConfig` only proves transmission) and deliberately uses a non-default 50ms period, since at
  the kernel's own 100ms the assertion would pass with the period never sent.
  **The uncapped sentinel is decided on the input, never on the computed quota** — a count that rounds away
  to 0µs is still a request for a cap, and quota `0` is docker's "no limit" (measured: the cgroup then reads
  `max 100000`), so the two zeroes must not be conflated. Audit iteration 23 found exactly that inversion.
  An *over*-large value needs no clamp: the raw cfs path is not bounded by host CPU count the way docker's
  `--cpus` is (measured on 16 cores, a 1000-core quota is accepted verbatim), it simply means uncapped.
- **`forLimits` is the entry point's one call for the whole per-container budget** (`SPC_GRINDER_CPUS` +
  `SPC_GRINDER_MEMORY_GIB`), and the memory half follows the CPU half's rules exactly — exact `0` uncapped,
  a smaller positive value raised to the daemon's floor (6 MB, its own message). **Do not treat the memory
  cap as a throughput knob.** The grinder's packs leave `javaArgs` empty, so nothing passes `-Xmx` and the
  JVM sizes the server's heap from the cgroup limit: measured on Temurin 21, `--memory=3g` →
  `MaxHeapSize 805306368` (768 MiB, 25%), `--memory=1g` → `268435456`. It is simultaneously the divisor in
  README §5's worker-sizing formula, so both directions of change end in OOM kills that are scored
  INCONCLUSIVE — the failure mode that looks like a hanging mod rather than a mis-set host.
  `ContainerLimitsWiringTest` asserts both knobs reach both collaborators, and that `main`'s fallbacks
  resolve to exactly the `ContainerResources` defaults every other construction site falls back to.
- **A boot container has a fixed, *resolvable* hostname** (`CONTAINER_HOST_NAME` = `spc-grinder`, set with
  `withHostName` and mapped to `127.0.0.1` with `withExtraHosts`). The daemon writes an `<ip> <hostname>` line
  into `/etc/hosts` only for a container that *has* an address, and `--network none` has none — so a container
  could not resolve its own name, and the first thing a Minecraft server does is ask for it: log4j calls
  `InetAddress.getLocalHost()` while configuring itself, so every boot opened with three
  `UnknownHostException: <container-id>: Temporary failure in name resolution` stacktraces before a mod was
  touched. **The name has to be fixed rather than the daemon's default**, because the mapping is part of the
  create call and the container id does not exist until after it. Measured against docker 29.7.2 under
  `--network none`: `wget: bad address '<id>'` before, `can't connect to remote host (127.0.0.1)` after — i.e.
  `getaddrinfo` now succeeds, and `--add-host` is honoured with no network at all, which is what makes this
  possible without granting the boot one. `theContainersOwnHostnameResolvesWithoutANetwork` asserts it through
  `wget` (the same `getaddrinfo` the JVM calls) rather than by reading `/etc/hosts`, which would only show that
  a line was written.
- **The boot tmpfs is mounted `exec` (`TMPFS_OPTIONS`), and that is a decided trade-off, not an oversight.**
  Docker mounts a `--tmpfs` `nosuid,nodev,noexec` and the rootfs is read-only, so nothing inside a boot could
  write a shared object and map it executable. Griefed approved granting `exec` on 2026-08-24 once the cost was
  measured, and the measurement is the reason: the report that raised it looked cosmetic
  (`Could not initialize class com.sun.jna.Native` in a crash report), but booting a real server under the
  actual posture showed **every boot the grinder ever ran silently lost Netty's native epoll transport**:

  | `/tmp` | boot console |
  |---|---|
  | `rw` | `NativeLibraryLoader: /tmp/libnetty_transport_native_epoll_….so exists but cannot be executed … check volume for "noexec" flag` → `Using default channel type` |
  | `rw,exec` | `Using epoll channel type` |

  JNA directly, production posture otherwise unchanged: `rw` gives
  `UnsatisfiedLinkError: /tmp/jna….tmp: failed to map segment from shared object`, `rw,exec` gives
  `JNA-OK pointerSize=8`.
  **What is given away, and what is not.** A mod can run a native binary it wrote into `/tmp` — against a
  workload that is already an untrusted JVM, i.e. an arbitrary-code execution engine, in a container with no
  network, no capabilities, no privilege escalation, a read-only rootfs and a non-root user, none of which
  changed. **`nosuid` and `nodev` are still applied**, which was verified rather than assumed: `rw,exec` and
  `rw,nosuid,nodev,exec` both produce `rw,nosuid,nodev,relatime`. So do not "restore" `noexec` believing it
  costs nothing, and do not widen the grant further.
  `aBootCanExecuteFromItsTmpfsWhileKeepingTheRestOfItsHardening` pins both halves — it **executes** a binary out
  of `/tmp` (the flag is the mechanism, running the file is the promise) and then asserts `nosuid`/`nodev`
  survived.
- **`DockerJavaContainerEngine`** is the real docker-java impl (create → start → follow logs → stop →
  inspect exit → force-remove). **Not unit-tested** (needs a live daemon) — that is the whole reason
  the testable orchestration sits in `ContainerServerRunner` behind the seam. If you change it, verify
  against a real Docker daemon.

**Shutdown drain.** `DockerJavaContainerEngine` is `AutoCloseable` and force-removes the containers it
still owns, because `run`'s per-run `finally` never executes when the JVM is torn down mid-boot;
`GrinderApplication` registers that as a shutdown hook (covering the one-shot path too), and
`GrindPool.requestStop()` makes workers abandon the queue after their current candidate.
