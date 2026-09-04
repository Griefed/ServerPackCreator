package de.griefed.serverpackcreator.api.modscanning

import java.io.File

/**
 * What one scanner made of one mod jar: which side it belongs on, and what it declared it needs.
 *
 * A scanner returns one of these per jar it was handed, whatever the outcome — a jar it could not open, or
 * one carrying no descriptor it understands, comes back with the defaults rather than being dropped. The
 * include-list is built solely from what the scanners return, so an omission here is a mod missing from the
 * finished server pack.
 *
 * There is deliberately no `equals`/`hashCode`. Scanning a directory with two scanners yields two instances
 * for one jar which may disagree on [sideness], and value-equality would let a `Set` or `distinct()` keep
 * whichever landed first and silently drop the other verdict. Merging those verdicts is a decision the caller
 * makes explicitly; compare on [file] when identifying the same jar across two scans.
 */
class ScannedMod @JvmOverloads constructor(
    /** The jar this was read from. The only identity that holds across two scans of the same directory. */
    val file: File,
    /**
     * The id the mod declares, or the filename when none could be read.
     *
     * The fallback is **not** a real mod id, and anything joining on this field should expect that: an
     * unreadable jar will not match a dependency naming the mod it actually contains. It is a filename rather
     * than a shared placeholder so that two unreadable jars do not compare equal to each other.
     */
    val modID: String = file.nameWithoutExtension,
    /**
     * Which side this mod belongs on. Defaults to [Sideness.SERVER] so a jar nothing could be determined
     * about is kept: dropping a mod that does belong on the server breaks the pack, while keeping a
     * superfluous one costs a few megabytes.
     */
    val sideness: Sideness = Sideness.SERVER,
    /** The non-platform mods this one declared it needs. The loader, Java and Minecraft are not recorded. */
    val dependencies: List<ModDependency> = emptyList(),
    /**
     * Other mod-ids this mod answers to, from a Fabric/Quilt `provides` block — empty for loaders that
     * have no such concept.
     *
     * Carried because a dependency names an id, not a jar: Fabric API 0.92.11+1.20.1 declares
     * `"id": "fabric-api"` and `"provides": ["fabric"]`, so a mod writing `depends: {"fabric": "*"}` is
     * satisfied by it. Without the alias, anything matching a dependency against a mod's own id alone —
     * `ModListCompiler`'s dependency rescue, above all — compares "fabric" to "fabric-api" and misses.
     */
    val provides: List<String> = emptyList(),
    /**
     * The Minecraft version range the descriptor itself declares, verbatim, or `null` when it declares none.
     *
     * Every scanner parses this already and used to discard it — Fabric and Quilt as an excluded "platform"
     * dependency, Forge and NeoForge by consuming the platform entry for its `side`. It is kept because it
     * answers a question nothing else can: **what did this jar say it was built for?** A platform's declared
     * version list is what its author ticked, and a boot chosen from that alone can land a jar on a Minecraft
     * whose mappings it has never seen — which fails as a mixin error that looks exactly like a crash.
     */
    val minecraftConstraint: String? = null,
    /**
     * Whether a descriptor was actually read, or this is the "nothing could be read" fallback.
     *
     * The fallback is not an error — every scanner is handed the whole mods directory, so a Fabric-only jar
     * yields one from the Quilt scanner by design. But it is **indistinguishable from a real scan by value
     * alone**: `modID` falls back to the file name, `sideness` to `SERVER`, and the lists to empty, all of
     * which a genuine descriptor could also produce. Anything *merging* two scans of the same jar therefore
     * has to be told, or it will treat "I found nothing" as "I found nothing to declare" — which is exactly
     * how a Quilt pack scan came to discard a Fabric jar's dependencies.
     */
    val descriptorRead: Boolean = false
) {
    /**
     * One line for a scan log, with the dependencies spelled out instead of left as object identities — they
     * are the part a scan log is usually being read for, and the reason this is written by hand.
     */
    override fun toString(): String {
        return "ScannedMod(file=$file, modID='$modID', sideness=$sideness, dependencies=${dependencies.joinToString(", ")})"
    }
}

/**
 * A mod named as a dependency by another, and the side that dependency is needed on.
 *
 * Only the id is known — the declaring descriptor names a mod, not a file — so matching this back to a jar
 * happens against [ScannedMod.modID].
 */
class ModDependency @JvmOverloads constructor(
    /** Id of the mod being depended on, as the declaring descriptor spells it. */
    val modID: String,
    /**
     * The side this dependency is needed on. Defaults to [Sideness.SERVER]: only Forge-style descriptors
     * state a side per dependency, so for the others every recorded dependency is one the server may need.
     */
    val sideness: Sideness = Sideness.SERVER,
    /**
     * The version constraint the descriptor spelled, **verbatim and unparsed**, or `null` when it stated
     * none. Left as written because the grammars differ per loader — Fabric and Quilt use npm-style ranges
     * (`>=0.92.0`, `^2.0.0`), Forge and NeoForge use Maven ranges (`[15.2,)`) — and a consumer that wants
     * to match one is better served by the original text than by a lossy normalisation done here.
     */
    val versionConstraint: String? = null,
    /**
     * Whether the descriptor marked this dependency as one the mod can load **without** — Forge's
     * `mandatory = false`, NeoForge's `type = "optional"` (and `"incompatible"`/`"discouraged"`, neither of
     * which is a thing to go and fetch).
     *
     * Defaults to `false`, i.e. required, which is both NeoForge's own documented default for an absent
     * `type` and the safe direction: reading a required dependency as optional boots a mod without something
     * it needs and fails as a crash, which can publish a *wrong* verdict, while reading an optional one as
     * required merely refuses a boot and learns nothing.
     */
    val optional: Boolean = false
) {
    /** One line for a scan log: the id that was depended on, the side asked for, and whether it is optional. */
    override fun toString(): String {
        return "ModDependency(modID='$modID', sideness=$sideness, versionConstraint=$versionConstraint, " +
            "optional=$optional)"
    }
}

/** Which side of a Minecraft install a mod belongs on. */
enum class Sideness {
    /** Belongs in the server pack — including "both sides", and everything undetermined. */
    SERVER,

    /** Client-only, and therefore excludable from the server pack. */
    CLIENT
}

/**
 * Reduces the sidenesses a scanner read from one descriptor to the single verdict for that mod: it belongs
 * on a server unless *nothing* it declared put it there.
 *
 * A scanner appends one entry per signal it finds — a declared environment, the side demanded of the platform,
 * the sideness of a second mod bundled in the same jar. A mod is only clientside when every one of those said
 * so, because dropping a mod that does belong on the server breaks the pack, while keeping a superfluous one
 * costs a few megabytes. An empty list therefore reads as CLIENT by construction, which is why each scanner
 * appends SERVER on the paths where it could not determine anything.
 */
internal fun sidenessOf(sidenesses: List<Sideness>): Sideness =
    if (sidenesses.any { it == Sideness.SERVER }) Sideness.SERVER else Sideness.CLIENT
