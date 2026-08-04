# Investigation — "MongoDB container not reachable, SPC connects to localhost"

**Status:** root cause of the *reported symptom* still unconfirmed (needs one line from the reporter);
five independent defects found along the way, all verified.
**Branch:** `claude-docker-mongo-connectivity`, off `develop` (`4bb403ef1`).
**Report:** a user's `serverpackcreator` container cannot reach the `serverpackcreatordb` container;
SPC appears to connect to `localhost`.

---

## How the override chain works (verified by reading, not assumed)

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
(`ApiProperties.kt:1254`). The live connection is made by Spring Data from `spring.data.mongodb.uri`,
so a theory about SPC's own settings file does not explain the symptom on its own.

## Spring Boot 4 does not fall back — measured, not read from docs

From `spring-boot-mongodb-4.0.2` and `mongodb-driver-core-5.6.2` in the Gradle cache, via `javap -c`
(no sources jar is published for the autoconfigure module):

```java
// PropertiesMongoConnectionDetails.getConnectionString()
if (properties.getUri() != null) return new ConnectionString(properties.getUri());   // verbatim
// else: getProtocol() + "://" + [user:pass@] + (host != null ? host : "localhost") + [":" + port] …
```

- **No fallback and no validation.** Any non-null `uri` goes straight to `ConnectionString`; the
  `host`/`port` branch is unreachable whenever a uri is set.
- `ConnectionString` accepts only `MONGODB_PREFIX = "mongodb://"` or
  `MONGODB_SRV_PREFIX = "mongodb+srv://"`, else throws *"The connection string is invalid. Connection
  strings must start with either '%s' or '%s'"*.
- The literal `"localhost"` exists **only** in the else-branch. `MongoProperties.DEFAULT_URI =
  "mongodb://localhost/test"` and `DEFAULT_PORT = 27017` are real, and `determineUri()` returns
  `uri ?: DEFAULT_URI` — but the connection path never calls `determineUri()`.
- Two Boot-4 traps worth knowing: `uri` **silently wins** over `host`/`port`/`username`/`password`, so
  mixing the two styles ignores the latter without warning; and `MongoProperties` has gained
  `protocol` and `additionalHosts`.

**Consequence: a literal localhost connection requires `spring.data.mongodb.uri` to be absent from all
eight config locations.** A malformed value cannot produce it — it fails fast before any socket opens.

## Triage table — the reporter's exception identifies the cause outright

| Exception in their SPC log | Cause |
|---|---|
| `IllegalArgumentException` … *must start with either 'mongodb://'* | D1, the degenerate URI |
| `MongoSocketOpenException` … `localhost:27017` | the property reached Spring from nowhere — see "still open" |
| `MongoSecurityException` / auth failure | D4, the database-name mismatch |

Still needed from them: the `spring.data.mongodb.uri` line out of their container's
`/app/serverpackcreator/overrides.properties`, and that exception.

## Still open: what makes the property absent entirely

Nothing found so far explains it, and it is the only path to the reported wording. Candidates not yet
excluded: the s6 init service not running (older image, overridden entrypoint/command, or a bind mount
shadowing `/app/serverpackcreator`); `homeDirectory` resolving somewhere other than
`/app/serverpackcreator`, so `api.apiProperties.overridesPropertiesFile` points at a different file
than the script writes; or SPC rewriting the file — `CommandlineParser.kt:50` treats
`<jarFolder>/overrides.properties` as SPC's *own* settings store, so the same file is both a Spring
config location and a file `ApiProperties.saveProperties` may rewrite. That last one wants a
`PropertyStore` characterization test before anything is changed.

---

## Defects found, all verified, none of them yet proven to be *this* report

### D1 — the all-or-nothing env check writes an unparseable URI · `init-spc-config/run`

If **any one** of the five `SPC_DATABASE_*` vars is unset, the good branch never runs and the file gets
`spring.data.mongodb.uri=mongodb\:` (the `\:` unescapes to `:`). Per the Boot-4 finding above this is a
**hard startup failure**, not a fallback. `docker-compose.yml:16-17` ships `<DB_USERNAME>` /
`<DB_PASSWORD>` as placeholders, so a user running an auth-less Mongo who deletes those two lines —
entirely reasonable — silently gets a URI that cannot parse. `WebserviceConfig`'s sanity check misses
it too: `mongodb:` satisfies `!dbPath.startsWith("mongodb")` (`WebserviceConfig.kt:93`).

