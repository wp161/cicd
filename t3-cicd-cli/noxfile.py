import nox


# Define a session for running tests
@nox.session
def tests(session):
    session.run("poetry", "install", external=True)
    session.run("pytest", "tests/")


# Define a session for linting with flake8
@nox.session
def lint(session):
    session.run("poetry", "install", external=True)
    session.run("black", "src/", "tests/")
    session.run("flake8", "src/", "tests/")


# Define a session for building the project
@nox.session
def build(session):
    session.run("poetry", "install", external=True)
    session.run("poetry", "build", external=True)


# Define an "all" session that runs multiple tasks
@nox.session
def all(session):
    session.notify("tests")  # Run tests session
    session.notify("lint")  # Run lint session
    session.notify("build")  # Run build session
