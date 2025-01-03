from click.testing import CliRunner
from t3_cicd_cli.cli import cli


class TestLogCommands:

    def test_log_no_options(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "log",
            ],
        )
        assert result.exit_code == 0
        assert "Shows all logs for every pipeline" in result.output

    def test_log_pipeline_(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "log",
                "--pipeline",
                "p1",
            ],
        )
        assert result.exit_code == 0
        assert (
            "Shows all logs for the jobs and stages in " + "pipeline p1."
        ) in result.output

    def test_log_pipeline_stage(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "log",
                "--pipeline",
                "p1",
                "--stage",
                "s1",
            ],
        )
        assert result.exit_code == 0
        assert (
            "Shows all logs for the jobs in " + "pipeline p1, stage s1."
        ) in result.output

    def test_log_pipeline_stage_job(self):
        runner = CliRunner()
        result = runner.invoke(
            cli, ["log", "--pipeline", "p1", "--stage", "s1", "--job", "j1"]
        )
        assert result.exit_code == 0
        assert ("Shows logs for job j1 in " + "pipeline p1, stage s1.") in result.output
