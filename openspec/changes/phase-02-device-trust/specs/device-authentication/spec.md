# Device Authentication Specification

## Purpose

Defines ongoing authenticated requests and revocation semantics.

## ADDED Requirements

### Requirement: Scoped authentication
Every device request for protected profile data SHALL authenticate as one registered device.

#### Scenario: Unregistered device

- **WHEN** an unknown device attempts to fetch a protected manifest
- **THEN** the server SHALL deny access


#### Scenario: Revoked device

- **WHEN** a revoked device presents previously valid credentials
- **THEN** the server SHALL deny access


### Requirement: Credential rotation
A valid device SHALL be able to rotate its authentication credential without re-enrollment.

#### Scenario: Rotation

- **WHEN** credential rotation completes
- **THEN** the previous credential SHALL no longer authorize protected requests
