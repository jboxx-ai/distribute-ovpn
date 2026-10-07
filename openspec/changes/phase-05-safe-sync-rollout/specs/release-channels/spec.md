# Release Channels Specification

## Purpose

Defines stable and beta profile distribution without duplicating logical profile identity.

## ADDED Requirements

### Requirement: Channel-specific publication
A logical profile SHALL support independent current versions for stable and beta channels.

#### Scenario: Beta ahead of stable

- **GIVEN** stable is version 10 and beta is version 11

- **WHEN** a stable device checks for updates
- **THEN** it SHALL remain on version 10


#### Scenario: Beta device

- **WHEN** a beta-assigned device checks for updates
- **THEN** it SHALL resolve version 11
