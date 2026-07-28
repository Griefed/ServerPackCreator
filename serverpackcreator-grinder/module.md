# Module serverpackcreator-grinder

**A standalone, fire-and-forget service that boot-verifies mods at scale.** It reuses the
`serverpackcreator-clientside` engine but swaps the host-process boot for an **isolated, network-less
Docker container per mod**, so many candidates can be ground through in parallel and an untrusted mod
can never touch the host. Depends on `serverpackcreator-clientside` (and through it `-api`) plus
docker-java; it carries no Spring or Swing.

# Package de.griefed.serverpackcreator.grinder

## ELI5: what is this?

The clientside engine can already boot one mod and tell you whether the server crashes (the strongest
"this mod is client-only" signal). But booting a Minecraft server takes minutes, and there are tens of
thousands of mods — doing them one-at-a-time on the host would take forever and a malicious mod would
run with your user's privileges. The grinder fixes both: each boot runs in its **own container**,
**resource-capped** and with **no network**, so you can run a pile of them at once and leave the
service running for days to accumulate a list of suspected-clientside mods.

## What's here so far (the boot seam)

- [ContainerServerRunner][de.griefed.serverpackcreator.grinder.container.ContainerServerRunner] — a
  `de.griefed.serverpackcreator.clientside.ServerRunner` that boots a prepared server pack inside a
  hardened container instead of as a host process. It drops straight into `BootVerifier` and feeds the
  same `BootLogClassifier`. Host-side staging (start-script check, eula, spec assembly) and result
  mapping live here; the container interaction is delegated so this is unit-testable.
- [ContainerEngine][de.griefed.serverpackcreator.grinder.container.ContainerEngine] — the thin, mockable
  boundary over the container runtime (`ContainerSpec` in, captured lines + exit code out), with
  [ContainerSpec][de.griefed.serverpackcreator.grinder.container.ContainerSpec] /
  [ContainerResources][de.griefed.serverpackcreator.grinder.container.ContainerResources] /
  [BindMount][de.griefed.serverpackcreator.grinder.container.BindMount] carrying the untrusted-mod hardening
  defaults (no network, read-only rootfs, dropped capabilities, non-root, pid/cpu/memory caps).
- [DockerJavaContainerEngine][de.griefed.serverpackcreator.grinder.container.DockerJavaContainerEngine] — the
  real docker-java translation (create → start → stream → stop → inspect → remove). Exercised only
  against a live daemon; the testable orchestration is deliberately kept out of it.

## Still to build

The container runner is the foundation; the fire-and-forget service still needs: a per-`(loader,
loaderVersion, minecraftVersion)` **pre-bake cache** of the installed loader + libraries (so each
mod-boot runs offline under `--network none`), a **popularity-ranked work queue** + bounded **worker
pool** (parallelism ≈ host-RAM / per-boot-memory), and a **verdict store** the results accumulate into
for the sortable/CSV-exportable table.
