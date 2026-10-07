# Android Enrollment Specification

## Purpose

Defines first-run Android registration behavior.

## ADDED Requirements

### Requirement: User-authorized enrollment
The Android client SHALL allow a user to enroll the device using a valid enrollment code and SHALL persist device credentials in OS-protected storage.

#### Scenario: First enrollment

- **WHEN** the user submits a valid enrollment code
- **THEN** the client SHALL register the device and show the assigned logical profile


#### Scenario: Invalid enrollment

- **WHEN** the enrollment code is invalid or expired
- **THEN** the client SHALL remain unenrolled and display a recoverable error
