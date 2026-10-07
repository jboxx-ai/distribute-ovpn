# Atomic Profile Activation Specification

## Purpose

Defines safe state transition from an active profile version to a staged profile version.

## ADDED Requirements

### Requirement: Preserve last-known-good profile
A client MUST retain the current active profile until the candidate profile has passed verification and activation checks.

#### Scenario: Candidate invalid

- **WHEN** a candidate fails pre-activation validation
- **THEN** the existing active profile SHALL remain unchanged


#### Scenario: Candidate activates

- **WHEN** a candidate establishes the tunnel and passes configured health checks
- **THEN** the candidate SHALL become active and the previous version SHALL become rollback history


### Requirement: Crash-safe state
Profile activation state SHALL survive process termination without leaving an ambiguous active version.

#### Scenario: Crash during activation

- **WHEN** the client restarts after termination during activation
- **THEN** it SHALL recover to either the previous committed active version or a deterministically resumable staged state
