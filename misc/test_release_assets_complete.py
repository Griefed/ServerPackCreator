"""Self-test for release-assets-complete.py, the news job's check that the GitHub release carries every asset
the canonical Forgejo release does. Run with `python3 -B -m unittest discover -s misc -p 'test_*.py'`."""

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

CHECK = Path(__file__).resolve().parent / "release-assets-complete.py"


def run_check(canonical: list, mirror: list) -> subprocess.CompletedProcess:
    with tempfile.TemporaryDirectory() as tmp:
        files = []
        for name, assets in (("canonical.json", canonical), ("mirror.json", mirror)):
            path = Path(tmp) / name
            path.write_text(json.dumps({"assets": [{"name": a} for a in assets]}))
            files.append(str(path))
        return subprocess.run([sys.executable, str(CHECK), *files], capture_output=True, text=True)


TWELVE = [f"asset-{n}.jar" for n in range(12)]


class ReleaseAssetsComplete(unittest.TestCase):

    def test_a_complete_mirror_passes_and_reports_the_count(self):
        result = run_check(TWELVE, list(reversed(TWELVE)))
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual("12", result.stdout.strip())

    def test_a_partially_uploaded_mirror_fails_naming_what_is_missing(self):
        result = run_check(TWELVE, TWELVE[:5])
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("asset-5.jar", result.stderr)
        self.assertIn("7 of 12", result.stderr)

    def test_an_empty_mirror_fails(self):
        self.assertEqual(1, run_check(TWELVE, []).returncode)

    def test_a_canonical_release_without_assets_cannot_vouch_for_anything(self):
        result = run_check([], [])
        self.assertEqual(1, result.returncode)
        self.assertIn("no assets", result.stderr)

    def test_extra_assets_on_the_mirror_do_not_matter(self):
        self.assertEqual(0, run_check(TWELVE, TWELVE + ["extra.txt"]).returncode)


if __name__ == "__main__":
    unittest.main()
