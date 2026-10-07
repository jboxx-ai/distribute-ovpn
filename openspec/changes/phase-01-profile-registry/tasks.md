# Tasks

## 1. Persistence

- [x] 1.1 Create PostgreSQL schema and migrations for profiles, versions, and channel publications.
- [x] 1.2 Implement repository interfaces with transaction tests.
- [x] 1.3 Enforce immutable `(profile_id, version)` records.

## 2. OVPN ingestion

- [x] 2.1 Implement shared `.ovpn` parser covering required client directives and inline blocks.
- [x] 2.2 Implement allow/deny validation policy with explicit forbidden directive errors.
- [x] 2.3 Add fixture corpus for valid, malformed, and dangerous configurations.

## 3. Registry API

- [x] 3.1 Implement create profile, upload version, promote channel, manifest, and artifact endpoints.
- [x] 3.2 Implement structured error responses and request size limits.
- [x] 3.3 Add API integration tests against ephemeral PostgreSQL.

## 4. Packaging

- [x] 4.1 Create production Docker image.
- [x] 4.2 Add Docker Compose service definitions for registry and PostgreSQL.
- [x] 4.3 Document local migration, startup, and smoke-test procedure.
