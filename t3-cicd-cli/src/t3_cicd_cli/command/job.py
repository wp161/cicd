"""
Defines `job` commands for managing and executing stages and jobs in a CI/CD pipeline.
"""

import click


@click.command(name="rerun")
@click.option(
    "--job",
    type=str,
    required=True,
    help="The name of the job to rerun",
)
@click.option(
    "--override",
    multiple=True,
    default=None,
    help="Override configuration values. Format: key=value",
)
def rerun(job: str, override: str):
    """
    Rerun a specific job in the pipeline with optional configuration overrides.

    Args:
        job (str): The name of the job to rerun.
        override (str): Optional key-value pairs to override configuration values.

    Example:
        rerun --job="build" --override="key1=value1" --override="key2=value2"
    """
    if override:
        overrides = dict(item.split("=") for item in override)
        click.echo(
            "Temporarily overriding the following config values: " + f"{overrides}"
        )
    click.echo(f"Rerunning job {job}")


@click.command(name="stop")
@click.option(
    "--stage",
    type=str,
    help="Specify the stage you want to stop.",
)
@click.option(
    "--job",
    type=str,
    help="Specific the name of the job you want to stop.",
)
@click.pass_context
def stop(context, stage: str, job: str):
    """
    Stop a running stage or job in the pipeline.

    Args:
        stage (str): The name of the stage to stop.
        job (str): The name of the job to stop.

    Rules:
        - Specify either `--stage` or `--job`, but not both.
        - At least one of `--stage` or `--job` is required.

    Example:
        stop --stage="build"
        stop --job="test-job"

    Raises:
        click.BadParameter: If neither or both `--stage` and `--job` are provided.
    """
    if stage and job:
        raise click.BadParameter("Specify either --job " + "or --stage, but not both.")
    if not stage and not job:
        raise click.BadParameter("Either --job or --stage is required.")

    if stage:
        click.echo(f"Stage {stage} has stopped.")
    elif job:
        click.echo(f"Job {job} has stopped.")
