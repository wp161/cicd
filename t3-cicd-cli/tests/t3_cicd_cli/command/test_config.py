from click.testing import CliRunner
from t3_cicd_cli.command.config import config, ConfigurationCommands
from unittest.mock import patch, mock_open

mock_config_data = """
{
  "is-repo-remote": false,
  "is-run-remote": false,
  "repo": null,
  "branch": "main",
  "server": null,
  "format": "plain"
}
"""

MOCK_CONFIG_FILE_PATH = "/mock/path/config.json"


class TestConfigurationCommands:
    @patch(
        "t3_cicd_cli.constant.default.DEFAULT_CLI_CONFIG_PATH", "/mock/path/config.json"
    )
    @patch("builtins.open", new_callable=mock_open, read_data=mock_config_data)
    def test_show_default(self, mock_file):
        runner = CliRunner()
        result = runner.invoke(config, ["reset"])
        result = runner.invoke(config, ["show"])
        assert result.exit_code == 0
        assert "Displaying current configuration" in result.output
        assert "is-repo-remote: False" in result.output
        assert "is-run-remote: False" in result.output
        assert "repo: None" in result.output
        assert "branch: main" in result.output
        assert "server: None" in result.output
        assert "format: plain" in result.output

    @patch(
        "t3_cicd_cli.constant.default.DEFAULT_CLI_CONFIG_PATH", "/mock/path/config.json"
    )
    @patch("builtins.open", new_callable=mock_open, read_data=mock_config_data)
    def test_set_all_options(self, mock_file):
        runner = CliRunner()
        result = runner.invoke(config, ["reset"])
        result = runner.invoke(
            config,
            [
                "set",
                "--is-repo-remote",
                "True",
                "--is-run-remote",
                "True",
                "--repo",
                "https://example.com/repo.git",
                "--branch",
                "example-branch",
                "--server",
                "https://example.com/server",
                "--format",
                "json",
            ],
        )
        assert result.exit_code == 0
        assert "Settings updated." in result.output
        assert "is-repo-remote: True" in result.output
        assert "is-run-remote: True" in result.output
        assert "repo: https://example.com/repo.git" in result.output
        assert "branch: example-branch" in result.output
        assert "server: https://example.com/server" in result.output
        assert "format: json" in result.output

    @patch(
        "t3_cicd_cli.constant.default.DEFAULT_CLI_CONFIG_PATH", "/mock/path/config.json"
    )
    @patch("builtins.open", new_callable=mock_open, read_data=mock_config_data)
    def test_set_null_values(self, mock_file):
        """Test setting values to null and coverage for line 89"""
        runner = CliRunner()
        result = runner.invoke(config, ["reset"])

        result = runner.invoke(
            config,
            [
                "set",
                "--branch",
                "none",
                "--server",
                "none",
            ],
        )
        assert result.exit_code == 0
        assert "Settings updated." in result.output
        assert "branch: main" in result.output
        assert "server: None" in result.output

    @patch(
        "t3_cicd_cli.constant.default.DEFAULT_CLI_CONFIG_PATH", MOCK_CONFIG_FILE_PATH
    )
    @patch(
        "builtins.open", new_callable=mock_open, read_data='{"is-repo-remote": false}'
    )
    def test_invalid_format(self, mock_file):
        """Test that an invalid format results in an error message"""
        configuration = ConfigurationCommands()
        runner = CliRunner()
        result = runner.invoke(config, ["set", "--format", "invalidFormat"])
        assert result.exit_code == 0
        assert "Format has to be plain/ json/ yaml." in result.output
        assert configuration.format == "plain"
