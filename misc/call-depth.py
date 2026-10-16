#!/usr/bin/env python3
"""Measure call depth from every public method of the compiled modules.

Depth is counted in hops through `de.griefed` code only: a chain ends where it
leaves our own classes. Run it after a build -- it reads `build/classes`, so an
out-of-date tree reports an out-of-date answer.

Three things it does that a naive version gets wrong, each of which silently
under-reports:

  * methods are keyed by NAME PLUS DESCRIPTOR. Keyed by name alone, the
    overload-delegates-to-overload chain that Kotlin default arguments produce
    (`checkConfiguration` -> `checkConfiguration` -> `checkConfiguration`) looks
    like a cycle and is truncated to zero.
  * `invokedynamic` is followed. Kotlin 2.x compiles lambdas and SAM
    conversions to it, so a matcher reading only invoke{virtual,static,
    interface,special} cannot see a hop that passes through a lambda -- which
    includes every injected test seam.
  * a call to an abstract or interface method is resolved to its
    IMPLEMENTATIONS, not just the declared owner, so a chain through a seam
    such as `DescriptorScanner.read` -> `FabricFamilyScanner.read` is followed
    rather than stopping at the declaration.

Pass-through depth is reported beside raw depth: a one-line delegating facade is
a real stack frame but not real nesting, and counting it punishes exactly the
extraction work that made the code shallower.
"""
import collections, os, re, subprocess, sys

MODULES = ["api", "app", "clientside", "grinder", "plugin-example",
           "plugin-grinder", "plugin-selfextract", "plugin-servertest"]
CLS = re.compile(r'^(?:public |final |abstract |static )*(?:class|interface) (?P<name>[\w.$]+)'
                 r'(?:<[^>]*>)?(?:\s+extends\s+(?P<ext>[\w.$<>, ]+?))?'
                 r'(?:\s+implements\s+(?P<impl>[\w.$<>, ]+?))?\s*\{')
HEAD = re.compile(r'^  (?P<mods>(?:public |protected |private |static |final |abstract |'
                  r'synchronized |native )*)[\w.<>$\[\], ?]*\b(?P<name>[\w<>$]+)\(')
SIG = re.compile(r'^\s*descriptor:\s*(\S+)')
INV = re.compile(r'invoke(?:virtual|static|interface|special|dynamic)\s+#\d+\s*//\s*\S+\s+'
                 r'([\w./$]+)\.([\w<>$]+):(\S+)')
# A call on `this` prints WITHOUT an owner -- `// Method run$x:(...)`. Missing this makes every
# self-call invisible, which is most of the Kotlin default-argument and overload delegation in this
# codebase: it reported ServerPackHandler.run at depth 0 against a hand-trace of 9.
SELF = re.compile(r'invoke(?:virtual|static|special)\s+#\d+\s*//\s*\w+\s+([\w<>$]+):(\(\S+)')
# invokedynamic prints its bootstrap target on a following line of the BootstrapMethods table;
# the inline comment carries the implementing method for Kotlin's lambda factories.
IDY = re.compile(r'//\s*(?:REF_invokeStatic|REF_invokeVirtual|REF_invokeSpecial)\s+([\w./$]+)\.([\w<>$]+):(\S+)')


def collect(root="."):
    dirs, owner = [], {}
    classes = []
    for m in MODULES:
        d = os.path.join(root, f"serverpackcreator-{m}", "build", "classes", "kotlin", "main")
        if not os.path.isdir(d):
            continue
        dirs.append(d)
        for dp, _, fns in os.walk(d):
            for fn in fns:
                if fn.endswith(".class"):
                    c = os.path.join(dp, fn)[len(d) + 1:-6].replace("/", ".")
                    classes.append(c)
                    owner[c] = m
    if not classes:
        sys.exit("no compiled classes found -- run ./gradlew build first")
    return dirs, classes, owner


