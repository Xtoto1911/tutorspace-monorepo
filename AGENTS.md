# AGENTS.md

## Working rules
- Trust executable config over prose. `ARCHITECTURE.md` and `frontend/FRONTEND_BACKEND_CONTRACT.md` are useful, but some current-state sections lag the repo.
- Repo-local OpenCode guidance lives at `.opencode/agens/build_norm.md` if you need to adjust it; the path is easy to mistype.
- Require evidence for changes. There is no CI here, so run the narrowest relevant local check and report it.

## Canonical references
- `ARCHITECTURE.md`: intended service boundaries and ownership. Verify against `backend/*` and `docker-compose.yml` before assuming a service is still only "planned".
- `frontend/FRONTEND_BACKEND_CONTRACT.md`: frontend/API contract. Frontend API helpers omit `/api`; `VITE_API_BASE_URL` adds it.
- `frontend/PRODUCT.md` and `frontend/DESIGN.md`: product and UI constraints. Keep the warm, calm, professional style, OKLCH tokens, Onest font, no glassmorphism, and WCAG AA target.
- TutorSpace charges tutors for CRM subscriptions only, not lesson payments. Any YooKassa work must stay on test credentials.

## Local stack
- Start and stop the whole stack from repo root with `docker compose up --build` and `docker compose down`.
- Host ports: frontend `4173`, auth `18080`, marketplace `18081`, CRM `18082`, notification `18083`, admin `18084`, Kafka UI `18085`, subscription `18086`.
- `.env.example` is the local source of truth for shared JWT values, backend base URLs, frontend API mode flags, and seeded admin credentials (`admin@tutorspace.local` / `admin12345`).
- Backend data stores are in-memory H2 databases; restarting a service or container wipes local data.

## Frontend
- Work from `frontend/`: `npm run dev`, `npm run build`, `npm run typecheck`, `npm test`.
- This is not a normal Vite workflow even though Vite is installed. `npm run dev` and `npm run preview` run `node scripts/serve.mjs`; `npm run build` is `tsc --noEmit -p tsconfig.json && node scripts/build.mjs`; bundling happens through custom Rollup scripts.
- There is no `vite.config.*` or `vitest.config.*`. Do not assume standard Vite config locations.
- Outside Docker, `frontend/src/shared/api/http.ts` defaults `VITE_API_MODE` to `mock`, `VITE_AUTH_API_MODE` to `live`, and `VITE_API_BASE_URL` to `/api`. If you expect live frontend data outside Docker, set env explicitly.
- The local dev proxy in `frontend/scripts/serve.mjs` only knows auth, marketplace, and CRM routes. It forwards every `/api/tutor/**` request to `CRM_SERVICE_URL`, so tutor subscription endpoints are misrouted locally, and it has no dedicated proxy for `/api/admin/**` or `/api/moderator/**`.
- Browser code should keep using one public `/api/**` origin. Do not hardcode Docker service hostnames in frontend code.

## Backend
- Each service under `backend/*` is its own Maven project; there is no root `pom.xml` aggregator.
- The real module set today is `auth-service`, `marketplace-service`, `crm-service`, `notification-service`, `admin-service`, and `subscription-service`; `api-gateway` is the missing piece. Trust `backend/*` and `docker-compose.yml` over stale "planned next" prose.
- Services use Spring Boot `3.5.10` and Java `21`. `auth-service` has `mvnw`/`mvnw.cmd`; the other services rely on system `mvn`.
- Run backend verification from the target service directory, usually `mvn test`.
- Every backend Dockerfile builds with `mvn -B package -DskipTests`; a successful Docker build does not prove tests passed.

## Tooling gaps
- No GitHub Actions workflows, pre-commit config, ESLint, Prettier, or Checkstyle configs were found. Do not invent lint or format commands.
