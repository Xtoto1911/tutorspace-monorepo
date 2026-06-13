# TutorSpace Monorepo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create and publish the private `Xtoto1911/tutorspace-monorepo` repository without modifying the structure or history of the seven existing repositories.

**Architecture:** Build a separate working directory at `C:\JavaMy\newDiploma\monorepo`. Export tracked files from each existing Git repository, copy an explicit allowlist of root deployment files, add monorepo-level ignore and validation files, then initialize one clean Git history. Create the GitHub repository as private and push `main`.

**Tech Stack:** Git, GitHub connector/API, PowerShell, Docker Compose, Maven, Node.js, React

---

### Task 1: Inventory and Source Safety

**Files:**
- Read: `C:\JavaMy\newDiploma\frontend`
- Read: `C:\JavaMy\newDiploma\backend\*\`
- Read: `C:\JavaMy\newDiploma\docker-compose.yml`

- [ ] **Step 1: Verify all seven source repositories are clean**

Run `git status --short --branch` in frontend and every backend service.

Expected: every repository is on `feature/live-api-responsive` with no modified or untracked application files.

- [ ] **Step 2: Record source commit SHAs**

Run `git rev-parse HEAD` in every source repository and save the values in the execution log.

Expected: seven non-empty commit SHAs.

- [ ] **Step 3: Verify the target path**

Resolve `C:\JavaMy\newDiploma\monorepo` and confirm it is a child of `C:\JavaMy\newDiploma`.

Expected: the path does not resolve to any source repository.

### Task 2: Assemble the Monorepo Copy

**Files:**
- Create: `C:\JavaMy\newDiploma\monorepo\frontend\`
- Create: `C:\JavaMy\newDiploma\monorepo\backend\*\`
- Create: `C:\JavaMy\newDiploma\monorepo\.gitignore`
- Copy: root deployment files

- [ ] **Step 1: Create an empty target directory**

Remove the target only if its resolved path is exactly `C:\JavaMy\newDiploma\monorepo`, then recreate it.

- [ ] **Step 2: Export tracked frontend files**

Use `git archive HEAD` from `frontend` and extract into `monorepo\frontend`.

Expected: no nested `frontend\.git` directory.

- [ ] **Step 3: Export tracked backend files**

For each of the six backend repositories, use `git archive HEAD` and extract into the matching `monorepo\backend\<service>` directory.

Expected: all service Dockerfiles, Maven manifests, sources, and tests are present without nested `.git`.

- [ ] **Step 4: Copy the root allowlist**

Copy:

```text
.env.example
AGENTS.md
ARCHITECTURE.md
BACKLOG.md
README.md
docker-compose.yml
docs/
docker/
.opencode/
```

Exclude `.env`, archives, screenshots, generated directories, and planning/build directories.

- [ ] **Step 5: Add monorepo `.gitignore`**

Create rules for:

```gitignore
.env
.env.*
!.env.example
node_modules/
dist/
target/
test-results/
playwright-report/
.idea/
.vscode/
*.iml
*.log
*.rar
*.zip
.codex-audit-screenshots/
BOOT-INF/
presentation-work/
```

### Task 3: Validate the Assembled Tree

**Files:**
- Inspect: `C:\JavaMy\newDiploma\monorepo`

- [ ] **Step 1: Scan for nested repositories and secrets**

Search recursively for `.git`, `.env`, private keys, archive files, `node_modules`, `dist`, and `target`.

Expected: none are present except the monorepo root `.git` after initialization and `.env.example`.

- [ ] **Step 2: Validate Docker Compose**

Run:

```powershell
docker compose --env-file .env.example config --quiet
```

Expected: exit code `0`.

- [ ] **Step 3: Verify frontend**

Run from `monorepo\frontend`:

```powershell
npm ci
npm test
npm run build
```

Expected: 123 unit tests pass and the production build succeeds.

- [ ] **Step 4: Verify backend module manifests**

Run `mvn test` in each backend service, using `mvnw.cmd test` for auth-service when needed.

Expected: all six backend test suites pass.

### Task 4: Initialize Clean Git History

**Files:**
- Create: `C:\JavaMy\newDiploma\monorepo\.git\`

- [ ] **Step 1: Initialize `main`**

Run:

```powershell
git init -b main
git add --all
git diff --cached --check
```

Expected: no whitespace errors and no secret/generated files staged.

- [ ] **Step 2: Inspect staged content**

Run `git status --short` and `git diff --cached --stat`.

Expected: frontend, six backend services, root Compose, and documentation are included.

- [ ] **Step 3: Create the initial commit**

Run:

```powershell
git commit -m "feat: initialize TutorSpace monorepo"
```

Expected: one root commit on `main`.

### Task 5: Create Private GitHub Repository

**Target:** `Xtoto1911/tutorspace-monorepo`

- [ ] **Step 1: Check repository availability**

Request metadata for `Xtoto1911/tutorspace-monorepo`.

Expected: repository is absent. If it exists, stop before overwriting anything.

- [ ] **Step 2: Create the repository**

Create `Xtoto1911/tutorspace-monorepo` with:

```text
visibility: private
auto_init: false
description: TutorSpace marketplace and CRM monorepo
```

- [ ] **Step 3: Configure origin**

Add the returned GitHub URL as `origin`.

- [ ] **Step 4: Push `main`**

Push the initial commit and set upstream tracking.

Expected: `origin/main` points to the local root commit.

### Task 6: Publication Verification

- [ ] **Step 1: Read repository metadata from GitHub**

Expected:

```text
full_name: Xtoto1911/tutorspace-monorepo
private: true
default_branch: main
```

- [ ] **Step 2: Compare local and remote commits**

Run:

```powershell
git rev-parse HEAD
git rev-parse origin/main
```

Expected: identical SHAs.

- [ ] **Step 3: Verify source repositories remain unchanged**

Repeat `git status --short --branch` in the original seven repositories.

Expected: all remain clean on `feature/live-api-responsive`.

- [ ] **Step 4: Report result**

Provide the private GitHub URL, initial commit SHA, validation results, local monorepo path, and note that CI/CD deployment is the next separate change.