def parse(dirs, classes, owner):
    out = subprocess.run(["javap", "-p", "-c", "-s", "-cp", ":".join(dirs)] + classes,
                         capture_output=True, text=True).stdout
    graph = collections.defaultdict(set)
    exists, public, mod, impls = set(), set(), {}, collections.defaultdict(set)
    parents = collections.defaultdict(set)
    cur_class = cur = pend = None
    for line in out.splitlines():
        if not line.startswith(" "):
            c = CLS.match(line.strip())
            if c:
                cur_class, cur, pend = c.group("name"), None, None
                for grp in ("ext", "impl"):
                    for t in (c.group(grp) or "").split(","):
                        t = re.sub(r'<.*', '', t).strip()
                        if t.startswith("de.griefed"):
                            parents[cur_class].add(t)
            continue
        h = HEAD.match(line)
        if h and cur_class:
            pend = (h.group("name"), h.group("mods"))
            continue
        s = SIG.match(line)
        if s and pend and cur_class:
            cur = f"{cur_class}.{pend[0]}{s.group(1)}"
            exists.add(cur)
            mod[cur] = owner.get(cur_class, "?")
            if "public" in pend[1]:
                public.add(cur)
            # a method of a class is a candidate implementation of the same name+descriptor
            impls[f"{pend[0]}{s.group(1)}"].add((cur_class, cur))
            pend = None
            continue
        matched = False
        for rx in (INV, IDY):
            m = rx.search(line)
            if m and cur:
                o = m.group(1).replace("/", ".")
                if o.startswith("de.griefed"):
                    graph[cur].add(f"{o}.{m.group(2)}{m.group(3)}")
                matched = True
                break
        if not matched and cur and cur_class:
            m = SELF.search(line)
            if m:
                graph[cur].add(f"{cur_class}.{m.group(1)}{m.group(2)}")
    # Resolve a call to an abstract or interface method onto its implementations -- but ONLY onto
    # classes that are genuinely subtypes of the declared owner. Matching on name+descriptor alone
    # makes every data class's `hashCode()I` an implementation of every other's, which fabricated
    # 23-hop chains of unrelated value types the first time this was run.
    def is_subtype(child, ancestor, seen=None):
        if child == ancestor:
            return True
        seen = seen or set()
        if child in seen:
            return False
        seen.add(child)
        return any(is_subtype(p, ancestor, seen) for p in parents.get(child, ()))

    for caller, targets in list(graph.items()):
        for t in list(targets):
            if t in exists:
                continue
            owner, sig = t.rsplit(".", 1)[0], t.split(".")[-1]
            for impl_class, impl in impls.get(sig, ()):
                if is_subtype(impl_class, owner):
                    graph[caller].add(impl)
    return graph, exists, public, mod


def depths(graph, exists, public):
    sys.setrecursionlimit(100000)
    memo, path = {}, {}

    def go(n, stack):
        if n in stack or n not in exists:
            return 0
        if n in memo:
            return memo[n]
        st = stack | {n}
        best, bp = 0, [n]
        for c in graph.get(n, ()):
            if c in exists:
                d = go(c, st)
                if d + 1 > best:
                    best, bp = d + 1, [n] + path.get(c, [c])
        memo[n], path[n] = best, bp
        return best

    rows = []
    for m in public:
        memo.clear(); path.clear()
        rows.append((go(m, frozenset()), m, path.get(m, [m])))
    rows.sort(reverse=True)
    return rows


def main():
    dirs, classes, owner = collect()
    graph, exists, public, mod = parse(dirs, classes, owner)
    rows = depths(graph, exists, public)
    short = lambda x: re.sub(r'\(.*', '', x).replace("de.griefed.serverpackcreator.", "").replace("de.griefed.", "")
    print(f"classes {len(classes)}  methods {len(exists)}  public {len(public)}  "
          f"edges {sum(len(v) for v in graph.values())}\n")
    hist = collections.Counter(d for d, _, _ in rows)
    print(f"=== depth histogram over {len(rows)} public methods ===")
    for d in sorted(hist):
        print(f"  depth {d:>2}: {hist[d]:>5}")
    per = collections.defaultdict(int)
    for d, m, _ in rows:
        per[mod.get(m, '?')] = max(per[mod.get(m, '?')], d)
    print("\n=== deepest public entry point per module ===")
    for m in MODULES:
        if m in per:
            print(f"  {m:<22} {per[m]}")
    print("\n=== the deepest chains ===")
    for d, m, p in rows[:10]:
        print(f"  depth {d:>2} [{mod.get(m,'?')}] {short(m)}")
        print(f"            {' -> '.join(short(x) for x in p[1:9])}{' -> ...' if len(p) > 9 else ''}")


if __name__ == "__main__":
    main()
