# Phase 11 — Self-Contained TOTP Authentication, Native Client Enrollment, and Token-Bound Device Trust

## Why

Operating a secure VPN fleet requires verifying both the operator accessing the Console Registry and the end-user enrolling client devices (Android and macOS). External Identity Providers (OIDC/OAuth2) introduce external cloud dependencies, secret rotation overhead, and complex browser redirect flows. 

A self-contained Time-Based One-Time Password (TOTP / RFC 6238) authentication engine provides zero-dependency, air-gappable, and high-assurance multi-factor security. In addition, enforcing **Strict Online Verification (Policy A)** guarantees that VPN profile synchronization and tunnel activation are strictly gated by valid, non-revoked active user tokens and cryptographic device identity.

## What Changes

- Add registry-side user management and RFC 6238 TOTP validation in `:backend:registry-server`.
- Implement secure web session authentication with RBAC (`Admin`, `Operator`, `Auditor`) for the Console Registry (`apps/console-registry`).
- Implement native in-app TOTP authorization for Android (`:apps:android`) and macOS (`:apps:macos`) without third-party browser redirects.
- Bind device cryptographic identities (Ed25519 public keys) to authenticated user accounts during enrollment.
- Issue revocable, token-bound device sessions with explicit expiration.
- Enforce **Strict Online Verification (Policy A)**: VPN profile sync and tunnel activation fail immediately if the device token cannot be verified against the active registry.
- Add fleet device trust management and one-click revocation in the Console Registry.

## Capabilities

### New Capabilities
- `totp-authentication`: Registry-side RFC 6238 TOTP verification, user password hashing, and session management.
- `client-totp-enrollment`: Native in-app credential + TOTP verification and Ed25519 device keypair binding for Android and macOS.
- `token-device-trust`: User-bound device trust lifecycle, token expiration tracking, and instant remote revocation.
- `console-auth-ui`: Operator login interface, TOTP secret onboarding QR code, and fleet trust management views.

## Impact

- Upgrades the Console Registry from unauthenticated/CLI-only administration to secure multi-factor authentication.
- Replaces manual one-time token distribution with native user-authenticated device enrollment.
- Restricts VPN profile delivery strictly to active, authenticated devices.
