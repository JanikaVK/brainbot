# BRAINBOT

**A student wellbeing Android app with private check-ins, an encrypted journal and support messages, and an optional ML inference service.** Built with Expo / React Native, Spring Boot, FastAPI, and MySQL.

BrainBot is supportive software, not a clinical diagnostic system. Its model endpoint intentionally reports unavailable until a verified, permissioned dataset is supplied and a model is trained and reviewed. No student data, trained model, metrics, or credentials are included in this repository.

> **Project status:** working student workflows and CI checks are implemented. This is a development project, not a deployed or Play Store released service. See [current limitations](docs/CODEX_PROGRESS.md#remaining-constraints) before evaluating it.

[![CI](https://github.com/JanikaVK/brainbot/actions/workflows/ci.yml/badge.svg)](https://github.com/JanikaVK/brainbot/actions/workflows/ci.yml)

![BrainBot app icon](mobile/assets/icon.png)

## What it does

- Student registration and sign-in with expiring JWT sessions and secure mobile token storage.
- One-time emailed password reset links; sessions issued before a reset are revoked when SMTP is configured.
- Daily check-ins, recent history, and simple descriptive trends.
- Journal entries encrypted by the backend before database storage.
- Saved support requests, with their pending-routing status made explicit.
- Account data export and account deletion.
- Optional wellness-model inference and feature explanations, disabled until a compatible reviewed artifact is installed.
- Everyday suggestions based on the current check-in; these are not clinical advice.

The current mobile app supports the student role. Faculty and therapist workspaces, institution provisioning, and notification/routing integrations are not implemented.

## Architecture

```text
Expo Android app -> Spring Boot REST API -> MySQL
                              |
                              +-> FastAPI inference service (trained artifact required)
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/ML_PIPELINE.md](docs/ML_PIPELINE.md), [docs/API.md](docs/API.md), [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md), [docs/PLAY_STORE.md](docs/PLAY_STORE.md), and [docs/CODEX_PROGRESS.md](docs/CODEX_PROGRESS.md).

## Prerequisites

- Node.js 22.13+
- Java 21 and Maven 3.9+
- Python 3.11+
- Docker Compose (optional for local MySQL stack)
- Android Studio/device for Android builds

## Start services

1. Copy `.env.example` to `.env`; generate an encryption key with `openssl rand -base64 32`, paste it as `DATA_ENCRYPTION_KEY_B64`, and replace every sample credential before starting. Losing this key makes encrypted journal and support message contents unrecoverable.
2. Run `docker compose up --build` for MySQL, API, and ML API. The API uses MySQL health checks; no model is bundled.
3. Start mobile: `cd mobile && npm install && npx expo start` (press `a` with an Android emulator running).
4. Configure `SMTP_HOST`, SMTP credentials, `BRAINBOT_MAIL_FROM`, and `BRAINBOT_PASSWORD_RESET_BASE_URL` to enable emailed password recovery; leave SMTP unset to keep recovery explicitly unavailable. For a physical device, set `EXPO_PUBLIC_API_URL` to the host machine's reachable HTTPS endpoint. Android emulator uses `http://10.0.2.2:8080` in local development.

Backend local run (Spring Boot 3.5, Java 21): `set -a; source .env; set +a; cd backend && mvn spring-boot:run`. ML local run: `cd ml && python -m venv .venv && . .venv/bin/activate && pip install -r requirements.txt && uvicorn app.main:app --reload --port 8000`.

## ML training

Put a verified, permissioned CSV at `ml/data/student_wellness.csv` (ignored by Git), with `target` as the documented label and feature columns described in `docs/ML_PIPELINE.md`. Install `ml/requirements-train.txt`; run `python -m training.train --input data/student_wellness.csv --output artifacts`. The script validates data, splits before fitting preprocessing, trains Random Forest, XGBoost, LightGBM and a probability-averaging ensemble, evaluates on held-out data, and serializes the fitted pipeline. No metrics or prediction are claimed until actual data is used. SHAP is optional and returned only when supported by an available trained artifact.

## Tests and build status

- API: `cd backend && mvn test`
- ML: `cd ml && pip install -r requirements-dev.txt && pytest`
- Mobile: `cd mobile && npm install && npx expo-doctor`
- Android release: `cd mobile && npx expo prebuild --platform android && cd android && ./gradlew bundleRelease`

GitHub Actions runs the backend, ML, mobile type, Expo compatibility, and Android JavaScript export checks on pushes and pull requests to `main`.

## Project layout

```text
backend/     Spring Boot REST API, persistence, authentication, encryption
ml/          FastAPI inference API and dataset-driven training pipeline
mobile/      Expo Router Android client and generated Android project
docs/        Architecture, API, deployment, ML, and release notes
compose.yaml Local MySQL, backend, and inference-service stack
```

## Security and privacy

Journal ciphertext is stored in MySQL; the encryption key is configured outside the app. The mobile client contains no database credentials or backend secrets. Do not put real student data, datasets, model artifacts, `.env` files, or signing keys in Git. The account export contains recent records (up to 90 check-ins and 50 items for each other history type); deletion removes the account and its saved records.

This project has not been release-built or externally security-reviewed. It uses Expo SDK 57 / React Native 0.86 and targets Android API 36; the current Play Console target API requirement is API 36 for new app submissions from August 31, 2026. The release build needs Node.js 22.13+, a configured Android SDK, signing key stored outside source control, and production HTTPS endpoints. Never commit `.env`, signing keys, datasets, or model artifacts.

## Privacy and safety

Collect only account and wellness inputs required for an explicitly requested feature. Journal entry content is encrypted with AES-GCM before database storage; keep the encryption key separate from database backups. Provide complete deletion/export workflows before production launch. Never expose model output as diagnosis or substitute for professional support. Crisis support information must be localized and verified before it is enabled. Review applicable privacy law, Google Play policies, and institutional requirements before release.
