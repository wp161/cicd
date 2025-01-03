"""
Defines the `info` command to display environment information for a specific stage or job.
"""

import click


@click.command(name="info")
@click.option(
    "--stage",
    type=str,
    help="Specify the stage you want to view the env info for.",
)
@click.option(
    "--job",
    type=str,
    help="Specify the job you want to view the env info for.",
)
@click.pass_context
def info(context, stage: str, job: str):
    """
    Display environment information for a specified stage or job.

    This command outputs environment details based on the provided
    `--stage` or `--job` options.

    Usage:
        - Provide `--stage` to display environment information for a specific stage.
        - Provide `--job` to display environment information for a specific job.
        - Do not provide both options simultaneously; only one is allowed.
        - At least one option (`--stage` or `--job`) is required.

    Args:
        context (click.Context): The Click context object.
        stage (str): The stage for which environment information is requested.
        job (str): The job for which environment information is requested.

    Raises:
        click.BadParameter: If both `--stage` and `--job` are provided, or if neither is specified.
    """
    if stage and job:
        raise click.BadParameter("Specify either --job " + "or --stage, but not both.")
    if not stage and not job:
        raise click.BadParameter("Either --job or --stage is required.")

    if stage:
        click.echo(f"Environment info of stage {stage}.")
    if job:
        click.echo(f"Environment info of job {job}.")
