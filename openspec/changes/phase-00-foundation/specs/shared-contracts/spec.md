# Shared Contracts Specification

## Purpose

Defines cross-platform contracts that later phases implement without coupling shared code to platform VPN APIs.

## ADDED Requirements

### Requirement: Platform-independent tunnel contract
Shared code SHALL interact with VPN tunnel behavior through a platform-neutral contract.

#### Scenario: Android adapter substitution

- **WHEN** shared synchronization logic invokes tunnel preparation
- **THEN** the Android implementation SHALL be replaceable without changing shared synchronization code


#### Scenario: macOS adapter substitution

- **WHEN** shared synchronization logic invokes tunnel preparation
- **THEN** the macOS implementation SHALL be replaceable without changing shared synchronization code


### Requirement: Versioned wire contracts
Network payloads and profile manifests SHALL include an explicit schema version.

#### Scenario: Unknown schema received

- **WHEN** a client receives a manifest schema version it does not support
- **THEN** the client SHALL reject it without modifying the active profile
