# Design — Phase 01

## Backend components

- Ktor routing layer
- application services
- PostgreSQL repositories
- profile parser/policy from shared KMP modules
- immutable artifact storage abstraction

V1 may store artifact bytes in PostgreSQL for simplicity. The storage abstraction must allow later migration to object storage without changing external behavior.

## Core entities

```text
Profile
- id
- name
- created_at

ProfileVersion
- profile_id
- version
- sha256
- byte_size
- content
- validation_policy_version
- created_at

Publication
- profile_id
- channel
- version
- published_at
```

`(profile_id, version)` is immutable and unique.

## APIs

Initial operator APIs may be local/admin-auth protected until Phase 02:

```text
POST /v1/admin/profiles
POST /v1/admin/profiles/{id}/versions
POST /v1/admin/profiles/{id}/channels/{channel}/promote

GET /v1/profiles/{id}/channels/{channel}/manifest
GET /v1/profiles/{id}/versions/{version}/artifact
```

## Upload transaction

1. Read bytes with size limit.
2. Parse `.ovpn`.
3. Enforce profile policy.
4. Canonicalize only metadata; do not rewrite security-sensitive content.
5. Calculate digest.
6. Allocate next monotonically increasing version.
7. Store immutable bytes and metadata atomically.
8. Return version.

## Error contract

Use structured API errors with stable machine code and human-readable detail. Never leak certificate/key contents in errors or logs.

## Open Questions

None blocking implementation.
