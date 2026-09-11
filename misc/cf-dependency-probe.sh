#!/usr/bin/env bash
#
# Grinder deploy + diagnosis helper. Three checks, one script:
#
#   deps    Does CurseForge honour ?gameVersion= on /v1/mods/{id}/files, and does it publish the file a
#           refusal claims is missing? (the original probe; needs CURSEFORGE_API_KEY)
#   build   Do the INSTALLED jars actually carry the 2026-09-07/08 fixes? (run on the grinder host)
#   health  Is the daemon behaving after a deploy — and specifically, has the false-conflict storm stopped?
#
# ---------------------------------------------------------------------------------------------------
# DEPLOY (on the grinder host). Re-fetched rather than run from /opt/spc-grinder-src, which deploy mode
# wipes at the start of every run — it would delete the file bash is still reading:
#
#   f=$(mktemp) \
#     && curl -fsSL https://git.griefed.de/griefed/serverpackcreator/raw/branch/develop/serverpackcreator-grinder/deploy/install-grinder.sh -o "$f" \
#     && sudo bash "$f"
#
# It installs from a fresh clone of `develop`, NOT from your working copy: push first, or you will
# deploy whatever the remote happens to hold. Then: `./cf-dependency-probe.sh build` and, a pass later,
# `./cf-dependency-probe.sh health`.
# ---------------------------------------------------------------------------------------------------
#
# WHY THIS EXISTS. On 2026-09-07 the live daemon published 47 ERROR verdicts reading "Required dependency
# unavailable" for files that exist — 245 of them by the next morning, across 273 projects. Cause: a
# CurseForge `ModFile.version` is the author-typed `displayName`, and `VersionConstraint.numbersOf` read
# `Balm 26.2.0.7` as [0,2,0,7], so DependencyBacktrack demoted nearly every CurseForge dependency until
# each project's file list was exhausted.
#
# MEASURED 2026-09-07 against the live API, all four projects: the filter WORKS and was never the bug.
#   thermal-foundation 1.20.1  filtered total=6  (unfiltered 52)  newest tagged [.,NeoForge,1.20.1,Forge,.]
#   create-fabric      1.20.1  filtered total=10 (unfiltered 67)  newest tagged [.,Fabric,1.20.1,.,Quilt]
#   balm               26.2    filtered total=14 (unfiltered 558) balm-fabric-26.2-26.2.0.7.jar present
#   architectury-api   1.20.1  filtered total=10 (unfiltered 625) architectury-9.2.14-fabric.jar present
# Note how much the filter earns on the big two: balm and architectury-api publish 558 and 625 files, so
# their newest-50 window holds nothing older than 26.1.2 — 67b848f8a was right, just not the cause.
#
set -u

BASE=https://api.curseforge.com/v1
PORT="${SPC_GRINDER_PORT:-8757}"
# Loopback by default, because that is where the report server binds unless SPC_GRINDER_HOST says
# otherwise. Point it elsewhere to check a daemon from your own machine:
#   SPC_GRINDER_URL=https://grinder.serverpackcreator.de ./cf-dependency-probe.sh health
URL="${SPC_GRINDER_URL:-http://localhost:$PORT}"
UNIT="${SPC_GRINDER_UNIT:-spc-grinder}"
PREFIX="${SPC_GRINDER_PREFIX:-/opt/spc-grinder}"

usage() {
    cat <<'USAGE'
usage: cf-dependency-probe.sh <command> [args]

  deps [slug mc]...   Ask CurseForge for each project's files, filtered by Minecraft version and
                      unfiltered, and print what it returns. Needs CURSEFORGE_API_KEY in the
                      environment; defaults to the four projects the storm was diagnosed on.
                      Read it as: filtered returned=0 while unfiltered returns the file -> the filter
                      is the bug; both return it -> the bug is downstream, in pickDependencyFile.

  build [prefix]      Grep the installed jars for the symbols the fixes introduced. Answers "is the
                      running code the fixed code", which a green deploy does not by itself prove.
                      Default prefix: /opt/spc-grinder (override with SPC_GRINDER_PREFIX).

  health [minutes]    Post-deploy check: the unit, the report server, the staging-refusal reasons in
                      the journal, and the verdict mix. Default window: 60 minutes.

Environment: CURSEFORGE_API_KEY, SPC_GRINDER_URL (http://localhost:8757 — set it to check a remote
daemon from your own machine), SPC_GRINDER_PORT (8757), SPC_GRINDER_UNIT (spc-grinder),
SPC_GRINDER_PREFIX (/opt/spc-grinder).

The journal section of `health` only works on the host; every other section works over SPC_GRINDER_URL.
USAGE
}

