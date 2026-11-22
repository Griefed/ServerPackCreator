"""Self-test for dockerignore-inputs-check.py. Run with `python3 -B -m unittest discover -s misc -p 'test_*.py'`."""

import importlib.util
import tempfile
import textwrap
import unittest
from pathlib import Path

CHECK = Path(__file__).resolve().parent / "dockerignore-inputs-check.py"
spec = importlib.util.spec_from_file_location("dockerignore_inputs_check", CHECK)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

DOCKERIGNORE = textwrap.dedent("""\
    # comment
    **/build
    .idea/
    .github/*
    !.github/scripts/
    docker/docker-compose.yml
    """)


class DockerIgnoreSemantics(unittest.TestCase):

    def setUp(self):
        self.rules = module.parse_dockerignore(DOCKERIGNORE)

    def test_a_double_star_pattern_excludes_at_any_depth(self):
        self.assertTrue(module.is_excluded("buildSrc/build/libs/x.jar", self.rules))
        self.assertTrue(module.is_excluded("build/x", self.rules))

    def test_a_directory_pattern_excludes_its_contents(self):
        self.assertTrue(module.is_excluded(".idea/workspace.xml", self.rules))

    def test_a_later_negation_re_includes(self):
        self.assertFalse(module.is_excluded(".github/scripts/clientside-verification.js", self.rules))
        self.assertTrue(module.is_excluded(".github/workflows/pages.yml", self.rules))

    def test_an_unmatched_path_stays_in_the_context(self):
        self.assertFalse(module.is_excluded(".forgejo/actions/resolve-version/action.yml", self.rules))


class DeclaredInputs(unittest.TestCase):

    def write_repo(self, tmp: Path, dockerignore: str, x_flags: str = ""):
        (tmp / "docker").mkdir()
        (tmp / ".dockerignore").write_text(dockerignore)
        (tmp / "docker" / "Dockerfile").write_text(
            f'RUN sh gradlew build --info {x_flags}\n')
        (tmp / "buildSrc").mkdir()
        (tmp / "buildSrc" / "build.gradle.kts").write_text(textwrap.dedent("""\
            tasks.test {
                inputs.file(file("../.forgejo/actions/resolve-version/action.yml"))
            }
            """))
        (tmp / "serverpackcreator-app").mkdir()
        (tmp / "serverpackcreator-app" / "build.gradle.kts").write_text(textwrap.dedent("""\
            tasks.test {
                inputs.file(rootProject.file("docker/docker-compose.yml"))
            }
            """))

    def test_inputs_are_found_with_the_task_that_declares_them(self):
        with tempfile.TemporaryDirectory() as name:
            tmp = Path(name)
            self.write_repo(tmp, "")
            self.assertEqual(
                [(":buildSrc:test", ".forgejo/actions/resolve-version/action.yml"),
                 (":serverpackcreator-app:test", "docker/docker-compose.yml")],
                sorted(module.declared_inputs(tmp)))

    def test_an_excluded_input_of_a_task_the_image_build_runs_is_a_finding(self):
        with tempfile.TemporaryDirectory() as name:
            tmp = Path(name)
            self.write_repo(tmp, ".forgejo/\n")
            findings, _ = module.check(tmp)
            self.assertEqual([(":buildSrc:test", ".forgejo/actions/resolve-version/action.yml")], findings)

    def test_an_input_of_a_task_the_dockerfile_skips_is_exempt(self):
        with tempfile.TemporaryDirectory() as name:
            tmp = Path(name)
            self.write_repo(tmp, "docker/docker-compose.yml\n", "-x :serverpackcreator-app:test")
            findings, exempt = module.check(tmp)
            self.assertEqual([], findings)
            self.assertEqual([(":serverpackcreator-app:test", "docker/docker-compose.yml")], exempt)


if __name__ == "__main__":
    unittest.main()
