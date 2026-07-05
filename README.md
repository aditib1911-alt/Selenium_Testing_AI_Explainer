# Selenium + Java Test Automation Framework

UI (Login, Dashboard) and REST API test suite against two public demo services:
- UI: [SauceDemo](https://www.saucedemo.com)
- API: [reqres.in](https://reqres.in)

Stack: Maven, TestNG (parallel execution), Selenium 4 (Grid), RestAssured, AssertJ, Allure. Includes a read-only, Claude-powered [AI failure explainer](#ai-failure-explainer) that triages test failures on every PR.

## Table of contents

- [Quick start — run this on your own computer](#quick-start--run-this-on-your-own-computer)
- [Project structure](#project-structure)
- [Running a single test class or suite section](#running-a-single-test-class-or-suite-section)
- [Reports](#reports)
- [AI failure explainer](#ai-failure-explainer)
- [Scope boundaries (intentional, not TODOs)](#scope-boundaries-intentional-not-todos)
- [reqres.in is a mock API](#reqresin-is-a-mock-api)
- [Test coverage](#test-coverage)
- [Verifying the acceptance criteria](#verifying-the-acceptance-criteria)
- [Troubleshooting](#troubleshooting)

## Quick start — run this on your own computer

This section assumes no prior experience. Every command below is meant to be copy-pasted, one
at a time, into a **terminal**. On macOS, open the terminal via Spotlight (press `Cmd + Space`,
type `Terminal`, press Enter). On Windows, use **Git Bash** (installed alongside Git in Step 1)
or **WSL**. On Linux, use your regular terminal app.

### Step 1 — Install the required tools (one-time only)

You need four things installed: **Git**, **Java 17+**, **Maven 3.9+**, and **Docker Desktop**.
Check what you already have by pasting these one at a time:
```bash
git --version
java -version
mvn -version
docker --version
```
If a command prints a version number, that tool is already installed — skip it. If it says
"command not found," install it:

| Tool | What it's for | macOS (with [Homebrew](https://brew.sh)) | Windows / Linux |
|---|---|---|---|
| Git | Downloads the project's code | `brew install git` | [git-scm.com/downloads](https://git-scm.com/downloads) |
| Java 17+ | Runs the test code | `brew install openjdk@21` | [adoptium.net](https://adoptium.net/) (choose version 17 or newer) |
| Maven 3.9+ | Builds the project and runs the tests | `brew install maven` | [maven.apache.org/download](https://maven.apache.org/download.cgi) |
| Docker Desktop | Runs the disposable browsers (Chrome/Firefox/Edge) the UI tests drive | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) | Same link |

After installing, **close and reopen your terminal**, then re-run the four check commands above
to confirm each one now prints a version. Also open the Docker Desktop app once from your
Applications/Start menu and leave it running in the background — the `docker` command doesn't
work unless the Docker Desktop app itself is open.

### Step 2 — Download (clone) this project

```bash
git clone <this-repo-url>
cd <repo-directory>
```
(Replace `<this-repo-url>` with the repository's URL and `<repo-directory>` with the folder name
Git just created — it's the last part of the URL, without `.git`.)

### Step 3 — Get your personal API keys

This project talks to a real third-party API (reqres.in) that requires a free personal key, so
you need to sign up and get your own. Nobody else's key will work for you, and this key is never
shared or committed to the project.

1. **reqres.in key (required)** — every API test fails immediately with a clear error message
   if this is missing.
   - Go to https://app.reqres.in/api-keys
   - Sign up for a free account (or log in)
   - Copy the API key it gives you
2. **Anthropic key (optional)** — only needed if you want to run the
   [AI failure explainer](#ai-failure-explainer), a bonus feature that explains *why* a test
   failed. Everything else in this project works fine without it.
   - Go to https://console.anthropic.com/settings/keys
   - Sign in (or create an account) and click "Create Key"
   - Copy the key

Keep both keys somewhere safe — treat them like passwords. Don't paste them into chat messages,
screenshots, or anywhere public.

### Step 4 — Give the project your keys

The project reads keys from a file named `.env`, which is never uploaded to GitHub (it's listed
in `.gitignore` for exactly that reason — see [Troubleshooting](#troubleshooting) if you're ever
unsure).

1. Create your own copy of the template:
   ```bash
   cp .env.example .env
   ```
2. Open the new `.env` file in any text editor (e.g. `open -e .env` on macOS, or open it from
   Finder/File Explorer) and paste your keys in, so it looks like this:
   ```dotenv
   REQRES_API_KEY=paste-your-reqres-key-here
   ANTHROPIC_API_KEY=paste-your-anthropic-key-here
   ```
   (Leave `ANTHROPIC_API_KEY` blank if you skipped the optional step above.)
3. Save the file, then load it into your current terminal session — you'll need to repeat this
   one line any time you open a **new** terminal window/tab to run the tests:
   ```bash
   export $(grep -v '^#' .env | xargs)
   ```

### Step 5 — Install the project's dependencies

This downloads all the libraries the project needs and checks that everything compiles.
It can take a few minutes the first time:
```bash
mvn -q compile
```

### Step 6 — Start the local browser grid

The UI tests drive real browsers running inside Docker containers, managed by a tool called
Selenium Grid. Start it with:
```bash
docker compose up -d --wait
```
This may take a minute the first time, as it downloads the browser images. Leave it running —
you'll shut it down in the last step.

### Step 7 — Run the tests

```bash
mvn test
```
This runs every UI test (in Chrome, Firefox, and Edge) and every API test. It typically takes
under 5 minutes. You'll see live output in the terminal as each test passes or fails.

### Step 8 — View the results in a readable report

```bash
mvn io.qameta.allure:allure-maven:report
```
Then open `target/site/allure-maven-plugin/index.html` in your browser (double-click it from
Finder/File Explorer, or run `open target/site/allure-maven-plugin/index.html` on macOS) to see
a visual pass/fail report.

### Step 9 — Shut down the browser grid when you're done

```bash
docker compose down
```
This frees up the resources Docker was using. Run `docker compose up -d --wait` again next time
you want to run the tests.

---

That's the whole loop. Once set up, running the suite again later is just: open a terminal in
this folder, run the `export` command from Step 4, then Steps 6–9.

## Project structure

```
.
├── docker-compose.yml                  # Local Selenium Grid: hub + Chrome/Firefox/Edge nodes
├── pom.xml                             # Maven build, dependencies, surefire/allure/exec plugins
├── testsuites/
│   ├── testng-ci.xml                   # Chrome/Firefox/Edge + API + AI-agent unit tests (matches CI)
│   └── testng-full.xml                 # Same, plus Safari (macOS-local only)
├── .github/workflows/ci.yml            # GitHub Actions: grid up -> mvn test -> Allure report (test suite only)
└── src/test/java/com/qa/framework/
    ├── base/          # DriverFactory (ThreadLocal WebDriver), BaseUiTest, BaseApiTest
    ├── pages/         # Page objects (BasePage + LoginPage, InventoryPage, CartPage, ProductDetailPage)
    ├── api/           # ApiClient, ApiEndpoints, request/response models
    ├── tests/ui/      # LoginTests, DashboardTests
    ├── tests/api/     # UserApiTests, RegisterApiTests
    ├── listeners/     # TestListener -- screenshot + DOM capture on failure, feeds the AI explainer
    ├── utils/         # ConfigReader (env-var-overrides-properties), TestDataProvider
    ├── constants/     # FrameworkConstants (explicit wait time, etc.)
    └── aiagent/       # Read-only AI failure explainer (see below)
```

**(macOS only, optional) Safari** — to also run the local-only `testng-full.xml` leg, which adds
Safari to the browser matrix:
```bash
sudo safaridriver --enable
```
Then run:
```bash
mvn test -DsuiteFile=testsuites/testng-full.xml
```
Chrome, Firefox, and Edge run **headless** by default (faster, no display server needed, matches
CI). Safari always runs headed — Apple does not provide a headless mode for SafariDriver, and it
cannot join the Dockerized Grid (no official Apple-sanctioned container image exists), so it runs
as a separate local session on the host instead.

## Running a single test class or suite section

e.g. just the login tests:
```bash
mvn test -Dtest=LoginTests
```
> Note: because `surefire.suiteXmlFiles` is pinned to `testsuites/testng-ci.xml` by default, `-Dtest` filtering only works reliably if the class is listed in that suite file — for ad hoc single-class runs it's often simpler to comment out unrelated `<test>` blocks in a scratch copy of the suite XML, or point `-DsuiteFile` at a small custom TestNG XML.

## Reports

```bash
mvn io.qameta.allure:allure-maven:report
# or, live:
allure serve target/allure-results
```

## AI failure explainer

A read-only, local CLI agent (`com.qa.framework.aiagent.explainer.FailureExplainerRunner`)
scans `allure-results/` for any failed test -- UI or API -- and asks Claude, **once per
failing test**, for a root-cause hypothesis and a concrete recommended fix (e.g. the
corrected CSS/XPath selector for a drifted UI locator, or the specific assertion/code
change for an API failure).

It only reads Allure results/attachments and existing source files -- it never edits
source, never commits, and is not wired into any CI/CD or GitHub workflow. This is
unrelated to the "no auto-retry" policy below: no test is ever rerun or retried by this
agent, it only explains failures that already happened. Requires `ANTHROPIC_API_KEY`
(see `.env.example`).

Run it after a local `mvn test`:
```bash
export ANTHROPIC_API_KEY=your-key-here
mvn exec:java -Dexec.mainClass=com.qa.framework.aiagent.explainer.FailureExplainerRunner -Dexec.classpathScope=test
```

As each failure is analyzed, its explanation prints to the console immediately, e.g.:
```
2 failed test(s) found. Asking Claude to explain each one...

[1/2] Analyzing LOGIN-02...
### LOGIN-02 (UI)
- **Category:** LOCATOR_DRIFT
- **Confidence:** high
- **Root cause:** The [data-test='login-button'] selector no longer matches any element;
  the DOM snapshot shows the attribute was renamed to data-test='login-submit'.
- **Recommended fix:** Update LOGIN_BUTTON in LoginPage.java to
  By.cssSelector("[data-test='login-submit']").

[2/2] Analyzing API-07...
...

Wrote 2 explanation(s) to target/aiagent/failure-explanations.md
```

All explanations are also written to `target/aiagent/failure-explanations.md` for later reference. If there are no failed tests, it prints `No failed tests found -- nothing to explain.` and exits cleanly without calling the Claude API.

## Scope boundaries (intentional, not TODOs)

- **No mobile/device emulation** — desktop browsers only, by design.
- **Safari is excluded from CI** — GitHub Actions Linux runners can't run Safari. It's a macOS-local-only leg (`testng-full.xml`).
- **No auto-retry on failure** — a flaky test should be visible, not hidden behind a retry. See the verification loop below.

## reqres.in is a mock API

POST/PUT/PATCH/DELETE responses are echoed back but never persisted server-side. Tests assert only against the response of the same call that produced it — never via a follow-up GET, which would read the static mock dataset and produce a false failure.

## Test coverage

38 enumerated scenarios across Login (9), Dashboard (15), and REST API (14), each tagged with a stable ID (`LOGIN-01`, `DASH-04`, `API-12`, ...) matched to a `@Test(description = "ID")` method — see the plan/coverage matrix for the full list. (A planned API-15 missing/invalid-key negative test was dropped: reqres.in's own key enforcement was verified live to be non-deterministic — 401 one moment, no enforcement at all a minute later with zero code change on our side — so asserting a specific status code there would be flaky by construction.)

## Verifying the acceptance criteria

1. **Coverage** — count implemented `@Test` scenario IDs against the 39-item matrix; target >95%.
2. **Performance** — full suite (`testng-ci.xml`) should complete in <5 minutes; check the Allure Timeline tab for the long-pole thread if not.
3. **Flakiness** — run the suite 3 consecutive times (fresh `docker compose down && up -d --wait` between runs); every test must produce an identical result across all three runs.

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| API tests fail fast with a "missing REQRES_API_KEY" message | `.env` not created/exported, or the key wasn't picked up by your shell. Re-run `export $(grep -v '^#' .env \| xargs)` in the same shell you run `mvn test` from. |
| UI tests can't connect / `SessionNotCreatedException` | Selenium Grid isn't up. Run `docker compose up -d --wait` and confirm all 4 containers are healthy with `docker compose ps`. |
| `docker compose up` hangs on `--wait` | A previous Grid is still running on the same ports. `docker compose down` first, then retry. |
| Safari tests don't run / are skipped | Safari only runs via `testng-full.xml` on macOS, and requires `sudo safaridriver --enable` once per machine. It's intentionally excluded from `testng-ci.xml` and CI. |
| AI failure explainer logs `HTTP 401` per failure | `ANTHROPIC_API_KEY` is missing, empty, or invalid in your shell. This is read-only and logs to stderr per failure without crashing the run. |
| `allure serve` command not found | Install the Allure CLI (`brew install allure` on macOS, or see the [Allure docs](https://allurereport.org/docs/gettingstarted-installation/)) — the Maven plugin alone only generates the static report, not the `serve` command. |
