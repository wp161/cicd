"""
Defines the `log` command for querying logs in pipelines, stages, or jobs.
"""

import click


@click.command(name="log")
@click.option(
    "--pipeline",
    type=str,
    help="The pipeline name to query the logs for.",
)
@click.option(
    "--stage",
    type=str,
    help="The stage name to query the logs for.",
)
@click.option("--job", type=str, help="The job name to query the logs for.")
def log(pipeline: str, stage: str, job: str):
    """
    Query logs for completed pipelines, stages, or jobs.

    Options:
        --pipeline: Displays logs for the specified pipeline.
        --stage: Displays logs for the specified stage within a pipeline.
        --job: Displays logs for the specified job within a stage.

    Behavior:
        - If all options (--pipeline, --stage, --job) are provided:
          Displays logs for the specific job in the specified pipeline and stage.
        - If --pipeline and --stage are provided:
          Displays all logs for jobs in the specified stage of the pipeline.
        - If only --pipeline is provided:
          Displays all logs for stages and jobs in the pipeline.
        - If no options are provided:
          Displays logs for all pipelines.
    """
    if pipeline and stage and job:
        click.echo(
            f"Shows logs for job {job} in " + f"pipeline {pipeline}, stage {stage}."
        )
    if pipeline and stage:
        click.echo(
            "Shows all logs for the jobs in " + f"pipeline {pipeline}, stage {stage}."
        )
    if pipeline:
        click.echo(
            "Shows all logs for the jobs and stages in " + f"pipeline {pipeline}."
        )
    if not pipeline and not stage and not job:
        click.echo("Shows all logs for every pipeline")
