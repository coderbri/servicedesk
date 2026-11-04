# GitHub Actions CI and Git authentication

This guide records how the ServiceDesk continuous-integration workflow is set up and how to troubleshoot the GitHub authentication problems encountered while publishing it. The workflow example is specific to this Java 21 and Gradle project; the concepts apply to other repositories, but adjust the runtime, build tool, trigger branches, and test command for each project.

## What CI does

Continuous integration (CI) asks GitHub to check out a commit on a temporary machine and run a repeatable build or test command. A passing run shows that the selected checks passed for that commit. It does not prove that checks you did not run passed.

The ServiceDesk workflow is [`.github/workflows/ci.yml`](../.github/workflows/ci.yml). It runs when a commit is pushed to `main`, when a pull request targets `main`, or when a user starts it manually from the Actions page.

## A common workflow shape

There is no single finished template that fits every project. GitHub expects a workflow to follow its YAML structure, but each repository chooses its own language setup, build tool, test command, runner, and triggers. Start with the smallest workflow that runs checks the project can actually complete.

This outline shows the common outer structure. It is for learning the layout; by itself, it only checks out the code. Add the runtime setup and build/test steps required by your project before relying on it as CI.

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  workflow_dispatch:

jobs:
  checks:
    runs-on: ubuntu-latest
    steps:
      - name: Check out the repository
        uses: actions/checkout@v4
      # Add a step to install the project's language and tools.
      # Add a step to run its build and selected tests.
```

The shared pattern is `name` → `on` → `jobs` → runner and `steps`. The parts after checkout depend on the project. For this repository, the concrete version is [`.github/workflows/ci.yml`](../.github/workflows/ci.yml): it sets up Java 21, uses the Gradle wrapper, and filters test execution to the isolated service and controller tests. A Node.js, Python, or other Java project would keep the general structure but use its own runtime setup and checks.

The first CI workflow deliberately runs only these tests:

- `com.coderbri.servicedesk.service.ServiceRequestServiceTest`
- `com.coderbri.servicedesk.controller.ServiceRequestControllerTest`

They use mocks and do not need Oracle. `ServicedeskApplicationTests` starts the full Spring application context and currently needs Oracle. The workflow does not run that test or claim that the full suite passes. Gradle can compile other test source files while the `--tests` filters limit which tests execute.

## Workflow file concepts

GitHub discovers workflow YAML files saved under `.github/workflows/`.

- `name` gives the workflow a readable label in GitHub's **Actions** tab.
- `on` lists the events that start it. `push` and `pull_request` can be limited to a branch; `workflow_dispatch` enables a manual run.
- `jobs` lists independent units of work. A job runs on a runner such as `ubuntu-latest`.
- `steps` are the ordered actions and shell commands inside a job. A failed step normally fails the job.
- `actions/checkout` puts the repository files on the runner.
- `actions/setup-java` installs the requested Java distribution and version. Gradle caching can make later runs faster.
- `./gradlew` uses the Gradle version selected by the repository's wrapper, so the runner does not need a separate Gradle installation.

In the ServiceDesk workflow, the Gradle command is equivalent to:

```bash
./gradlew test \
  --tests "com.coderbri.servicedesk.service.ServiceRequestServiceTest" \
  --tests "com.coderbri.servicedesk.controller.ServiceRequestControllerTest"
```

When adapting this example, use the test classes that actually exist in the project. Avoid a broad test command if some tests require unavailable services such as a database; choose filters or configure the required service explicitly.

## Add, review, and publish a workflow

1. Inspect the project's build configuration, runtime version, test classes, default branch, and existing workflows. Make sure the proposed test command runs without developer-only services.
2. Add a YAML file such as `.github/workflows/ci.yml`. Configure the events, runner, setup actions, and build/test command for that repository.
3. Review only the intended file before committing:

   ```bash
   git add .github/workflows/ci.yml
   git diff --cached --check
   git diff --cached
   ```

   `git diff --cached` may open a pager. Press `q` to return to the shell.
4. Commit and push the change:

   ```bash
   git commit -m "ci: add isolated service and controller tests"
   git push origin main
   ```

   If the default branch is protected, push a feature branch and open a pull request targeting the default branch instead. Stage the workflow file explicitly so unrelated edits are not accidentally included.
5. In GitHub, open the repository's **Actions** tab, choose the workflow, and open the run for your commit. Select the job, then expand each step to see its logs. A green check means this workflow's configured job passed; a red mark means a step failed. Read the first failing step's output for the cause.

A run can be started manually from the workflow page when `workflow_dispatch` is configured. Select **Run workflow**, choose the branch, and start it.

## GitHub authentication for `git push`

Git authentication and workflow execution are separate. A personal access token is used here to authenticate a developer's HTTPS `git push`; it is not needed by this workflow to run tests. GitHub Actions provides a separate `GITHUB_TOKEN` for workflow tasks that need repository access.

GitHub does not accept an account password for HTTPS Git operations. Use one of these options instead:

- **GitHub CLI:** run `gh auth login` and follow the browser-based prompts. If the shell says `gh: command not found`, GitHub CLI is not installed.
- **GitHub Desktop:** sign in through the app and use it to publish or push the local repository.
- **Personal access token (PAT):** enter the token at Git's password prompt. Do not put a token in a command, commit, issue, screenshot, or chat message.

### Create a fine-grained token for this repository

1. Open GitHub **Settings → Developer settings → Personal access tokens → Fine-grained tokens**, then choose **Generate new token**.
2. Give it a recognizable name and select `coderbri` as the resource owner.
3. Under **Repository access**, choose **Only select repositories**, then select `servicedesk`.
4. Under **Repository permissions**, set **Contents** to **Read and write**. Also set **Workflows** to **Read and write** when the token will push a change under `.github/workflows/`.
5. Generate the token and copy the displayed secret to a secure place. GitHub shows the secret at creation; the token list later shows its settings, not the secret itself. Fine-grained token details and availability can vary if an organization requires approval.

At the HTTPS prompts, enter the GitHub username, then paste the token at the password prompt. The terminal does not display password input; that is expected. Keep the token private. Revoke a token that is lost or exposed, then create a replacement with only the access it needs.

### Troubleshoot `Invalid username or token`

This message means GitHub did not accept the credentials. Check these items without sharing the secret:

1. Confirm the password field received the token itself, not the token's name or your GitHub account password. A fine-grained token commonly begins with `github_pat_`; the complete value is longer.
2. Confirm the token's resource owner, selected repository, and required permissions. For a workflow-file push, check both **Contents: Read and write** and **Workflows: Read and write**.
3. Make sure the token was copied completely, with no surrounding quotes or added spaces. If uncertain, create a replacement and use GitHub's copy control when it is shown.
4. On macOS, if Git keeps using an old saved credential, open **Keychain Access**, search for `github.com`, and remove the stale GitHub internet-password entry. Retry the push and enter the new token when prompted.
5. If the token belongs to an organization, check whether an administrator must approve it. For a personal repository, check that the resource owner is the correct account.

Never paste a token into a support conversation. If authentication still fails, share only the exact error text after removing usernames or repository details you consider private; do not include the token.

## Tool installation trouble

If `gh auth login` reports `command not found`, GitHub CLI is not installed. On macOS, Homebrew can install it with `brew install gh` when Homebrew is working. An error such as `cannot load such file -- sorbet-runtime` occurs inside Homebrew before it installs `gh`; it is a Homebrew problem, not a GitHub authentication problem. Use GitHub Desktop or a PAT for the push while diagnosing Homebrew separately. Do not apply random Ruby, ownership, or permission changes to repair Homebrew.
