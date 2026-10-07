# Backup and Restore Specification

## Purpose

Defines recoverability for persistent registry state and cryptographic material.

## ADDED Requirements

### Requirement: Restorable registry state
Documented backups SHALL contain enough data to restore profiles, versions, assignments, rollout state, and audit history.

#### Scenario: Restore drill

- **WHEN** a backup is restored into an empty supported environment
- **THEN** published manifests and authorized device assignments SHALL match the backup point


### Requirement: Signing key recovery
Signing key backup SHALL be protected separately from ordinary database backup and SHALL have a documented recovery procedure.

#### Scenario: Database backup theft

- **WHEN** only a database backup is exposed
- **THEN** it SHALL not alone provide the profile signing private key
