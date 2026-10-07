# Deployment

Development uses Compose with MySQL, API, and ML service. Supply `.env` locally; never commit it. Compose intentionally keeps MySQL/ML un-published to the host. For production, use managed secret storage, private networking, TLS termination, managed/secured database backups, migration tooling, health probes, resource limits, and pinned/scanned image versions. Do not expose the ML service publicly.

Production deployment is not configured or verified. Rotate all sample credentials, use a high-entropy JWT secret, use HTTPS, and complete privacy/security review before accepting real wellness data.
