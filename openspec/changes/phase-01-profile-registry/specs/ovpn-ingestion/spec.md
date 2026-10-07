# OVPN Ingestion Specification

## Purpose

Defines server-side acceptance rules for uploaded OpenVPN profiles.

## ADDED Requirements

### Requirement: Syntactic validation
The registry SHALL reject profile content that cannot be parsed as an allowed OpenVPN client configuration.

#### Scenario: Malformed inline block

- **WHEN** an uploaded profile contains an unterminated certificate or key block
- **THEN** the upload SHALL be rejected and no new version SHALL be created


### Requirement: Dangerous directive rejection
The registry MUST reject directives forbidden by the configured profile security policy.

#### Scenario: Plugin directive

- **WHEN** an uploaded profile contains a forbidden `plugin` directive
- **THEN** the upload SHALL be rejected with a policy violation


#### Scenario: Script directive

- **WHEN** an uploaded profile contains a forbidden local script execution directive
- **THEN** the upload SHALL be rejected with a policy violation
