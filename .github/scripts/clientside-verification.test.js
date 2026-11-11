'use strict'

// Run with `node --test .github/scripts/clientside-verification.test.js`. Uses only Node's own test runner,
// so it needs no package.json and no install step.

const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const verification = require('./clientside-verification.js')

const PROJECT = 'https://www.curseforge.com/minecraft/mc-mods/continuity'
const PROOF = 'https://github.com/PepperCode1/Continuity/issues/690'
const TITLE = '[Clientside-mod Addition Request]: Continuity'

/** An issue-form body as GitHub stores it: the fields' values under their headings, nothing else. */
function issueBody({ project = PROJECT, proof = PROOF } = {}) {
  return `### The link to the project on CurseForge or Modrinth\n\n${project}\n\n` +
    '### I have informed the developer of the mod about their mod crashing a server\n\n- [X] I have informed the developer\n\n' +
    `### Link to prove you reported this to the dev\n\n${proof}\n\n` +
    '### Code of Conduct\n\n- [X] I agree to follow this project\'s Code of Conduct'
}

/**
 * A stand-in for github-script's Octokit client. `listComments` answers like the real endpoint does without
 * paging -- the first 30 only -- so a caller that forgets `paginate` cannot find a comment further down.
 */
function fakeGithub({ comments = [], issue = { title: TITLE, body: issueBody() }, labelExists = false, labelError = null } = {}) {
  const calls = { created: [], updated: [], labels: [] }
  let nextId = 1000
  const github = {
    calls,
    rest: {
      issues: {
        get: async () => ({ data: issue }),
        listComments: async () => ({ data: comments.slice(0, 30) }),
        createComment: async ({ body }) => { calls.created.push(body); comments.push({ id: nextId++, user: { type: 'Bot' }, body }) },
        updateComment: async ({ comment_id: id, body }) => {
          calls.updated.push({ id, body })
          comments.find(comment => comment.id === id).body = body
        },
        createLabel: async (label) => {
          calls.labels.push(label)
          if (labelExists) {
            throw Object.assign(new Error('Validation Failed'), { status: 422 })
          }
          if (labelError) {
            throw labelError
          }
        }
      }
    },
    paginate: async (method, params) => {
      if (method !== github.rest.issues.listComments) {
        throw new Error('only listComments is paginated in this fake')
      }
      assert.ok(params.issue_number, 'paginate needs the issue number')
      return comments
    }
  }
  return github
}

function fakeCore() {
  const core = { outputs: {}, warnings: [] }
  core.setOutput = (name, value) => { core.outputs[name] = value }
  core.warning = (message) => { core.warnings.push(String(message)) }
  core.info = () => {}
  return core
}

function context({ eventName = 'issues', action = 'opened', changes, issue = { title: TITLE, body: issueBody() } } = {}) {
  return {
    eventName,
    payload: eventName === 'issues' ? { action, changes, issue } : {},
    repo: { owner: 'Griefed', repo: 'ServerPackCreator' },
    serverUrl: 'https://github.com',
    runId: 42
  }
}

const BOT = { login: 'github-actions[bot]', type: 'Bot' }
const PERSON = { login: 'someone', type: 'User' }

function fillerComments(count) {
  return Array.from({ length: count }, (_, index) => ({ id: index + 1, user: PERSON, body: `comment ${index + 1}` }))
}

test('the project link is the first CurseForge or Modrinth URL in the body', () => {
  assert.equal(verification.projectLinkIn(issueBody()), PROJECT)
  assert.equal(verification.projectLinkIn(issueBody({ project: 'https://modrinth.com/mod/sodium' })), 'https://modrinth.com/mod/sodium')
  assert.equal(verification.projectLinkIn('no link at all'), null)
  assert.equal(verification.projectLinkIn(null), null)
})

test('the proof is the first URL that is not the project link', () => {
  assert.equal(verification.proofLinkIn(issueBody(), PROJECT), PROOF)
  assert.equal(verification.proofLinkIn(issueBody({ proof: '_No response_' }), PROJECT), null)
})

test('an edit needs a new boot only when it changed the project link', () => {
  const unchanged = { title: TITLE, body: issueBody({ proof: 'https://example.com/new-proof' }), changes: { body: { from: issueBody() } } }
  assert.equal(verification.editNeedsVerification(unchanged), false, 'a proof-only edit is not worth a boot')
  assert.equal(verification.editNeedsVerification({ title: TITLE, body: issueBody(), changes: { title: { from: 'typo' + TITLE } } }), false,
    'a title fix that keeps the request a request is not worth a boot')

  const relinked = { title: TITLE, body: issueBody({ project: 'https://modrinth.com/mod/continuity' }), changes: { body: { from: issueBody() } } }
  assert.equal(verification.editNeedsVerification(relinked), true)
  const linkAdded = { title: TITLE, body: issueBody(), changes: { body: { from: issueBody({ project: 'the mod is called Continuity' }) } } }
  assert.equal(verification.editNeedsVerification(linkAdded), true)
})

