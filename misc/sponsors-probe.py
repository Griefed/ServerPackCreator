#!/usr/bin/env python3
"""Judge GitHub's answer to the sponsors action's own GraphQL query; exit 1 when the action would write an
empty sponsor block over README.md.

Usage: sponsors-probe.py <graphql-response.json>

`update-readme.yml` sends the query and runs this before `JamesIves/github-sponsors-readme-action`, which
cannot tell a rejected query from an account without sponsors and exits 0 either way.
"""

import json
import sys


def main() -> int:
    with open(sys.argv[1], encoding="utf-8") as handle:
        response = json.load(handle)

    # Printed before anything is judged: a partial GraphQL failure answers 200 with BOTH `data` and
    # `errors`, so an error here is not necessarily fatal and is not necessarily visible anywhere else.
    # `path` is what says which field was refused.
    for error in response.get('errors', []):
        path = '.'.join(str(p) for p in error.get('path', [])) or '<root>'
        print(f"::error::GitHub GraphQL [{error.get('type', 'ERROR')}] at {path}: {error.get('message')}")

    # A transport-level rejection (401/403) answers with GitHub's REST-shaped body -- a bare `message`, no
    # `errors` and no `data` -- so it would otherwise reach the failure below with nothing printed to
    # explain it.
    if 'data' not in response and 'message' in response:
        print(f"::error::GitHub refused the request: {response['message']}")

    maintainer = (response.get('data') or {}).get('viewer') or {}
    sponsorships = maintainer.get('sponsorshipsAsMaintainer')
    if sponsorships is None:
        # Exactly the condition the action tests before logging "No sponsorship data was found".
        print('::error::`data.viewer.sponsorshipsAsMaintainer` is absent, which is the state the sponsors '
              'action reports as "No sponsorship data was found" and then writes an empty block for. A '
              'classic PAT needs the read:user scope, plus read:org for an organization account; the errors '
              'above, if any, name the actual refusal.')
        return 1

    nodes = sponsorships.get('nodes') or []
    renderable = [n for n in nodes if n and n.get('privacyLevel') != 'PRIVATE']
    print(f"{maintainer.get('login')} has {sponsorships['totalCount']} sponsorships visible to this token; "
          f"{len(nodes)} returned, {len(renderable)} of them public.")
    for node in renderable:
        entity = node.get('sponsorEntity') or {}
        print(f"  - {entity.get('login') or '<no login>'} ({node.get('privacyLevel')})")

    # `include-private` is false, so the action renders exactly `renderable`. Zero of them means the next
    # step overwrites the sponsor block with nothing, and for this account that is never a legitimate
    # answer, so it fails here rather than being committed.
    if not renderable:
        print('::error::No PUBLIC sponsorships came back, so the sponsors action would write an empty block '
              'over whatever README.md currently holds. Refusing to continue.')
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
