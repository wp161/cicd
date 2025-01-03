from click.testing import CliRunner
from unittest.mock import call, patch, MagicMock
from t3_cicd_cli.command.validate import validate
from t3_cicd_cli.constant.default import DEFAULT_CONFIG_PATH


class TestValidateCommand:
    @patch("t3_cicd_cli.command.validate.configuration")
    @patch("t3_cicd_cli.command.validate.check_file_exists")
    @patch("t3_cicd_cli.command.validate.is_github_repo", return_value=True)
    @patch("t3_cicd_cli.command.validate.os.path.exists", return_value=True)
    @patch("t3_cicd_cli.command.validate.requests.post")
    @patch("t3_cicd_cli.command.validate.check_file_exists", return_value=True)
    def test_validate_good_config(
        self,
        mock_file_exists,
        mock_post,
        mock_exists,
        mock_is_github_repo,
        mock_check_file_exists,
        mock_config,
    ):
        mock_config.is_repo_remote = True
        mock_config.repo = "https://github.com/example.git"
        mock_config.branch = "main"
        mock_config.server = "http://localhost"
        mock_check_file_exists.return_value = True
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = "Success"
        mock_post.return_value = mock_response
        runner = CliRunner()
        result = runner.invoke(
            validate,
            ["--file", "good_config.yaml"],
        )
        assert result.exit_code == 0
        assert "The Config File is successfully validated." in result.output
        mock_post.assert_called_once_with(
            "http://localhost/validate",
            json={
                "repo_url": "https://github.com/example.git",
                "branch": "main",
                "config_path": "good_config.yaml",
            },
        )

    @patch("os.path.exists", return_value=True)
    @patch("t3_cicd_cli.command.validate.push", return_value="mock_branch")
    @patch("t3_cicd_cli.command.validate.configuration")
    @patch("t3_cicd_cli.command.validate.requests.post")
    def test_validate_bad_config(self, mock_post, mock_config, mock_push, mock_exists):
        mock_config.is_repo_remote = False
        mock_config.repo = "https://github.com/example.git"
        mock_response = MagicMock()
        mock_response.status_code = 400
        mock_response.text = "Validation error"
        mock_post.return_value = mock_response
        runner = CliRunner()
        result = runner.invoke(
            validate,
            ["--file", "tests/t3_cicd_cli/test_files/cicd-localrepo/bad_config.yaml"],
        )
        assert result.exit_code == 0
        assert "Validation failed" in result.output
        mock_push.assert_not_called

    @patch("t3_cicd_cli.command.validate.configuration")
    @patch("t3_cicd_cli.command.validate.requests.post")
    @patch("t3_cicd_cli.command.validate.is_github_repo", return_value=True)
    @patch("t3_cicd_cli.command.validate.os.path.exists", return_value=True)
    @patch("t3_cicd_cli.command.validate.check_file_exists", return_value=True)
    def test_validate_default_config_path(
        self, mock_file_exists, mock_exists, mock_is_github_repo, mock_post, mock_config
    ):
        mock_config.is_repo_remote = True
        mock_config.repo = "https://github.com/example.git"
        mock_config.branch = "main"
        mock_config.server = "http://localhost"
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = "Success"
        mock_post.return_value = mock_response
        runner = CliRunner()
        result = runner.invoke(validate)
        assert result.exit_code == 0
        assert "The Config File is successfully validated." in result.output
        mock_post.assert_called_once_with(
            "http://localhost/validate",
            json={
                "repo_url": "https://github.com/example.git",
                "branch": "main",
                "config_path": DEFAULT_CONFIG_PATH,
            },
        )

    @patch("t3_cicd_cli.command.validate.configuration")
    @patch("t3_cicd_cli.command.validate.os.path.exists")
    @patch("t3_cicd_cli.command.validate.requests.post")
    @patch("t3_cicd_cli.command.validate.push")
    def test_validate_good_file_local_repo(
        self,
        mock_push,
        mock_post,
        mock_exists,
        mock_config,
    ):
        """Test the 'validate' command when --file is specified on a local repo."""
        mock_config.is_repo_remote = False
        mock_config.repo = "/path/to/local/repo"
        mock_config.branch = "main"
        mock_config.server = "http://localhost"
        mock_exists.return_value = True
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = "Success"
        mock_post.return_value = mock_response
        mock_push.return_value = "main"

        runner = CliRunner()
        result = runner.invoke(validate, ["--file", "config.yaml"])

        assert result.exit_code == 0
        assert "The Config File is successfully validated." in result.output
        mock_post.assert_called_once_with(
            "http://localhost/validate",
            json={
                "repo_url": "https://github.com/wp161/cicd-localrepo.git",
                "branch": "main",
                "config_path": "config.yaml",
            },
        )

    @patch("t3_cicd_cli.command.validate.configuration")
    @patch("t3_cicd_cli.command.validate.os.path.exists")
    def test_validate_non_existent_local_repo(self, mock_exists, mock_config):
        """Test the 'validate' command when the local repo path does not exist."""
        mock_config.is_repo_remote = False
        mock_config.repo = "/path/to/nonexistent/repo"
        mock_exists.side_effect = lambda path: path != "/path/to/nonexistent/repo"

        runner = CliRunner()
        result = runner.invoke(validate)

        assert result.exit_code == 0
        assert (
            "Error: The path of the repo does not exist in the local file system. Please check again."
            in result.output
        )
        assert call("/path/to/nonexistent/repo") in mock_exists.mock_calls

    @patch("t3_cicd_cli.command.validate.configuration")
    @patch("t3_cicd_cli.command.validate.requests.post")
    @patch("t3_cicd_cli.command.validate.check_file_exists")
    @patch("t3_cicd_cli.command.validate.is_github_repo", return_value=True)
    def test_validate_non_exist_file_remote_repo(
        self, mock_is_github_repo, mock_check_file_exists, mock_post, mock_config
    ):
        """Test the 'validate' command when --file does not exist in a remote repo."""

        mock_config.is_repo_remote = True
        mock_config.repo = "https://github.com/example.git"
        mock_config.branch = "main"
        mock_check_file_exists.return_value = False

        runner = CliRunner()
        result = runner.invoke(validate, ["--file", "nonexistent_file.yaml"])

        assert result.exit_code == 0
        assert (
            "Cannot find file nonexistent_file.yaml in given repo https://github.com/example.git in main branch."
            in result.output
        )
        mock_post.assert_not_called()
        mock_check_file_exists.assert_called_once_with(
            "https://github.com/example.git", "main", "nonexistent_file.yaml"
        )

    @patch("t3_cicd_cli.command.validate.configuration")
    @patch("t3_cicd_cli.command.validate.requests.post")
    @patch("t3_cicd_cli.command.validate.check_file_exists")
    @patch("t3_cicd_cli.command.validate.is_github_repo", return_value=False)
    def test_validate_non_exist_remote_repo(
        self, mock_is_github_repo, mock_check_file_exists, mock_post, mock_config
    ):
        """Test the 'validate' command when --file does not exist in a remote repo."""

        mock_config.is_repo_remote = True
        mock_config.repo = "https://github.com/example.git"
        mock_config.branch = "main"
        mock_check_file_exists.return_value = False

        runner = CliRunner()
        result = runner.invoke(validate, ["--file", "nonexistent_file.yaml"])

        assert result.exit_code == 0
        assert (
            "Error: Provided repo https://github.com/example.git is not a valid public remote Git repo."
            in result.output
        )
        mock_post.assert_not_called()
