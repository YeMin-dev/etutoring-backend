# API Documentation

This document covers all public API controllers.

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
  - `POST /api/auth/forgot-password`
  - `POST /api/auth/reset-password`

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

### POST `/api/auth/forgot-password`
Request a password reset link for the given email. If an account exists, an email with a reset link is sent; the response is always the same so that the endpoint does not reveal whether the email is registered.

- Auth: Public
- Status: `200 OK`

Request body:

```json
{
  "email": "user@example.com"
}
```

Success: No response body. A reset email is sent when the email is associated with an active account. The link in the email expires in 15 minutes.

Common errors:
- `400 VALIDATION_ERROR` (e.g. invalid or missing email)

### POST `/api/auth/reset-password`
Set a new password using a valid reset token (from the forgot-password email link).

- Auth: Public
- Status: `200 OK`

Request body:

```json
{
  "token": "<token-from-email>",
  "newPassword": "NewSecurePassword123"
}
```

Notes:
- `newPassword` must be at least 8 characters.

Success: No response body. The user can then log in with the new password.

Common errors:
- `400 VALIDATION_ERROR` (e.g. token blank, password too short)
- `400 INVALID_OR_EXPIRED_TOKEN` — token not found or expired

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

## AdminUserController (User management)
Base path: `/api/admin`

All endpoints below require **ADMIN** role. List and CRUD operations on users are admin-only. Students and tutors are listed via separate paged endpoints.

### GET `/api/admin/users/students`
Paged list of users with role **STUDENT** (non-deleted only).

- Auth: Required (ADMIN)
- Status: `200 OK`

Query params:
- `page` (optional): 0-based page index; default `0`.
- `size` (optional): Page size; default `20`, max `100`.

Success response: JSON object with pagination metadata and `content` array of user objects (same shape as single user below).

Common errors:
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### GET `/api/admin/users/tutors`
Paged list of users with role **TUTOR** (non-deleted only).

- Auth: Required (ADMIN)
- Status: `200 OK`

Query params: same as `GET /api/admin/users/students`.

Success response: same paginated shape as students.

Common errors:
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### GET `/api/admin/admin-user`
Get the single admin user (non-deleted). At most one admin user exists in the system.

- Auth: Required (ADMIN)
- Status: `200 OK`

Success response: same shape as `GET /api/admin/users/{id}` (single user object).

Common errors:
- `404 ADMIN_NOT_FOUND` (no admin user exists)
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### GET `/api/admin/users/{id}`
Get one user by ID (non-deleted only).

- Auth: Required (ADMIN)
- Status: `200 OK`

Path param: `id` (UUID)

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
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### POST `/api/admin/users`
Create a new user (admin only).

- Auth: Required (ADMIN)
- Status: `201 Created`

Request body:

```json
{
  "username": "tutor1",
  "firstName": "Tom",
  "lastName": "Tutor",
  "email": "tutor1@example.com",
  "password": "Password123",
  "role": "TUTOR",
  "isActive": true,
  "isLocked": false
}
```

- `role` is required (`ADMIN`, `TUTOR`, or `STUDENT`). `isActive` and `isLocked` are optional (default true and false).
- Only one admin user is allowed: creating with `role: ADMIN` fails if an admin already exists.

Success response: same shape as `GET /api/admin/users/{id}`.

Common errors:
- `400 VALIDATION_ERROR`
- `400 ONLY_ONE_ADMIN_ALLOWED` (creating with role ADMIN when an admin already exists)
- `409 DUPLICATE_USERNAME`
- `409 DUPLICATE_EMAIL`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### PUT `/api/admin/users/{id}`
Update user fields.

- Auth: Required (ADMIN)
- Status: `200 OK`

Path param: `id` (UUID)

Request body (all fields optional): same as before (username, firstName, lastName, email, password, role, isActive, isLocked). Only one admin is allowed: updating a user's role to ADMIN fails if another admin already exists.

Success response: same shape as `GET /api/admin/users/{id}`.

