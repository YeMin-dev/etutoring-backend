# API Documentation

This document covers all public API controllers except `RoleProtectedController`.

## Base URL
- `http://localhost:8080`

## Conventions
- Content type: `application/json`
- Error response format:
  ```json
  { "code": "...", "message": "..." }
  ```
- Date-time response format (for `Instant` fields): `dd/MM/yyyy HH:mm` (UTC)

## Authentication
- JWT bearer auth is required for all endpoints except:
  - `POST /api/auth/signup`
  - `POST /api/auth/login`

Add header for protected endpoints:

```http
Authorization: Bearer <accessToken>
```

---

## AuthController
Base path: `/api/auth`

### POST `/api/auth/signup`
Create a new user account and return access token.

- Auth: Public
- Status: `201 Created`

Request body:

```json
{
  "username": "student1",
  "firstName": "Stu",
  "lastName": "Dent",
  "email": "student1@example.com",
  "password": "Password123",
  "role": "STUDENT"
}
```

Notes:
- `role` is optional; defaults to `STUDENT`.
- Allowed roles: `ADMIN`, `TUTOR`, `STUDENT`.

Success response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "id": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "username": "student1",
  "role": "STUDENT"
}
```

Common errors:
- `400 VALIDATION_ERROR`
- `409 DUPLICATE_USERNAME`
- `409 DUPLICATE_EMAIL`

### POST `/api/auth/login`
Authenticate with username/password and return access token.

- Auth: Public
- Status: `200 OK`

Request body:

```json
{
  "username": "student1",
  "password": "Password123"
}
```

Success response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "id": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "username": "student1",
  "role": "STUDENT"
}
```

Common errors:
- `401 AUTHENTICATION_FAILED` (invalid credentials)
- `401 AUTHENTICATION_FAILED` with message like:
  - `User <username> does not exist. Please sign up.`
- `400 VALIDATION_ERROR`

---

## MeController

### GET `/api/me`
Return current authenticated user profile.

- Auth: Required
- Status: `200 OK`

Success response:

```json
{
  "id": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "username": "student1",
  "firstName": "Stu",
  "lastName": "Dent",
  "email": "student1@example.com",
  "role": "STUDENT",
  "isActive": true,
  "isLocked": false,
  "lastLoginDate": "02/03/2026 01:12"
}
```

Common errors:
- `401 UNAUTHORIZED`

---

## UserController
Base path: `/api/v1/users`

> `POST /api/v1/users` is intentionally not available. User creation is handled via signup.

### GET `/api/v1/users/{id}`
Get user by ID (non-deleted only).

- Auth: Required
- Status: `200 OK`

Path param:
- `id` (UUID)

Success response:

```json
{
  "id": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "role": "STUDENT",
  "username": "student1",
  "firstName": "Stu",
  "lastName": "Dent",
  "email": "student1@example.com",
  "isActive": true,
  "isLocked": false,
  "createdDate": "02/03/2026 08:30",
  "updatedDate": "02/03/2026 09:10",
  "lastLoginDate": "02/03/2026 10:05"
}
```

Common errors:
- `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED`

### GET `/api/v1/users`
List users (non-deleted only).

- Auth: Required
- Status: `200 OK`

Success response:

```json
[
  {
    "id": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
    "role": "STUDENT",
    "username": "student1",
    "firstName": "Stu",
    "lastName": "Dent",
    "email": "student1@example.com",
    "isActive": true,
    "isLocked": false,
    "createdDate": "02/03/2026 08:30",
    "updatedDate": "02/03/2026 09:10",
    "lastLoginDate": "02/03/2026 10:05"
  }
]
```

Common errors:
- `401 UNAUTHORIZED`

### PUT `/api/v1/users/{id}`
Update user fields.

- Auth: Required
- Status: `200 OK`

Path param:
- `id` (UUID)

Request body (all fields optional):

```json
{
  "username": "student1",
  "firstName": "Stu",
  "lastName": "Dent",
  "email": "student1@example.com",
  "password": "NewPassword123",
  "role": "STUDENT",
  "isActive": true,
  "isLocked": false
}
```

Success response:
- Same shape as `GET /api/v1/users/{id}`.

Common errors:
- `400 VALIDATION_ERROR`
- `404 USER_NOT_FOUND`
- `409 DUPLICATE_USERNAME`
- `409 DUPLICATE_EMAIL`
- `401 UNAUTHORIZED`

### DELETE `/api/v1/users/{id}`
Soft delete user (`deleted_date` is set; row is not physically removed).

- Auth: Required
- Status: `204 No Content`

Path param:
- `id` (UUID)

Common errors:
- `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED`

---

## Global Error Codes (Current)
- `VALIDATION_ERROR` -> `400`
- `DATA_INTEGRITY_ERROR` -> `400`
- `AUTHENTICATION_FAILED` -> `401`
- `UNAUTHORIZED` -> `401`
- `USER_NOT_FOUND` -> `404`
- `DUPLICATE_USERNAME` -> `409`
- `DUPLICATE_EMAIL` -> `409`
- `INTERNAL_SERVER_ERROR` -> `500`
