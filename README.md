# TutorSpace

## Project docs

- [Architecture](./ARCHITECTURE.md)
- [Backlog](./BACKLOG.md)
- [Ubuntu deployment and CI/CD](./docs/DEPLOYMENT.md)

## Start everything

```powershell
docker compose up --build
```

After startup:

- Frontend: http://localhost:4173
- Gateway health: http://localhost:4173/health

## Frontend API flow

The frontend always talks to its own `/api/**` origin. Inside Docker the Nginx
gateway proxies these paths:

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

Backend services, PostgreSQL, Kafka, and Kafka UI stay on the internal Docker
network and are not published on the host.

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

- The Docker stack uses PostgreSQL with the persistent `postgres_data` volume.
- Maven tests use isolated H2 databases.
- Public catalog, subjects, tutor profile and request submission already use live `marketplace-service` endpoints.
- Home page metrics/process content and FAQ pages remain local static content during the pre-gateway MVP phase.
