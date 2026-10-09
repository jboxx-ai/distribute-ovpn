# Tasks — Phase 11

## 1. Backend TOTP Engine & User Management (`:backend:registry-server`)

- [x] 1.1 Create database migrations for `users` and updated `devices` table with `user_id` and `token_expires_at`.
- [x] 1.2 Implement RFC 6238 TOTP validation service with ±1 step window and replay protection.
- [x] 1.3 Implement `POST /api/v1/auth/login` and `POST /api/v1/auth/totp/setup` endpoints.
- [x] 1.4 Implement Ktor JWT/Session authentication filter with RBAC enforcement (`Admin`, `Operator`, `Auditor`).
- [ ] 1.5 Add brute-force rate-limiting on authentication endpoints.

## 2. Token-Bound Device Trust & Enrollment (`:backend:registry-server` & `:shared:api-model`)

- [x] 2.1 Implement `POST /api/v1/devices/authorize` supporting user TOTP credentials, metadata, and Ed25519 public key.
- [x] 2.2 Update sync endpoints (`/api/v1/sync/*`) to enforce valid device token and active user checks.
- [x] 2.3 Implement instant remote revocation cascade in registry repository.
- [x] 2.4 Add unit and integration tests for expired tokens, clock drift, and revoked device sync rejection.

## 3. Multiplatform Client Auth Core (`:shared:domain` & `:shared:sync`)

- [ ] 3.1 Implement multiplatform `AuthRepository` and `DeviceTokenStorage` interfaces.
- [ ] 3.2 Implement `StrictOnlineSyncPolicy` (Policy A) gating profile sync and tunnel activation.
- [ ] 3.3 Add automatic logout / session termination handling on `401 Unauthorized` / `403 Forbidden`.

## 4. Native Client Applications UI (`:apps:android` & `:apps:macos`)

- [ ] 4.1 Implement Jetpack Compose native login screen (Username, Password, TOTP 6-digit input) on Android.
- [ ] 4.2 Securely store issued device token in Android Keystore / `EncryptedSharedPreferences`.
- [ ] 4.3 Implement SwiftUI native login view on macOS with Keychain token persistence.
- [ ] 4.4 Hook tunnel activation lifecycle to pre-flight online registry token verification.

## 5. Console Registry UI (`apps/console-registry`)

- [ ] 5.1 Implement Kumo UI login screen with username, password, and TOTP code fields.
- [ ] 5.2 Implement authenticated user session context and token refresh handler.
- [ ] 5.3 Implement TOTP onboarding view with QR code rendering (`otpauth://` URI).
- [ ] 5.4 Enhance Device Fleet UI with user association column, token expiration status, and one-click revocation.
