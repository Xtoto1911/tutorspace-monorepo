# TutorSpace Monorepo Design

## Goal

Create a private GitHub repository named `Xtoto1911/tutorspace-monorepo` that contains the complete deployable TutorSpace application while leaving the seven existing local repositories unchanged.

## Source Repositories

- `frontend`
- `backend/auth-service`
- `backend/marketplace-service`
- `backend/crm-service`
- `backend/notification-service`
- `backend/admin-service`
- `backend/subscription-service`

The root deployment files and documentation from `C:\JavaMy\newDiploma` are also included when they are required to build, run, test, or deploy the application.

## Monorepo Structure

```text
tutorspace-monorepo/
├── .github/
│   └── workflows/
├── backend/
│   ├── admin-service/
│   ├── auth-service/
│   ├── crm-service/
│   ├── marketplace-service/
│   ├── notification-service/
│   └── subscription-service/
├── frontend/
├── docs/
├── .env.example
├── .gitignore
├── AGENTS.md
├── ARCHITECTURE.md
├── BACKLOG.md
├── README.md
└── docker-compose.yml
```

## History Strategy

The monorepo starts with one clean initial commit. Existing repositories and their commit histories remain intact in `C:\JavaMy\newDiploma`. The monorepo does not contain nested `.git` directories.

## Included Content

Tracked source code, tests, Dockerfiles, Maven wrappers, package manifests, deployment configuration, and project documentation are included. Root files are selected explicitly instead of copying the entire directory blindly.

## Excluded Content

- Every `.git` directory
- `.env` and other local secrets
- `node_modules`
- `frontend/dist`
- Maven `target` directories
- Playwright and test result artifacts
- IDE metadata
- screenshots and temporary audit output
- `*.rar`, `*.zip`, and other local archives
- generated Java extraction directories such as `BOOT-INF`
- presentation working files that are not needed to run the application

## Security

Only `.env.example` is published. The repository is private. Production secrets will later be stored in GitHub Actions secrets and in a server-side `.env` file, never committed to Git.

## Validation

Before publication:

1. Verify there are no nested `.git` directories.
2. Verify `.env` and archive files are absent.
3. Run `git status` and inspect the complete initial commit.
4. Run frontend build and tests from the monorepo copy.
5. Validate Docker Compose configuration with `docker compose config`.
6. Create the private GitHub repository and push the `main` branch.
7. Read repository metadata back from GitHub and confirm `private: true`.

## CI/CD Scope

The initial monorepo creation prepares `.github/workflows` but does not deploy to the Ubuntu server until SSH details and production secrets are configured. Deployment automation will be added as a separate reviewed change.
