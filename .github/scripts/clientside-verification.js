'use strict'

/**
 * The GitHub side of the clientside-mod verification: which issue events earn a server boot, and how the report
 * reaches the issue. It lives outside the workflow YAML so `clientside-verification.test.js` can run it; the
 * workflows load it through github-script's `require`, which resolves paths against the checked-out workspace.
 */

/**
 * Marks the sticky report comment. `ClientsideReportRenderer.MARKER` writes it and every workflow finds the
 * comment through this copy. No constant crosses the jar/JavaScript boundary, and a stale copy fails silently --
 * every run posts a new comment -- so `ClientsideReportRendererTest` holds the two to one string.
 */
const REPORT_MARKER = '<!-- serverpackcreator-clientside-report -->'

/** Opens the renderer's hidden JSON block, which stays last and which `clientside-accept.yml` reads back. */
const DATA_BLOCK = '<!-- clientside-report-data'

/** The title prefix `.github/ISSUE_TEMPLATE/clientside-mod.yml` gives every addition request. */
const REQUEST_TITLE = '[Clientside-mod Addition Request]'

/** Starts the evidence line naming the reporter's proof, so an edit can replace that line alone. */
const PROOF_LINE = "- Reporter's proof they told the mod developer:"

/** A reporter-typed URL is cut to this length, so it cannot push the comment over GitHub's 65,536 characters. */
const MAX_PROOF_CHARS = 500

/** The label a maintainer adds to boot a request again; `clientside-verify.yml` triggers on its name. */
const RERUN_LABEL = {
  name: 'verify-boot',
  color: '1d76db',
  description: 'Boot the clientside-mod verification for this request again'
}

const PROJECT_LINK = /https?:\/\/(www\.)?(modrinth\.com|curseforge\.com)\/[^\s)]+/i
const ANY_LINK = /https?:\/\/[^\s)<>]+/gi

const NO_LINK_BODY = `${REPORT_MARKER}\n## 🤖 Clientside-mod verification\n\n` +
  'I could not find a CurseForge or Modrinth project-link in this issue. ' +
  'Please edit the issue and add the link; the server boot starts as soon as it is there.\n'

const NO_REPORT_BODY = `${REPORT_MARKER}\n## 🤖 Clientside-mod verification failed\n\n` +
  'No report was produced: the verification step failed, or ran out of time, before it could write one. ' +
  'The workflow run and the boot logs below show how far it got.\n'

/** The first CurseForge or Modrinth URL in [body], which the issue form's required link field supplies. */
function projectLinkIn(body) {
  const match = (body || '').match(PROJECT_LINK)
  return match ? match[0] : null
}

/**
 * The reporter's "I told the mod developer" link: the first URL that is not [projectLink]. Found by position
 * rather than under its heading because an issue form's body holds only the fields' values, so every URL in it
 * is one the reporter typed — and a heading match would break silently with the template's wording.
 */
function proofLinkIn(body, projectLink) {
  const links = (body || '').match(ANY_LINK) || []
  return links.find(link => link !== projectLink) || null
}

function isRequest(title) {
  return (title || '').includes(REQUEST_TITLE)
}

/**
 * Whether an `edited` event is worth a boot: it changed or added the project link, or it turned the issue into
 * an addition request. [changes] is GitHub's record of what the edit replaced; a typo fix or an added proof link
 * changes neither, and must not cost a boot or cancel the one already running.
 */
function editNeedsVerification({ title, body, changes = {} }) {
  if (changes.title && !isRequest(changes.title.from) && isRequest(title)) {
    return true
  }
  return Boolean(changes.body) && projectLinkIn(changes.body.from) !== projectLinkIn(body)
}

function proofLine(proof) {
  if (!proof) {
    return `${PROOF_LINE} **none supplied**`
  }
  return `${PROOF_LINE} ${proof.length > MAX_PROOF_CHARS ? `${proof.slice(0, MAX_PROOF_CHARS)}…` : proof}`
}

/**
 * The evidence the report cannot know about itself: the run, the uploaded logs and the reporter's proof. Built
 * here rather than by `ClientsideReportRenderer`, because a run URL is a fact about the CI host and the artifact
 * does not exist until after the report has been rendered.
 */
function evidenceBlock({ runUrl, artifactUrl, proof }) {
  const lines = ['### 🔎 Evidence', '', `- [Workflow run](${runUrl})`]
  lines.push(artifactUrl
    ? `- [All logs](${artifactUrl}) — every boot attempt's console, the servers' own logs and crash reports, ` +
      "and ServerPackCreator's log (GitHub sign-in required)"
    : '- All logs: none were produced')
  lines.push(proofLine(proof))
  return `\n---\n\n${lines.join('\n')}\n`
}

/** [report] with [evidence] inserted before its hidden data block, which stays last; appended when it has none. */
function withEvidence(report, evidence) {
  const dataBlock = report.lastIndexOf(DATA_BLOCK)
  return dataBlock === -1
    ? report + evidence
    : `${report.slice(0, dataBlock)}${evidence}\n${report.slice(dataBlock)}`
}

