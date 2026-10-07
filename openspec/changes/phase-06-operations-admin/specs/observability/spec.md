# Observability Specification

## Purpose

Defines operational signals required to diagnose registry and deployment health.

## ADDED Requirements

### Requirement: Service health
The registry SHALL expose liveness and readiness state suitable for orchestration and monitoring.

#### Scenario: Database unavailable

- **WHEN** the registry cannot reach required persistence
- **THEN** readiness SHALL report unhealthy while liveness may remain healthy


### Requirement: Metrics
The service SHALL expose aggregate metrics for request outcomes, synchronization results, activation failures, rollback outcomes, and rollout progress without exposing VPN credentials.

#### Scenario: Failed activation reported

- **WHEN** a client reports a candidate activation failure
- **THEN** an aggregate failure metric SHALL increase
