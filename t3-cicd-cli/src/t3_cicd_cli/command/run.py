"""
Defines the `run` command for executing CI/CD pipelines.
"""

import click
import os
import requests
from t3_cicd_cli.command.config import configuration
from t3_cicd_cli.utils.api import assemble_request
from t3_cicd_cli.utils.git_operations import (
    check_file_exists,
    is_github_repo,
    is_git_repo,
    is_repo_dirty,
    push,
)
from t3_cicd_cli.constant.default import DEFAULT_CONFIG_PATH, DEFAULT_GITHUB_URL
from t3_cicd_cli.constant.api import RUN_URI


@click.command()
@click.option(
    "--commit",
    type=str,
    required=False,
    help="Optional commit hash of the remote repo to use to run the pipeline.",
)
@click.option(
    "--dry-run",
    is_flag=True,
    help="Run the pipeline without executing any jobs (simulation).",
)
@click.option(
    "--override",
    type=str,
    help="Override configuration values. Format: key=value.",
)
@click.option(
    "--file",
    type=str,
    help="Relative path to the configuration file from the project root for this pipeline run."
    + f"if not provided, the path will be defaulted to {DEFAULT_CONFIG_PATH}",
)
@click.option(
    "--pipeline",
    type=str,
    help="Name of the pipeline to run from the configuration.",
)
def run(commit, dry_run, override, file, pipeline):
    """
    Executes the CI/CD pipeline with various options.

    This method provides flexibility in running a pipeline by allowing users to:
    - Specify a commit hash for the pipeline.
    - Perform a dry run (simulate the execution without running any jobs).
    - Override default configuration values.
    - Specify a configuration file or pipeline name for execution.

    Args:
        commit (str): Optional commit hash for the pipeline run.
        dry_run (bool): If True, performs a simulation of the pipeline run without executing jobs.
        override (str): Configuration values to override in key=value format, separated by commas.
        file (str): Path to the configuration file from the project root.
        pipeline (str): Name of the pipeline to run.

    Returns:
        None
    """
    if file and pipeline:
        click.echo("Error: Specify either --file or --pipeline, but not both.")
        return
    if not dry_run:
        config_path = DEFAULT_CONFIG_PATH
        if not configuration.repo:
            click.echo(
                "Error: The path/URL of the repo cannot be null. Please configure it with cicd config --set."
            )
            return
        if configuration.is_repo_remote:  # use user's Git repo
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
        else:  # upload to our Git cicd-localrepo, user needs to commit any change first
            if file:
                if not os.path.exists(configuration.repo + "/" + file):
                    click.echo(
                        f"Error: The file '{file}' does not exist in the project root {configuration.repo}. Please check again."
                    )
                    return
                config_path = file
            if not os.path.exists(configuration.repo):
                click.echo(
                    f"Error: The path of the repo {configuration.repo} does not exist in the local file system. Please check again."
                )
                return
            elif is_git_repo(configuration.repo):
                if is_repo_dirty(configuration.repo):
                    return
            repo_url = DEFAULT_GITHUB_URL
            branch = push(configuration.repo)
        if pipeline:
            config_path = None
        else:
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
        if override:
            overrides = dict(item.split("=") for item in override.split(","))
        else:
            overrides = {}
        endpoint = f"{configuration.server}{RUN_URI}"
        param = assemble_request(
            repo_url=repo_url,
            branch=branch,
            commit=commit,
            override=overrides,
            config_path=config_path,
            pipeline_name=pipeline,
        )

        click.echo("Executing the pipeline...")
        try:
            response = requests.post(endpoint, json=param)
            if response.status_code == 200:
                click.echo("The Pipeline is successfully started.")
            else:
                try:
                    data = response.json()
                    click.echo(f"{response.status_code} {data['message']}")
                except ValueError:
                    click.echo(f"{response.status_code} {response.text}")

        except requests.exceptions.RequestException as e:
            click.echo(f"Error: An error occurred during the request - {str(e)}")
        except Exception as e:
            click.echo(f"Error: Unexpected error occurred - {str(e)}")

    else:
        click.echo("Performing a dry run of the pipeline...")
        click.echo("Dry run complete. No jobs were executed.")
