# Administration CLI Specification

## Purpose

Defines supported operator workflows without requiring a web dashboard.

## ADDED Requirements

### Requirement: Profile administration
Authorized operators SHALL be able to create profiles, upload versions, promote versions, and inspect publication state using the CLI.

#### Scenario: Upload profile

- **WHEN** an authorized operator uploads a valid `.ovpn` file
- **THEN** the CLI SHALL return the immutable created version and digest


### Requirement: Device administration
Authorized operators SHALL be able to issue enrollment authorizations, list devices, assign profiles/channels, and revoke devices.

#### Scenario: Revoke device

- **WHEN** an operator revokes a device
- **THEN** the CLI SHALL confirm the device is revoked and subsequent protected API access SHALL fail
