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
- Date-time response format (for `Instant` fields): `dd/MM/yyyy HH:mm` in **`app.default-time-zone`** (default `Asia/Yangon`; env `APP_DEFAULT_TIME_ZONE`)

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
  "role": "STUDENT",
  "previousLoginAt": null
}
```

- `previousLoginAt` is always `null` on signup (first session token).

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
  "role": "STUDENT",
  "previousLoginAt": "15/03/2026 14:30"
}
```

- `previousLoginAt`: **Instant** of the last successful login **before** this one (same `dd/MM/yyyy HH:mm` formatting as other instants, in `app.default-time-zone`). **`null`** means there was no prior login (show a first-time welcome). After this response, the server stores the current login time as the new last login.

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

### POST `/api/admin/allocations/preview`

Preview bulk allocation schedule without saving. Accepts a date, slot duration, tutor, list of students, and optional time zone; computes `scheduleStart` and `scheduleEnd` for each student using 9am–5pm with lunch 12pm–1pm excluded. Only **future** slots are returned (e.g. if it is 9:40, the first slot is 10:00). Response times are in the requested time zone (ISO-8601 with offset). The response can be sent to `POST /api/admin/allocations/bulk` (schedule strings parse as `Instant`).

- Auth: Required (ADMIN)
- Status: `200 OK`

Request body:

```json
{
  "date": "2026-03-10",
  "slotDurationMinutes": 60,
  "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
  "studentUserIds": [
    "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
    "5d85eec2-5f4i-7c2e-eg9f-0e9h008057fg"
  ],
  "reason": "Math support",
  "timeZoneId": "Asia/Yangon",
  "startTime": "10:30"
}
```

Notes:

- `date` (ISO-8601 date), `slotDurationMinutes` (positive integer), `tutorUserId`, and `studentUserIds` (non-empty, max 500) are required. `reason` is optional.
- `timeZoneId` (optional): IANA time zone ID (e.g. `Asia/Yangon`). If omitted, **`app.default-time-zone`** is used. Slots are in this zone.
- `startTime` (optional): Local time (e.g. `"10:30"` or `"10:45"`) on the given date in the request time zone. If set, slot generation starts from this time: the first slot begins at this time (e.g. 10:30–11:30). Morning slots still end by 12:00 (lunch); afternoon slots run from 13:00. If omitted, slots use the default work-day grid and only slots that start **after** the current time are returned.
- Response `scheduleStart` and `scheduleEnd` are ISO-8601 strings with offset (e.g. `2026-03-10T10:00:00+06:30`) so times display correctly for the client and can be submitted to the bulk create API as-is.

Success response: `items` with `studentUserId`, `tutorUserId`, `reason`, `scheduleStart`, `scheduleEnd` per item (strings with offset).

```json
{
  "items": [
    {
      "studentUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
      "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
      "reason": "Math support",
      "scheduleStart": "2026-03-10T10:00:00+06:30",
      "scheduleEnd": "2026-03-10T11:00:00+06:30"
    },
    {
      "studentUserId": "5d85eec2-5f4i-7c2e-eg9f-0e9h008057fg",
      "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
      "reason": "Math support",
      "scheduleStart": "2026-03-10T11:00:00+06:30",
      "scheduleEnd": "2026-03-10T12:00:00+06:30"
    }
  ]
}
```

Common errors:

- `400 VALIDATION_ERROR` (invalid date, non-positive slot duration, etc.)
- `400 TOO_MANY_STUDENTS` (number of students exceeds available remaining slots for the day)
- `400 INVALID_TIMEZONE` (invalid `timeZoneId`)
- `400 INVALID_TUTOR` (user is not a tutor)
- `400 INVALID_STUDENT` (user is not a student)
- `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED`

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

## TutorAllocationController (Tutor allocations)

Base path: `/api/tutor`

All endpoints require **TUTOR** or **ADMIN** role. Allocation data is scoped to the authenticated user as the tutor.

### GET `/api/tutor/allocated-students`

