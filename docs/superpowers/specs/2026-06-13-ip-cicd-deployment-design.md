# TutorSpace IP CI/CD Deployment Design

## Goal

Deploy TutorSpace to Ubuntu 22.04 at `http://176.109.106.132` and automatically
update it after every successful push to `main`.

## Architecture

GitHub Actions provides the CI gate. It runs the frontend tests and build plus
all six backend Maven test suites. Deployment runs only after those checks pass.

The deploy job connects to the server over SSH as `kirill`, then invokes one
root-owned deployment script through a narrowly scoped `sudoers` rule. The
script updates `/opt/tutorspace` to the exact tested commit and runs Docker
Compose.

The existing gateway container is the only public application container. It is
published as host port `80` and continues to route browser `/api/**` requests to
the internal services. PostgreSQL, Kafka, Kafka UI, and backend services have no
public host ports.

## Repository Changes

- `.github/workflows/deploy.yml` runs CI and deploys `main`.
- `docker-compose.prod.yml` adds production port and restart overrides.
- `scripts/deploy.sh` performs a deterministic server update.
- `scripts/bootstrap-server.sh` prepares the checkout, permissions, and sudo
  rule after the operator has installed Docker and configured SSH access.
- `docs/DEPLOYMENT.md` documents initial setup, GitHub secrets, deployment,
  rollback, logs, and health checks.

## Server Layout

- Application checkout: `/opt/tutorspace`
- Environment file: `/opt/tutorspace/.env`
- Deployment script: `/opt/tutorspace/scripts/deploy.sh`
- Persistent database volume: Docker named volume `postgres_data`

The `.env` file is created manually on the server with production secrets and is
never copied through GitHub Actions or committed to Git.

## SSH And Secrets

Two separate key pairs are used:

- A read-only GitHub deploy key lets the server clone and fetch the private
  repository.
- A CI deployment key lets GitHub Actions connect to the Ubuntu account
  `kirill`.

GitHub Actions repository secrets:

- `DEPLOY_HOST`: `176.109.106.132`
- `DEPLOY_USER`: `kirill`
- `DEPLOY_SSH_KEY`: private CI deployment key
- `DEPLOY_KNOWN_HOSTS`: pinned SSH host key from `ssh-keyscan`

The workflow uses strict host-key verification. It does not disable
`StrictHostKeyChecking`.

## Deployment Flow

1. A commit is pushed to `main`.
2. GitHub Actions checks out that commit.
3. Frontend dependencies are installed with `npm ci`; tests and build run.
4. Each backend service runs `mvn test`.
5. The Compose configuration is validated.
6. GitHub Actions connects to the server through SSH.
7. The root-owned deploy script fetches `origin/main`, verifies the requested
   SHA exists, and checks out that exact SHA.
8. Docker Compose validates the production configuration.
9. Docker Compose rebuilds and starts the stack with
   `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build --remove-orphans`.
10. The script waits for `http://127.0.0.1/health` and fails if it is not ready.

Only one deployment runs at a time. A newer `main` deployment cancels an older
one that has not completed.

## Failure And Rollback

CI failure prevents deployment. A deployment failure leaves logs available in
GitHub Actions and on the server through `docker compose logs`.

Rollback is explicit: run the deploy script with a previous commit SHA. Database
data remains in the named PostgreSQL volume. Deployment commands never use
`docker compose down -v`.

## Production Environment

The server `.env` uses unique random values for JWT, PostgreSQL users, database
passwords, and the seeded administrator password. YooKassa remains in test mode.
The return URL uses:

`http://176.109.106.132/app/tutor/subscription/checkout/result`

## Acceptance Criteria

- `http://176.109.106.132/health` returns HTTP 200.
- `http://176.109.106.132` loads the frontend.
- Backend and infrastructure ports are not publicly published.
- A successful push to `main` updates the server to the pushed SHA.
- A failing test prevents deployment.
- PostgreSQL data survives container rebuilds and restarts.
- Server `.env` and private SSH keys never appear in Git history or workflow
  logs.
