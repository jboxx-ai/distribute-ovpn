# Android Profile Sync Specification

## Purpose

Defines manual profile synchronization behavior on Android.

## ADDED Requirements

### Requirement: Manual update check
An enrolled Android client SHALL allow the user to request a profile synchronization check.

#### Scenario: Already current

- **WHEN** the installed version equals the signed current manifest
- **THEN** the client SHALL report that the profile is current without replacing local state


#### Scenario: New version available

- **WHEN** a higher authorized version is available
- **THEN** the client SHALL download, verify, validate, and stage that version


### Requirement: Verification before staging
The Android client MUST verify digest, signature, and local profile policy before a downloaded version becomes staged.

#### Scenario: Signature failure

- **WHEN** signature verification fails
- **THEN** the client SHALL discard the downloaded artifact and preserve the active profile