### D2 — `SPC_LOG_LEVEL` has never worked · `init-spc-config/run`

`echo "…loglevel={SPC_LOG_LEVEL}"` is missing the `$`, so the literal string `{SPC_LOG_LEVEL}` is
written to `overrides.properties`.

### D3 — the zip-exclude overrides wrap their values in literal braces · `init-spc-config/run`

`{$SPC_SERVERPACK_ZIP_EXCLUDE}` and `{$SPC_SERVERPACK_ZIP_EXCLUDE_ENABLED}` expand, then brace the
result: `…zip.exclude={a,b,c}` and `…enabled={true}`.

### D4 — both compose files point SPC at a database its user cannot authenticate against

`init-mongo.js` creates the user **inside** `MONGO_INITDB_DATABASE` with `readWrite` on that database
only. A connection string's path segment is also its default `authSource`. So SPC must be told the
*same* database name:

| file | `SPC_DATABASE_DB` | `MONGO_INITDB_DATABASE` | verdict |
|---|---|---|---|
| `docker-compose.yml` | `serverpackcreator` (`:20`) | `serverpackcreatordb` (`:47`) | mismatch |
| `docker-compose-dev.yml` | `serverpackcreator` (`:22`) | `serverpackcreatordb` | mismatch |

`README.md:282` says `serverpackcreatordb`, which agrees with `init-mongo.js`. `docker-compose-dev.yml`
additionally sets `SPC_DATABASE_USERNAME=serverpackcreator` while `init-mongo.js` creates the user from
`MONGO_INITDB_ROOT_USERNAME`, which that file sets to `root` — so the dev credentials do not match the
user that gets created either.

### D5 — `docker-compose-dev.yml` is not a valid compose project

```
$ docker compose -f docker-compose-dev.yml config
service "serverpackcreator-mongo-express" depends on undefined service "mongodb": invalid compose project
```

There is no `mongodb` service; it is called `serverpackcreatordb`. The dev stack cannot start at all,
which is also why it could not be used to reproduce the report. (`docker-compose.yml` validates clean.)
Both files also still carry the obsolete `version:` key, which compose warns about.

### D6 — the last CLI argument is silently discarded · `WebService.kt:48-54`

```kotlin
temp[temp.lastIndex] = lastIndex   // overwrites whatever the user's last argument was
```

The container starts SPC as `-web --home "/app/serverpackcreator"` (`svc-spc/run`), so the argument
overwritten is **the value of `--home`**. The config-location string is built beforehand from the
already-resolved `ApiWrapper`, so the paths inside it are right, but Spring then receives a valueless
`--home` and any deployment whose last argument matters loses it.

---

## Testing

`init-spc-config/run` is a pure function from environment to `overrides.properties`, which is exactly
the silently-failing category `CLAUDE.md` requires a red test for first. `docker/tests/init-spc-config-test.sh`
now executes the **real** script inside `ghcr.io/linuxserver/baseimage-ubuntu:noble` (which provides
`with-contenv` and `lsiown`, so nothing is stubbed) with a controlled environment, and asserts the
generated file. It is not wired into Gradle: the script lives outside every module and needs Docker, so
adding a Gradle→Docker dependency for it would cost more than it returns. Run it by hand or from CI:

```
docker/tests/init-spc-config-test.sh
```

D1, D2 and D3 were each observed **red** there before being fixed.

**Not fixed here, deliberately:** `WebserviceConfig.databaseUri`'s getter (`WebserviceConfig.kt:87-101`)
writes a hard-coded `mongodb://user:password@localhost:27017/serverpackcreatordb` into the store as a
side-effect of *reading* it. A getter that mutates is a real design defect and a plausible route to a
localhost value in SPC's own file, but it is `-api` surface with GUI consumers and wants its own
characterization tests rather than riding a Docker fix.
