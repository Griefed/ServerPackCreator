/**
 * A `fetch` stand-in that answers from a map of URL -> { body, link } and records every URL asked
 * for, so a test can assert what the counter requested as well as what it computed.
 */
export function fetchStub(responses) {
  const requested = []

  const fetch = (url, init) => {
    requested.push(String(url))
    const answer = responses[String(url)]
    if (answer === undefined) {
      return Promise.resolve(
        new Response('[]', { status: 404, headers: { 'content-type': 'application/json' } })
      )
    }
    if (answer.status !== undefined && answer.status >= 400) {
      return Promise.resolve(new Response('', { status: answer.status }))
    }
    void init
    const headers = { 'content-type': 'application/json' }
    if (answer.link !== undefined) headers.link = answer.link
    return Promise.resolve(new Response(JSON.stringify(answer.body), { status: 200, headers }))
  }

  return { fetch, requested }
}

/** A release carrying `counts` as one asset each. */
export function release(...counts) {
  return { assets: counts.map((download_count) => ({ download_count })) }
}
