from click.testing import CliRunner
from t3_cicd_cli.cli import cli


class TestJobCommands:

    def test_rerun(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "rerun",
                "--job",
                "1",
                "--override",
                "key1=value1",
                "--override",
                "key2=value2",
            ],
        )
        assert result.exit_code == 0
        assert (
            "Temporarily overriding the following config values: "
            + "{'key1': 'value1', 'key2': 'value2'}"
        ) in result.output
        assert "Rerunning job 1" in result.output

    def test_rerun_no_override(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "rerun",
                "--job",
                "1",
            ],
        )
        assert result.exit_code == 0
        assert "Rerunning job 1" in result.output

    def test_stop_stage(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "stop",
                "--stage",
                "1",
            ],
        )
        assert result.exit_code == 0
        assert "Stage 1 has stopped" in result.output

    def test_stop_job(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "stop",
                "--job",
                "1",
            ],
        )
        assert result.exit_code == 0
        assert "Job 1 has stopped" in result.output

    def test_stop_stage_and_job(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ["stop", "--stage", "1", "--job", "1"],
        )
        assert result.exit_code == 2
        assert ("Specify either --job " + "or --stage, but not both.") in result.output

    def test_stop_nothing(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "stop",
            ],
        )
        assert result.exit_code == 2
        assert ("Either --job or --stage is required.") in result.output
