# Background Sync Specification

## Purpose

Defines best-effort periodic update checks within Android and macOS platform scheduling constraints.

## ADDED Requirements

### Requirement: Periodic check
Clients SHALL support platform-appropriate background checks without promising exact execution time.

#### Scenario: Background execution delayed

- **WHEN** the OS delays scheduled background work
- **THEN** the client SHALL remain correct and SHALL check again when execution is permitted


#### Scenario: Update while tunnel active

- **WHEN** a background check discovers a newer version during an active tunnel
- **THEN** the configured update policy SHALL determine whether to defer, notify, or safely activate
