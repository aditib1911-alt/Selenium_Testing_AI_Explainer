# Selenium + Java Test Automation Framework

UI (Login, Dashboard) and REST API test suite against two public demo services:
- UI: [SauceDemo](https://www.saucedemo.com)
- API: [reqres.in](https://reqres.in)

Stack: Maven, TestNG (parallel execution), Selenium 4 (Grid), RestAssured, AssertJ, Allure. Includes a read-only, Claude-powered [AI failure explainer](#ai-failure-explainer) that triages test failures on every PR.

## Table of contents

- [Project structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Running the suite locally](#running-the-suite-locally)
- [Reports](#reports)
- [AI failure explainer](#ai-failure-explainer)
- [Scope boundaries (intentional, not TODOs)](#scope-boundaries-intentional-not-todos)
- [reqres.in is a mock API](#reqresin-is-a-mock-api)
- [Test coverage](#test-coverage)
- [Verifying the acceptance criteria](#verifying-the-acceptance-criteria)
- [Troubleshooting](#troubleshooting)

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

## Prerequisites

1. **Java 17+** and **Maven 3.9+** installed.
   ```bash
   java -version
   mvn -version
   ```
2. **Docker Desktop** (or another Docker engine) — required to run the local Selenium Grid (Chrome/Firefox/Edge nodes).
3. **A free reqres.in API key** — sign up at https://app.reqres.in/api-keys.
   Every API test will fail fast with a clear message if this isn't set — it will not silently 401.
4. **(Optional) Anthropic API key** — only needed if you want to run the [AI failure explainer](#ai-failure-explainer) locally. Get one at https://console.anthropic.com/settings/keys.
5. **(macOS only, optional) Safari**, for the local-only `testng-full.xml` leg:
   ```bash
   sudo safaridriver --enable
   ```
   SafariDriver cannot run headless and cannot join the Dockerized Grid (no official Apple-sanctioned container image exists) — it runs as a separate local session on the host.

## Setup

1. **Clone the repo:**
   ```bash
   git clone <this-repo-url>
   cd <repo-directory>
   ```
2. **Configure secrets.** Copy the example env file and fill in your keys:
   ```bash
   cp .env.example .env
   ```
   Then edit `.env`:
   ```dotenv
   REQRES_API_KEY=your-reqres-key-here
   ANTHROPIC_API_KEY=your-anthropic-key-here   # optional, only for the AI failure explainer
   ```
   `.env` is git-ignored and never committed. Export these into your shell before running tests (or use a tool like `direnv`/`dotenv` to load them automatically):
   ```bash
   export $(grep -v '^#' .env | xargs)
   ```
3. **Install dependencies** (also validates the project compiles):
   ```bash
   mvn -q compile
   ```

## Running the suite locally

```bash
# 1. Start the Grid (Chrome, Firefox, Edge nodes)
docker compose up -d --wait

# 2. Run the same suite CI runs (Chrome/Firefox/Edge + API)
mvn test

# ...or, locally only, add Safari:
mvn test -DsuiteFile=testsuites/testng-full.xml

# 3. Tear the Grid down when done
docker compose down
```

Chrome, Firefox, and Edge run **headless** by default (faster, no display server needed, matches CI). Safari always runs headed — Apple does not provide a headless mode for SafariDriver.

**Running a single test class or suite section**, e.g. just the login tests:
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
