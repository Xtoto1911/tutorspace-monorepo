# TutorSpace local Docker setup

## Project docs

- [Architecture](./ARCHITECTURE.md)
- [Backlog](./BACKLOG.md)

## Start everything

```powershell
docker compose up --build
```

After startup:

- Frontend: http://localhost:4173
- Auth service: http://localhost:18080
- Marketplace service: http://localhost:18081
- CRM service: http://localhost:18082

## Frontend API flow

The frontend always talks to its own `/api/**` origin. Inside Docker the frontend dev server currently proxies these paths:

- `/api/auth/**` -> `auth-service`
- `/api/marketplace/**` -> `marketplace-service`
- `/api/client/**` -> `crm-service`
- `/api/tutor/**` -> `crm-service`

Example for auth:

```text
http://auth-service:8080/api/auth/**
```

That means the browser still uses one origin:

```text
http://localhost:4173/api/auth/login
http://localhost:4173/api/auth/register
```

Backend ports are published as `18080` and `18081` on the host to avoid collisions with locally running Spring Boot services on `8080` and `8081`.

## Environment

`docker-compose.yml` provides a development JWT secret by default:

```text
JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
```

To override it, create a local `.env` near `docker-compose.yml`:

```env
JWT_SECRET=your-base64-secret
JWT_ACCESS_TOKEN_TTL=15m
JWT_REFRESH_TOKEN_TTL=24h
```

Future subscription payments are planned through YooKassa in test mode only. The diploma environment must use test shop credentials and must not charge real money.

Planned variables for `subscription-service`:

```env
YOOKASSA_SHOP_ID=test-shop-id
YOOKASSA_SECRET_KEY=test-secret-key
YOOKASSA_RETURN_URL=http://localhost:4173/app/tutor/subscription
YOOKASSA_WEBHOOK_SECRET=local-webhook-secret
YOOKASSA_TEST_MODE=true
```

Official YooKassa test documentation: https://yookassa.ru/developers/payment-acceptance/testing-and-going-live/testing

Potential Java SDK candidate for the future `subscription-service`: https://github.com/DeelTer/YooKassaSDK

Before using it, check the latest release, license and Java 21 / Spring Boot 3 compatibility. Official YooKassa documentation remains the source of truth.

## Stop

```powershell
docker compose down
```

## Notes

- Databases are currently in-memory H2 databases inside each backend service.
- Registered users disappear after the `auth-service` container restarts.
- Public catalog, subjects, tutor profile and request submission already use live `marketplace-service` endpoints.
- Home page metrics/process content and FAQ pages remain local static content during the pre-gateway MVP phase.
