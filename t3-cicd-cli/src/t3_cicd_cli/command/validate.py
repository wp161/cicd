"""
Defines the `validate` command to validate configuration files for pipelines.
"""

import os
import click
import requests
from t3_cicd_cli.constant.api import VALIDATE_URI
from t3_cicd_cli.constant.default import DEFAULT_CONFIG_PATH, DEFAULT_GITHUB_URL
from t3_cicd_cli.utils.git_operations import (
    check_file_exists,
    is_github_repo,
    is_git_repo,
    is_repo_dirty,
    push,
)
from t3_cicd_cli.command.config import configuration
from t3_cicd_cli.utils.api import assemble_request


@click.command(name="validate")
@click.option(
    "--file",
    type=str,
    required=False,
    help="Relative path to the configuration file from the project root for this pipeline run."
    + f"if not provided, the path will be defaulted to {DEFAULT_CONFIG_PATH}",
)
def validate(file: str):
    """
    Validates a configuration file for pipeline execution.

    Parameters:
        file (str): Optional. Relative path to the configuration file. If omitted,
                    defaults to the standard path defined in `DEFAULT_CONFIG_PATH`.

    Process:
    - Validates if the file exists locally or remotely.
    - Checks repository state (clean or dirty).
    - Sends a POST request to an API endpoint to validate the configuration content.

    Returns:
    - Prints success or error messages to the console.
    """
    config_path = DEFAULT_CONFIG_PATH

    if configuration.is_repo_remote:
        repo_url = configuration.repo
        if not is_github_repo(repo_url):
            click.echo(
                f"Error: Provided repo {repo_url} is not a valid public remote Git repo."
            )
            return
        branch = configuration.branch
        if file:
            is_file_exist = check_file_exists(repo_url, branch, file)
            if not is_file_exist:
                click.echo(
                    f"Error: Cannot find file {file} in given repo {repo_url} in {branch} branch."
                )
                return
            config_path = file
    else:  # upload to our Git cicd-localrepo
        if file:
            if not os.path.exists(configuration.repo + "/" + file):
                click.echo(
                    f"Error: The file '{file}' does not exist in the project root {configuration.repo}. Please check again."
                )
                return
            config_path = file
        if not os.path.exists(configuration.repo):
            click.echo(
                "Error: The path of the repo does not exist in the local file system. Please check again."
            )
            return
        elif is_git_repo(configuration.repo):
            if is_repo_dirty(configuration.repo):
                return
        repo_url = DEFAULT_GITHUB_URL
        branch = push(configuration.repo)

    if configuration.is_repo_remote:
        is_file_exist = check_file_exists(repo_url, branch, config_path)
        if not is_file_exist:
            click.echo(
                f"Error: Cannot find the default config {file} in in given repo {repo_url} in {branch} branch."
            )
            return
    else:
        if not os.path.exists(configuration.repo + "/" + config_path):
            click.echo(
                f"Error: The default config '{config_path}' does not exist in the project root {configuration.repo}. Please check again."
            )
            return

    endpoint = f"{configuration.server}{VALIDATE_URI}"
    param = assemble_request(repo_url=repo_url, branch=branch, config_path=config_path)

    try:
        response = requests.post(endpoint, json=param)
        if response.status_code == 200:
            click.echo("The Config File is successfully validated.")
        else:
            data = response.json()
            click.echo(
                f"{response.status_code} {data['message']} " + "Validation failed."
            )
    except requests.exceptions.RequestException as e:
        click.echo(f"Error: An error occurred during the request - {e}")