Common errors:
- `400 VALIDATION_ERROR`
- `400 ONLY_ONE_ADMIN_ALLOWED` (setting role to ADMIN when another admin already exists)
- `404 USER_NOT_FOUND`
- `409 DUPLICATE_USERNAME`
- `409 DUPLICATE_EMAIL`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### DELETE `/api/admin/users/{id}`
Soft delete user (`deleted_date` is set).

- Auth: Required (ADMIN)
- Status: `204 No Content`

Path param: `id` (UUID)

Common errors:
- `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## AdminTutorAllocationController
Base path: `/api/admin`

All endpoints require **ADMIN** role. Used to allocate students to tutors (single or bulk). `scheduleStart` and `scheduleEnd` are required; the API rejects the request if the tutor already has an active allocation whose schedule overlaps. Creating an allocation (single or bulk) triggers email notifications to the student and tutor.

### GET `/api/admin/allocations`
List allocations with pagination and optional search by tutor or student name. Returns **only active allocations** (allocations that have not been undone; i.e. `endedDate` is null). Ended allocations are excluded from the list.

- Auth: Required (ADMIN)
- Status: `200 OK`

Query params:
- `page` (optional): 0-based page index; default `0`.
- `size` (optional): Page size; default `20`, max `100`.
- `search` (optional): Case-insensitive substring match on tutor full name or student full name (firstName + lastName).

Success response: JSON object with pagination metadata and flat list of allocation items.

```json
{
  "content": [
    {
      "id": "4c74ddc1-4e3h-6b1d-df8e-9d8g997946ef",
      "studentUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
      "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
      "allocatedById": "1a41aab8-1b1e-38a8-ac5b-6a5d664613bd",
      "allocatedDate": "04/03/2026 09:15",
      "endedDate": null,
      "reason": "Math support",
      "scheduleStart": "10/03/2026 09:00",
      "scheduleEnd": "10/03/2026 10:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

Common errors:
- `401 UNAUTHORIZED`

### POST `/api/admin/allocations`
Create a single student–tutor allocation.

- Auth: Required (ADMIN)
- Status: `201 Created`

Request body:

```json
{
  "studentUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
  "reason": "Math support",
  "scheduleStart": "2026-03-10T09:00:00Z",
  "scheduleEnd": "2026-03-10T10:00:00Z"
}
```

Notes:
- `studentUserId`, `tutorUserId`, `scheduleStart`, `scheduleEnd` are required (UUIDs for IDs; ISO-8601 for dates).
- `reason` is optional.
- `scheduleEnd` must be ≥ `scheduleStart`.
- `allocated_by_id` is set to the current admin user.

Success response:

```json
{
  "id": "4c74ddc1-4e3h-6b1d-df8e-9d8g997946ef",
  "studentUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
  "allocatedById": "1a41aab8-1b1e-38a8-ac5b-6a5d664613bd",
  "allocatedDate": "04/03/2026 09:15",
  "endedDate": null,
  "reason": "Math support",
  "scheduleStart": "10/03/2026 09:00",
  "scheduleEnd": "10/03/2026 10:00"
}
```

Common errors:
- `400 VALIDATION_ERROR`
- `400 INVALID_STUDENT` (user is not a student)
- `400 INVALID_TUTOR` (user is not a tutor)
- `400 INVALID_SCHEDULE` (e.g. scheduleEnd before scheduleStart)
- `400 SCHEDULE_OVERLAP` (tutor already allocated for this schedule)
- `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED`

### POST `/api/admin/allocations/bulk`
Create multiple student–tutor allocations in one request. Validations apply per item; fails on first validation error (fail-fast).

- Auth: Required (ADMIN)
- Status: `200 OK`

Request body:

```json
{
  "items": [
    {
      "studentUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
      "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
      "reason": "Math support",
      "scheduleStart": "2026-03-10T09:00:00Z",
      "scheduleEnd": "2026-03-10T10:00:00Z"
    },
    {
      "studentUserId": "5d85eec2-5f4i-7c2e-eg9f-0e9h008057fg",
      "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
      "reason": "Physics",
      "scheduleStart": "2026-03-11T14:00:00Z",
      "scheduleEnd": "2026-03-11T15:00:00Z"
    }
  ]
}
```

Notes:
- `items` is required; max 500 entries. Each item has the same shape as the single allocation request.

Success response: array of allocation objects (same shape as single `POST /api/admin/allocations` response).

```json
[
  {
    "id": "...",
    "studentUserId": "...",
    "tutorUserId": "...",
    "allocatedById": "...",
    "allocatedDate": "...",
    "endedDate": null,
    "reason": "Math support",
    "scheduleStart": "10/03/2026 09:00",
    "scheduleEnd": "10/03/2026 10:00"
  },
  {
    "id": "...",
    "studentUserId": "...",
    "tutorUserId": "...",
    "allocatedById": "...",
    "allocatedDate": "...",
    "endedDate": null,
    "reason": "Physics",
    "scheduleStart": "11/03/2026 14:00",
    "scheduleEnd": "11/03/2026 15:00"
  }
]
```

Common errors: same as single allocation (per first failing item).

### POST `/api/admin/allocations/{id}/undo`
End an allocation (set `ended_date` to now). Only active allocations can be undone. Once ended, the same student, tutor, and schedule can be allocated again via POST create.

- Auth: Required (ADMIN)
- Status: `200 OK`

Path param:
- `id` (UUID): allocation id

Success response: allocation object (same shape as create response) with `endedDate` set to the undo time.

Common errors:
- `404 ALLOCATION_NOT_FOUND`
- `400 ALLOCATION_ALREADY_ENDED` (allocation is already ended)
- `401 UNAUTHORIZED`

### PUT `/api/admin/allocations/{id}`
Update an existing allocation (reallocation). Only active allocations can be updated. At least one field must be provided. Schedule overlap is checked excluding the current allocation.

- Auth: Required (ADMIN)
- Status: `200 OK`

Path param:
- `id` (UUID): allocation id

Request body (all fields optional; at least one required):

```json
{
  "reason": "Updated reason",
  "scheduleStart": "2026-03-12T09:00:00Z",
  "scheduleEnd": "2026-03-12T10:00:00Z",
  "studentUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de"
}
```

Notes:
- If `scheduleStart` or `scheduleEnd` is set, both must be set; `scheduleEnd` must be ≥ `scheduleStart`.
- If `studentUserId` or `tutorUserId` is set, user must exist and have role STUDENT or TUTOR respectively.
- When schedule or tutor is changed, overlap is checked for the (possibly new) tutor excluding this allocation.

Success response: allocation object (same shape as create response) with updated fields.

Common errors:
- `400 NO_UPDATE_FIELDS` (no fields provided)
- `400 INVALID_SCHEDULE` (partial schedule or scheduleEnd before scheduleStart)
- `400 INVALID_STUDENT` / `400 INVALID_TUTOR`
- `400 SCHEDULE_OVERLAP`
- `400 CANNOT_UPDATE_ENDED_ALLOCATION` (allocation already ended)
- `404 ALLOCATION_NOT_FOUND` / `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED`

---

## TutorMeetingController (Meetings)
Base path: `/api/tutor`

All endpoints require **TUTOR** or **ADMIN** role. Only the tutor who owns a meeting can list, get, update, or delete it (list returns meetings where the authenticated user is the tutor). Creating a meeting sends an email to the student with meeting details. Only tutors can create meetings.

### GET `/api/tutor/meetings`
Paged list of meetings for the authenticated tutor (meetings where tutor_user_id is the current user), sorted by startDate descending.

- Auth: Required (TUTOR or ADMIN)
- Status: `200 OK`

Query params:
- `page` (optional): 0-based page index; default `0`.
- `size` (optional): Page size; default `20`, max `100`.

Success response: JSON object with pagination metadata and `content` array of meeting objects (same shape as single meeting below).

Common errors:
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### GET `/api/tutor/meetings/{id}`
Get one meeting by ID. Only allowed if the meeting’s tutor is the authenticated user.

- Auth: Required (TUTOR or ADMIN)
- Status: `200 OK`

Path param: `id` (UUID)

Success response:

```json
{
  "id": "7d84ffd2-6g5j-8d3e-fh0g-1f0i119168gh",
  "studentUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
  "createdById": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
  "startDate": "15/03/2026 10:00",
  "endDate": "15/03/2026 11:00",
  "mode": "VIRTUAL",
  "location": null,
  "link": "https://meet.example.com/abc",
  "description": "Math revision",
  "createdDate": "04/03/2026 09:20",
  "updatedDate": null
}
```

Common errors:
- `404 MEETING_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### POST `/api/tutor/meetings`
Create a meeting. The authenticated user must be a TUTOR; they become the tutor and creator. An email is sent to the student with meeting details.

- Auth: Required (TUTOR or ADMIN; only TUTOR can create)
- Status: `201 Created`

Request body:

```json
{
  "studentUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "startDate": "2026-03-15T10:00:00Z",
  "endDate": "2026-03-15T11:00:00Z",
  "mode": "VIRTUAL",
  "location": null,
  "link": "https://meet.example.com/abc",
  "description": "Math revision"
}
```

- `studentUserId`, `startDate`, `endDate`, `mode` are required. `mode` must be `IN_PERSON` or `VIRTUAL`. `location`, `link`, `description` are optional.

Success response: same shape as `GET /api/tutor/meetings/{id}`.

Common errors:
- `400 VALIDATION_ERROR`
- `400 ONLY_TUTORS_CAN_ARRANGE` (non-tutor user attempts to create)
- `400 INVALID_SCHEDULE` (endDate before startDate)
- `400 INVALID_STUDENT` (user is not a student)
- `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### PUT `/api/tutor/meetings/{id}`
Update a meeting. Only the meeting’s tutor can update. All request body fields are optional (partial update).

- Auth: Required (TUTOR or ADMIN)
- Status: `200 OK`

Path param: `id` (UUID)

Request body (all optional):

```json
{
  "startDate": "2026-03-16T10:00:00Z",
  "endDate": "2026-03-16T11:00:00Z",
  "mode": "IN_PERSON",
  "location": "Room 101",
  "link": null,
  "description": "Updated description"
}
```

- If both `startDate` and `endDate` are present, `endDate` must be ≥ `startDate`.

Success response: same shape as `GET /api/tutor/meetings/{id}`.

Common errors:
- `400 VALIDATION_ERROR`
- `400 INVALID_SCHEDULE` (endDate before startDate after update)
- `404 MEETING_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### DELETE `/api/tutor/meetings/{id}`
Delete a meeting. Only the meeting’s tutor can delete.

- Auth: Required (TUTOR or ADMIN)
- Status: `204 No Content`

Path param: `id` (UUID)

Common errors:
- `404 MEETING_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## Global Error Codes (Current)
- `VALIDATION_ERROR` -> `400`
- `DATA_INTEGRITY_ERROR` -> `400`
- `INVALID_STUDENT` -> `400` (allocation: user is not a student)
- `INVALID_TUTOR` -> `400` (allocation: user is not a tutor)
- `INVALID_SCHEDULE` -> `400` (allocation: e.g. scheduleEnd before scheduleStart)
- `SCHEDULE_OVERLAP` -> `400` (allocation: tutor already allocated for this schedule)
- `AUTHENTICATION_FAILED` -> `401`
- `UNAUTHORIZED` -> `401`
- `USER_NOT_FOUND` -> `404`
- `ADMIN_NOT_FOUND` -> `404` (GET admin-user: no admin user exists)
- `ONLY_ONE_ADMIN_ALLOWED` -> `400` (create/update user with role ADMIN when an admin already exists)
- `MEETING_NOT_FOUND` -> `404` (meetings: meeting not found or not owned by current tutor)
- `ONLY_TUTORS_CAN_ARRANGE` -> `400` (meetings: only tutors can create meetings)
- `ALLOCATION_NOT_FOUND` -> `404`
- `ALLOCATION_ALREADY_ENDED` -> `400` (undo: allocation already ended)
- `CANNOT_UPDATE_ENDED_ALLOCATION` -> `400` (PUT: cannot update ended allocation)
- `NO_UPDATE_FIELDS` -> `400` (PUT: at least one field required)
- `INVALID_OR_EXPIRED_TOKEN` -> `400` (reset-password: token invalid or expired)
- `DUPLICATE_USERNAME` -> `409`
- `DUPLICATE_EMAIL` -> `409`
- `INTERNAL_SERVER_ERROR` -> `500`
