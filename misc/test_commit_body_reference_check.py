"""Self-test for commit-body-reference-check.py. Run with `python3 -B -m unittest discover -s misc -p 'test_*.py'`.

Every expectation about which tokens the parser extracts was confirmed against conventional-commits-parser
6.4.0 with conventional-changelog-conventionalcommits@9's parser options, the pair release-generate.yml runs.
"""

import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path

CHECK = Path(__file__).resolve().parent / "commit-body-reference-check.py"
spec = importlib.util.spec_from_file_location("commit_body_reference_check", CHECK)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def message(*body_lines: str, subject: str = "fix: a subject") -> str:
    return subject + "\n\n" + "\n".join(body_lines) + "\n"


class MislinkedReferences(unittest.TestCase):

    def test_a_bare_hex_hash_is_flagged(self):
        self.assertEqual(["#54a896"], module.mislinked(message("the #54a896 heading")))

    def test_a_backticked_hex_hash_is_not_flagged(self):
        self.assertEqual([], module.mislinked(message("the `#54a896` heading")))

    def test_backticks_protect_only_when_the_closing_tick_touches_the_token(self):
        self.assertEqual(["#fff"], module.mislinked(message("the `color #fff here` rule")))

    def test_an_owner_repo_reference_is_flagged(self):
        self.assertEqual(["gitlab-org/gitlab#327317"],
                         module.mislinked(message("see gitlab-org/gitlab#327317 for the issue")))

    def test_a_backticked_owner_repo_reference_is_not_flagged(self):
        self.assertEqual([], module.mislinked(message("see `gitlab-org/gitlab#327317` for the issue")))

    def test_a_repository_qualified_number_is_flagged(self):
        self.assertEqual(["forgejo#6657", "717/#718"],
                         module.mislinked(message("upstream forgejo#6657 is fixed", "before #717/#718 landed")))

    def test_a_class_member_reference_is_flagged(self):
        self.assertEqual(["ConfigUtilities#writeConfigToFile"],
                         module.mislinked(message("moved ConfigUtilities#writeConfigToFile here")))

    def test_a_numeric_issue_reference_is_allowed(self):
        self.assertEqual([], module.mislinked(message("Closes #738 #737", "see #123")))

    def test_fenced_code_is_flagged_because_the_parser_does_not_read_markdown(self):
        self.assertEqual(["#54a896"],
                         module.mislinked(message("output:", "```", "color: #54a896", "```")))

    def test_a_url_is_not_flagged(self):
        self.assertEqual([], module.mislinked(message(
            "Closes https://git.griefed.de/Griefed/ServerPackCreator/pulls/123#issuecomment-5")))

    def test_a_url_hides_every_reference_on_its_line_as_in_the_parser(self):
        self.assertEqual([], module.mislinked(message("see https://example.com/a and #54a896 too")))

    def test_text_before_a_closing_keyword_is_not_parsed(self):
        self.assertEqual([], module.mislinked(message("#54a896 fixed it")))

    def test_a_breaking_change_line_itself_is_not_parsed_but_the_next_line_is(self):
        self.assertEqual(["#def2"], module.mislinked(message("BREAKING CHANGE: see #abc1 now", "#def2 next")))

    def test_a_reference_in_the_header_is_flagged(self):
        self.assertEqual(["#abc1"], module.mislinked("fix: colour #abc1 here\n"))

    def test_the_subject_is_linked_even_inside_backticks(self):
        self.assertEqual(["#abc1"], module.mislinked("fix: colour `#abc1` here\n"))

    def test_a_numeric_subject_reference_is_allowed(self):
        self.assertEqual([], module.mislinked("deps: update node.js (#743)\n"))

    def test_every_offender_is_reported_in_order(self):
        # Lines 4, 19 and 21 of db025e531's body, verbatim; the parser extracted these seven.
        self.assertEqual(
            ["#54a896", "#ec982f", "#f4655f", "#9c27b0", "#3b97d1", "#54a896", "#f7f7f7"],
            module.mislinked(message(
                "#54a896 on the #ec982f, #f4655f, #9c27b0 and #3b97d1 bands is poor contrast -- a readability",
                "    -- restoring :style=\"headingTint(3)\": expected 'color: #54a896;' not to contain 'color'",
                "    -- pinning headingTint to the resting colour: expected 'color: #f7f7f7;' to contain '#54a896'",
                subject="fix(homepage): the Features Excerpt headings keep their white on hover")))


