from click.testing import CliRunner
from t3_cicd_cli.cli import cli


class TestInfoCommands:

    def test_info_stage(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "info",
                "--stage",
                "s1",
            ],
        )
        assert result.exit_code == 0
        assert "Environment info of stage s1." in result.output

    def test_info_job(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "info",
                "--job",
                "j1",
            ],
        )
        assert result.exit_code == 0
        assert "Environment info of job j1." in result.output

    def test_info_stage_and_job(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            ["info", "--stage", "s1", "--job", "j1"],
        )
        assert result.exit_code == 2
        assert ("Specify either --job " + "or --stage, but not both.") in result.output

    def test_info_nothing(self):
        runner = CliRunner()
        result = runner.invoke(
            cli,
            [
                "info",
            ],
        )
        assert result.exit_code == 2
        assert ("Either --job or --stage is required.") in result.output
