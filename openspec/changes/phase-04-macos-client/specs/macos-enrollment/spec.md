# macOS Enrollment Specification

## Purpose

Defines first-run macOS registration behavior.

## ADDED Requirements

### Requirement: Keychain-protected enrollment
The macOS client SHALL enroll using a valid enrollment authorization and SHALL store device credentials using macOS protected credential storage.

#### Scenario: Successful enrollment

- **WHEN** the user submits a valid enrollment code
- **THEN** the app SHALL register the Mac and display its assigned logical profile


#### Scenario: Credential unavailable

- **WHEN** required device credentials cannot be read from protected storage
- **THEN** the app SHALL not fetch protected profile content and SHALL surface a recoverable error
