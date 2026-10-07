# Profile Registry Specification

## Purpose

Defines logical profiles, immutable versions, and publication behavior.

## ADDED Requirements

### Requirement: Logical profile identity
The registry SHALL maintain stable logical profile identifiers independent of underlying `.ovpn` version contents.

#### Scenario: New version keeps logical identity

- **GIVEN** profile `personal-vpn` exists at version 3

- **WHEN** a new profile artifact is uploaded
- **THEN** the registry SHALL create version 4 under the same logical profile identifier


### Requirement: Immutable version history
A published profile version MUST NOT be mutated in place.

#### Scenario: Attempted replacement

- **WHEN** an operator attempts to overwrite an existing version
- **THEN** the registry SHALL reject the operation and require a new version


### Requirement: Current version pointer
Each logical profile SHALL expose at most one current published version per release channel.

#### Scenario: Promote version

- **WHEN** an operator promotes version 5 to stable
- **THEN** subsequent stable manifest requests SHALL resolve to version 5
