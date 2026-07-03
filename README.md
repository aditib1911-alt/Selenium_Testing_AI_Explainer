# Selenium + Java Test Automation Framework

UI (Login, Dashboard) and REST API test suite against two public demo services:
- UI: [SauceDemo](https://www.saucedemo.com)
- API: [reqres.in](https://reqres.in)

Stack: Maven, TestNG (parallel execution), Selenium 4 (Grid), RestAssured, AssertJ, Allure.

## Prerequisites

1. **Java 17+** and **Maven** installed.
2. **Docker Desktop** — required to run the local Selenium Grid (Chrome/Firefox/Edge nodes).
3. **A free reqres.in API key** — sign up at https://app.reqres.in/api-keys, then:
   ```
   export REQRES_API_KEY=your-key-here
   ```
   Every API test will fail fast with a clear message if this isn't set — it will not silently 401.
4. **Safari only** (macOS-local runs via `testng-full.xml`): one-time
   ```
   sudo safaridriver --enable
   ```
   SafariDriver cannot run headless and cannot join the Dockerized Grid (no official Apple-sanctioned container image exists) — it runs as a separate local session on the host.

## Running the suite

```bash
# Start the Grid (Chrome, Firefox, Edge nodes)
docker compose up -d --wait

# Chrome/Firefox/Edge + API — same suite CI runs
mvn test

# Local-only, adds Safari
mvn test -DsuiteFile=testsuites/testng-full.xml

docker compose down
```

Chrome, Firefox, and Edge run **headless** by default (faster, no display server needed, matches CI). Safari always runs headed — Apple does not provide a headless mode for SafariDriver.

## Reports

```bash
mvn io.qameta.allure:allure-maven:report
# or, live:
allure serve target/allure-results
```

## Scope boundaries (intentional, not TODOs)

- **No mobile/device emulation** — desktop browsers only, by design.
- **Safari is excluded from CI** — GitHub Actions Linux runners can't run Safari. It's a macOS-local-only leg (`testng-full.xml`).
- **No auto-retry on failure** — a flaky test should be visible, not hidden behind a retry. See the verification loop below.

## reqres.in is a mock API

POST/PUT/PATCH/DELETE responses are echoed back but never persisted server-side. Tests assert only against the response of the same call that produced it — never via a follow-up GET, which would read the static mock dataset and produce a false failure.

## Test coverage

38 enumerated scenarios across Login (9), Dashboard (15), and REST API (14), each tagged with a stable ID (`LOGIN-01`, `DASH-04`, `API-12`, ...) matched to a `@Test(description = "ID")` method — see the plan/coverage matrix for the full list. (A planned API-15 missing/invalid-key negative test was dropped: reqres.in's own key enforcement was verified live to be non-deterministic — 401 one moment, no enforcement at all a minute later with zero code change on our side — so asserting a specific status code there would be flaky by construction.)

## AI failure explainer

On PR-triggered CI runs, after `mvn test`, a read-only agent
(`com.qa.framework.aiagent.explainer.FailureExplainerRunner`) scans `allure-results/` for
any failed test -- UI or API -- and asks Claude for a root-cause hypothesis and a concrete
recommended fix (e.g. the corrected CSS/XPath selector for a drifted UI locator, or the
specific assertion/code change for an API failure). It posts one aggregated PR comment.

It never edits source, never commits, and never opens a branch or PR -- it only reads
Allure results/attachments and existing source files. This is unrelated to the "no
auto-retry" policy above: no test is ever rerun or retried by this agent, it only
explains failures that already happened. Requires `ANTHROPIC_API_KEY` (see `.env.example`)
set locally or as a `ANTHROPIC_API_KEY` GitHub Actions secret in CI.

Run it locally after a local `mvn test`:
```bash
export ANTHROPIC_API_KEY=your-key-here
mvn exec:java -Dexec.mainClass=com.qa.framework.aiagent.explainer.FailureExplainerRunner -Dexec.classpathScope=test
cat target/aiagent/failure-explanations.md
```

## Verifying the acceptance criteria

1. **Coverage** — count implemented `@Test` scenario IDs against the 39-item matrix; target >95%.
2. **Performance** — full suite (`testng-ci.xml`) should complete in <5 minutes; check the Allure Timeline tab for the long-pole thread if not.
3. **Flakiness** — run the suite 3 consecutive times (fresh `docker compose down && up -d --wait` between runs); every test must produce an identical result across all three runs.
