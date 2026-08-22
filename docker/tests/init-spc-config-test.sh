#!/usr/bin/env bash
#
# Characterization tests for docker/root/etc/s6-overlay/s6-rc.d/init-spc-config/run.
#
# That script is a pure function from environment to /app/serverpackcreator/overrides.properties, and
# it fails *silently*: a wrong branch or a missing `$` produces a plausible-looking file, not an error.
# CLAUDE.md therefore requires a test here, so this executes the real script - unmodified, mounted
# read-only - inside the same base image the production Dockerfile uses. That image already provides
# `with-contenv` and `lsiown`, so nothing has to be stubbed and the script runs exactly as it does in a
# real container.
#
# Not wired into Gradle on purpose: the script lives outside every module and needs Docker, so a
# Gradle -> Docker dependency would cost more than it returns. Run it by hand or from CI.
#
# Usage: docker/tests/init-spc-config-test.sh
# Exit:  0 = all assertions passed, 1 = one or more failed, 2 = cannot run (no Docker)

set -uo pipefail

readonly IMAGE="ghcr.io/linuxserver/baseimage-ubuntu:noble"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly INIT_SCRIPT="${SCRIPT_DIR}/../root/etc/s6-overlay/s6-rc.d/init-spc-config/run"
readonly DEFAULTS="${SCRIPT_DIR}/../root/defaults/serverpackcreator.properties"

declare -i passed=0 failed=0

if ! docker info >/dev/null 2>&1; then
  echo "SKIP: Docker is not available - cannot execute the init script." >&2
  exit 2
fi

if [[ ! -f ${INIT_SCRIPT} ]]; then
  echo "FAIL: init script not found at ${INIT_SCRIPT}" >&2
  exit 1
fi

# Runs the real init script with the given `KEY=VALUE` environment and echoes the resulting
# overrides.properties. The script copies /defaults/serverpackcreator.properties, so that is mounted
# too; its chown/stat block is exercised as-is.
run_init() {
  local -a env_args=()
  local pair
  for pair in "$@"; do
    env_args+=("-e" "${pair}")
  done
  docker run --rm \
    -v "$(cd "$(dirname "${INIT_SCRIPT}")" && pwd)/run":/init-run:ro \
    -v "$(cd "$(dirname "${DEFAULTS}")" && pwd)/serverpackcreator.properties":/defaults/serverpackcreator.properties:ro \
    "${env_args[@]}" \
    --entrypoint bash \
    "${IMAGE}" -c \
    'mkdir -p /app/serverpackcreator && bash /init-run >/dev/null 2>&1; cat /app/serverpackcreator/overrides.properties'
}

# Asserts that the generated overrides.properties contains an exact line.
assert_line() {
  local description="$1" expected="$2" output="$3"
  if grep -Fxq -- "${expected}" <<<"${output}"; then
    echo "  PASS: ${description}"
    (( passed++ ))
  else
    echo "  FAIL: ${description}"
    echo "        expected line: ${expected}"
    echo "        actual output:"
    sed 's/^/          /' <<<"${output}" | grep -E "mongodb|loglevel|zip.exclude" || sed 's/^/          /' <<<"${output}"
    (( failed++ ))
  fi
}

# Asserts that no line of the generated file matches a pattern.
assert_no_match() {
  local description="$1" pattern="$2" output="$3"
  if grep -Eq -- "${pattern}" <<<"${output}"; then
    echo "  FAIL: ${description}"
    echo "        offending line(s):"
    grep -E -- "${pattern}" <<<"${output}" | sed 's/^/          /'
    (( failed++ ))
  else
    echo "  PASS: ${description}"
    (( passed++ ))
  fi
}

echo "=== Case 1: all five SPC_DATABASE_* set - full authenticated URI ==="
out="$(run_init \
  SPC_DATABASE_HOST=serverpackcreatordb \
  SPC_DATABASE_PORT=27017 \
  SPC_DATABASE_DB=serverpackcreatordb \
  SPC_DATABASE_USERNAME=spcuser \
  SPC_DATABASE_PASSWORD=spcpass)"
assert_line "authenticated URI is composed from the env" \
  'spring.mongodb.uri=mongodb\://spcuser\:spcpass@serverpackcreatordb\:27017/serverpackcreatordb' \
  "${out}"

echo "=== Case 2: auth-less Mongo (no username/password) - must still be a parseable URI ==="
# Spring Boot 4 hands any non-null spring.mongodb.uri straight to com.mongodb.ConnectionString,
# which rejects anything not starting with mongodb:// or mongodb+srv://. A degenerate value is a hard
# startup failure, so this must produce a valid auth-less URI - or no line at all.
out="$(run_init \
  SPC_DATABASE_HOST=serverpackcreatordb \
  SPC_DATABASE_PORT=27017 \
  SPC_DATABASE_DB=serverpackcreatordb)"
assert_no_match "no degenerate 'mongodb:' URI is written" \
  '^spring\.mongodb\.uri=mongodb\\?:$' "${out}"
assert_line "auth-less URI omits the credentials segment" \
  'spring.mongodb.uri=mongodb\://serverpackcreatordb\:27017/serverpackcreatordb' \
  "${out}"

echo "=== Case 3: SPC_LOG_LEVEL is expanded, not written literally ==="
out="$(run_init SPC_LOG_LEVEL=DEBUG)"
assert_line "loglevel carries the value" \
  'de.griefed.serverpackcreator.loglevel=DEBUG' "${out}"
assert_no_match "loglevel is not the literal placeholder" \
  '\{S?PC?_?LOG_LEVEL\}|loglevel=\{' "${out}"

echo "=== Case 4: zip-exclude overrides are not wrapped in literal braces ==="
out="$(run_init \
  SPC_SERVERPACK_ZIP_EXCLUDE=server.jar,other.jar \
  SPC_SERVERPACK_ZIP_EXCLUDE_ENABLED=true)"
assert_line "zip.exclude carries the bare value" \
  'de.griefed.serverpackcreator.serverpack.zip.exclude=server.jar,other.jar' "${out}"
assert_line "zip.exclude.enabled carries the bare value" \
  'de.griefed.serverpackcreator.serverpack.zip.exclude.enabled=true' "${out}"
assert_no_match "no braces leak into any value" 'zip\.exclude(\.enabled)?=\{' "${out}"

echo "=== Case 5: unconditional lines are always present ==="
out="$(run_init SPC_LOG_LEVEL=INFO)"
assert_line "tomcat basedir is pinned to the app dir" \
  'server.tomcat.basedir=/app/serverpackcreator' "${out}"
assert_line "home is pinned to the app dir" \
  'de.griefed.serverpackcreator.home=/app/serverpackcreator' "${out}"

echo
echo "===================================="
echo " passed: ${passed}   failed: ${failed}"
echo "===================================="
(( failed == 0 )) || exit 1
