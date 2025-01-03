"""
cicd: A CLI tool for managing CI/CD pipelines.

This program serves as the main entry point for the cicd CLI application,
providing various commands for managing configurations, jobs, and pipelines.
Commands are dynamically loaded from the `command` submodule.

Usage:
    Run this CLI tool to interact with your CI/CD pipelines:
        $ cicd --help

Powered by the Click library.
"""

import click
import importlib
import pkgutil
from t3_cicd_cli import command


@click.group()
def cli():
    """Main entry point for the cicd CLI."""
    pass


# Dynamically import all modules from the command folder
for module_info in pkgutil.iter_modules(command.__path__):
    module = importlib.import_module(f"t3_cicd_cli.command.{module_info.name}")

    for attr_name in dir(module):
        attr = getattr(module, attr_name)
        if isinstance(attr, click.Command):
            cli.add_command(attr)

if __name__ == "__main__":
    cli()
