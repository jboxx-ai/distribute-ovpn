# Profile Signing Specification

## Purpose

Defines artifact authenticity guarantees independent of transport security.

## ADDED Requirements

### Requirement: Signed published artifact
Every published profile version SHALL have a signature over security-relevant manifest data and the artifact digest.

#### Scenario: Valid artifact

- **WHEN** a client verifies an unmodified published artifact with the configured public key
- **THEN** verification SHALL succeed


#### Scenario: Modified artifact

- **WHEN** any signed artifact byte is changed
- **THEN** verification SHALL fail and the profile MUST NOT be staged


### Requirement: Unknown signing key
Clients MUST reject artifacts signed by an untrusted signing key.

#### Scenario: Unrecognized key id

- **WHEN** the manifest references a signing key the client does not trust
- **THEN** the client SHALL reject the update without changing the active profile
