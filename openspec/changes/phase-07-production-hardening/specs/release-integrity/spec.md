# Release Integrity Specification

## Purpose

Defines provenance requirements for server and client releases.

## ADDED Requirements

### Requirement: Signed client release
Production Android and macOS application artifacts SHALL be signed using platform-appropriate release identities.

#### Scenario: Unsigned production artifact

- **WHEN** a release pipeline produces an unsigned production client
- **THEN** the release gate SHALL fail


### Requirement: Dependency inventory
Each production release SHALL generate a software bill of materials or equivalent dependency inventory.

#### Scenario: Release output

- **WHEN** a production release completes
- **THEN** the release artifacts SHALL include a dependency inventory associated with that version
