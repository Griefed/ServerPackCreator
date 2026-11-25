"""Pins that the dev stack publishes nothing beyond the host's loopback: its credentials are committed in the
file, and Docker's published ports bypass host firewalls."""

import sys
import unittest
from pathlib import Path

import yaml

DEV_COMPOSE = Path(__file__).resolve().parent.parent / "docker" / "docker-compose-dev.yml"


def exposed_ports(compose_text: str) -> list:
    """Every `ports:` entry, of any service, that is not bound to 127.0.0.1."""
    services = (yaml.safe_load(compose_text) or {}).get("services") or {}
    return [f"{name}: {entry}"
            for name, service in services.items()
            for entry in (service or {}).get("ports") or []
            if not str(entry).startswith("127.0.0.1:")]


class DevStackPorts(unittest.TestCase):

    def test_every_published_port_is_bound_to_loopback(self):
        self.assertEqual([], exposed_ports(DEV_COMPOSE.read_text()))


if __name__ == "__main__":
    # `python3 misc/test_dev_compose_ports.py <file>` lists what a given compose file exposes.
    print(exposed_ports(Path(sys.argv[1]).read_text()))
