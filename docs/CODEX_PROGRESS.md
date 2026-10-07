# BrainBot build checkpoint

Bootstrapped from an empty workspace on 2026-10-07. No verified dataset, trained model, therapist directory, institution roster, production credentials, Android SDK, or Docker engine was available.

## Status

- [x] Repository audit and clean project scaffold
- [x] Spring Boot API, MySQL configuration, JWT auth, bcrypt password storage, role checks
- [x] Student registration/login, account JSON export/deletion, and email password reset with one-time tokens and session invalidation
- [x] Student check-ins, history, 7-day descriptive trends
- [x] Encrypted owner-scoped journal (AES-256-GCM)
- [x] Owner-scoped support requests persisted as `PENDING_ROUTING`
- [x] ML training pipeline for real CSV input; no dataset or trained model supplied
- [x] FastAPI inference, model status and permutation-based explanations; unavailable without a compatible artifact
- [x] Spring API assessment proxy, saved estimate history, explanation proxy; no estimates invented
- [x] Expo Router Android app foundation and connected student workflows (SDK 57 / React Native 0.86)
- [x] Docker Compose configuration; not runtime-tested because Docker is unavailable
- [x] Documentation including architecture, ML pipeline, deployment, API, and Play Store preparation
- [x] Backend tests: 7 passed (H2 integration/security, encrypted-payload, account-export, and password-reset/session-revocation tests)
- [x] ML tests: 5 passed
- [x] Mobile TypeScript check, Expo SDK dependency check, Android JS export (1,245 modules)
- [x] Android native project generated; external release signing configuration added
- [x] Android native project regenerated with Expo SDK 57; release signing plugin retained
- [x] Client sessions now expire against the JWT expiry timestamp; stale sessions are cleared
- [x] Model explanation UI translates factors to plain language and hides raw contribution scores
- [x] Optional everyday suggestions respond to the current check-in without presenting clinical guidance
- [x] Git ignore rules exclude generated build/cache directories and local work files
- [ ] Faculty/therapist workspaces, institution provisioning, assignment and notifications
- [x] Email password reset flow with one-time tokens, account enumeration protection, throttling, and session revocation; SMTP provider configuration remains external
- [ ] Configurable emergency/support directory and externally hosted deletion webpage
- [ ] Real-data model training, validation, artifact compatibility and explanation verification; the reviewed open dataset's features do not match the current check-in inputs and the cohort is too small/narrow for the app
- [ ] Docker runtime validation and MySQL integration tests
- [ ] Android SDK/device testing and signed release AAB
- [ ] Resolve or accept documented upstream npm advisories affecting Expo/Metro build dependencies before release

## Verified commands

- `backend`: Maven test suite passed (6 tests, 0 failures) with Java 21 and H2.
- `ml`: `pytest -q` passed (5 tests); one upstream Starlette deprecation warning.
- `mobile`: final SDK 57 `tsc --noEmit`, `expo install --check`, `expo prebuild --platform android --clean --no-install`, and `expo export --platform android` passed; JS export bundled 1,244 modules.
- Backend: `JAVA_HOME=<Java 21>` `mvn -B -Dmaven.repo.local=<temporary-dir> test` passed (7 tests, 0 failures); `mvn -B -Dmaven.repo.local=<temporary-dir> package -DskipTests` built the executable jar. ML: `pytest -q` passed (5 tests, one upstream Starlette deprecation warning).
- Mobile: SDK 57 `tsc --noEmit`, Expo dependency check, and Android JavaScript export passed (1,245 modules), including the reset-password deep-link screen.
- `npm audit --omit=dev` reports 29 moderate/high advisories (18 high). Current advisories flag transitive Expo/Metro dependencies including `braces` and `node-forge`; npm offers no safe fix and `npm audit fix --force` proposes incompatible major changes. Do not force-upgrade; recheck after upstream patches or replace affected tooling before release.

## Remaining constraints

The student workflows are implemented, but this does not establish a full production release. Inference intentionally returns unavailable: the reviewed open research dataset does not match the check-in schema and is too narrow to validate the app's estimate. Faculty/therapist routing and notifications need real institution and service integrations. Password reset is implemented but requires a production SMTP provider and mail identity. Externally hosted policy/deletion pages are not implemented. The JSON export includes up to 90 recent check-ins and 50 recent records of each other type. The Android bundle exports and native project prebuilds, but Android compilation/device testing could not be exercised because Android SDK/device are not present. Docker/MySQL and signed release builds also remain unverified. The npm audit findings above remain open. Do not describe the app as Play Store ready until these gaps are addressed.

The project is saved at `~/Desktop/brainbot`.

GitHub publication is requested for `JK-cyber31` as a recruiter-shareable public project. The source-only project is committed on local `main` at `~/Desktop/brainbot`; generated dependencies, local work/cache files, `.env`, and Android debug keystore are excluded. The local tree is clean. Creating/pushing the public repository and configuring the profile README remain pending account authentication. Do not claim a production deployment or release.
