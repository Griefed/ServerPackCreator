# serverpackcreator-plugin-selfextract — module context

> A pf4j plugin that wraps every generated server pack in two self-extracting artifacts: a `.bsx`
> for Linux and macOS, a `.cmd` for Windows 10 1803+. It is the `HELP.md` chapter *Fun Stuff →
> Self-extracting, self-contained script*, executed automatically, and the two must stay in step.

## Shape

One extension point — `PostGenExtension` — and nothing else. No tab, no config file.

- `SelfExtractPlugin` — the pf4j entry class, stateless.
- `SelfExtractPostGenExtension` — the hook. Reads the destination, refuses what it cannot wrap,
  never throws.
- `core/TarGzWriter` — ustar + gzip, written by hand.
- `core/SelfExtractingArchive` — stub + payload, and the offset arithmetic.
- `core/Stubs` — the two scripts, with the reason for each awkward part beside it.

## Why there is no configuration, and what would change that

Every generated pack gets both artifacts, built from the pack that was just written. There is
nothing to choose, so installing the plugin is the setting and removing it is how you turn it off —
one fewer place for the answer to live than a checkbox that can disagree with what the plugin does.

The one thing that might earn a setting later is **cost**: each artifact is the size of the pack, so
a generation that produced a 6 GB pack now writes 18 GB. If that becomes the complaint, the setting
to add is which artifacts to write, not whether to run.

## Landmines

- **LANDMINE — the TAR format is written here by hand, and that is a delivery constraint, not a
  preference.** A pf4j plugin jar carries no dependencies, nothing in this repository builds a fat
  plugin jar, and the host's runtime classpath has no TAR writer on it — so `commons-compress` could
  not reach the machine this runs on. What makes it safe is that nothing in `TarGzWriterTest` judges
  the output with a reader of ours: every assertion is the verdict of the machine's own `tar`. Keep
  it that way. Measured against a real generated pack (`serverpackcreator-api/tests/server-packs/forge_tests`):
  44 files out, `start.sh` `-rwxr-xr-x`, no warnings, under **bsdtar (macOS), GNU tar (debian) and
  busybox tar (alpine)**.
- **LANDMINE — a zero-padded fixed-width offset does not work, however tidy it looks.** Each stub
  states the byte its payload begins at, which is the stub's own length, so writing the number
  changes what it measures. Padding to a fixed width settles that circularity and breaks the reader:
  BSD tail, which is what macOS ships, rejects a padded count outright — `illegal offset --
  +000000000639`. `SelfExtractingArchive.settleOffset` iterates instead, which settles in one or two
  rounds because a round can only add digits.
- **LANDMINE — the batch stub's rules are not style.** No `goto` and no label, because cmd.exe
  resolves a label by scanning the file and gzip output is full of null bytes it cannot scan across;
  `EXIT /B` before the payload; and no `"`, `%` or `!` anywhere inside the `-Command` argument, each
  of which cmd eats or acts on before PowerShell sees the line. `SelfExtractingArchiveTest` pins all
  four, and `BatchStubPowerShellTest` asks a real PowerShell whether the one-liner parses at all.
- **The `.cmd` has never been run.** Its PowerShell parses, its structure is pinned, and cmd.exe's
  own tolerance of an appended binary payload needs a real Windows machine to confirm. Say so rather
  than implying otherwise, and if you get the chance: run one, check it extracts, starts the server,
  and refuses a second time.
- **Modes are decided, never copied.** The host sets 0544 on generated scripts
  (`File.setExecutable(true)` is owner-only) and a pack built on Windows has no POSIX bits at all, so
  copying what is on disk produces a server only the account that built it can start.
