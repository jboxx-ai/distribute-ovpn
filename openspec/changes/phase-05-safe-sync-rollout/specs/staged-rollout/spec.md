# Staged Rollout Specification

## Purpose

Defines server-controlled deployment progression and pause behavior.

## ADDED Requirements

### Requirement: Eligible cohort
A rollout SHALL expose a candidate version only to devices currently eligible for that rollout stage.

#### Scenario: Device outside cohort

- **WHEN** a device is not yet eligible for the rollout stage
- **THEN** its manifest SHALL continue to resolve the prior published version


### Requirement: Pause rollout
An operator SHALL be able to pause further rollout without invalidating already activated devices.

#### Scenario: Pause after failures

- **WHEN** an operator pauses a rollout
- **THEN** no additional non-eligible device SHALL receive the candidate version
