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
- **`DockerJavaContainerEngine`** is the real docker-java impl (create → start → follow logs → stop →
  inspect exit → force-remove). **Not unit-tested** (needs a live daemon) — that is the whole reason
  the testable orchestration sits in `ContainerServerRunner` behind the seam. If you change it, verify
  against a real Docker daemon.

**Shutdown drain.** `DockerJavaContainerEngine` is `AutoCloseable` and force-removes the containers it
still owns, because `run`'s per-run `finally` never executes when the JVM is torn down mid-boot;
`GrinderApplication` registers that as a shutdown hook (covering the one-shot path too), and
`GrindPool.requestStop()` makes workers abandon the queue after their current candidate.
