# macOS Profile Sync Specification

## Purpose

Defines macOS behavior for retrieving and staging profile versions.

## ADDED Requirements

### Requirement: Shared synchronization semantics
macOS SHALL enforce the same manifest, digest, signature, and profile-policy validation semantics as Android.

#### Scenario: Tampered artifact

- **WHEN** a downloaded artifact fails digest or signature validation
- **THEN** macOS SHALL discard it and preserve the existing active profile


### Requirement: Manual sync
The user SHALL be able to request a synchronization check from the macOS application.

#### Scenario: Update found

- **WHEN** a valid newer assigned version exists
- **THEN** the application SHALL stage it and report the staged version
