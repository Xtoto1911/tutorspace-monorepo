# TutorSpace IP CI/CD Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically test and deploy TutorSpace from `main` to Ubuntu at `http://176.109.106.132`.

**Architecture:** GitHub Actions runs frontend and backend checks, then connects to the server with a dedicated SSH key. A root-owned deployment script checks out the exact tested SHA and starts the existing stack with a production Compose override that exposes only the gateway on port 80.

**Tech Stack:** GitHub Actions, Bash, OpenSSH, Docker Engine, Docker Compose, React/Vitest, Maven/Spring Boot

---

### Task 1: Production Compose Override

**Files:**
- Create: `docker-compose.prod.yml`

- [ ] **Step 1: Add the production override**

Create a Compose override that publishes `80:80`, sets restart policies for
long-running services, and does not expose backend or infrastructure ports.

- [ ] **Step 2: Validate merged Compose**

Run:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.example config --quiet
```

Expected: exit code 0.

- [ ] **Step 3: Verify public ports**

Run:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.example config
```

Expected: only gateway publishes host port 80.

### Task 2: Deterministic Deployment Script

**Files:**
- Create: `scripts/deploy.sh`
- Create: `scripts/tests/deploy-script.test.sh`

- [ ] **Step 1: Write static contract tests**

Test that the script requires a 40-character SHA, fetches `origin main`, checks
out the requested SHA, validates Compose, starts with `--remove-orphans`, avoids
volume deletion, and checks `/health`.

- [ ] **Step 2: Run the test and verify failure**

Run:

```bash
bash scripts/tests/deploy-script.test.sh
```

Expected: failure because `scripts/deploy.sh` does not exist.

- [ ] **Step 3: Implement deployment**

The script must:

```text
accept exact SHA
lock concurrent deployment
verify repository and .env
fetch origin/main
verify SHA is reachable from origin/main
checkout detached SHA
validate merged Compose
build and start services
wait for health endpoint
print deployed SHA
```

- [ ] **Step 4: Run the contract test**

Run:

```bash
bash scripts/tests/deploy-script.test.sh
```

Expected: pass.

### Task 3: Server Bootstrap Script

**Files:**
- Create: `scripts/bootstrap-server.sh`
- Create: `scripts/tests/bootstrap-script.test.sh`

- [ ] **Step 1: Write static contract tests**

Test that bootstrap accepts repository URL and CI public key, creates
`/opt/tutorspace`, installs authorized keys safely, creates a narrow sudoers
entry, and never creates `.env` with default secrets.

- [ ] **Step 2: Run the test and verify failure**

Run:

```bash
bash scripts/tests/bootstrap-script.test.sh
```

Expected: failure because bootstrap script does not exist.

- [ ] **Step 3: Implement bootstrap**

The script validates root execution and dependencies, installs the CI public key
for `kirill`, clones or updates the repository, installs the deploy script as
root-owned, and writes a sudoers rule allowing only the deployment wrapper.

- [ ] **Step 4: Run the contract test**

Run:

```bash
bash scripts/tests/bootstrap-script.test.sh
```

Expected: pass.

### Task 4: GitHub Actions Workflow

**Files:**
- Create: `.github/workflows/deploy.yml`
- Create: `scripts/tests/workflow.test.mjs`

- [ ] **Step 1: Write workflow contract tests**

Use Node's YAML-capable environment only if already available; otherwise test
the workflow text for triggers, CI jobs, permissions, concurrency, pinned host
key, and exact SHA deployment.

- [ ] **Step 2: Run the test and verify failure**

Run:

```bash
node scripts/tests/workflow.test.mjs
```

Expected: failure because workflow does not exist.

- [ ] **Step 3: Implement workflow**

The workflow must trigger on `main` pushes and manual dispatch, run frontend
checks, run six Maven suites through a matrix, validate Compose, and deploy only
after all checks pass.

- [ ] **Step 4: Run workflow contract tests**

Run:

```bash
node scripts/tests/workflow.test.mjs
```

Expected: pass.

### Task 5: Deployment Documentation

**Files:**
- Create: `docs/DEPLOYMENT.md`
- Modify: `README.md`

- [ ] **Step 1: Document key generation**

Include commands for a server GitHub deploy key and a separate Actions-to-server
key.

- [ ] **Step 2: Document server setup**

Include bootstrap execution, server `.env`, firewall rules, first deployment,
health checks, logs, and rollback.

- [ ] **Step 3: Document GitHub settings**

List exact repository deploy key and Actions secret names.

- [ ] **Step 4: Update README**

Link deployment documentation and correct stale PostgreSQL/H2 notes.

### Task 6: Verification And Publication

**Files:**
- Verify all changed files

- [ ] **Step 1: Run script contract tests**

```bash
bash scripts/tests/deploy-script.test.sh
bash scripts/tests/bootstrap-script.test.sh
node scripts/tests/workflow.test.mjs
```

- [ ] **Step 2: Validate Compose**

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.example config --quiet
```

- [ ] **Step 3: Run frontend checks**

```bash
cd frontend
npm test
npm run build
```

- [ ] **Step 4: Run backend checks**

Run `mvn test` in each of the six backend service directories.

- [ ] **Step 5: Review secrets and Git state**

Confirm no private keys or `.env` are tracked and inspect the final diff.

- [ ] **Step 6: Commit and push**

Commit the CI/CD implementation and push `main` to GitHub.
