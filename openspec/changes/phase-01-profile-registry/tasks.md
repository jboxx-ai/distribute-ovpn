# Tasks

## 1. Persistence

- [ ] 1.1 Create PostgreSQL schema and migrations for profiles, versions, and channel publications.
- [ ] 1.2 Implement repository interfaces with transaction tests.
- [ ] 1.3 Enforce immutable `(profile_id, version)` records.

## 2. OVPN ingestion

- [ ] 2.1 Implement shared `.ovpn` parser covering required client directives and inline blocks.
- [ ] 2.2 Implement allow/deny validation policy with explicit forbidden directive errors.
- [ ] 2.3 Add fixture corpus for valid, malformed, and dangerous configurations.

## 3. Registry API

- [ ] 3.1 Implement create profile, upload version, promote channel, manifest, and artifact endpoints.
- [ ] 3.2 Implement structured error responses and request size limits.
- [ ] 3.3 Add API integration tests against ephemeral PostgreSQL.

## 4. Packaging

- [ ] 4.1 Create production Docker image.
- [ ] 4.2 Add Docker Compose service definitions for registry and PostgreSQL.
- [ ] 4.3 Document local migration, startup, and smoke-test procedure.
