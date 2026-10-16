# Call depth from the public entry points

> **What this answers:** how far a call goes, counted in hops through `de.griefed` code, starting from
> every public method an implementor could call. The expectation being tested is that a SOLID codebase
> does not go deep.
>
> **Re-run it, do not trust the numbers below.** `./gradlew build && python3 -I misc/call-depth.py`.
> It reads `build/classes`, so an out-of-date tree gives an out-of-date answer. Figures here are from
> 2026-10-11 against 949 classes / 7,111 methods / 7,634 intra-`de.griefed` edges.

## The answer

| depth | public methods | | module | deepest |
|---|---:|---|---|---:|
| 0 | 3547 | | api | 13 |
| 1 | 812 | | app | 17 |
| 2 | 355 | | clientside | 11 |
| 3 | 253 | | grinder | 10 |
| 4 | 153 | | plugin-example | 11 |
| 5 | 101 | | plugin-grinder | 5 |
| 6 | 64 | | plugin-selfextract | 7 |
| 7–9 | 96 | | plugin-servertest | 9 |
| 10–17 | 40 | | | |

**87% of 5,421 public methods bottom out within three hops**, and 136 of them — 2.5% — go deeper than
six. The hypothesis holds: nothing here is deep by accident.

Where it *is* deep, the chain is one of three named shapes rather than nesting:

- **The settings read.** `ApiProperties.getX -> <Area>Config.getX -> PropertyStore.getY` is the tail of
  most long chains. It is what took `ApiProperties.kt` from 3,007 lines to 1,372, and it is three hops
  by construction.
- **A manifest read that may fetch.** `MinecraftMeta.requiredJavaVersion -> getServer ->
  MinecraftServer.javaVersion -> setServerJson -> readServerJson -> WebUtilities.downloadFile ->
  openTimedStream -> openTimedConnection -> ApiProperties.getNetworkConnectTimeout -> …` — nine of the
  deepest thirteen hops, and the reason the GUI's config-check timer was memoised.
- **Strangler-Fig facades.** `ServerPackHandler -> ServerPackProvisioner` (1,466 → 490 lines) and the
  Kotlin default-argument overloads, which are stack frames rather than nesting. See *What it counts*.

## Two findings, neither of them a depth problem

**`ConfigurationHandler.checkConfiguration` performs a network download.** At depth 13:

```
ConfigurationHandler.checkConfiguration -> isZip -> checkManifests
  -> ModpackManifestParser.checkManifests -> updateConfigModelFromMinecraftInstance
  -> getAndSetIcon -> WebUtilities.downloadFile -> openTimedStream -> …
```

`getAndSetIcon` fetches a CurseForge thumbnail. A call documented as a *check* reaches a third-party
host. It is timeout-bounded, so it is not a hang, but it is a responsibility leak and the single best
argument this metric produces. Filed as a backlog entry rather than fixed here.

**Nothing points outward.** No chain from `-api` reaches `-app`, `-clientside`, `-grinder` or the
frontend. The inward-dependency rule holds in the bytecode, not merely in prose.

## What it counts, and what it cannot

Depth is hops through `de.griefed` only; a chain ends where it leaves our code. Four things the tool
does that a naive version gets wrong, each found by a measurement disagreeing with a hand-trace:

- **A call on `this` prints with no owner class.** `// Method run$x:(…)` rather than
  `// Method Owner.run$x:(…)`. Missing it makes every self-call invisible — which is most of the
  overload delegation in this codebase. Before this was fixed the tool reported `ServerPackHandler.run`
  at depth **0** against a hand-trace of 9, and the whole graph was short by 2,808 edges.
- **Methods are keyed by name *and descriptor*.** Keyed by name alone, the
  overload-delegates-to-overload chain Kotlin default arguments produce looks like a cycle and is
  truncated to zero.
- **`invokedynamic` is followed**, because Kotlin 2.x compiles lambdas and SAM conversions to it.
- **A call to an abstract or interface method resolves to its implementations — but only within the
  real type hierarchy.** Matching name+descriptor alone makes every data class's `hashCode()I` an
  implementation of every other's; that fabricated 23-hop chains of unrelated value types the first
  time this ran.

**Known limits, stated rather than hidden.**

- **Pass-through hops are counted.** A one-line delegating facade is a real stack frame and not real
  nesting, so a raw depth metric mildly punishes the extraction work that made this code shallower. A
  second "pass-through-excluded" figure would be the honest companion number and **is not implemented**;
  read the chains, not only the integer.
- A chain through a `fun interface` a *host* supplies (`ServerRunner`) ends at our declaration, because
  the implementation is not ours to find.
- Reflection, `@Scheduled` and pf4j extension dispatch are invisible — a plugin's own stack is not
  reachable from here at all.
- Synthetic Kotlin lambda classes appear as their own entry points and add a hop.
