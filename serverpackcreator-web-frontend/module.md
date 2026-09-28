# Module serverpackcreator-web-frontend

**The web user-interface of ServerPackCreator** — a single-page application (SPA) built with
**Quasar 2 / Vue 3** in **TypeScript**, with **Pinia** for state. It is the browser client for the
`serverpackcreator-app` `web` backend: everything it shows comes from that backend's REST API. At
build time the org.siouan Gradle plugin compiles this app and bundles it *into* the web backend, so
the two ship as one.

> Note: this is a JavaScript/TypeScript module, so it is not part of the Kotlin Dokka publication.
> This `module.md` is in-repo documentation. For build/test/TypeScript specifics see the module's
> `CLAUDE.md`.

## ELI5: how does a Vue/Quasar SPA fit together?

If you've never touched Vue: the app is a tree of **components** (`.vue` files — each is a reusable
chunk of HTML + logic + style). The **router** decides which **page** component to show for each
URL. A page is composed of smaller **components**. Shared data that many components need (like the
list of available versions) lives in a **store** instead of being passed around by hand. **Boot**
files run once at start-up to set things up (HTTP client, translations). That's the whole mental
model — the directories below map one-to-one onto it.

## ELI5: the directory map (each is a folder under `src/`)

- **`boot/`** — start-up setup that runs before the app mounts. `axios.ts` configures the HTTP client
  used to call the backend; `i18n.ts` installs translations.
- **`router/`** — URL → page wiring. `routes.ts` lists every route and the page it renders;
  `index.ts` builds the Vue Router from it.
- **`layouts/`** — the persistent page chrome. `MainLayout.vue` is the frame (header, navigation
  drawer, footer) that wraps every page; the router swaps page content inside it.
- **`pages/`** — one component per screen (each maps to a route): `SubmissionPage` (submit a modpack),
  `DownloadsPage` / `ModPackDownload` / `ServerPackDownload` (browse & download results),
  `HistoryPage` (past generations), `AboutPage`, and `ErrorPage` (the SPA's error/404 target).
- **`components/`** — the reusable building-blocks pages are assembled from (see below).
- **`stores/`** — Pinia stores: shared, reactive application state. `setting-store.ts` holds this
  instance's settings/version data fetched from the backend; `index.ts` is the Pinia setup. The
  store is deliberately decoupled from Quasar's `$q` so it is pure, unit-testable data-fetching.
- **`types/`** — `api.ts`: TypeScript interfaces for the backend's JSON payloads (only the fields the
  templates actually use), so the components are type-checked against what the API really returns.
- **`i18n/`** — translation message bundles (`en-US/`) and their index. UI text comes from here, not
  hardcoded in components.
- **`assets/`**, **`css/`** — static images and global styles.

## ELI5: the components, by job

**Forms & input**
- `SubmitModPackForm` — the main interaction: upload a modpack and request a server pack (the most
  logic-heavy component).

**Result "cards"** (display one fetched record)
- `ModPackCard` — a submitted modpack. `ServerPackCard` — a generated server pack.
- `RunConfigurationCard` — the run-configuration (mods, start arguments) used for a pack.
- `ErrorsCard` — errors reported for a submission. `NotAvailableCard` — a placeholder when there's
  nothing to show.

**Lists / tables**
- `ModpacksTable`, `ServerPacksTable`, `HistoryTable` — tabular listings (mostly presentational; their
  only logic is trivial column-format lambdas).

**Navigation & chrome**
- `DrawerLink` — one entry in the navigation drawer. `IndexItem` / `AboutItem` — list items on the
  index/about screens.