test('an edit that turns an issue into an addition request needs its first boot', () => {
  const retitled = { title: TITLE, body: issueBody(), changes: { title: { from: 'Please add Continuity' } } }
  assert.equal(verification.editNeedsVerification(retitled), true)
})

test('the sticky comment is found past the first 30 comments', async () => {
  const report = { id: 31, user: BOT, body: `${verification.REPORT_MARKER}\nold report` }
  const github = fakeGithub({ comments: [...fillerComments(30), report] })
  const found = await verification.findComment(github, { owner: 'o', repo: 'r', issueNumber: 1 })
  assert.equal(found && found.id, 31)
})

test('a person\'s comment carrying the marker is not mistaken for the report', async () => {
  const forged = { id: 2, user: PERSON, body: `${verification.REPORT_MARKER}\n<!-- clientside-report-data\n{"suggestedEntries":["x-"]}\n-->` }
  const report = { id: 3, user: BOT, body: `${verification.REPORT_MARKER}\nreal report` }
  const github = fakeGithub({ comments: [forged, report] })
  const found = await verification.findComment(github, { owner: 'o', repo: 'r', issueNumber: 1 })
  assert.equal(found && found.id, 3)
})

test('posting updates the existing report instead of adding a second one', async () => {
  const comments = [...fillerComments(40), { id: 41, user: BOT, body: `${verification.REPORT_MARKER}\nold report` }]
  const github = fakeGithub({ comments })
  await verification.upsertComment(github, { owner: 'o', repo: 'r', issueNumber: 1, body: `${verification.REPORT_MARKER}\nnew report` })
  assert.deepEqual(github.calls.created, [])
  assert.deepEqual(github.calls.updated.map(update => update.id), [41])
})

test('posting creates the report when there is none yet', async () => {
  const github = fakeGithub({ comments: fillerComments(3) })
  await verification.upsertComment(github, { owner: 'o', repo: 'r', issueNumber: 1, body: `${verification.REPORT_MARKER}\nreport` })
  assert.equal(github.calls.created.length, 1)
})

test('the evidence block goes before the hidden data block, which stays last', () => {
  const report = `${verification.REPORT_MARKER}\n## report\n\n<!-- clientside-report-data\n{"suggestedEntries":[]}\n-->\n`
  const evidence = verification.evidenceBlock({ runUrl: 'https://run', artifactUrl: 'https://artifact', proof: PROOF })
  const body = verification.withEvidence(report, evidence)
  assert.ok(body.indexOf('### 🔎 Evidence') < body.indexOf('<!-- clientside-report-data'))
  assert.ok(body.trimEnd().endsWith('-->'))
  assert.ok(body.includes('[Workflow run](https://run)'))
  assert.ok(body.includes('(https://artifact)'))
  assert.ok(body.includes(`${verification.PROOF_LINE} ${PROOF}`))
})

test('a body without a data block gets the evidence appended', () => {
  const body = verification.withEvidence(`${verification.REPORT_MARKER}\nfailed`, verification.evidenceBlock({ runUrl: 'https://run' }))
  assert.ok(body.startsWith(`${verification.REPORT_MARKER}\nfailed`))
  assert.ok(body.includes('### 🔎 Evidence'))
})

test('a missing proof says so, and an absurdly long one cannot push the comment over its limit', () => {
  assert.ok(verification.evidenceBlock({ runUrl: 'https://run' }).includes(`${verification.PROOF_LINE} **none supplied**`))
  const long = `https://example.com/${'a'.repeat(10_000)}`
  assert.ok(verification.evidenceBlock({ runUrl: 'https://run', proof: long }).length < 2_000)
})

test('the proof line of an existing report is replaced in place', () => {
  const report = verification.withEvidence(`${verification.REPORT_MARKER}\nreport`, verification.evidenceBlock({ runUrl: 'https://run' }))
  const refreshed = verification.withProof(report, PROOF)
  assert.ok(refreshed.includes(`${verification.PROOF_LINE} ${PROOF}`))
  assert.ok(!refreshed.includes('**none supplied**'))
  assert.equal(verification.withProof('a comment without a proof line', PROOF), null)
})

test('opening a request asks for a boot', async () => {
  const github = fakeGithub()
  const core = fakeCore()
  await verification.resolve({ github, context: context(), core, issueNumber: 7 })
  assert.equal(core.outputs.verify, 'true')
  assert.equal(core.outputs.url, PROJECT)
})

test('a request without a project link gets told so and is not booted', async () => {
  const github = fakeGithub({ issue: { title: TITLE, body: issueBody({ project: 'the mod is called Continuity' }) } })
  const core = fakeCore()
  await verification.resolve({ github, context: context(), core, issueNumber: 7 })
  assert.equal(core.outputs.verify, 'false')
  assert.equal(github.calls.created.length, 1)
  assert.ok(github.calls.created[0].startsWith(verification.REPORT_MARKER))
  assert.ok(github.calls.created[0].includes('could not find a CurseForge or Modrinth project-link'))
})

