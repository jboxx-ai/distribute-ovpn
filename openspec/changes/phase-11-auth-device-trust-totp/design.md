# Design — Phase 11

## 1. Authentication Architecture

### 1.1 Self-Contained TOTP Engine
- Standard: RFC 6238 (TOTP) with HMAC-SHA1 / HMAC-SHA256, 30-second time step, and 6-digit output.
- Passwords are encrypted using Argon2id or BCrypt before persistence in `users`.
- TOTP secrets are stored encrypted at rest with the registry server key.
- Time-drift compensation: Server accepts code within a ±1 step window (current timestamp ± 30 seconds) to tolerate client clock drift.
- Anti-replay: A consumed TOTP code cannot be reused within the same time window.

### 1.2 Web Console Session Flow
- Operator accesses Console Registry -> Prompts for Username, Password, and 6-digit TOTP code.
- Endpoint: `POST /api/v1/auth/login`.
- Server returns HTTP-only, Secure, SameSite=Strict session cookie or Bearer JWT.
- RBAC roles: `Admin` (full mutation and fleet revocation), `Operator` (read/telemetry/profile staging), `Auditor` (read-only audit logs).

## 2. Client Device Enrollment & Key Binding Flow

### 2.1 Native In-App Form
- Android (`Jetpack Compose`) and macOS (`SwiftUI`) display a native, embedded login screen (Username, Password, 6-digit TOTP).
- No external browser bouncing, custom URI schemes, or external OAuth redirect handlers are required.

### 2.2 Cryptographic Key Binding
1. Client generates local Ed25519 asymmetric keypair in Android Keystore / macOS Keychain.
2. Client submits `POST /api/v1/devices/authorize`:
   - `username`, `password`, `totp_code`
   - `device_public_key` (Base64 Ed25519 public key)
   - `device_metadata` (OS name, version, model, app build)
3. Registry validates user credentials and TOTP code.
4. Registry links `device_id` to `user_id` and records the device's public key in PostgreSQL (`devices` table).
5. Registry returns a scoped `device_token` (JWT with `sub=device_id`, `uid=user_id`, `exp=timestamp`).
6. Client stores `device_token` securely in Keystore/Keychain.

## 3. Token-Bound Device Trust & Strict Online Policy (Policy A)

### 3.1 Strict Online Verification
- Profile synchronization (`GET /api/v1/sync/manifest` & `GET /api/v1/sync/profiles/{id}/version/{v}`) requires `Authorization: Bearer <device_token>` and request signature matching the registered Ed25519 key.
- Registry verifies:
  1. Device exists and status is `ACTIVE`.
  2. Device token is unexpired and unrevoked.
  3. Associated user account is active.
- If any check fails, or if registry is unreachable, client immediately transitions to `UNAUTHENTICATED` / `TRUST_REVOKED` and **aborts VPN tunnel activation**.

### 3.2 Instant Revocation
- When an administrator revokes a device via Console or `vpnctl`:
  - Device status is set to `REVOKED`.
  - Active tokens for that device are invalidated immediately.
  - Next sync check fails with `403 Forbidden`.
  - Client shuts down active VPN tunnel and purges decrypted local tunnel state.

## 4. Database Schema Updates

```sql
-- Users and credentials
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    totp_secret_encrypted VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'OPERATOR',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enhance devices table with user ownership and token metadata
ALTER TABLE devices ADD COLUMN user_id UUID REFERENCES users(id);
ALTER TABLE devices ADD COLUMN token_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE devices ADD COLUMN last_authenticated_at TIMESTAMP WITH TIME ZONE;
```
