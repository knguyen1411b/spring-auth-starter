# Spring Auth Starter

A Spring Boot starter for authentication and role-based authorization, built with Java 21, PostgreSQL, and Gradle 9. It includes email verification, password recovery, rotating refresh tokens, and database-backed sessions for immediate access-token revocation.

[Getting started](#getting-started) Â· [Authentication](#authentication) Â· [Docker](#docker) Â· [Configuration](#configuration) Â· [Testing and CI](#testing-and-ci) Â· [Security policy](SECURITY.md) Â· [MIT license](LICENSE)

## Features

- Registration and single-use email activation links.
- BCrypt passwords, signed JWT access tokens, and hashed refresh tokens.
- Device sessions, refresh rotation, and immediate session revocation.
- Password reset by email with expiring tokens, resend cooldowns, and change notifications.
- Users, roles, permissions, protected service methods, and audit logs.
- PostgreSQL migrations with Flyway and schema validation with Hibernate.
- OpenAPI/Swagger in development, a public health endpoint, and optional Cloudinary avatars.
- PostgreSQL integration tests, Docker Compose, and GitHub Actions checks.

This is a source-code starter to adapt for your application. It does not include a general-purpose frontend, OAuth login, or MFA. The activation and password-reset pages are included.

## Requirements

- Java 21 for running or building outside Docker.
- Docker Engine or Docker Desktop with Linux containers and Docker Compose v2.
- No separately installed Gradle: use the committed Gradle Wrapper.

Docker is also required for the PostgreSQL Testcontainers integration tests. Running the packaged application only requires a Java 21 runtime and the configured database/SMTP services.

## Getting started

### 1. Configure the environment

For a new checkout, copy `.env.example` to `.env`. If `.env` already exists, merge the settings instead of replacing your credentials.

```bash
# Linux / macOS
[ -f .env ] || cp .env.example .env
```

```powershell
# Windows PowerShell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

Generate a random secret and paste it into `JWT_SECRET` in `.env`:

```bash
openssl rand -base64 64 | tr -d '\n'
```

PowerShell alternative:

```powershell
$bytes = [byte[]]::new(64)
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

`JWT_SECRET` must contain at least 64 characters. It is used as UTF-8 key material, not decoded from Base64. Never commit `.env` or reuse the example passwords in a deployment.

### 2. Start PostgreSQL and Mailpit

```bash
docker compose up -d
```

The example configuration creates database `auth_template` with user `auth` and password `local-dev-only`. Mailpit captures local email without sending it to real recipients.

If a host port is already in use, change `POSTGRES_PORT`, `MAILPIT_SMTP_PORT`, or `MAILPIT_UI_PORT` in `.env`. For a host-run application, also update the port in `SPRING_DATASOURCE_URL` or `MAIL_PORT` as appropriate. You can use an existing PostgreSQL server instead of the Compose database. If you change `PORT`, update `APP_PUBLIC_BASE_URL` so email links point to the correct application URL.

### 3. Run the application

```bash
# Linux / macOS
bash ./gradlew bootRun
```

```powershell
# Windows
.\gradlew.bat bootRun
```

| Service | Default URL |
| --- | --- |
| Application | http://localhost:8080 |
| Swagger UI, development only | http://localhost:8080/swagger-ui/index.html |
| Mailpit inbox | http://localhost:8025 |
| Health | http://localhost:8080/actuator/health |

A healthy application returns `{"status":"UP"}`. The health endpoint does not expose component details and does not probe SMTP.

## Authentication

### Register, activate, and sign in

The following examples use a POSIX shell. On Windows, use Swagger UI or adapt quoting for `curl.exe`.

```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-up \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"Example-password-123!","email":"demo@example.test","fullName":"Demo User"}'
```

Open the activation email in Mailpit, follow its link, and then sign in:

```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-in \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"Example-password-123!","deviceId":"browser-1","deviceName":"My browser"}'
```

Send `data.accessToken` in `Authorization: Bearer <accessToken>`. A new user has no roles; an administrator must grant permissions before that user can access administrative APIs.

### Auth endpoints

All paths below are relative to `/api/v1/auth`.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/sign-up` | Register an inactive account; HTTP 201 |
| POST | `/activate` | Activate using `{ "token": "..." }` |
| POST | `/sign-in` | Obtain access and refresh tokens |
| POST | `/refresh` | Rotate using `{ "refreshToken": "...", "deviceId": "..." }` |
| POST | `/forgot-password` | Request recovery using `{ "email": "..." }` |
| POST | `/reset-password` | Reset using `{ "token": "...", "newPassword": "..." }` |
| GET | `/profile` | Read the current profile and permissions |
| GET | `/sessions` | List active device sessions |
| DELETE | `/sessions/{deviceId}` | Revoke one device's access and refresh tokens |
| DELETE | `/sessions` | Revoke all sessions |
| POST | `/change-password` | Submit `currentPassword`, `newPassword`, and `logoutAllSessions` |
| POST | `/avatar` | Upload multipart `avatarFile` when Cloudinary is enabled |

The first six endpoints are public; the remaining endpoints require a valid access token. Validation errors return 400, authentication failures return 401, insufficient permissions return 403, and data conflicts return 409. The API uses the `success`, `statusCode`, `message`, and optional `data` response envelope.

### Sessions and revocation

Access tokens expire after 15 minutes by default; refresh tokens expire after 7 days. Refresh token rotation is single-use: only one concurrent refresh succeeds. Clients must serialize refresh requests and store the new token pair before refreshing again.

A JWT's `sid` identifies a database session. Each authenticated request checks that the session exists, belongs to the user, is unexpired, and is not revoked. Refresh keeps the same session identity. Logout therefore rejects both earlier and refreshed access tokens on subsequent requests. Already executing requests are not cancelled.

Signing in again with the same `deviceId` revokes that device's previous session. Administrative locking or deactivation revokes all sessions; unlocking does not restore old tokens. With `logoutAllSessions: true`, an authenticated password change revokes all sessions, including the current one. With `false`, sessions remain active. Either option invalidates outstanding recovery links.

### Password recovery

Request a reset email:

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.test"}'
```

The endpoint returns the same HTTP 200 message for validly formatted addresses whether the account exists, is eligible, or is in its resend cooldown. Response timing is not guaranteed to be identical.

Eligible accounts receive a link to `/reset-password#token=...`. The included form reads the token from the fragment, removes it from the address bar, and submits the new password. Reset tokens are random, stored only as SHA-256 hashes, single-use, and expire after 15 minutes by default. The default resend cooldown is 60 seconds per account; issuing a new link invalidates the previous link.

A successful reset invalidates all outstanding recovery links and all access/refresh sessions, writes an audit event, and sends a notification email. It does not sign the user in automatically or remove an administrative lock.

Email is sent synchronously. A failed reset-link delivery rolls back the new token and returns the generic response. A failed password-change notification rolls back the reset so the token can be retried. Configure reliable SMTP before deployment.

## Administration

To provision the first administrator, set these values in `.env`:

```dotenv
BOOTSTRAP_ADMIN_ENABLED=true
SUPER_USER_USERNAME=your_admin_username
SUPER_USER_PASSWORD=your_unique_strong_password
SUPER_USER_EMAIL=admin@example.test
SUPER_USER_FULL_NAME=Administrator
```

Start the application once, then disable `BOOTSTRAP_ADMIN_ENABLED` and restart. No administrator is created by default. The bootstrap password must have at least 12 characters and at most 72 UTF-8 bytes.

Administrative resources are `/api/v1/users`, `/api/v1/roles`, and `/api/v1/permissions`. Authorization is checked in services with permission codes such as `user:read` and `role:update`. A superuser has `all:all`.

`PATCH /api/v1/users/{id}` updates only supplied `fullName`, `active`, `locked`, and `roleIds` fields; it does not accept a password. Roles use `PUT /api/v1/roles/{id}` for updates. See Swagger for request schemas.

Usernames contain 6â€“100 ASCII letters, digits, `_`, `.`, or `-` and are case-sensitive. New email addresses are trimmed and lowercased. Passwords require 12â€“64 characters and at most 72 UTF-8 bytes; passwords are not trimmed. Pagination defaults to 20 items, is capped at 100, and allows only supported sort fields.

## Docker

### Run the complete local stack

After configuring `.env` and `JWT_SECRET`:

```bash
docker compose --profile app up --build -d
```

The `app` profile adds the application image to PostgreSQL and Mailpit. Container-to-container database and SMTP addresses are configured automatically. The application runs as a non-root user, uses the `prod` profile, and exposes only its health endpoint through Actuator. Swagger is disabled in this profile. All published ports bind to `127.0.0.1`.

```bash
docker compose --profile app logs -f app
docker compose --profile app down
```

Database data is kept in the named volume when stopping the stack. Adding `--volumes` deletes that local database, so use it only for disposable data.

Compose is a **local development/demo stack**, even though the application uses its production profile. It captures email in Mailpit and includes development database defaults. Use deployment-specific configuration, real SMTP, HTTPS, and secret management for a public service.

### Build and run the image separately

```bash
docker build -t spring-auth-starter:local .
docker run --rm --name spring-auth-starter \
  --env-file /path/to/production.env \
  -p 127.0.0.1:8080:8080 spring-auth-starter:local
```

The image defaults to `prod`. Supply the required production variables below. `localhost` inside a container is the container itself; use database and SMTP hosts reachable from that container. On Docker Desktop, `host.docker.internal` can reach host services.

The build uses the committed Gradle Wrapper with its distribution checksum. Only production sources and build inputs enter the build context. Environment files and local keys are excluded; secrets are supplied at runtime. Tests are run by CI before the image smoke test, not inside `docker build`.

For a separate Compose environment file, set `APP_ENV_FILE` to its path and pass that path to `docker compose --env-file`. This keeps local `.env` credentials separate from verification environments.

## Configuration

See [.env.example](.env.example) for the complete local configuration.

| Variable | Purpose / default |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` locally; `prod` in the image |
| `JWT_SECRET` | Required signing key; no default |
| `JWT_ACCESS_EXPIRATION` | Access-token lifetime; `900000` ms |
| `JWT_REFRESH_EXPIRATION` | Refresh-token lifetime; `604800000` ms |
| `PASSWORD_RESET_EXPIRATION` | Recovery-token lifetime; `900000` ms |
| `PASSWORD_RESET_RESEND_COOLDOWN` | Per-account email cooldown; `60000` ms |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Database credentials |
| `APP_PUBLIC_BASE_URL` | Public application URL used for email links |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM` | SMTP host, port, and sender |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP credentials, if required |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | Provider-specific SMTP settings |
| `CLOUDINARY_ENABLED` | Optional avatar uploads; `false` by default |
| `PORT` | Application host port; `8080` |
| `POSTGRES_PORT`, `MAILPIT_SMTP_PORT`, `MAILPIT_UI_PORT` | Compose host ports; `5432`, `1025`, `8025` |

- **dev:** default profile; reads `.env` using Spring Config Import and enables Swagger.
- **test:** isolated PostgreSQL Testcontainers and mocked SMTP; does not read `.env`.
- **prod:** reads deployment environment variables, does not import `.env`, and disables Swagger. Set the database variables, `JWT_SECRET`, `APP_PUBLIC_BASE_URL`, `CORS_ALLOWED_ORIGINS`, and `MAIL_FROM` explicitly.

Cloudinary is optional. Enable it and supply the `CLOUDINARY_*` settings to upload avatars; otherwise the endpoint returns 503.

## Database migrations

Flyway owns the schema; Hibernate uses `ddl-auto: validate`.

`V1__init_schema.sql` creates the complete initial schema: users, roles, permissions, audit logs, authentication sessions, refresh/activation/reset tokens, foreign keys, and indexes. Future schema changes should start with V2. Do not edit an initial migration after deploying it.

This consolidated V1 is intended for a **fresh database**. Databases initialized with the earlier development migrations V1–V4 have a different Flyway history and V1 checksum. They cannot adopt this file unchanged. Use a new database for a fresh template installation; preserve and plan migration of existing data separately. Do not run Flyway repair or delete data just to bypass validation.

## Testing and CI

With Docker running:

```bash
bash ./gradlew spotlessCheck build
```

```powershell
.\gradlew.bat spotlessCheck build
```

Use `spotlessApply` to apply Java formatting. Test reports are written to `build/reports/tests/test/index.html`. Integration tests use real PostgreSQL, including concurrent refresh/reset and session-revocation scenarios. Missing Docker causes a failure rather than silently skipping these tests.

The [CI workflow](.github/workflows/ci.yml) runs on pushes, pull requests, and manual dispatch:

1. Set up Java 21, validate the Gradle Wrapper, check formatting, run tests, and package the application.
2. Build the Docker image and start an isolated Compose stack with a generated test secret.
3. Run inline HTTP/SMTP checks in the workflow: health, production defaults, registration, activation/recovery emails, password reset, and immediate session revocation. Verify non-root execution.
4. Upload test reports and remove the disposable Compose stack.

Actions are pinned to commit SHAs. CI needs no application credentials and does not publish images or deploy the application. After pushing to your own repository, confirm both jobs pass and make them required checks for your default branch.

## Project layout

```text
src/main/java/org/app/backend/
  common/        Configuration properties, validation, and API responses
  configs/       Spring Security, application setup, and bootstrap data
  exception/     API and authentication error handling
  filter/        JWT authentication and request logging
  modules/
    auth/        Login, recovery, sessions, and token persistence
    user/        User management
    role/        Roles and permission assignments
    permission/  Permission lookup
    log/         Audit events
    utils/       JWT, mail, and Cloudinary helpers
src/main/resources/
  db/migration/  Flyway migrations
  templates/     Activation and password-reset pages
src/test/        Configuration and PostgreSQL integration tests
```

## Using this starter

Use GitHub's **Use this template** feature once the repository owner enables **Template repository** in Settings, or clone the repository and create your own remote. Update the project name, Java package, application metadata, and permission bootstrap for your product. Keep secrets out of version control and retain the MIT copyright/license notice when redistributing the starter.

Read [SECURITY.md](SECURITY.md) before exposing a deployment publicly or reporting a vulnerability.