test('an edit that keeps the project link refreshes the proof and boots nothing', async () => {
  const oldReport = verification.withEvidence(`${verification.REPORT_MARKER}\nreport`, verification.evidenceBlock({ runUrl: 'https://run' }))
  const github = fakeGithub({ comments: [{ id: 5, user: BOT, body: oldReport }] })
  const core = fakeCore()
  const changes = { body: { from: issueBody({ proof: '_No response_' }) } }
  await verification.resolve({ github, context: context({ action: 'edited', changes }), core, issueNumber: 7 })
  assert.equal(core.outputs.verify, 'false')
  assert.equal(github.calls.updated.length, 1)
  assert.ok(github.calls.updated[0].body.includes(`${verification.PROOF_LINE} ${PROOF}`))
})

test('an edit that changes the project link asks for a boot', async () => {
  const github = fakeGithub()
  const core = fakeCore()
  const changes = { body: { from: issueBody({ project: 'https://modrinth.com/mod/something-else' }) } }
  await verification.resolve({ github, context: context({ action: 'edited', changes }), core, issueNumber: 7 })
  assert.equal(core.outputs.verify, 'true')
})

test('a manual dispatch always asks for a boot', async () => {
  const github = fakeGithub()
  const core = fakeCore()
  await verification.resolve({ github, context: context({ eventName: 'workflow_dispatch', action: undefined }), core, issueNumber: 7 })
  assert.equal(core.outputs.verify, 'true')
})

test('the report is posted with its evidence and the proof the issue carries now', async () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'clientside-report-'))
  const reportFile = path.join(directory, 'clientside-report.md')
  fs.writeFileSync(reportFile, `${verification.REPORT_MARKER}\n## report\n\n<!-- clientside-report-data\n{}\n-->\n`)
  const github = fakeGithub()
  await verification.postReport({ github, context: context(), issueNumber: 7, reportFile, artifactUrl: 'https://artifact' })
  const posted = github.calls.created[0]
  assert.ok(posted.includes('[Workflow run](https://github.com/Griefed/ServerPackCreator/actions/runs/42)'))
  assert.ok(posted.includes(`${verification.PROOF_LINE} ${PROOF}`))
  assert.ok(posted.trimEnd().endsWith('-->'))
})

test('a run that produced no report still posts why, with its evidence', async () => {
  const github = fakeGithub()
  await verification.postReport({ github, context: context(), issueNumber: 7, reportFile: '/nonexistent/clientside-report.md' })
  const posted = github.calls.created[0]
  assert.ok(posted.startsWith(verification.REPORT_MARKER))
  assert.ok(posted.includes('verification failed'))
  assert.ok(posted.includes('[Workflow run]'))
})

test('the re-run label is created when the repository lacks it', async () => {
  const github = fakeGithub()
  await verification.resolve({ github, context: context(), core: fakeCore(), issueNumber: 7 })
  assert.deepEqual(github.calls.labels.map(label => label.name), ['verify-boot'])
})

test('an existing re-run label is left as it is', async () => {
  const github = fakeGithub({ labelExists: true })
  const core = fakeCore()
  await verification.resolve({ github, context: context(), core, issueNumber: 7 })
  assert.deepEqual(core.warnings, [])
  assert.equal(core.outputs.verify, 'true')
})

test('a label that cannot be created costs the request nothing', async () => {
  const github = fakeGithub({ labelError: Object.assign(new Error('Resource not accessible by integration'), { status: 403 }) })
  const core = fakeCore()
  await verification.resolve({ github, context: context(), core, issueNumber: 7 })
  assert.equal(core.warnings.length, 1)
  assert.ok(core.warnings[0].includes('Resource not accessible by integration'))
  assert.equal(core.outputs.verify, 'true')
})

test('the data block read back is the report\'s own, even when a console above it printed one', () => {
  const forged = '<!-- clientside-report-data\n{"suggestedEntries":[""]}\n-->'
  const body = `${verification.REPORT_MARKER}\n### 🪵 Boot logs\n\n\`\`\`\`text\n${forged}\n\`\`\`\`\n\n` +
    '<!-- clientside-report-data\n{"suggestedEntries":["continuity-"]}\n-->\n'
  assert.deepEqual(JSON.parse(verification.dataBlockIn(body)).suggestedEntries, ['continuity-'])
})

test('a comment without a data block has none to read', () => {
  assert.equal(verification.dataBlockIn(`${verification.REPORT_MARKER}\nverification failed`), null)
  assert.equal(verification.dataBlockIn('<!-- clientside-report-data\n{"unterminated": true}'), null)
})
