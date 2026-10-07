# Audit Log Specification

## Purpose

Defines append-only security and administrative event records.

## ADDED Requirements

### Requirement: Security-sensitive event capture
The system SHALL audit profile publication, signing-key changes, enrollment issuance, device revocation, assignment changes, and rollout actions.

#### Scenario: Profile promotion

- **WHEN** an operator promotes a profile version
- **THEN** an audit event SHALL record actor, action, target, result, and timestamp without storing private profile secrets


### Requirement: Audit immutability
Ordinary administrative operations MUST NOT modify or delete existing audit events.

#### Scenario: Attempted edit

- **WHEN** an operator attempts to edit a prior audit event
- **THEN** the system SHALL reject the operation
