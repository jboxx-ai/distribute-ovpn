# Client TOTP Enrollment Specification

## Purpose

Defines native, in-app client enrollment and cryptographic device binding for Android and macOS applications using TOTP credentials.

## ADDED Requirements

### Requirement: Native In-App Authentication
Client applications (Android and macOS) SHALL provide a native login interface accepting Username, Password, and 6-digit TOTP code without launching an external web browser.

#### Scenario: Submitting native credentials
- **GIVEN** an un-enrolled client application
- **WHEN** the user inputs valid account credentials and a live 6-digit TOTP code
- **THEN** the client application SHALL generate an Ed25519 asymmetric keypair and submit the enrollment request to `/api/v1/devices/authorize`

### Requirement: Cryptographic Identity Binding
The client application SHALL generate a private Ed25519 key within platform secure hardware (Android Keystore / macOS Keychain) and upload ONLY the public key to the registry server.

#### Scenario: Key registration on registry
- **GIVEN** a successful TOTP authorization
- **WHEN** the server registers the device
- **THEN** the device's public key SHALL be permanently bound to the user's account ID and assigned device ID
