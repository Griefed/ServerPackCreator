# Module serverpackcreator-plugin-selfextract

Wraps every generated server pack in a self-extracting script — a `.bsx` for Linux and macOS and a
`.cmd` for Windows — which unpacks itself into the user's home directory and starts the server.

The recipe is the one documented in `HELP.md` under *Fun Stuff → Self-extracting, self-contained
script*; this plugin is that chapter, executed automatically for every generation.
