"""
Utilities for performing Git operations, including repository checks,
branch management, remote setup, and file existence validation in GitHub repositories.
"""

import git
from git import Repo
from t3_cicd_cli.constant.default import (
    DEFAULT_GITHUB_URL,
    DEFAULT_GITHUB_REMOTE_NAME,
    MAX_UNSIGNED_64_BIT_INT,
)
import requests
import time
import os


def is_git_repo(path):
    """
    Determine if a given path is a valid Git repository.

    Parameters:
        path (str): Local path to check.

    Returns:
        bool: True if the path is a Git repository, False otherwise.
    """
    try:
        result = Repo(path)
        return True
    except git.exc.InvalidGitRepositoryError:
        return False


def is_repo_dirty(path):
    """
    Check if the repository has uncommitted changes or untracked files.

    Parameters:
        path (str): Local repository path.

    Returns:
        bool: True if the repository is dirty, False otherwise.
    """
    repo = Repo(path)

    if not repo.head.is_valid():
        return False

    dirty = repo.is_dirty(untracked_files=True)

    if dirty:
        print(
            "You have the following uncommitted changes. Please commit them before proceeding."
        )
        print("Changes not staged for commit: ")
        for diff in repo.index.diff(None):
            print(f"      Modified:     {diff.a_path}")
        print("\nChanges staged for commit:")
        for diff in repo.index.diff("HEAD"):
            print(f"      Staged:       {diff.a_path}")
        if repo.untracked_files:
            print("\nUntracked files:")
            for untracked in repo.untracked_files:
                print(f"      Untracked:    {untracked}")
        return True
    return False


def setup_repo(path):
    """
    Initialize or retrieve a Git repository at the given path.

    Parameters:
        path (str): Path to the repository.

    Returns:
        Repo: Git repository object.
    """
    if not is_git_repo(path):
        repo = Repo.init(path)
        untracked_files = [
            os.path.join(repo.working_dir, f) for f in repo.untracked_files
        ]
        for file in untracked_files:
            repo.index.add(file)
        repo.index.commit("Initial commit")

    else:
        repo = Repo(path)
    return repo


def setup_remote(repo, remote_name, remote_url):
    """
    Ensure the specified remote exists in the repository.

    Parameters:
        repo (Repo): Git repository object.
        remote_name (str): Name of the remote.
        remote_url (str): URL of the remote.

    Returns:
        Remote: The specified remote object.
    """
    for remote in repo.remotes:
        if remote.name == remote_name:
            return remote
    return repo.create_remote(remote_name, remote_url)


def set_up_branch(repo, branch_name):
    """
    Ensure the specified branch exists in the repository.

    Parameters:
        repo (Repo): Git repository object.
        branch_name (str): Name of the branch to create or retrieve.

    Returns:
        Head: The specified branch object.
    """
    if branch_name in repo.branches:
        return repo.branches[branch_name]
    else:
        return repo.create_head(branch_name)


def push(path):
    """
    Push the current state of the repository to a remote branch.

    Parameters:
        path (str): Local repository path.

    Returns:
        str: Name of the newly created branch.
    """
    branch_name = str(hash(path + str(time.time())) & MAX_UNSIGNED_64_BIT_INT)
    repo = setup_repo(path)
    remote = setup_remote(repo, DEFAULT_GITHUB_REMOTE_NAME, DEFAULT_GITHUB_URL)
    branch = repo.create_head(branch_name)
    repo.git.checkout(branch)
    remote.push(refspec=f"{branch_name}:{branch_name}")
    return branch_name


def check_file_exists(git_url, branch, file_path):
    """
    Check if a file exists in a GitHub repository.

    Parameters:
        git_url (str): URL of the Git repository.
        branch (str): Branch name (e.g., 'main', 'master').
        file_path (str): Relative path to the file in the repository.

    Returns:
        bool: True if the file exists, False otherwise.
    """
    parts = git_url.rstrip("/").replace(".git", "").split("/")
    owner, repo = parts[-2], parts[-1]

    api_url = (
        f"https://api.github.com/repos/{owner}/{repo}/contents/{file_path}?ref={branch}"
    )

    response = requests.get(api_url)

    if response.status_code == 200:
        return True
    else:
        return False


def is_github_repo(url):
    """
    Verify if a given GitHub URL is a valid public repository.

    Parameters:
        url (str): URL of the GitHub repository.

    Returns:
        bool: True if the URL points to a valid GitHub repository, False otherwise.
    """
    if not url.startswith("https://github.com/"):
        return False

    try:
        parts = url.replace("https://github.com/", "").rstrip("/").split("/")
        if len(parts) < 2:
            return False
        owner, repo = parts[:2]
        repo = repo.replace(".git", "")
        api_url = f"https://api.github.com/repos/{owner}/{repo}"
        response = requests.get(api_url, timeout=10)
        return response.status_code == 200

    except requests.RequestException:
        return False