class PushRevisions(unittest.TestCase):

    def test_a_push_to_an_existing_branch_checks_what_it_added(self):
        self.assertEqual(["aaa..bbb"], module.push_revisions("aaa", "bbb", "develop", lambda rev: True))

    def test_a_new_branch_checks_only_commits_no_other_branch_has(self):
        self.assertEqual(["bbb", "--not", "--exclude=claude-x", "--branches"],
                         module.push_revisions("0" * 40, "bbb", "claude-x", lambda rev: True))

    def test_a_force_push_whose_old_tip_is_gone_falls_back_to_the_same(self):
        self.assertEqual(["bbb", "--not", "--exclude=develop", "--branches"],
                         module.push_revisions("aaa", "bbb", "develop", lambda rev: False))

    def test_no_before_means_no_push_range(self):
        self.assertIsNone(module.push_revisions("", "bbb", "develop", lambda rev: True))


class CommandLine(unittest.TestCase):

    def git(self, repo: Path, *args: str) -> str:
        return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True,
                              text=True).stdout.strip()

    def commit(self, repo: Path, text: str) -> str:
        self.git(repo, "commit", "-q", "--allow-empty", "-m", text)
        return self.git(repo, "rev-parse", "HEAD")

    def make_repo(self, tmp: Path) -> Path:
        repo = tmp / "repo"
        repo.mkdir()
        self.git(repo, "init", "-q", "-b", "main")
        self.git(repo, "config", "user.email", "test@example.invalid")
        self.git(repo, "config", "user.name", "test")
        self.git(repo, "config", "commit.gpgsign", "false")
        return repo

    def run_check(self, *args: str):
        return subprocess.run(["python3", "-B", str(CHECK), *args], capture_output=True, text=True)

    def test_an_offending_commit_is_printed_and_fails(self):
        with tempfile.TemporaryDirectory() as name:
            repo = self.make_repo(Path(name))
            base = self.commit(repo, "chore: base")
            self.commit(repo, "fix: clean\n\nCloses #12")
            bad = self.commit(repo, "fix: the colour\n\nthe #54a896 band")
            result = self.run_check("--git-dir", str(repo / ".git"), f"{base}..HEAD")
            self.assertEqual(1, result.returncode, result.stdout + result.stderr)
            self.assertIn(self.git(repo, "rev-parse", "--short", bad), result.stdout)
            self.assertIn("fix: the colour", result.stdout)
            self.assertIn("#54a896", result.stdout)
            self.assertNotIn("#12", result.stdout)

    def test_a_clean_range_passes(self):
        with tempfile.TemporaryDirectory() as name:
            repo = self.make_repo(Path(name))
            base = self.commit(repo, "chore: base")
            self.commit(repo, "fix: clean\n\nthe `#54a896` band, Closes #12")
            result = self.run_check("--git-dir", str(repo / ".git"), f"{base}..HEAD")
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            self.assertIn("clean", result.stdout)

    def test_an_unresolvable_range_fails_and_says_so(self):
        with tempfile.TemporaryDirectory() as name:
            repo = self.make_repo(Path(name))
            self.commit(repo, "chore: base")
            result = self.run_check("--git-dir", str(repo / ".git"), "origin/main..HEAD")
            self.assertEqual(1, result.returncode)
            self.assertIn("cannot resolve", result.stdout)

    def test_a_shallow_repository_is_refused(self):
        with tempfile.TemporaryDirectory() as name:
            tmp = Path(name)
            repo = self.make_repo(tmp)
            self.commit(repo, "chore: one")
            self.commit(repo, "chore: two")
            shallow = tmp / "shallow"
            subprocess.run(["git", "clone", "-q", "--depth", "1", f"file://{repo}", str(shallow)], check=True)
            result = self.run_check("--git-dir", str(shallow / ".git"), "HEAD~1..HEAD")
            self.assertEqual(1, result.returncode)
            self.assertIn("shallow", result.stdout)

    def test_a_push_to_a_new_branch_skips_commits_another_branch_has(self):
        with tempfile.TemporaryDirectory() as name:
            repo = self.make_repo(Path(name))
            self.commit(repo, "fix: old\n\nthe #54a896 band")
            self.git(repo, "checkout", "-q", "-b", "feature")
            new = self.commit(repo, "fix: new\n\nthe #ec982f band")
            result = self.run_check("--git-dir", str(repo / ".git"), "--push", "0" * 40, new, "feature")
            self.assertEqual(1, result.returncode, result.stdout + result.stderr)
            self.assertIn("#ec982f", result.stdout)
            self.assertNotIn("#54a896", result.stdout)

    def test_a_run_that_is_not_a_push_is_skipped_loudly(self):
        with tempfile.TemporaryDirectory() as name:
            repo = self.make_repo(Path(name))
            head = self.commit(repo, "fix: old\n\nthe #54a896 band")
            result = self.run_check("--git-dir", str(repo / ".git"), "--push", "", head, "main")
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            self.assertIn("skipped", result.stdout)


if __name__ == "__main__":
    unittest.main()
