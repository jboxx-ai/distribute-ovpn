# Upgrade Compatibility Specification

## Purpose

Defines supported client/server and manifest-version upgrade behavior.

## ADDED Requirements

### Requirement: Supported skew
The project SHALL document and test the supported client/server version skew for every production release.

#### Scenario: Older supported client

- **WHEN** a supported older client communicates with a newer server
- **THEN** the server SHALL return only contracts the client can safely interpret or a non-destructive upgrade-required response


### Requirement: Unknown security semantics
A client MUST fail closed when a newer contract introduces unknown security-critical semantics.

#### Scenario: Unsupported policy requirement

- **WHEN** a manifest requires a security policy capability the client does not implement
- **THEN** the client SHALL not activate the candidate
