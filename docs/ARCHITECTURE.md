# Architecture

The Android client is an Expo React Native application. It talks only to the Spring Boot API over HTTPS in deployments. Spring owns authentication, authorization, persistence, and orchestration; MySQL remains private to the service network. The Python FastAPI service accepts a fixed feature schema and does not mount or accept arbitrary filesystem paths.

The initial slice provides account registration/login, account deletion, authenticated student check-ins and descriptive 7-day summaries, and saved support requests. Support requests are private to the student and remain `PENDING_ROUTING`; no organization/staff directory exists, so the system does not claim that anyone is notified or assigned. Roles are `STUDENT`, `FACULTY`, and `THERAPIST`; staff access needs explicit institution-scoped authorization before use. The client never receives database credentials or the signing secret.

Prediction is a separate capability. Spring Boot now proxies the fixed feature schema to ML. If the versioned trained artifact is missing, inference reports unavailable. API output must identify model version and uncertainty where available; it must never be described as a diagnosis. Assessment result persistence and explanation orchestration are not implemented yet.

Before production: add refresh-token rotation/revocation, rate limiting, account recovery email integration, audit controls, data export/deletion, consent versioning, encrypted backups, observability without sensitive payloads, TLS and deployment secrets management.