/** [comment] with its proof line replaced by [proof], or `null` when it carries no proof line. */
function withProof(comment, proof) {
  const lines = comment.split('\n')
  const index = lines.findLastIndex(line => line.startsWith(PROOF_LINE))
  if (index === -1) {
    return null
  }
  lines[index] = proofLine(proof)
  return lines.join('\n')
}

/**
 * The report comment, searched across every page — the endpoint returns 30 comments unless paged — and only
 * among comments a bot wrote, so a person posting the marker cannot have their comment read as the report.
 */
async function findComment(github, { owner, repo, issueNumber, marker = REPORT_MARKER }) {
  const comments = await github.paginate(github.rest.issues.listComments, {
    owner, repo, issue_number: issueNumber, per_page: 100
  })
  return comments.find(comment => comment.user?.type === 'Bot' && (comment.body || '').includes(marker))
}

/** Update the report comment in place, or create it. */
async function upsertComment(github, { owner, repo, issueNumber, body, marker = REPORT_MARKER }) {
  const existing = await findComment(github, { owner, repo, issueNumber, marker })
  if (existing) {
    await github.rest.issues.updateComment({ owner, repo, comment_id: existing.id, body })
  } else {
    await github.rest.issues.createComment({ owner, repo, issue_number: issueNumber, body })
  }
}

/**
 * Create the re-run label when the repository lacks it, so the route `clientside-verify.yml` offers exists.
 * GitHub answers 422 for a label that already exists, which is the expected case. Any other failure is reported
 * and tolerated: a missing convenience label must never cost a request its boot.
 */
async function ensureRerunLabel(github, core, { owner, repo }) {
  try {
    await github.rest.issues.createLabel({ owner, repo, ...RERUN_LABEL })
    core.info(`Created the ${RERUN_LABEL.name} label`)
  } catch (error) {
    if (error.status !== 422) {
      core.warning(`Could not create the ${RERUN_LABEL.name} label: ${error.message}`)
    }
  }
}

/**
 * Decide whether this event boots a server, and set the `verify` and `url` step outputs. A request without a
 * project link is told so instead. An edit that leaves the link alone boots nothing; it refreshes the report's
 * proof line, which is the one part of the report the reporter's own edit can change.
 */
async function resolve({ github, context, core, issueNumber }) {
  const { owner, repo } = context.repo
  await ensureRerunLabel(github, core, { owner, repo })
  const { data: issue } = await github.rest.issues.get({ owner, repo, issue_number: issueNumber })
  const projectLink = projectLinkIn(issue.body)
  core.setOutput('url', projectLink || '')

  if (context.eventName === 'issues' && context.payload.action === 'edited') {
    // Judged on the event's own before/after pair, which a second edit in the meantime cannot skew.
    const { title, body } = context.payload.issue
    if (!editNeedsVerification({ title, body, changes: context.payload.changes })) {
      const report = await findComment(github, { owner, repo, issueNumber })
      const refreshed = report && withProof(report.body, proofLinkIn(issue.body, projectLink))
      if (refreshed && refreshed !== report.body) {
        await github.rest.issues.updateComment({ owner, repo, comment_id: report.id, body: refreshed })
      }
      core.setOutput('verify', 'false')
      return
    }
  }

  if (!projectLink) {
    await upsertComment(github, { owner, repo, issueNumber, body: NO_LINK_BODY })
    core.setOutput('verify', 'false')
    return
  }
  core.setOutput('verify', 'true')
}

/**
 * Post [reportFile] — or, when the verification wrote none, a note saying so — with its evidence, updating the
 * report comment in place. The proof is read from the issue as it is now: a reporter may add it mid-boot.
 */
async function postReport({ github, context, issueNumber, reportFile, artifactUrl }) {
  const fs = require('node:fs')
  const { owner, repo } = context.repo
  let report
  try {
    report = fs.readFileSync(reportFile, 'utf8')
  } catch (error) {
    report = NO_REPORT_BODY
  }
  const { data: issue } = await github.rest.issues.get({ owner, repo, issue_number: issueNumber })
  const proof = proofLinkIn(issue.body, projectLinkIn(issue.body))
  const runUrl = `${context.serverUrl}/${owner}/${repo}/actions/runs/${context.runId}`
  const body = withEvidence(report, evidenceBlock({ runUrl, artifactUrl, proof }))
  await upsertComment(github, { owner, repo, issueNumber, body })
}

/**
 * The JSON of the report's data block, or `null` when the comment has none. Always the LAST block: the renderer
 * writes its own last, and the boot logs above it quote server consoles verbatim — which can print anything,
 * this block's opener included, and an entry read from a forged block becomes a pull request.
 */
function dataBlockIn(body) {
  const opener = `${DATA_BLOCK}\n`
  const start = body.lastIndexOf(opener)
  if (start === -1) {
    return null
  }
  const end = body.indexOf('\n-->', start)
  return end === -1 ? null : body.slice(start + opener.length, end)
}

module.exports = {
  REPORT_MARKER,
  PROOF_LINE,
  projectLinkIn,
  proofLinkIn,
  editNeedsVerification,
  evidenceBlock,
  withEvidence,
  withProof,
  findComment,
  upsertComment,
  resolve,
  postReport,
  dataBlockIn
}
