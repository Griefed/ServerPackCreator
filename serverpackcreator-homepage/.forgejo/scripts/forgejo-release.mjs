#!/usr/bin/env node
/**
 * Creates (or updates) a Forgejo release.
 *
 * Replaces @semantic-release/gitlab, which speaks the GitLab API and therefore
 * cannot be used against Forgejo. It is driven by @semantic-release/exec from
 * .releaserc.yml:
 *
 *   node .forgejo/scripts/forgejo-release.mjs <version>   # notes on stdin
 *
 * The release notes are piped in rather than passed as an argument so that
 * markdown containing quotes, backticks or `$` cannot be mangled by, or
 * injected into, the shell.
 *
 * Environment:
 *   RELEASE_TOKEN     required, personal access token with repository write access
 *   RELEASE_API_URL   optional, defaults to $GITHUB_SERVER_URL/api/v1
 *   RELEASE_REPOSITORY optional, defaults to $GITHUB_REPOSITORY ("owner/repo")
 *
 * Requires Node 18+ for the global fetch(); the release job runs Node 24.
 */

const fail = (message) => {
  console.error(`forgejo-release: ${message}`);
  process.exit(1);
};

const version = process.argv[2];
if (!version) fail('missing <version> argument');

const token = process.env.RELEASE_TOKEN;
if (!token) fail('RELEASE_TOKEN is not set');

const serverUrl = process.env.GITHUB_SERVER_URL;
const apiUrl = (process.env.RELEASE_API_URL || (serverUrl ? `${serverUrl}/api/v1` : '')).replace(/\/+$/, '');
if (!apiUrl) fail('neither RELEASE_API_URL nor GITHUB_SERVER_URL is set');

const repository = process.env.RELEASE_REPOSITORY || process.env.GITHUB_REPOSITORY;
if (!repository) fail('neither RELEASE_REPOSITORY nor GITHUB_REPOSITORY is set');

const readStdin = async () => {
  const chunks = [];
  for await (const chunk of process.stdin) chunks.push(chunk);
  return Buffer.concat(chunks).toString('utf8').trim();
};

const request = (url, method, body) =>
  fetch(url, {
    method,
    headers: {
      Authorization: `token ${token}`,
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(body),
  });

const notes = await readStdin();
const releasesUrl = `${apiUrl}/repos/${repository}/releases`;

// tagFormat in .releaserc.yml is '${version}', so the tag is the bare version.
const payload = {
  tag_name: version,
  name: version,
  body: notes,
  draft: false,
  prerelease: false,
};

let response = await request(releasesUrl, 'POST', payload);

// semantic-release retries the publish step after a partial failure, and
// @semantic-release/git has already pushed the tag by this point, so an
// existing release is an expected state rather than an error.
if (response.status === 409) {
  const existing = await fetch(`${releasesUrl}/tags/${encodeURIComponent(version)}`, {
    headers: { Authorization: `token ${token}`, Accept: 'application/json' },
  });
  if (!existing.ok) {
    fail(`release ${version} already exists but could not be read (HTTP ${existing.status})`);
  }
  const { id } = await existing.json();
  response = await request(`${releasesUrl}/${id}`, 'PATCH', payload);
}

if (!response.ok) {
  fail(`${response.status} ${response.statusText} from ${releasesUrl}\n${await response.text()}`);
}

const { html_url: htmlUrl } = await response.json();
console.log(`Published Forgejo release ${version}: ${htmlUrl}`);
