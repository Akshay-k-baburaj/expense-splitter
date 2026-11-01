# Expense Splitter — Backend

Spring Boot REST API for shared groups, expenses, balances, and recorded settlements. The React frontend is maintained separately in [expense-splitter-frontend](https://github.com/Akshay-k-baburaj/expense-splitter-frontend).

## Production deployment

- API: <https://expense-splitter-4frk.onrender.com>
- Database: PostgreSQL on Neon (Singapore)
- Hosting: Render Docker Web Service
- Schema: Flyway migrations run automatically when the application starts; Hibernate validates the schema.
- CI: GitHub Actions runs `./mvnw --batch-mode verify` for pushes and pull requests.

The current Render service uses the free plan. Free services sleep after inactivity, so the first request after a sleep can take a while.

## Features

- Auth0 access-token validation (signature, issuer, expiry, and audience); all API endpoints require authentication.
- User profile setup for the authenticated identity, requiring a verified email claim.
- Groups with ownership and membership controls, six-digit invite codes, and protected group access.
- One ISO currency per group; equal splits and custom per-member amounts.
- Expense history, calculated member balances, suggested payments, and settlement records. Settlements record payments already made; the API does not transfer funds.
- PostgreSQL persistence, validation, and versioned Flyway schema migrations.

Equal-split remainder minor units are assigned in ascending profile-ID order. Custom shares must sum exactly to the expense total. FX conversion is not supported.

## Stack

- Java 21
- Spring Boot (version is defined in `pom.xml`)
- Spring Security OAuth2 Resource Server
- Spring Data JPA / Hibernate
- PostgreSQL and Flyway
- Maven

## Run locally (Windows PowerShell)

Prerequisites: Java 21, PostgreSQL, and an Auth0 tenant with the API configured. Create a local database named `expense_splitter_db`, then set the environment values in the PowerShell window where you will run the backend:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/expense_splitter_db"
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "<your-local-postgres-password>"
$env:AUTH0_ISSUER_URI = "https://<your-auth0-domain>/"
$env:AUTH0_JWKS_URI = "https://<your-auth0-domain>/.well-known/jwks.json"
$env:AUTH0_AUDIENCE = "https://expense-splitter-api"
$env:APP_CORS_ALLOWED_ORIGINS = "http://localhost:5173"

.\mvnw.cmd spring-boot:run
```

Keep the database username and password in their own variables. **Do not put credentials in `DATABASE_URL`**: Hibernate may include the JDBC URL in startup logs. Do not commit local environment files. The application uses Flyway to create/update the schema and `ddl-auto=validate` to check entity mappings.

The API listens on port `8080` locally (or the `PORT` provided by the host). The frontend dev server normally runs on `http://localhost:5173`.

## Configuration

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://<host>/<database>?sslmode=require`; omit user/password parameters. |
| `DATABASE_USERNAME` | PostgreSQL role name. |
| `DATABASE_PASSWORD` | PostgreSQL role password; keep secret. |
| `AUTH0_ISSUER_URI` | Auth0 issuer URL, including the trailing slash. |
| `AUTH0_JWKS_URI` | Auth0 signing-key endpoint: `https://<domain>/.well-known/jwks.json`. |
| `AUTH0_AUDIENCE` | Identifier of the Auth0 API. |
| `APP_CORS_ALLOWED_ORIGINS` | Exact comma-separated browser origins allowed to call the API; no wildcard. |
| `AUTH0_EMAIL_CLAIM` | Optional email claim name; defaults to `https://expense-splitter.example/claims/email`. |
| `AUTH0_EMAIL_VERIFIED_CLAIM` | Optional verified-email claim name; defaults to `https://expense-splitter.example/claims/email_verified`. |
| `PORT` | HTTP port supplied by the hosting platform; defaults to `8080`. |

Production values are configured in Render’s Environment settings. The current frontend origin is `https://expense-splitter-frontend-xi.vercel.app`; set `APP_CORS_ALLOWED_ORIGINS` to that exact origin. When changing frontend domains, update both this value and the Auth0 SPA URL allowlists.

Auth0 must issue the namespaced email and verified-email claims used by the backend. See [SECURITY_SETUP.md](SECURITY_SETUP.md) for the Auth0 Post Login Action, identity-linking behavior, and security details. Never put Auth0 client secrets or database credentials in the frontend.

## API routes

Every route requires `Authorization: Bearer <Auth0 access token>`. Group and financial routes are restricted to group members, with owner-only operations enforced by the backend.

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` / `POST` / `PUT` / `DELETE` | `/api/v1/users/me` | Read, create/setup, update, or deactivate the signed-in user’s profile. |
| `POST` | `/api/v1/groups` | Create a group. |
| `GET` | `/api/v1/groups/me` | List the signed-in user’s groups. |
| `GET` / `PUT` / `DELETE` | `/api/v1/groups/{id}` | Read, update, or delete a group. |
| `GET` / `POST` | `/api/v1/groups/{groupId}/members` | List members or add a member. |
| `DELETE` | `/api/v1/groups/{groupId}/members/{userId}` | Remove a member, subject to balance rules. |
| `PUT` | `/api/v1/groups/{groupId}/owner` | Transfer group ownership. |
| `GET` / `POST` | `/api/v1/groups/{groupId}/invite-code` | View invite-code status or generate/replace a code. The plain code is returned when generated. |
| `POST` | `/api/v1/groups/join-by-code` | Join a group using an invite code. |
| `GET` / `POST` | `/api/v1/groups/{groupId}/expenses` | List or create expenses. |
| `PUT` / `DELETE` | `/api/v1/groups/{groupId}/expenses/{expenseId}` | Update or delete an expense. |
| `GET` | `/api/v1/groups/{groupId}/balances` | Get group member balances and suggested payments. |
| `GET` / `POST` | `/api/v1/groups/{groupId}/settlements` | List or record a payment that has already happened. |
| `DELETE` | `/api/v1/groups/{groupId}/settlements/{settlementId}` | Remove a settlement recorded by the signed-in member. |

## Database migrations

Flyway migrations are in `src/main/resources/db/migration`. Add a new migration for schema changes; do not edit migrations that have already been applied to a shared database. Current migrations are `V1` through `V5`.

## Docker

The root `Dockerfile` builds the application with Maven and runs it on Java 21. Render uses this Dockerfile from the repository root. Set the environment variables above in the hosting dashboard; no secrets are baked into the image.

## CI

GitHub Actions verifies the backend on pushes and pull requests using Java 21 and a temporary PostgreSQL service:

```bash
./mvnw --batch-mode verify
```

On Windows, use `.\mvnw.cmd --batch-mode verify` in PowerShell. Render is configured to deploy on commits; its auto-deploy is not currently gated on the GitHub Actions result.
