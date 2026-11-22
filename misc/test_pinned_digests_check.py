"""Self-test for pinned-digests-check.py. Run with `python3 -B -m unittest discover -s misc -p 'test_*.py'`."""

import importlib.util
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

CHECK = Path(__file__).resolve().parent / "pinned-digests-check.py"
spec = importlib.util.spec_from_file_location("pinned_digests_check", CHECK)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

A = "a" * 64
B = "b" * 64


class FindingPins(unittest.TestCase):

    def test_from_lines_compose_images_and_a_variable_default_are_all_found(self):
        text = textwrap.dedent(f"""\
            FROM eclipse-temurin:21-jdk-jammy@sha256:{A} AS builder
            FROM ghcr.io/linuxserver/nginx:1.30.6@sha256:{B}
                image: ${{SPC_DEV_MONGO_IMAGE:-mongodb/mongodb-community-server:9.0.8-ubuntu2204@sha256:{A}}}
                image: griefed/serverpackcreator:9.4.1 # unpinned, ignored
            """)
        self.assertEqual([f"eclipse-temurin:21-jdk-jammy@sha256:{A}",
                          f"ghcr.io/linuxserver/nginx:1.30.6@sha256:{B}",
                          f"mongodb/mongodb-community-server:9.0.8-ubuntu2204@sha256:{A}"],
                         module.pinned_references(text))


class AskingByDigest(unittest.TestCase):

    def test_the_tag_is_dropped_so_a_moved_tag_is_not_reported_as_a_missing_digest(self):
        self.assertEqual(f"ghcr.io/linuxserver/nginx@sha256:{A}",
                         module.by_digest(f"ghcr.io/linuxserver/nginx:1.30.6@sha256:{A}"))
        self.assertEqual(f"debian@sha256:{A}", module.by_digest(f"debian:bookworm-slim@sha256:{A}"))


class Verdict(unittest.TestCase):

    def run_check(self, inspector_exit: str) -> subprocess.CompletedProcess:
        with tempfile.TemporaryDirectory() as tmp:
            pinned = Path(tmp) / "Dockerfile"
            pinned.write_text(f"FROM debian:bookworm-slim@sha256:{A}\nFROM node:26@sha256:{B}\n")
            inspector = Path(tmp) / "inspect.sh"
            # Exits with the code given for the reference whose digest starts with b, 0 otherwise.
            inspector.write_text(f'#!/bin/sh\ncase "$1" in *@sha256:b*) exit {inspector_exit};; esac\nexit 0\n')
            inspector.chmod(0o755)
            return subprocess.run([sys.executable, str(CHECK), "--inspector", str(inspector), str(pinned)],
                                  capture_output=True, text=True)

    def test_every_digest_resolving_passes(self):
        result = self.run_check("0")
        self.assertEqual(0, result.returncode, result.stdout)
        self.assertIn("2 pinned digests resolve", result.stdout)

    def test_a_digest_the_registry_no_longer_serves_fails_naming_it(self):
        result = self.run_check("1")
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn(f"MISSING node:26@sha256:{B}", result.stdout)


if __name__ == "__main__":
    unittest.main()
