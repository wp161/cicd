# t3-cicd-cli

A Python CLI for executing and orchestrating CI/CD pipelines. This CLI is built using Python's `Click` framework and supports YAML configuration for defining pipeline stages and jobs.

Developer Documentation: https://docs.google.com/document/d/1wbyMICGzKt7-Wu9GzD_DK_xpOU29ml3RfjknHgq26FA/edit?usp=sharing

## Table of Contents

- [Features](#features)
- [Development Setup](#development-setup)
  - [Prerequisites](#prerequisites)
  - [Build Instructions](#build-instructions)
  - [Other Useful Commands](#other-useful-commands)
- [User Manual](#user-manual)
  - [Installation Steps](#installation-steps)
  - [CLI Configuration](#cli-configuration)
  - [Run Pipeline](#run-pipeline)
- [Pull Request Process](#pull-request-process)
  - [Creating a PR](#creating-a-pr)
  - [Rules for PRs](#rules-for-prs)
- [CI/CD Workflows](#cicd-workflows)
- [License](#license)

## Features

- **Command-Line Interface (CLI)**: Allows users to trigger CI/CD pipeline execution.
- **Pipeline Configuration Support**: Define pipeline stages, jobs, and dependencies using GitLab CI/CD YAML syntax.

## Development Setup

### Prerequisites

To use this project, you need:

- Python 3.9 or higher
- Poetry (for dependency management)
- Nox (if you want to run automated sessions)

### Build Instructions

**Clone the repository:**

```bash
git clone https://github.com/CS6510-SEA-F24/t3-cicd-cli.git
```

**Run Nox sessions:**

We use nox (`noxfile.py`) to run automated sessions like testing, linting, and building the project.
Nox allows us to define a "do-all" type script to ensure everything runs in one command.

```bash
nox -s all
```

To run individual build tasks, use below `poetry` commands.

**Install the dependencies using Poetry:**

```bash
poetry install
```

**Build source distribution and wheel files:**

```bash
poetry build
```

**Run CLI command in dev mode with Poetry:**

```bash
poetry run <command>
# e.g. poetry run cicd config show
```

**Run all unit tests:**

```bash
poetry run pytest
```

**Run tests with test report and coverage reports:**

```bash
poetry run pytest --cov=src/ --cov-report=term-missing --cov-report=html --cov-fail-under=80
```
Test report can be found in `reports/`. Coverage reports can be found in the `htmlcov/`. If test coverage is below 80%, the build will fail.

### Other Useful Commands:

**Generate documentation**:

```bash
pdoc -o docs t3_cicd_cli 
```

Documentation will be available in the `docs/` directory.

**Automatically format the code in src/ and tests/ to follow PEP8 guidelines:**

```bash
poetry run black src/ tests
```

**Check code style against PEP8 guidelines:**

```bash
poetry run flake8 src/ tests/
```

## User Manual

### Installation Steps

#### Option 1: Install from GitHub Release (Requires Python 3.9+)

Go to the [release page](https://github.com/CS6510-SEA-F24/t3-cicd-cli/releases), download the `.whl` file from latest release, `cd` to the directory of the downloaded file, then run one of below `pip` commands:

```bash
# Install in current Python's site-packages: 
pip install t3_cicd_cli-0.1.0-py3-none-any.whl
# Or install the package for a specific user (make sure your `PATH` environment variable is correctly configured):
pip install --user t3_cicd_cli-0.1.0-py3-none-any.whl
```

#### Option 2: Install with local build (Requires Python 3.9+ and Poetry)

Clone the repository:

```bash
git clone https://github.com/CS6510-SEA-F24/t3-cicd-cli.git
```

`cd` into project directory, install dependencies and build:

```bash
poetry install
poetry build
```

Install package (use one of below `pip` commands):

```bash
# Install in current Python's site-packages: 
pip install dist/t3_cicd_cli-0.1.0-py3-none-any.whl
# Or install the package for a specific user (make sure your `PATH` environment variable is correctly configured):
pip install --user dist/t3_cicd_cli-0.1.0-py3-none-any.whl
```

### CLI Configuration

#### `cicd_config.json`

To simplify CLI commands and reduce the need of manually typing parameters when using
the CLI, some of the key configurations are stored in the user's local file system (default
path is `~/.cicd_config.json`) as a JSON file:

```bash
{
    "is-repo-remote": false,     # If the repo is a remote repo - default is false
    "is-run-remote": false,      # If the pipeline should run remotely - default is false
    "repo": null,                # URL to remote repo/ path to local repo - default is null
    "branch": "main",            # Branch of the remote repo - default is main
    "server": null,              # Endpoint for the remote CI/CD server - default is null
    "format": "plain"            # Output format: plain/ json/ yaml - default is plain
}
```

#### CLI configuration commands

You can update, reset, or view the current configurations using the following command:

```bash
cicd config -help
```

This will show all available commands and options for managing your CLI configuration.

### Run Pipeline

#### Handling remote/ local repo

To run the CI/CD pipeline, you need to specify whether to use a remote Git repository or a local repository in the [CLI Configuration](#cli-configuration).

- **Remote Repository**: If you're using a remote Git repository, the CI/CD will run using the `main` branch by default. You can override this and specify a different branch in the [CLI Configuration](#cli-configuration).

- **Local Repository**: If you're using a local repository that is tracked by Git, you must commit all changes before running the CI/CD. The system will not allow the pipeline to run with uncommitted changes.

Ensure that your configuration is set up correctly before running the pipeline.

## Pull Request Process

### Creating a PR

#### Always use feature branch to make change:

```bash
git checkout -b <branch_name> # create a new branch with <branch_name>
```

> Direct push to the `main` branch is strictly forbidden as this is the Production branch. All change
> should be merged with an approved PR.

#### Ensure your code is up-to-date with the `main` branch:

```bash
git config pull.rebase true # always use rebase to reconcile divergent branches
git pull
```

> Regularly pull from the `main` branch avoids conflicts pilling up.
> Please make sure you pull again before creating a PR.

#### Follow the PR Template:

- Your PR description should address any relevant context to help the reviewer to understand the
  PR. If this is related to an issue, reference the issue in the description.
- Make sure to use the checklist, and give explanations to any unchecked ones.

#### Check PR details:

- Make sure the origin and destination of the PR is correct, as well as everything in the Commits
  and Files changed tabs before clicking "Create Pull Request".

#### Submit your PR:

- No need to manually select reviewers. Once the PR is created, 2 reviewers will be automatically
  assigned based on the [Reviewer Lottery](https://github.com/marketplace/actions/reviewer-lottery)
  process.

### Rules for PRs

#### PR Size Limit:

- PRs should not exceed 150 lines unless absolutely necessary.

  > To override this, add an `override-size-limit` label in the PR, and provide explanation in the
  > PR description.

#### Testing:

- Ensure all new code is properly tested.

#### Commit Guidelines:

- Use [meaningful commit messages](https://www.freecodecamp.org/news/how-to-write-better-git-commit-messages/).
  Squash commits if necessary to clean up the history.

#### Merging:

- When merging PRs, always use **SQUASH AND MERGE** to combine all changes into a single commit.

#### Review Timebox:

- Please post reviews within 24 hours after you received the review request, or ask for others to
  do the review.

## CI/CD Workflows

This project uses **GitHub Actions** for CI/CD automation. The configured CI/CD workflows are:

- **PR Size Check**:

  - This workflow is triggered when a new PR is created, or when a new commit is pushed to an existing PR.
  - It fails if the size of PR is greater than 150 lines, and no override label is provided.

- **Assign Reviewers**:

  - This workflow is triggered when a new PR is created.
  - It assigns 2 random reviewers to the PR. Subsequent updates to the PR won't add more reviewers.

- **Pipeline Run**:
  - This workflow is triggered when:
    - A PR is created/ updated.
    - A PR is merged.
  - This workflow will execute:
    - Build: Ensures the code builds successfully.
    - Run Unit Tests: All tests must pass.
    - Test Coverage Verification: Verifies that test coverage meets minimum requirements.
    - Code Quality Checks: Checkstyle and SpotBugs
    - Artifacts Upload: Build artifacts and generated reports are uploaded after pipeline run

## License

This project is licensed under the MIT License. See the `LICENSE` file for more details.
