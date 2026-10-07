# API (initial)

Base URL: `http://localhost:8080` in development; HTTPS is required in deployment.

- `GET /api/health` — API liveness.
- `GET /api/model/status` — whether an inference artifact is installed; exposes no user data.
- `POST /api/assessment/predict` — authenticated student; forwards a validated wellness feature set to ML and returns prediction or explicit unavailable response. No result is fabricated.
- `GET /api/assessments` — authenticated student's saved model estimates.
- `POST /api/assessment/explain` — authenticated student; requests model feature contributions when a compatible trained artifact is installed.
- `POST /api/auth/register` — `{ "name", "email", "password", "role" }`; role may be student only for public registration.
- `POST /api/auth/login` — `{ "email", "password" }`; returns bearer access token, role, `expiresInSeconds`, and absolute `expiresAtEpochSeconds` for secure client-side session expiry.
- `POST /api/auth/password-reset/request` — `{ "email" }`; sends a one-time, 15-minute deep link when SMTP and `BRAINBOT_PASSWORD_RESET_BASE_URL` are configured. Response does not reveal whether the account exists. Requests are rate-limited per account.
- `POST /api/auth/password-reset/confirm` — `{ "token", "newPassword" }`; consumes the one-time reset token and invalidates existing sessions.
- `POST /api/check-ins` — authenticated student, wellness fields in 1–5 scale where applicable.
- `GET /api/check-ins` — authenticated user's own check-in history.
- `GET /api/check-ins/trends` — descriptive 7-day averages for the authenticated student; no risk label is inferred.
- `GET /api/account/export` — authenticated student data export in JSON (`brainbot-account-export-v1`); includes up to 90 recent check-ins and 50 recent items per other history type, and excludes credentials.
- `DELETE /api/account` — deletes the authenticated account and associated check-ins, assessments, journal entries, and support requests.
- `POST /api/support-requests` and `GET /api/support-requests` — create and view a student's own saved support requests. Requests remain `PENDING_ROUTING`; no staff directory, assignment, or notification integration exists yet.
- `POST /api/journal`, `GET /api/journal`, `PUT /api/journal/{entryId}`, `DELETE /api/journal/{entryId}` — encrypted, owner-scoped student journal entries.
- ML service `GET /health`, `GET /model/version`, `POST /predict`, `POST /explain`.

The ML service returns HTTP 503 while no trained artifact is installed. Journal endpoints require `BRAINBOT_DATA_KEY_BASE64` to be set to base64-encoded 32 random bytes. API error responses should be generic and must not include stack traces. API versioning, OpenAPI security schemes, pagination, role-scoped staff endpoints, and staff provisioning remain to be implemented. Password recovery requires a production SMTP provider, from-address, and app deep-link configuration.