List of students currently allocated to the authenticated tutor (active allocations only; `endedDate` is null). Allocations whose `scheduleEnd` has already passed are excluded. Each item includes the student (same shape as admin user) and that student’s allocation date ranges (`allocationSlots`) so the frontend can validate meeting start/end against them.

- Auth: Required (TUTOR or ADMIN)
- Status: `200 OK`

Success response: array of objects with:
- `student`: user object (same shape as `GET /api/admin/users/{id}`: `id`, `role`, `username`, `firstName`, `lastName`, `email`, `isActive`, `isLocked`, `createdDate`, `updatedDate`, `lastLoginDate`).
- `allocationSlots`: array of `{ scheduleStart, scheduleEnd }` (same date format as other allocation endpoints). Each slot is one allocation window; meeting start/end must fall within one of these ranges.

Common errors:

- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## TutorMeetingController (Meetings)

Base path: `/api/tutor`

All endpoints require **TUTOR** or **ADMIN** role. Only the tutor who owns a meeting can list, get, update, or delete it (list returns meetings where the authenticated user is the tutor). The student receives an email notification when a meeting is created, updated, or deleted (cancelled). Only tutors can create meetings.

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
  "virtualPlatform": "GOOGLE_MEET",
  "description": "Math revision",
  "createdDate": "04/03/2026 09:20",
  "updatedDate": null
}
```

Common errors:

- `404 MEETING_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### POST `/api/tutor/meetings`

Create a meeting. The authenticated user must be a TUTOR; they become the tutor and creator. An email notification is sent to the student with meeting details.

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
  "virtualPlatform": "GOOGLE_MEET",
  "description": "Math revision"
}
```

- `studentUserId`, `startDate`, `endDate`, `mode` are required. `mode` must be `IN_PERSON` or `VIRTUAL`.
- **In person (`IN_PERSON`):** `location` is required (non-blank after trim). Do not send `link` or `virtualPlatform` (or send them omitted / null only).
- **Virtual (`VIRTUAL`):** `link` is required (non-blank, must start with `http://` or `https://`). `virtualPlatform` is required. Allowed values: `ZOOM`, `MICROSOFT_TEAMS`, `GOOGLE_MEET`, `OTHER`. Do not send a non-blank `location`.
- `description` is optional for both modes.

Success response: same shape as `GET /api/tutor/meetings/{id}`.

Common errors:

- `400 VALIDATION_ERROR`
- `400 ONLY_TUTORS_CAN_ARRANGE` (non-tutor user attempts to create)
- `400 INVALID_SCHEDULE` (endDate before startDate)
- `400 INVALID_MEETING_LOCATION` (in-person without a usable location)
- `400 INVALID_MEETING_IN_PERSON` (in-person request includes link or platform)
- `400 INVALID_MEETING_LINK` (virtual without a valid http(s) link)
- `400 INVALID_MEETING_PLATFORM` (virtual without `virtualPlatform`)
- `400 INVALID_MEETING_VIRTUAL` (virtual request includes a non-blank location)
- `400 MEETING_NOT_WITHIN_ALLOCATION` (meeting window must fall inside an allocation slot for this tutor–student pair)
- `400 MEETING_OVERLAP` (tutor already has a meeting in this time range)
- `400 INVALID_STUDENT` (user is not a student)
- `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### PUT `/api/tutor/meetings/{id}`

Update a meeting. Only the meeting’s tutor can update. All request body fields are optional (partial update). An email notification is sent to the student with the updated meeting details.

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
  "virtualPlatform": null,
  "description": "Updated description"
}
```

- If both `startDate` and `endDate` are present, `endDate` must be ≥ `startDate`.
- After merging omitted fields with the existing meeting, the same mode rules as **POST** apply (in-person: location required, no link/platform; virtual: http(s) link + `virtualPlatform`, no location).

Success response: same shape as `GET /api/tutor/meetings/{id}`.

Common errors:

