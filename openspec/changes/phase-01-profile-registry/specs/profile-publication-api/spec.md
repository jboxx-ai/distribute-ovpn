# Profile Publication API Specification

## Purpose

Defines observable API behavior for retrieving profile manifests and profile artifacts.

## ADDED Requirements

### Requirement: Versioned manifest
The service SHALL return a manifest containing schema version, profile identity, artifact version, digest, size, and publication metadata.

#### Scenario: Fetch current manifest

- **WHEN** a caller requests the current manifest for an existing published profile
- **THEN** the response SHALL identify the exact immutable version to download


#### Scenario: Unknown profile

- **WHEN** a caller requests a profile that does not exist
- **THEN** the service SHALL return a not-found response without creating state


### Requirement: Artifact digest consistency
The digest in a manifest SHALL match the bytes returned for that immutable artifact version.

#### Scenario: Digest validation

- **WHEN** a downloaded artifact is hashed using the declared algorithm
- **THEN** the result SHALL equal the manifest digest
