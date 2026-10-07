# macOS Client Status Specification

## Purpose

Defines registry-visible macOS status using the same privacy rules as Android.

## ADDED Requirements

### Requirement: Cross-platform status contract
macOS SHALL report installed profile version, application version, sync outcome, and tunnel state using the shared client status schema.

#### Scenario: Status parity

- **WHEN** Android and macOS report equivalent states
- **THEN** the registry SHALL interpret them using the same state vocabulary
