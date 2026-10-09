# TOTP Authentication Specification

## Purpose

Defines self-contained RFC 6238 Time-Based One-Time Password verification, user credential storage, and session generation on the registry server.

## ADDED Requirements

### Requirement: Multi-Factor Operator Login
The registry server SHALL require both valid user password credentials and a valid 6-digit TOTP code for operator authentication.

#### Scenario: Valid credentials and TOTP
- **GIVEN** a registered user with password and configured TOTP secret
- **WHEN** the operator submits correct username, password, and the current 6-digit TOTP code
- **THEN** the server SHALL issue an authenticated session token with the user's assigned RBAC role

#### Scenario: Invalid or expired TOTP
- **WHEN** the operator submits correct password but an invalid or out-of-window TOTP code
- **THEN** the server SHALL reject authentication with HTTP 401 Unauthorized

### Requirement: Anti-Replay and Time-Drift Window
The server SHALL accept TOTP codes within a ±1 time step (±30 seconds) window and SHALL prevent reuse of the same TOTP code within the active window.

#### Scenario: Reused TOTP code
- **GIVEN** a TOTP code successfully consumed within the current 30-second window
- **WHEN** the same code is submitted again for authentication
- **THEN** the server SHALL reject the request with HTTP 401 Unauthorized