# ----------------------------------------------------------------------------- deps

# GET $1 into $BODY, printing only the HTTP status. The key never reaches stdout.
api() {
    curl -s -m 30 -o "$BODY" -w '%{http_code}' \
        -H "x-api-key: $KEY" -H 'Accept: application/json' "$BASE$1"
}

# One project, asked twice: narrowed by Minecraft version, then not narrowed at all.
probe() {
    slug=$1
    minecraft=$2

    status=$(api "/mods/search?gameId=432&classId=6&slug=$slug")
    id=$(jq -r '.data[0].id // empty' "$BODY" 2>/dev/null)
    echo "== $slug (id=${id:-LOOKUP-FAILED-HTTP-$status}) mc=$minecraft"
    if [ -z "$id" ]; then
        return
    fi

    for query in "&gameVersion=$minecraft" ""; do
        if [ -z "$query" ]; then label=unfiltered; else label=$query; fi
        printf '   %-22s ' "$label"
        status=$(api "/mods/$id/files?index=0&pageSize=50$query")
        if [ "$status" != 200 ]; then
            echo "HTTP $status :: $(head -c 160 "$BODY")"
            continue
        fi
        # totalCount says how much the filter kept; the three file names say whether the loader we
        # wanted is even in the window.
        jq -r '"total=\(.pagination.totalCount // "?") returned=\(.data|length) first=\([.data[0:3][] | "\(.fileName) [\(.gameVersions // [] | join(","))]"] | join(" | "))"' "$BODY"
    done
}

cmd_deps() {
    : "${CURSEFORGE_API_KEY:?not set - source the unit EnvironmentFile first}"
    KEY="$CURSEFORGE_API_KEY"
    BODY=$(mktemp)
    trap 'rm -f "$BODY"' EXIT

    status=$(api "/games")
    echo "auth check: HTTP $status (key length ${#KEY})"
    if [ "$status" != 200 ]; then
        echo "body: $(head -c 200 "$BODY")"
        exit 1
    fi

    if [ "$#" -gt 0 ]; then
        while [ "$#" -ge 2 ]; do
            probe "$1" "$2"
            shift 2
        done
    else
        probe thermal-foundation 1.20.1
        probe create-fabric 1.20.1
        probe balm 26.2
        probe architectury-api 1.20.1
    fi
}

# ----------------------------------------------------------------------------- build