- `400 VALIDATION_ERROR`
- `400 INVALID_SCHEDULE` (endDate before startDate after update)
- `400 INVALID_MEETING_LOCATION` / `INVALID_MEETING_IN_PERSON` / `INVALID_MEETING_LINK` / `INVALID_MEETING_PLATFORM` / `INVALID_MEETING_VIRTUAL` (same semantics as create)
- `400 MEETING_NOT_WITHIN_ALLOCATION` (meeting window must fall inside an allocation slot for this tutor–student pair)
- `400 MEETING_OVERLAP` (tutor already has another meeting in this time range)
- `404 MEETING_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### DELETE `/api/tutor/meetings/{id}`

Delete a meeting (cancelled). Only the meeting's tutor can delete. An email notification is sent to the student that the meeting has been cancelled.

- Auth: Required (TUTOR or ADMIN)
- Status: `204 No Content`

Path param: `id` (UUID)

Common errors:

- `404 MEETING_NOT_FOUND`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## StudentAllocationController (Allocations)

Base path: `/api/student`

### GET `/api/student/allocated-tutors`

List of tutors currently allocated to the authenticated student (active allocations only; `endedDate` is null). Allocations whose `scheduleEnd` has already passed are excluded. Each item includes the tutor (same shape as admin user response) and that tutor’s allocation date ranges (`allocationSlots`) for this student.

- Auth: Required (STUDENT or ADMIN)
- Status: `200 OK`

Success response: array of objects with:
- `tutor`: user object (same shape as `GET /api/admin/users/{id}`: `id`, `role`, `username`, `firstName`, `lastName`, `email`, `isActive`, `isLocked`, `createdDate`, `updatedDate`, `lastLoginDate`).
- `allocationSlots`: array of `{ scheduleStart, scheduleEnd }` (same date format as other allocation endpoints). Each slot is one allocation window for this student–tutor pair.

Common errors:

- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## StudentMeetingController (Meetings)

Base path: `/api/student`

Endpoints require **STUDENT** or **ADMIN** role. Students can only access meetings where they are the student.

### GET `/api/student/meetings`

Paged list of meetings for the authenticated student (meetings where student_user_id is the current user), sorted by startDate descending.

- Auth: Required (STUDENT or ADMIN)
- Status: `200 OK`

Query params:

- `page` (optional): 0-based page index; default `0`.
- `size` (optional): Page size; default `20`, max `100`.

Success response: JSON object with pagination metadata and `content` array of meeting objects (same shape as single meeting in `GET /api/student/meetings/{id}`).

Common errors:

- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### GET `/api/student/meetings/{id}`

Get one meeting by ID. Allowed only if the meeting’s student is the authenticated user.

- Auth: Required (STUDENT or ADMIN)
- Status: `200 OK`

Path param: `id` (UUID)

Success response: same shape as `GET /api/tutor/meetings/{id}` (see TutorMeetingController).

Common errors:

- `404 MEETING_NOT_FOUND` (meeting not found or not the current user’s as student)
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## ConversationController (Chat)

Base path: `/api/conversations`

REST-based chat between students and tutors. Requires **STUDENT**, **TUTOR**, or **ADMIN**. There is **one conversation per tutor–student pair**; creating with the same pair returns the existing conversation (no new one is created). Caller must be the tutor or student. Only participants can access a conversation.

### POST `/api/conversations`

Ensure a conversation exists for the given tutor–student pair. Caller must be either the tutor or the student. If a conversation already exists for that pair, it is returned (no new one is created).

- Auth: Required (STUDENT, TUTOR, or ADMIN)
- Status: `201 Created` when a new conversation is created; `200 OK` when the conversation for that pair already exists

Request body:

```json
{
  "tutorUserId": "uuid",
  "studentUserId": "uuid"
}
```

Success response: `ConversationResponse` (see GET by id).

Common errors:

- `403 NOT_PARTICIPANT` (caller is not the tutor or student)
- `404 USER_NOT_FOUND` (tutor or student user not found)
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### GET `/api/conversations`

Paged list of conversations for the authenticated user (where they are student or tutor). Sorted by `createdDate` descending.

- Auth: Required (STUDENT, TUTOR, or ADMIN)
- Status: `200 OK`

Query params: `page` (default 0), `size` (default 20, max 100).

Success response: Paginated object with `content` array of `ConversationResponse`.

### GET `/api/conversations/{conversationId}`

Get one conversation. Caller must be the student or tutor of the conversation.

- Auth: Required (STUDENT, TUTOR, or ADMIN)
- Status: `200 OK`

Path param: `conversationId` (UUID)

Success response:

```json
{
  "id": "uuid",
  "studentUserId": "uuid",
  "tutorUserId": "uuid",
  "createdDate": "dd/MM/yyyy HH:mm"
}
```

Common errors:

- `404 CONVERSATION_NOT_FOUND`
- `403 FORBIDDEN` (not a participant)
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### GET `/api/conversations/{conversationId}/messages`

Paged list of messages in a conversation. Caller must be a participant. Ordered by `createdDate` ascending.

- Auth: Required (STUDENT, TUTOR, or ADMIN)
- Status: `200 OK`

Path param: `conversationId` (UUID). Query params: `page` (default 0), `size` (default 50, max 100).

Success response: Paginated object with `content` array of message objects: `id`, `conversationId`, `senderUserId`, `body`, `createdDate`, `readDate`.

### POST `/api/conversations/{conversationId}/messages`

Send a message. Caller must be a participant.

- Auth: Required (STUDENT, TUTOR, or ADMIN)
- Status: `201 Created`

Path param: `conversationId` (UUID)

Request body:

```json
{
  "body": "Message text (max 10000 chars)"
}
```

Success response: `MessageResponse` (id, conversationId, senderUserId, body, createdDate, readDate).

Common errors:

- `404 CONVERSATION_NOT_FOUND`
- `403 FORBIDDEN` (not a participant)
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### PATCH `/api/conversations/{conversationId}/read`

Mark all messages in the conversation sent by the **other** participant as read. Call when the current user opens the conversation. The backend uses the logged-in user and conversation to determine the other participant (the sender whose messages to mark); sets `read_date` on all their messages in this conversation that were unread.

- Auth: Required (STUDENT, TUTOR, or ADMIN)
- Status: `204 No Content`

Path param: `conversationId` (UUID). No request body.

Common errors:

- `404 CONVERSATION_NOT_FOUND`
- `403 FORBIDDEN` (not a participant)
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## TutorBlogController (Blogs)

Base path: `/api/tutor`

Endpoints provide a simple announcement/blog feature where tutors can post updates, and students/tutors/admins can read them. Posts can optionally target specific students and include attachments; comments are text-only.

### POST `/api/tutor/blogs`

Create a new blog post.

- Auth: Required (TUTOR)
- Status: `201 Created`

Request:

- `multipart/form-data`
  - Part `body` (string, required): Post text (max 5000 chars).
  - Part `targetStudentIds` (optional, may appear multiple times): UUIDs of students to target. If omitted or empty, the system automatically targets all active allocated students for the tutor, based on allocation slots (`endedDate` is null and `scheduleEnd` is null or in the future). If there are no such allocations, the post has no student targets and is only visible to tutors/admins via staff views.
  - Part `attachments` (optional, may appear multiple times): Files to upload and attach to the post (e.g. PDFs, images). Each file is stored and returned as a file name in `attachments`.

Success response:

- `BlogPostResponse` object (see below).

Common errors:

- `400 VALIDATION_ERROR`
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### PUT `/api/tutor/blogs/{id}`

Edit an existing blog post, including attachments and targets.

- Auth: Required (TUTOR)
- Status: `200 OK`

Path param:

- `id` (UUID): Blog post id.

Request:

- `multipart/form-data`
  - Part `body` (string, required): New post text (max 5000 chars).
  - Part `targetStudentIds` (optional, may appear multiple times): New list of target student UUIDs. If present and non-empty, replaces the existing targets. If omitted or empty, targets are left unchanged.
  - Part `attachments` (optional, may appear multiple times): New files to upload and attach in addition to any kept attachments.
  - Field `keepAttachmentNames` (optional, may appear multiple times as a regular form field): List of existing attachment file names to keep. Any existing attachments whose file name is not included here are removed before adding new files. If this field is omitted or empty, all existing attachments are removed before adding new ones (if any).

Success response:

- Updated `BlogPostResponse` object.

Common errors:

- `400 VALIDATION_ERROR`
- `404` (post not found or not visible to current user)
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### DELETE `/api/tutor/blogs/{id}`

Soft-delete a blog post. Only the tutor who created the post can delete it.

- Auth: Required (TUTOR)
- Status: `204 No Content`

Path param:

- `id` (UUID): Blog post id.

Success: The post is marked as deleted (`deleted_date` set). It no longer appears in list responses and get/update/comment on this post return 404.

Common errors:

- `404` (post not found or already deleted)
- `403 FORBIDDEN` (not the post creator)
- `401 UNAUTHORIZED`

### POST `/api/tutor/blogs/{id}/comments`

Add a comment to a blog post.

- Auth: Required (TUTOR)
- Status: `201 Created`

Path param:

- `id` (UUID): Blog post id.

Request body:

```json
{
  "comment": "Nice update!"
}
```

Fields:

- `comment` (string, required): Comment text (max 5000 chars).

Success response:

- `BlogCommentResponse`:

```json
{
  "id": "d3b8fa5c-1b2c-4d5e-8f9a-1234567890ab",
  "postId": "7d84ffd2-6g5j-8d3e-fh0g-1f0i119168gh",
  "authorUserId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
  "comment": "Nice update!",
  "createdDate": "15/03/2026 11:00",
  "updatedDate": null
}
```

Common errors:

- `400 VALIDATION_ERROR`
- `404` (post not found or not visible to current user)
- `401 UNAUTHORIZED` / `403 FORBIDDEN`

### GET `/api/tutor/blogs`

List blog posts visible to the authenticated user.

- Auth: Required (STUDENT, TUTOR, or ADMIN)
- Status: `200 OK`

Behavior:

- For tutors/admins: visible posts may include global posts and posts targeted to specific students.
- For students: returns only posts either not targeted (global) or explicitly targeted to the current student.

Success response:

- Array of `BlogPostResponse` objects (same shape as above).

Common errors:

- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## TutorAssignmentController (Assignments)

Base path: `/api/tutor/assignments`

Tutors create assignments with instructions and optional requirement files (stored as BLOBs). **No per-assignment target table**: any student with an **active allocation** to the tutor (`endedDate` is null and `scheduleEnd` is null or in the future) can see the assignment. Tutors can list submissions and set feedback (text only; students cannot reply). Application-wide local calendar: `app.default-time-zone` (see root `application.properties`).

### POST `/api/tutor/assignments`

- Auth: TUTOR
- Status: `201 Created`
- Content-Type: `multipart/form-data`

| Part | Required | Description |
|------|----------|-------------|
| `title` | Yes | Max 255 chars |
| `instructions` | Yes | Max 10000 chars |
| `dueDate` | No | Text `dd/MM/yyyy HH:mm` (24-hour), e.g. `31/12/2026 23:59`. Parsed in **`app.default-time-zone`** (default `Asia/Yangon`, overridable via `APP_DEFAULT_TIME_ZONE`); stored as an absolute instant in the database (`TIMESTAMPTZ`). JSON `Instant` fields (including `dueDate`) are formatted as the same `dd/MM/yyyy HH:mm` in that zone. |
| `attachments` | No | One or more files (requirement documents) |

Success: `AssignmentResponse` (id, createdById, title, instructions, dueDate, createdDate, updatedDate, attachments: `{ id, fileName }[]`, `submissions`: always `[]` on create).

### PUT `/api/tutor/assignments/{id}`

- Auth: TUTOR (creator only)
- Status: `200 OK`
- Content-Type: `multipart/form-data`

| Part / param | Required | Description |
|--------------|----------|-------------|
| `title`, `instructions` | Yes | Same limits as create |
| `dueDate` | No | Same `dd/MM/yyyy HH:mm` format as create, or omit |
| `keepAttachmentIds` | No | Query/form param; UUIDs of assignment attachments to keep. If omitted or empty, **all** requirement attachments are removed before new ones are added. |
| `attachments` | No | New files to add |

Success: `AssignmentResponse`; `submissions` is always `[]` on update (use GET `/{id}` for the full list).

### DELETE `/api/tutor/assignments/{id}`

- Auth: TUTOR (creator)
- Status: `204 No Content`  
Soft-deletes the assignment (hidden from lists; not found for students).

### GET `/api/tutor/assignments`

- Auth: TUTOR  
- Status: `200 OK`  
Returns `AssignmentSummaryResponse[]` (id, createdById, title, dueDate, createdDate, updatedDate).

### GET `/api/tutor/assignments/{id}`

- Auth: TUTOR (creator)  
- Status: `200 OK`  
Returns `AssignmentResponse` with `submissions[]` **inside** the same object (`AssignmentSubmissionSummaryResponse`: id, studentId, status, submittedAt, updatedAt, hasFeedback, `submissionAttachments`: `{ id, fileName }[]`), ordered by submitted time. Download bytes via `GET /api/assignments/submissions/attachments/{attachmentId}`.

### GET `/api/tutor/assignments/{assignmentId}/submissions/{submissionId}`

- Auth: TUTOR (assignment creator)  
- Status: `200 OK`  
Returns full `AssignmentSubmissionResponse` (includes attachment metadata and feedback fields).

### PUT `/api/tutor/assignments/{assignmentId}/submissions/{submissionId}/feedback`

- Auth: TUTOR (assignment creator)  
- Status: `204 No Content`  
Body: `{ "feedbackText": "..." }` (required, max 5000 chars).

Common errors: `ASSIGNMENT_NOT_FOUND`, `SUBMISSION_NOT_FOUND`, `ACCESS_DENIED`, `ONLY_TUTORS`, validation errors.

---

## StudentAssignmentController (Assignments)

Base path: `/api/student/assignments`

Students see assignments only from tutors they are **actively allocated** to (same allocation window as elsewhere).

### GET `/api/student/assignments`

- Auth: STUDENT  
- Status: `200 OK`  
`AssignmentSummaryResponse[]`.

### GET `/api/student/assignments/{id}`

- Auth: STUDENT  
- Status: `200 OK`  
`AssignmentResponse` if the student has an active allocation to the assignment’s tutor. `submissions` contains **at most one** item — the logged-in student’s own submission (`AssignmentSubmissionSummaryResponse` with `submissionAttachments`), or `[]` if they have not opened/submitted yet (no row). Other students’ submissions are never included.

### GET `/api/student/assignments/{id}/submission`

- Auth: STUDENT  
- Status: `200 OK`  
Returns the student’s submission for this assignment. If none exists yet, a **DRAFT** submission is created (empty attachments) and returned.

### PUT `/api/student/assignments/{id}/submission`

- Auth: STUDENT  
- Status: `200 OK`  
- Content-Type: `multipart/form-data`  
- Part `files`: **required** — at least one file. Replaces any previous submission files and sets status to `SUBMITTED`.

Common errors: `ACCESS_DENIED`, `ONLY_STUDENTS`, `INVALID_SUBMISSION` (no files), `ASSIGNMENT_NOT_FOUND`.

---

## AssignmentAttachmentController (Downloads)

Base path: `/api/assignments`

- Auth: STUDENT, TUTOR, or ADMIN (fine-grained checks in service).

### GET `/api/assignments/attachments/{attachmentId}`

Download a **requirement** file from `assignment_attachments`.  
- **Tutor**: must own the assignment.  
- **Student**: must have active allocation to that tutor.  
- **ADMIN**: allowed.

### GET `/api/assignments/submissions/attachments/{attachmentId}`

Download a **submission** file from `submission_attachments`.  
- **Student**: only their own submission.  
- **Tutor**: only for submissions on assignments they created.  
- **ADMIN**: allowed.

Response: raw bytes with `Content-Type` and `Content-Disposition: attachment`.

---

## PageViewController (Analytics ingest)

Base path: `/api/analytics`

Records one page view per request for usage reporting (school scope: path, user, browser label).

### POST `/api/analytics/page-views`

- Auth: STUDENT, TUTOR, or ADMIN (JWT required)
- Status: `204 No Content`
- Content-Type: `application/json`

Body:

```json
{ "pagePath": "/assignments" }
```

- `pagePath` (required): Logical route or screen id; leading `/` is added if missing. Max 255 chars.

The server stores `userId` from the JWT, `viewedAt` as now, and a short **browser** label derived from the `User-Agent` header (`Chrome`, `Edge`, `Firefox`, `Safari`, or `Other`).

Common errors: `400` validation, `401`, `404` if user record missing.

---

## InteractionReportController (Reports)

Base path: `/api/admin/reports`

Reports for platform usage and interaction (inactive users, page-view aggregates).

### GET `/api/admin/reports/usage-summary`

Aggregates page views in the date range (inclusive `from`, inclusive `to`), interpreted in **`app.default-time-zone`**.

- Auth: ADMIN
- Status: `200 OK`

Query params (required):

- `from` — ISO date (e.g. `2026-01-01`)
- `to` — ISO date (e.g. `2026-01-31`)

Max range: **366** days. Response:

```json
{
  "topPages": [{ "pagePath": "/assignments", "viewCount": 42 }],
  "topUsers": [
    { "userId": "…", "username": "student1", "email": "…", "viewCount": 30 }
  ],
  "browsers": [{ "browser": "Chrome", "viewCount": 100 }]
```

Up to **50** rows per list, ordered by count descending. Empty lists if no data.

Common errors: `400 INVALID_RANGE`, `401`, `403`.

### GET `/api/admin/reports/messaging-summary`

Message volume for a rolling time window ending **now** (UTC instants), plus one row per **personal tutor** (any user with at least one **active, current** tutor allocation: `ended_date` null and `schedule_end` null or in the future).

- Auth: ADMIN
- Status: `200 OK`

Query params:

- `windowDays` (optional, default `7`): Length of the window in days; must be between **1** and **90** inclusive. Messages counted with `windowStart <= created_date < windowEndExclusive` where `windowEndExclusive` is the server time when the report is generated and `windowStart = windowEndExclusive - windowDays`.

Success response:

```json
{
  "windowStart": "2026-03-22T10:00:00Z",
  "windowEndExclusive": "2026-03-29T10:00:00Z",
  "windowDays": 7,
  "totalMessagesInWindow": 42,
  "tutors": [
    {
      "tutorUserId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
      "username": "tutor1",
      "email": "tutor1@example.com",
      "firstName": "Tu",
      "lastName": "Tor",
      "messageCount": 14,
      "averageMessagesPerDay": 2.0
    }
  ]
}
```

- **`totalMessagesInWindow`:** All messages in any conversation in the window (not only those tied to listed tutors).
- **`tutors`:** Every distinct tutor with an active current allocation, sorted by username. **`messageCount`** is messages in conversations where that user is `tutor_user_id` within the same window. **`averageMessagesPerDay`** is `messageCount / windowDays`, rounded half-up to **2** decimal places (including `0.00` when there are no messages).

Common errors: `400 INVALID_REPORT_RANGE` (bad `windowDays`), `401`, `403`.

### GET `/api/admin/reports/inactive-users`

Generate a report of inactive students and tutors over a time window.

- Auth: Required (ADMIN)
- Status: `200 OK`

Query params:

- `days` (optional, integer): Threshold in days for inactivity; default `7`. Users whose last interaction was more than this many days ago are included.

Success response:

```json
{
  "daysThreshold": 7,
  "generatedAt": "15/03/2026 12:00",
  "students": [
    {
      "userId": "2a52bab9-2c1f-49b9-bd6c-7b6e775724cd",
      "role": "STUDENT",
      "username": "student1",
      "firstName": "Stu",
      "lastName": "Dent",
      "email": "student1@example.com",
      "lastInteractionDate": "01/03/2026 09:00",
      "inactivityDays": 14
    }
  ],
  "tutors": [
    {
      "userId": "3b63cbc0-3d2g-5a0c-ce7d-8c7f886835de",
      "role": "TUTOR",
      "username": "tutor1",
      "firstName": "Tu",
      "lastName": "Tor",
      "email": "tutor1@example.com",
      "lastInteractionDate": "05/03/2026 10:00",
      "inactivityDays": 10
    }
  ]
}
```

This shape corresponds to `InactiveUsersReportResponse` containing lists of `InactiveUserResponse` for students and tutors.

Common errors:

- `401 UNAUTHORIZED` / `403 FORBIDDEN`

---

## Scheduled jobs (operations, not HTTP)

These run inside the Spring Boot process; there is no public REST endpoint for them.

### 28-day student inactivity warning emails

- **Purpose:** If a **student** has no recorded activity for at least **`app.inactivity-reminder.threshold-days`** (default **28**), the system sends a warning email to the **student** and to each **allocated tutor** with an **active, current** tutor allocation (same notion of “active” as elsewhere: `ended_date` null and `schedule_end` null or in the future).
- **Activity baseline:** Same as the inactive-users report: `coalesce(last_interaction_date, created_date)` on the user row. In the current codebase, `last_interaction_date` is updated on signup, login, and certain blog actions—not on every API call (e.g. messaging or assignments). Align product wording with that definition or extend touch points later.
- **Schedule:** Cron `app.inactivity-reminder.cron` (default `0 0 8 * * *` — daily 08:00) in zone `app.default-time-zone`.
- **Enable/disable:** `app.inactivity-reminder.enabled` (default `true`; tests typically set `false`).
- **Audit trail:** Each attempt is stored in table **`inactivity_reminder_log`**: `student_user_id`, `tutor_user_id`, `activity_baseline_at`, `status` (`PENDING`, `SENT`, `PARTIAL`, `FAILED`), optional `student_sent_at` / `tutor_sent_at`, and error text if a send failed. **Idempotency:** unique on `(student_user_id, tutor_user_id, activity_baseline_at)` so the same inactivity episode is not emailed twice for that pair; after the student interacts again, the baseline changes and a new episode can generate a new row later.
- **Mail:** Uses synchronous send for this job so success/failure can be recorded. Requires a configured `spring.mail.from` and working SMTP (same as other emails).

---

## Global Error Codes (Current)

- `INVALID_RANGE` -> `400` (usage-summary: bad or too-wide date range)
- `INVALID_REPORT_RANGE` -> `400` (inactive-users: days not 7 or 28; messaging-summary: `windowDays` not 1–90)
- `INVALID_PAGE_PATH` -> `400` (page-views: blank or too long path)
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
- `MEETING_NOT_FOUND` -> `404` (meetings: meeting not found or not owned by current tutor / or not the student’s meeting for student GET)
- `ONLY_TUTORS_CAN_ARRANGE` -> `400` (meetings: only tutors can create meetings)
- `MEETING_NOT_WITHIN_ALLOCATION` -> `400` (meetings: meeting must fall within an allocation slot for this tutor–student pair)
- `MEETING_OVERLAP` -> `400` (meetings: tutor already has a meeting in this time range)
- `INVALID_MEETING_LOCATION` -> `400` (meetings: in-person requires non-blank location)
- `INVALID_MEETING_IN_PERSON` -> `400` (meetings: in-person must not include link or virtual platform)
- `INVALID_MEETING_LINK` -> `400` (meetings: virtual requires http(s) invitation link)
- `INVALID_MEETING_PLATFORM` -> `400` (meetings: virtual requires `virtualPlatform`)
- `INVALID_MEETING_VIRTUAL` -> `400` (meetings: virtual must not include physical location)
- `ALLOCATION_NOT_FOUND` -> `404`
- `CONVERSATION_NOT_FOUND` -> `404`
- `NOT_PARTICIPANT` -> `403` (conversations: not the student or tutor of the allocation/conversation)
- `ALLOCATION_ALREADY_ENDED` -> `400` (undo: allocation already ended)
- `CANNOT_UPDATE_ENDED_ALLOCATION` -> `400` (PUT: cannot update ended allocation)
- `NO_UPDATE_FIELDS` -> `400` (PUT: at least one field required)
- `INVALID_OR_EXPIRED_TOKEN` -> `400` (reset-password: token invalid or expired)
- `DUPLICATE_USERNAME` -> `409`
- `DUPLICATE_EMAIL` -> `409`
- `INTERNAL_SERVER_ERROR` -> `500`

