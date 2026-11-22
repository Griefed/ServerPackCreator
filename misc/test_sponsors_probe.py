"""Self-test for sponsors-probe.py, which decides whether update-readme.yml may let the sponsors action
overwrite README.md's sponsor block. Run with `python3 -B -m unittest discover -s misc -p 'test_*.py'`."""

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

PROBE = Path(__file__).resolve().parent / "sponsors-probe.py"


def judge(response: dict) -> subprocess.CompletedProcess:
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "response.json"
        path.write_text(json.dumps(response))
        return subprocess.run([sys.executable, str(PROBE), str(path)], capture_output=True, text=True)


def sponsorships(*privacy_levels: str) -> dict:
    nodes = [{"sponsorEntity": {"login": f"s{n}"}, "privacyLevel": level}
             for n, level in enumerate(privacy_levels)]
    return {"data": {"viewer": {"login": "Griefed",
                                "sponsorshipsAsMaintainer": {"totalCount": len(nodes), "nodes": nodes}}}}


class SponsorsProbe(unittest.TestCase):

    def test_public_sponsors_let_the_action_run(self):
        result = judge(sponsorships("PUBLIC", "PRIVATE", "PUBLIC"))
        self.assertEqual(0, result.returncode, result.stdout)
        self.assertIn("2 of them public", result.stdout)

    def test_a_partial_graphql_error_is_printed_but_not_fatal_when_sponsors_came_back(self):
        response = sponsorships("PUBLIC")
        response["errors"] = [{"type": "INSUFFICIENT_SCOPES", "path": ["viewer", "x"], "message": "no"}]
        result = judge(response)
        self.assertEqual(0, result.returncode, result.stdout)
        self.assertIn("::error::GitHub GraphQL [INSUFFICIENT_SCOPES] at viewer.x: no", result.stdout)

    def test_a_transport_rejection_fails_and_says_so(self):
        result = judge({"message": "Bad credentials"})
        self.assertEqual(1, result.returncode)
        self.assertIn("GitHub refused the request: Bad credentials", result.stdout)

    def test_absent_sponsorship_data_fails_as_the_action_would_report_it(self):
        result = judge({"data": {"viewer": {"login": "Griefed"}},
                        "errors": [{"type": "INSUFFICIENT_SCOPES", "message": "read:user"}]})
        self.assertEqual(1, result.returncode)
        self.assertIn("sponsorshipsAsMaintainer` is absent", result.stdout)

    def test_only_private_sponsors_would_write_an_empty_block_and_fail(self):
        result = judge(sponsorships("PRIVATE", "PRIVATE"))
        self.assertEqual(1, result.returncode)
        self.assertIn("No PUBLIC sponsorships", result.stdout)

    def test_no_sponsors_at_all_fails(self):
        self.assertEqual(1, judge(sponsorships()).returncode)


if __name__ == "__main__":
    unittest.main()
