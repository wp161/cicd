import unittest
from unittest.mock import patch, MagicMock
import git
from t3_cicd_cli.constant.default import DEFAULT_GITHUB_URL, DEFAULT_GITHUB_REMOTE_NAME
from t3_cicd_cli.utils.git_operations import (
    check_file_exists,
    is_github_repo,
    is_git_repo,
    is_repo_dirty,
    setup_repo,
    setup_remote,
    set_up_branch,
    push,
)


class TestGitMethods(unittest.TestCase):

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_is_git_repo_true(self, mock_repo):
        """Test if is_git_repo returns True for a valid Git repo"""
        mock_repo.side_effect = MagicMock()
        result = is_git_repo("/path/to/repo")
        self.assertTrue(result)

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_is_git_repo_false(self, mock_repo):
        """Test if is_git_repo returns False for an invalid Git repo"""
        mock_repo.side_effect = git.exc.InvalidGitRepositoryError
        result = is_git_repo("/invalid/path")
        self.assertFalse(result)

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_is_repo_dirty_clean_repo(self, mock_repo):
        """Test if is_repo_dirty returns True for a clean repo"""
        mock_repo.return_value.is_dirty.return_value = False
        result = is_repo_dirty("/path/to/repo")
        self.assertFalse(result)

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_is_repo_dirty_dirty_repo(self, mock_repo):
        """Test if is_repo_dirty returns False for a dirty repo and prints changes"""
        mock_repo.return_value.is_dirty.return_value = True
        mock_repo.return_value.index.diff.return_value = [MagicMock(a_path="file.txt")]
        mock_repo.return_value.untracked_files = ["untracked_file.txt"]

        with patch("builtins.print") as mocked_print:
            result = is_repo_dirty("/path/to/repo")
            self.assertTrue(result)
            mocked_print.assert_any_call("      Modified:     file.txt")
            mocked_print.assert_any_call("      Untracked:    untracked_file.txt")

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_setup_repo_new(self, mock_repo):
        """Test setup_repo initializes a new Git repo if it doesn't exist"""
        with patch("t3_cicd_cli.utils.git_operations.is_git_repo", return_value=False):
            setup_repo("/path/to/new/repo")
            mock_repo.init.assert_called_once_with("/path/to/new/repo")

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_setup_repo_existing(self, mock_repo):
        """Test setup_repo returns an existing repo"""
        with patch("t3_cicd_cli.utils.git_operations.is_git_repo", return_value=True):
            result = setup_repo("/path/to/existing/repo")
            mock_repo.assert_called_once_with("/path/to/existing/repo")
            self.assertEqual(result, mock_repo("/path/to/existing/repo"))

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_setup_remote_add_new(self, mock_repo):
        """Test setup_repo_remote adds a new remote if it doesn't exist"""
        mock_repo.return_value.remotes = []
        repo = mock_repo("/path/to/repo")
        remote = setup_remote(repo, DEFAULT_GITHUB_REMOTE_NAME, DEFAULT_GITHUB_URL)
        repo.create_remote.assert_called_once_with(
            DEFAULT_GITHUB_REMOTE_NAME, DEFAULT_GITHUB_URL
        )
        self.assertEqual(remote, repo.create_remote.return_value)

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_setup_remote_existing(self, mock_repo):
        """Test setup_repo_remote returns existing remote if it already exists"""
        mock_remote = MagicMock()
        mock_remote.name = "origin"
        mock_repo.return_value.remotes = [mock_remote]
        repo = mock_repo("/path/to/repo")
        remote = setup_remote(repo, "origin", "https://example.com")
        self.assertEqual(remote, mock_remote)
        repo.create_remote.assert_not_called()

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_set_up_branch_create_new(self, mock_repo):
        """Test set_up_branch creates a new branch if it doesn't exist"""
        mock_repo.return_value.branches = []
        repo = mock_repo("/path/to/repo")
        branch = set_up_branch(repo, "new_branch")
        repo.create_head.assert_called_once_with("new_branch")
        self.assertEqual(branch, repo.create_head.return_value)

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    def test_set_up_branch_existing(self, mock_repo):
        """Test set_up_branch returns existing branch if it already exists"""
        mock_branch = MagicMock(name="existing_branch")
        mock_repo.return_value.branches = {"existing_branch": mock_branch}
        repo = mock_repo("/path/to/repo")
        branch = set_up_branch(repo, "existing_branch")
        self.assertEqual(branch, mock_branch)

    @patch("t3_cicd_cli.utils.git_operations.Repo")
    @patch("t3_cicd_cli.utils.git_operations.time.time", return_value=1234567890)
    def test_push(self, mock_time, mock_repo):
        """Test push creates a new branch and pushes it to the remote"""
        mock_remote = MagicMock()
        mock_repo.return_value.remotes = []
        mock_repo.return_value.create_remote.return_value = mock_remote
        repo = mock_repo("/path/to/repo")
        branch_name_hash = str(
            (hash("/path/to/repo" + str(mock_time.return_value)) & 0xFFFFFFFFFFFFFFFF)
        )
        branch = push("/path/to/repo")
        repo.create_head.assert_called_once_with(branch_name_hash)
        repo.git.checkout.assert_called_once_with(repo.create_head.return_value)
        mock_repo.return_value.create_remote.assert_called_once_with(
            "cicd", "https://github.com/wp161/cicd-localrepo.git"
        )
        mock_remote.push.assert_called_once_with(
            refspec=f"{branch_name_hash}:{branch_name_hash}"
        )
        self.assertEqual(branch, branch_name_hash)

    @patch("t3_cicd_cli.utils.git_operations.requests.get")
    def test_check_file_exists_success(self, mock_get):
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = "Success"
        mock_get.return_value = mock_response
        self.assertTrue(
            check_file_exists(
                "https://github.com/wp161/cicd-localrepo.git", "main", "LICENSE"
            )
        )

    @patch("t3_cicd_cli.utils.git_operations.requests.get")
    def test_check_file_exists_fail(self, mock_get):
        mock_response = MagicMock()
        mock_response.status_code = 400
        mock_response.text = "Fail"
        mock_get.return_value = mock_response
        self.assertFalse(
            check_file_exists(
                "https://github.com/wp161/cicd-localrepo.git", "main", "non-exist"
            )
        )

    @patch("t3_cicd_cli.utils.git_operations.requests.get")
    def test_is_github_repo_success(self, mock_get):
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = "Success"
        mock_get.return_value = mock_response

        self.assertTrue(is_github_repo("https://github.com/wp161/cicd-localrepo.git"))

    @patch("t3_cicd_cli.utils.git_operations.requests.get")
    def test_is_github_repo_fail(self, mock_get):
        mock_response = MagicMock()
        mock_response.status_code = 400
        mock_response.text = "Fail"
        mock_get.return_value = mock_response

        self.assertFalse(
            is_github_repo("https://github.com/CS6510-SEA-F24/t3-cicd-cli.git")
        )
        self.assertFalse(
            is_github_repo("/Users/User/Desktop/example/project/t3-cicd-cli")
        )
