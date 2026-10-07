# Automatic Rollback Specification

## Purpose

Defines recovery behavior after candidate activation failure.

## ADDED Requirements

### Requirement: Rollback on failed health check
The client SHALL restore the last-known-good profile when a newly activated candidate fails required tunnel health verification.

#### Scenario: New tunnel cannot reach health target

- **WHEN** candidate activation fails the configured health check within the allowed window
- **THEN** the client SHALL disconnect the candidate and restore the previous active profile


#### Scenario: Rollback also fails

- **WHEN** restoring the previous profile fails
- **THEN** the client SHALL enter a degraded disconnected state and preserve diagnostic state for the user
