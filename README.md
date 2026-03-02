# eTutoring Backend

Spring Boot backend for eTutoring.

## Project Spec
- Framework: Spring Boot `4.0.3`
- Java: `21`
- Build: Maven
- Database: PostgreSQL + Flyway
- Auth: JWT stateless auth with DB-backed users
- Base package: `com.a9.etutoring`
- Architecture: layered (`controller -> service -> repository -> domain`)
- API style: DTO-first
- Soft delete: `deleted_date`
- Timestamp fields: `Instant` mapped to `TIMESTAMPTZ`
- Global error response format:
  ```json
  { "code": "...", "message": "..." }
  ```
- Date response format for API: `dd/MM/yyyy HH:mm` (UTC)

## Project Structure
```text
.
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/a9/etutoring
│   │   │   ├── config
│   │   │   ├── controller
│   │   │   ├── domain
│   │   │   │   ├── dto
│   │   │   │   ├── enums
│   │   │   │   └── model
│   │   │   ├── exception
│   │   │   ├── repository
│   │   │   ├── security
│   │   │   ├── service
│   │   │   └── util
│   │   └── resources
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── db/migration/V1__init_schema.sql
│   └── test
│       ├── java/com/a9/etutoring
│       └── resources
└── .env.example
```

## Prerequisites
- JDK 21
- Docker + Docker Compose
- (Manual run only) Maven wrapper `./mvnw`

## Environment Variables
Create `.env` at project root:

```env
JWT_SECRET=replace-with-long-random-secret
```

You can copy from template:

```bash
cp .env.example .env
```

## Run with Docker Compose (Recommended)
This runs both `backend` and `postgres`.

1. Ensure `.env` exists with `JWT_SECRET`.
2. Start services:
   ```bash
   docker compose up -d --build
   ```
3. Check logs:
   ```bash
   docker compose logs -f backend
   docker compose logs -f postgres
   ```
4. Stop services:
   ```bash
   docker compose down
   ```

If PostgreSQL fails after image/version changes, reset volumes:

```bash
docker compose down -v
```

Then run `docker compose up -d --build` again.

## Run Manually (IDE or CLI)

### 1) Start PostgreSQL only
Use compose for DB:

```bash
docker compose up -d postgres
```

### 2) Set env var for JWT secret
In terminal:

```bash
export JWT_SECRET=replace-with-long-random-secret
```

In IDE (Run Configuration), set env var:
- `JWT_SECRET=...`

### 3) Run backend
From CLI:

```bash
./mvnw spring-boot:run
```

Or run `EtutoringApplication` from IDE.

The app uses `application-dev.properties` by default and connects to:
- DB: `jdbc:postgresql://localhost:5432/eTutoring`
- User: `eTutoring`
- Password: `eTutoring_pw`

## API Base URL
- Local: `http://localhost:8080`

Public auth endpoints:
- `POST /api/auth/signup`
- `POST /api/auth/login`

Authenticated endpoint:
- `GET /api/me`

## Build Docker Image Manually

```bash
docker build -t <dockerhub-username>/etutoring-backend:1.0.0 .
```

Push:

```bash
docker login
docker push <dockerhub-username>/etutoring-backend:1.0.0
```

## Notes
- Do not commit `.env`.
- Commit `.env.example` only.
- Flyway migration file is at `src/main/resources/db/migration/V1__init_schema.sql`.
