# Control Plane: Registry Server & Storage Subsystem

## 1. Subsystem Purpose & Responsibilities
The **Registry Server** (`:backend:registry-server`) acts as the authoritative source of truth for VPN profile definitions, immutable version histories, cryptographic signing, release channel promotions, and device credentials.

---

## 2. API Contract Specification

All JSON API endpoints encapsulate payloads in standard `ApiEnvelope<T>` wrappers:

```json
{
  "status": "SUCCESS",
  "data": { ... },
  "error": null
}
```

### 2.1 Public & Device Enrollment Endpoints

#### `POST /api/v1/devices/tokens`
Generates a single-use enrollment token bound to a device name, platform, and profile assignment.
- **Request**:
  ```json
  {
    "deviceName": "dev-laptop-01",
    "platform": "MACOS",
    "assignedProfileId": "corp-vpn",
    "assignedChannel": "STABLE",
    "expiresInMinutes": 60
  }
  ```
- **Response**: `201 Created` with `{ "token": "uuid", "expiresAtEpochMs": 1791400000000 }`

#### `POST /api/v1/devices/enroll`
Consumes an enrollment token, issuing persistent device credentials and the public Ed25519 signing key.
- **Request**:
  ```json
  {
    "token": "uuid",
    "deviceId": "dev-01",
    "deviceName": "dev-laptop-01",
    "platform": "MACOS"
  }
  ```
- **Response**: `201 Created` with `DeviceCredentials(deviceId, apiKey, serverPublicKeyBase64, keyId)`

#### `POST /api/v1/devices/{id}/revoke`
Administrative endpoint to immediately revoke device authorization. Subsequent sync calls return `403 Forbidden`.

---

### 2.2 Authenticated Device Profile Sync API

#### `GET /api/v1/sync/manifest`
- **Headers**: `Authorization: Bearer <device_api_key>`
- **Behavior**: Verifies device status (rejects if `REVOKED`), resolves assigned profile and channel, returns signed profile metadata.
- **Response**:
  ```json
  {
    "status": "SUCCESS",
    "data": {
      "profileId": "corp-vpn",
      "version": 3,
      "channel": "STABLE",
      "sha256Digest": "a3f5...",
      "signature": "d4e2...",
      "keyId": "srv-ed25519-v1",
      "artifactUrl": "https://vpn-registry.company.com/api/v1/profiles/corp-vpn/versions/3/artifact"
    }
  }
  ```

---

### 2.3 Profile & Artifact Administration

- `POST /api/v1/profiles`: Register a logical profile ID (`id`, `name`, `description`).
- `POST /api/v1/profiles/{id}/versions`: Upload a `.ovpn` configuration text. Runs parser and security policy validator. If valid, computes SHA-256 digest, signs with Ed25519, and stores immutable version record.
- `POST /api/v1/profiles/{id}/channels`: Promote a version to `STABLE`, `BETA`, or `CANARY`.
- `GET /api/v1/profiles/{id}/versions/{version}/artifact`: Downloads verbatim `.ovpn` configuration payload.
- `GET /healthz`: Lightweight healthcheck endpoint returning `{ "status": "UP", "database": "CONNECTED" }`.

---

## 3. Database Relational Model (PostgreSQL)

```sql
-- Profiles logical entities
CREATE TABLE profiles (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at_epoch_ms BIGINT NOT NULL
);

-- Immutable version payloads with cryptographic proof
CREATE TABLE profile_versions (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) REFERENCES profiles(id),
    version BIGINT NOT NULL,
    ovpn_content TEXT NOT NULL,
    sha256_digest VARCHAR(64) NOT NULL,
    signature VARCHAR(256) NOT NULL,
    key_id VARCHAR(64) NOT NULL,
    created_at_epoch_ms BIGINT NOT NULL,
    CONSTRAINT idx_profile_version_unique UNIQUE (profile_id, version)
);

-- Active version pointers per release channel
CREATE TABLE channel_publications (
    channel VARCHAR(32) NOT NULL,
    profile_id VARCHAR(64) REFERENCES profiles(id),
    active_version_id VARCHAR(64) REFERENCES profile_versions(id),
    updated_at_epoch_ms BIGINT NOT NULL,
    PRIMARY KEY (channel, profile_id)
);

-- Enrolled devices with unique API keys
CREATE TABLE devices (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    api_key VARCHAR(128) UNIQUE NOT NULL,
    status VARCHAR(32) NOT NULL, -- ACTIVE, REVOKED
    assigned_profile_id VARCHAR(64) REFERENCES profiles(id),
    assigned_channel VARCHAR(32) NOT NULL,
    enrolled_at_epoch_ms BIGINT NOT NULL,
    last_seen_at_epoch_ms BIGINT
);

-- One-time short-lived enrollment tokens
CREATE TABLE enrollment_tokens (
    token VARCHAR(64) PRIMARY KEY,
    device_name VARCHAR(128) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    assigned_profile_id VARCHAR(64) REFERENCES profiles(id),
    assigned_channel VARCHAR(32) NOT NULL,
    expires_at_epoch_ms BIGINT NOT NULL,
    used BOOLEAN DEFAULT FALSE NOT NULL
);

-- Tamper-evident sanitized audit trails
CREATE TABLE audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    actor VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    details_sanitized TEXT NOT NULL,
    timestamp_epoch_ms BIGINT NOT NULL
);
```
