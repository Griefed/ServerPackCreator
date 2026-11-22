"""Self-test for ci-env-reference-check.py: the defect it exists for, its four learned exclusions, and
both allowlist directions. Run with `python3 -B -m unittest discover -s misc -p 'test_*.py'`."""

import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

CHECK = Path(__file__).resolve().parent / "ci-env-reference-check.py"


def run_check(workflow: str, allowlist: str = "") -> subprocess.CompletedProcess:
    """Run the checker over one workflow file against its own allowlist, so the real ones play no part."""
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "wf.yml"
        path.write_text(textwrap.dedent(workflow))
        allow = Path(tmp) / "allow.yml"
        allow.write_text(textwrap.dedent(allowlist).replace("WF", str(path)))
        return subprocess.run([sys.executable, str(CHECK), "--allowlist", str(allow), str(path)],
                              capture_output=True, text=True)


def job(steps: str) -> str:
    return "jobs:\n  j:\n    steps:\n" + textwrap.indent(textwrap.dedent(steps), "      ")


class TheDefectItPins(unittest.TestCase):

    def test_a_renamed_env_key_is_reported_in_both_directions(self):
        result = run_check(job("""\
            - name: Announce
              env:
                WEBHOOK_URL: x
              run: curl "${NEWS_WEBHOOK_URL:-}"
            """))
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("UNUSED: ", result.stdout)
        self.assertIn("::Announce::WEBHOOK_URL", result.stdout)
        self.assertIn("UNDEFINED: ", result.stdout)
        self.assertIn("::Announce::NEWS_WEBHOOK_URL", result.stdout)


class TheLearnedExclusions(unittest.TestCase):

    def assertClean(self, workflow: str):
        result = run_check(workflow)
        self.assertEqual(0, result.returncode, result.stdout)

    def test_a_quoted_heredoc_is_not_shell_but_its_env_lookups_are_references(self):
        self.assertClean(job("""\
            - name: Py
              env:
                TOKEN: x
              run: |
                python3 - <<'PY'
                import os
                print("$not_a_shell_name", os.environ["TOKEN"])
                PY
            """))

    def test_mid_line_and_branch_assignments_define_their_names(self):
        self.assertClean(job("""\
            - name: Assign
              run: |
                U="x"; R="y"
                if true; then VERB="a"; else VERB="b"; fi
                echo "$U $R $VERB"
            """))

    def test_single_quoted_text_and_workflow_expressions_are_not_references(self):
        self.assertClean(job("""\
            - name: Quote
              run: |
                awk '{ print $field }' file
                echo "${{ github.sha }}"
            """))

    def test_powershell_is_scanned_for_env_only(self):
        self.assertClean(job("""\
            - name: Pwsh
              shell: pwsh
              env:
                NAME: x
              run: |
                $local = $env:NAME
                Write-Output $local
            """))

    def test_an_undeclared_powershell_env_read_is_still_reported(self):
        result = run_check(job("""\
            - name: Pwsh
              shell: pwsh
              run: Write-Output $env:MISSING
            """))
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("::Pwsh::MISSING", result.stdout)


class TheAllowlist(unittest.TestCase):

    UNUSED_KEY = job("""\
        - name: Gradle
          env:
            JAVA_HOME: x
          run: ./gradlew build
        """)

    def test_an_allowlisted_finding_passes(self):
        result = run_check(self.UNUSED_KEY, '"WF::j::Gradle::JAVA_HOME": "read by Gradle"\n')
        self.assertEqual(0, result.returncode, result.stdout)
        self.assertIn("1 allowlist entries matched", result.stdout)

    def test_an_entry_matching_no_finding_fails_as_stale(self):
        result = run_check(self.UNUSED_KEY, """\
            "WF::j::Gradle::JAVA_HOME": "read by Gradle"
            "WF::j::Deleted step::TOKEN": "nothing has this key any more"
            """)
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("STALE ALLOWLIST ENTRY", result.stdout)
        self.assertIn("Deleted step::TOKEN", result.stdout)

    def test_an_entry_for_a_file_not_scanned_is_not_judged(self):
        result = run_check(self.UNUSED_KEY, """\
            "WF::j::Gradle::JAVA_HOME": "read by Gradle"
            ".forgejo/workflows/other.yml::j::Step::TOKEN": "belongs to a file this run did not read"
            """)
        self.assertEqual(0, result.returncode, result.stdout)


if __name__ == "__main__":
    unittest.main()
