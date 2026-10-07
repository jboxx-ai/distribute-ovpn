# Security Hardening Specification

## Purpose

Defines minimum security guarantees before production designation.

## ADDED Requirements

### Requirement: No arbitrary local execution from profile
A remotely distributed profile MUST NOT cause arbitrary local script or plugin execution through unsupported OpenVPN directives.

#### Scenario: Dangerous directive bypass attempt

- **WHEN** a profile contains an encoded or alternate-form forbidden execution directive
- **THEN** server and client validation SHALL reject it


### Requirement: Secret minimization
Logs, metrics, crash reports, and audit records MUST NOT contain private keys, passwords, bearer tokens, or complete sensitive profile bodies.

#### Scenario: Error path

- **WHEN** profile parsing fails around an inline private key
- **THEN** diagnostics SHALL identify the location/type without reproducing private key contents
