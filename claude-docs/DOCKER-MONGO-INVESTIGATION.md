# Investigation plan — "MongoDB container not reachable, SPC connects to localhost"

**Status:** investigation only. Nothing fixed, nothing changed outside this file.
**Branch:** `claude-docker-mongo-connectivity`, off `develop` (`4bb403ef1`).
**Report:** a user's `serverpackcreator` container cannot reach the `serverpackcreatordb` container;
SPC appears to connect to `localhost` instead.

---

## How the override chain is *supposed* to work (verified by reading, not assumed)

1. `docker-compose.yml:16-20` passes `SPC_DATABASE_USERNAME`, `_PASSWORD`, `_HOST`
   (`serverpackcreatordb`), `_PORT`, `_DB`.
2. `docker/root/etc/s6-overlay/s6-rc.d/init-spc-config/run` composes them into
   `spring.data.mongodb.uri=…` and appends it to `/app/serverpackcreator/overrides.properties`.
3. `WebService.kt:40-47` passes `--spring.config.location=` with eight comma-separated locations,
   `overrides.properties` **last**.
4. Later locations win, so `overrides.properties` outranks `serverpackcreator.properties`
   ([Spring Boot externalized config](https://docs.spring.io/spring-boot/reference/features/external-config.html)).
   **The ordering is correct** — that is not the bug.

Also established: **nothing connects to Mongo using `apiProperties.databaseUri`.** Its only consumers
are the Swing settings panel (`WebserviceSettings.kt:52,162,171,230`) and a log line
(`ApiProperties.kt:1254`). The live connection is made by Spring Data from
`spring.data.mongodb.uri`. So any fix must land on *that property as Spring resolves it*, and a
theory that only explains SPC's own settings file does not explain the symptom on its own.

---

## Hypotheses, most likely first

### H1 — the all-or-nothing env check writes a degenerate URI · `init-spc-config/run`

```bash
if [[ -z ${SPC_DATABASE_HOST} ]] || [[ -z ${SPC_DATABASE_PORT} ]] || [[ -z ${SPC_DATABASE_DB} ]] \
   || [[ -z ${SPC_DATABASE_USERNAME} ]] || [[ -z ${SPC_DATABASE_PASSWORD} ]];then
  echo "spring.data.mongodb.uri=mongodb\:" >> ${OVERRIDES}      # <-- degenerate
else
  echo "spring.data.mongodb.uri=mongodb\://${SPC_DATABASE_USERNAME}\:…" >> ${OVERRIDES}
fi
```

If **any one** of the five is unset, the good branch never runs and the file gets
`spring.data.mongodb.uri=mongodb:` (the `\:` unescapes to `:`). Why that is the most likely trigger:
`docker-compose.yml` ships `SPC_DATABASE_USERNAME=<DB_USERNAME>` / `PASSWORD=<DB_PASSWORD>` as
**placeholders**, and a user standing up a Mongo without auth will reasonably delete those two lines
— which silently switches the whole URI to garbage rather than to a working unauthenticated one.
`WebserviceConfig`'s sanity check does not catch it either: `mongodb:` passes
`!dbPath.startsWith("mongodb")` (`WebserviceConfig.kt:93`), so it is never migrated.

**Predicts:** a driver/parse failure, or — if Spring treats the value as unusable and falls back to
`spring.data.mongodb.host`/`port` — the observed **localhost:27017**. Which of the two happens on
Spring Boot 4 / Spring Data Mongo 4 is the single most important thing to settle, because it decides
whether H1 explains the *exact* wording of the report or only a nearby failure.

### H2 — nothing lands in `overrides.properties` at all, so Spring uses its own default

Spring's default when no `spring.data.mongodb.uri` is resolvable is **localhost:27017**, which is
exactly the reported symptom. Ways to reach it: the s6 init service did not run or failed before the
mongo block; the file was written somewhere other than where `WebService` looks
(`api.apiProperties.overridesPropertiesFile` = `File(homeDirectory, "overrides.properties")`, so this
only holds if `homeDirectory` resolves to something other than `/app/serverpackcreator`); or the user
bind-mounted over `/app/serverpackcreator` and shadowed it.

**Note the interaction with `--home`:** `CommandlineParser.kt:50` *also* treats
`<jarFolder>/overrides.properties` as SPC's own properties file, so the same file is both a Spring
config location and SPC's settings store. Whether `ApiProperties.saveProperties` ever rewrites it —
and whether a rewrite preserves `spring.data.mongodb.uri` — is unverified and worth checking, because
`WebserviceConfig.databaseUri`'s **getter has a side effect**: reading it calls
`store.define(DATABASE_URI_KEY, …)` with a hard-coded `mongodb://user:password@localhost:27017/…`
fallback (`WebserviceConfig.kt:44,87-101`). A read followed by a save is therefore a mechanism for
writing *localhost* into the very file that is supposed to override it.

### H3 — the last CLI argument is silently discarded · `WebService.kt:48-54`

```kotlin
val temp = args.toList().toTypedArray()
temp[temp.lastIndex] = lastIndex   // overwrites whatever the user's last argument was
```

The container starts SPC as `-web --home "/app/serverpackcreator"` (`svc-spc/run`), so the argument
that gets overwritten is **the value of `--home`**. The config-location string itself is built before
this and from the already-resolved `ApiWrapper`, so the *paths inside it* are probably right — but
Spring then receives a valueless `--home`, and any deployment whose last argument matters loses it.
This is a certain bug regardless of the MongoDB symptom (see below); it is ranked third only because
it is not obvious it can produce *localhost* by itself.

### H4 — plain container networking / auth, not SPC's config

Worth eliminating early and cheaply: `serverpackcreatordb` unreachable by name (custom network,
`container_name` vs service-name mismatch), or the URI correct but authentication failing against a
DB created with `MONGO_INITDB_DATABASE: serverpackcreatordb` while SPC is told
`SPC_DATABASE_DB=serverpackcreator` — **the compose file itself disagrees on the database name**
(`docker-compose.yml:20` vs `:47`), and `init-mongo.js` should be read alongside them. An auth error
against the right host is a different bug from connecting to localhost, so the log line matters.

---

## Decisive experiments, in order

1. **Ask for two things from the reporter** (cheapest, and it discriminates H1/H2/H4 immediately):
   the `spring.data.mongodb.uri` line from their container's
   `/app/serverpackcreator/overrides.properties`, and the actual exception from their SPC log. A URI
   naming `serverpackcreatordb` with a localhost connection error means H2/H3; `mongodb:` means H1;
   a `MongoSecurityException` means H4.
2. **Reproduce locally** with `docker/docker-compose-dev.yml` (it has real credentials, not
   placeholders) — confirm the happy path works at all on this machine. Then re-run with
   `SPC_DATABASE_USERNAME`/`_PASSWORD` removed to test H1 directly, and inspect the resulting
   `overrides.properties` plus the startup exception. *Caveat:* this machine's Docker VM is capped at
   **1.93 GiB** (it OOM-killed Qodana), so Mongo + SPC together may not fit; if so, run the init
   script alone in the SPC image to get `overrides.properties`, which is all H1 needs.
3. **Settle Spring Boot 4's behaviour for an unparseable `spring.data.mongodb.uri`** — fall back to
   `host`/`port` (localhost) or fail fast? Check `MongoProperties`/`MongoConnectionDetails` in the
   pinned `spring-boot-starter-data-mongodb:4.0.6` rather than assuming; this is what confirms or
   kills H1 as the explanation for the exact symptom.
4. **Trace the file's lifecycle for H2** — does `ApiProperties.saveProperties` ever target
   `overrides.properties` (it is `CommandlineParser`'s `propertiesFile` when it exists), and does a
   save preserve or drop `spring.data.mongodb.uri`? Add a characterization test around
   `PropertyStore` before changing anything here.

---

## Certain bugs found on the way, independent of the root cause

These need no reproduction — they are wrong as written. Each wants its own commit, and **H5/H6 must
not be bundled with the MongoDB fix**.

| # | Location | Defect |
|---|---|---|
| H3 | `WebService.kt:52` | `temp[temp.lastIndex] = lastIndex` discards the user's last CLI argument on every args-bearing start. |
| H5 | `init-spc-config/run` (loglevel block) | `echo "…loglevel={SPC_LOG_LEVEL}"` — **no `$`**, so the literal string `{SPC_LOG_LEVEL}` is written. `SPC_LOG_LEVEL` has never worked. |
| H6 | `init-spc-config/run` (zip-exclude blocks) | `{$SPC_SERVERPACK_ZIP_EXCLUDE}` and `{$SPC_SERVERPACK_ZIP_EXCLUDE_ENABLED}` wrap the value in **literal braces**. |
| H7 | `docker-compose.yml:20` vs `:47` | `SPC_DATABASE_DB=serverpackcreator` but `MONGO_INITDB_DATABASE: serverpackcreatordb`. One of them is wrong; `README.md:282` says `serverpackcreatordb`. |
| H8 | `WebserviceConfig.kt:87-101` | A **getter with a write side-effect** that silently rewrites any non-`mongodb` URI to a hard-coded localhost default, warning only. Reading a setting should not mutate the store. |

`SPC_LOG_LEVEL`, the zip-exclude pair and the DB-name mismatch are all user-visible documentation
promises that do not hold, so they are worth fixing even though they are not this report.

## Testing note

Per `CLAUDE.md`, the shell templates and the s6 init script fail *silently* — a wrong branch produces
a plausible file, not an error — so this is exactly the category that gets a test written and observed
failing first. The tractable unit is `init-spc-config/run`: it is a pure function from environment to
`overrides.properties`, so it can be executed in the built image with a controlled environment and
its output asserted, which would have caught H1, H5 and H6. That harness does not exist yet and is
the first thing to build.