# Symbols the 2026-09-07/08 fixes introduced. Grepped out of the class constant pool rather than read
# with javap, so this works on a host with a JRE and no JDK.
cmd_build() {
    prefix="${1:-$PREFIX}"
    jar=$(ls "$prefix"/lib/serverpackcreator-clientside*.jar 2>/dev/null | head -1)
    if [ -z "$jar" ]; then
        echo "no clientside jar under $prefix/lib — is that the install prefix?" >&2
        exit 1
    fi
    echo "jar: $jar"
    echo "     built $(date -r "$jar" '+%Y-%m-%d %H:%M' 2>/dev/null || stat -c %y "$jar" 2>/dev/null)"

    work=$(mktemp -d)
    trap 'rm -rf "$work"' EXIT
    # unzip exits non-zero when nothing matched, which is a different fault from a corrupt archive: it
    # means this is not the clientside jar. Saying "could not unpack" of it sends the reader to the wrong
    # question -- the same conflation the refusal reasons in the daemon exist to avoid.
    unzip -q -o "$jar" 'de/griefed/serverpackcreator/clientside/*' -d "$work" 2>/dev/null
    if ! ls "$work"/de/griefed/serverpackcreator/clientside/*.class >/dev/null 2>&1; then
        echo "   $jar holds no clientside classes — wrong jar, or a corrupt archive" >&2
        exit 1
    fi

    missing=0
    # readableVersion   the version side of "unreadable accepts" -- the storm fix itself
    # versionsIn        nested-jar versions, so a bundled library counts as staged
    # unambiguous       the one implementation of the drop-a-contested-id rule
    # nestedVersions    the same rule across a pack
    # backtrackReason   "we excluded it" told apart from "the project publishes nothing"
    # worthAppending    the refusal reason vocabulary
    for symbol in readableVersion versionsIn unambiguous nestedVersions backtrackReason worthAppending; do
        printf '   %-18s ' "$symbol"
        if grep -arqs "$symbol" "$work"/de/griefed/serverpackcreator/clientside/*.class; then
            echo present
        else
            echo MISSING
            missing=$((missing + 1))
        fi
    done

    if [ "$missing" -gt 0 ]; then
        echo "=> $missing symbol(s) missing: this install PREDATES the fixes. Deploy again, and check the"
        echo "   deploy pulled a branch that has them (deploy mode clones the remote, not your working copy)."
        exit 1
    fi
    echo "=> the installed jar carries every fix"
}

# ----------------------------------------------------------------------------- health

cmd_health() {
    minutes="${1:-60}"

    echo "== unit"
    if command -v systemctl >/dev/null 2>&1; then
        printf '   %s: %s (since %s)\n' "$UNIT" \
            "$(systemctl is-active "$UNIT" 2>/dev/null)" \
            "$(systemctl show "$UNIT" -p ActiveEnterTimestamp --value 2>/dev/null)"
    else
        echo "   no systemctl here — run this on the grinder host"
    fi

    echo "== report server ($URL)"
    status_json=$(curl -s -m 15 "$URL/status" 2>/dev/null)
    if [ -z "$status_json" ]; then
        echo "   no answer. The report server binds loopback unless SPC_GRINDER_HOST says otherwise,"
        echo "   so run this ON the host, tunnel (ssh -L $PORT:127.0.0.1:$PORT <host>), or set"
        echo "   SPC_GRINDER_URL to wherever it is reachable."
    else
        echo "$status_json" | jq -r '
            "   uptime=\(.activity.uptimeSeconds // 0)s pass=\(.activity.pass // 0) verdicts=\(.verdicts // 0) requeued=\(.requeued // 0)",
            "   verified=\(.activity.verified // 0) failed=\(.activity.failed // 0) skippedFresh=\(.activity.skippedFresh // 0)",
            "   rules=\(.bootRules.ruleCount // 0) from \(.bootRules.source // "none") errors=\(.bootRules.errors // [] | length)",
            "   workers: \([.activity.workers // [] | .[] | "\(.platform)/\(.slug) \(.busySeconds)s"] | join(", "))"'
    fi

    echo "== staging refusals in the journal, last ${minutes}m"
    if journal=$(journalctl -u "$UNIT" --since "${minutes} min ago" --no-pager 2>/dev/null); then
        # `re-staging ... without it` is DependencyBacktrack demoting. Before the fix the live daemon
        # logged ~1014 of these in a DAY against 4 real staging failures; a handful per hour is normal,
        # dozens means something is manufacturing conflicts again.
        printf '   %-52s %s\n' "re-staging ... without it (demotions)" "$(printf '%s' "$journal" | grep -c 're-staging .* without it')"
        # The refusal reasons, which only exist since 2026-09-08 -- the OLD message was
        # "publishes no <loader> file for Minecraft <mc>", so grepping for that now finds nothing and
        # would read as "fixed" whatever the truth is. Match the reasons instead.
        for reason in \
            "every usable build was dropped resolving a version conflict" \
            "nothing published for this loader and Minecraft version" \
            "distribution-locked on" \
            "download failed" \
            "unresolved on"; do
            printf '   %-52s %s\n' "$reason" "$(printf '%s' "$journal" | grep -c "$reason")"
        done
    else
        echo "   could not read the journal (try sudo, or add yourself to the systemd-journal group)"
    fi

    echo "== verdict mix"
    verdicts=$(curl -s -m 60 "$URL/verdicts.json" 2>/dev/null)
    if [ -z "$verdicts" ]; then
        echo "   no feed to read"
    else
        echo "$verdicts" | jq -r '
            "   total=\(.total // 0)",
            "   " + ([.verdicts[]?.verdict] | group_by(.) | map("\(.[0])=\(length)") | join(" ")),
            "   dependency-unavailable rows=\([.verdicts[]? | select(.detail? // "" | test("Required dependency unavailable"))] | length)",
            "   DEPENDENCY_FAILURE rows=\([.verdicts[]? | select(.decidedBy? == "DEPENDENCY_FAILURE")] | length)"'
        echo "   (both counts were 245 and 137 on the pre-fix build, across 273 projects — they should now"
        echo "    be a small fraction of the store, and the reasons above say which kind each one is)"
    fi
}

# ----------------------------------------------------------------------------- dispatch

command="${1:-}"
if [ "$#" -gt 0 ]; then shift; fi
case "$command" in
    deps)   cmd_deps "$@" ;;
    build)  cmd_build "$@" ;;
    health) cmd_health "$@" ;;
    ""|-h|--help|help) usage ;;
    *)      echo "unknown command '$command'" >&2; echo >&2; usage >&2; exit 2 ;;
esac
